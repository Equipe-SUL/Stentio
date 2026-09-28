FROM eclipse-temurin:25-jdk AS build

WORKDIR /workspace

COPY gradlew settings.gradle build.gradle ./

COPY gradle ./gradle

RUN sed -i 's/\r$//' gradlew && chmod +x gradlew

RUN ./gradlew dependencies --no-daemon

COPY src ./src

RUN ./gradlew bootJar --no-daemon && \
    find build/libs -maxdepth 1 -type f -name '*.jar' ! -name '*-plain.jar' -exec cp {} /workspace/app.jar \;

FROM eclipse-temurin:25-jre AS runner

WORKDIR /app

RUN useradd --system --uid 1001 spring

COPY --from=build /workspace/app.jar ./app.jar

USER spring

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]