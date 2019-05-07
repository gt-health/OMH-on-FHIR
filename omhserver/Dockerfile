FROM maven:3.5.4-jdk-8 as builder
COPY . /usr/src/app
WORKDIR /usr/src/app
RUN mvn package
FROM java:8-jdk
COPY --from=builder /usr/src/app/target/mdataserver-0.0.1-SNAPSHOT.jar /usr/src/app/mdataserver-0.0.1-SNAPSHOT.jar
COPY --from=builder /usr/src/app/src/test/resources/testconfig1.json /testconfig1.json
COPY --from=builder /usr/src/app/src/test/resources/testomhresponse1.json /testomhresponse1.json
EXPOSE 8080
#CMD ["tail", "-f", "/dev/nulljava"]
CMD ["java", "-jar", "/usr/src/app/mdataserver-0.0.1-SNAPSHOT.jar"]
