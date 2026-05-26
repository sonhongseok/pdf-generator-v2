# 📋 OP 성적서 발행기 — 개발 설명서

> 주식회사 옵티로(Optilo Co., Ltd.)의 내부 교정 성적서(Calibration Certificate)를 A4 1페이지 PDF로 자동 생성하고 다운로드하는 웹 기반 내부 업무 도구입니다.

> [!NOTE]
> 사용자(비개발자) 대상 안내는 **[사용설명서.md](./사용설명서.md)** 를 참고하세요.

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

### 실행 환경 필수 조건
| 항목 | 이유 |
|---|---|
| Microsoft Word 설치 | `documents4j`가 Windows COM 자동화를 통해 Word를 직접 실행하여 `.docx` → `.pdf` 변환 수행 |
| MySQL 서버 실행 중 | 발급 이력 데이터 저장 및 중복 검사에 사용 |

---

## 2. 사용 라이브러리 (Dependencies)

### 백엔드 주요 의존성 (`pom.xml`)

| 라이브러리 | 버전 | 용도 |
|---|---|---|
| `spring-boot-starter-web` | 3.2.5 (Boot 기본) | REST API 서버 구성, HTTP 요청/응답 처리 |
| `spring-boot-starter-data-jpa` | 3.2.5 (Boot 기본) | JPA/Hibernate를 통한 ORM 데이터베이스 연동 |
| `mysql-connector-j` | Boot 관리 버전 | Java ↔ MySQL 연결 드라이버 |
| `poi-ooxml` | 5.2.5 | Word XML 구조 직접 조작 (정렬, 색상 후처리) |
| `poi-tl` | 1.12.2 | MS Word(`docx`) 템플릿 기반 데이터 치환 라이브러리 |
| `documents4j-local` | 1.1.7 | MS Word COM 자동화를 통한 `.docx` → `.pdf` 완벽 변환 |
| `documents4j-transformer-msoffice-word` | 1.1.7 | `documents4j` Word 변환 플러그인 |
| `pdfbox` | 2.0.30 | 다중 페이지 PDF 병합 처리 |
| `h2` | Boot 관리 버전 | 테스트 전용 인메모리 DB (`scope: test`) |
| `spring-boot-starter-test` | 3.2.5 (Boot 기본) | JUnit 5, Mockito, MockMvc 통합 테스트 환경 |

### 프론트엔드 주요 의존성 (`package.json`)

| 라이브러리 | 버전 | 용도 |
|---|---|---|
| `react` | 18.3.1 | UI 컴포넌트 렌더링 |
| `react-dom` | 18.3.1 | 브라우저 DOM 마운팅 |
| `axios` | 1.6.8 | 백엔드 REST API HTTP 통신 |
| `vite` | 5.2.11 | 개발 서버 및 번들링 도구 |

---

## 3. 프로젝트 폴더 구조

```
pdf-generator/
├── certificate_template.docx                     # 성적서 MS Word 원본 템플릿 (외부 분리됨)
├── 사용설명서.md                                   # 사용자(비개발자) 대상 사용 설명서
├── README.md                                      # 현재 문서 (개발자 대상)
├── OP_성적서발행기.bat                              # 백엔드+프론트엔드 동시 실행 배치파일
│
├── backend/                                       # Spring Boot 백엔드
│   ├── pom.xml                                    # Maven 의존성 관리
│   ├── mvnw.cmd                                   # Maven Wrapper (Windows)
│   └── src/
│       ├── main/
│       │   ├── java/com/example/pdfgen/
│       │   │   ├── PdfGeneratorApplication.java   # 애플리케이션 진입점
│       │   │   ├── controller/
│       │   │   │   └── CertificateController.java # REST API 엔드포인트, 비즈니스 흐름 제어
│       │   │   ├── domain/
│       │   │   │   ├── CertificateHistory.java    # 발급 이력 JPA 엔티티
│       │   │   │   └── CertificateSerialMapping.java # 시리얼-페이지 매핑 JPA 엔티티
│       │   │   ├── dto/
│       │   │   │   └── CertificateRequest.java    # API 요청 바디 DTO
│       │   │   ├── repository/
│       │   │   │   └── CertificateHistoryRepository.java # JPA Repository (중복 조회 쿼리 포함)
│       │   │   ├── service/
│       │   │   │   ├── DocxTemplateService.java   # poi-tl 기반 템플릿 치환 + 정렬/색상 후처리
│       │   │   │   └── MsWordPdfConverter.java    # documents4j 기반 docx→PDF 변환 및 PDF 병합
│       │   │   └── util/
│       │   │       └── ResourceInitializer.java   # 기동 시 폰트/이미지 리소스 초기화
│       │   └── resources/
│       │       ├── application.yml                # DB 연결, 서버 포트, 템플릿 경로 설정
│       │       ├── fonts/
│       │       │   └── NanumGothic.ttf            # 한글 폰트 (리소스)
│       │       ├── images/
│       │       │   ├── logo.png                   # 로고 이미지
│       │       │   ├── signature.png              # 서명 이미지
│       │       │   └── stamp.png                  # 직인 이미지
│       │       └── templates/
│       │           └── certificate_template.docx  # 내부 폴백용 템플릿 (실제 사용 시 루트 파일 우선)
│       └── test/
│           └── java/com/example/pdfgen/
│               ├── controller/
│               │   └── CertificateControllerTest.java    # API 유효성 검증, 중복 차단, 정상 발급 테스트
│               ├── repository/
│               │   └── CertificateHistoryRepositoryTest.java # DB 저장, 날짜 조건 조회 테스트
│               └── service/
│                   └── DocxTemplateServiceTest.java       # 외부 템플릿 파일 로딩, 치환 테스트
│
└── frontend/                                      # React 프론트엔드
    ├── package.json
    ├── vite.config.js                             # Vite 설정 (API 프록시: /api → localhost:8080)
    ├── index.html
    └── src/
        ├── main.jsx
        ├── App.jsx                                # 앱 진입점, 창 레이아웃(메뉴바 포함)
        ├── App.css                                # 전체 UI 스타일 (Windows 클래식 테마)
        └── components/
            └── CertificateForm.jsx               # 입력 폼, 유효성 검증, API 호출, 다운로드 처리
```

---

## 4. 핵심 기능 설명

### 4-1. Word 템플릿 기반 PDF 성적서 자동 생성
- 사용자가 4가지 정보를 입력하면 MS Word 템플릿(`certificate_template.docx`)에 데이터를 자동으로 치환한 뒤 PDF로 변환하여 다운로드합니다.
- `documents4j` 라이브러리를 통해 PC에 설치된 MS Word를 직접 호출하므로, 서식이나 레이아웃의 깨짐 없이 완벽한 품질의 PDF가 생성됩니다.
- 코드로 표를 직접 그리는 방식(하드코딩)에서 벗어나 **MS Word 템플릿 방식**을 도입, 비개발자도 Word에서 직접 폰트와 레이아웃을 수정할 수 있도록 설계되었습니다.

### 4-2. 외부 경로 템플릿 분리 (`app.template.path`)
- 템플릿 파일은 `application.yml`의 `app.template.path` 설정값으로 경로를 지정합니다.
- 기본값은 `../certificate_template.docx`로, 백엔드 폴더 상위(= 프로젝트 루트)의 파일을 참조합니다.
- 서버를 재시작하지 않아도 이 파일만 수정하면 **즉시 반영**됩니다.

```yaml
# application.yml
app:
  template:
    path: ../certificate_template.docx
```

### 4-3. Certificate NO. 자동 증가 구성 방식
- **형식**: `OP` + Certificate Date(8자리 숫자) + 일련번호(4자리) (예: `OP202605260001`)
- **번호 규칙**: 같은 날짜에 발급되는 여러 시리얼 번호는 페이지 순서대로 끝 4자리가 `0001`부터 순차적으로 증가합니다.

### 4-4. 다중 시리얼 번호 일괄 처리
- `Serial No.` 칸에 공백으로 구분하여 여러 개 입력하면, 시리얼 개수만큼 PDF 페이지가 자동 생성되어 하나의 PDF 파일로 병합됩니다.
- PDF 병합은 `Apache PDFBox`를 사용합니다.

### 4-5. 발급 이력 데이터베이스 저장 및 중복 방지 로직
- 성공적으로 PDF가 생성된 후에만 DB 저장이 수행됩니다. (PDF 생성 실패 시 DB에 잔존 데이터가 남지 않음)
- **완전 중복 방지**: 동일한 발행일, 교정일, 만료일 + 시리얼 번호 목록이 완전히 동일하면 HTTP 400 에러로 차단합니다.
- **만료일 논리 검증**: 만료일이 발행일보다 이른 경우, 프론트엔드와 백엔드 양쪽에서 모두 차단합니다.

### 4-6. 템플릿 후처리 (색상 및 정렬 정규화)
- `DocxTemplateService`는 데이터 치환 이후 아래 두 가지 후처리를 재귀적으로 수행합니다.
  - **색상 강제 덮어쓰기**: 템플릿의 `{변수}` 태그에 지정된 주황색·파란색 글자색을 모두 검정(#000000)으로 변환
  - **수직/수평 정렬 통일**: 중첩 표(Nested Table)를 포함한 모든 셀의 정렬을 `CENTER`로 통일

---

## 5. API 명세

### POST `/api/documents/certificates/pdf`

성적서 PDF를 생성하고 바이너리 파일로 응답합니다.

**Request Body** (`application/json`)

```json
{
  "certificateDate": "2026-05-26",
  "calibrationDate": "2026-05-20",
  "expiryDate": "2027-05-20",
  "serialNos": ["SN-001", "SN-002"]
}
```

**Response (성공)** — HTTP 200

```
Content-Type: application/pdf
Content-Disposition: attachment; filename="OP202605260001.pdf"

<binary PDF data>
```

**Response (실패)** — HTTP 400

```json
{ "message": "오류 메시지 내용" }
```

**주요 유효성 검사 목록**

| 조건 | HTTP 상태 | 메시지 |
|---|---|---|
| Certificate Date 누락 | 400 | Certificate Date는 필수 선택 항목입니다. |
| Expiry Date 누락 | 400 | Expiry Date는 필수 선택 항목입니다. |
| Serial No 목록 비어 있음 | 400 | Serial NO는 최소 하나 이상 입력되어야 합니다. |
| 날짜 포맷 오류 | 400 | 날짜 포맷이 올바르지 않습니다. YYYY-MM-DD 형식이어야 합니다. |
| 만료일 < 발행일 | 400 | 만료일(Expiry Date)은 발행일(Certificate Date)보다 빠를 수 없습니다. |
| 완전 중복 발급 | 400 | 동일한 발행일, 교정일, 만료일, 시리얼 번호로 이미 발급된 성적서가 존재합니다. |
| 서버 내부 오류 | 500 | 서버 내부 에러로 PDF 생성을 진행할 수 없습니다. |

---

## 6. 데이터베이스 테이블 구조

### `certificate_history` (발급 이력 테이블)
| 컬럼명 | 타입 | 설명 |
|---|---|---|
| `id` | BIGINT (PK, AUTO) | 기본 키 |
| `certificate_no` | VARCHAR(50), UNIQUE | 최종 발급 번호 (예: OP202605260001) |
| `certificate_date` | DATE | 발행일 |
| `calibration_date` | DATE | 교정일 |
| `expiry_date` | DATE | 만료일 |
| `created_date` | DATETIME | 발급 처리된 실제 시간 |

### `certificate_serial_mapping` (시리얼-페이지 매핑 테이블)
| 컬럼명 | 타입 | 설명 |
|---|---|---|
| `id` | BIGINT (PK, AUTO) | 기본 키 |
| `certificate_history_id` | BIGINT (FK) | 부모 발급 이력 참조 (`certificate_history.id`) |
| `serial_no` | VARCHAR | 제품 시리얼 번호 |
| `page_number` | INT | 문서 내 페이지 인덱스 (1부터 시작) |

---

## 7. 단위 테스트

`./mvnw test` 명령으로 전체 테스트를 실행합니다.

```
./mvnw test
```

### 테스트 구성 (총 8개, 전체 통과)

| 테스트 클래스 | 어노테이션 | 주요 검증 항목 |
|---|---|---|
| `CertificateHistoryRepositoryTest` | `@DataJpaTest` + MySQL 실제 DB | 발급 이력 저장, 날짜 조건 조회 |
| `CertificateControllerTest` | `@WebMvcTest` + Mockito | 필수값 검증, 날짜 논리 오류, 중복 차단, 정상 PDF 응답 |
| `DocxTemplateServiceTest` | `@SpringBootTest` | 외부 경로 템플릿 로딩, 변수 치환, docx 바이너리 반환 |

> [!NOTE]
> `CertificateHistoryRepositoryTest`는 `@AutoConfigureTestDatabase(replace = NONE)` 설정으로 H2가 아닌 실제 MySQL DB를 사용합니다. 테스트 실행 전 MySQL 서버가 켜져 있어야 합니다.

---

## 8. 로컬 개발 서버 시작

```bash
# 백엔드
cd backend
./mvnw spring-boot:run

# 프론트엔드 (별도 터미널)
cd frontend
npm install   # 최초 1회만
npm run dev
```

브라우저에서 `http://localhost:5173` 접속

> [!NOTE]
> Vite 개발 서버는 `/api` 경로를 `http://localhost:8080`으로 프록시합니다. (`vite.config.js` 설정)

---

## 9. 성적서 템플릿 수정 방법

성적서의 디자인, 문구, 표 크기 등을 변경할 때는 코드 수정 없이 Word 파일만 수정합니다.

1. 프로젝트 루트의 `certificate_template.docx` 파일을 MS Word로 엽니다.
2. 레이아웃, 폰트, 문구, 로고 이미지 등을 자유롭게 수정합니다.
3. 아래 변수 태그는 **반드시 유지**해야 합니다.

| 태그 | 치환되는 내용 |
|---|---|
| `{cno}` | Certificate NO. |
| `{sno}` | Serial No. |
| `{cedate}` | Certificate Date (발행일) |
| `{pdate}` | Calibration Date (교정일) |
| `{edate}` | Expiry Date (만료일) |

4. 저장 후 **서버 재시작 없이** 다음 번 발급 시 즉시 반영됩니다.

---

## 10. 주요 트러블슈팅 및 개선 기록

| 이슈 | 원인 | 해결 |
|---|---|---|
| **오픈소스 PDF 레이아웃 한계** | OpenPDF(iText) 하드코딩 방식으로 표를 그릴 때 한글/영문 높이가 맞지 않고 유지보수 불가 | **Word 템플릿 기반 방식(`poi-tl` + `documents4j`)으로 아키텍처 전면 개편** |
| **완전 동일 정보로 중복 발급** | 동일한 시리얼을 넣어도 무한 발급 가능 | 날짜 3개 + 시리얼 목록이 완전히 동일하면 DB 대조 후 400 에러 반환 |
| **만료일 과거 설정 오류** | 날짜 간 선후 관계 검증 로직 누락 | 프론트엔드에서 폼 제출 전 만료일 논리 검증 로직 추가 |
| **템플릿 표 텍스트 수직 쏠림** | Word 표 내부 패딩 설정과 중첩 표(Nested Table) 인식 한계 | `DocxTemplateService`에 재귀 탐색 함수 도입, 모든 셀 수직/수평 중앙 정렬 강제 적용 |
| **치환 후 글자색 잔존** | 템플릿 변수의 주황/파란색이 결과물에도 그대로 남음 | `XWPFRun.setColor("000000")`으로 모든 글자색 검정색 강제 덮어쓰기 |
| **IConverter is not AutoCloseable** | `documents4j` 버전에서 `try-with-resources` 미지원 | `try-finally` 블록으로 변경하여 `converter.shutDown()` 명시적 호출 |
| **템플릿 ClassPath 의존성** | 배포 후 JAR 내부의 파일을 수정할 수 없음 | `ClassPathResource` → `FileSystemResource`로 전환, `app.template.path` 외부 설정으로 분리 |
| **`LibreOfficePdfConverter` 이름 혼선** | PDF 엔진을 documents4j(MS Word)로 교체했으나 클래스명이 `LibreOffice`로 남아 있음 | `MsWordPdfConverter`로 파일명과 클래스명 일괄 리팩토링 |
