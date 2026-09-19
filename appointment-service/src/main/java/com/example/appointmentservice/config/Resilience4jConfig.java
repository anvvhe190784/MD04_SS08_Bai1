package com.example.appointmentservice.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.core.registry.EntryAddedEvent;
import io.github.resilience4j.core.registry.EntryRemovedEvent;
import io.github.resilience4j.core.registry.EntryReplacedEvent;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class Resilience4jConfig {

    private static final Logger log = LoggerFactory.getLogger(Resilience4jConfig.class);

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(2000);
        return new RestTemplate(factory);
    }

    @Bean
    public RegistryEventConsumer<CircuitBreaker> circuitBreakerEventConsumer() {
        return new RegistryEventConsumer<CircuitBreaker>() {
            @Override
            public void onEntryAddedEvent(EntryAddedEvent<CircuitBreaker> entryAddedEvent) {
                CircuitBreaker circuitBreaker = entryAddedEvent.getAddedEntry();
                log.info("CircuitBreaker '{}' da duoc khoi tao va dang ky theo doi su kien.", circuitBreaker.getName());

                circuitBreaker.getEventPublisher()
                        .onStateTransition(event -> log.info("⚡⚡⚡ [CIRCUIT BREAKER: {}] CHUYEN TRANG THAI: {} -> {} ⚡⚡⚡",
                                event.getCircuitBreakerName(),
                                event.getStateTransition().getFromState(),
                                event.getStateTransition().getToState()))
                        .onError(event -> log.warn("[CIRCUIT BREAKER: {}] Cuoc goi that bai: {}",
                                event.getCircuitBreakerName(),
                                event.getThrowable() != null ? event.getThrowable().getMessage() : "Unknown error"))
                        .onSuccess(event -> log.info("[CIRCUIT BREAKER: {}] Cuoc goi thanh cong (thoi gian: {}ms)",
                                event.getCircuitBreakerName(),
                                event.getElapsedDuration().toMillis()));
            }

            @Override
            public void onEntryRemovedEvent(EntryRemovedEvent<CircuitBreaker> entryRemoveEvent) {
            }

            @Override
            public void onEntryReplacedEvent(EntryReplacedEvent<CircuitBreaker> entryReplacedEvent) {
            }
        };
    }
}
