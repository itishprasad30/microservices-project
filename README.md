# Microservices Architecture Diagram

``` text
┌─────────────┐
│   Client    │
│  (Browser/  │
│   Mobile)   │
└──────┬──────┘
       │
       ▼
┌─────────────────────────────────────────────────┐
│           API GATEWAY (Port 8080)               │
│  - Route: /api/users/**    → User Service       │
│  - Route: /api/orders/**   → Order Service      │
│  - Route: /api/notifications/** → Notification  │
└──────┬──────────────────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────────────────┐
│       SERVICE DISCOVERY (Eureka Port 8761)      │
│  - User Service registered                      │
│  - Order Service registered                     │
│  - Notification Service registered              │
└─────────────────────────────────────────────────┘
       │
       ▼
┌───────────────┐  ┌───────────────┐  ┌─────────────────────┐
│ User Service  │  │ Order Service │  │ Notification Service│
│   (8081)      │  │   (8082)      │  │      (8083)         │
│  - User CRUD  │  │  - Orders     │  │  - Send Notifications│
│  - Redis Cache│  │  - Kafka      │  │  - Kafka Consumer   │
│  - MySQL      │  │  - Redis      │  │  - MySQL            │
└───────────────┘  └───────┬───────┘  └─────────────────────┘
                           │
                           ▼
                    ┌─────────────┐
                    │    Kafka    │
                    │   Events    │
                    └─────────────┘
```
