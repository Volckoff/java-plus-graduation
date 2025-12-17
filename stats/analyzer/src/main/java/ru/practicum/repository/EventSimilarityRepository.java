package ru.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.model.EventSimilarity;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface EventSimilarityRepository extends JpaRepository<EventSimilarity, Long> {

    Optional<EventSimilarity> findByAeventIdAndBeventId(Long aEventId, Long bEventId);

    @Query("SELECT es FROM EventSimilarity es WHERE es.aeventId = :id OR es.beventId = :id")
    List<EventSimilarity> findAllByEvent(@Param("id") Long eventId);

    @Query("SELECT es FROM EventSimilarity es " +
            " WHERE (es.aeventId = :id AND es.beventId in :ids) OR " +
            " (es.beventId = :id AND es.aeventId in :ids) " +
            " ORDER BY es.score DESC" +
            " LIMIT :limit")
    List<EventSimilarity> findAllByEventAndEventIdInLimitedTo(
            @Param("id") Long eventId,
            @Param("ids") List<Long> eventIds,
            @Param("limit") Long limit);

    @Query("SELECT es FROM EventSimilarity es " +
            " WHERE es.aeventId IN :eventIds OR es.beventId IN :eventIds " +
            " ORDER BY es.score DESC LIMIT :maxResults")
    List<EventSimilarity> findNPairContainsEventIdsSortedDescScore(@Param("eventIds") Set<Long> eventIds,
                                                                   @Param("maxResults") int maxResults);
}
