# Event-Driven Job Application Platform

**Production-Grade Backend System for High-Scale Job Applications**

## 🎯 Project Goal

Build a scalable, fault-tolerant job application platform that handles thousands of concurrent applications without breaking the database or degrading user experience.

## 🏗 Architecture Overview

```
React (S3 + CloudFront)
    ↓
API Gateway (Rate Limiting, Auth, Throttling)
    ↓
Spring Boot Services (ECS Fargate)
    ↓
Kafka (MSK - KRaft mode)
    ↓
Async Workers (Notification, Analytics)
    ↓
MySQL (RDS with Read Replicas)
    ↓
Redis (ElastiCache - Cache + Rate Limiting)
```

## 📦 Services

### Core Services
- **User Service**: Authentication, JWT, rate limiting, session management
- **Job Service**: Job CRUD, search with heavy indexing, caching
- **Application Service**: Job applications, event publishing, outbox pattern
- **Notification Service**: Async email/SMS/push notifications
- **Analytics Service**: Event aggregation, write-behind caching, batch writes

### Service Design Principles
- Each service owns its database schema (no shared DB)
- Event-driven communication via Kafka
- Independent scaling and deployment
- Bulkhead pattern for failure isolation

## 🔥 Key Technical Highlights

### Database Design
- MySQL with production-grade indexing
- Composite indexes for search queries
- Deadlock prevention strategies
- Connection pool protection (HikariCP)
- READ COMMITTED isolation for write-heavy paths

### Kafka Architecture
- Topics: `application.created`, `job.lifecycle`, `notification.requested`
- Partition key: `job_id` for ordering guarantees
- Avro schemas with backward compatibility
- At-least-once delivery + idempotent consumers
- Consumer lag monitoring

### Redis Caching Strategies
- **Cache-Aside**: Job search results (TTL: 60s)
- **Read-Through**: Hot job details (TTL: 5min)
- **Write-Through**: User profiles (TTL: 30min)
- **Write-Behind**: Analytics counters (flush-driven)
- Stale-while-revalidate for cache stampede prevention

### Spring Boot Optimizations
- Bounded thread pools aligned with DB capacity
- Bulkhead pattern for resource isolation
- Circuit breakers for DB and Redis
- G1GC / ZGC for predictable pause times
- HikariCP tuning for connection efficiency

### AWS Serverless Deployment
- **API Gateway**: Entry point, throttling, protection
- **ECS Fargate**: Spring Boot services (avoid Lambda cold starts)
- **MSK**: Managed Kafka with KRaft
- **RDS MySQL**: Primary + read replicas
- **ElastiCache**: Redis for caching and rate limiting
- **CloudWatch + Prometheus + Grafana**: Monitoring

## 🚨 Production Failure Scenarios Handled

1. **Thread Pool Starvation**: Bounded pools, fail-fast behavior
2. **Kafka Consumer Lag**: Horizontal scaling, partition tuning
3. **Cache Stampede**: Distributed locks, stale-while-revalidate
4. **GC Pauses**: Heap tuning, reduced batch sizes
5. **DB Deadlocks**: Consistent update order, short transactions
6. **Duplicate Events**: Idempotent consumers, DB constraints
7. **Thundering Herd**: Rate limiting, backpressure, circuit breakers

## 📊 Monitoring & Observability

- API latency (p95, p99)
- Kafka consumer lag
- JVM heap usage & GC pause time
- DB connection pool saturation
- Cache hit rates
- Error rates by service

## 🔐 Security

- JWT authentication with refresh tokens
- Token blacklisting for revocation
- Rate limiting (login, API calls)
- SQL injection prevention
- Input validation
- IAM roles with least privilege
- VPC isolation

## 📈 Scalability

- Horizontal scaling via ECS auto-scaling
- Kafka absorbs traffic spikes
- DB protected by cache and rate limiting
- Graceful degradation under load
- No single point of failure

## 🧪 Testing Strategy

- Unit tests (JUnit, Mockito)
- Integration tests (Testcontainers: MySQL + Kafka)
- Load testing (k6 / JMeter)
- Chaos testing (kill pods, introduce latency)

## 🎓 Key Learnings

**Scaling is not about adding more threads or pods.**

It's about:
- Protecting the slowest dependency
- Aligning concurrency with downstream capacity
- Making failure explicit and graceful
- Designing for partial failure, not perfect success

## 📚 Documentation

- [ARCHITECTURE.md](./ARCHITECTURE.md) - Detailed system design
- [DECISIONS.md](./DECISIONS.md) - Why Redis, why Kafka, why MySQL
- [SYSTEM_FAILURES.md](./SYSTEM_FAILURES.md) - Incident stories and fixes

## 🚀 Getting Started

See individual service READMEs for setup instructions:
- [User Service](./services/user-service/README.md)
- [Job Service](./services/job-service/README.md)
- [Application Service](./services/application-service/README.md)
- [Notification Service](./services/notification-service/README.md)
- [Analytics Service](./services/analytics-service/README.md)

## 💡 Interview Talking Points

This project demonstrates:
- Production-grade architecture, not tutorial code
- Understanding of failure modes and recovery
- Cost-aware AWS deployment
- Real performance bottlenecks (GC, thread pools, DB)
- Event-driven design with consistency trade-offs
