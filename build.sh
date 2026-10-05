#!/usr/bin/env bash
# ==============================================================================
# CampusConnect - Maven Build & WAR Packaging Script
# ==============================================================================

set -e
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MVN_BIN="$PROJECT_DIR/.server/maven/bin/mvn"
TOMCAT_DIR="$PROJECT_DIR/.server/tomcat"

echo "=========================================="
echo " Building CampusConnect Java Servlet WAR  "
echo "=========================================="

if command -v mvn &> /dev/null; then
    mvn clean package
elif [ -f "$MVN_BIN" ]; then
    "$MVN_BIN" clean package
else
    echo "Maven not found. Installing standalone Maven..."
    mkdir -p "$PROJECT_DIR/.server"
    curl -L -o "$PROJECT_DIR/.server/maven.tar.gz" https://archive.apache.org/dist/maven/maven-3/3.9.9/binaries/apache-maven-3.9.9-bin.tar.gz
    tar -xzf "$PROJECT_DIR/.server/maven.tar.gz" -C "$PROJECT_DIR/.server/"
    rm "$PROJECT_DIR/.server/maven.tar.gz"
    mv "$PROJECT_DIR/.server/apache-maven-3.9.9" "$PROJECT_DIR/.server/maven"
    "$MVN_BIN" clean package
fi

# Deploy newly packaged WAR to Tomcat if directory exists
if [ -d "$TOMCAT_DIR/webapps" ]; then
    echo ""
    echo "Deploying to local Tomcat webapps..."
    rm -rf "$TOMCAT_DIR/webapps/ROOT" "$TOMCAT_DIR/webapps/ROOT.war"
    cp "$PROJECT_DIR/target/CampusConnect.war" "$TOMCAT_DIR/webapps/ROOT.war"
    cp "$PROJECT_DIR/target/CampusConnect.war" "$TOMCAT_DIR/webapps/CampusConnect.war"
fi

echo ""
echo "Build and Deployment completed successfully!"
echo "Target WAR: $PROJECT_DIR/target/CampusConnect.war"
