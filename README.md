# delivery-gateway-service

Spring Boot 3 (Java 21) Delivery Gateway Service.

## Environment Variables

- `SERVER_PORT` (default `8106` when running in Docker/local container)
- `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` (MariaDB connection)
- `JWT_JWK_SET_URI` (URL to expose JWKs for JWT validation)
- `EMAIL_*` variables for SMTP (optional)
- `CREDITS_ENABLED`, `SMS_COST`, `WHATSAPP_COST`, `EMAIL_COST` (prepaid wallet configuration)

## Running Locally

```bash
mvn -q clean test
```

```bash
docker compose up --build
```

## API

- Swagger UI: `http://localhost:8106/swagger-ui`
- OpenAPI: `http://localhost:8106/v3/api-docs`

### Example CURLs (replace `<jwt>` with your JWT)

```bash
curl -X POST http://localhost:8106/api/v1/deliveries \
  -H "Authorization: Bearer <dummy-jwt>" \
  -H "Content-Type: application/json" \
  -d '{
    "attemptId": "att-123",
    "channel": "EMAIL",
    "to": "user@example.com, user2@example.com",
    "deliveryMode": "INDIVIDUAL",
    "subject": "Hello",
    "content": {
      "text": "You have a message"
    }
  }'
```

```bash
curl http://localhost:8106/api/v1/deliveries/att-123 \
  -H "Authorization: Bearer <dummy-jwt>"
```

```bash
curl -X POST http://localhost:8106/api/v1/wallet/topups \
  -H "Authorization: Bearer <dummy-jwt>" \
  -H "Content-Type: application/json" \
  -d '{"amount":5}'
```

```bash
curl http://localhost:8106/api/v1/wallet \
  -H "Authorization: Bearer <dummy-jwt>"
```
