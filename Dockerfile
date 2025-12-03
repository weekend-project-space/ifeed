#FROM ghcr.io/graalvm/native-image-community:21 AS builder
#WORKDIR /app
#COPY . .
#RUN ./mvnw -Pnative native:compile -DskipTests

#FROM gcr.io/distroless/base-debian12
#WORKDIR /app
##COPY --from=builder /app/target/ifeed /app/ifeed
#COPY ./target/ifeed /app/ifeed
#EXPOSE 8080
#ENTRYPOINT ["/app/ifeed"]

# 运行阶段 - 使用更小的基础镜像
FROM oraclelinux:9-slim

WORKDIR /app

COPY ./target/ifeed /app/ifeed
# 直接复制二进制文件（GraalVM native-image 默认输出在 target/<artifactId>）
#COPY --from=builder /workspace/app/target/msgsender /app/msgsender

EXPOSE 8080

ENTRYPOINT ["/app/ifeed"]