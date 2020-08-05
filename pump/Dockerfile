## build first.
FROM gradle:6.5.1-jdk14 AS build
RUN apt-get update && apt-get -y install binutils

ARG GITHUB_ACTOR
ARG GITHUB_TOKEN
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src


RUN gradle runtime --no-daemon --console plain

## create target image.
FROM debian:stable-slim
RUN mkdir /app
COPY --from=build /home/gradle/src/pump/build/image /app/
ENTRYPOINT [ "/app/bin/pump" ]
