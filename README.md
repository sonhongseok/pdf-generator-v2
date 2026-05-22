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
| `poi-tl` | 1.12.2 | MS Word(`docx`) 템플릿 기반 데이터 치환 라이브러리 |
| `documents4j` | 1.1.7 | MS Word COM 자동화를 통한 `.docx` → `.pdf` 완벽 변환 |
| `pdfbox` | 2.0.30 | 다중 페이지 PDF 병합 처리 |

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
├── certificate_template.docx                     # 성적서 MS Word 원본 템플릿 (외부 분리)
├── backend/                                      # Spring Boot 백엔드
│   ├── src/main/java/com/example/pdfgen/
│   │   ├── controller/
│   │   │   └── CertificateController.java        # REST API 엔드포인트 정의
│   │   ├── domain/
│   │   │   └── CertificateHistory.java           # 발급 이력 JPA 엔티티
│   │   ├── service/
│   │   │   ├── PdfGenerationService.java         # 성적서 발급 및 병합 오케스트레이션
│   │   │   ├── DocxTemplateService.java          # Word 템플릿 치환 및 표 정렬 후처리
│   │   │   └── LibreOfficePdfConverter.java      # documents4j 기반 PDF 변환 모듈
│   │   └── util/
│   └── src/main/resources/
│       └── application.yml                       # DB 연결 및 서버 설정
│
├── frontend/                                     # React 프론트엔드
│   └── src/
│       └── components/
│           └── CertificateForm.jsx              # 입력 폼 + 유효성 검증 + API 호출
│
└── OP_성적서발행기.bat                            # 백엔드+프론트엔드 동시 실행 배치파일
```

---

## 4. 핵심 기능 설명

### 4-1. Word 템플릿 기반 PDF 성적서 자동 생성
- 사용자가 4가지 정보를 입력하면 MS Word 템플릿(`certificate_template.docx`)에 데이터를 자동으로 치환한 뒤, PDF로 변환하여 다운로드합니다.
- 코드로 표를 직접 그리는 방식(하드코딩)에서 벗어나 **MS Word 템플릿 방식**을 도입하여, 비개발자도 Word에서 직접 폰트와 레이아웃을 수정할 수 있도록 설계되었습니다.
- `documents4j` 라이브러리를 통해 PC에 설치된 MS Word를 직접 호출하므로, 서식이나 레이아웃의 깨짐 없이 완벽한 품질의 PDF가 생성됩니다.

### 4-2. Certificate NO. 자동 증가 구성 방식
- 사용자가 번호를 직접 입력하지 않아도 됩니다.
- **형식**: `OP` + Certificate Date(8자리 숫자) + 일련번호(4자리) (예: `OP202605210001`)
- **번호 규칙**: 같은 날짜에 발급되는 여러 시리얼 번호는 내부 페이지 순서대로 끝 4자리가 `0001`부터 순차적으로 증가합니다.

### 4-3. 다중 시리얼 번호 일괄 처리
- `Serial No.` 칸에 시리얼을 **공백(스페이스)으로 구분하여 여러 개** 입력하면, 시리얼 개수만큼 PDF 페이지가 자동 생성되어 하나의 파일로 병합됩니다.

### 4-4. 발급 이력 데이터베이스 저장 및 중복 방지 로직
- 생성된 발급 이력은 MySQL DB에 자동 저장됩니다.
- **완전 중복 발급 방지**: 동일한 발행일, 교정일, 만료일과 시리얼 번호의 조합이 DB에 이미 존재할 경우 HTTP 400 에러를 발생시키며, 프론트엔드 화면에 Warning 메시지로 차단합니다.
- **만료일 논리 검증**: 만료일이 발행일/교정일보다 이른 '과거'로 설정될 경우, 서버로 데이터를 보내기 전에 프론트엔드에서 즉시 폼 제출을 차단합니다.

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
| `created_date` | DATETIME | 발급 처리된 실제 시간 |

### `certificate_serial_mapping` (시리얼-페이지 매핑 테이블)
| 컬럼명 | 타입 | 설명 |
|---|---|---|
| `id` | BIGINT (PK, AUTO) | 기본 키 |
| `certificate_history_id` | BIGINT (FK) | 부모 발급 이력 참조 |
| `serial_no` | VARCHAR | 제품 시리얼 번호 |
| `page_number` | INT | 문서 내 페이지 인덱스 |

---

## 6. 성적서 템플릿 수정 방법

성적서의 디자인, 문구, 표 크기 등을 변경하고 싶을 때는 코드를 수정할 필요 없이 바깥으로 분리된 템플릿 파일만 수정하면 됩니다.
(서버가 켜져 있는 상태이거나, 실제 서버에 배포된 이후에도 이 파일만 교체하면 즉시 반영됩니다.)

1. 프로젝트 최상단의 `pdf-generator/certificate_template.docx` 파일을 MS Word로 엽니다.
2. 원하는 레이아웃, 폰트 크기, 문구, 로고 이미지 등을 자유롭게 수정합니다.
3. 변수가 들어가야 할 자리는 반드시 `{cno}`, `{sno}`, `{cedate}`와 같이 중괄호 태그 형태를 유지해야 합니다.
4. 파일을 저장하면, 다음 번 PDF 생성 시점부터 즉시 새로운 양식이 적용됩니다. (서버 재시작 불필요)

---

## 7. 주요 트러블슈팅 및 개선 기록

| 이슈 | 원인 | 해결 |
|---|---|---|
| **오픈소스 PDF 레이아웃 한계** | OpenPDF(iText) 하드코딩 방식으로 표를 그릴 때 한글/영문 높이가 안 맞고 유지보수가 불가능함 | **Word 템플릿 기반 방식(`poi-tl` + `documents4j`)으로 아키텍처 전면 개편** |
| **완전 동일 정보로 중복 발급** | 동일한 시리얼을 넣어도 무한 발급 가능 | 날짜 3개 + 시리얼 목록이 완전히 동일하면 DB 단에서 대조 후 400 Bad Request 에러 반환 및 Warning 표시 |
| **만료일 과거 설정 오류** | 날짜 간의 선후 관계 검증 로직 누락 | 프론트엔드에서 폼 제출 전 만료일 논리 검증 로직 추가 |
| **템플릿 표 텍스트 수직 쏠림** | Word 표 내부의 패딩(상하 여백) 설정과 중첩 표(Nested Table) 인식 한계 | `DocxTemplateService`에 재귀 탐색 함수를 도입하여 표 안의 모든 셀을 찾아내어 완벽한 수직/수평 중앙 정렬 적용 |
| **템플릿 치환 후 글자색 잔존** | 템플릿 변수에 있던 주황/파란색이 결과물에도 그대로 남는 현상 | `XWPFRun.setColor("000000")`를 통해 문서 내 모든 글자색을 검정색으로 강제 덮어쓰기 하는 후처리 로직 구현 |
