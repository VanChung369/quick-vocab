package com.chung.lexis.core

import android.content.Context
import androidx.room.Room
import com.chung.lexis.core.database.LexisDatabase
import com.chung.lexis.core.database.MIGRATION_1_2
import com.chung.lexis.core.database.repository.RoomLexemeRepository
import com.chung.lexis.core.database.repository.RoomReviewRepository
import com.chung.lexis.domain.model.ReviewSessionLaunchPolicy
import com.chung.lexis.domain.repository.LexemeRepository
import com.chung.lexis.domain.repository.ReviewRepository
import com.chung.lexis.domain.scheduler.DefaultReviewScheduler
import com.chung.lexis.domain.scheduler.Fsrs6MemoryPolicy
import com.chung.lexis.domain.scheduler.LearningStepPolicy
import com.chung.lexis.domain.scheduler.ReviewScheduler
import com.chung.lexis.domain.scheduler.ReviewStateMachine
import com.chung.lexis.domain.usecase.SubmitReviewUseCase
import java.time.Duration

interface AppContainer {

    val lexemeRepository: LexemeRepository

    val reviewRepository: ReviewRepository

    val submitReviewUseCase: SubmitReviewUseCase

    val reviewSessionLaunchPolicy: ReviewSessionLaunchPolicy
}

class DefaultAppContainer(
    context: Context,
) : AppContainer {

    private val applicationContext = context.applicationContext

    private val database: LexisDatabase by lazy {
        Room.databaseBuilder(
            applicationContext,
            LexisDatabase::class.java,
            DATABASE_NAME,
        )
            .addMigrations(MIGRATION_1_2)
            .build()
    }

    override val lexemeRepository:
            LexemeRepository by lazy {

        RoomLexemeRepository(
            database = database,
        )
    }

    override val reviewRepository:
            ReviewRepository by lazy {

        RoomReviewRepository(
            database = database,
        )
    }

    private val reviewScheduler:
            ReviewScheduler by lazy {

        DefaultReviewScheduler(
            stateMachine =
                ReviewStateMachine(),

            stepPolicy =
                LearningStepPolicy(
                    learningSteps =
                        DEFAULT_LEARNING_STEPS,

                    relearningSteps =
                        DEFAULT_RELEARNING_STEPS,
                ),

            memoryPolicy =
                Fsrs6MemoryPolicy(),
        )
    }

    override val submitReviewUseCase:
            SubmitReviewUseCase by lazy {

        SubmitReviewUseCase(
            reviewRepository =
                reviewRepository,

            reviewScheduler =
                reviewScheduler,
        )
    }

    override val reviewSessionLaunchPolicy =
        ReviewSessionLaunchPolicy(
            dueLimit =
                Int.MAX_VALUE,

            newLimit =
                DEFAULT_NEW_CARDS_PER_DAY,

            learnAheadLimit =
                DEFAULT_LEARN_AHEAD_LIMIT,
        )

    companion object {

        private const val DATABASE_NAME =
            "lexis.db"

        private const val DEFAULT_NEW_CARDS_PER_DAY =
            20

        /*
         * Giống mặc định của Anki.
         *
         * Lớn hơn mọi learning/relearning step bên dưới
         * → cuối session không phải chờ step 1m/10m.
         */
        private val DEFAULT_LEARN_AHEAD_LIMIT =
            Duration.ofMinutes(20)

        /*
         * Production learning policy.
         *
         * NEW:
         * AGAIN -> 1 phút
         * HARD -> 5,5 phút
         * GOOD -> 10 phút
         * GOOD tiếp -> REVIEW
         */
        private val DEFAULT_LEARNING_STEPS =
            listOf(
                Duration.ofMinutes(1),
                Duration.ofMinutes(10),
            )

        /*
         * REVIEW + AGAIN
         * -> RELEARNING
         * -> 10 phút
         *
         * RELEARNING + HARD -> 15 phút
         */
        private val DEFAULT_RELEARNING_STEPS =
            listOf(
                Duration.ofMinutes(10),
            )
    }
}