# Bases fixadas por digest (manifest list, cobre amd64 e arm64) para build
# reproduzível. Atualizar com:
#   docker pull eclipse-temurin:21-jdk && \
#     docker inspect eclipse-temurin:21-jdk --format '{{index .RepoDigests 0}}'
# Fixar digest não corrige CVE: reconstrua a imagem após atualizar o digest.
FROM eclipse-temurin:21-jdk@sha256:5d90b26c0784ccf55af55f97cbcf7f3969c66a06299c93bdc72649219942a48f AS build
WORKDIR /build

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY src src

RUN chmod +x mvnw && ./mvnw -B package -DskipTests

FROM eclipse-temurin:21-jre@sha256:f37956012e0cb88400e5b3689aeae2d2d735364c1922ae2225536163b5a99f56
WORKDIR /deployments

COPY --from=build /build/target/quarkus-app/lib/ ./lib/
COPY --from=build /build/target/quarkus-app/*.jar ./
COPY --from=build /build/target/quarkus-app/app/ ./app/
COPY --from=build /build/target/quarkus-app/quarkus/ ./quarkus/

ENV JAVA_OPTS="-Dquarkus.http.host=0.0.0.0 -Djava.util.logging.manager=org.jboss.logmanager.LogManager"
EXPOSE 8080

CMD ["java", "-jar", "quarkus-run.jar"]
