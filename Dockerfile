# ==============================================================================
# CampusConnect - Multi-Stage Production Dockerfile
# ==============================================================================

# Stage 1: Build the Java Servlet WAR bundle with Maven
FROM maven:3.9.9-eclipse-temurin-17 AS builder
WORKDIR /app

# Copy POM and Source Code
COPY pom.xml .
COPY src ./src

# Compile and package WAR archive
RUN mvn clean package -DskipTests

# Stage 2: Apache Tomcat 10.1 (Jakarta EE 10 / Servlet 6.0 Runtime)
FROM tomcat:10.1-jdk17-temurin
WORKDIR /usr/local/tomcat

# Remove default Tomcat webapps
RUN rm -rf webapps/*

# Deploy CampusConnect as ROOT application
COPY --from=builder /app/target/CampusConnect.war webapps/ROOT.war
COPY campus_connect_schema.sql ./campus_connect_schema.sql

# Create dynamic port startup entrypoint for PaaS (Render, Railway, Heroku)
RUN echo '#!/bin/sh\n\
if [ -n "$PORT" ] && [ "$PORT" != "8080" ]; then\n\
  echo "Configuring Tomcat port to $PORT..."\n\
  sed -i "s/port=\"8080\"/port=\"$PORT\"/g" conf/server.xml\n\
fi\n\
exec catalina.sh run' > /entrypoint.sh && chmod +x /entrypoint.sh

EXPOSE 8080
ENTRYPOINT ["/entrypoint.sh"]
