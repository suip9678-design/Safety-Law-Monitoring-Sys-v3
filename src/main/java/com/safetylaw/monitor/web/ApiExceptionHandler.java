package com.safetylaw.monitor.web;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.safetylaw.monitor.lawapi.LawApiException;
import com.safetylaw.monitor.service.EmailService;
import com.safetylaw.monitor.service.LawService;
import com.safetylaw.monitor.service.MappingService;

/**
 * 오류 응답 형식.
 *
 * <p>기존 화면(frontend/app.js)이 오류 메시지를 {@code detail} 필드에서 읽으므로
 * 그 형식을 그대로 유지한다.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(NotFoundException e) {
        return detail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler({
            ConflictException.class,
            LawService.AlreadyTrackedException.class,
            MappingService.DuplicateMappingException.class})
    public ResponseEntity<Map<String, String>> handleConflict(RuntimeException e) {
        return detail(HttpStatus.CONFLICT, e.getMessage());
    }

    /** 외부 API 쪽 문제이므로 502 로 구분해 알린다. */
    @ExceptionHandler(LawApiException.class)
    public ResponseEntity<Map<String, String>> handleLawApi(LawApiException e) {
        log.warn("법령 API 호출 실패: {}", e.getMessage());
        return detail(HttpStatus.BAD_GATEWAY, e.getMessage());
    }

    @ExceptionHandler(EmailService.NotConfiguredException.class)
    public ResponseEntity<Map<String, String>> handleEmailNotConfigured(EmailService.NotConfiguredException e) {
        return detail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException e) {
        return detail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getDefaultMessage() == null
                        ? error.getField() + " 값이 올바르지 않습니다."
                        : error.getDefaultMessage())
                .findFirst()
                .orElse("요청 값이 올바르지 않습니다.");
        return detail(HttpStatus.BAD_REQUEST, message);
    }

    private ResponseEntity<Map<String, String>> detail(HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .body(Map.of("detail", message == null ? status.getReasonPhrase() : message));
    }
}
