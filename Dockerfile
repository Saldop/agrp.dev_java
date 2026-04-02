# Build stage
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline -q
COPY src/ src/
RUN ./mvnw clean package -DskipTests -q

# Run stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/agrp-dev-0.0.1-SNAPSHOT.jar app.jar
# Required environment variables:
# OPENAI_API_KEY — OpenAI API key for contract analysis (e.g. sk-...)
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
