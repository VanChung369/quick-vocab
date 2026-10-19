package com.chung.lexis.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import com.chung.lexis.core.database.entity.ExampleEntity
import com.chung.lexis.core.database.entity.ExampleTranslationEntity
import com.chung.lexis.core.database.entity.LexemeEntity
import com.chung.lexis.core.database.entity.LexemeUserStateEntity
import com.chung.lexis.core.database.entity.MeaningEntity
import com.chung.lexis.core.database.entity.PronunciationEntity
import com.chung.lexis.core.database.relation.ExampleWithTranslation

@Dao
interface LexemeDao {

    @Insert(
        onConflict = OnConflictStrategy.IGNORE,
    )
    suspend fun insertLexeme(
        lexeme: LexemeEntity,
    ): Long


    @Update
    suspend fun updateLexeme(
        lexeme: LexemeEntity,
    ): Int

    @Query(
        """
        SELECT *
        FROM lexemes
        WHERE id = :lexemeId
        LIMIT 1
        """
    )
    suspend fun getLexeme(
        lexemeId: Long,
    ): LexemeEntity?


    @Query(
        """
        SELECT *
        FROM lexemes
        WHERE normalizedText = :normalizedText
          AND languageTag = :languageTag
        LIMIT 1
        """
    )
    suspend fun findLexeme(
        normalizedText: String,
        languageTag: String,
    ): LexemeEntity?


    @Query(
        """
        DELETE FROM lexemes
        WHERE id = :lexemeId
        """
    )
    suspend fun deleteLexeme(
        lexemeId: Long,
    )

    @Insert
    suspend fun insertMeanings(
        meanings: List<MeaningEntity>,
    ): List<Long>

    @Query(
        """
        SELECT *
        FROM meanings
        WHERE lexemeId = :lexemeId
        ORDER BY sortOrder ASC, id ASC
        """
    )
    suspend fun getMeanings(
        lexemeId: Long,
    ): List<MeaningEntity>

    @Query(
        """
        DELETE FROM meanings
        WHERE lexemeId = :lexemeId
        """
    )
    suspend fun deleteMeanings(
        lexemeId: Long,
    )

    @Query(
        """
        SELECT EXISTS(
            SELECT 1
            FROM meanings
            WHERE id = :meaningId
              AND lexemeId = :lexemeId
        )
        """
    )
    suspend fun meaningBelongsToLexeme(
        meaningId: Long,
        lexemeId: Long,
    ): Boolean

    @Insert
    suspend fun insertPronunciations(
        pronunciations: List<PronunciationEntity>,
    ): List<Long>

    @Query(
        """
        SELECT *
        FROM pronunciations
        WHERE lexemeId = :lexemeId
        ORDER BY id ASC
        """
    )
    suspend fun getPronunciations(
        lexemeId: Long,
    ): List<PronunciationEntity>

    @Query(
        """
        DELETE FROM pronunciations
        WHERE lexemeId = :lexemeId
        """
    )
    suspend fun deletePronunciations(
        lexemeId: Long,
    )

    @Insert
    suspend fun insertExample(
        example: ExampleEntity,
    ): Long

    @Update
    suspend fun updateExample(
        example: ExampleEntity,
    ): Int

    @Query(
        """
        DELETE FROM examples
        WHERE id = :exampleId
        """
    )
    suspend fun deleteExample(
        exampleId: Long,
    )

    @Transaction
    @Query(
        """
        SELECT *
        FROM examples
        WHERE lexemeId = :lexemeId
        ORDER BY id ASC
        """
    )
    suspend fun getExamples(
        lexemeId: Long,
    ): List<ExampleWithTranslation>

    @Upsert
    suspend fun upsertExampleTranslation(
        translation: ExampleTranslationEntity,
    )

    @Query(
        """
        DELETE FROM example_translations
        WHERE exampleId = :exampleId
        """
    )
    suspend fun deleteExampleTranslation(
        exampleId: Long,
    )

    @Upsert
    suspend fun upsertUserState(
        state: LexemeUserStateEntity,
    )

    @Query(
        """
        SELECT *
        FROM lexeme_user_states
        WHERE lexemeId = :lexemeId
        LIMIT 1
        """
    )
    suspend fun getUserState(
        lexemeId: Long,
    ): LexemeUserStateEntity?
}