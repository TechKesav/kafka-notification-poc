# Apache Kafka Notification System 📨

A real-time notification system built with **Spring Boot**, **Apache Kafka**, **PostgreSQL**, and **WebSockets** that supports both in-app and email notifications with real-time acknowledgments.

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

### 2. Configure Environment Variables
Create a `.env` file in the project root (copy from `.env.example`):
```bash
cp .env.example .env
```

Edit `.env` with your configuration:
```properties
# Database Configuration
DB_USER=kesav
DB_PASSWORD=kesav
DB_NAME=appdb
DB_PORT=5432

# pgAdmin Configuration
PGADMIN_EMAIL=pgadmin4@pgadmin.org
PGADMIN_PASSWORD=admin
PGADMIN_PORT=5050

# Email Configuration (Optional)
SPRING_MAIL_USERNAME=your-email@gmail.com
SPRING_MAIL_PASSWORD=your-app-password
```

### 3. Configure Email Settings (Optional)
If you want to send actual emails, update the email variables in `.env`:
```properties
SPRING_MAIL_USERNAME=your-email@gmail.com
SPRING_MAIL_PASSWORD=your-app-password
```

**Note**: For Gmail, you need to create an [App Password](https://support.google.com/accounts/answer/185833).

### 4. Start Docker Containers
```bash
docker-compose up -d
```

This will start:
- ZooKeeper (port 2181)
- Kafka (port 9092)
- PostgreSQL (port configured in .env)
- pgAdmin (port configured in .env)
- Kafka UI (port 8080)

### 5. Verify Containers are Running
```bash
docker ps
```

You should see 5 containers running: `zookeeper`, `kafka`, `postgres_container`, `pgadmin_container`, `kafka-ui`.

---

## ▶️ Running the Application

### Using Maven Wrapper (Recommended)

#### Windows
```cmd
.\mvnw.cmd clean spring-boot:run
```

#### Linux/Mac
```bash
./mvnw clean spring-boot:run
```

### Using Installed Maven
```bash
mvn clean spring-boot:run
```

The application will start on **http://localhost:8081**

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
   - Database management UI (credentials configured in docker-compose.yml)

### Testing Workflow

#### Test In-App Notifications
1. Open Admin Panel in one browser tab
2. Open Student Panel in another tab/window
3. In Student Panel, enter Student ID (e.g., `STU001`) and click "Join"
4. In Admin Panel, send notification to `STU001`
5. See the notification appear in real-time on Student Panel
6. Send acknowledgment from Student Panel
7. See acknowledgment appear on Admin Panel

#### Test Email Notifications
1. Open Admin Panel
2. In the "Send Email Notification" section:
   - Enter recipient email: `test@example.com`
   - Enter message: `Test email notification`
   - Click "Send Email"
3. Check Kafka UI to verify message was sent to `email_notifications` topic
4. Check application logs to see consumer processing

---

## 🔌 API Endpoints

### In-App Notifications

#### Send Notification to Student
```http
POST /api/inapp/send?studentId={studentId}&message={message}
```

**Example**:
```bash
curl -X POST "http://localhost:8081/api/inapp/send?studentId=STU001&message=Your%20assignment%20is%20ready"
```

**Response**: `Notification published to Kafka`

#### Get Notifications for Student
```http
GET /api/inapp/{studentId}
```

**Example**:
```bash
curl "http://localhost:8081/api/inapp/STU001"
```

**Response**:
```json
[
  {
    "id": 1,
    "studentId": "STU001",
    "message": "Your assignment is ready",
    "status": "DELIVERED",
    "createdAt": "2026-01-15T10:30:00",
    "updatedAt": "2026-01-15T10:30:00"
  }
]
```

### Email Notifications

#### Send Email Notification
```http
POST /notify/email?email={email}&message={message}
```

**Example**:
```bash
curl -X POST "http://localhost:8081/notify/email?email=student@example.com&message=Important%20Update"
```

**Response**: `Email notification sent to student@example.com`

### WebSocket Endpoints

#### Connect to WebSocket
```javascript
const socket = new SockJS('http://localhost:8081/ws');
const stompClient = Stomp.over(socket);
```

#### Topics
- `/topic/student/{studentId}` - Receive notifications for specific student
- `/topic/admin/acks` - Receive acknowledgments (admin)
- `/app/ack` - Send acknowledgment (student)

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

### Connect to PostgreSQL Database

1. **Open pgAdmin**: http://localhost:5050
2. **Login**: Use credentials configured in `docker-compose.yml`

3. **Add Server**:
   - Right-click **Servers** → **Register** → **Server**
   
4. **General Tab**:
   - Name: `ApacheKafka PostgreSQL` (or any name)

5. **Connection Tab**:
   - Host name/address: `postgres` (Docker) or `localhost` (host machine)
   - Port: `5432`
   - Maintenance database: `postgres`
   - Username/Password: Check `docker-compose.yml`

6. **Click Save**

7. **Navigate to Database**:
   - Expand server → Databases → `appdb` → Schemas → public → Tables

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

Planned improvements to make this system production-ready:

### Short Term
- [ ] Kafka Dead Letter Queue (DLQ) for retry topics
- [ ] Spring Security with JWT authentication
- [ ] API rate limiting per user/IP
- [ ] Notification templates with variable substitution
- [ ] User roles and permissions (Admin, Student, Instructor)

### Medium Term
- [ ] Push notifications via Firebase Cloud Messaging (FCM)
- [ ] SMS notifications via Twilio
- [ ] Slack/Teams integration
- [ ] Email HTML templates with styling
- [ ] Notification preferences (users opt-in/out)

### Long Term
- [ ] Analytics dashboard (delivery rates, response times)
- [ ] Metrics collection (Prometheus)
- [ ] Monitoring and alerting (Grafana)
- [ ] Load testing and performance tuning
- [ ] Distributed tracing (Jaeger/Zipkin)
- [ ] Multi-tenant support
- [ ] Notification scheduling (send at specific time)

---

## 🐛 Troubleshooting

### Application Won't Start

**Issue**: Timezone error `FATAL: invalid value for parameter "TimeZone": "Asia/Calcutta"`

**Solution**: Already fixed with `DatabaseConfig.java` that sets timezone to UTC. If issue persists:
```bash
# Restart PostgreSQL container
docker restart postgres_container
```

### Kafka Connection Failed

**Solution**:
```bash
# Check Kafka is running
docker logs kafka

# Restart Kafka
docker-compose restart kafka
```

### Database Connection Failed

**Solution**:
```bash
# Check PostgreSQL logs
docker logs postgres_container

# Verify credentials in docker-compose.yml
# Database credentials configured in environment section
```

### Port Already in Use

**Solution**:
```bash
# Stop conflicting services or change ports in docker-compose.yml
# Find process using port (Windows)
netstat -ano | findstr :8081

# Kill process
taskkill /PID <process_id> /F
```

### Docker Containers Not Starting

**Solution**:
```bash
# Clean up and restart
docker-compose down
docker system prune -f
docker-compose up -d
```

### WebSocket Connection Failed

**Solution**:
- Ensure application is running on port 8081
- Check browser console for errors
- Verify CORS settings if accessing from different domain

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

---

## 📝 Configuration Files

### .env (Environment Variables)
Create a `.env` file in the project root with:
```properties
# Database credentials
DB_USER=kesav
DB_PASSWORD=kesav
DB_NAME=appdb
DB_PORT=5432

# pgAdmin credentials
PGADMIN_EMAIL=pgadmin4@pgadmin.org
PGADMIN_PASSWORD=admin
PGADMIN_PORT=5050

# Email settings (optional)
SPRING_MAIL_USERNAME=your-email@gmail.com
SPRING_MAIL_PASSWORD=your-app-password
SPRING_MAIL_HOST=smtp.gmail.com
SPRING_MAIL_PORT=587
```

**Tip**: Use `.env.example` as a template - copy and customize for your environment.

### application.properties
Key configurations:
- Server port: `8081`
- Kafka bootstrap servers: `localhost:9092`
- Database URL: Derived from .env variables
- Mail settings: Derived from .env variables

### docker-compose.yml
Services configured with environment variables from `.env`:
- ZooKeeper
- Kafka
- PostgreSQL (with DB credentials from .env)
- pgAdmin (with credentials from .env)
- Kafka UI

---

## 🔐 Security Configuration

| Component | Configuration Method | Location |
|-----------|----------------------|----------|
| PostgreSQL | Environment Variables | `.env` file |
| pgAdmin | Environment Variables | `.env` file |
| Email SMTP | Environment Variables | `.env` file |
| Application | application.properties | `src/main/resources/` |

### Environment Variable Security

1. **Create `.env` from template**:
   ```bash
   cp .env.example .env
   ```

2. **Update `.env` with your values** (never commit to git):
   ```bash
   # Add to .gitignore
   echo ".env" >> .gitignore
   ```

3. **Use `.env.example` for documentation**:
   - Keep `.env.example` in git
   - Shows required variables without sensitive values
   - Developers copy and customize locally

**Note**: For production deployments, use proper secrets management systems (AWS Secrets Manager, Kubernetes Secrets, Vault, etc.)

---

## 📞 Support

For issues or questions:
1. Check the [Troubleshooting](#troubleshooting) section
2. Review [TESTING_GUIDE.md](TESTING_GUIDE.md)
3. Check application logs
4. Verify Docker containers are running

---

**Happy Coding! 🚀**
