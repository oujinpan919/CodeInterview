# 码上记后端运行镜像（单 jar，T18）
#
# 构建（在 backend/ 目录）：
#   mvn -q clean package -DskipTests
#   docker build -t codeinterview-backend:1.0.0 .
#
# 运行（与 Nginx 同网络，数据库用本机 MySQL 时 DB_HOST 填 host.docker.internal 或宿主 IP）：
#   docker run -d --name codeinterview-backend --env-file ../.env \
#     -p 127.0.0.1:8080:8080 codeinterview-backend:1.0.0
#   ↑ 只绑宿主机回环，避免绕过 Nginx 直连 8080；容器间部署则由 Docker 网络访问，不要 publish。
#
# 注意：本镜像只跑 jar；前端 dist 由 Nginx 托管（见 nginx/default.conf）。
# 密钥只通过环境变量注入（AI_API_KEY / DB_PASSWORD / ADMIN_INIT_PASSWORD），不写进镜像。
FROM eclipse-temurin:17-jre

WORKDIR /app

COPY target/codeinterview-backend-1.0.0.jar /app/app.jar

ENV TZ=Asia/Shanghai \
    JAVA_OPTS="-Xms256m -Xmx512m" \
    SPRING_PROFILES_ACTIVE=prod

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
