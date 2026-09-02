# EXAMIQ – Running the Application Locally

This guide provides step-by-step instructions to run EXAMIQ locally on your Windows machine.

## Prerequisites

- Java 21+ (currently using JDK 25)
- MySQL 8.0
- Node.js 18+ with npm
- Python 3.10+ (for AI service)
- Docker (optional, for containerized setup)

## Configuration

### Step 1: Configure Environment Variables

Copy `.env.example` to `.env` and update with your MySQL credentials:

```bash
# Database
DATABASE_URL=jdbc:mysql://localhost:3306/examiq
DATABASE_USERNAME=root
DATABASE_PASSWORD=root
MYSQL_DATABASE=examiq
MYSQL_ROOT_PASSWORD=root
MYSQL_USER=root
MYSQL_PASSWORD=root

# JWT
JWT_SECRET=your-super-secret-key-change-this-in-production

# AI Service
AI_SERVICE_URL=http://localhost:8001
AI_SERVICE_PORT=8001

# Storage
FILE_STORAGE_PATH=./storage

# Ports
BACKEND_PORT=8080
FRONTEND_PORT=3000
```

### Step 2: Set Up MySQL Database

1. Start MySQL Server (use MySQL Workbench or Command Line):
   ```bash
   mysql -u root -p
   ```

2. Create the database and tables:
   ```bash
   mysql -u root -p < db/schema.sql
   ```

3. Verify the database:
   ```sql
   USE examiq;
   SHOW TABLES;
   SELECT * FROM roles;
   SELECT * FROM subjects;
   ```

## Starting the Application

### Option A: Local Terminal (Recommended for Development)

#### Terminal 1: Start MySQL

```bash
# Windows (if MySQL is in PATH)
mysqld

# Or use MySQL Workbench or Services
```

#### Terminal 2: Start Backend (Port 8080)

```bash
cd backend
mvn spring-boot:run
```

Backend will start at: `http://localhost:8080`

#### Terminal 3: Start AI Service (Port 8001)

```bash
cd ai-service
pip install -r requirements.txt
python main.py
```

AI Service will start at: `http://localhost:8001`

#### Terminal 4: Start Frontend (Port 3000)

```bash
cd frontend
npm install
npm run dev
```

Frontend will start at: `http://localhost:3000`

### Option B: Docker Compose (Fastest)

```bash
docker-compose up --build
```

This starts all services:
- MySQL: `localhost:3306`
- Backend: `localhost:8080`
- AI Service: `localhost:8001`
- Frontend: `localhost:3000`

## Testing the Application

### 1. Register a New User

**Endpoint:** `POST /api/auth/register`

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "student01",
    "email": "student@example.com",
    "password": "Password123",
    "fullName": "John Student",
    "role": "STUDENT"
  }'
```

### 2. Login

**Endpoint:** `POST /api/auth/login`

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "student01",
    "password": "Password123"
  }'
```

**Response:**
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "token": "eyJhbGc...",
    "username": "student01",
    "role": "STUDENT"
  }
}
```

### 3. Access Dashboard

Open browser: `http://localhost:3000`

- Login with your credentials
- You'll be redirected to your role-specific dashboard (Student/Faculty/Admin)

### 4. Search Papers

**Endpoint:** `GET /api/papers/search`

```bash
curl -X GET "http://localhost:8080/api/papers/search" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### 5. Test AI Service

```bash
curl -X POST http://localhost:8001/ai/verify \
  -H "Content-Type: application/json" \
  -d '{
    "paper_id": 1,
    "upload_id": 1,
    "file_path": "/storage/paper.pdf"
  }'
```

## Troubleshooting

### Port Already in Use

```bash
# Find process using port 8080
netstat -ano | findstr :8080

# Kill process (replace PID)
taskkill /PID <PID> /F
```

### MySQL Connection Failed

1. Verify MySQL is running:
   ```bash
   mysql -u root -p
   ```

2. Check credentials in `application.yml`:
   ```yaml
   spring.datasource.url: jdbc:mysql://localhost:3306/examiq
   spring.datasource.username: root
   spring.datasource.password: root
   ```

3. Verify database exists:
   ```sql
   SHOW DATABASES;
   ```

### Frontend Can't Connect to Backend

1. Check backend is running: `curl http://localhost:8080/actuator/health`
2. Verify CORS settings in `SecurityConfig.java`
3. Check browser console for CORS errors

### AI Service Not Responding

1. Verify Python and dependencies:
   ```bash
   python --version
   pip list | grep fastapi
   ```

2. Check port 8001:
   ```bash
   curl http://localhost:8001/health
   ```

3. Check logs in terminal

## Database Schema Overview

- **users** – User accounts with roles
- **roles** – STUDENT, FACULTY, ADMIN
- **papers** – Paper metadata
- **uploads** – File upload tracking
- **questions** – Extracted questions
- **subjects** – Subject/topic information
- **bookmarks** – User bookmarks
- **ratings** – Paper ratings
- **notifications** – User notifications
- **verification_logs** – AI verification pipeline logs

## Default Test Credentials

After running `db/schema.sql`, the database is seeded with roles. Create users via the registration endpoint.

## Development Tips

- **Hot Reload Frontend:** Changes to React files auto-reload
- **Backend Restart:** Stop Maven, make changes, restart with `mvn spring-boot:run`
- **AI Service Debugging:** Add print statements to `main.py` or use `python -m pdb`
- **Database Reset:** Run `db/schema.sql` again to clear all data

## API Documentation

- **Swagger UI** (when implemented): `http://localhost:8080/swagger-ui.html`
- **API Design**: See `docs/API-DESIGN.md`
- **Architecture**: See `docs/ARCHITECTURE.md`

## Next Steps

1. **Implement Upload:** Add file upload endpoint in backend
2. **Integrate OCR:** Connect AI service for real Tesseract processing
3. **Add Search:** Implement semantic search with FAISS
4. **Dashboard Features:** Build out analytics and reporting
5. **Tests:** Add unit and integration tests

## Production Deployment

For production:
1. Change JWT secret in `.env`
2. Use HTTPS/SSL
3. Deploy to cloud (AWS, Google Cloud, Azure)
4. Use managed MySQL (RDS, Cloud SQL)
5. Configure proper CORS origins
6. Add API rate limiting
7. Implement logging and monitoring

## Support

For issues or questions:
1. Check the browser console for errors
2. Check the terminal output for backend/AI logs
3. Review `docs/ARCHITECTURE.md` for system design
4. Check `docs/API-DESIGN.md` for endpoint contracts
