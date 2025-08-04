package dev.study.kafkademo.producer.service;

import dev.study.kafkademo.dto.EngineDto;
import dev.study.kafkademo.producer.service.batch.EngineDataSaveBatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class SimulatorProducerService {

    private static final String TOPIC = "engine";
    private final KafkaTemplate<String, EngineDto> kafkaTemplate;
    private final WebClient webClient;
    private final EngineDataSaveBatchService engineDataSaveBatchService;

    @Value("${spring.api.url}")
    private String apiUrl;


    @Scheduled(fixedRate = 1000)
    public void fetchAndSendDataAsync() {
        webClient.get()
                .uri(apiUrl)
                .retrieve()
                .bodyToMono(EngineDto.class)
                .flatMap(this::processEngineDataBatch)
                .subscribe(
                        result -> log.debug("엔진 데이터 처리 성공: {}", result),
                        error -> log.error("엔진 데이터 처리 실패: {}", error.getMessage())
                );
    }

    /**
     * 엔진 데이터 레디스 저장 및 DB 저장 배치
     */
    private Mono<String> processEngineDataBatch(EngineDto engineDto) {
        return Mono.just(engineDto)
                .flatMap(data -> {
                            data.setTimestamp();
                            // 엔진 데이터를 Redis에 저장
                            Mono<Long> dataMono = engineDataSaveBatchService.saveEngineData(data);
                            Mono<String> kafkaMono = sendToKafka(engineDto);
                            return Mono.zip(dataMono, kafkaMono)
                                    .map(tuple -> String.format("Redis 저장: %d, Kafka 전송: %s", tuple.getT1(), tuple.getT2()))
                                    .timeout(Duration.ofSeconds(10))
                                    .onErrorResume(error -> Mono.just(error.getMessage()));
                        }
                );
    }

    private Mono<String> sendToKafka(EngineDto engineDto) {
        return Mono.fromFuture(() -> {
                    engineDto.setTimestamp();
                    return kafkaTemplate.send(TOPIC, UUID.randomUUID().toString(), engineDto)
                            .toCompletableFuture();
                }).map(result -> "SUCCESS")
                .timeout(Duration.ofSeconds(3))
                .onErrorResume(error -> {
                    log.error("Kafka 메시지 전송 실패: {}, 에러: {}", engineDto, error.getMessage());
                    return Mono.just("FAILURE");
                });

    }

}
