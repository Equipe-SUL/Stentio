FROM eclipse-temurin:25-jdk AS build

WORKDIR /workspace

COPY gradlew settings.gradle build.gradle gradle.properties ./

COPY gradle ./gradle

RUN sed -i 's/\r$//' gradlew && chmod +x gradlew

RUN --mount=type=cache,target=/root/.gradle,id=stentio-gradle-core \
    --mount=type=cache,target=/workspace/.gradle,id=stentio-project-gradle-core \
    ./gradlew dependencies --no-daemon

COPY src ./src

RUN --mount=type=cache,target=/root/.gradle,id=stentio-gradle-core \
    --mount=type=cache,target=/workspace/.gradle,id=stentio-project-gradle-core \
    ./gradlew bootJar --no-daemon && \
    find build/libs -maxdepth 1 -type f -name '*.jar' ! -name '*-plain.jar' -exec cp {} /workspace/app.jar \;

FROM eclipse-temurin:25-jre AS runner

WORKDIR /app

RUN useradd --system --uid 1001 spring

COPY --from=build /workspace/app.jar ./app.jar

RUN mkdir -p /app/uploads && chown -R spring:spring /app/uploads

USER spring

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
