# Bases fixadas por digest (manifest list, cobre amd64 e arm64) para build
# reproduzível. Atualizar com:
#   docker pull eclipse-temurin:21-jdk && \
#     docker inspect eclipse-temurin:21-jdk --format '{{index .RepoDigests 0}}'
# Fixar digest não corrige CVE: reconstrua a imagem após atualizar o digest.
FROM eclipse-temurin:21-jdk@sha256:ebfbb18d11ff163ee83b21258acf12248af48d22a0fdfdc68fa1a1516c05976a AS build
WORKDIR /build

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY src src

RUN chmod +x mvnw && ./mvnw -B package -DskipTests

FROM eclipse-temurin:21-jre@sha256:0b99b3376fbb50dbf9c0af8535fdd5b8eb0d3fdfc8b1e4967f383b6919f6901a
WORKDIR /deployments

COPY --from=build /build/target/quarkus-app/lib/ ./lib/
COPY --from=build /build/target/quarkus-app/*.jar ./
COPY --from=build /build/target/quarkus-app/app/ ./app/
COPY --from=build /build/target/quarkus-app/quarkus/ ./quarkus/

ENV JAVA_OPTS="-Dquarkus.http.host=0.0.0.0 -Djava.util.logging.manager=org.jboss.logmanager.LogManager"
EXPOSE 8080

CMD ["java", "-jar", "quarkus-run.jar"]
