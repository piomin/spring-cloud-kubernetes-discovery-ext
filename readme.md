## Spring Cloud Library for External Kubernetes Discovery [![Twitter](https://img.shields.io/twitter/follow/piotr_minkowski.svg?style=social&logo=twitter&label=Follow%20Me)](https://twitter.com/piotr_minkowski)

[![CircleCI](https://circleci.com/gh/piomin/sample-spring-cloud-kubernetes-discovery-ext.svg?style=svg)](https://circleci.com/gh/piomin/sample-spring-cloud-kubernetes-discovery-ext)

[![SonarCloud](https://sonarcloud.io/images/project_badges/sonarcloud-black.svg)](https://sonarcloud.io/dashboard?id=piomin_spring-cloud-kubernetes-discovery-ext)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=piomin_spring-cloud-kubernetes-discovery-ext&metric=bugs)](https://sonarcloud.io/dashboard?id=piomin_spring-cloud-kubernetes-discovery-ext)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=piomin_spring-cloud-kubernetes-discovery-ext&metric=coverage)](https://sonarcloud.io/dashboard?id=piomin_spring-cloud-kubernetes-discovery-ext)
[![Lines of Code](https://sonarcloud.io/api/project_badges/measure?project=piomin_spring-cloud-kubernetes-discovery-ext&metric=ncloc)](https://sonarcloud.io/dashboard?id=piomin_spring-cloud-kubernetes-discovery-ext)

### Motivation

The article which describes motivation for creating library and showing use case for it with source code example: 
[Spring Cloud Kubernetes For Hybrid Microservices Architecture](https://piotrminkowski.com/2020/01/03/spring-cloud-kubernetes-for-hybrid-microservices-architecture/)

### Usage

The library is published on Maven Central. Current version is `1.0.0.RELEASE`
```
<dependency>
  <groupId>com.github.piomin</groupId>
  <artifactId>spring-cloud-kubernetes-discovery-ext</artifactId>
  <version>1.0.0.RELEASE</version>
</dependency>
```

The registration is still disabled, since we won't set property `spring.cloud.kubernetes.discovery.register` to `true`.
```
spring:
  cloud:
    kubernetes:
      discovery:
        register: true
```
It might be usable to set static IP address in configuration, in case you would have multiple network interfaces.
```
spring:
  cloud:
    kubernetes:
      discovery:
        ipAddress: 192.168.99.1
```