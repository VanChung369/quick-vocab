package com.chung.lexis.core.database.repository

import androidx.room.withTransaction
import com.chung.lexis.core.database.LexisDatabase
import com.chung.lexis.core.database.mapper.toDomainResult
import com.chung.lexis.core.database.mapper.toEntity
import com.chung.lexis.domain.model.DeckItem
import com.chung.lexis.domain.model.Lexeme
import com.chung.lexis.domain.repository.AddLexemeToDeckResult
import com.chung.lexis.domain.repository.LexemeRepository
import java.time.Instant

class RoomLexemeRepository(
    private val database: LexisDatabase,
) : LexemeRepository {

    private val deckDao = database.deckDao()
    private val lexemeDao = database.lexemeDao()

    override suspend fun addLexemeToDeck(
        deckId: Long,
        candidate: Lexeme,
        addedAt: Instant,
        sortOrder: Int,
    ): AddLexemeToDeckResult =
        database.withTransaction {

            require(candidate.id == 0L) {
                "Candidate lexeme must not already have an id"
            }

            require(sortOrder >= 0) {
                "sortOrder must be >= 0"
            }

            val deck = deckDao.getDeck(deckId)
                ?: return@withTransaction AddLexemeToDeckResult.DeckNotFound

            if (
                deck.learningLanguageTag !=
                candidate.languageTag
            ) {
                return@withTransaction AddLexemeToDeckResult.LanguageMismatch(
                    deckLanguageTag =
                        deck.learningLanguageTag,
                    lexemeLanguageTag =
                        candidate.languageTag,
                )
            }

            val candidateEntity =
                candidate
                    .toEntity()
                    .copy(id = 0L)

            val insertedLexemeId =
                lexemeDao.insertLexeme(
                    candidateEntity
                )

            val createdLexeme =
                insertedLexemeId != INSERT_IGNORED

            val persistedEntity =
                if (createdLexeme) {

                    lexemeDao.getLexeme(
                        insertedLexemeId
                    ) ?: error(
                        "Inserted lexeme " +
                                "$insertedLexemeId not found"
                    )

                } else {

                    lexemeDao.findLexeme(
                        normalizedText =
                            candidate.normalizedText,
                        languageTag =
                            candidate.languageTag,
                    ) ?: error(
                        "Lexeme insert was ignored " +
                                "but existing lexeme " +
                                "could not be found"
                    )
                }

            val persistedLexeme =
                persistedEntity
                    .toDomainResult()
                    .getOrElse { cause ->
                        throw IllegalStateException(
                            "Cannot map persisted " +
                                    "lexeme ${persistedEntity.id}",
                            cause,
                        )
                    }

            val deckItem =
                DeckItem(
                    deckId = deckId,
                    lexemeId = persistedLexeme.id,
                    addedAt = addedAt,
                    sortOrder = sortOrder,
                ).toEntity(
                    languageTag =
                        persistedLexeme.languageTag,
                )

            val insertedDeckItem =
                deckDao.insertDeckItem(
                    deckItem
                )

            if (insertedDeckItem == INSERT_IGNORED) {

                AddLexemeToDeckResult.AlreadyInDeck(
                    lexeme = persistedLexeme,
                )

            } else {

                AddLexemeToDeckResult.Added(
                    lexeme = persistedLexeme,
                    createdLexeme = createdLexeme,
                )
            }
        }

    private companion object {
        const val INSERT_IGNORED = -1L
    }
}