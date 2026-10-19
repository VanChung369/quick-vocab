package com.chung.lexis.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.chung.lexis.core.database.entity.DeckEntity
import com.chung.lexis.core.database.entity.DeckItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DeckDao {
    @Insert
    suspend fun insertDeck(
        deck: DeckEntity,
    ): Long

    @Update
    suspend fun updateDeck(
        deck: DeckEntity,
    ): Int

    @Query(
        """
            DELETE FROM decks 
            WHERE id = :deckId
        """
    )
    suspend fun deleteDeck(
        deckId: Long,
    )

    @Query(
        """
            SELECT * FROM decks
            WHERE id = :deckId
            LIMIT 1
        """
    )
    suspend fun getDeck(
        deckId: Long,
    ): DeckEntity?

    @Query(
        """
        SELECT *
        FROM decks
        ORDER BY updatedAtEpochMillis DESC
        """
    )
    fun observeDecks(): Flow<List<DeckEntity>>

    @Insert(
        onConflict = OnConflictStrategy.IGNORE,
    )
    suspend fun insertDeckItem(
        item: DeckItemEntity,
    ): Long

    @Query(
        """
        DELETE FROM deck_items
        WHERE deckId = :deckId
          AND lexemeId = :lexemeId
        """
    )
    suspend fun deleteDeckItem(
        deckId: Long,
        lexemeId: Long,
    )

    @Query(
        """
        SELECT *
        FROM deck_items
        WHERE deckId = :deckId
        ORDER BY sortOrder ASC, addedAtEpochMillis ASC
        """
    )
    fun observeDeckItems(
        deckId: Long,
    ): Flow<List<DeckItemEntity>>
}