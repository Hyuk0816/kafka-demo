package dev.study.kafkademo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.math.BigDecimal;

/**
 * Engine Entity - Based on NASA CMAPS Dataset Structure
 * 
 * Represents aircraft engine sensor data for real-time monitoring and predictive maintenance.
 * Contains both operational settings and sensor measurements following NASA CMAPS standards.
 * 
 * Data Structure Reference:
 * - Columns 1-2: Unit identification and time data
 * - Columns 3-5: Three operational settings (altitude, throttle, temperature conditions)
 * - Columns 6-26: 21 sensor measurements (temperature, pressure, speed, flow rates, etc.)
 */
@Entity
@Table(name = "engines", indexes = {
    @Index(name = "idx_engine_timestamp", columnList = "timestamp"),
})
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Engine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    // Simplified/Business-Friendly Fields for Alerts
    @Column(name = "temperature", precision = 10, scale = 2)
    private BigDecimal temperature; // Derived from sensor readings (°C)

    @Column(name = "rpm", precision = 10, scale = 2)
    private BigDecimal rpm; // Derived from sensor readings

    @Column(name = "pressure", precision = 10, scale = 2)
    private BigDecimal pressure; // Derived from sensor readings (psia)

    @Column(name = "fuel_flow", precision = 10, scale = 4)
    private BigDecimal fuelFlow; // Derived fuel flow rate
    
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    // Audit fields
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

}