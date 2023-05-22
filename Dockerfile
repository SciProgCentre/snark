FROM ubuntu:latest

RUN apt-get update
RUN apt-get install -y curl zip unzip

ARG JAVA_VERSION="17.0.7-zulu"

WORKDIR /app

COPY ./snark-main/ci ./snark-main/ci

RUN ./snark-main/ci/install_sdk.sh
RUN ./snark-main/ci/install_java.sh "$JAVA_VERSION"

COPY . .

RUN ./requirements.sh

RUN bash -c "source ~/.sdkman/bin/sdkman-init.sh && ./gradlew clean build"

EXPOSE 8080

RUN mkdir -p ~/.aws/ && ln -s /run/secrets/credentials.json ~/.aws/credentials.json

CMD bash -c "source ~/.sdkman/bin/sdkman-init.sh && ./gradlew :snark-main:run_server"