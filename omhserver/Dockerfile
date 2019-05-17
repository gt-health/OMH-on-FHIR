FROM maven:3.6.1-jdk-8 as builder
COPY . /usr/src/app
WORKDIR /usr/src/app
RUN mvn package
FROM java:8-jdk
COPY --from=builder /usr/src/app/application/target/omh-server-0.0.1-SNAPSHOT.jar /usr/src/app/omh-server-0.0.1-SNAPSHOT.jar
EXPOSE 8080
#CMD ["tail", "-f", "/dev/null"]
CMD ["java", "-jar", "/usr/src/app/omh-server-0.0.1-SNAPSHOT.jar"]
