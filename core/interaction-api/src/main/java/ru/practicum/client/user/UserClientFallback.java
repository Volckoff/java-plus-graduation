package ru.practicum.client.user;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.dto.user.UserDto;

import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class UserClientFallback implements UserOperation {

    @Override
    public UserDto getUserById(Long userId) {
        log.warn("User service is unavailable. Fallback: returning null for user ID: {}", userId);
        return null;
    }

    @Override
    public List<UserDto> getUsersByIds(List<Long> ids) {
        log.warn("User service is unavailable. Fallback: returning empty list for user IDs: {}", ids);
        return Collections.emptyList();
    }
}




