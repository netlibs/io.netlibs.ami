## build first.
FROM gradle:6.7-jdk14 AS build
RUN apt-get update && apt-get -y install binutils

ARG GITHUB_ACTOR
ARG GITHUB_TOKEN

COPY --chown=gradle:gradle . /home/gradle/src

WORKDIR /home/gradle/src

RUN gradle jib --no-daemon --console plain
