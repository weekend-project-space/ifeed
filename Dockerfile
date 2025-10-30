FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /build

# 如果工程使用 Maven Wrapper，拷贝 wrapper 和 .mvn 目录
COPY mvnw .
COPY .mvn .mvn
RUN chmod +x ./mvnw || true

# 拷贝 pom 和源码
COPY pom.xml .
COPY src ./src

# 构建（跳过测试以加速）
RUN ./mvnw  clean package -DskipTests

FROM openjdk:21-jdk-slim AS runtime
WORKDIR /app
ENV TZ=Asia/Shanghai
# 复制构建产物（假设生成的 jar 在 target）
COPY --from=build /build/target/*.jar app.jar

EXPOSE 8080

# Specify the command to run the application
CMD ["java", "-jar", "/app/app.jar"]