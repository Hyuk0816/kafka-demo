package dev.study.kafkademo.handler;

import dev.study.kafkademo.event.EngineDataReceivedEvent;
import dev.study.kafkademo.producer.service.batch.EngineDataSaveBatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 엔진 데이터 영속성 처리 핸들러
 * 단일 책임: Redis 배치 저장 처리
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class EngineDataPersistenceHandler {

    private final EngineDataSaveBatchService engineDataSaveBatchService;

    /**
     * 엔진 데이터 수신 이벤트 처리 - Redis 배치 저장
     */
    @EventListener
    @Async
    public void handleEngineDataForPersistence(EngineDataReceivedEvent event) {
        try {
            log.debug("엔진 데이터 영속성 처리 시작: {}", event.getEngineData());
            
            engineDataSaveBatchService.saveEngineData(event.getEngineData())
                    .subscribe(
                            queueSize -> {
                                log.debug("엔진 데이터 Redis 저장 성공: 큐 크기 = {}, 데이터 = {}", 
                                         queueSize, event.getEngineData());
                            },
                            error -> {
                                log.error("엔진 데이터 Redis 저장 실패: 데이터 = {}, 에러 = {}", 
                                         event.getEngineData(), error.getMessage());
                            }
                    );
                    
        } catch (Exception e) {
            log.error("엔진 데이터 영속성 처리 중 예외 발생: 데이터 = {}, 에러 = {}", 
                     event.getEngineData(), e.getMessage());
        }
    }
}