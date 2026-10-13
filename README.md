# Shoploc Identity Service

## Overview

The `shoploc-identity-service` is the identity and authentication microservice of the Shoploc platform.

It is responsible for managing the identity-related aspects of the platform and integrating with the authentication infrastructure.

Authentication and identity management are handled using **Keycloak**.

---

## Responsibilities

The Identity Service is responsible for:

- Authentication-related integration
- User identity management
- Role-based access control integration
- Integration with Keycloak
- Providing identity-related APIs required by the Shoploc platform

> Business validation and merchant account approval are handled by the
> `shoploc-governance-service`.

---

## Architecture

The service is part of the Shoploc microservices architecture.

```text
                    ┌─────────────────────┐
                    │    API Gateway      │
                    └──────────┬──────────┘
                               │
                               ▼
                    ┌─────────────────────┐
                    │ Identity Service    │
                    │                     │
                    │ Spring Boot         │
                    │ Java 21             │
                    └──────────┬──────────┘
                               │
                               ▼
                    ┌─────────────────────┐
                    │      Keycloak       │
                    │ Authentication /    │
                    │ Identity Management  │
                    └─────────────────────┘