package dev.study.kafkademo.event;

import dev.study.kafkademo.dto.EngineDto;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;

/**
 * 엔진 데이터 수신 이벤트
 * SimulatorProducerService에서 발행하고 각종 Handler들이 구독하는 이벤트
 */
@Getter
public class EngineDataReceivedEvent extends ApplicationEvent {
    
    private final EngineDto engineData;
    private final LocalDateTime receivedAt;
    
    public EngineDataReceivedEvent(Object source, EngineDto engineData) {
        super(source);
        this.engineData = engineData;
        this.receivedAt = LocalDateTime.now();
    }
    
    /**
     * 편의 생성자 - source를 engineData로 사용
     */
    public EngineDataReceivedEvent(EngineDto engineData) {
        this(engineData, engineData);
    }
    
    @Override
    public String toString() {
        return String.format("EngineDataReceivedEvent{timestamp=%s, temperature=%s, rpm=%s, receivedAt=%s}",
                engineData != null && engineData.getTimestamp() != null ? engineData.getTimestamp() : "unknown",
                engineData != null ? engineData.getTemperature() : "0.0",
                engineData != null ? engineData.getRpm() : "0",
                receivedAt
        );
    }
}