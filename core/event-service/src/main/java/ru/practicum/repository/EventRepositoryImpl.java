package ru.practicum.repository;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import ru.practicum.model.Event;
import ru.practicum.model.QCategory;
import ru.practicum.model.QEvent;
import ru.practicum.model.EventState;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class EventRepositoryImpl implements EventRepositoryCustom {

    private final EntityManager entityManager;

    @Override
    public List<Event> findEventsByAdminFilters(List<Long> users,
                                                List<EventState> states,
                                                List<Long> categories,
                                                LocalDateTime rangeStart,
                                                LocalDateTime rangeEnd,
                                                int from,
                                                int size) {
        QEvent e = QEvent.event;
        QCategory c = QCategory.category;

        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);

        BooleanExpression byUsers = (users == null || users.isEmpty()) ? null : e.initiatorId.in(users);
        BooleanExpression byStates = (states == null || states.isEmpty()) ? null : e.state.in(states);
        BooleanExpression byCategories = (categories == null || categories.isEmpty()) ? null : e.category.id.in(categories);
        BooleanExpression afterStart = (rangeStart == null) ? null : e.eventDate.goe(rangeStart);
        BooleanExpression beforeEnd = (rangeEnd == null) ? null : e.eventDate.loe(rangeEnd);

        return queryFactory.selectFrom(e)
                .leftJoin(e.category, c).fetchJoin()
                .where(byUsers, byStates, byCategories, afterStart, beforeEnd)
                .orderBy(e.eventDate.desc())
                .offset(from)
                .limit(size)
                .fetch();
    }

    @Override
    public List<Event> findPublishedEvents(String text,
                                           List<Long> categories,
                                           Boolean paid,
                                           LocalDateTime rangeStart,
                                           LocalDateTime rangeEnd,
                                           Boolean onlyAvailable,
                                           String sort,
                                           int from,
                                           int size) {
        QEvent e = QEvent.event;
        QCategory c = QCategory.category;

        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);

        BooleanExpression statePub = e.state.eq(EventState.PUBLISHED);
        BooleanExpression byText = (text == null || text.isBlank()) ? null : e.annotation.containsIgnoreCase(text)
                .or(e.description.containsIgnoreCase(text));
        BooleanExpression byCategories = (categories == null || categories.isEmpty()) ? null : e.category.id.in(categories);
        BooleanExpression byPaid = paid == null ? null : e.paid.eq(paid);
        BooleanExpression afterStart = (rangeStart == null) ? e.eventDate.goe(LocalDateTime.now()) : e.eventDate.goe(rangeStart);
        BooleanExpression beforeEnd = (rangeEnd == null) ? null : e.eventDate.loe(rangeEnd);
        BooleanExpression available = null;
        if (Boolean.TRUE.equals(onlyAvailable)) {
            available = e.participantLimit.eq(0).or(e.confirmedRequests.lt(e.participantLimit));
        }
        OrderSpecifier<?> order = "VIEWS".equals(sort) ? e.views.desc() : e.eventDate.asc();

        return queryFactory.selectFrom(e)
                .leftJoin(e.category, c).fetchJoin()
                .where(statePub, byText, byCategories, byPaid, afterStart, beforeEnd, available)
                .orderBy(order)
                .offset(from)
                .limit(size)
                .fetch();
    }
}

