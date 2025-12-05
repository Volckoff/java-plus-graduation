package ru.practicum.client.user;

import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "user-service", fallback = UserClientFallback.class)
public interface UserClient extends UserOperation {
}




