package com.travelbudget.dto.response;

/**
 * Единый формат ошибки для всего API:
 *   {"code": "EMAIL_EXISTS", "message": "..."}
 * code — машиночитаемый (для фронта), message — человекочитаемый.
 */
public record ErrorResponse(
        String code,
        String message
) {
}
