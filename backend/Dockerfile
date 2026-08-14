FROM eclipse-temurin:21-jdk

RUN apt-get update \
    && apt-get install --yes --no-install-recommends unzip \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /workspace
COPY . .
RUN chmod +x mvnw && ./mvnw --batch-mode --no-transfer-progress dependency:go-offline

EXPOSE 8080
CMD ["./mvnw", "--batch-mode", "--no-transfer-progress", "spring-boot:run"]
