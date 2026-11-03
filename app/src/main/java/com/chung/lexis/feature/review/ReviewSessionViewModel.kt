package com.chung.lexis.feature.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.chung.lexis.core.log.AppLogger
import com.chung.lexis.domain.model.CorruptReviewDataException
import com.chung.lexis.domain.model.LearningProgress
import com.chung.lexis.domain.model.ReviewCard
import com.chung.lexis.domain.model.ReviewLog
import com.chung.lexis.domain.model.ReviewRating
import com.chung.lexis.domain.model.ReviewSessionLaunchPolicy
import com.chung.lexis.domain.model.ReviewState
import com.chung.lexis.domain.repository.ReviewRepository
import com.chung.lexis.domain.repository.SaveReviewResult
import com.chung.lexis.domain.repository.UndoReviewResult
import com.chung.lexis.domain.usecase.SubmitReviewUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Clock
import java.time.Duration
import java.time.Instant

sealed interface ReviewSessionUiState {
    data object Idle : ReviewSessionUiState

    data object Loading : ReviewSessionUiState

    data class Active(
        val card: ReviewCard,

        /*
         * Khoảng tới lần ôn tiếp theo nếu chọn
         * từng rating, để hiện dưới mỗi nút.
         */
        val ratingIntervals: Map<ReviewRating, Duration>,

        val isAnswerRevealed: Boolean = false,
        val isSubmitting: Boolean = false,
        val reviewedCount: Int,
        val remainingCount: Int,
        val error: ReviewSessionError? = null,

        /*
         * Có rating trước đó trong session
         * hoàn tác được.
         */
        val canUndo: Boolean = false,
    ) : ReviewSessionUiState

    data class Waiting(
        val nextDueAt: Instant,
        val reviewedCount: Int,
        val remainingCount: Int,
    ) : ReviewSessionUiState

    data class Completed(
        val stats: ReviewSessionStats,

        /*
         * Từ lúc bắt đầu session tới lúc Completed.
         */
        val duration: Duration,
    ) : ReviewSessionUiState {

        val reviewedCount: Int
            get() = stats.reviewedCount
    }

    data class Failed(
        val error: ReviewSessionError,
    ) : ReviewSessionUiState
}

enum class ReviewSessionError {
    LOAD_FAILED,
    SAVE_FAILED,
    STALE_PROGRESS,
    UNDO_FAILED,
}

/*
 * Đếm theo từng lần rating đã lưu: card học lại
 * trong session (learning step) được đếm mỗi lần.
 */
data class ReviewSessionStats(
    val againCount: Int = 0,
    val hardCount: Int = 0,
    val goodCount: Int = 0,
    val easyCount: Int = 0,

    /*
     * Lexeme bị chọn AGAIN hoặc HARD ít nhất một lần,
     * theo thứ tự lần đầu gặp khó.
     */
    val difficultLexemeIds: List<Long> = emptyList(),
) {

    val reviewedCount: Int
        get() =
            againCount +
                    hardCount +
                    goodCount +
                    easyCount

    val rememberedCount: Int
        get() =
            goodCount +
                    easyCount

    fun record(
        lexemeId: Long,
        rating: ReviewRating,
    ): ReviewSessionStats {

        val counted =
            when (rating) {
                ReviewRating.AGAIN ->
                    copy(againCount = againCount + 1)

                ReviewRating.HARD ->
                    copy(hardCount = hardCount + 1)

                ReviewRating.GOOD ->
                    copy(goodCount = goodCount + 1)

                ReviewRating.EASY ->
                    copy(easyCount = easyCount + 1)
            }

        val isDifficult =
            rating == ReviewRating.AGAIN ||
                    rating == ReviewRating.HARD

        return if (
            isDifficult &&
            lexemeId !in difficultLexemeIds
        ) {
            counted.copy(
                difficultLexemeIds =
                    difficultLexemeIds + lexemeId,
            )
        } else {
            counted
        }
    }
}


class ReviewSessionViewModel(
    private val reviewRepository: ReviewRepository,

    private val submitReviewUseCase: SubmitReviewUseCase,

    private val launchPolicy: ReviewSessionLaunchPolicy,

    private val logger: AppLogger,

    /*
     * Zone của clock quyết định "hôm nay"
     * cho giới hạn từ mới mỗi ngày.
     */
    private val clock: Clock = Clock.systemDefaultZone(),
) : ViewModel() {

    private enum class SessionQueueKind {
        SHORT_TERM,
        REVIEW,
        NEW,
    }

    private data class SessionQueueItem(
        val lexemeId: Long,
        val kind: SessionQueueKind,
        val dueAt: Instant?,
        val sequence: Long,
    )

    private data class ScheduledRepeat(
        val lexemeId: Long,
        val dueAt: Instant,
    )

    private data class LoadedCard(
        val card: ReviewCard,
        val ratingIntervals: Map<ReviewRating, Duration>,
    )

    private sealed interface ReviewSessionRecovery {

        data object StartSession :
            ReviewSessionRecovery

        data object LoadNextCard :
            ReviewSessionRecovery

        data class ReloadCurrentCard(
            val lexemeId: Long,
        ) : ReviewSessionRecovery

        data class ShowUndoneCard(
            val lexemeId: Long,
        ) : ReviewSessionRecovery
    }

    /*
     * Một rating đã lưu, kèm session ngay trước nó:
     * undo trả DB về previousProgress và
     * queue / stats về snapshot.
     */
    private data class UndoEntry(
        val previousProgress: LearningProgress,
        val savedProgress: LearningProgress,
        val reviewLog: ReviewLog,
        val statsBefore: ReviewSessionStats,
        val pendingBefore: List<SessionQueueItem>,
        val repeatsBefore: Map<Long, ScheduledRepeat>,
    )

    private val _uiState =
        MutableStateFlow<ReviewSessionUiState>(
            ReviewSessionUiState.Idle
        )

    val uiState:
            StateFlow<ReviewSessionUiState> =
        _uiState.asStateFlow()

    private val pendingCards =
        mutableListOf<SessionQueueItem>()

    private val scheduledRepeats =
        mutableMapOf<Long, ScheduledRepeat>()

    private var nextSequence =
        0L

    private var sessionStats =
        ReviewSessionStats()

    private var sessionStartedAt: Instant? =
        null

    private var sessionDeckId: Long? =
        null

    private val undoStack =
        ArrayDeque<UndoEntry>()

    private val reviewedCount: Int
        get() = sessionStats.reviewedCount

    private var pendingRecovery:
            ReviewSessionRecovery? = null

    private val foregroundEvents =
        Channel<Unit>(
            capacity =
                Channel.CONFLATED,
        )

    private val queueComparator =
        Comparator<SessionQueueItem> {
                left,
                right,
            ->

            val kindComparison =
                left.kind
                    .ordinal
                    .compareTo(
                        right.kind.ordinal
                    )

            if (kindComparison != 0) {

                kindComparison

            } else if (
                left.kind !=
                SessionQueueKind.NEW
            ) {

                val leftDue =
                    requireNotNull(
                        left.dueAt
                    )

                val rightDue =
                    requireNotNull(
                        right.dueAt
                    )

                val dueComparison =
                    leftDue.compareTo(
                        rightDue
                    )

                if (dueComparison != 0) {
                    dueComparison
                } else {
                    left.sequence.compareTo(
                        right.sequence
                    )
                }

            } else {
                left.sequence.compareTo(
                    right.sequence
                )
            }
        }

    /**
     * @param deckId null = ôn mọi deck,
     * khác null = chỉ card thuộc deck đó.
     * Retry / startAnotherSession giữ nguyên deck.
     */
    fun startSession(
        deckId: Long? = null,
    ) {

        if (
            _uiState.value != ReviewSessionUiState.Idle
        ) {
            return
        }

        sessionDeckId =
            deckId

        _uiState.value = ReviewSessionUiState.Loading

        pendingRecovery =
            ReviewSessionRecovery.StartSession

        viewModelScope.launch {

            try {

                val now =
                    clock.instant()

                val queue =
                    reviewRepository
                        .getReviewQueue(
                            now = now,

                            dueLimit =
                                launchPolicy
                                    .dueLimit,

                            newLimit =
                                remainingNewToday(
                                    now = now,
                                ),

                            deckId =
                                deckId,
                        )

                val lexemeIds = queue.map {
                    it.lexemeId
                }

                check(
                    lexemeIds.size ==
                            lexemeIds
                                .toSet()
                                .size
                ) {
                    "Review queue contains duplicate lexemeId"
                }

                /*
                 * Short-term card chưa tới hạn
                 * (vd. process bị kill khi đang chờ step 1m)
                 * → nạp lại như repeat của session này,
                 * thay vì Completed sớm.
                 *
                 * dueLimit = 0 → session không lấy due card,
                 * nên cũng không lấy short-term.
                 */
                val upcomingShortTerm =
                    if (launchPolicy.dueLimit == 0) {
                        emptyList()
                    } else {
                        reviewRepository
                            .getUpcomingShortTerm(
                                now = now,
                                deckId = deckId,
                            )
                    }

                pendingCards.clear()
                scheduledRepeats.clear()

                nextSequence = 0L

                queue.forEach { progress ->

                    pendingCards +=
                        progress
                            .toSessionQueueItem(
                                sequence =
                                    nextSequence++,
                            )
                }

                upcomingShortTerm
                    .filter { progress ->
                        progress.lexemeId !in
                                lexemeIds
                    }
                    .forEach { progress ->

                        updateScheduledRepeat(
                            progress = progress,
                        )
                    }

                pendingCards.sortWith(
                    queueComparator
                )

                sessionStats =
                    ReviewSessionStats()

                sessionStartedAt =
                    now

                undoStack.clear()

                pendingRecovery =
                    ReviewSessionRecovery.LoadNextCard

                loadNextCard()

                pendingRecovery = null

            } catch (
                cancellation:
                CancellationException,
            ) {

                throw cancellation

            } catch (failure: Exception) {

                logger.error(
                    TAG,
                    "Cannot start review session",
                    failure,
                )

                _uiState.value =
                    ReviewSessionUiState.Failed(
                        error =
                            ReviewSessionError
                                .LOAD_FAILED,
                    )
            }
        }
    }

    /*
     * newLimit là budget của cả ngày:
     * trừ đi từ mới đã học từ 0h hôm nay,
     * để process death / mở lại app
     * không cấp thêm newLimit từ mới.
     */
    private suspend fun remainingNewToday(
        now: Instant,
    ): Int {

        if (launchPolicy.newLimit == 0) {
            return 0
        }

        val startOfDay =
            now
                .atZone(clock.zone)
                .toLocalDate()
                .atStartOfDay(clock.zone)
                .toInstant()

        val startedToday =
            reviewRepository
                .countNewCardsStartedSince(
                    since = startOfDay,
                )

        return (launchPolicy.newLimit - startedToday)
            .coerceAtLeast(0)
    }

    fun startAnotherSession() {

        if (
            _uiState.value !is
                    ReviewSessionUiState.Completed
        ) {
            return
        }

        /*
         * Đây là explicit user action.
         *
         * Khác với Route gọi startSession():
         * Completed không tự restart.
         */
        _uiState.value =
            ReviewSessionUiState.Idle

        startSession(
            deckId = sessionDeckId,
        )
    }

    fun retry() {

        if (
            _uiState.value !is
                    ReviewSessionUiState.Failed
        ) {
            return
        }

        val recovery =
            pendingRecovery
                ?: return

        when (recovery) {

            ReviewSessionRecovery.StartSession -> {

                _uiState.value =
                    ReviewSessionUiState.Idle

                startSession(
                    deckId = sessionDeckId,
                )
            }

            ReviewSessionRecovery.LoadNextCard -> {

                /*
                 * Set Loading đồng bộ trước launch
                 * để double tap Retry không launch
                 * hai coroutine.
                 */
                _uiState.value =
                    ReviewSessionUiState.Loading

                viewModelScope.launch {

                    loadNextCardSafely()
                }
            }

            is ReviewSessionRecovery
                .ReloadCurrentCard -> {

                _uiState.value =
                    ReviewSessionUiState.Loading

                viewModelScope.launch {

                    reloadCurrentAfterStaleSafely(
                        lexemeId =
                            recovery.lexemeId,
                    )
                }
            }

            is ReviewSessionRecovery
                .ShowUndoneCard -> {

                _uiState.value =
                    ReviewSessionUiState.Loading

                viewModelScope.launch {

                    showUndoneCardSafely(
                        lexemeId =
                            recovery.lexemeId,
                    )
                }
            }
        }
    }

    /*
     * Hoàn tác rating gần nhất của session:
     * DB về progress trước rating, queue / stats
     * về snapshot, rồi hiện lại card đó ở mặt sau.
     */
    fun undoLastRating() {

        val current =
            _uiState.value
                    as? ReviewSessionUiState.Active
                ?: return

        if (
            current.isSubmitting ||
            undoStack.isEmpty()
        ) {
            return
        }

        val entry =
            undoStack.last()

        /*
         * Khoá Active như lúc submit:
         * double tap không undo hai lần.
         */
        _uiState.value =
            current.copy(
                isSubmitting = true,
                error = null,
            )

        viewModelScope.launch {

            val result =
                try {

                    reviewRepository.undoReview(
                        previousProgress =
                            entry.previousProgress,

                        savedProgress =
                            entry.savedProgress,

                        reviewLog =
                            entry.reviewLog,
                    )

                } catch (
                    cancellation:
                    CancellationException,
                ) {

                    throw cancellation

                } catch (failure: Exception) {

                    logger.error(
                        TAG,
                        "Cannot undo review of lexeme " +
                                entry.reviewLog.lexemeId,
                        failure,
                    )

                    /*
                     * Chưa hoàn tác: giữ undo stack
                     * để user thử lại.
                     */
                    _uiState.value =
                        current.copy(
                            isSubmitting = false,

                            error =
                                ReviewSessionError
                                    .UNDO_FAILED,
                        )

                    return@launch
                }

            when (result) {

                is UndoReviewResult.Undone -> {

                    undoStack.removeLast()

                    sessionStats =
                        entry.statsBefore

                    pendingCards.clear()
                    pendingCards +=
                        entry.pendingBefore

                    scheduledRepeats.clear()
                    scheduledRepeats +=
                        entry.repeatsBefore

                    _uiState.value =
                        ReviewSessionUiState.Loading

                    showUndoneCardSafely(
                        lexemeId =
                            entry.reviewLog.lexemeId,
                    )
                }

                is UndoReviewResult.StaleProgress,
                UndoReviewResult.ReviewNotFound -> {

                    /*
                     * Rating gần nhất không hoàn tác được
                     * → snapshot của rating cũ hơn
                     * cũng không còn đúng.
                     */
                    undoStack.clear()

                    _uiState.value =
                        current.copy(
                            isSubmitting = false,

                            canUndo = false,

                            error =
                                ReviewSessionError
                                    .UNDO_FAILED,
                        )
                }
            }
        }
    }

    private suspend fun showUndoneCardSafely(
        lexemeId: Long,
    ) {

        pendingRecovery =
            ReviewSessionRecovery
                .ShowUndoneCard(
                    lexemeId =
                        lexemeId,
                )

        try {

            showUndoneCard(
                lexemeId =
                    lexemeId,
            )

            pendingRecovery =
                null

        } catch (
            cancellation:
            CancellationException,
        ) {

            throw cancellation

        } catch (failure: Exception) {

            logger.error(
                TAG,
                "Cannot show undone lexeme $lexemeId",
                failure,
            )

            _uiState.value =
                ReviewSessionUiState.Failed(
                    error =
                        ReviewSessionError
                            .LOAD_FAILED,
                )
        }
    }

    /*
     * Mặt sau: user vừa thấy đáp án,
     * chỉ cần chọn lại rating.
     */
    private suspend fun showUndoneCard(
        lexemeId: Long,
    ) {

        val loaded =
            loadCardOrSkip(
                lexemeId
            )

        if (loaded == null) {

            loadNextCard()
            return
        }

        _uiState.value =
            ReviewSessionUiState.Active(
                card =
                    loaded.card,

                ratingIntervals =
                    loaded.ratingIntervals,

                isAnswerRevealed =
                    true,

                isSubmitting =
                    false,

                reviewedCount =
                    reviewedCount,

                remainingCount =
                    1 +
                            pendingCards.size +
                            scheduledRepeats.size,

                error =
                    null,

                canUndo =
                    undoStack.isNotEmpty(),
            )
    }

    fun onForeground() {

        foregroundEvents
            .trySend(Unit)
    }

    fun revealAnswer() {

        val current =
            _uiState.value
                    as? ReviewSessionUiState.Active
                ?: return

        if (
            current.isSubmitting ||
            current.isAnswerRevealed
        ) {
            return
        }

        _uiState.value =
            current.copy(
                isAnswerRevealed = true,
                error = null,
            )
    }

    fun submitRating(
        rating: ReviewRating,
    ) {

        val current =
            _uiState.value
                    as? ReviewSessionUiState.Active
                ?: return

        if (
            !current.isAnswerRevealed ||
            current.isSubmitting
        ) {
            return
        }

        _uiState.value =
            current.copy(
                isSubmitting = true,
                error = null,
            )

        viewModelScope.launch {

            val result =
                try {

                    submitReviewUseCase(
                        /*
                         * Snapshot user đang thấy:
                         * write khác xen vào → StaleProgress.
                         */
                        progress =
                            current
                                .card
                                .progress,

                        rating =
                            rating,

                        reviewedAt =
                            clock.instant(),
                    )

                } catch (
                    cancellation:
                    CancellationException,
                ) {

                    throw cancellation

                } catch (
                    corrupt:
                    CorruptReviewDataException,
                ) {

                    /*
                     * Retry rating không bao giờ thành công
                     * → bỏ qua card, đi tiếp session.
                     */
                    skipCorruptCard(corrupt)

                    _uiState.value =
                        ReviewSessionUiState.Loading

                    loadNextCardSafely()

                    return@launch

                } catch (failure: Exception) {

                    logger.error(
                        TAG,
                        "Cannot save review of lexeme " +
                                current.card.lexeme.id,
                        failure,
                    )

                    /*
                     * Review CHƯA được xác nhận save.
                     * Cho user retry rating hiện tại.
                     */
                    _uiState.value =
                        current.copy(
                            isSubmitting =
                                false,

                            error =
                                ReviewSessionError
                                    .SAVE_FAILED,
                        )

                    return@launch
                }

            when (result) {

                is SaveReviewResult.Saved -> {

                    /*
                     * Chụp session trước khi rating này
                     * đổi queue / stats.
                     */
                    undoStack.addLast(
                        UndoEntry(
                            previousProgress =
                                current
                                    .card
                                    .progress,

                            savedProgress =
                                result.progress,

                            reviewLog =
                                result.reviewLog,

                            statsBefore =
                                sessionStats,

                            pendingBefore =
                                pendingCards.toList(),

                            repeatsBefore =
                                scheduledRepeats.toMap(),
                        )
                    )

                    sessionStats =
                        sessionStats.record(
                            lexemeId =
                                current
                                    .card
                                    .lexeme
                                    .id,

                            rating =
                                rating,
                        )

                    updateScheduledRepeat(
                        progress =
                            result.progress,
                    )

                    _uiState.value =
                        ReviewSessionUiState.Loading

                    loadNextCardSafely()
                }

                SaveReviewResult.LexemeNotFound -> {

                    _uiState.value =
                        ReviewSessionUiState.Loading

                    loadNextCardSafely()
                }

                is SaveReviewResult.StaleProgress -> {
                    handleStaleProgress(
                        lexemeId =
                            current
                                .card
                                .lexeme
                                .id,

                        progress =
                            result.current,
                    )
                }
            }
        }
    }

    private suspend fun handleStaleProgress(
        lexemeId: Long,
        progress: LearningProgress,
    ) {

        val now =
            clock.instant()

        val nextReviewAt =
            progress.nextReviewAt

        val isFutureScheduled =
            progress.state !=
                    ReviewState.NEW &&
                    nextReviewAt != null &&
                    nextReviewAt.isAfter(now)

        if (isFutureScheduled) {

            /*
             * Nếu external write chuyển nó thành
             * LEARNING/RELEARNING future,
             * giữ nó trong session.
             *
             * Nếu REVIEW future,
             * remove khỏi in-session repeats.
             */
            updateScheduledRepeat(
                progress = progress,
            )

            _uiState.value =
                ReviewSessionUiState.Loading

            loadNextCardSafely()

            return
        }

        /*
         * Vẫn due ngay → hydrate snapshot mới,
         * bắt user đánh giá lại.
         */
        pendingRecovery =
            ReviewSessionRecovery
                .ReloadCurrentCard(
                    lexemeId =
                        lexemeId,
                )

        reloadCurrentAfterStaleSafely(
            lexemeId =
                lexemeId,
        )
    }

    private suspend fun loadNextCardSafely() {

        pendingRecovery =
            ReviewSessionRecovery.LoadNextCard

        try {

            loadNextCard()

            pendingRecovery = null

        } catch (
            cancellation:
            CancellationException,
        ) {

            throw cancellation

        } catch (failure: Exception) {

            logger.error(
                TAG,
                "Cannot load next review card",
                failure,
            )

            _uiState.value =
                ReviewSessionUiState.Failed(
                    error =
                        ReviewSessionError
                            .LOAD_FAILED,
                )
        }
    }

    private suspend fun reloadCurrentAfterStaleSafely(
        lexemeId: Long,
    ) {

        pendingRecovery =
            ReviewSessionRecovery
                .ReloadCurrentCard(
                    lexemeId =
                        lexemeId,
                )

        try {

            reloadCurrentAfterStale(
                lexemeId =
                    lexemeId,
            )

            pendingRecovery =
                null

        } catch (
            cancellation:
            CancellationException,
        ) {

            throw cancellation

        } catch (failure: Exception) {

            logger.error(
                TAG,
                "Cannot reload lexeme $lexemeId after stale progress",
                failure,
            )

            _uiState.value =
                ReviewSessionUiState.Failed(
                    error =
                        ReviewSessionError
                            .LOAD_FAILED,
                )
        }
    }

    private fun updateScheduledRepeat(
        progress: LearningProgress,
    ) {

        when (progress.state) {

            ReviewState.LEARNING,
            ReviewState.RELEARNING -> {

                val dueAt =
                    requireNotNull(
                        progress.nextReviewAt
                    ) {
                        "${progress.state} " +
                                "must have nextReviewAt"
                    }

                scheduledRepeats[
                    progress.lexemeId
                ] =
                    ScheduledRepeat(
                        lexemeId =
                            progress.lexemeId,

                        dueAt =
                            dueAt,
                    )
            }

            ReviewState.NEW,
            ReviewState.REVIEW -> {

                scheduledRepeats.remove(
                    progress.lexemeId
                )
            }
        }
    }

    private fun promoteDueRepeats(
        dueBy: Instant,
    ) {

        val dueRepeats =
            scheduledRepeats
                .values
                .filter { repeat ->

                    !repeat
                        .dueAt
                        .isAfter(dueBy)
                }
                .sortedWith(
                    compareBy<ScheduledRepeat> {
                        it.dueAt
                    }.thenBy {
                        it.lexemeId
                    }
                )

        dueRepeats.forEach { repeat ->

            scheduledRepeats.remove(
                repeat.lexemeId
            )

            /*
             * Defensive dedupe.
             */
            if (
                pendingCards.none {
                    it.lexemeId ==
                            repeat.lexemeId
                }
            ) {

                pendingCards +=
                    SessionQueueItem(
                        lexemeId =
                            repeat.lexemeId,

                        kind =
                            SessionQueueKind
                                .SHORT_TERM,

                        dueAt =
                            repeat.dueAt,

                        sequence =
                            nextSequence++,
                    )
            }
        }

        pendingCards.sortWith(
            queueComparator
        )
    }

    private suspend fun loadNextCard() {

        while (true) {

            val now =
                clock.instant()

            promoteDueRepeats(
                dueBy = now,
            )

            /*
             * Hết card khác → học trước short-term repeat
             * tới hạn trong learnAheadLimit,
             * thay vì Waiting cả learning step.
             */
            if (
                pendingCards.isEmpty()
            ) {

                promoteDueRepeats(
                    dueBy =
                        now.plus(
                            launchPolicy
                                .learnAheadLimit
                        ),
                )
            }

            if (
                pendingCards.isNotEmpty()
            ) {

                val item =
                    pendingCards.first()

                val loaded =
                    loadCardOrSkip(
                        item.lexemeId
                    )

                if (loaded == null) {

                    pendingCards
                        .removeAt(0)

                    scheduledRepeats.remove(
                        item.lexemeId
                    )

                    continue
                }

                pendingCards
                    .removeAt(0)

                _uiState.value =
                    ReviewSessionUiState.Active(
                        card =
                            loaded.card,

                        ratingIntervals =
                            loaded.ratingIntervals,

                        isAnswerRevealed =
                            false,

                        isSubmitting =
                            false,

                        reviewedCount =
                            reviewedCount,

                        /*
                         * Known work hiện tại:
                         *
                         * current
                         * + ready queue
                         * + future short-term repeats
                         */
                        remainingCount =
                            1 +
                                    pendingCards.size +
                                    scheduledRepeats.size,

                        error =
                            null,

                        canUndo =
                            undoStack.isNotEmpty(),
                    )

                return
            }

            val nextRepeat =
                scheduledRepeats
                    .values
                    .minWithOrNull(
                        compareBy<ScheduledRepeat> {
                            it.dueAt
                        }.thenBy {
                            it.lexemeId
                        }
                    )

            if (nextRepeat == null) {

                _uiState.value =
                    ReviewSessionUiState.Completed(
                        stats =
                            sessionStats,

                        duration =
                            Duration.between(
                                sessionStartedAt
                                    ?: now,
                                now,
                            ),
                    )

                return
            }

            /*
             * Repeat học được ngay khi vào
             * learn-ahead window, trước dueAt thật.
             */
            val nextAvailableAt =
                nextRepeat
                    .dueAt
                    .minus(
                        launchPolicy
                            .learnAheadLimit
                    )

            _uiState.value =
                ReviewSessionUiState.Waiting(
                    nextDueAt =
                        nextAvailableAt,

                    reviewedCount =
                        reviewedCount,

                    remainingCount =
                        scheduledRepeats.size,
                )

            awaitDueOrForeground(
                dueAt =
                    nextAvailableAt,
            )

            /*
             * Wake → loop lại → đọc Clock thật
             * → promoteDueRepeats().
             */
        }
    }

    private suspend fun awaitDueOrForeground(
        dueAt: Instant,
    ) {

        while (true) {

            val remainingMillis =
                Duration
                    .between(
                        clock.instant(),
                        dueAt,
                    )
                    .toMillis()

            if (remainingMillis <= 0L) {
                return
            }

            /*
             * Wake bởi một trong hai:
             *
             * 1. timer tới hạn
             * 2. app foreground lại
             *
             * Sau đó luôn quay lên đọc Clock thật.
             */
            withTimeoutOrNull(
                remainingMillis
                    .coerceAtLeast(1L)
            ) {

                foregroundEvents
                    .receive()
            }
        }
    }

    private suspend fun reloadCurrentAfterStale(
        lexemeId: Long,
    ) {

        val loaded =
            loadCardOrSkip(
                lexemeId
            )

        if (loaded == null) {

            loadNextCard()
            return
        }

        _uiState.value =
            ReviewSessionUiState.Active(
                card =
                    loaded.card,

                ratingIntervals =
                    loaded.ratingIntervals,

                isAnswerRevealed =
                    false,

                isSubmitting =
                    false,

                reviewedCount =
                    reviewedCount,

                remainingCount =
                    1 +
                            pendingCards.size +
                            scheduledRepeats.size,

                error =
                    ReviewSessionError
                        .STALE_PROGRESS,

                canUndo =
                    undoStack.isNotEmpty(),
            )
    }

    /*
     * Card có data hỏng → retry không bao giờ thành công.
     *
     * Skip rồi trả null để caller bỏ qua nó
     * giống card đã bị xoá, thay vì kẹt cả
     * session ở LOAD_FAILED.
     */
    private suspend fun loadCardOrSkip(
        lexemeId: Long,
    ): LoadedCard? {

        return try {

            val card =
                reviewRepository
                    .getReviewCard(
                        lexemeId
                    )
                    ?: return null

            LoadedCard(
                card =
                    card,

                /*
                 * Tính lúc hiện card: progress mà
                 * scheduler từ chối bị phát hiện
                 * trước khi user trả lời.
                 */
                ratingIntervals =
                    submitReviewUseCase
                        .previewIntervals(
                            progress =
                                card.progress,

                            reviewedAt =
                                clock.instant(),
                        ),
            )

        } catch (
            corrupt:
            CorruptReviewDataException,
        ) {

            skipCorruptCard(corrupt)

            null
        }
    }

    /*
     * Log và cách ly card: queue / badge của
     * session sau bỏ qua nó, thay vì load rồi
     * skip lại mãi.
     */
    private suspend fun skipCorruptCard(
        corrupt: CorruptReviewDataException,
    ) {

        logger.error(
            TAG,
            "Skip lexeme ${corrupt.lexemeId}: " +
                    "invalid review data",
            corrupt,
        )

        try {

            reviewRepository
                .quarantineReviewCard(
                    lexemeId =
                        corrupt.lexemeId,

                    reason =
                        (corrupt.cause ?: corrupt)
                            .toString(),

                    detectedAt =
                        clock.instant(),
                )

        } catch (
            cancellation:
            CancellationException,
        ) {

            throw cancellation

        } catch (failure: Exception) {

            /*
             * Không cách ly được vẫn skip card,
             * chỉ là session sau sẽ gặp lại nó.
             */
            logger.error(
                TAG,
                "Cannot quarantine lexeme " +
                        corrupt.lexemeId,
                failure,
            )
        }
    }

    private fun LearningProgress.toSessionQueueItem(
        sequence: Long,
    ): SessionQueueItem {

        val kind =
            when (state) {

                ReviewState.LEARNING,
                ReviewState.RELEARNING ->
                    SessionQueueKind
                        .SHORT_TERM

                ReviewState.REVIEW ->
                    SessionQueueKind
                        .REVIEW

                ReviewState.NEW ->
                    SessionQueueKind
                        .NEW
            }

        val dueAt =
            when (kind) {

                SessionQueueKind.NEW ->
                    null

                SessionQueueKind.SHORT_TERM,
                SessionQueueKind.REVIEW ->
                    requireNotNull(
                        nextReviewAt
                    ) {
                        "$state progress $lexemeId " +
                                "must have nextReviewAt"
                    }
            }

        return SessionQueueItem(
            lexemeId =
                lexemeId,

            kind =
                kind,

            dueAt =
                dueAt,

            sequence =
                sequence,
        )
    }

    class Factory(
        private val reviewRepository:
        ReviewRepository,

        private val submitReviewUseCase:
        SubmitReviewUseCase,

        private val launchPolicy:
        ReviewSessionLaunchPolicy,

        private val logger:
        AppLogger,
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel>
                create(
            modelClass: Class<T>,
        ): T {

            require(
                modelClass.isAssignableFrom(
                    ReviewSessionViewModel::
                    class.java
                )
            ) {
                "Unknown ViewModel class: " +
                        modelClass.name
            }

            return ReviewSessionViewModel(
                reviewRepository =
                    reviewRepository,

                submitReviewUseCase =
                    submitReviewUseCase,

                launchPolicy =
                    launchPolicy,

                logger =
                    logger,
            ) as T
        }
    }

    private companion object {
        const val TAG = "ReviewSession"
    }
}
