@echo off
chcp 65001 >nul
setlocal

rem ====================================================================
rem  안전보건 법령·고시 개정 추적 시스템 v3 (Java)
rem ====================================================================
rem  실행 전에 아래 접속 정보를 이 회사 환경에 맞게 채워 넣으세요.
rem  비밀번호를 파일에 적어 두기 어렵다면, 이 줄들을 지우고 명령 프롬프트에서
rem  set DB_PASSWORD=... 를 먼저 실행한 뒤 이 파일을 실행해도 됩니다.
rem ====================================================================

rem --- Oracle 접속 정보 -----------------------------------------------
rem  SID 방식     : jdbc:oracle:thin:@서버주소:1521:SID
rem  서비스명 방식 : jdbc:oracle:thin:@//서버주소:1521/서비스명
if "%DB_URL%"=="" set DB_URL=jdbc:oracle:thin:@localhost:1521:ORCL
if "%DB_USERNAME%"=="" set DB_USERNAME=safety
rem set DB_PASSWORD=여기에_비밀번호

rem --- 국가법령정보 API 인증키 (비워 두면 예시 데이터로 동작) ------------
rem set LAW_API_OC=발급받은_인증키

rem --- 화면 접속 보호 (둘 다 채우면 로그인을 요구) ----------------------
rem set DASHBOARD_USERNAME=admin
rem set DASHBOARD_PASSWORD=여기에_비밀번호

set SERVER_PORT=8000

rem --------------------------------------------------------------------
where java >nul 2>nul
if errorlevel 1 (
    echo [오류] Java 를 찾을 수 없습니다. JDK 17 이 설치되어 있는지 확인하세요.
    pause
    exit /b 1
)

set JAR=target\safety-law-monitor-3.0.0.jar
if not exist "%JAR%" (
    echo 실행 파일이 없어 먼저 빌드합니다. 처음 한 번은 몇 분 걸릴 수 있습니다.
    call mvn -B clean package -DskipTests
    if errorlevel 1 (
        echo [오류] 빌드에 실패했습니다.
        pause
        exit /b 1
    )
)

echo.
echo 서버를 시작합니다. 브라우저에서 http://localhost:%SERVER_PORT% 로 접속하세요.
echo 종료하려면 이 창에서 Ctrl+C 를 누르세요.
echo.

java -jar "%JAR%"

pause
endlocal
