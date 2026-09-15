#!/usr/bin/env bash
# =====================================================================
#  안전보건 법령·고시 개정 추적 시스템 v3 (Java)
# =====================================================================
#  접속 정보는 이 파일을 고치지 말고 환경변수로 넘기는 것을 권장한다.
#    DB_PASSWORD='...' ./run-server.sh
# =====================================================================
set -euo pipefail

cd "$(dirname "$0")"

# SID 방식     : jdbc:oracle:thin:@서버주소:1521:SID
# 서비스명 방식 : jdbc:oracle:thin:@//서버주소:1521/서비스명
export DB_URL="${DB_URL:-jdbc:oracle:thin:@localhost:1521:ORCL}"
export DB_USERNAME="${DB_USERNAME:-safety}"
export SERVER_PORT="${SERVER_PORT:-8000}"

if ! command -v java >/dev/null 2>&1; then
    echo "[오류] Java 를 찾을 수 없습니다. JDK 17 이 설치되어 있는지 확인하세요." >&2
    exit 1
fi

JAR="target/safety-law-monitor-3.0.0.jar"
if [ ! -f "$JAR" ]; then
    echo "실행 파일이 없어 먼저 빌드합니다. 처음 한 번은 몇 분 걸릴 수 있습니다."
    mvn -B clean package -DskipTests
fi

echo
echo "서버를 시작합니다. 브라우저에서 http://localhost:${SERVER_PORT} 로 접속하세요."
echo "종료하려면 Ctrl+C 를 누르세요."
echo

exec java -jar "$JAR"
