# Payment Gateway System

A robust, scalable, and secure payment gateway system designed for modern fintech and e-commerce applications. This system supports card, wallet, and bank payments, with asynchronous processing, idempotency, fraud detection, and seamless integration with AWS and Kafka.

---

## Features

- **Multi-Method Payments:** Accepts card, wallet, and bank transfer payments.
- **API Gateway + Lambda Preprocessing:** Uses AWS API Gateway and a Node.js Lambda for JWT authentication and fraud checks before forwarding requests to the backend.
- **Spring Boot Backend:** Handles business logic, validation, persistence, and event publishing.
- **Idempotency:** Ensures exactly-once processing using Redis and idempotency keys.
- **Asynchronous Processing:** Decouples request handling and payment processing using Apache Kafka.
- **PostgreSQL Storage:** Persists all transaction data, audit logs, and webhook events.
- **Webhooks & Notifications:** Notifies downstream services and users via webhooks, email, or SNS/SQS.
- **Fraud Detection:** Basic fraud scoring in Lambda and extensible fraud logic in backend.
- **Observability:** Integrated with Prometheus, Spring Boot Actuator, and CloudWatch for monitoring and tracing.
- **Security:** JWT authentication, encrypted database, secure Kafka, and IAM for internal calls.
- **Scalable:** Horizontally scalable backend, Kafka buffering, and AWS Lambda for instant scaling.

---

## High-Level Architecture

```
Client
  |
  v
API Gateway (AWS) --(Lambda: JWT & Fraud Check)--> Spring Boot Backend
                                                      |
                                                      v
                                                Kafka (payment-events)
                                                      |
                                                      v
                                            Payment Processor (Consumer)
                                                      |
                                                      v
                                                PostgreSQL (RDS)
                                                      |
                                                      v
                                         Webhooks / SNS / SQS / Email
```

---

## Technology Stack

- **Backend:** Java 17, Spring Boot, Spring Data JPA, Spring Security, Spring Kafka, Redis
- **Database:** PostgreSQL (with Flyway migrations)
- **Messaging:** Apache Kafka (or AWS MSK)
- **Preprocessing:** AWS Lambda (Node.js)
- **API Gateway:** AWS API Gateway (REST/HTTP API)
- **Cloud:** AWS (S3, SNS, SQS, Lambda, CloudWatch)
- **Observability:** Prometheus, Micrometer, Spring Boot Actuator
- **Testing:** JUnit, Mockito, Testcontainers

---

## Example Flow

1. **Client** calls `/payments/initiate` on API Gateway.
2. **Lambda** validates JWT, performs basic fraud checks.
3. **Lambda** forwards valid requests to the Spring Boot backend.
4. **Spring Boot** saves the transaction, checks idempotency, and publishes an event to Kafka.
5. **Kafka Consumer** processes the payment asynchronously, updates the database, and emits result events.
6. **Webhooks/SNS** notify downstream services or users of payment status.

---

## Getting Started

1. **Clone the repository**
2. **Configure environment variables** for database, Kafka, Redis, AWS, and JWT.
3. **Run database migrations** (Flyway).
4. **Start the Spring Boot application**
5. **Deploy the Lambda preprocessor** (see `lambda/paymentPreprocessor.js`)
6. **Set up API Gateway** to route `/payments/initiate` to the Lambda.

---

## Extensibility

- Plug in advanced fraud detection (ML, 3rd-party APIs)
- Add more payment providers (Stripe, PayPal, etc.)
- Integrate with additional notification channels (SMS, push, etc.)
- Enhance observability with distributed tracing

---

## License

MIT or your preferred license. 