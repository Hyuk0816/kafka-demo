package dev.study.kafkademo.handler;

import dev.study.kafkademo.dto.EngineDto;
import dev.study.kafkademo.event.EngineDataReceivedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 엔진 데이터 Kafka 메시지 처리 핸들러
 * 단일 책임: Kafka 메시지 발송
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class EngineDataKafkaHandler {

    private static final String TOPIC = "engine";
    private final KafkaTemplate<String, EngineDto> kafkaTemplate;

    /**
     * 엔진 데이터 수신 이벤트 처리 - Kafka 메시지 발송
     */
    @EventListener
    @Async
    public void handleEngineDataForKafka(EngineDataReceivedEvent event) {
        try {
            log.debug("엔진 데이터 Kafka 처리 시작: {}", event.getEngineData());
            
            sendToKafkaAsync(event.getEngineData())
                    .whenComplete((result, throwable) -> {
                        if (throwable != null) {
                            log.error("Kafka 메시지 전송 실패: 데이터 = {}, 에러 = {}", 
                                     event.getEngineData(), throwable.getMessage());
                        } else {
                            log.debug("Kafka 메시지 전송 성공: 데이터 = {}, 결과 = {}", 
                                     event.getEngineData(), result);
                        }
                    });
                    
        } catch (Exception e) {
            log.error("엔진 데이터 Kafka 처리 중 예외 발생: 데이터 = {}, 에러 = {}", 
                     event.getEngineData(), e.getMessage());
        }
    }

    /**
     * Kafka로 비동기 메시지 전송
     */
    private CompletableFuture<String> sendToKafkaAsync(EngineDto engineDto) {
        String messageKey = UUID.randomUUID().toString();
        
        return kafkaTemplate.send(TOPIC, messageKey, engineDto)
                .toCompletableFuture()
                .thenApply(result -> {
                    String partition = String.valueOf(result.getRecordMetadata().partition());
                    String offset = String.valueOf(result.getRecordMetadata().offset());
                    return String.format("SUCCESS - Partition: %s, Offset: %s", partition, offset);
                })
                .exceptionally(throwable -> {
                    log.error("Kafka 전송 중 예외: key = {}, 데이터 = {}, 에러 = {}", 
                             messageKey, engineDto, throwable.getMessage());
                    return "FAILURE - " + throwable.getMessage();
                });
    }
}