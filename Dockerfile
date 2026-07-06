# ============ Stage 1: сборка ============
# Собираем в образе с JDK 21 + Maven. Это "build-time" образ, в финал он не попадёт.
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app

# Сначала копируем только pom.xml и тянем зависимости — этот слой кэшируется
# и переигрывается только при изменении pom.xml (быстрее пересборки).
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

# Теперь исходники и сборка jar (тесты в образе пропускаем — они гоняются в CI).
COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ============ Stage 2: рантайм ============
# Только JRE (без Maven и исходников) — образ маленький. alpine = минимальный дистрибутив.
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Непривилегированный пользователь: контейнер НЕ должен работать под root (безопасность).
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Забираем готовый jar из стадии сборки.
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

# HEALTHCHECK: Docker сам периодически проверяет здоровье контейнера.
# start-period даёт приложению время подняться перед первой проверкой.
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget -q --spider http://localhost:8080/actuator/health || exit 1

# MaxRAMPercentage — JVM берёт % от лимита памяти КОНТЕЙНЕРА, а не хоста.
# Критично в облаке: без этого JVM может увидеть всю память ноды и словить OOM-kill.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
