# ---- Build: compile the jar with the Maven Wrapper ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /src

# Maven files first: dependencies stay cached until pom.xml changes.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -q -B dependency:go-offline

COPY src/ src/
# Tests (which need Docker) run outside the image build.
RUN ./mvnw -q -B package -DskipTests

# ---- Runtime: JRE only, unprivileged user ----
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 1001 app
COPY --from=build /src/target/plannershop-*.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
