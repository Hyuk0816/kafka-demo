package dev.study.kafkademo.producer.service;

import dev.study.kafkademo.dto.EngineDto;
import dev.study.kafkademo.event.EngineDataReceivedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
@Slf4j
@RequiredArgsConstructor
public class SimulatorProducerService {

    private final WebClient webClient;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${spring.api.url}")
    private String apiUrl;

    /**
     * 시뮬레이터로부터 데이터를 수집하고 이벤트를 발행 (단일 책임: 데이터 수집)
     */
    @Scheduled(fixedRate = 1000)
    public void fetchEngineDataAndPublishEvent() {
        webClient.get()
                .uri(apiUrl)
                .retrieve()
                .bodyToMono(EngineDto.class)
                .timeout(Duration.ofSeconds(5))
                .subscribe(
                        this::publishEngineDataEvent,
                        error -> log.error("시뮬레이터 데이터 수집 실패: {}", error.getMessage())
                );
    }

    /**
     * 엔진 데이터 수신 이벤트 발행
     */
    private void publishEngineDataEvent(EngineDto engineDto) {
        try {
            // 타임스탬프 설정
            engineDto.setTimestamp();
            
            // 이벤트 발행
            EngineDataReceivedEvent event = new EngineDataReceivedEvent(this, engineDto);
            eventPublisher.publishEvent(event);
            
            log.debug("엔진 데이터 이벤트 발행 성공: {}", engineDto);
        } catch (Exception e) {
            log.error("엔진 데이터 이벤트 발행 실패: {}, 에러: {}", engineDto, e.getMessage());
        }
    }
}
