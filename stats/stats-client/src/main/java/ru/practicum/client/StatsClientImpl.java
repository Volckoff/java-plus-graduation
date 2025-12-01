package ru.practicum.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.MaxAttemptsRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriComponentsBuilder;
import ru.practicum.EndpointHitDto;
import ru.practicum.ViewStatsDto;

import java.net.URI;
import java.util.List;

@Slf4j
@Component
public class StatsClientImpl implements StatsClient {

    private final RestTemplate restTemplate;
    private final DiscoveryClient discoveryClient;
    private final RetryTemplate retryTemplate;
    private final String statsServiceId;

    @Autowired
    public StatsClientImpl(DiscoveryClient discoveryClient,
                          @Value("${discovery.services.stats-server-id:stats-server}") String statsServiceId,
                          RestTemplateBuilder builder) {
        this.discoveryClient = discoveryClient;
        this.statsServiceId = statsServiceId;
        this.restTemplate = builder
                .uriTemplateHandler(new DefaultUriBuilderFactory(""))
                .requestFactory(() -> new HttpComponentsClientHttpRequestFactory())
                .build();
        
        this.retryTemplate = new RetryTemplate();
        FixedBackOffPolicy fixedBackOffPolicy = new FixedBackOffPolicy();
        fixedBackOffPolicy.setBackOffPeriod(3000L);
        retryTemplate.setBackOffPolicy(fixedBackOffPolicy);
        
        MaxAttemptsRetryPolicy retryPolicy = new MaxAttemptsRetryPolicy();
        retryPolicy.setMaxAttempts(3);
        retryTemplate.setRetryPolicy(retryPolicy);
    }

    @Override
    public void saveHit(EndpointHitDto endpointHitDto) {
        HttpEntity<EndpointHitDto> requestEntity = new HttpEntity<>(endpointHitDto, defaultHeaders());
        try {
            restTemplate.exchange(makeUri("/hit"), HttpMethod.POST, requestEntity, Object.class);
            log.info("Статистика успешно отправлена: {}", endpointHitDto);
        } catch (HttpStatusCodeException e) {
            log.error("Не удалось отправить хит статистики. Код ошибки: {}, сообщение: {}", 
                    e.getStatusCode(), e.getMessage(), e);
        } catch (Exception e) {
            log.error("Не удалось отправить хит статистики. Исключение: {}, сообщение: {}", 
                    e.getClass().getName(), e.getMessage(), e);
        }
    }

    @Override
    public List<ViewStatsDto> getStats(String start, String end, List<String> uris, Boolean unique) {
        if (!checkValidParams(start, end, uris)) {
            log.error("Не удалось получить статистику из-за некорректных параметров: start={}, end={}, uris={}", 
                    start, end, uris);
            return List.of();
        }

        UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromPath("/stats")
                .queryParam("start", start)
                .queryParam("end", end);

        if (uris != null && !uris.isEmpty()) {
            for (String uri : uris) {
                uriComponentsBuilder.queryParam("uris", uri);
            }
        }

        if (unique != null) {
            uriComponentsBuilder.queryParam("unique", unique);
        }

        String uri = uriComponentsBuilder.build(false)
                .encode()
                .toUriString();

        HttpEntity<String> requestEntity = new HttpEntity<>(defaultHeaders());

        ResponseEntity<ViewStatsDto[]> statServerResponse;
        try {
            statServerResponse = restTemplate.exchange(makeUri(uri), HttpMethod.GET, requestEntity, ViewStatsDto[].class);
            log.info("Статистика успешно получена");
        } catch (HttpStatusCodeException e) {
            log.error("Не удалось получить статистику. Код ошибки: {}, сообщение: {}", 
                    e.getStatusCode(), e.getMessage(), e);
            return List.of();
        } catch (Exception e) {
            log.error("Не удалось получить статистику. Исключение: {}, сообщение: {}", 
                    e.getClass().getName(), e.getMessage(), e);
            return List.of();
        }

        ViewStatsDto[] body = statServerResponse.getBody();
        return body != null ? List.of(body) : List.of();
    }

    private boolean checkValidParams(String start, String end, List<String> uris) {
        if (start == null || end == null || start.isEmpty() || end.isEmpty()) {
            return false;
        }
        if (uris != null && uris.isEmpty()) {
            return false;
        }
        return true;
    }

    private HttpHeaders defaultHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return headers;
    }

    private URI makeUri(String path) {
        ServiceInstance instance = retryTemplate.execute(context -> getInstance(statsServiceId));
        log.debug("Используется инстанс stats-server: host={}, port={}", instance.getHost(), instance.getPort());
        return URI.create("http://" + instance.getHost() + ":" + instance.getPort() + path);
    }

    private ServiceInstance getInstance(String serviceId) {
        try {
            List<ServiceInstance> instances = discoveryClient.getInstances(serviceId);
            if (instances == null || instances.isEmpty()) {
                throw new RuntimeException("Не найдено инстансов сервиса статистики с id: " + serviceId);
            }
            return instances.getFirst();
        } catch (Exception exception) {
            throw new RuntimeException(
                    "Ошибка обнаружения адреса сервиса статистики с id: " + serviceId,
                    exception
            );
        }
    }
}
