package ru.practicum.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Location {
    Float lat;
    Float lon;
}
