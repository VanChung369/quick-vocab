package com.chung.lexis.core.database.repository

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.chung.lexis.core.database.LexisDatabase
import com.chung.lexis.core.database.entity.EnrichmentStatusColumn
import com.chung.lexis.core.database.entity.ExampleEntity
import com.chung.lexis.core.database.entity.ExampleTranslationEntity
import com.chung.lexis.core.database.entity.LearningProgressEntity
import com.chung.lexis.core.database.entity.LexemeEntity
import com.chung.lexis.core.database.entity.MeaningEntity
import com.chung.lexis.core.database.entity.MeaningKindColumn
import com.chung.lexis.core.database.entity.PronunciationEntity
import com.chung.lexis.core.database.entity.PronunciationTypeColumn
import com.chung.lexis.core.database.entity.ReviewStateColumn
import com.chung.lexis.domain.model.EnrichmentState
import com.chung.lexis.domain.model.LearningProgress
import com.chung.lexis.domain.model.LocalizedText
import com.chung.lexis.domain.model.MeaningKind
import com.chung.lexis.domain.model.PronunciationType
import com.chung.lexis.domain.model.ReviewState
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class RoomReviewCardTest {

    private lateinit var database: LexisDatabase
    private lateinit var repository: RoomReviewRepository

    private val now =
        Instant.parse("2026-10-21T12:00:00Z")

    @Before
    fun setUp() {
        val context =
            InstrumentationRegistry
                .getInstrumentation()
                .targetContext

        database =
            Room.inMemoryDatabaseBuilder(
                context,
                LexisDatabase::class.java,
            )
                .build()

        repository =
            RoomReviewRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun missingLexemeReturnsNull() =
        runBlocking {

            val card =
                repository.getReviewCard(
                    lexemeId = 999L,
                )

            assertNull(card)
        }

    @Test
    fun cardContainsAllContentOfLexeme() =
        runBlocking {

            val lexemeId =
                insertLexeme("apple")

            /*
             * Lexeme khác cùng có content.
             * Card không được lẫn data của nó.
             */
            val otherId =
                insertLexeme("banana")

            /*
             * Insert ngược sortOrder để kiểm tra
             * card được sort theo sortOrder,
             * không theo id.
             */
            val meaningIds =
                database
                    .lexemeDao()
                    .insertMeanings(
                        listOf(
                            meaning(
                                lexemeId = lexemeId,
                                text = "a round fruit",
                                kind =
                                    MeaningKindColumn.DEFINITION,
                                languageTag = LANGUAGE_TAG,
                                sortOrder = 1,
                            ),

                            meaning(
                                lexemeId = lexemeId,
                                text = "quả táo",
                                kind =
                                    MeaningKindColumn.TRANSLATION,
                                languageTag = "vi",
                                sortOrder = 0,
                            ),

                            meaning(
                                lexemeId = otherId,
                                text = "quả chuối",
                                kind =
                                    MeaningKindColumn.TRANSLATION,
                                languageTag = "vi",
                                sortOrder = 0,
                            ),
                        )
                    )

            val translationMeaningId =
                meaningIds[1]

            database
                .lexemeDao()
                .insertPronunciations(
                    listOf(
                        PronunciationEntity(
                            lexemeId = lexemeId,
                            type =
                                PronunciationTypeColumn.IPA,
                            value = "/ˈæp.əl/",
                            languageTag = LANGUAGE_TAG,
                        ),

                        PronunciationEntity(
                            lexemeId = otherId,
                            type =
                                PronunciationTypeColumn.IPA,
                            value = "/bəˈnɑː.nə/",
                            languageTag = LANGUAGE_TAG,
                        ),
                    )
                )

            val translatedExampleId =
                insertExample(
                    lexemeId = lexemeId,
                    meaningId = translationMeaningId,
                    sentence = "I eat an apple.",
                )

            database
                .lexemeDao()
                .upsertExampleTranslation(
                    ExampleTranslationEntity(
                        exampleId = translatedExampleId,
                        text = "Tôi ăn một quả táo.",
                        languageTag = "vi",
                    )
                )

            /*
             * Example chưa có translation.
             */
            insertExample(
                lexemeId = lexemeId,
                sentence = "Apples are red.",
            )

            insertExample(
                lexemeId = otherId,
                sentence = "Bananas are yellow.",
            )

            val nextReviewAt =
                now.plusSeconds(600)

            database
                .reviewDao()
                .upsertProgress(
                    LearningProgressEntity(
                        lexemeId = lexemeId,
                        state =
                            ReviewStateColumn.LEARNING,
                        step = 1,
                        stability = 2.5,
                        difficulty = 5.0,
                        reviewCount = 2,
                        lapseCount = 0,
                        scheduledDays = 0,
                        lastReviewedAtEpochMillis =
                            now.toEpochMilli(),
                        nextReviewAtEpochMillis =
                            nextReviewAt.toEpochMilli(),
                    )
                )

            val card =
                repository.getReviewCard(
                    lexemeId = lexemeId,
                )

            assertNotNull(card)
            card!!

            assertEquals(
                lexemeId,
                card.lexeme.id,
            )

            assertEquals(
                "apple",
                card.lexeme.text,
            )

            assertEquals(
                EnrichmentState.Ready,
                card.lexeme.enrichmentState,
            )

            /*
             * Meanings
             */
            assertEquals(
                listOf(
                    "quả táo",
                    "a round fruit",
                ),
                card.meanings.map { it.text },
            )

            assertEquals(
                listOf(
                    MeaningKind.TRANSLATION,
                    MeaningKind.DEFINITION,
                ),
                card.meanings.map { it.kind },
            )

            /*
             * Pronunciations
             */
            assertEquals(
                1,
                card.pronunciations.size,
            )

            assertEquals(
                PronunciationType.IPA,
                card.pronunciations.single().type,
            )

            assertEquals(
                "/ˈæp.əl/",
                card.pronunciations.single().value,
            )

            /*
             * Examples
             */
            assertEquals(
                2,
                card.examples.size,
            )

            val translatedExample =
                card.examples.first()

            assertEquals(
                LocalizedText(
                    text = "I eat an apple.",
                    languageTag = LANGUAGE_TAG,
                ),
                translatedExample.sentence,
            )

            assertEquals(
                translationMeaningId,
                translatedExample.meaningId,
            )

            assertEquals(
                LocalizedText(
                    text = "Tôi ăn một quả táo.",
                    languageTag = "vi",
                ),
                translatedExample.translation,
            )

            val untranslatedExample =
                card.examples.last()

            assertEquals(
                "Apples are red.",
                untranslatedExample.sentence.text,
            )

            assertNull(
                untranslatedExample.translation
            )

            /*
             * Progress
             */
            assertEquals(
                LearningProgress(
                    lexemeId = lexemeId,
                    state = ReviewState.LEARNING,
                    step = 1,
                    stability = 2.5,
                    difficulty = 5.0,
                    reviewCount = 2,
                    lapseCount = 0,
                    scheduledDays = 0,
                    lastReviewedAt = now,
                    nextReviewAt = nextReviewAt,
                ),
                card.progress,
            )
        }

    @Test
    fun missingProgressIsRepresentedAsNew() =
        runBlocking {

            val lexemeId =
                insertLexeme("apple")

            val card =
                repository.getReviewCard(
                    lexemeId = lexemeId,
                )

            assertNotNull(card)
            card!!

            assertEquals(
                LearningProgress(
                    lexemeId = lexemeId,
                ),
                card.progress,
            )

            assertTrue(
                card.meanings.isEmpty()
            )

            assertTrue(
                card.pronunciations.isEmpty()
            )

            assertTrue(
                card.examples.isEmpty()
            )
        }

    @Test
    fun invalidFailedLexemeThrows() =
        runBlocking {

            /*
             * FAILED nhưng thiếu failureMessage
             * và failedAt -> mapper phải fail.
             */
            val lexemeId =
                insertLexeme(
                    text = "broken",
                    enrichmentStatus =
                        EnrichmentStatusColumn.FAILED,
                )

            val result =
                runCatching {
                    repository.getReviewCard(
                        lexemeId = lexemeId,
                    )
                }

            assertTrue(
                result.exceptionOrNull() is
                        IllegalStateException
            )
        }

    private suspend fun insertLexeme(
        text: String,
        enrichmentStatus:
        EnrichmentStatusColumn =
            EnrichmentStatusColumn.READY,
    ): Long =
        database
            .lexemeDao()
            .insertLexeme(
                LexemeEntity(
                    text = text,

                    normalizedText =
                        text.lowercase(),

                    languageTag =
                        LANGUAGE_TAG,

                    enrichmentStatus =
                        enrichmentStatus,
                )
            )

    private fun meaning(
        lexemeId: Long,
        text: String,
        kind: MeaningKindColumn,
        languageTag: String,
        sortOrder: Int,
    ): MeaningEntity =
        MeaningEntity(
            lexemeId = lexemeId,
            kind = kind,
            languageTag = languageTag,
            text = text,
            sortOrder = sortOrder,
        )

    private suspend fun insertExample(
        lexemeId: Long,
        sentence: String,
        meaningId: Long? = null,
    ): Long =
        database
            .lexemeDao()
            .insertExample(
                ExampleEntity(
                    lexemeId = lexemeId,
                    meaningId = meaningId,
                    sentenceText = sentence,
                    sentenceLanguageTag =
                        LANGUAGE_TAG,
                )
            )

    companion object {
        private const val LANGUAGE_TAG =
            "en"
    }
}
