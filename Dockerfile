FROM ubuntu:latest

WORKDIR Snark

COPY . .

RUN ./requirments.sh
RUN ./gradlew build test