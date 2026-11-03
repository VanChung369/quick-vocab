package com.chung.lexis.core.database

import android.database.sqlite.SQLiteConstraintException
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LexisMigrationTest {

    @get:Rule
    val helper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            LexisDatabase::class.java,
        )

    @Test
    fun migrate1To2KeepsDataAndDetachesMeaningOfAnotherLexeme() {

        createVersion1WithWrongMeaningLinks()

        val db =
            helper.runMigrationsAndValidate(
                TEST_DB,
                2,
                true,
                MIGRATION_1_2,
            )

        /*
         * Link đúng giữ nguyên,
         * link sang meaning của banana bị bỏ.
         */
        assertEquals(
            mapOf(100L to 10L, 101L to null),
            db.meaningIds("pronunciations"),
        )

        assertEquals(
            mapOf(200L to 10L, 201L to null),
            db.meaningIds("examples"),
        )

        /*
         * Tạo lại bảng examples không được
         * cascade xoá translation.
         */
        assertEquals(
            1L,
            db.count("example_translations"),
        )

        assertEquals(
            0L,
            db.count("review_quarantine"),
        )

        db.close()
    }

    @Test
    fun migratedDatabaseEnforcesMeaningOfSameLexeme() {

        createVersion1WithWrongMeaningLinks()

        val db =
            helper.runMigrationsAndValidate(
                TEST_DB,
                2,
                true,
                MIGRATION_1_2,
            )

        db.execSQL("PRAGMA foreign_keys = ON")

        val rejected =
            runCatching {
                db.execSQL(
                    """
                    INSERT INTO pronunciations
                        (lexemeId, meaningId, type, value, languageTag)
                    VALUES (1, 20, 'IPA', '/x/', 'en')
                    """
                )
            }.exceptionOrNull()

        assertTrue(
            "Expected SQLiteConstraintException but was $rejected",
            rejected is SQLiteConstraintException
        )

        /*
         * Xoá meaning vẫn được:
         * pronunciation / example chỉ mất link.
         */
        db.execSQL("DELETE FROM meanings WHERE id = 10")

        assertEquals(
            mapOf(100L to null, 101L to null),
            db.meaningIds("pronunciations"),
        )

        assertEquals(
            mapOf(200L to null, 201L to null),
            db.meaningIds("examples"),
        )

        db.close()
    }

    /*
     * apple (1) có meaning 10, banana (2) có meaning 20.
     * Row 101 / 201 của apple trỏ nhầm sang meaning 20.
     */
    private fun createVersion1WithWrongMeaningLinks() {

        helper.createDatabase(TEST_DB, 1).apply {

            execSQL(
                """
                INSERT INTO lexemes
                    (id, text, normalizedText, languageTag, enrichmentStatus)
                VALUES
                    (1, 'apple', 'apple', 'en', 'READY'),
                    (2, 'banana', 'banana', 'en', 'READY')
                """
            )

            execSQL(
                """
                INSERT INTO meanings
                    (id, lexemeId, kind, languageTag, text, sortOrder)
                VALUES
                    (10, 1, 'TRANSLATION', 'vi', 'quả táo', 0),
                    (20, 2, 'TRANSLATION', 'vi', 'quả chuối', 0)
                """
            )

            execSQL(
                """
                INSERT INTO pronunciations
                    (id, lexemeId, meaningId, type, value, languageTag)
                VALUES
                    (100, 1, 10, 'IPA', '/ˈæp.əl/', 'en'),
                    (101, 1, 20, 'IPA', '/wrong/', 'en')
                """
            )

            execSQL(
                """
                INSERT INTO examples
                    (id, lexemeId, meaningId, sentenceText, sentenceLanguageTag)
                VALUES
                    (200, 1, 10, 'I eat an apple.', 'en'),
                    (201, 1, 20, 'Wrong link.', 'en')
                """
            )

            execSQL(
                """
                INSERT INTO example_translations
                    (exampleId, text, languageTag)
                VALUES
                    (200, 'Tôi ăn một quả táo.', 'vi')
                """
            )

            close()
        }
    }

    private fun SupportSQLiteDatabase.meaningIds(
        table: String,
    ): Map<Long, Long?> =
        query("SELECT id, meaningId FROM $table ORDER BY id").use { cursor ->

            buildMap {
                while (cursor.moveToNext()) {
                    put(
                        cursor.getLong(0),
                        if (cursor.isNull(1)) null else cursor.getLong(1),
                    )
                }
            }
        }

    private fun SupportSQLiteDatabase.count(
        table: String,
    ): Long =
        query("SELECT COUNT(*) FROM $table").use { cursor ->

            cursor.moveToFirst()

            cursor.getLong(0)
        }

    companion object {
        private const val TEST_DB =
            "migration-test"
    }
}
