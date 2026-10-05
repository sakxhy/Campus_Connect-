#!/usr/bin/env bash
# ==============================================================================
# CampusConnect - Tomcat Server Stop Script
# ==============================================================================

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TOMCAT_DIR="$PROJECT_DIR/.server/tomcat"

echo "Stopping CampusConnect Tomcat server..."
if [ -d "$TOMCAT_DIR" ]; then
    "$TOMCAT_DIR/bin/catalina.sh" stop 5 -force || true
fi
pkill -9 -f "org.apache.catalina.startup.Bootstrap" || true
echo "Tomcat server stopped."
