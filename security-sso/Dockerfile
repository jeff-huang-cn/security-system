# 基于 JDK 1.8 镜像
FROM openjdk:8-jdk-slim

# 复制打包好的 JAR 文件（提前用 Maven 打包：mvn clean package -DskipTests）
COPY target/security-sso-1.0.0.jar /app/security-sso.jar

# 暴露端口（与 application.yml 中 server.port 一致，默认 9000）
EXPOSE 9000

# 启动命令（配置连接已部署的 MySQL、Redis）
ENTRYPOINT ["java", "-jar", "/app/security-sso.jar"]