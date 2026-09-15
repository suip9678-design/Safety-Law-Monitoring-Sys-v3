import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Oracle 접속 가능 여부 + 스키마 설계에 필요한 정보를 한 번에 확인하는 점검 도구.
 *
 * 실행 방법은 tools/README.md 참고. 기본 실행은 읽기 전용이며,
 * --ddl 옵션을 준 경우에만 임시 테이블을 만들었다가 지운다.
 */
public class OracleConnectionTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        if (args.length < 3) {
            usage();
            return;
        }
        String url = args[0];
        String user = args[1];
        String password = args[2];
        boolean allowDdl = args.length > 3 && "--ddl".equalsIgnoreCase(args[3]);

        line();
        System.out.println(" Oracle 접속 / 호환성 점검");
        line();
        System.out.println(" 실행 JVM   : " + System.getProperty("java.version")
                + " (" + System.getProperty("java.vendor") + ")");
        System.out.println(" 접속 주소  : " + url);
        System.out.println(" 접속 계정  : " + user);
        System.out.println(" DDL 테스트 : " + (allowDdl ? "예 (임시 테이블 생성 후 삭제)" : "아니오 (읽기 전용)"));
        System.out.println();

        long started = System.currentTimeMillis();
        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            System.out.println(">>> 1단계: 접속 성공 ("
                    + (System.currentTimeMillis() - started) + "ms)");
            System.out.println();

            printVersions(conn);
            printCharacterSet(conn);
            runFeatureChecks(conn, allowDdl);

            System.out.println();
            line();
            System.out.println(" 결과: 성공 " + passed + "건 / 실패 " + failed + "건");
            line();
            System.out.println();
            System.out.println(" 위 내용 전체를 그대로 복사해서 전달해 주세요.");

        } catch (SQLException e) {
            System.out.println(">>> 1단계: 접속 실패");
            System.out.println();
            System.out.println("  오류 코드 : ORA-" + String.format("%05d", e.getErrorCode()));
            System.out.println("  오류 내용 : " + e.getMessage());
            System.out.println();
            explainConnectionError(e);
        }
    }

    private static void printVersions(Connection conn) {
        System.out.println(">>> 2단계: 버전 정보");
        try {
            DatabaseMetaData meta = conn.getMetaData();
            System.out.println("  DB 제품    : " + meta.getDatabaseProductName());
            System.out.println("  DB 버전    : " + meta.getDatabaseProductVersion());
            System.out.println("  드라이버   : " + meta.getDriverName());
            System.out.println("  드라이버버전: " + meta.getDriverVersion());
            System.out.println("  최대 식별자 길이: " + meta.getMaxColumnNameLength() + "자");
            passed++;
        } catch (SQLException e) {
            System.out.println("  [실패] " + e.getMessage());
            failed++;
        }
        System.out.println();
    }

    private static void printCharacterSet(Connection conn) {
        System.out.println(">>> 3단계: 문자셋 (한글 저장 용량에 직접 영향)");
        String sql = "SELECT parameter, value FROM nls_database_parameters "
                + "WHERE parameter IN ('NLS_CHARACTERSET','NLS_NCHAR_CHARACTERSET','NLS_LENGTH_SEMANTICS')";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            String charset = null;
            while (rs.next()) {
                String p = rs.getString(1);
                String v = rs.getString(2);
                System.out.println("  " + p + " = " + v);
                if ("NLS_CHARACTERSET".equals(p)) {
                    charset = v;
                }
            }
            if (charset != null) {
                System.out.println();
                System.out.println("  -> " + charsetAdvice(charset));
            }
            passed++;
        } catch (SQLException e) {
            System.out.println("  [확인 불가] " + e.getMessage());
            System.out.println("  (조회 권한이 없을 수 있습니다. DBA에게 NLS_CHARACTERSET 값을 문의하세요.)");
            failed++;
        }
        System.out.println();
    }

    private static String charsetAdvice(String charset) {
        if (charset.startsWith("AL32UTF8") || charset.startsWith("UTF8")) {
            return "유니코드(" + charset + "). 한글 1자가 3바이트를 쓰므로 "
                    + "컬럼을 반드시 VARCHAR2(n CHAR) 형태로 만들어야 합니다.";
        }
        if (charset.startsWith("KO16")) {
            return "한국어 전용(" + charset + "). 한글 1자가 2바이트입니다. "
                    + "법령 본문에 한자/특수문자가 있으면 깨질 수 있어 확인이 필요합니다.";
        }
        return "예상치 못한 문자셋(" + charset + "). 한글 저장 가능 여부를 반드시 확인해야 합니다.";
    }

    private static void runFeatureChecks(Connection conn, boolean allowDdl) {
        System.out.println(">>> 4단계: 기능 점검");

        check("한글 저장/조회 왕복", conn, c -> {
            try (PreparedStatement ps = c.prepareStatement("SELECT ? FROM dual")) {
                String sample = "안전보건 법령·고시 개정 추적";
                ps.setString(1, sample);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    String got = rs.getString(1);
                    if (!sample.equals(got)) {
                        throw new IllegalStateException("한글이 깨짐. 보낸 값=[" + sample + "] 받은 값=[" + got + "]");
                    }
                }
            }
        });

        check("ROWNUM 페이징 (10g는 OFFSET/FETCH 미지원)", conn, c -> {
            String sql = "SELECT * FROM (SELECT a.*, ROWNUM rn FROM "
                    + "(SELECT level AS n FROM dual CONNECT BY level <= 20 ORDER BY level) a "
                    + "WHERE ROWNUM <= 10) WHERE rn > 5";
            try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
                int count = 0;
                while (rs.next()) {
                    count++;
                }
                if (count != 5) {
                    throw new IllegalStateException("5건을 기대했으나 " + count + "건 반환됨");
                }
            }
        });

        check("OFFSET/FETCH 지원 여부 (지원되면 11g 이상)", conn, c -> {
            try (Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery("SELECT 1 FROM dual OFFSET 0 ROWS FETCH FIRST 1 ROWS ONLY")) {
                rs.next();
            }
        });

        check("CLOB 읽기/쓰기 (법령 본문 저장용)", conn, c -> {
            StringBuilder sb = new StringBuilder();
            while (sb.length() < 8000) {
                sb.append("제1조(목적) 이 법은 산업 안전 및 보건에 관한 기준을 확립한다. ");
            }
            String big = sb.toString();
            try (PreparedStatement ps = c.prepareStatement("SELECT TO_CLOB(?) FROM dual")) {
                ps.setString(1, big.substring(0, 3000));
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    if (rs.getString(1) == null) {
                        throw new IllegalStateException("CLOB 값이 null로 반환됨");
                    }
                }
            }
        });

        check("시퀀스 조회 권한 (기본키 채번에 필요)", conn, c -> {
            try (Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM user_sequences")) {
                rs.next();
                System.out.println("       (현재 계정 보유 시퀀스: " + rs.getInt(1) + "개)");
            }
        });

        check("현재 계정 테이블 생성 권한 확인", conn, c -> {
            try (Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery(
                         "SELECT COUNT(*) FROM user_sys_privs WHERE privilege = 'CREATE TABLE'")) {
                rs.next();
                if (rs.getInt(1) == 0) {
                    System.out.println("       (직접 부여된 CREATE TABLE 권한 없음 - 롤을 통해 받았을 수 있음)");
                }
            }
        });

        if (allowDdl) {
            check("실제 테이블 생성/입력/삭제 (임시)", conn, c -> {
                String table = "SLM_CONN_TEST";
                try (Statement st = c.createStatement()) {
                    dropQuietly(c, table);
                    st.execute("CREATE TABLE " + table + " ("
                            + "ID NUMBER(19) NOT NULL PRIMARY KEY, "
                            + "NAME VARCHAR2(255 CHAR), "
                            + "BODY CLOB, "
                            + "IS_ACTIVE NUMBER(1) DEFAULT 1 NOT NULL, "
                            + "CREATED_AT TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL)");
                    try (PreparedStatement ps = c.prepareStatement(
                            "INSERT INTO " + table + " (ID, NAME, BODY) VALUES (?, ?, ?)")) {
                        ps.setLong(1, 1L);
                        ps.setString(2, "산업안전보건법 시행규칙");
                        ps.setString(3, "제1조(목적) 이 규칙은 산업안전보건법에서 위임된 사항을 규정한다.");
                        ps.executeUpdate();
                    }
                    try (ResultSet rs = st.executeQuery("SELECT NAME, BODY FROM " + table + " WHERE ID = 1")) {
                        rs.next();
                        if (!"산업안전보건법 시행규칙".equals(rs.getString(1))) {
                            throw new IllegalStateException("저장된 한글이 깨짐: " + rs.getString(1));
                        }
                    }
                } finally {
                    dropQuietly(c, table);
                }
            });
        } else {
            System.out.println("  [건너뜀] 실제 테이블 생성 테스트 (--ddl 옵션을 주면 실행)");
        }
    }

    private static void dropQuietly(Connection c, String table) {
        try (Statement st = c.createStatement()) {
            st.execute("DROP TABLE " + table + " PURGE");
        } catch (SQLException ignored) {
            // 테이블이 없으면 무시
        }
    }

    private interface Check {
        void run(Connection c) throws Exception;
    }

    private static void check(String label, Connection conn, Check check) {
        try {
            check.run(conn);
            System.out.println("  [성공] " + label);
            passed++;
        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg != null) {
                msg = msg.split("\\R")[0];
            }
            System.out.println("  [실패] " + label + "  ->  " + msg);
            failed++;
        }
    }

    private static void explainConnectionError(SQLException e) {
        int code = e.getErrorCode();
        System.out.println("  해석:");
        switch (code) {
            case 28040:
                System.out.println("  ORA-28040은 '드라이버가 너무 최신이라 이 DB에 접속을 거부했다'는 뜻입니다.");
                System.out.println("  가장 흔하고, 가장 중요한 경우입니다. 더 낮은 버전의 ojdbc 드라이버가 필요합니다.");
                break;
            case 1017:
                System.out.println("  ORA-01017은 계정 또는 비밀번호가 틀렸다는 뜻입니다. 드라이버 문제는 아닙니다.");
                break;
            case 12541:
                System.out.println("  ORA-12541은 해당 주소/포트에서 리스너를 찾지 못했다는 뜻입니다.");
                System.out.println("  주소, 포트, 방화벽 개방 여부를 확인하세요.");
                break;
            case 12514:
                System.out.println("  ORA-12514는 리스너는 찾았지만 서비스 이름(SID/Service Name)이 틀렸다는 뜻입니다.");
                break;
            case 17002:
            case 0:
                System.out.println("  네트워크 자체가 닿지 않는 상태일 수 있습니다(방화벽/VPN/주소 오류).");
                break;
            default:
                System.out.println("  위 오류 코드와 메시지를 그대로 전달해 주세요.");
        }
        System.out.println();
        System.out.println("  어떤 경우든 위 출력 전체를 복사해서 전달해 주시면 다음 단계를 판단할 수 있습니다.");
    }

    private static void line() {
        System.out.println("======================================================================");
    }

    private static void usage() {
        System.out.println("사용법:");
        System.out.println("  java -cp <ojdbc파일> OracleConnectionTest.java <접속주소> <계정> <비밀번호> [--ddl]");
        System.out.println();
        System.out.println("예시 (Windows):");
        System.out.println("  java -cp ojdbc8.jar OracleConnectionTest.java "
                + "jdbc:oracle:thin:@//10.0.0.5:1521/ORCL scott tiger");
        System.out.println();
        System.out.println("예시 (SID 방식 - 10g에서 흔함):");
        System.out.println("  java -cp ojdbc8.jar OracleConnectionTest.java "
                + "jdbc:oracle:thin:@10.0.0.5:1521:ORCL scott tiger");
    }
}
