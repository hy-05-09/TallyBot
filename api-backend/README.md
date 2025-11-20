# 🧮 TallyBot Backend 

TallyBot은 카카오톡 대화 기반 자동 정산 서비스로,
사용자가 업로드한 대화를 분석하 결제 내역을 추출하고,
송금 관계를 최소화한 최적 정산 결과를 제공하는 서비스입니다.

Backend는 Spring Boot 기반 REST API 서버로 구성되며,
업로드된 대화 → 데이터 전처리 → GPT 기반 정보 추출 → 정산 생성 → 최적화 → 응답 반환까지
완전한 백엔드 파이프라인을 구현했습니다.

<br>

# 📌 Features
## 1️⃣ Group / Member Management

- 그룹/멤버 생성 및 조회

- UserGroup 기반 도메인 구조

  <br>

## 2️⃣ Chat Upload & Pre-processing

- <kbd>POST /api/chat/upload</kbd>

- 카카오톡 챗봇이 전달한 JSON 대화를 저장

- Group / Member 존재 여부 검증

- 정산 기간 내 메시지 필터링

  <br>

## 3️⃣ Calculate Flow (Settlement Pipeline)
### ✔ Start Settlement

<kbd>POST /api/calculate/start</kbd>

- Calculate 엔티티 생성

- 시간 범위 내 채팅 조회

- GPT API 비동기 호출

- GPT 결과 누락 시 calculateId 자동 삭제 처리

### ✔ GPT Integration (FastAPI) & Post-processing
 
- RestTemplate 기반 POST 요청

- system + user 기반 메시지 변환

- 응답 null/empty 시 NoSettlementResultException 발생

### ✔ Settlement & Participant Generation

- 정산 결과(Settlement) 생성

- 참여자(Participant) 자동 매핑

- payer / payee 관계 기반 Detail 생성

### ✔ Optimization

- 불필요한 송금 관계를 제거하는 그래프 기반 최적화 적용
(Graph / Euler Circuit / Summarize 로직)

### ✔ Result API

<kbd>GET /api/calculate/{id}/brief-result</kbd>

- COMPLETED 결과 반환

- CALCULATING 상태 안내

- 데이터 없음 → 예외 처리

<br>

# ⚠ Exception Handling

GlobalExceptionHandler에서 다음 처리:

- IllegalArgumentException

- NoSuchElementException

- NoSettlementResultException

- JSON parsing error

- Validation errors

<br> 

# 🧪 Test Coverage

- Controller 테스트 (MockMvc)

- Service 레이어 테스트 (Mockito)

- GPT 응답 성공 / 실패 케이스 테스트

- 비동기 로직 검증 (Awaitility)

- Settlement 변환, Chat 저장, 오류 케이스 등 단위 테스트 포함

  <br>

# ⚙ Mock Data Generator
DataInitializer 자동 데이터 생성:

- group, member

- COMPLETED / PENDING / CALCULATING 상태의 calculate 3개 생성

- Settlement 3개 + Participant 전체 등록

- CalculateDetail 1개 생성

프론트 개발 및 테스트에서 즉시 사용 가능.

<br>

# ☁ Deployment

- EC2 환경에서 Spring Boot 서버 구성
- /actuator/health 기반 애플리케이션 상태 점검 (Spring Boot Actuator)
- AWS Secrets Manager 기반 환경 변수 안전 관리 

<br>

# 🧩 Tech Stack
## Backend

- Java 17

- Spring Boot 3

- Spring MVC

- JPA / Hibernate

- H2 (test), MySQL (real)

- Lombok

- Jackson

- Validation


## GPT Integration

- RestTemplate

- FastAPI 서버 연동

## Testing

- JUnit5

- Mockito

- MockMvc

- Awaitility
## Infra & Deployment
- AWS EC2
- AWS Secrets Manager
- Spring Boot Actuator (Health Check)

<br>

# 👥 Team Roles
## 🟥 Backend A (본인)

- 정산 Pipeline 전체 설계 및 구현
(Chat 업로드 → Preprocessing → GPT 호출 → Settlement 생성 → 최적화 결과 재구성 → Result API)
- GPTService FastAPI 연동 및 예외/비정상 응답 처리
- Settlement / Participant 생성 및 상태 관리 로직 구현
- GlobalExceptionHandler 작성 및 예외 흐름 통합
- 팀원이 작성한 OptimizationService 로직을 실제 정산 파이프라인에 통합하도록 리팩토링
  - 최적화 알고리즘 결과를 CalculateDetail로 재생성하는 흐름 재설계

- MockMvc / Mockito / Awaitility 기반 테스트 코드 작성

- Mock Data 자동 생성기(DataInitializer) 전체 구현

- 프론트엔드(웹/챗봇) 연동 테스트 및 오류 해결
- AWS EC2 배포 환경 구축 및 서버 운영
- /actuator/health 기반 애플리케이션 상태 점검 구현 (Spring Boot Actuator)
- AWS Secrets Manager 기반 보안 관리

## 🟦 Backend B (팀원)

- 그래프 기반 송금 최소화 알고리즘 구현

  - Graph / Eulerize / Summarize / UnionFind

  - WeightStrategy, FlattedGraph 구조 설계

- OptimizationService 초기 코드 작성

  <br>

# 📡 주요 API
## 🔹 Start Settlement

<kbd>POST /api/calculate/start</kbd>

## 🔹 Get Result (Brief)

<kbd>GET /api/calculate/{id}/brief-result</kbd>

## 🔹 Upload Chat Data

<kbd>POST /api/chat/upload</kbd>

<br>

# 🚀 How to Run
## Backend (Spring Boot)
<kbd>./gradlew build</kbd> <br>
<kbd>./gradlew bootRun</kbd>

<br>

## Environment

- application.properties에서 H2 또는 MySQL 선택

- GPT 연동 FastAPI 서버 URL 설정 필요

<br>

## GPT FastAPI Server
<kbd>uvicorn main:app --reload --port 8000</kbd>

<br>

# 📂 Project Structure (Simplified)
api-backend/ <br>
 ├── controller/ → REST API 엔드포인트 <br>
 ├── service/ → 핵심 비즈니스 로직 <br>
 ├── repository/ → JPA Repository 계층 <br>
 ├── dto/ → 요청·응답 DTO <br>
 ├── domain/ → JPA 엔티티 <br>
 ├── debtopt/ → Graph 기반 정산 최적화 알고리즘 <br>
 ├── config/ → 예외 처리, 설정 파일 <br>
 ├── test/ → 단위·통합 테스트 <br>
