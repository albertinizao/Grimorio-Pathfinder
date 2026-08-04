# Build the Vue SPA and the Spring Boot executable JAR with the project's
# required Node.js and Java versions.
FROM node:22-bookworm AS node-runtime

FROM maven:3.9-eclipse-temurin-21 AS build

# Maven runs npm during prepare-package. Reuse the official Node.js 22
# installation so frontend and backend builds use the same toolchain as CI.
COPY --from=node-runtime /usr/local/bin/node /usr/local/bin/node
COPY --from=node-runtime /usr/local/lib/node_modules /usr/local/lib/node_modules
RUN ln -s /usr/local/lib/node_modules/npm/bin/npm-cli.js /usr/local/bin/npm \
    && ln -s /usr/local/lib/node_modules/npm/bin/npx-cli.js /usr/local/bin/npx

WORKDIR /workspace

# Install frontend dependencies before copying the rest of the sources to
# maximize layer reuse when only backend or data files change.
COPY pom.xml ./
COPY frontend/package.json frontend/package-lock.json ./frontend/
RUN cd frontend && npm ci

COPY src ./src
COPY frontend ./frontend

# The executable JAR embeds the compiled SPA.
RUN mvn -DskipTests package

FROM eclipse-temurin:21-jre AS runtime

WORKDIR /app

RUN useradd --system --uid 10001 --create-home appuser \
    && mkdir -p /app/data/generated /app/data/overrides

COPY --from=build /workspace/target/grimorio-pathfinder-*.jar /app/grimorio-pathfinder.jar
COPY data/generated/spells-es.generated.json /app/data/generated/spells-es.generated.json
COPY data/overrides/spells-es.overrides.json /app/data/overrides/spells-es.overrides.json

RUN chown -R appuser:appuser /app

USER appuser

ENV SERVER_PORT=8081 \
    JAVA_TOOL_OPTIONS="-XX:InitialRAMPercentage=25.0 -XX:MaxRAMPercentage=75.0 -XX:+UseSerialGC"

VOLUME ["/app/data/overrides"]

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "/app/grimorio-pathfinder.jar"]
