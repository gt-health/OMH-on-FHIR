FROM maven:3.6.1-jdk-8 as builder
COPY . /usr/src/app
WORKDIR /usr/src/app
RUN mvn clean install