package ru.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.model.Request;
import ru.practicum.model.RequestStatus;

import java.util.List;
import java.util.Optional;

public interface RequestRepository extends JpaRepository<Request, Long> {

    boolean existsByRequesterIdAndEventId(Long requesterId, Long eventId);

    List<Request> findByRequesterId(Long requesterId);

    List<Request> findByEventId(Long eventId);

    Optional<Request> findByIdAndRequesterId(Long id, Long requesterId);

    long countByEventIdAndStatus(Long eventId, RequestStatus status);

    List<Request> findByEventIdAndStatus(Long eventId, RequestStatus status);

    List<Request> findAllByRequesterId(Long requesterId);

    List<Request> findAllByEventId(Long eventId);
}