package ru.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.model.UserAction;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface UserActionRepository extends JpaRepository<UserAction, Long> {

    Optional<UserAction> findByUserIdAndEventId(Long userId, Long eventId);

    List<UserAction> findAllByUserId(Long userId);

    @Query("SELECT DISTINCT ua.eventId FROM UserAction ua WHERE ua.userId = :userId ORDER BY ua.created DESC LIMIT :maxResult")
    List<Long> findByUserIdOrderByCreatedDescLimitedTo(@Param("userId") Long userId, @Param("maxResult") int maxResult);

    List<UserAction> findAllByEventIdIn(Set<Long> eventIds);

    @Query("SELECT DISTINCT ua.eventId FROM UserAction ua WHERE ua.userId = :userId AND ua.eventId IN :otherEventId")
    List<Long> findEventIdsByUserIdAndEventIdIn(@Param("userId") long userId,
                                                @Param("otherEventId") Set<Long> otherEventId);
}
