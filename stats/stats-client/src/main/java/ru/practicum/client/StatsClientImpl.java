package ru.practicum.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import ru.practicum.EndpointHitDto;
import ru.practicum.ViewStatsDto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Slf4j
public class StatsClientImpl implements StatsClient {

    private final RestClient restClient;
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public StatsClientImpl(@Value("${stats-server.url}") String statsServerUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(statsServerUrl)
                .defaultStatusHandler(HttpStatusCode::isError, (request, response) -> {
                    String errorMessage = "Ошибка при обращении к серверу статистики: " +
                            response.getStatusCode() + " " + response.getStatusText();
                    log.error(errorMessage);
                    if (response.getStatusCode().is4xxClientError()) {
                        throw new RestClientException("Ошибка запроса: " + errorMessage);
                    } else if (response.getStatusCode().is5xxServerError()) {
                        throw new RestClientException("Ошибка сервера: " + errorMessage);
                    } else {
                        throw new RestClientException(errorMessage);
                    }
                })
                .build();
    }

    @Override
    public void saveHit(EndpointHitDto endpointHitDto) {
        log.info("Отправка данных статистики: {}", endpointHitDto);
        restClient.post()
                .uri("/hit")
                .contentType(MediaType.APPLICATION_JSON)
                .body(endpointHitDto)
                .retrieve()
                .toBodilessEntity();
        log.info("Статистика добавлена");
    }

    @Override
    public List<ViewStatsDto> getStats(String start, String end, List<String> uris, Boolean unique) {
        log.info("Запрос статистики с параметрами start={}, end={}, uris={}, unique={}", start, end, uris, unique);

        LocalDateTime.parse(start, DATE_TIME_FORMATTER);
        LocalDateTime.parse(end, DATE_TIME_FORMATTER);

        List<ViewStatsDto> stats = restClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path("/stats")
                            .queryParam("start", start)
                            .queryParam("end", end);
                    if (uris != null && !uris.isEmpty()) {
                        for (String uri : uris) {
                            uriBuilder.queryParam("uris", uri);
                        }
                    }
                    if (unique != null) {
                        uriBuilder.queryParam("unique", unique);
                    }
                    return uriBuilder.build();
                })
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
        log.info("Статистика получена: {}", stats);
        return stats;
    }
}
