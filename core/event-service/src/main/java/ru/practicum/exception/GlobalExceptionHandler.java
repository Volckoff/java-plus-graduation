package ru.practicum.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import feign.FeignException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
@Slf4j
@SuppressWarnings("unused")
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError handleNotFound(NotFoundException e) {
        log.warn("Сущность не найдена: {}", e.getMessage());
        return new ApiError(
                "NOT_FOUND",
                "Запрашиваемый объект не найден.",
                e.getMessage(),
                LocalDateTime.now()
        );
    }

    @ExceptionHandler(AlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleAlreadyExists(AlreadyExistsException e) {
        log.warn("Сущность уже существует: {}", e.getMessage());
        return new ApiError(
                "CONFLICT",
                "Нарушено ограничение целостности данных.",
                e.getMessage(),
                LocalDateTime.now()
        );
    }

    @ExceptionHandler(DeletedException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleDeletedException(DeletedException e) {
        log.warn("Entity restriction of removal - not empty");
        return new ApiError(
                "CONFLICT",
                "Нарушено ограничение целостности данных.",
                e.getMessage(),
                LocalDateTime.now()
        );
    }

    @ExceptionHandler(ConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleConflictException(ConflictException e) {
        log.warn("Конфликт: {}", e.getMessage());
        return new ApiError(
                "CONFLICT",
                "Нарушение бизнес-логики.",
                e.getMessage(),
                LocalDateTime.now()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        return new ApiError(
                "BAD_REQUEST",
                "Переданы некорректные данные.",
                message,
                LocalDateTime.now()
        );
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleMissingServletRequestParameterException(MissingServletRequestParameterException e) {
        String message = String.format("Отсутствует обязательный параметр: '%s' типа '%s'", 
                e.getParameterName(), e.getParameterType());
        log.warn("Отсутствует параметр запроса: {}", message);
        return new ApiError(
                "BAD_REQUEST",
                "Переданы некорректные данные.",
                message,
                LocalDateTime.now()
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleConstraintViolationException(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .findFirst()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .orElse("Ошибка валидации");
        log.warn("Ошибка валидации параметров: {}", message);
        return new ApiError(
                "BAD_REQUEST",
                "Переданы некорректные данные.",
                message,
                LocalDateTime.now()
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.warn("Ошибка чтения тела запроса: {}", e.getMessage());
        return new ApiError(
                "BAD_REQUEST",
                "Переданы некорректные данные.",
                "Некорректно составлен запрос.",
                LocalDateTime.now()
        );
    }

    @ExceptionHandler(FeignException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ApiError handleFeignException(FeignException e) {
        log.error("Ошибка при вызове внешнего сервиса: {}", e.getMessage());
        return new ApiError(
                "SERVICE_UNAVAILABLE",
                "Внешний сервис недоступен.",
                "Сервис временно недоступен. Попробуйте позже.",
                LocalDateTime.now()
        );
    }

    @ExceptionHandler(ArithmeticException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleArithmeticException(ArithmeticException e) {
        log.warn("Ошибка арифметической операции: {}", e.getMessage());
        return new ApiError(
                "BAD_REQUEST",
                "Переданы некорректные данные.",
                "Некорректные параметры пагинации.",
                LocalDateTime.now()
        );
    }

    @ExceptionHandler(NullPointerException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleNullPointerException(NullPointerException e) {
        log.warn("Ошибка: обращение к null объекту: {}", e.getMessage());
        return new ApiError(
                "BAD_REQUEST",
                "Переданы некорректные данные.",
                "Отсутствует обязательное поле.",
                LocalDateTime.now()
        );
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiError handleException(Exception e) {
        log.error("Внутренняя ошибка сервера", e);
        return new ApiError(
                "INTERNAL_ERROR",
                "Внутренняя ошибка сервера.",
                "Произошла непредвиденная ошибка. Администратор уже уведомлён.",
                LocalDateTime.now()
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("Некорректный аргумент: {}", e.getMessage());
        return new ApiError(
                "BAD_REQUEST",
                "Переданы некорректные данные.",
                e.getMessage(),
                LocalDateTime.now()
        );
    }
}
