# aegis-backend

Production-ready Spring Boot 3 backend service.

## Requirements

- **Java 21**
- **Maven 3.9+**

## Build & Run

```bash
# Build
./mvnw clean package

# Run
./mvnw spring-boot:run
```

The app starts on **http://localhost:8080**.

## Endpoints

| Method | Path      | Description        |
| ------ | --------- | ------------------ |
| GET    | `/health` | Application health |

## Project Structure

```
src/main/java/com/aegis/backend/
├── AegisBackendApplication.java   # Entry point
├── controller/                    # REST controllers
├── service/                       # Business logic
├── agent/                         # AI agent orchestration (TBD)
├── tools/                         # Agent tool definitions (TBD)
└── config/                        # Spring configuration
```
