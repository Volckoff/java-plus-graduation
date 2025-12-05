package ru.practicum.dto.comment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;
import ru.practicum.model.CommentStatus;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CommentAdminDto {

    @NotBlank(message = "Коммент не может быть пустым")
    @Size(min = 1, max = 2000, message = "Текст комментария должен содержать от 1 до 2000 символов.")
    String text;

    @NotNull
    CommentStatus status;

}




