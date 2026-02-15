# delivery-gateway-service

Spring Boot 3 (Java 21) Service fuer Delivery-Gateway-Funktionen.

## Swagger / OpenAPI

- Swagger UI: `http://localhost:8080/swagger-ui`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

Hinweis: Die Endpunkte sind nur erreichbar, wenn der Service lokal laeuft.

## Docker Compose

### Start

```bash
docker compose up -d --build
```

### Stop

```bash
docker compose down
```

### Reset (DB + Container + Rebuild)

```bash
docker compose down -v --remove-orphans
docker compose up -d --build
```
