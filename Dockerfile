# Spark batch job image: Maven build, then a JRE image running the jobs on local Spark.
FROM maven:3.9-eclipse-temurin-11 AS build
WORKDIR /src
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests \
 && mvn -B -q dependency:copy-dependencies -DincludeScope=runtime -DoutputDirectory=target/lib

FROM eclipse-temurin:11-jre
WORKDIR /app
# Exploded classpath instead of the Spring Boot fat jar: Spark's code generation and
# service loading are more reliable with plain jars on the classpath.
COPY --from=build /src/target/lib ./lib
COPY --from=build /src/target/classes ./classes
COPY src/main/resources/data ./data
COPY docker/run-pipeline.sh /usr/local/bin/run-pipeline
RUN chmod +x /usr/local/bin/run-pipeline
ENV JAVA_OPTS="-Xmx1g"
ENTRYPOINT ["run-pipeline"]
