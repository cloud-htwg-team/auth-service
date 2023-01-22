# syntax = docker/dockerfile:experimental

### Build Image ##

FROM gradle:7.5.1-jdk17-alpine AS builder
COPY --chown=gradle:gradle . /build
WORKDIR /build
RUN gradle build --no-daemon
#RUN echo $(ls -1 /build/build/libs)
#RUN ls -1 /build/build/libs

### RUN Image ##

# use JRE as JDK can be a security vulnerability; alpine is a slim version, then resulting image is 300MB smaller
FROM eclipse-temurin:17-jre-alpine
ARG GOOGLE_CLOUD_IDENTITY_PLATFORM_API_KEY
ENV GOOGLE_CLOUD_IDENTITY_PLATFORM_API_KEY ${GOOGLE_CLOUD_IDENTITY_PLATFORM_API_KEY}
# -- will create the directory, set's path context --> ./ == /opt/app
WORKDIR /opt/app
RUN addgroup --system javauser && adduser -S -s /usr/sbin/nologin -G javauser javauser
# there is a command 'ADD' but 'COPY" is prefered
COPY --from=builder /build/build/libs/auth-microservice.jar ./auth-microservice.jar

RUN chown -R javauser:javauser .
USER javauser

# ENTRYPOINT ["java", "-Dgoogle.cloud.identity-platform.api-key=${GOOGLE_CLOUD_IDENTITY_PLATFORM_API_KEY}", "-jar", "./auth-microservice.jar"]
ENTRYPOINT ["java", "-jar", "./auth-microservice.jar"]


# Optimizing Docker Sprring Boot images
# https://mydeveloperplanet.com/2022/11/30/docker-best-practices/
# https://mydeveloperplanet.com/2022/12/14/spring-boot-docker-best-practices/