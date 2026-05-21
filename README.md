# 📋 OP 성적서 발행기 - 개발 설명서

> 주식회사 옵티로(Optilo Co., Ltd.)의 내부 교정 성적서(Calibration Certificate)를 A4 1페이지 PDF로 자동 생성하고 다운로드하는 웹 기반 내부 업무 도구입니다.

---

## 1. 개발 환경 (Tech Stack)

### 백엔드 (Backend)
| 항목 | 내용 |
|---|---|
| 언어 | Java 17 |
| 프레임워크 | Spring Boot 3.2.5 |
| 빌드 도구 | Maven (mvnw 내장 래퍼) |
| 서버 포트 | 8080 |

### 프론트엔드 (Frontend)
| 항목 | 내용 |
|---|---|
| 언어 | JavaScript (JSX) |
| 라이브러리 | React 18.3.1 |
| 번들러 | Vite 5.2.11 |
| HTTP 클라이언트 | Axios 1.6.8 |
| 개발 서버 포트 | 5173 (기본값) |

### 데이터베이스
| 항목 | 내용 |
|---|---|
| DBMS | MySQL |
| 데이터베이스명 | `pdf_db` |
| 연결 포트 | 3306 |
| 타임존 | Asia/Seoul |
| DDL 정책 | `ddl-auto: update` (서버 기동 시 테이블 자동 생성/변경) |

---

## 2. 사용 라이브러리 (Dependencies)

### 백엔드 주요 의존성 (`pom.xml`)

| 라이브러리 | 버전 | 용도 |
|---|---|---|
| `spring-boot-starter-web` | 3.2.5 (Boot 기본) | REST API 서버 구성, HTTP 요청/응답 처리 |
| `spring-boot-starter-data-jpa` | 3.2.5 (Boot 기본) | JPA/Hibernate를 통한 ORM 데이터베이스 연동 |
| `mysql-connector-j` | Boot 관리 버전 | Java ↔ MySQL 연결 드라이버 |
| `openpdf` | 1.3.30 | A4 PDF 레이아웃 생성 (iText 오픈소스 포크) |
| `spring-boot-starter-test` | 3.2.5 (Boot 기본) | JUnit 기반 단위/통합 테스트 |

### 프론트엔드 주요 의존성 (`package.json`)

| 라이브러리 | 버전 | 용도 |
|---|---|---|
| `react` | 18.3.1 | UI 컴포넌트 렌더링 |
| `react-dom` | 18.3.1 | 브라우저 DOM 마운팅 |
| `axios` | 1.6.8 | 백엔드 REST API HTTP 통신 |
| `vite` | 5.2.11 | 개발 서버 및 번들링 도구 |
| `@vitejs/plugin-react` | 4.3.0 | Vite에서 JSX/React HMR 지원 |

---

## 3. 프로젝트 폴더 구조

```
pdf-generator/
├── backend/                                      # Spring Boot 백엔드
│   ├── src/main/java/com/example/pdfgen/
│   │   ├── PdfGeneratorApplication.java          # 스프링 부트 진입점(main)
│   │   ├── controller/
│   │   │   └── CertificateController.java        # REST API 엔드포인트 정의
│   │   ├── domain/
│   │   │   ├── CertificateHistory.java           # 발급 이력 JPA 엔티티
│   │   │   └── CertificateSerialMapping.java     # 시리얼 번호 매핑 엔티티
│   │   ├── dto/
│   │   │   └── CertificateRequest.java           # 프론트엔드 요청 데이터 모델
│   │   ├── repository/
│   │   │   └── CertificateHistoryRepository.java # DB CRUD 인터페이스
│   │   ├── service/
│   │   │   └── PdfGenerationService.java         # A4 PDF 생성 핵심 로직
│   │   └── util/
│   │       └── ResourceInitializer.java          # 폰트/이미지 리소스 자동 준비
│   └── src/main/resources/
│       ├── application.yml                       # DB 연결 및 서버 설정
│       ├── fonts/
│       │   └── NanumGothic.ttf                   # 한글 폰트 (PDF 삽입용)
│       └── images/
│           ├── logo.png                          # 회사 로고 이미지
│           ├── signature.png                     # 교정 기술자 서명 이미지
│           └── stamp.png                         # 회사 직인 이미지
│
├── frontend/                                     # React 프론트엔드
│   └── src/
│       ├── App.jsx                               # 최상위 진입 컴포넌트 (레이아웃)
│       ├── App.css                               # 전체 UI 스타일시트
│       ├── main.jsx                              # React 앱 DOM 마운팅
│       └── components/
│           └── CertificateForm.jsx              # 입력 폼 + API 호출 로직
│
└── OP_성적서발행기.bat                            # 백엔드+프론트엔드 동시 실행 배치파일
```

---

## 4. 핵심 기능 설명

### 4-1. PDF 성적서 자동 생성
- 사용자가 4가지 정보를 입력하면 A4 1페이지짜리 교정 성적서 PDF를 자동으로 만들어 다운로드합니다.
- PDF 레이아웃은 OpenPDF 라이브러리를 사용하여 코드로 직접 그립니다.
- 한글 출력을 위해 나눔고딕(NanumGothic.ttf) 폰트를 PDF 파일 내부에 임베드합니다.
- 회사 로고, 기술자 서명, 회사 직인 이미지(PNG)를 PDF에 자동 삽입합니다.

### 4-2. Certificate NO. 구성 방식
- 사용자가 번호를 직접 입력하지 않아도 됩니다.
- **형식**: `OP` + Certificate Date(8자리 숫자) + 일련번호(4자리)
  - 예시: `OP202605210001`
- **번호 규칙**: 발급 건마다 항상 `0001`부터 시작합니다. 같은 날짜라도 시리얼 번호가 다르면 새 발급 건으로 취급하여 `0001`부터 시작합니다.
  - 예: `123A12 123A13 123A14` 3개 발급 → Certificate NO. `0001` / `0002` / `0003`
  - 이후 `456A12` 1개 발급 → Certificate NO. `0001` (새 발급 건이므로 처음부터 시작)

### 4-3. 다중 시리얼 번호 처리
- `Serial No.` 입력칸에 시리얼 번호를 **스페이스(공백)로 구분하여 여러 개** 입력하면, 시리얼 개수만큼 PDF를 여러 페이지로 자동 생성합니다.
- 같은 발급 건 내에서 각 페이지마다 Certificate NO. 일련번호가 하나씩 증가합니다.
  - 예: 시리얼 3개 입력 → 페이지 1은 `0001`, 페이지 2는 `0002`, 페이지 3은 `0003`

### 4-4. 입력 날짜 자동 정규화
- 날짜 입력 시 `20260521` 형식(하이픈 없이 8자리)으로 입력해도 자동으로 `2026-05-21` 형식으로 변환해서 처리합니다.

### 4-5. 발급 이력 데이터베이스 저장
- 성적서 PDF가 정상적으로 생성된 경우에만 MySQL DB에 발급 이력을 저장합니다. (PDF 생성 실패 시 DB 저장 안 함)
- 저장 정보: Certificate NO., Certificate Date(발행일), Calibration Date(교정일), Expiry Date(만료일), 발급 시각, 시리얼 번호 목록 + 페이지 번호

### 4-6. 입력값 유효성 검사 및 완전 중복 방지
- 프론트엔드와 백엔드 양쪽 모두에서 아래 항목을 검증합니다.
  - 필수 입력 항목 누락 여부
  - 날짜 형식 오류 (YYYY-MM-DD 또는 YYYYMMDD만 허용)
  - 날짜 논리 오류 (만료일이 발행일/교정일보다 이른 경우)
  - Serial No. 미입력
- **완전 중복 발급 방지**: 4가지 입력값(Certificate Date + Calibration Date + Expiry Date + Serial No. 목록)이 이전 발급 이력과 완전히 동일한 경우 Warning 메시지를 표시하고 PDF 생성을 차단합니다.

---

## 5. 데이터베이스 테이블 구조

### `certificate_history` (발급 이력 테이블)
| 컬럼명 | 타입 | 설명 |
|---|---|---|
| `id` | BIGINT (PK, AUTO) | 기본 키 |
| `certificate_no` | VARCHAR(50), UNIQUE | 최종 발급 번호 (예: OP202605210001) |
| `certificate_date` | DATE | 발행일 |
| `calibration_date` | DATE | 교정일 |
| `expiry_date` | DATE | 만료일 |
| `created_date` | DATETIME | 서버에서 발급한 실제 시각 |

### `certificate_serial_mapping` (시리얼-페이지 매핑 테이블)
| 컬럼명 | 타입 | 설명 |
|---|---|---|
| `id` | BIGINT (PK, AUTO) | 기본 키 |
| `certificate_history_id` | BIGINT (FK) | `certificate_history.id` 참조 |
| `serial_no` | VARCHAR | 제품 시리얼 번호 |
| `page_number` | INT | PDF 내 페이지 번호 (1부터 시작) |

---

## 6. REST API 명세

### POST `/api/documents/certificates/pdf`
성적서 PDF를 생성하여 바이너리 파일로 응답합니다.

**요청 (Request Body - JSON)**
```json
{
  "certificateDate": "2026-05-21",
  "calibrationDate": "2026-05-20",
  "expiryDate": "2027-05-20",
  "serialNos": ["SN001", "SN002"]
}
```

**성공 응답 (200 OK)**
- Content-Type: `application/pdf`
- PDF 파일 바이너리 스트림

**실패 응답 (400 Bad Request / 500 Internal Server Error)**
```json
{
  "message": "에러 원인 메시지"
}
```

---

## 7. PDF 성적서 레이아웃 구성 (10개 섹션)

PDF는 A4 용지 크기(595 × 842pt)에 좌우 여백 30pt, 상하 여백 25pt로 구성되며, 아래 10개 섹션이 순서대로 배치됩니다.

| 순서 | 섹션 내용 |
|---|---|
| 1 | 회사 로고 (logo.png) |
| 2 | 타이틀 배너 ("Calibration Certificate", 파란색 배경) |
| 3 | Certificate NO. 및 발행일(Certificate Date) 정보 표 |
| 4 | 기준 장비(Reference Instrumentation) 표 |
| 5 | 인증 문단 (Optilo Co., Ltd. certifies that...) |
| 6 | 제품 정보(Product Information) 표 (시리얼, 교정일, 만료일 등) |
| 7 | 교정 결과(Calibration Results) 헤더 행 |
| 8 | 교정 결과 데이터 표 (Validation Points / Tolerance / Result) |
| 9 | 서명 표 (Pass/Fail 결과 + 기술자 서명 이미지) |
| 10 | 하단 회사 주소, 연락처, 회사 직인(stamp.png) |

---

## 8. 실행 방법

### 일반 실행 (배치파일 사용)
프로젝트 루트의 `OP_성적서발행기.bat` 파일을 더블클릭하면 백엔드와 프론트엔드 서버가 동시에 실행됩니다.

### 수동 실행 (개발 시)
```bash
# 백엔드 실행 (터미널 1)
cd pdf-generator/backend
./mvnw clean compile spring-boot:run

# 프론트엔드 실행 (터미널 2)
cd pdf-generator/frontend
npm run dev
```

### 접속 주소
- 프론트엔드(사용자 화면): `http://localhost:5173`
- 백엔드 API: `http://localhost:8080`

---

## 9. 이미지 자산 교체 방법

서명, 직인, 로고 이미지를 직접 교체하고 싶을 때는 아래 경로의 파일을 동일한 파일명으로 덮어쓰면 됩니다.

| 파일 | 경로 | 설명 |
|---|---|---|
| `logo.png` | `backend/src/main/resources/images/logo.png` | 회사 로고 |
| `signature.png` | `backend/src/main/resources/images/signature.png` | 기술자 서명 |
| `stamp.png` | `backend/src/main/resources/images/stamp.png` | 회사 직인 |

> **주의**: 이미지 파일을 교체한 후에는 백엔드 서버를 재기동해야 변경된 이미지가 PDF에 적용됩니다.

---

## 10. 주요 개발 이슈 해결 기록

| 이슈 | 원인 | 해결 |
|---|---|---|
| 한글 폰트 PDF에서 깨짐 | 기본 폰트는 한글 미지원 | NanumGothic.ttf를 PDF에 직접 임베드 |
| 서명/직인 이미지 잘림 | 셀 너비 계산 오류 (DPI 보정 미처리) | DPI 기반 자연 크기 계산 후 `scaleAbsolute` 적용 |
| PDF 2페이지 넘침 | 섹션 간격(spacingAfter) 합계가 A4 가용 높이(792pt) 초과 | 각 섹션 간격 상수를 조정하여 총 높이 792pt 이하로 제어 |
| 완전 동일 정보로 중복 발급 | 다른 Serial No. 입력 시 번호 누적으로 중복 구분 불가 | 날짜 3개 + 시리얼 목록이 완전히 동일하면 Warning 표시 후 차단 |
| DB 저장 후 PDF 생성 실패 시 데이터 오염 | 저장 → 생성 순서로 처리 | **생성 성공 후 저장** 순서로 변경하여 원천 차단 |
