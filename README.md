# Apache Kafka Notification System 📨

A real-time notification system built with **Spring Boot**, **Apache Kafka**, **PostgreSQL**, and **WebSockets** that supports both in-app and email notifications with real-time acknowledgments.

**Quick Summary**: Event-driven notification system using Apache Kafka for asynchronous message processing, WebSockets for real-time delivery, and PostgreSQL for persistence. Demonstrates microservices patterns, producer-consumer architecture, and resilient messaging with retry mechanisms.

---

## 📋 Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Why Kafka](#why-kafka)
- [Architecture](#architecture)
- [Technology Stack](#technology-stack)
- [Prerequisites](#prerequisites)
- [Installation & Setup](#installation--setup)
- [Running the Application](#running-the-application)
- [Usage & Testing](#usage--testing)
- [API Endpoints](#api-endpoints)
- [Database Schema](#database-schema)
- [pgAdmin Setup](#pgadmin-setup)
- [Kafka Topics](#kafka-topics)
- [Limitations](#limitations)
- [Future Enhancements](#future-enhancements)
- [Troubleshooting](#troubleshooting)
- [Project Structure](#project-structure)

---

## 💡 What You'll Learn

This project demonstrates:
- **Event-Driven Architecture**: Async messaging with Kafka
- **Real-Time Communication**: WebSocket implementation with STOMP
- **Resilient Systems**: Retry logic, error handling, DLQ patterns
- **Microservices Patterns**: Producer-Consumer, Event Sourcing
- **Database Design**: PostgreSQL with Spring Data JPA
- **DevOps**: Docker Compose orchestration, containerization
- **Full-Stack Integration**: Backend APIs + Frontend real-time UI

---

## 🌟 Overview

This application demonstrates a production-ready notification system using Apache Kafka for event streaming. It supports:
- **In-App Notifications**: Real-time notifications delivered to students via WebSocket
- **Email Notifications**: Email messages sent through Kafka topics
- **Acknowledgment System**: Students can acknowledge notifications in real-time
- **Retry Mechanism**: Failed messages are automatically retried
- **Persistence**: All notifications are stored in PostgreSQL database

---

## ✨ Features

### Core Features
- ✅ Real-time in-app notifications via WebSocket (STOMP protocol)
- ✅ Email notification system using Kafka
- ✅ Student acknowledgment tracking
- ✅ Automatic retry mechanism for failed messages
- ✅ PostgreSQL database persistence
- ✅ Kafka-based message streaming
- ✅ Docker containerization for all services
- ✅ pgAdmin for database management
- ✅ Kafka UI for monitoring Kafka topics

### Admin Panel Features
- Send in-app notifications to specific students
- Send email notifications to specific email addresses
- Real-time acknowledgment monitoring
- Separate columns for different notification types

### Student Panel Features
- Receive real-time notifications
- Send acknowledgments back to admin
- View notification history

---

## 🤔 Why Apache Kafka?

Apache Kafka is essential to this architecture because it provides:

1. **Decoupling**: Notification producers (Admin) are decoupled from consumers (Email/WebSocket)
   - Producers don't wait for consumers
   - Consumers can process at their own pace
   
2. **High Throughput**: Handles thousands of notifications per second without blocking

3. **Non-Blocking API Responses**: Controllers return immediately (202 Accepted) while Kafka processes asynchronously
   - Users get instant feedback: "Notification accepted"
   - Actual delivery happens in the background

4. **Built-in Retry & Failure Handling**: Failed messages can be retried with exponential backoff
   - Email retries 3 times (1min, 5min, 15min)
   - Permanently failed messages are tracked

5. **Future Scalability**: Easy to add new notification channels
   - SMS notifications
   - Push notifications (FCM)
   - Slack/Teams messages
   - All consuming from the same Kafka topics

This design follows the **producer-consumer pattern**, making the system responsive, resilient, and scalable.

---

## 🏗️ Architecture

### In-App Notification Flow

```
┌─────────────────────┐
│   Admin Panel       │
│  (send-notif.html)  │
└──────────┬──────────┘
           │ POST /api/inapp/send
           ▼
┌─────────────────────────────────┐
│  InAppNotificationController    │
│  (Immediate Response: ACCEPTED) │
└──────────┬──────────────────────┘
           │
           ▼
┌────────────────────────────┐
│ InAppNotificationProducer  │
│  (Fast: ~milliseconds)     │
└──────────┬─────────────────┘
           │
           ▼
     ┌──────────────────┐
     │ inapp_           │
     │ notifications    │
     │ (Kafka Topic)    │
     └────────┬─────────┘
              │
              ▼
    ┌──────────────────────────┐
    │ InAppNotificationConsumer │
    │  (Kafka Consumer)         │
    └─────────┬────────────────┘
              │
        ┌─────┴─────┐
        │           │
        ▼           ▼
   ┌─────────┐  ┌─────────────────────┐
   │ Save to │  │ Push via WebSocket   │
   │  DB     │  │ (/topic/notifications)
   └─────────┘  └─────────────────────┘
        │           │
        └─────┬─────┘
              ▼
    ┌──────────────────────┐
    │  Student Panel       │
    │  (student.html)      │
    │  Receives in Real-time
    └──────────────────────┘
```

### Email Notification Flow

```
┌──────────────────────┐
│   Admin Panel        │
│ (send-notif.html)    │
└──────────┬───────────┘
           │ POST /notify/email
           ▼
┌──────────────────────────────┐
│  NotificationController      │
│  (Immediate Response: ACCEPTED)
└──────────┬───────────────────┘
           │
           ▼
┌────────────────────────┐
│ NotificationProducer   │
│  (Fast: ~milliseconds) │
└──────────┬─────────────┘
           │
           ▼
     ┌──────────────────┐
     │ email_           │
     │ notifications    │
     │ (Kafka Topic)    │
     └────────┬─────────┘
              │
              ▼
    ┌──────────────────────────┐
    │ NotificationConsumer     │
    │  (Kafka Consumer)        │
    └─────────┬────────────────┘
              │
        ┌─────┴─────────────────────┐
        │                           │
        ▼                           ▼
   ┌──────────────┐        ┌──────────────────┐
   │ Retry Logic  │        │ Attempt Send     │
   │ 3x with      │        │ via SMTP         │
   │ Backoff:     │        │                  │
   │ 1m, 5m, 15m  │        └────────┬─────────┘
   └──────┬───────┘                 │
          │         ┌───────────────┼───────────────┐
          │         │               │               │
          │         ▼               ▼               ▼
          │      ┌─────────┐    ┌─────────┐   ┌──────────┐
          │      │ Success │    │ Retry   │   │ Max      │
          │      │ Save to │    │ Failed  │   │ Retries  │
          │      │ DB      │    │ Backoff │   │ Exceeded │
          │      └─────────┘    └────┬────┘   └────┬─────┘
          │                          │             │
          └──────────────┬───────────┘             │
                         │                        │
                         ▼                        ▼
                  ┌─────────────┐         ┌──────────────────┐
                  │   DB Table  │         │ failed_message   │
                  │ (SUCCESSFUL)│         │   TABLE (MARK)   │
                  │notification│         │ PERMANENTLY      │
                  │_message     │         │ FAILED           │
                  └─────────────┘         └──────────────────┘
```

---

## 🛠️ Technology Stack

### Backend
- **Java 17**
- **Spring Boot 3.5.7**
- **Spring Kafka** - Kafka integration
- **Spring Data JPA** - Database ORM
- **Spring WebSocket** - Real-time communication
- **HikariCP** - Connection pooling
- **PostgreSQL Driver** - Database connectivity

### Infrastructure
- **Apache Kafka 7.7.0** - Message broker
- **ZooKeeper 7.7.0** - Kafka coordination
- **PostgreSQL 15** - Relational database
- **pgAdmin 4** - Database management UI
- **Kafka UI** - Kafka monitoring UI
- **Docker Compose** - Container orchestration

### Frontend
- **HTML5 / JavaScript**
- **SockJS** - WebSocket client library
- **STOMP.js** - WebSocket messaging protocol

---

## 📦 Prerequisites

Before running this application, ensure you have:

- **Java 17+** installed
- **Maven 3.6+** installed (or use included Maven wrapper)
- **Docker Desktop** installed and running
- **Git** (for cloning the repository)
- Minimum **4GB RAM** available for Docker containers
- Ports available: `8080`, `8081`, `5050`, `5432`, `9092`, `2181`

---

## 🚀 Installation & Setup

### 1. Clone the Repository
```bash
git clone <repository-url>
cd ApacheKafka
```

> **Note**: Configuration files (`docker-compose.yml`, `application.properties`) are pre-configured. Update credentials if needed.

### 2. Start Docker Containers
```bash
docker-compose up -d
```

This will start: ZooKeeper (2181), Kafka (9092), PostgreSQL (5432), pgAdmin (5050), and Kafka UI (8080).

### 3. Verify Containers are Running
```bash
docker ps
```

You should see 5 containers running: `zookeeper`, `kafka`, `postgres_container`, `pgadmin_container`, `kafka-ui`.

---

## ▶️ Running the Application

```bash
# Windows
.\mvnw.cmd spring-boot:run

# Linux/Mac
./mvnw spring-boot:run
```

Application runs on **http://localhost:8081**

---

## 🧪 Usage & Testing

### Access the User Interfaces

1. **Admin Notification Panel**: http://localhost:8081/send-notification.html
   - Send in-app notifications to students
   - Send email notifications
   - Monitor acknowledgments in real-time

2. **Student Notification Panel**: http://localhost:8081/student.html
   - Receive notifications
   - Send acknowledgments

3. **Kafka UI**: http://localhost:8080
   - Monitor Kafka topics
   - View message flow
   - Check consumer groups

4. **pgAdmin**: http://localhost:5050
   - Database management UI
   - Login credentials: Check `docker-compose.yml` environment section

### Testing Workflow

**In-App Notifications**: Open Admin Panel and Student Panel in separate tabs. Student joins with ID (e.g., `STU001`), Admin sends notification, Student receives and acknowledges in real-time.

**Email Notifications**: Send email from Admin Panel, verify in Kafka UI and application logs.

---

## 🔌 API Endpoints

**In-App**: 
- `POST /api/inapp/send?studentId={id}&message={msg}` - Send notification
- `GET /api/inapp/{studentId}` - Get notifications

**Email**: 
- `POST /notify/email?email={email}&message={msg}` - Send email

**WebSocket**: 
- Connect: `new SockJS('http://localhost:8081/ws')`
- Topics: `/topic/student/{studentId}`, `/topic/admin/acks`, `/app/ack`

---

## 🗄️ Database Schema

### Table: `notification_message`
| Column | Type | Description |
|--------|------|-------------|
| id | BIGSERIAL | Primary key |
| student_id | VARCHAR(255) | Student identifier |
| message | TEXT | Notification content |
| status | VARCHAR(50) | Delivery status |
| created_at | TIMESTAMP | Creation time |
| updated_at | TIMESTAMP | Last update time |

### Table: `failed_message`
| Column | Type | Description |
|--------|------|-------------|
| id | BIGSERIAL | Primary key |
| topic | VARCHAR(255) | Kafka topic name |
| message | TEXT | Failed message content |
| error | TEXT | Error details |
| retry_count | INTEGER | Number of retry attempts |
| created_at | TIMESTAMP | Failure timestamp |

---

## 🔧 pgAdmin Setup

1. Access http://localhost:5050 and login (credentials in `docker-compose.yml`)
2. Register new server: Host = `postgres` (or `localhost`), Port = `5432`
3. Navigate: Databases → `appdb` → Schemas → public → Tables

---

## 📊 Kafka Topics

The application uses the following Kafka topics:

| Topic | Description | Partitions | Producer | Consumer |
|-------|-------------|------------|----------|----------|
| `email_notifications` | Email notifications queue | 3 | NotificationProducer | NotificationConsumer |
| `inapp_notifications` | In-app notifications queue | 1 | InAppNotificationProducer | InAppNotificationConsumer |
| `ack_notifications` | Acknowledgment messages | 1 | InAppNotificationProducer | AcknowledgmentConsumer |

### View Topics in Kafka UI
1. Open http://localhost:8080
2. Click on "Topics"
3. View messages, partitions, and consumer groups

---

## ⚠️ Current Limitations

The current implementation has the following limitations that should be addressed in production:

1. **No Authentication/Authorization**
   - API endpoints are completely open
   - No role-based access control (RBAC)
   - Students can potentially access other students' notifications
   - Recommendation: Implement Spring Security with JWT

2. **Email Retry Strategy**
   - Email retries are handled using application-level exponential backoff (1m, 5m, 15m)
   - Messages are persisted to the `failed_message` table only after all retry attempts fail
   - A scheduler becomes active only when failed messages exist, primarily for monitoring or controlled reprocessing
   - Recommendation: Implement Kafka Dead Letter Queue (DLQ) with retry topics for high-volume scenarios

3. **No Rate Limiting**
   - API endpoints can be called infinitely
   - No protection against notification spam
   - No per-user or per-IP rate limits
   - Recommendation: Add Spring Cloud Gateway or custom rate-limiting filter

4. **WebSocket State Not Persistent**
   - Active connections lost on application restart
   - No session recovery mechanism
   - Students need to reconnect manually
   - Recommendation: Implement Redis-backed session store

5. **No Notification Templates**
   - Messages are sent as plain text
   - No variable substitution or formatting
   - Hard to maintain consistent message formats
   - Recommendation: Use Thymeleaf or FreeMarker templates

6. **Limited Monitoring**
   - No metrics collection (Prometheus)
   - No alerts on failures
   - Kafka UI shows topics but limited insights
   - Recommendation: Add Spring Boot Actuator + Prometheus + Grafana

These limitations don't affect the core async design but should be considered for production deployments.

---

## 🚀 Future Enhancements

- **Spring Security with JWT** - Add authentication and authorization
- **Push Notifications (FCM)** - Mobile push notification support
- **Monitoring Dashboard** - Prometheus + Grafana for metrics and alerts  
- **Notification Templates** - HTML email templates with variable substitution
- **Rate Limiting** - Prevent API abuse with per-user/IP throttling

---

## 🐛 Troubleshooting

**Containers**: `docker-compose down && docker-compose up -d`  
**Kafka**: `docker logs kafka` or `docker-compose restart kafka`  
**Database**: `docker logs postgres_container`  
**Port in use**: `netstat -ano | findstr :8081` then `taskkill /PID <id> /F`  
**WebSocket**: Ensure app runs on port 8081, check browser console

---

## 📁 Project Structure

```
ApacheKafka/
├── src/
│   ├── main/
│   │   ├── java/com/example/ApacheKafka/
│   │   │   ├── config/
│   │   │   │   ├── AsyncSchedulerConfig.java
│   │   │   │   ├── DatabaseConfig.java
│   │   │   │   ├── KafkaConfig.java
│   │   │   │   ├── KafkaErrorHandlerConfig.java
│   │   │   │   └── WebSocketConfig.java
│   │   │   ├── controller/
│   │   │   │   ├── AcknowledgmentController.java
│   │   │   │   ├── InAppNotificationController.java
│   │   │   │   └── NotificationController.java
│   │   │   ├── entity/
│   │   │   │   ├── FailedMessage.java
│   │   │   │   └── NotificationMessage.java
│   │   │   ├── repository/
│   │   │   │   ├── FailedMessageRepository.java
│   │   │   │   └── NotificationRepository.java
│   │   │   ├── service/
│   │   │   │   ├── EmailService.java
│   │   │   │   ├── InAppNotificationConsumer.java
│   │   │   │   ├── InAppNotificationProducer.java
│   │   │   │   ├── NotificationConsumer.java
│   │   │   │   ├── NotificationProducer.java
│   │   │   │   └── RetryService.java
│   │   │   ├── util/
│   │   │   │   └── ExcelReader.java
│   │   │   └── ApacheKafkaApplication.java
│   │   └── resources/
│   │       ├── static/
│   │       │   ├── send-notification.html
│   │       │   └── student.html
│   │       ├── db/migration/
│   │       │   └── V1__create_notification_message_table.sql
│   │       └── application.properties
│   └── test/
├── docker-compose.yml
├── pom.xml
├── README.md
├── TESTING_GUIDE.md
└── mvnw / mvnw.cmd
```

