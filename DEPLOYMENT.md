# Deployment Guide

This document provides instructions for deploying the Social Media Content Moderation System to production environments.

## Table of Contents
1. [Pre-Deployment Checklist](#pre-deployment-checklist)
2. [Docker Deployment](#docker-deployment)
3. [Kubernetes Deployment](#kubernetes-deployment)
4. [AWS Deployment](#aws-deployment)
5. [Environment Configuration](#environment-configuration)
6. [Database Migration](#database-migration)
7. [Monitoring & Logging](#monitoring--logging)
8. [Troubleshooting](#troubleshooting)

---

## Pre-Deployment Checklist

### Security
- [ ] Change JWT secret to strong random value (min 256 bits)
  ```bash
  openssl rand -base64 32
  ```
- [ ] Update database credentials
- [ ] Enable HTTPS/SSL
- [ ] Configure CORS for production domains only
- [ ] Review and update security headers
- [ ] Run security vulnerability scan
  ```bash
  mvn clean verify -Psecurity
  ```

### Code Quality
- [ ] All tests passing
  ```bash
  mvn clean test
  ```
- [ ] Code coverage > 80% for critical paths
- [ ] No console.log or debug statements
- [ ] All warnings resolved
- [ ] Code review completed
- [ ] No hardcoded secrets/credentials

### Build & Packaging
- [ ] Build artifact created
  ```bash
  mvn clean package -P production
  ```
- [ ] Docker image built and tested
- [ ] Image pushed to container registry
- [ ] Version tags applied

### Infrastructure
- [ ] Production database set up
- [ ] Database backups configured
- [ ] Monitoring tools configured
- [ ] Logging infrastructure ready
- [ ] CDN configured (if applicable)
- [ ] Load balancer configured

### Documentation
- [ ] API documentation updated
- [ ] Environment variables documented
- [ ] Runbook created for operations team
- [ ] Incident response plan documented
- [ ] Rollback procedure documented

---

## Docker Deployment

### Build Docker Image

**Create Dockerfile (if not exists):**
```dockerfile
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy JAR file
COPY target/socialapp-1.0.0.jar app.jar

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:8080/api/v1/auth/health || exit 1

# Run application
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Build image:**
```bash
# Build
mvn clean package -DskipTests
docker build -t socialapp:1.0.0 .

# Tag for registry
docker tag socialapp:1.0.0 your-registry/socialapp:1.0.0

# Push to registry
docker push your-registry/socialapp:1.0.0
```

### Run Container

```bash
# Run with environment variables
docker run -d \
  --name socialapp \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e JWT_SECRET=$(openssl rand -base64 32) \
  -e DATABASE_URL=jdbc:postgresql://db-host:5432/socialapp \
  -e DATABASE_USER=socialapp_user \
  -e DATABASE_PASSWORD=secure_password \
  -v /var/log/socialapp:/app/logs \
  your-registry/socialapp:1.0.0

# Check logs
docker logs -f socialapp

# Stop container
docker stop socialapp
docker rm socialapp
```

### Docker Compose

**Create docker-compose.yml:**
```yaml
version: '3.8'

services:
  app:
    image: your-registry/socialapp:1.0.0
    ports:
      - "8080:8080"
    environment:
      SPRING_PROFILES_ACTIVE: prod
      JWT_SECRET: ${JWT_SECRET}
      DATABASE_URL: jdbc:postgresql://postgres:5432/socialapp
      DATABASE_USER: ${DB_USER}
      DATABASE_PASSWORD: ${DB_PASSWORD}
    depends_on:
      - postgres
    networks:
      - app-network

  postgres:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: socialapp
      POSTGRES_USER: ${DB_USER}
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
    networks:
      - app-network

volumes:
  postgres_data:

networks:
  app-network:
    driver: bridge
```

**Deploy:**
```bash
# Create .env file with secrets
echo "JWT_SECRET=$(openssl rand -base64 32)" > .env
echo "DB_USER=socialapp" >> .env
echo "DB_PASSWORD=$(openssl rand -base64 16)" >> .env

# Deploy
docker-compose up -d

# View logs
docker-compose logs -f app
```

---

## Kubernetes Deployment

### Create Kubernetes Manifests

**1. Namespace:**
```yaml
# namespace.yaml
apiVersion: v1
kind: Namespace
metadata:
  name: socialapp
```

**2. ConfigMap:**
```yaml
# configmap.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: socialapp-config
  namespace: socialapp
data:
  SPRING_PROFILES_ACTIVE: production
  LOG_LEVEL: INFO
  CORS_ALLOWED_ORIGINS: https://app.example.com,https://admin.example.com
```

**3. Secret:**
```yaml
# secret.yaml
apiVersion: v1
kind: Secret
metadata:
  name: socialapp-secret
  namespace: socialapp
type: Opaque
stringData:
  JWT_SECRET: your-secure-jwt-secret-here
  DATABASE_URL: jdbc:postgresql://postgres-service:5432/socialapp
  DATABASE_USER: socialapp
  DATABASE_PASSWORD: secure-db-password
```

**4. Deployment:**
```yaml
# deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: socialapp
  namespace: socialapp
spec:
  replicas: 3
  selector:
    matchLabels:
      app: socialapp
  template:
    metadata:
      labels:
        app: socialapp
    spec:
      containers:
      - name: socialapp
        image: your-registry/socialapp:1.0.0
        imagePullPolicy: Always
        ports:
        - containerPort: 8080
          name: http
        env:
        - name: SPRING_PROFILES_ACTIVE
          valueFrom:
            configMapKeyRef:
              name: socialapp-config
              key: SPRING_PROFILES_ACTIVE
        - name: JWT_SECRET
          valueFrom:
            secretKeyRef:
              name: socialapp-secret
              key: JWT_SECRET
        - name: DATABASE_URL
          valueFrom:
            secretKeyRef:
              name: socialapp-secret
              key: DATABASE_URL
        - name: DATABASE_USER
          valueFrom:
            secretKeyRef:
              name: socialapp-secret
              key: DATABASE_USER
        - name: DATABASE_PASSWORD
          valueFrom:
            secretKeyRef:
              name: socialapp-secret
              key: DATABASE_PASSWORD
        
        livenessProbe:
          httpGet:
            path: /api/v1/auth/health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
          timeoutSeconds: 5
          failureThreshold: 3
        
        readinessProbe:
          httpGet:
            path: /api/v1/auth/health
            port: 8080
          initialDelaySeconds: 10
          periodSeconds: 5
          timeoutSeconds: 3
          failureThreshold: 3
        
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "500m"
```

**5. Service:**
```yaml
# service.yaml
apiVersion: v1
kind: Service
metadata:
  name: socialapp-service
  namespace: socialapp
spec:
  selector:
    app: socialapp
  ports:
  - port: 80
    targetPort: 8080
    protocol: TCP
  type: LoadBalancer
```

**6. Ingress:**
```yaml
# ingress.yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: socialapp-ingress
  namespace: socialapp
  annotations:
    cert-manager.io/cluster-issuer: letsencrypt-prod
    nginx.ingress.kubernetes.io/rate-limit: "100"
spec:
  tls:
  - hosts:
    - api.example.com
    secretName: socialapp-tls
  rules:
  - host: api.example.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: socialapp-service
            port:
              number: 80
```

### Deploy to Kubernetes

```bash
# Create namespace
kubectl create -f namespace.yaml

# Create secrets and configs
kubectl create -f secret.yaml
kubectl create -f configmap.yaml

# Deploy application
kubectl create -f deployment.yaml
kubectl create -f service.yaml
kubectl create -f ingress.yaml

# Check status
kubectl get all -n socialapp
kubectl logs -f -n socialapp deployment/socialapp

# View service
kubectl get svc -n socialapp
```

### Scale Application

```bash
# Scale replicas
kubectl scale deployment socialapp -n socialapp --replicas 5

# Monitor replicas
kubectl get deployment socialapp -n socialapp -w
```

---

## AWS Deployment

### ECS (Elastic Container Service)

**1. Create ECR Repository:**
```bash
aws ecr create-repository --repository-name socialapp --region us-east-1
```

**2. Push Docker Image:**
```bash
# Get login token
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin YOUR_ACCOUNT_ID.dkr.ecr.us-east-1.amazonaws.com

# Tag and push
docker tag socialapp:1.0.0 YOUR_ACCOUNT_ID.dkr.ecr.us-east-1.amazonaws.com/socialapp:1.0.0
docker push YOUR_ACCOUNT_ID.dkr.ecr.us-east-1.amazonaws.com/socialapp:1.0.0
```

**3. Create ECS Task Definition (task-definition.json):**
```json
{
  "family": "socialapp",
  "networkMode": "awsvpc",
  "requiresCompatibilities": ["FARGATE"],
  "cpu": "256",
  "memory": "512",
  "containerDefinitions": [
    {
      "name": "socialapp",
      "image": "YOUR_ACCOUNT_ID.dkr.ecr.us-east-1.amazonaws.com/socialapp:1.0.0",
      "portMappings": [
        {
          "containerPort": 8080,
          "hostPort": 8080
        }
      ],
      "environment": [
        {
          "name": "SPRING_PROFILES_ACTIVE",
          "value": "prod"
        }
      ],
      "secrets": [
        {
          "name": "JWT_SECRET",
          "valueFrom": "arn:aws:secretsmanager:us-east-1:ACCOUNT_ID:secret:socialapp/jwt-secret"
        },
        {
          "name": "DATABASE_URL",
          "valueFrom": "arn:aws:secretsmanager:us-east-1:ACCOUNT_ID:secret:socialapp/db-url"
        }
      ],
      "logConfiguration": {
        "logDriver": "awslogs",
        "options": {
          "awslogs-group": "/ecs/socialapp",
          "awslogs-region": "us-east-1",
          "awslogs-stream-prefix": "ecs"
        }
      }
    }
  ]
}
```

**4. Register Task Definition:**
```bash
aws ecs register-task-definition --cli-input-json file://task-definition.json
```

**5. Create ECS Service:**
```bash
aws ecs create-service \
  --cluster socialapp-cluster \
  --service-name socialapp-service \
  --task-definition socialapp:1 \
  --desired-count 3 \
  --launch-type FARGATE \
  --network-configuration "awsvpcConfiguration={subnets=[subnet-xxx,subnet-yyy],securityGroups=[sg-xxx],assignPublicIp=ENABLED}"
```

---

## Environment Configuration

### Production Environment Variables

```bash
# Required
export SPRING_PROFILES_ACTIVE=prod
export JWT_SECRET=your-secure-secret-here
export DATABASE_URL=jdbc:postgresql://your-host:5432/socialapp
export DATABASE_USER=socialapp_user
export DATABASE_PASSWORD=secure_password

# Optional
export SERVER_PORT=8080
export CORS_ALLOWED_ORIGINS=https://app.example.com
export LOG_LEVEL=INFO
export ENABLE_HTTPS=true
```

### AWS Secrets Manager

```bash
# Store secrets
aws secretsmanager create-secret \
  --name socialapp/jwt-secret \
  --secret-string $(openssl rand -base64 32)

aws secretsmanager create-secret \
  --name socialapp/db-password \
  --secret-string "your-secure-password"

# Retrieve secrets
aws secretsmanager get-secret-value --secret-id socialapp/jwt-secret
```

---

## Database Migration

### PostgreSQL Setup

```bash
# Create database
createdb -U postgres socialapp

# Create user
psql -U postgres -d socialapp \
  -c "CREATE USER socialapp_user WITH PASSWORD 'secure_password';"

# Grant privileges
psql -U postgres -d socialapp \
  -c "GRANT ALL PRIVILEGES ON DATABASE socialapp TO socialapp_user;"

psql -U postgres -d socialapp \
  -c "ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO socialapp_user;"
```

### Automatic Schema Creation

The application creates tables automatically with `hibernate.ddl-auto: update` in development, but for production:

```yaml
# application-prod.yml
spring:
  jpa:
    hibernate:
      ddl-auto: validate  # Only validate, don't create
```

Manually create schema using migration tools like Flyway or Liquibase (future implementation).

---

## Monitoring & Logging

### CloudWatch Logs (AWS)

```bash
# Create log group
aws logs create-log-group --log-group-name /ecs/socialapp

# View logs
aws logs tail /ecs/socialapp --follow
```

### Application Logging

```yaml
# application-prod.yml
logging:
  file:
    name: /var/log/socialapp/app.log
    max-size: 10MB
    max-history: 30
  level:
    root: WARN
    com.example.socialapp: INFO
```

### Monitoring Endpoints

Add Spring Boot Actuator for monitoring:

```xml
<!-- pom.xml -->
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

```yaml
# application-prod.yml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics
  endpoint:
    health:
      show-details: when-authorized
```

Access metrics: `http://localhost:8080/api/actuator/health`

---

## Troubleshooting

### Common Issues

**1. Database Connection Timeout:**
```bash
# Check connection string
# Verify database is running and accessible
# Check firewall/security groups
nc -zv database-host 5432
```

**2. JWT Token Validation Failed:**
- Verify JWT secret is consistent across instances
- Check token expiration time configuration
- Ensure server time is synchronized

**3. Container Out of Memory:**
```bash
# Increase memory limits
# Docker: set -m flag
# Kubernetes: update resources.limits.memory
```

**4. Slow Response Times:**
- Enable database query logging
- Check database indexes
- Monitor application metrics
- Review CPU/memory usage

### Debug Mode

```bash
# Run with debug logging
LOGGING_LEVEL_COM_EXAMPLE_SOCIALAPP=DEBUG \
java -jar socialapp-1.0.0.jar

# Remote debugging
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005 \
  -jar socialapp-1.0.0.jar
```

### Health Checks

```bash
# Check application health
curl http://localhost:8080/api/v1/auth/health

# Check with Kubernetes
kubectl get pods -n socialapp
kubectl logs -n socialapp deployment/socialapp
```

---

## Rollback Procedure

### Docker Container

```bash
# Stop new version
docker stop socialapp
docker rm socialapp

# Start previous version
docker run -d \
  --name socialapp \
  your-registry/socialapp:1.0.0-previous \
  ... (same parameters)
```

### Kubernetes

```bash
# Get rollout history
kubectl rollout history deployment/socialapp -n socialapp

# Rollback to previous version
kubectl rollout undo deployment/socialapp -n socialapp

# Rollback to specific revision
kubectl rollout undo deployment/socialapp -n socialapp --to-revision=2
```

### ECS

```bash
# Update service with previous task definition
aws ecs update-service \
  --cluster socialapp-cluster \
  --service socialapp-service \
  --task-definition socialapp:1
```

---

## Performance Optimization

### Application Level

```yaml
# application-prod.yml
spring:
  jpa:
    properties:
      hibernate:
        jdbc:
          batch_size: 30
        order_inserts: true
        order_updates: true
```

### Database Level

```sql
-- Create indexes for frequently queried columns
CREATE INDEX idx_user_email ON users(email);
CREATE INDEX idx_user_enabled ON users(enabled);
CREATE INDEX idx_user_created_at ON users(created_at);

-- Analyze query plans
EXPLAIN ANALYZE SELECT * FROM users WHERE email = 'test@example.com';
```

### Caching (Future)

```yaml
spring:
  cache:
    type: redis
    redis:
      host: redis-server
      port: 6379
```

---

## Backups & Recovery

### Database Backups

```bash
# PostgreSQL backup
pg_dump -U socialapp_user -h localhost socialapp > backup.sql

# Automated backup (cron)
0 2 * * * pg_dump -U socialapp_user -h localhost socialapp | gzip > /backups/socialapp-$(date +\%Y\%m\%d).sql.gz

# Restore from backup
psql -U socialapp_user -h localhost socialapp < backup.sql
```

---

**Last Updated:** April 9, 2024
