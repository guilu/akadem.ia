FROM eclipse-temurin:25-jdk AS builder
WORKDIR /app
RUN apt-get update && apt-get install -y curl unzip dos2unix && rm -rf /var/lib/apt/lists/*

COPY . .
RUN ./gradlew --no-daemon clean bootJar

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
