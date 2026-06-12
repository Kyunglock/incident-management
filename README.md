# 장애 대응 관리 시스템

에듀넷 서비스 운영(SM) 단계의 장애 인지부터 복구·사후관리까지 전 과정을 지원하는 시스템입니다.  
SR 접수 → 조치 기록 → 반영 계획서 작성 → **LLM 기반 처리 문서 자동 생성**까지 한 화면에서 처리합니다.

---

## 기술 스택

| 구분 | 기술 |
|------|------|
| 프론트엔드 | Vue 3, Vite, Pinia, Vue Router, Axios |
| 백엔드 | Spring Boot 3.2, JDK 17, Maven |
| 데이터베이스 | MySQL 8.x |
| AI | 로컬 LLM API (kwaklabs.com) |

---

## 사전 요구사항

- JDK 17 이상
- Maven 3.8 이상 (또는 `./mvnw` Wrapper 사용)
- Node.js 18 이상
- MySQL 8.x

---

## 1. 데이터베이스 준비

MySQL에 접속하여 데이터베이스를 생성합니다.

```sql
CREATE DATABASE incident_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

> 기본 접속 정보: `root` / `password` (변경 시 아래 설정 파일 수정)

---

## 2. 백엔드 실행

### 설정 파일 수정 (필요 시)

`backend/src/main/resources/application.yml`

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/incident_db?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=UTF-8&serverTimezone=Asia/Seoul
    username: root        # DB 계정으로 변경
    password: password    # DB 비밀번호로 변경

ai:
  llm:
    url: https://kwaklabs.com/api/v1/kwakai/chat  # 로컬 LLM API 주소
```

### 실행

```bash
cd backend

# Maven Wrapper 사용 (권장)
./mvnw spring-boot:run

# 또는 Maven 직접 사용
mvn spring-boot:run
```

서버가 시작되면 `http://localhost:8080` 에서 API가 제공됩니다.  
최초 실행 시 `data.sql`의 샘플 장애 데이터 5건이 자동으로 삽입됩니다.

---

## 3. 프론트엔드 실행

```bash
cd frontend

# 의존성 설치 (최초 1회)
npm install

# 개발 서버 실행
npm run dev
```

브라우저에서 `http://localhost:3000` 으로 접속합니다.

> 프론트엔드 개발 서버는 `/api` 요청을 자동으로 `http://localhost:8080` 으로 프록시합니다.

---

## 4. 주요 기능

### 대시보드
- 상태별(접수 / 처리중 / 해결됨 / 종료) 통계 카드
- 최근 장애 5건 목록

### 장애 목록
- 상태 탭 필터 및 페이지네이션
- 새 장애 등록 모달

### 장애 상세

| 탭 | 기능 |
|----|------|
| 조치 내역 | 분석·수정·배포·검증 조치 추가 및 이력 확인 |
| 반영 계획서 | 계획서 작성 → 제출 → 승인/반려 |
| 처리 문서 | **AI 문서 자동 생성** 및 검토·편집 후 저장 |

### AI 문서 자동 생성

장애 정보와 조치 내역을 기반으로 로컬 LLM이 처리 문서 초안을 자동 작성합니다.

- **입력**: SR 내용, 담당자, 조치 내역
- **출력**: 증상 / 근본 원인 / 조치 내용 / 반영 내역 / 결과 구조의 문서 초안
- 생성 후 담당자가 검토·보완하여 최종 확정

```
POST /api/incidents/{id}/generate-document
```

---

## 5. API 엔드포인트 요약

```
GET    /api/dashboard/stats
GET    /api/incidents?status=OPEN&page=0&size=10
POST   /api/incidents
GET    /api/incidents/{id}
PUT    /api/incidents/{id}
PATCH  /api/incidents/{id}/status

POST   /api/incidents/{id}/actions
GET    /api/incidents/{id}/actions

GET    /api/incidents/{id}/deployment-plans
POST   /api/incidents/{id}/deployment-plans
PATCH  /api/incidents/{id}/deployment-plans/{planId}/status

GET    /api/incidents/{id}/documents
POST   /api/incidents/{id}/generate-document
PUT    /api/incidents/{id}/documents/{docId}
```

---

## 6. 프로젝트 구조

```
incident-management/
├── backend/                          # Spring Boot 3 백엔드
│   ├── pom.xml
│   └── src/main/java/com/incident/management/
│       ├── config/                   # CORS, WebClient (SSL bypass)
│       ├── controller/               # REST 컨트롤러 4개
│       ├── service/                  # 비즈니스 로직 + AiService
│       ├── entity/                   # JPA 엔티티 4개
│       ├── repository/               # Spring Data JPA
│       ├── dto/                      # Request / Response DTO
│       └── exception/                # 전역 예외 처리
└── frontend/                         # Vue 3 프론트엔드
    ├── package.json
    └── src/
        ├── views/                    # 대시보드, 목록, 상세, 등록
        ├── components/               # 공통 컴포넌트
        ├── stores/                   # Pinia 상태 관리
        ├── services/api.js           # Axios API 클라이언트
        └── router/                   # Vue Router
```
