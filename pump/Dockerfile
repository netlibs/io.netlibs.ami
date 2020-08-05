## build first.
FROM gradle:6.5.1-jdk14 AS build
RUN apt-get update && apt-get -y install binutils

ARG GITHUB_ACTOR
ARG GITHUB_TOKEN
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src


RUN gradle runtime --no-daemon --console plain

## create target image.
FROM adoptopenjdk:14.0.1_7-jre-hotspot
RUN mkdir /app
COPY --from=build /home/gradle/src/pump/build/jre/* /app/
ENTRYPOINT [ "/app/bin/pump" ]
