package com.chung.lexis.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/*
 * v1 → v2:
 *
 * - pronunciations / examples thêm composite FK
 *   (meaningId, lexemeId) → meanings(id, lexemeId)
 * - bảng review_quarantine
 *
 * SQLite không ALTER được FK → tạo lại bảng
 * giống Room auto-migration. Migration chạy khi
 * foreign_keys còn OFF nên DROP TABLE examples
 * không cascade xoá example_translations.
 */
val MIGRATION_1_2 =
    object : Migration(1, 2) {

        override fun migrate(
            db: SupportSQLiteDatabase,
        ) {

            /*
             * Link tới meaning của lexeme khác không giữ
             * được dưới FK mới → bỏ link meaning,
             * vẫn giữ pronunciation / example.
             */
            db.execSQL(
                """
                UPDATE pronunciations
                SET meaningId = NULL
                WHERE meaningId IS NOT NULL
                  AND NOT EXISTS (
                      SELECT 1
                      FROM meanings AS m
                      WHERE m.id = pronunciations.meaningId
                        AND m.lexemeId = pronunciations.lexemeId
                  )
                """
            )

            db.execSQL(
                """
                UPDATE examples
                SET meaningId = NULL
                WHERE meaningId IS NOT NULL
                  AND NOT EXISTS (
                      SELECT 1
                      FROM meanings AS m
                      WHERE m.id = examples.meaningId
                        AND m.lexemeId = examples.lexemeId
                  )
                """
            )

            db.execSQL(
                """
                CREATE UNIQUE INDEX IF NOT EXISTS `index_meanings_id_lexemeId`
                ON `meanings` (`id`, `lexemeId`)
                """
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `_new_pronunciations` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `lexemeId` INTEGER NOT NULL,
                    `meaningId` INTEGER,
                    `type` TEXT NOT NULL,
                    `value` TEXT NOT NULL,
                    `languageTag` TEXT NOT NULL,
                    FOREIGN KEY(`lexemeId`) REFERENCES `lexemes`(`id`)
                        ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(`meaningId`) REFERENCES `meanings`(`id`)
                        ON UPDATE NO ACTION ON DELETE SET NULL,
                    FOREIGN KEY(`meaningId`, `lexemeId`) REFERENCES `meanings`(`id`, `lexemeId`)
                        ON UPDATE NO ACTION ON DELETE NO ACTION
                )
                """
            )

            db.execSQL(
                """
                INSERT INTO `_new_pronunciations` (
                    `id`, `lexemeId`, `meaningId`, `type`, `value`, `languageTag`
                )
                SELECT
                    `id`, `lexemeId`, `meaningId`, `type`, `value`, `languageTag`
                FROM `pronunciations`
                """
            )

            db.execSQL(
                """
                DROP TABLE `pronunciations`
                """
            )

            db.execSQL(
                """
                ALTER TABLE `_new_pronunciations` RENAME TO `pronunciations`
                """
            )

            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS `index_pronunciations_lexemeId`
                ON `pronunciations` (`lexemeId`)
                """
            )

            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS `index_pronunciations_meaningId_lexemeId`
                ON `pronunciations` (`meaningId`, `lexemeId`)
                """
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `_new_examples` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `lexemeId` INTEGER NOT NULL,
                    `meaningId` INTEGER,
                    `sentenceText` TEXT NOT NULL,
                    `sentenceLanguageTag` TEXT NOT NULL,
                    FOREIGN KEY(`lexemeId`) REFERENCES `lexemes`(`id`)
                        ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(`meaningId`) REFERENCES `meanings`(`id`)
                        ON UPDATE NO ACTION ON DELETE SET NULL,
                    FOREIGN KEY(`meaningId`, `lexemeId`) REFERENCES `meanings`(`id`, `lexemeId`)
                        ON UPDATE NO ACTION ON DELETE NO ACTION
                )
                """
            )

            db.execSQL(
                """
                INSERT INTO `_new_examples` (
                    `id`, `lexemeId`, `meaningId`, `sentenceText`, `sentenceLanguageTag`
                )
                SELECT
                    `id`, `lexemeId`, `meaningId`, `sentenceText`, `sentenceLanguageTag`
                FROM `examples`
                """
            )

            db.execSQL(
                """
                DROP TABLE `examples`
                """
            )

            db.execSQL(
                """
                ALTER TABLE `_new_examples` RENAME TO `examples`
                """
            )

            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS `index_examples_lexemeId`
                ON `examples` (`lexemeId`)
                """
            )

            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS `index_examples_meaningId_lexemeId`
                ON `examples` (`meaningId`, `lexemeId`)
                """
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `review_quarantine` (
                    `lexemeId` INTEGER NOT NULL,
                    `reason` TEXT NOT NULL,
                    `detectedAtEpochMillis` INTEGER NOT NULL,
                    PRIMARY KEY(`lexemeId`),
                    FOREIGN KEY(`lexemeId`) REFERENCES `lexemes`(`id`)
                        ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """
            )

            requireNoForeignKeyViolation(db, "pronunciations")
            requireNoForeignKeyViolation(db, "examples")
            requireNoForeignKeyViolation(db, "example_translations")
        }
    }

/*
 * foreign_keys OFF trong lúc migrate
 * → phải tự check sau khi tạo lại bảng.
 */
private fun requireNoForeignKeyViolation(
    db: SupportSQLiteDatabase,
    table: String,
) {
    db.query("PRAGMA foreign_key_check(`$table`)").use { cursor ->

        check(!cursor.moveToFirst()) {
            "Migration 1→2 left a foreign key violation in $table"
        }
    }
}
