-- =====================================================================
--  안전보건 법령·고시 개정 추적 시스템 v3
--  Oracle Database 10g (10.2.0.4) 전용 스키마
-- =====================================================================
--
--  10g 제약 때문에 최신 Oracle과 다르게 작성한 부분:
--
--   1. IDENTITY 컬럼이 없음(12c부터 지원)  -> SEQUENCE 로 채번한다.
--      트리거 대신 애플리케이션(MyBatis selectKey)에서 NEXTVAL 을 먼저
--      읽어 INSERT 하므로, 여기서는 시퀀스만 만들어 둔다.
--
--   2. BOOLEAN 타입이 없음 -> NUMBER(1) + CHECK (0/1) 로 표현한다.
--
--   3. 객체 이름이 최대 30자 -> 모든 테이블/컬럼/제약 이름을 30자 이하로
--      맞췄다. (12.2부터 128자) 이름을 바꿀 때 이 제한을 반드시 지킬 것.
--
--   4. VARCHAR2 의 길이 단위가 기본 BYTE -> DB 문자셋이 AL32UTF8 인 경우
--      한글 1자가 3바이트라 VARCHAR2(255)에는 한글 85자밖에 안 들어간다.
--      그래서 모든 문자열 컬럼에 CHAR 단위를 명시했다. VARCHAR2(n CHAR).
--      단일 컬럼 최대치는 4000 바이트이므로 CHAR 단위 최대는 1333 이다.
--
--   5. 긴 본문(법령 조문 등)은 VARCHAR2 한계를 넘으므로 CLOB 을 쓴다.
--
--   6. Oracle 은 빈 문자열('')을 NULL 로 저장한다. "값 없음"과 "빈 문자열"을
--      구분하지 않으므로 애플리케이션에서 NULL 기준으로 처리한다.
--
--  모든 시각 컬럼은 UTC 기준으로 저장한다(기존 Python 버전과 동일).
--  화면 표시 시점에 KST로 변환한다.
-- =====================================================================


-- ---------------------------------------------------------------------
-- 1. 추적 대상 법령 / 행정규칙
-- ---------------------------------------------------------------------
CREATE TABLE TRACKED_LAWS (
    ID                        NUMBER(19)         NOT NULL,
    SOURCE_TYPE               VARCHAR2(16 CHAR)  NOT NULL,   -- law | admrul
    EXTERNAL_ID               VARCHAR2(64 CHAR)  NOT NULL,   -- 법령일련번호(MST). 개정되면 값이 바뀜
    MASTER_ID                 VARCHAR2(64 CHAR),             -- 법령ID. 개정돼도 고정
    NAME                      VARCHAR2(255 CHAR) NOT NULL,
    CATEGORY                  VARCHAR2(64 CHAR),             -- 법률/시행령/시행규칙/고시/예규/훈령
    DEPARTMENT                VARCHAR2(128 CHAR),
    CURRENT_PROMULGATION_NO   VARCHAR2(64 CHAR),
    CURRENT_PROMULGATION_DATE VARCHAR2(16 CHAR),             -- YYYYMMDD 문자열
    CURRENT_ENFORCEMENT_DATE  VARCHAR2(16 CHAR),
    DETAIL_LINK               VARCHAR2(512 CHAR),
    IS_ACTIVE                 NUMBER(1)  DEFAULT 1 NOT NULL,
    LAST_SYNCED_AT            TIMESTAMP,
    CREATED_AT                TIMESTAMP  DEFAULT SYSTIMESTAMP NOT NULL,
    UPDATED_AT                TIMESTAMP  DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT PK_TRACKED_LAWS      PRIMARY KEY (ID),
    CONSTRAINT UQ_TRACKED_LAWS_SRC  UNIQUE (SOURCE_TYPE, EXTERNAL_ID),
    CONSTRAINT CK_TRACKED_LAWS_ACT  CHECK (IS_ACTIVE IN (0, 1))
);

CREATE SEQUENCE SEQ_TRACKED_LAWS START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

CREATE INDEX IX_TRACKED_LAWS_ACTIVE ON TRACKED_LAWS (IS_ACTIVE);
CREATE INDEX IX_TRACKED_LAWS_MASTER ON TRACKED_LAWS (MASTER_ID);


-- ---------------------------------------------------------------------
-- 2. 개정 이력
-- ---------------------------------------------------------------------
CREATE TABLE LAW_REVISIONS (
    ID                         NUMBER(19)        NOT NULL,
    TRACKED_LAW_ID             NUMBER(19)        NOT NULL,
    PROMULGATION_NO            VARCHAR2(64 CHAR),
    PROMULGATION_DATE          VARCHAR2(16 CHAR),
    ENFORCEMENT_DATE           VARCHAR2(16 CHAR),
    PREVIOUS_PROMULGATION_NO   VARCHAR2(64 CHAR),
    PREVIOUS_PROMULGATION_DATE VARCHAR2(16 CHAR),
    PREVIOUS_ENFORCEMENT_DATE  VARCHAR2(16 CHAR),
    DETECTED_AT                TIMESTAMP         DEFAULT SYSTIMESTAMP NOT NULL,
    REVIEW_STATUS              VARCHAR2(16 CHAR) DEFAULT '미검토' NOT NULL,
    REVIEWER                   VARCHAR2(64 CHAR),
    REVIEWED_AT                TIMESTAMP,
    NOTE                       CLOB,
    RAW_DATA                   CLOB,
    CONSTRAINT PK_LAW_REVISIONS     PRIMARY KEY (ID),
    CONSTRAINT FK_LAW_REV_TRACKED   FOREIGN KEY (TRACKED_LAW_ID)
        REFERENCES TRACKED_LAWS (ID) ON DELETE CASCADE
);

CREATE SEQUENCE SEQ_LAW_REVISIONS START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

CREATE INDEX IX_LAW_REV_TRACKED   ON LAW_REVISIONS (TRACKED_LAW_ID);
CREATE INDEX IX_LAW_REV_DETECTED  ON LAW_REVISIONS (DETECTED_AT);
CREATE INDEX IX_LAW_REV_STATUS    ON LAW_REVISIONS (REVIEW_STATUS);


-- ---------------------------------------------------------------------
-- 3. 사내 문서 (절차서 / 지침서 / 작업표준)
-- ---------------------------------------------------------------------
CREATE TABLE COMPANY_DOCUMENTS (
    ID            NUMBER(19)         NOT NULL,
    DOC_TYPE      VARCHAR2(16 CHAR)  DEFAULT '절차서' NOT NULL,
    DOC_NUMBER    VARCHAR2(64 CHAR),
    TITLE         VARCHAR2(255 CHAR) NOT NULL,
    REVISION_NO   VARCHAR2(32 CHAR),
    REVISION_DATE VARCHAR2(16 CHAR),
    DOC_OWNER     VARCHAR2(64 CHAR),                         -- OWNER 는 혼동을 줄 수 있어 접두어를 붙임
    FILE_LINK     VARCHAR2(512 CHAR),
    NOTE          CLOB,
    TAGS          VARCHAR2(512 CHAR),                        -- 쉼표 구분 키워드. '#' 없이 저장
    CREATED_AT    TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    UPDATED_AT    TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT PK_COMPANY_DOCUMENTS PRIMARY KEY (ID)
);

CREATE SEQUENCE SEQ_COMPANY_DOCUMENTS START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;


-- ---------------------------------------------------------------------
-- 4. 문서 <-> 법령 매핑
-- ---------------------------------------------------------------------
CREATE TABLE DOC_LAW_MAPPINGS (
    ID             NUMBER(19)         NOT NULL,
    DOCUMENT_ID    NUMBER(19)         NOT NULL,
    TRACKED_LAW_ID NUMBER(19)         NOT NULL,
    NOTE           VARCHAR2(255 CHAR),
    CREATED_AT     TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT PK_DOC_LAW_MAPPINGS PRIMARY KEY (ID),
    CONSTRAINT UQ_DOC_LAW_MAPPINGS UNIQUE (DOCUMENT_ID, TRACKED_LAW_ID),
    CONSTRAINT FK_DOC_MAP_DOCUMENT FOREIGN KEY (DOCUMENT_ID)
        REFERENCES COMPANY_DOCUMENTS (ID) ON DELETE CASCADE,
    CONSTRAINT FK_DOC_MAP_LAW      FOREIGN KEY (TRACKED_LAW_ID)
        REFERENCES TRACKED_LAWS (ID) ON DELETE CASCADE
);

CREATE SEQUENCE SEQ_DOC_LAW_MAPPINGS START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

CREATE INDEX IX_DOC_MAP_LAW ON DOC_LAW_MAPPINGS (TRACKED_LAW_ID);


-- ---------------------------------------------------------------------
-- 5. 신규 제정 고시 후보
--    (등록해 둔 고시가 "개정"되는 게 아니라 매년 새로 "제정"되는 경우를
--     놓치지 않기 위해, 부처 + 키워드 이중 필터로 찾아 쌓아둔다.
--     무시한 항목도 행을 남겨야 다음 스캔에서 다시 뜨지 않는다.)
-- ---------------------------------------------------------------------
CREATE TABLE NEW_ADMRUL_CANDIDATES (
    ID                NUMBER(19)         NOT NULL,
    SOURCE_TYPE       VARCHAR2(16 CHAR)  DEFAULT 'admrul' NOT NULL,
    EXTERNAL_ID       VARCHAR2(64 CHAR)  NOT NULL,
    MASTER_ID         VARCHAR2(64 CHAR),
    NAME              VARCHAR2(255 CHAR) NOT NULL,
    CATEGORY          VARCHAR2(64 CHAR),
    DEPARTMENT        VARCHAR2(128 CHAR),
    PROMULGATION_NO   VARCHAR2(64 CHAR),
    PROMULGATION_DATE VARCHAR2(16 CHAR),
    ENFORCEMENT_DATE  VARCHAR2(16 CHAR),
    DETAIL_LINK       VARCHAR2(512 CHAR),
    MATCHED_KEYWORD   VARCHAR2(64 CHAR),
    STATUS            VARCHAR2(16 CHAR)  DEFAULT '신규' NOT NULL,  -- 신규 | 등록됨 | 무시됨
    FIRST_SEEN_AT     TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT PK_NEW_ADMRUL_CAND     PRIMARY KEY (ID),
    CONSTRAINT UQ_NEW_ADMRUL_SRC_EXT  UNIQUE (SOURCE_TYPE, EXTERNAL_ID)
);

CREATE SEQUENCE SEQ_NEW_ADMRUL_CAND START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

CREATE INDEX IX_NEW_ADMRUL_STATUS ON NEW_ADMRUL_CANDIDATES (STATUS);


-- ---------------------------------------------------------------------
-- 6. 법령 본문 캐시
--    국가법령정보 API 는 본문 전체 검색을 제공하지 않아(목록 조회는 법령명만
--    검색 가능), 본문을 미리 받아 두고 로컬에서 검색한다.
-- ---------------------------------------------------------------------
CREATE TABLE SCRAPED_LAW_CONTENTS (
    ID                  NUMBER(19)         NOT NULL,
    SOURCE_TYPE         VARCHAR2(16 CHAR)  NOT NULL,
    EXTERNAL_ID         VARCHAR2(64 CHAR)  NOT NULL,
    MASTER_ID           VARCHAR2(64 CHAR),
    NAME                VARCHAR2(255 CHAR) NOT NULL,
    CATEGORY            VARCHAR2(64 CHAR),
    DEPARTMENT          VARCHAR2(128 CHAR),
    PROMULGATION_NO     VARCHAR2(64 CHAR),
    PROMULGATION_DATE   VARCHAR2(16 CHAR),
    ENFORCEMENT_DATE    VARCHAR2(16 CHAR),
    DETAIL_LINK         VARCHAR2(512 CHAR),
    CONTENT             CLOB,
    ARTICLE_CONTENT_LEN NUMBER(10) DEFAULT 0 NOT NULL,
    CACHED_AT           TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT PK_SCRAPED_CONTENTS    PRIMARY KEY (ID),
    CONSTRAINT UQ_SCRAPED_SRC_EXT     UNIQUE (SOURCE_TYPE, EXTERNAL_ID)
);

CREATE SEQUENCE SEQ_SCRAPED_CONTENTS START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

CREATE INDEX IX_SCRAPED_NAME ON SCRAPED_LAW_CONTENTS (NAME);


-- ---------------------------------------------------------------------
-- 7. 애플리케이션 설정 (키-값)
--    KEY / VALUE 는 Oracle 에서 혼동을 부르는 이름이라 접두어를 붙였다.
-- ---------------------------------------------------------------------
-- SETTING_VALUE 는 CLOB 이 아니라 VARCHAR2 로 둔다. 저장되는 값이 API 인증키,
-- RSS 주소, 쉼표로 구분한 키워드 목록 정도라 길지 않고, CLOB 이면 MERGE 문이
-- 불필요하게 복잡해지기 때문이다.
-- 1333 은 AL32UTF8 에서 VARCHAR2 한 컬럼의 상한(4000 바이트)을 넘지 않는
-- 최대 글자 수다(1333 x 3 = 3999).
CREATE TABLE APP_SETTINGS (
    SETTING_KEY   VARCHAR2(64 CHAR)   NOT NULL,
    SETTING_VALUE VARCHAR2(1333 CHAR),
    CONSTRAINT PK_APP_SETTINGS PRIMARY KEY (SETTING_KEY)
);


-- ---------------------------------------------------------------------
-- 8. 안전보건 뉴스 (대시보드 자동 스크롤 게시판)
-- ---------------------------------------------------------------------
CREATE TABLE NEWS_ITEMS (
    ID           NUMBER(19)          NOT NULL,
    CATEGORY     VARCHAR2(16 CHAR)   NOT NULL,   -- moel | kosha | accident
    SOURCE_NAME  VARCHAR2(64 CHAR)   NOT NULL,
    TITLE        VARCHAR2(512 CHAR)  NOT NULL,
    LINK         VARCHAR2(1024 CHAR) NOT NULL,
    GUID         VARCHAR2(512 CHAR)  NOT NULL,   -- 보통 원문 링크. 중복 판단 기준
    PUBLISHED_AT TIMESTAMP,
    FETCHED_AT   TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    IS_DEMO      NUMBER(1) DEFAULT 0 NOT NULL,
    CONSTRAINT PK_NEWS_ITEMS      PRIMARY KEY (ID),
    CONSTRAINT UQ_NEWS_CAT_GUID   UNIQUE (CATEGORY, GUID),
    CONSTRAINT CK_NEWS_IS_DEMO    CHECK (IS_DEMO IN (0, 1))
);

CREATE SEQUENCE SEQ_NEWS_ITEMS START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

CREATE INDEX IX_NEWS_CAT_PUB ON NEWS_ITEMS (CATEGORY, PUBLISHED_AT);
