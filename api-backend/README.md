# 🧮 TallyBot Backend 

TallyBot은 카카오톡 대화 데이터를 기반으로  
비정형 메시지에서 결제 정보를 추출하고,  
송금 관계를 최소화한 정산 결과를 자동 생성하는 백엔드 서비스입니다.

GPT 기반 비동기 처리, 정산 실패 롤백, 그래프 기반 최적화를 포함한  
실제 운영을 고려한 정산 파이프라인을 Spring Boot로 구현했습니다.

<br>

# 📌 Features
## 1️⃣ Group / Member Management

- 그룹/멤버 생성 및 조회

- UserGroup 기반 도메인 구조

---

## 2️⃣ Chat Upload & Pre-processing

- <kbd>POST /api/chat/upload</kbd>

- 카카오톡 챗봇이 전달한 JSON 대화를 저장

- Group / Member 존재 여부 검증

- 정산 기간 내 메시지 필터링

---

## 3️⃣ Settlement Pipeline (Calculation & Optimization)
### ✔ Start Settlement

<kbd>POST /api/calculate/start</kbd>

- Calculate 엔티티 생성

- 시간 범위 내 채팅 조회

- GPT API 비동기 호출

- GPT 결과 누락 시 calculateId 자동 삭제 처리

### ✔ GPT Integration (FastAPI) & Post-processing
 
- RestTemplate 기반 POST 요청

- system + user 기반 메시지 변환

- GPT 응답 누락·지연·부분 응답 등 불안정성을 고려한 방어 로직 구현

- 정산 결과 생성 실패 시 Calculate 자동 롤백 처리로 데이터 정합성 보장
  
- 외부 AI API 의존 환경에서도 서비스 안정성을 유지하도록 설계


### ✔ Settlement & Participant Generation

- 정산 결과(Settlement) 생성

- 참여자(Participant) 자동 매핑

- payer / payee 관계 기반 Detail 생성

### ✔ Optimization

- 불필요한 송금 관계를 제거하는 그래프 기반 최적화 적용
- Graph / Euler Circuit / Summarize 알고리즘 활용

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

## 🧠 Engineering Challenges & Decisions

### 1️⃣ GPT 중복 호출로 인한 정산 결과 비정상 문제

**문제**
- 정산 요청 시 GPT API가 의도치 않게 두 번 호출되며  
  동일 결제 정보가 중복 인식되어 정산 결과가 비정상적으로 생성됨
- 알고리즘 및 GPT 프롬프트는 로컬 환경에서 정상 동작하여  
  원인 파악이 쉽지 않았음

**과정**
- GPT 호출 지점을 기준으로 로그를 세분화하여 요청 흐름 추적
- 동일한 요청이 GPT 서버로 중복 전달되는 현상 확인
- 디버깅 과정에서 임시로 추가한 raw response 출력 코드가  
  제거되지 않아 중복 호출을 유발하고 있음을 발견

**해결**
- GPT 호출을 단일 진입 지점에서만 수행하도록 코드 구조 정리
- 중복 호출 가능성이 있는 흐름 제거 및 호출 책임 명확화

**결과**
- 동일 요청에 대한 중복 정산 문제 완전 제거
- 외부 API 연동 시 호출 흐름을 명확히 통제해야 한다는 점을 체감
- 이후 모든 외부 API 연동 로직에서 단일 진입점 설계 원칙 유지

---

### 2️⃣ GPT 응답 누락·부분 응답으로 인한 데이터 정합성 문제

**문제**
- GPT 응답이 null / empty / partial한 경우에도  
  Calculate 엔티티만 DB에 남아 정산 상태 불일치 발생

**해결**
- GPT 응답 검증 로직 추가
- 정산 실패 시 Calculate, Settlement, Participant 전체를  
  트랜잭션 단위로 롤백 처리

**결과**
- 실패한 정산 요청이 시스템에 잔존하지 않도록 보장
- 정산 서비스 특성상 중요한 데이터 정합성 확보

---

### 3️⃣ 정산 기간 입력 방식에 대한 설계 결정

**문제**
- 정산 기간 입력 방식에 대해 팀 내 다양한 의견 존재  
  (당일 기준, 마지막 정산 이후, 특정 일자, 기간 입력 등)

**결정**
- 재정산 필요 상황 및 사용자 실수 복구 시나리오를 고려
- 시작일과 종료일을 명확히 입력받는 방식이  
  가장 안정적이라고 판단

**결과**
- “mm월 dd일 ~ mm월 dd일 정산” 형태로 API 구조 확정
- 날짜 파싱 기준을 문서화하여 프론트엔드 및 AI 파트와 공유
- 다양한 사용자 시나리오를 안정적으로 처리 가능

---

### 4️⃣ 운영 관점에서의 서버 상태 관리 도입

**문제**
- 서버가 정상적으로 기동되었는지 확인할 수 있는 구조 부재 장애 발생 시 원인 파악에 시간 소요


**해결**
- Spring Boot Actuator 기반 `/actuator/health` 활성화 - EC2 배포 환경에서 서버, DB, 외부 API 상태 확인 가능하도록 구성

**결과**
- 배포 후 서버 상태를 즉시 확인 가능
- 운영 환경에서의 불확실성 감소
- 팀원 모두가 동일한 기준으로 서버 상태를 공유

  
<br>

# 🧪 Test Coverage

- Controller 테스트 (MockMvc)

- Service 레이어 테스트 (Mockito)

- GPT 응답 성공 / 실패 케이스 테스트

- 비동기 로직 검증 (Awaitility)

- Settlement 변환, Chat 저장, 오류 케이스 등 단위 테스트 포함
- 외부 API(GPT) 의존성과 비동기 파이프라인 안정성 검증에 중점

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

- Profile(dev / rds) 분리로 로컬·운영 환경 안전하게 구성

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

- 그래프 기반 송금 최소화 알고리즘 설계 및 구현
  - Graph / Eulerize / Summarize / UnionFind
  - WeightStrategy, FlattedGraph 구조 설계

- 정산 알고리즘 결과를 저장하기 위한
  DB 테이블 구조 설계 및 일부 엔티티 구현 참여

- 챗봇(카카오톡 연동) 파트 구현 및
  정산 요청 흐름에서의 사용자 입력 처리 담당

- 초기 데이터 흐름 정의 및
  백엔드-알고리즘 간 데이터 포맷 논의 참여

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

✅ Local (Dev, AWS 미사용)
로컬에서는 AWS Secrets Manager를 사용하지 않는 dev 프로필로 실행합니다.
<kbd>./gradlew bootRun --args="--spring.profiles.active=dev"</kbd>

---
✅ RDS (AWS Secrets Manager 사용)

RDS 환경은 `rds` 프로필을 사용하며,
DB 접속 정보는 AWS Secrets Manager에서 로드합니다.

AWS Credentials가 설정된 환경(EC2, IAM Role 등)에서만 정상적으로 실행됩니다.
<kbd>./gradlew bootRun --args="--spring.profiles.active=rds"</kbd>

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
