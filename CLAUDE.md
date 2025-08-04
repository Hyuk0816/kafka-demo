# Kafka Demo 프로젝트 - Claude Code 개발 가이드

## 📊 프로젝트 개요

**프로젝트명**: Kafka Demo - 실시간 항공기 엔진 데이터 처리 시스템

**핵심 목표**:
- 시뮬레이션 데이터의 Kafka 전송 구현
- 크리티컬 임계치 모니터링 및 실시간 알람
- 배치 DB 저장으로 성능 최적화
- 실시간 데이터 처리 파이프라인 구축

**기술 스택**:
- **Backend**: Spring Boot + WebFlux + JPA
- **Messaging**: Apache Kafka
- **Database**: PostgreSQL + Redis
- **ML Consumer**: Python + FastAPI
- **Testing**: JUnit 5 + Testcontainers

---

## 🏗️ 아키텍처 및 데이터 흐름

### 핵심 데이터 흐름
```
Simulator API (항공기 엔진 데이터)
    ↓ (1초마다 WebClient 비동기 호출)
Producer Service
    ├─→ 임계치 체크 → Alert Service (비동기)
    ├─→ BlockingQueue → 배치 DB 저장 (100개 단위)
    └─→ Kafka Topic "engine"
         ↓
    Python Consumer (FastAPI)
         └─→ ML 모델 → 실시간 이상치 탐지
```

### 주요 설계 결정
1. **DB 저장 위치**: Producer에서 저장 (원본 데이터 보존, Alert 로직 통합)
2. **배치 처리**: BlockingQueue + 100개 단위 Bulk Insert (DB 부하 100배 감소)
3. **비동기 전략**: API 호출/Alert는 비동기, DB 저장은 배치로 동기식

---

## 📝 개발 규칙 및 유의사항

### 1. Context7 MCP 모듈 사용 필수
- **MANDATORY**: 모든 Spring Framework 관련 개발은 Context7 MCP를 통해 최신 문서 확인
- **목적**: Deprecated API 사용 방지, 최신 Best Practice 적용
- **사용법**: `--c7` 플래그 사용하여 Spring Boot, JPA, WebFlux 관련 패턴 검색

### 2. 실시간 시간 기록 (KST 기준)
- **규칙**: 모든 작업은 실제 시간 함수를 사용하여 KST 기준으로 기록
- **형식**: `YYYY.MM.DD HH:MM` (예: 2025.08.02 14:30)
- **적용**: work-report.md 업데이트 시 실제 작업 시간 반영

### 3. 단계별 진행 상황 추적
- **체크리스트**: 각 단계별로 완료 시 ✅ 표시
- **상태 관리**: 진행중/완료/보류 명확히 구분
- **문제 기록**: 에러 발생 시 원인과 해결책 상세 기록

---

## 🚀 개발 우선순위 및 액션 아이템

### Phase 1: 핵심 기능 구현 (즉시 실행)
1. **Engine 엔티티 및 Repository 구현**
   - engineId, status, temperature, rpm, timestamp 필드
   - JPA 엔티티 매핑 및 Repository 인터페이스

2. **EngineDto 및 변환 로직**
   - API 응답 매핑용 DTO
   - toEntity() 메서드 구현

3. **SimulatorProducerService 배치 처리 로직**
   - BlockingQueue<Engine> 구현
   - saveBatchSync() 메서드 추가
   - 100개 단위 배치 저장

4. **AlertService 구현**
   - 임계치: Temperature > 350°C, RPM > 8000
   - 비동기 알림 발송 (Email/Slack)

### Phase 2: 설정 및 최적화 (단기)
- application.yml JPA 배치 설정
- 임계치 및 배치 크기 외부화
- 에러 처리 및 재시도 로직

### Phase 3: 확장 및 모니터링 (중장기)
- Reactive Redis 도입 검토
- R2DBC 전환 POC
- 성능 메트릭 및 모니터링 대시보드

---

## 📊 성능 및 품질 목표

### 성능 지표
- **DB Insert**: 100개/배치, 36회/시간 이하
- **메모리 사용량**: 100MB 이하
- **데이터 지연시간**: 최대 10초
- **Alert 발송 시간**: 임계치 초과 후 1초 이내

### 품질 기준
- **데이터 손실률**: 0% (필수)
- **중복 데이터 방지**: 완전 보장
- **트랜잭션 무결성**: 완전 보장
- **장애 복구 시간**: 5분 이내

---

## 🔧 기술적 세부사항

### JPA 배치 최적화 설정
```yaml
spring:
  jpa:
    properties:
      hibernate:
        jdbc:
          batch_size: 100
          batch_versioned_data: true
        order_inserts: true
        order_updates: true
```

### 임계치 설정 (외부화 예정)
```yaml
engine:
  thresholds:
    temperature: 350.0  # °C
    rpm: 8000          # RPM
  batch:
    size: 100
    timeout: 10000     # 10초
```

### 큐 구현 방식
- **타입**: BlockingQueue<Engine>
- **크기 제한**: 1000개 (메모리 보호)
- **처리 방식**: drainTo() 활용한 효율적 배치 처리

---

## 🚨 리스크 관리

### 식별된 주요 리스크
1. **메모리 큐 오버플로우**
   - 대응: 큐 크기 제한 및 백프레셔 구현

2. **배치 저장 실패**
   - 대응: Dead Letter Queue 또는 재시도 로직

3. **Alert 서비스 장애**
   - 대응: Circuit Breaker 패턴 적용

4. **Consumer 처리 지연**
   - 대응: Kafka 파티션 증설 및 스케일아웃

---

## 📚 참고 문서 및 리소스

### 필수 문서
- [work-report.md](./work-report.md): 상세 진행 상황 및 설계 결정
- [Spring Boot WebFlux 공식 문서](https://docs.spring.io/spring-framework/docs/current/reference/html/web-reactive.html)
- [Apache Kafka 공식 문서](https://kafka.apache.org/documentation/)

### Context7 검색 키워드
- "Spring Boot WebFlux batch processing"
- "JPA batch insert optimization"
- "Kafka producer configuration"
- "BlockingQueue thread safety"

---

## ⚡ 빠른 시작 가이드

### 1. 개발 환경 확인
```bash
# Java 17+ 확인
java -version

# Docker 실행 (Kafka, PostgreSQL)
docker-compose up -d

# 프로젝트 빌드
./gradlew build
```

### 2. 현재 구현 상태 확인
- ✅ SimulatorProducerService (기본 구조)
- ✅ WebClient 비동기 데이터 수집
- ✅ Kafka 메시지 전송
- ⏳ DB 배치 저장 (구현 예정)
- ⏳ 임계치 Alert 시스템 (구현 예정)

### 3. 다음 작업
1. Engine 엔티티 생성 → Context7로 JPA 최신 패턴 확인
2. 배치 처리 로직 구현 → performance 최적화 고려
3. Alert 시스템 구현 → 비동기 처리 패턴 적용

---

*문서 생성일: 2025.08.02*
*최종 업데이트: KST 기준 실시간 반영*