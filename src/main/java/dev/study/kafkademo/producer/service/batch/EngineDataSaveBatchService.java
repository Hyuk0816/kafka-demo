package dev.study.kafkademo.producer.service.batch;

import dev.study.kafkademo.dto.EngineDto;
import dev.study.kafkademo.entity.Engine;
import dev.study.kafkademo.repository.EngineRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class EngineDataSaveBatchService {
    private static final String ENGINE_DATA_KEY = "engine:data:batch";

    private final ReactiveRedisTemplate<String, EngineDto> redisTemplate;
    private final EngineRepository engineRepository;

    private final int BATCH_SIZE = 100;

    private final int maxBatchSize = 100000;

    /**
     * 엔진 데이터를 Redis에 비동기로 리스트 저장
     */
    public Mono<Long> saveEngineData(EngineDto engineDto) {
        return redisTemplate.opsForList()
                .rightPush(ENGINE_DATA_KEY, engineDto)
                .doOnNext(size -> {
                    if (size > maxBatchSize) {
                        log.warn("Redis 리스트 크기가 {}를 초과했습니다. 현재 크기: {}", maxBatchSize, size);
                    }
                    log.debug("엔진 데이터 큐 추가 성공: {}, 현재 크기: {}", engineDto, size);
                })
                .onErrorResume(error -> {
                    log.error("엔진 데이터 큐 추가 실패: {}, 에러: {}", engineDto, error.getMessage());
                    return Mono.just(0L);
                });
    }

    /**
     * 배치 처리 스케줄러
     */
    @Scheduled(fixedDelay = 10000)
    public void processBatchSchedule() {
        processBatch()
                .subscribe(
                        count -> {
                            if (count > 0) {
                                log.info("배치 처리 완료: 저장된 엔진 데이터 수 = {}", count);
                            } else {
                                log.info("배치 처리 완료: 저장된 엔진 데이터 없음");
                            }
                        },
                        error -> log.error("배치 처리 중 에러 발생: {}", error.getMessage()
                        )
                );
    }

    /**
     * 배치 처리 로직
     */
    public Mono<Integer> processBatch() {
        return extractBatchData()
                .flatMap(this::saveBatchToDataBase)
                .onErrorResume(error -> {
                    log.error("배치 처리 중 에러 발생: {}", error.getMessage());
                    return Mono.just(0);
                });
    }

    /**
     * 배치 키 로부터 데이터 추출
     */
    private Mono<List<EngineDto>> extractBatchData() {
        return redisTemplate.opsForList()
                .range(ENGINE_DATA_KEY, 0, BATCH_SIZE - 1)
                .collectList()
                .filter(list -> !list.isEmpty())
                .flatMap(list ->
                        redisTemplate.opsForList()
                                .trim(ENGINE_DATA_KEY, list.size(), -1)
                                .thenReturn(list))
                .switchIfEmpty(Mono.just(Collections.emptyList()))
                .doOnNext(list -> {
                    if (!list.isEmpty()) {
                        log.debug("배치 데이터 추출 성공: 크기 {}", list.size());
                    } else {
                        log.debug("배치 데이터 추출 실패: 빈 리스트");
                    }
                });
    }

    /**
     * 레디스에서 조회 된 데이터를 DB에 저장
     */
    private Mono<Integer> saveBatchToDataBase(List<EngineDto> engineDtoList) {
        if (engineDtoList.isEmpty()) {
            return Mono.just(0);
        }

        return Mono.fromCallable(() -> {

                    List<Engine> engineDataList = EngineDto.toEntity(engineDtoList);

                    engineRepository.saveAll(engineDataList);
                    log.debug("배치 데이터 DB 저장 성공: 크기 {}", engineDataList.size());
                    return engineDataList.size();
                })
                .subscribeOn(Schedulers.boundedElastic())
                .timeout(Duration.ofSeconds(30))
                .onErrorResume(error -> {
                    log.error("배치 데이터 DB 저장 실패: {}, 에러: {}", engineDtoList.size(), error.getMessage());
                    return Mono.just(0);
                });
    }
}
