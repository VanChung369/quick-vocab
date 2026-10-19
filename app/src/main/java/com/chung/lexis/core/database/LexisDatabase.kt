package com.chung.lexis.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.chung.lexis.core.database.dao.DeckDao
import com.chung.lexis.core.database.dao.LexemeDao
import com.chung.lexis.core.database.dao.ReviewDao
import com.chung.lexis.core.database.entity.DeckEntity
import com.chung.lexis.core.database.entity.DeckItemEntity
import com.chung.lexis.core.database.entity.ExampleEntity
import com.chung.lexis.core.database.entity.ExampleTranslationEntity
import com.chung.lexis.core.database.entity.LearningProgressEntity
import com.chung.lexis.core.database.entity.LexemeEntity
import com.chung.lexis.core.database.entity.LexemeUserStateEntity
import com.chung.lexis.core.database.entity.MeaningEntity
import com.chung.lexis.core.database.entity.PronunciationEntity
import com.chung.lexis.core.database.entity.ReviewLogEntity

@Database(
    entities = [
        DeckEntity::class,
        DeckItemEntity::class,
        LexemeEntity::class,
        MeaningEntity::class,
        PronunciationEntity::class,
        ExampleEntity::class,
        ExampleTranslationEntity::class,
        LexemeUserStateEntity::class,
        LearningProgressEntity::class,
        ReviewLogEntity::class,
    ],
    version = 1,
    exportSchema = true,
)

abstract class LexisDatabase : RoomDatabase() {

    abstract fun deckDao(): DeckDao

    abstract fun lexemeDao(): LexemeDao

    abstract fun reviewDao(): ReviewDao
}