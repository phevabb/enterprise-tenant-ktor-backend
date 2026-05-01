# -------- BUILD STAGE --------
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# -------- RUNTIME STAGE --------
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*jar-with-dependencies.jar app.jar

ENV PORT=8080
EXPOSE 8080

CMD ["java", "-jar", "app.jar"]