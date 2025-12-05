package ru.practicum.client.user;

import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.dto.user.UserDto;

import java.util.List;

public interface UserOperation {

    @GetMapping("/admin/users/{userId}")
    UserDto getUserById(@PathVariable @NotNull Long userId);

    @GetMapping("/admin/users/by-ids")
    List<UserDto> getUsersByIds(@RequestParam(name = "ids", required = false) List<Long> ids);
}




