package com.chung.lexis.domain.model

data class ReviewCard(
    val lexeme: Lexeme,
    val meanings: List<Meaning>,
    val pronunciations: List<Pronunciation>,
    val examples: List<Example>,
    val progress: LearningProgress,
) {

    init {

        require(
            progress.lexemeId ==
                    lexeme.id
        ) {
            "Progress must belong to lexeme ${lexeme.id}"
        }

        require(
            meanings.all {
                it.lexemeId == lexeme.id
            }
        ) {
            "All meanings must belong to lexeme ${lexeme.id}"
        }

        require(
            pronunciations.all {
                it.lexemeId == lexeme.id
            }
        ) {
            "All pronunciations must belong to lexeme ${lexeme.id}"
        }

        require(
            examples.all {
                it.lexemeId == lexeme.id
            }
        ) {
            "All examples must belong to lexeme ${lexeme.id}"
        }

        val meaningIds =
            meanings
                .mapTo(
                    mutableSetOf()
                ) {
                    it.id
                }

        require(
            pronunciations.all { pronunciation ->

                pronunciation.meaningId == null ||
                        pronunciation.meaningId in
                        meaningIds
            }
        ) {
            "Pronunciation meaningId must belong to lexeme ${lexeme.id}"
        }

        require(
            examples.all { example ->

                example.meaningId == null ||
                        example.meaningId in
                        meaningIds
            }
        ) {
            "Example meaningId must belong to lexeme ${lexeme.id}"
        }
    }
}