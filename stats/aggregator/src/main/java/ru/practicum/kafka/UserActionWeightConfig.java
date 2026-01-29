package ru.practicum.kafka;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties("aggregator.user-action-weight")
public class UserActionWeightConfig {

    private double view;
    private double register;
    private double like;
}





