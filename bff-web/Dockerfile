# syntax=docker/dockerfile:1

# Etapa build: compila bff-web (y los modulos de los que depende) con Maven, usando la raiz del repo como contexto.
# Etapa runtime: imagen solo con JRE 17 que ejecuta el jar con un usuario sin privilegios.

FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .
RUN --mount=type=cache,target=/root/.m2 mvn -B -q -pl bff-web -am package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN groupadd --system app && useradd --system --gid app --no-create-home app
COPY --from=build /app/bff-web/target/bff-web-0.0.1-SNAPSHOT.jar app.jar
USER app
EXPOSE 8081
ENTRYPOINT ["java","-jar","/app/app.jar"]
