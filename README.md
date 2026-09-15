# 안전보건 법령·고시 개정 추적 시스템 v3 (Java)

회사가 보유한 안전보건 절차서·지침서·작업표준의 근거가 되는 대한민국 법령과
행정규칙(고시·예규·훈령)의 개정 여부를 자동으로 추적하고, 어떤 회사 문서가
영향을 받는지 한눈에 파악할 수 있는 웹 대시보드입니다.

기능 설명은 [README_v2.md](./README_v2.md)를 참고하세요. 기능은 그대로이고,
v3 는 **동작 환경만 Java 로 바꾼 것**입니다.

## 실행 환경

| 항목 | 버전 |
| --- | --- |
| Java | JDK 17 |
| 프레임워크 | Spring Boot 3.5.15 |
| 데이터베이스 | Oracle Database 10g (10.2.0.4) |
| DB 접근 | MyBatis 3 |

## 시작하기

### 1. 데이터베이스 준비

`src/main/resources/db/schema-oracle.sql` 을 대상 계정으로 실행해 테이블과
시퀀스를 만듭니다.

### 2. 접속 정보 설정

접속 정보는 소스에 적지 말고 환경변수로 넘깁니다.

| 환경변수 | 설명 |
| --- | --- |
| `DB_URL` | 예: `jdbc:oracle:thin:@서버주소:1521:SID` |
| `DB_USERNAME` | DB 계정 |
| `DB_PASSWORD` | DB 비밀번호 |
| `LAW_API_OC` | 국가법령정보 API 인증키. 비우면 예시 데이터로 동작 |
| `DASHBOARD_USERNAME` / `DASHBOARD_PASSWORD` | 둘 다 채우면 화면 접속 시 로그인을 요구 |
| `SERVER_PORT` | 기본 8000 |

### 3. 실행

```bash
mvn clean package
java -jar target/safety-law-monitor-3.0.0.jar
```

Windows 는 `run-server.bat`, Linux·macOS 는 `run-server.sh` 를 써도 됩니다.
브라우저에서 `http://localhost:8000` 으로 접속합니다.

### Oracle 없이 화면만 확인하기

메모리 DB(H2)로 띄워 기능을 둘러볼 수 있습니다. 데이터는 종료하면 사라집니다.

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

## Oracle 10g 에서 주의할 점

대상 DB 가 Oracle 이 공식 지원을 종료한 10.2 버전이라, 최신 환경을 전제로 한
구성을 그대로 쓸 수 없습니다. 아래는 그 때문에 내린 결정들입니다.

**JPA 대신 MyBatis 를 씁니다.** Spring Boot 3.x 가 쓰는 Hibernate 6 는
Oracle 10g 지원을 중단했습니다. SQL 을 직접 작성하는 MyBatis 를 써야
10g 문법에 맞출 수 있습니다.

**JDBC 드라이버 선택이 이 프로젝트의 가장 큰 변수입니다.** 최신 드라이버는
10.2 서버로의 접속을 거부하며 `ORA-28040` 오류를 냅니다. `pom.xml` 의
`ojdbc.version` 주석에 선택 근거와 대안을 적어 두었습니다. 실제 접속 가능
여부는 아래 도구로 먼저 확인하세요.

```bash
java -cp ojdbc8.jar tools/OracleConnectionTest.java <접속주소> <계정> <비밀번호>
```

접속 성공 여부뿐 아니라 문자셋, 한글 저장, 페이징 구문 지원 여부까지 함께
점검해 출력합니다. 이 출력을 그대로 전달하면 다음 단계를 판단할 수 있습니다.

**스키마에 반영한 10g 제약**

- 기본키 자동 증가(IDENTITY)가 없어 시퀀스로 채번합니다
- `BOOLEAN` 타입이 없어 `NUMBER(1)` 에 0/1 로 저장합니다
- 객체 이름이 최대 30자라 모든 테이블·컬럼·제약 이름을 그 안에서 지었습니다
- 문자열 길이 단위가 기본 바이트라, 한글이 잘리지 않도록 모든 문자열 컬럼에
  `VARCHAR2(n CHAR)` 로 글자 단위를 명시했습니다
- `OFFSET`/`FETCH` 구문이 없어 건수 제한은 `ROWNUM` 으로 처리합니다
- 빈 문자열이 `NULL` 로 저장되므로, 설정값이 "비워서 저장한 것"인지
  "저장한 적 없는 것"인지를 값이 아니라 행의 존재 여부로 구분합니다

## 프로젝트 구조

```
src/main/java/com/safetylaw/monitor/
  config/     설정, 보안, 스케줄러, HTTP 클라이언트
  domain/     테이블에 대응하는 객체
  mapper/     MyBatis 매퍼 (SQL 은 resources/mapper/*.xml)
  lawapi/     국가법령정보 API 클라이언트
  service/    동기화, 본문 캐시, 뉴스, 메일, 대시보드
  web/        REST 컨트롤러
  dto/        화면에 오가는 값
src/main/resources/
  db/         스키마 (Oracle / 개발용 H2)
  mapper/     SQL
  static/     화면 (HTML·CSS·JS)
tools/        Oracle 접속 점검 도구
```

## v2(Python) 파일에 대하여

이 저장소에는 참고용으로 v2 의 Python 구현이 아직 남아 있습니다
(`backend/`, `scripts/`, `installer/`, `demo/`, `run.bat`, `run.ps1`,
`render.yaml`). Java 버전 동작에는 쓰이지 않으므로, 더 참고할 필요가 없어지면
지워도 됩니다.
