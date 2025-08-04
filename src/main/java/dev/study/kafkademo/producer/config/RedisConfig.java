package dev.study.kafkademo.producer.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.study.kafkademo.dto.EngineDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
@Slf4j
public class RedisConfig {

    @Bean
    public LettuceConnectionFactory reactiveRedisConnectionFactory(
            @Value("${spring.data.redis.host:localhost}") String host,
            @Value("${spring.data.redis.port:6399}") int port) {
        
        // Lettuce 클라이언트 설정 - 타임아웃 및 성능 최적화
        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
            .commandTimeout(Duration.ofSeconds(2))
            .shutdownTimeout(Duration.ofMillis(100))
            .build();
            
        RedisStandaloneConfiguration serverConfig = new RedisStandaloneConfiguration(host, port);
        
        log.info("Redis Reactive 연결 설정: {}:{}", host, port);
        return new LettuceConnectionFactory(serverConfig, clientConfig);
    }
    
    @Bean
    public ReactiveRedisTemplate<String, EngineDto> reactiveEngineTemplate(
            ReactiveRedisConnectionFactory connectionFactory) {
        
        // ObjectMapper 설정 - LocalDateTime 지원
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        // EngineDto 전용 JSON 직렬화 설정
        Jackson2JsonRedisSerializer<EngineDto> jsonSerializer = 
            new Jackson2JsonRedisSerializer<>(objectMapper, EngineDto.class);
        
        // 직렬화 컨텍스트 구성
        RedisSerializationContext<String, EngineDto> serializationContext = 
            RedisSerializationContext.<String, EngineDto>newSerializationContext()
                .key(StringRedisSerializer.UTF_8)
                .value(jsonSerializer)
                .hashKey(StringRedisSerializer.UTF_8)
                .hashValue(jsonSerializer)
                .build();
                
        return new ReactiveRedisTemplate<>(connectionFactory, serializationContext);
    }
    
    @Bean
    public ReactiveStringRedisTemplate reactiveStringRedisTemplate(
            ReactiveRedisConnectionFactory connectionFactory) {
        // 범용 String 처리용 템플릿 (Alert 등에 사용)
        return new ReactiveStringRedisTemplate(connectionFactory);
    }
}
