FROM gradle:jdk19-jammy

ADD . src/

WORKDIR src/

RUN ./gradlew build

FROM openjdk:19-oracle

COPY --from=0 /home/gradle/src/build/libs/exchangebot.jar /app.jar

RUN mkdir /db/

ENV SPRING_PROFILES_ACTIVE=prod

CMD ["java", "-jar", "/app.jar"]

