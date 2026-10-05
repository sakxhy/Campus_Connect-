#!/usr/bin/env bash
# ==============================================================================
# CampusConnect - Apache Tomcat Server Start Script
# ==============================================================================

set -e
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TOMCAT_DIR="$PROJECT_DIR/.server/tomcat"

echo "=========================================="
echo " Starting CampusConnect Tomcat Backend... "
echo "=========================================="

# Auto-download and configure Tomcat if not present
if [ ! -d "$TOMCAT_DIR" ]; then
    echo "Tomcat not found locally. Setting up Apache Tomcat 10..."
    mkdir -p "$PROJECT_DIR/.server"
    curl -L -o "$PROJECT_DIR/.server/tomcat.tar.gz" https://downloads.apache.org/tomcat/tomcat-10/v10.1.60/bin/apache-tomcat-10.1.60.tar.gz
    tar -xzf "$PROJECT_DIR/.server/tomcat.tar.gz" -C "$PROJECT_DIR/.server/"
    rm "$PROJECT_DIR/.server/tomcat.tar.gz"
    mv "$PROJECT_DIR/.server/apache-tomcat-10.1.60" "$TOMCAT_DIR"
    
    # Configure default port to 8090 to avoid local port conflicts
    sed -i '' 's/port="8080"/port="8090"/g' "$TOMCAT_DIR/conf/server.xml"

    # Deploy built war if present
    if [ -f "$PROJECT_DIR/target/CampusConnect.war" ]; then
        rm -rf "$TOMCAT_DIR/webapps/ROOT" "$TOMCAT_DIR/webapps/ROOT.war"
        cp "$PROJECT_DIR/target/CampusConnect.war" "$TOMCAT_DIR/webapps/ROOT.war"
        cp "$PROJECT_DIR/target/CampusConnect.war" "$TOMCAT_DIR/webapps/CampusConnect.war"
    fi
fi

# Ensure executable permissions
chmod +x "$TOMCAT_DIR/bin"/*.sh

# Start Tomcat in background
"$TOMCAT_DIR/bin/catalina.sh" start

echo ""
echo "CampusConnect backend is running on Apache Tomcat!"
echo "Server URL:     http://localhost:8090/"
echo "Stats API:      http://localhost:8090/api/stats"
echo "Tickets API:    http://localhost:8090/api/tickets"
echo "Track API:      http://localhost:8090/api/track?id=TKT-2026-101"
echo ""
echo "To view server logs: tail -f \"$TOMCAT_DIR/logs/catalina.out\""
echo "To stop server:      ./stop-server.sh"
