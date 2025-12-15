package ru.practicum.client;

import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import ru.practicum.client.mapper.UserActionMapper;
import ru.practicum.grpc.stats.action.ActionTypeProto;
import ru.practicum.grpc.stats.action.UserActionProto;
import ru.practicum.grpc.stats.collector.UserActionControllerGrpc;

import java.time.Instant;

@Slf4j
@Component
public class StatsClientImpl implements StatsClient {

    @GrpcClient("collector")
    private UserActionControllerGrpc.UserActionControllerBlockingStub userActionStub;

    @Override
    @Retryable(
            retryFor = {StatusRuntimeException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 3000)
    )
    public void recordView(Long userId, Long eventId) {
        sendAction(userId, eventId, ActionTypeProto.ACTION_VIEW);
    }

    @Override
    @Retryable(
            retryFor = {StatusRuntimeException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 3000)
    )
    public void recordRegister(Long userId, Long eventId) {
        sendAction(userId, eventId, ActionTypeProto.ACTION_REGISTER);
    }

    @Override
    @Retryable(
            retryFor = {StatusRuntimeException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 3000)
    )
    public void recordLike(Long userId, Long eventId) {
        sendAction(userId, eventId, ActionTypeProto.ACTION_LIKE);
    }

    private void sendAction(Long userId, Long eventId, ActionTypeProto actionType) {
        try {
            UserActionProto userAction = UserActionMapper.toProto(userId, eventId, actionType, Instant.now());
            userActionStub.collectUserAction(userAction);
            log.info("Действие пользователя успешно отправлено: userId={}, eventId={}, actionType={}",
                    userId, eventId, actionType);
        } catch (StatusRuntimeException e) {
            log.error("Не удалось отправить действие пользователя. Статус: {}, сообщение: {}",
                    e.getStatus(), e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("Не удалось отправить действие пользователя. Исключение: {}, сообщение: {}",
                    e.getClass().getName(), e.getMessage(), e);
            throw new RuntimeException("Ошибка при отправке действия пользователя", e);
        }
    }
}
