package com.travelbudget.exception;

import com.travelbudget.dto.response.ErrorResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.stream.Collectors;

/**
 * Централизованная обработка ошибок для всех контроллеров.
 *
 * Наследуемся от ResponseEntityExceptionHandler — базовый класс Spring, который
 * УЖЕ корректно обрабатывает "фреймворковые" исключения (нет маршрута -> 404,
 * неверный метод -> 405, кривой JSON -> 400 и т.д.).
 *
 * Почему это важно: если просто повесить @ExceptionHandler(Exception.class) на всё,
 * он перехватит и эти внутренние исключения, превратив честный 404 в 500 (мы это словили).
 * Базовый класс разруливает их раньше нашего generic-обработчика.
 *
 * В Модуле 4 добавим TripNotFoundException и AccessDeniedException.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleEmailExists(EmailAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT) // 409
                .body(new ErrorResponse("EMAIL_EXISTS", ex.getMessage()));
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND) // 404
                .body(new ErrorResponse("USER_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(TripNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTripNotFound(TripNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND) // 404
                .body(new ErrorResponse("TRIP_NOT_FOUND", ex.getMessage()));
    }

    /**
     * Доступ к чужому ресурсу. Бросается в сервисном слое (TripService).
     * Наш @ExceptionHandler перехватывает исключение раньше, чем это сделал бы
     * механизм Spring Security, и отдаёт единый JSON-формат.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN) // 403
                .body(new ErrorResponse("FORBIDDEN", "Доступ запрещён"));
    }

    /**
     * Неверный email или пароль при логине (бросает AuthenticationManager).
     * Отдаём 401 и НЕ уточняем, что именно неверно — не подсказываем атакующему,
     * существует ли такой email.
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED) // 401
                .body(new ErrorResponse("INVALID_CREDENTIALS", "Неверный email или пароль"));
    }

    /**
     * Провал @Valid на DTO. Переопределяем метод базового класса, чтобы вернуть
     * НАШ формат ErrorResponse вместо стандартного ProblemDetail.
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining(", "));

        return ResponseEntity.badRequest() // 400
                .body(new ErrorResponse("VALIDATION_ERROR", message));
    }

    /**
     * Проблема при обращении к внешнему AI -> 502 Bad Gateway.
     * Сигнализирует клиенту, что сбой во внешнем сервисе, а не в его запросе.
     */
    @ExceptionHandler(AiIntegrationException.class)
    public ResponseEntity<ErrorResponse> handleAiError(AiIntegrationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY) // 502
                .body(new ErrorResponse("AI_ERROR", ex.getMessage()));
    }

    /**
     * Страховка от НЕОЖИДАННЫХ ошибок -> 500.
     * Фреймворковые 4xx сюда уже НЕ попадут (их разобрал базовый класс).
     * В проде здесь стоит логировать stack trace.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR) // 500
                .body(new ErrorResponse("INTERNAL_ERROR", "Внутренняя ошибка сервера"));
    }
}
