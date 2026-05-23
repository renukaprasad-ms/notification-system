FROM eclipse-temurin:17-jdk-alpine AS build

WORKDIR /workspace

COPY notification-backend/.mvn .mvn
COPY notification-backend/mvnw notification-backend/pom.xml ./
RUN ./mvnw -Dmaven.test.skip=true dependency:go-offline

COPY notification-backend/src src
RUN ./mvnw -Dmaven.test.skip=true clean package

FROM eclipse-temurin:17-jre-alpine

RUN addgroup -S app && adduser -S app -G app

WORKDIR /app

COPY --from=build /workspace/target/*.jar app.jar

USER app

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
