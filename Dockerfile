FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /workspace
COPY .mvn/ .mvn/
COPY mvnw .
RUN chmod +x mvnw
COPY backend/pom.xml backend/pom.xml
RUN ./mvnw -f backend/pom.xml dependency:go-offline -q
COPY backend/src/ backend/src/
RUN ./mvnw -f backend/pom.xml package -DskipTests -q

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /workspace/backend/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
