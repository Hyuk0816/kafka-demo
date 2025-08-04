package dev.study.kafkademo.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import dev.study.kafkademo.entity.Engine;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class EngineDto {

    private BigDecimal temperature; // °C
    private BigDecimal rpm; // Revolutions per minute
    private BigDecimal pressure; // psia
    private BigDecimal fuelFlow; // Fuel flow rate in kg/s
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp; // ISO 8601 format (e.g., "2023-10-01T12:00:00Z")

    public static List<Engine> toEntity(List<EngineDto> engineDtoList) {
        return engineDtoList.stream()
                .map(dto -> Engine.builder()
                        .temperature(dto.getTemperature())
                        .rpm(dto.getRpm())
                        .pressure(dto.getPressure())
                        .fuelFlow(dto.getFuelFlow())
                        .timestamp(dto.getTimestamp())
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build())
                .toList();
    }

    public void setTimestamp(){
        this.timestamp = LocalDateTime.now();
    }

}
