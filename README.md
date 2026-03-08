# inventory-management – Java Microservices Assignment

Two Spring Boot microservices simulating an e-commerce inventory and order management system.

---

## Architecture Overview

```
┌─────────────────────┐          REST          ┌──────────────────────┐
│    Order Service    │ ──────────────────────▶ │  Inventory Service   │
│    (port 8082)      │                         │    (port 8081)       │
│                     │◀────────────────────── │                      │
│  POST /order        │    JSON responses       │  GET  /inventory/{id}│
│  H2 (orderdb)       │                         │  POST /inventory/    │
│                     │                         │       update         │
└─────────────────────┘                         │  H2 (inventorydb)    │
                                                └──────────────────────┘
```

### Key Design Decisions

- **Factory Pattern** in Inventory Service: `InventoryHandlerFactory` resolves handlers by strategy name (currently `FEFO` – First Expiry, First Out). Adding a new strategy (e.g. `LIFO`) requires only a new `@Component("LIFO")` implementation of `InventoryHandler` — zero changes to existing classes.
- **FEFO strategy**: Pharmaceutically appropriate — stock with the earliest expiry date is consumed first.
- **RestTemplate** for inter-service communication from Order Service.
- **Liquibase** changelogs auto-run at startup, creating tables and loading seed data from CSV files.
- **Swagger/OpenAPI** available at `/swagger-ui.html` on each service.

---

## Project Structure

```
korber-pharma/
├── inventory-service/
│   ├── src/main/java/com/korber/inventory/
│   │   ├── controller/  InventoryController.java
│   │   ├── service/     InventoryService.java
│   │   ├── repository/  InventoryBatchRepository.java
│   │   ├── model/       InventoryBatch.java
│   │   ├── dto/         InventoryDtos.java
│   │   └── factory/     InventoryHandler.java (interface)
│   │                    FefoInventoryHandler.java
│   │                    InventoryHandlerFactory.java
│   └── src/main/resources/db/changelog/  (Liquibase + CSV)
│
└── order-service/
    ├── src/main/java/com/korber/order/
    │   ├── controller/  OrderController.java
    │   ├── service/     OrderService.java
    │   ├── repository/  OrderRepository.java
    │   ├── model/       Order.java
    │   ├── dto/         OrderDtos.java
    │   ├── client/      InventoryClient.java
    │   └── config/      AppConfig.java (RestTemplate bean)
    └── src/main/resources/db/changelog/  (Liquibase + CSV)
```

---

## Prerequisites

- Java 17+
- Maven 3.8+

---

## Setup & Running

### 1. Start Inventory Service

```bash
cd inventory-service
mvn spring-boot:run
```

Service starts on **http://localhost:8081**

### 2. Start Order Service

Open a **new terminal**:

```bash
cd order-service
mvn spring-boot:run
```

Service starts on **http://localhost:8082**

> **Order Service must be started after Inventory Service** because placing orders makes live HTTP calls to Inventory Service.

---

## API Documentation

### Inventory Service (port 8081)

Swagger UI: http://localhost:8081/swagger-ui.html  
H2 Console: http://localhost:8081/h2-console (JDBC URL: `jdbc:h2:mem:inventorydb`)

#### GET /inventory/{productId}

Returns all inventory batches for a product, sorted by expiry date (earliest first).

**Example:**
```
GET http://localhost:8081/inventory/1005
```

**Response 200:**
```json
{
  "productId": 1005,
  "productName": "Smartwatch",
  "batches": [
    { "batchId": 5, "quantity": 39, "expiryDate": "2026-03-31" },
    { "batchId": 7, "quantity": 40, "expiryDate": "2026-04-24" },
    { "batchId": 2, "quantity": 52, "expiryDate": "2026-05-30" }
  ]
}
```

**Response 404:** Product not found in inventory.

---

#### POST /inventory/update

Deducts stock using FEFO strategy.

**Request:**
```json
{ "productId": 1005, "quantityToDeduct": 50 }
```

**Response 200:**
```json
{
  "success": true,
  "reservedFromBatchIds": [5, 7],
  "message": "Inventory updated successfully."
}
```

**Response 400 (insufficient stock):**
```json
{
  "success": false,
  "reservedFromBatchIds": [],
  "message": "Insufficient stock for product 1005"
}
```

---

### Order Service (port 8082)

Swagger UI: http://localhost:8082/swagger-ui.html  
H2 Console: http://localhost:8082/h2-console (JDBC URL: `jdbc:h2:mem:orderdb`)

#### POST /order

Places an order. Internally checks availability and deducts stock from Inventory Service.

**Request:**
```json
{ "productId": 1002, "quantity": 3 }
```

**Response 200:**
```json
{
  "orderId": 11,
  "productId": 1002,
  "productName": "Smartphone",
  "quantity": 3,
  "status": "PLACED",
  "reservedFromBatchIds": [9],
  "message": "Order placed. Inventory reserved."
}
```

**Response 400 (insufficient stock):**
```json
{
  "productId": 1002,
  "productName": "Smartphone",
  "quantity": 9999,
  "status": "FAILED",
  "message": "Insufficient stock. Available: 112"
}
```

---

## Running Tests

### Inventory Service

```bash
cd inventory-service
mvn test
```

Tests included:
- `InventoryServiceTest` – unit tests with Mockito (FEFO logic, stock deduction, edge cases)
- `InventoryControllerTest` – `@WebMvcTest` covering all endpoint responses
- `InventoryIntegrationTest` – `@SpringBootTest` with real H2 + Liquibase data

### Order Service

```bash
cd order-service
mvn test
```

Tests included:
- `OrderServiceTest` – unit tests with Mockito (success, product not found, insufficient stock, update failure)
- `OrderControllerTest` – `@WebMvcTest` covering success and failure responses
- `OrderIntegrationTest` – `@SpringBootTest` using `MockRestServiceServer` to simulate Inventory Service

---

## Seed Data

### Inventory (loaded automatically at startup)

| Batch | Product | Name | Qty | Expiry |
|-------|---------|------|-----|--------|
| 1 | 1001 | Laptop | 68 | 2026-06-25 |
| 2 | 1005 | Smartwatch | 52 | 2026-05-30 |
| 3 | 1004 | Headphones | 20 | 2026-08-12 |
| 4 | 1003 | Tablet | 35 | 2026-09-03 |
| 5 | 1005 | Smartwatch | 39 | 2026-03-31 |
| 6 | 1004 | Headphones | 56 | 2026-06-06 |
| 7 | 1005 | Smartwatch | 40 | 2026-04-24 |
| 8 | 1003 | Tablet | 21 | 2026-09-09 |
| 9 | 1002 | Smartphone | 29 | 2026-05-31 |
| 10 | 1002 | Smartphone | 83 | 2026-11-15 |

### Orders (loaded automatically at startup)

10 historical orders pre-loaded with statuses: DELIVERED, PLACED, SHIPPED.

---

## Extending with a New Inventory Strategy

To add a LIFO (Last In, First Out) strategy:

1. Create a new class:
```java
@Component("LIFO")
@RequiredArgsConstructor
public class LifoInventoryHandler implements InventoryHandler {
    // implement getInventory() and updateInventory()
}
```

2. No other files need to change — `InventoryHandlerFactory` picks it up automatically.

3. Call it via: `handlerFactory.getHandler("LIFO")`
