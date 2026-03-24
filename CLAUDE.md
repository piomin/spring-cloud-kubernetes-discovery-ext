# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

A Spring Cloud library that enables Spring Boot applications running **outside** Kubernetes to register themselves with Kubernetes service discovery. This creates a hybrid microservices architecture where external services can be discovered by services running inside Kubernetes.

**Published on Maven Central**: `com.github.piomin:spring-cloud-kubernetes-discovery-ext:1.0.0.RELEASE`

## Architecture

### Two-Module System

1. **spring-cloud-kubernetes-discovery-ext-client** (Library Module)
   - Provides `KubernetesServiceRegistry` implementing Spring Cloud's `ServiceRegistry` interface
   - Auto-registers external applications as Kubernetes Services and Endpoints
   - Creates Kubernetes resources with `external=true` label for identification
   - Enabled by setting `spring.cloud.kubernetes.discovery.register=true`
   - Optional: Configure static IP via `spring.cloud.kubernetes.discovery.ipAddress`

2. **spring-cloud-kubernetes-discovery-ext-watcher** (Application Module)
   - Standalone Spring Boot application that monitors external service health
   - `LivenessScheduler`: Polls all external endpoints every 10 seconds via `/actuator/health`
   - `DeactivateServiceTask`: Retries unhealthy services 3 times (3 seconds apart) before deregistration
   - Uses async processing with `@EnableScheduling` and `@EnableAsync`

### Key Implementation Details

- External services identified by endpoints with label `external=true`
- Registration creates both Kubernetes Service and Endpoints objects
- Service metadata includes `healthUrl` annotation pointing to `/actuator/health`
- Watcher maintains a list of watched URLs to avoid duplicate health checks
- Deregistration removes endpoint addresses from subsets (cleanup of empty subsets is TODO)

## Build & Test

### Build the project
```bash
mvn clean install
```

### Run tests
```bash
mvn test
```

### Run tests for a single module
```bash
mvn test -pl spring-cloud-kubernetes-discovery-ext-client
```

### Build Docker image (watcher module only)
```bash
cd spring-cloud-kubernetes-discovery-ext-watcher
mvn compile jib:dockerBuild
```

### Run SonarCloud analysis
```bash
mvn verify sonar:sonar -DskipTests
```

## Technology Stack

- **Java**: 21
- **Spring Boot**: 4.0.4
- **Spring Cloud**: 2025.1.1
- **Kubernetes Client**: Fabric8 kubernetes-client
- **Build Tool**: Maven
- **CI/CD**: CircleCI with SonarCloud integration

### 1. Plan Mode Default
- Enter plan mode for ANY not-trivial task (3+ steps or architectural decisions)
- Use plan mode for verification steps, not just building
- Write detailed specs upfront to reduce ambiguity

### 2. Self-Improvement Loop
- After ANY correction from the user: update `tasks/lessons.md` with the pattern
- Write rules for yourself that prevent the same mistake
- Ruthlessly iterate on these lessons until the mistake rate drops
- Review lessons at session start for a project

### 3. Verification Before Done
- Never mark a task complete without proving it works
- Diff behavior between main and your changes when relevant
- Ask yourself: "Would a staff engineer approve this?"
- Run tests, check logs, demonstrate correctness

### 4. Demand Elegance (Balanced)
- For non-trivial changes: pause and ask "is there a more elegant way?"
- If a fix feels hacky: "Knowing everything I know now, implement the elegant solution"
- Skip this for simple, obvious fixes. Don't overengineer
- Challenge your own work before presenting it

## Core Principles
- **Simplicity First**: Make every change as simple as possible. Impact minimal code
- **No Laziness**: Find root causes. No temporary fixes. Senior developer standards

## Project General Instructions

- Always use the latest versions of dependencies.
- Always write Java code as the Spring Boot application.
- Always use Maven for dependency management.
- Always create test cases for the generated code both positive and negative.
- Always generate the CircleCI pipeline in the .circleci directory to verify the code.
- Minimize the amount of code generated.
- The Maven artifact name must be the same as the parent directory name.
- Use semantic versioning for the Maven project. Each time you generate a new version, bump the PATCH section of the version number.
- Use `pl.piomin.services` as the group ID for the Maven project and base Java package.
- Do not use the Lombok library.
- Generate the Docker Compose file to run all components used by the application.
- Update README.md each time you generate a new version.
