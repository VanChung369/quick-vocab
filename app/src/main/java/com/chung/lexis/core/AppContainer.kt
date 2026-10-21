package com.chung.lexis.core

import android.content.Context
import androidx.room.Room
import com.chung.lexis.core.database.LexisDatabase
import com.chung.lexis.core.database.repository.RoomLexemeRepository
import com.chung.lexis.core.database.repository.RoomReviewRepository
import com.chung.lexis.domain.repository.LexemeRepository
import com.chung.lexis.domain.repository.ReviewRepository

interface AppContainer {
    val lexemeRepository:
            LexemeRepository

    val reviewRepository:
            ReviewRepository
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

    companion object {

        private const val DATABASE_NAME =
            "lexis.db"
    }
}