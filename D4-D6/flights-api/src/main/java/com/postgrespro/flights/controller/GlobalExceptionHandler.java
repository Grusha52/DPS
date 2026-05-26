package com.postgrespro.flights.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    // 1. Ошибка в типах -> 400
    @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> handleTypeMismatch(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException e) {
        return new ResponseEntity<>(Map.of(
                "error", "Неверный формат параметра: " + e.getName(),
                "details", "Значение '" + e.getValue() + "' не может быть приведено к типу " + e.getRequiredType().getSimpleName()
        ), HttpStatus.BAD_REQUEST);
    }

    // 2. Пропущен обязательный параметр (@RequestParam) -> 400
    @ExceptionHandler(org.springframework.web.bind.MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, String>> handleMissingParam(org.springframework.web.bind.MissingServletRequestParameterException e) {
        return new ResponseEntity<>(Map.of(
                "error", "Отсутствует обязательный параметр: " + e.getParameterName()
        ), HttpStatus.BAD_REQUEST);
    }

    // 3. "Catch-all" для реальных падений сервера -> 500
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleException(Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        return new ResponseEntity<>(Map.of(
                "error", "Внутренняя ошибка сервера",
                "trace", sw.toString()
        ), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}