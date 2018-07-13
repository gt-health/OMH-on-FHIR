FROM maven:3.5.4-jdk-8 as builder
COPY . /usr/src/app
WORKDIR /usr/src/app
RUN mvn -e package

FROM java:8-jdk
COPY --from=builder /usr/src/app/target/mdataserver-0.0.1-SNAPSHOT.jar /usr/src/app/mdataserver-0.0.1-SNAPSHOT.jar
WORKDIR /usr/src/app
EXPOSE 8080
#CMD ["tail", "-f", "/dev/nulljava"]
CMD ["java", "-jar", "/usr/src/app/mdataserver-0.0.1-SNAPSHOT.jar"]