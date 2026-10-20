package com.chung.lexis.domain.repository

import com.chung.lexis.domain.model.Lexeme
import java.time.Instant

interface LexemeRepository {

    suspend fun addLexemeToDeck(
        deckId: Long,
        candidate: Lexeme,
        addedAt: Instant,
        sortOrder: Int = 0,
    ): AddLexemeToDeckResult
}

sealed interface AddLexemeToDeckResult {

    data class Added(
        val lexeme: Lexeme,
        val createdLexeme: Boolean,
    ) : AddLexemeToDeckResult

    data class AlreadyInDeck(
        val lexeme: Lexeme,
    ) : AddLexemeToDeckResult

    data object DeckNotFound : AddLexemeToDeckResult

    data class LanguageMismatch(
        val deckLanguageTag: String,
        val lexemeLanguageTag: String,
    ) : AddLexemeToDeckResult
}