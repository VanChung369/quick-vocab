package com.chung.lexis.domain.model

/*
 * Data đã lưu của một lexeme vi phạm invariant
 * (mapper / ReviewCard / scheduler require fail).
 *
 * Lỗi vĩnh viễn: retry không bao giờ thành công,
 * caller nên bỏ qua card thay vì retry.
 */
class CorruptReviewDataException(
    val lexemeId: Long,
    cause: Throwable,
) : RuntimeException(
    "Review data of lexeme $lexemeId is invalid",
    cause,
)
