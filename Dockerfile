FROM ubuntu:latest

RUN apt-get update
RUN apt-get install -y curl zip unzip

ARG JAVA_VERSION="17.0.7-zulu"

WORKDIR /app

COPY ./snark-main/ci ./snark-main/ci

RUN ./snark-main/ci/install_sdk.sh
RUN ./snark-main/ci/install_java.sh "$JAVA_VERSION"

COPY . .

RUN ./requirments.sh

RUN ./gradlew clean build

EXPOSE 8080

CMD ./gradlew :snark-main:run_server