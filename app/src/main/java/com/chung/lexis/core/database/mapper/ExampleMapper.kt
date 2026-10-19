package com.chung.lexis.core.database.mapper

import com.chung.lexis.core.database.entity.ExampleEntity
import com.chung.lexis.core.database.entity.ExampleTranslationEntity
import com.chung.lexis.domain.model.Example
import com.chung.lexis.domain.model.LocalizedText

fun ExampleEntity.toDomain(
    translation: ExampleTranslationEntity?,
): Example =
    Example(
        id = id,
        lexemeId = lexemeId,
        meaningId = meaningId,

        sentence = LocalizedText(
            text = sentenceText,
            languageTag = sentenceLanguageTag,
        ),

        translation = translation?.let {
            LocalizedText(
                text = it.text,
                languageTag = it.languageTag,
            )
        },
    )

fun Example.toEntity(): ExampleEntity =
    ExampleEntity(
        id = id,
        lexemeId = lexemeId,
        meaningId = meaningId,
        sentenceText = sentence.text,
        sentenceLanguageTag = sentence.languageTag,
    )


fun Example.toTranslationEntity(
    persistedExampleId: Long,
): ExampleTranslationEntity? =
    translation?.let {
        ExampleTranslationEntity(
            exampleId = persistedExampleId,
            text = it.text,
            languageTag = it.languageTag,
        )
    }