# syntax=docker/dockerfile:1

# Etapa build: compila config-server (y los modulos de los que depende) con Maven, usando la raiz del repo como contexto.
# Etapa runtime: imagen solo con JRE 17 que ejecuta el jar con un usuario sin privilegios.

FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .
RUN --mount=type=cache,target=/root/.m2 mvn -B -q -pl config-server -am package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN groupadd --system app && useradd --system --gid app --no-create-home app
COPY --from=build /app/config-server/target/config-server-0.0.1-SNAPSHOT.jar app.jar
USER app
EXPOSE 8888
ENTRYPOINT ["java","-jar","/app/app.jar"]
