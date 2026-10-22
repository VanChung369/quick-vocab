package com.chung.lexis.core

import android.content.Context
import androidx.room.Room
import com.chung.lexis.core.database.LexisDatabase
import com.chung.lexis.core.database.repository.RoomLexemeRepository
import com.chung.lexis.core.database.repository.RoomReviewRepository
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

    val lexemeRepository:
            LexemeRepository

    val reviewRepository:
            ReviewRepository

    val submitReviewUseCase:
            SubmitReviewUseCase
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
        ).build()
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

    companion object {

        private const val DATABASE_NAME =
            "lexis.db"

        /*
         * Production learning policy.
         *
         * NEW:
         * GOOD lần đầu -> 1 phút
         * GOOD tiếp -> 10 phút
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
         */
        private val DEFAULT_RELEARNING_STEPS =
            listOf(
                Duration.ofMinutes(10),
            )
    }
}