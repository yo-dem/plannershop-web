# ---- Build: compila il jar con Maven Wrapper ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /src

# Prima solo i file Maven: le dipendenze restano in cache finché pom.xml non cambia.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -q -B dependency:go-offline

COPY src/ src/
# I test (che richiedono Docker) si eseguono fuori dalla build dell'immagine.
RUN ./mvnw -q -B package -DskipTests

# ---- Runtime: solo JRE, utente non privilegiato ----
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 1001 app
COPY --from=build /src/target/plannershop-*.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
