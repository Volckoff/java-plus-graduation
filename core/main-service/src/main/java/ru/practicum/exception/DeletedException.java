package ru.practicum.exception;

public class DeletedException extends RuntimeException {

    public DeletedException(String message) {
        super(message);
    }

    public DeletedException(String entityName, String fieldName, Object value) {
        super(String.format("Нельзя удалить %s с %s = '%s' потому что связанные данные не пусты",
                entityName, fieldName, value));
    }
}
