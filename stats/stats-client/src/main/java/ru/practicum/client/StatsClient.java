package ru.practicum.client;

public interface StatsClient {

    void recordView(Long userId, Long eventId);

    void recordRegister(Long userId, Long eventId);

    void recordLike(Long userId, Long eventId);
}
