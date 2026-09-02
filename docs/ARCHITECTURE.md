# EXAMIQ Architecture

## 1. Overview

EXAMIQ is a role-based academic intelligence platform built around previous question papers. It combines database search, OCR-driven ingestion, AI-based semantic retrieval, verification workflows, and role-based access for students, faculty, and admins.

## 2. System components

### Frontend

- React app using Vite
- Role-based navigation and dashboards
- Search, upload, analytics, and admin review UIs
- Tailwind styling and charting via Recharts

### Backend

- Spring Boot 3 REST API
- Spring Security + JWT
- JPA for persistence
- DTOs, services, repositories, and controller layers
- Role-based authorization for STUDENT, FACULTY, ADMIN

### AI Service

- Python FastAPI service
- OCR via Tesseract
- Image preprocessing via OpenCV
- PDF extraction via PDFBox wrapper or PyMuPDF fallback
- Embedding generation with sentence-transformers
- Semantic search with FAISS and cosine similarity
- Quality, duplicate, and verification analysis

### Data layer

- MySQL relational database
- Metadata stored in MySQL
- Files stored in local storage abstraction
- FAISS vector index kept in AI service not MySQL

## 3. Request flow

1. User authenticates via Spring Boot
2. React sends role-aware requests to the backend
3. Backend validates role and performs business logic
4. Paper uploads are stored in local storage and recorded in DB
5. AI service processes files for OCR, embeddings, duplicate checks, and quality scoring
6. Verification engine computes overall confidence and triggers approval or admin review
7. Search results merge DB filtering and semantic similarity

## 4. Core modules

- Authentication and authorization
- User management
- Paper catalog and smart search
- Upload and verification pipeline
- Faculty analytics and generator
- Admin approval and moderation
- Notification and reputation system

## 5. Security model

- Passwords hashed with BCrypt
- JWT for stateless authentication
- Role-based access control at controller and service layers
- HTTPS-ready configuration and CORS controls
- Global exception handling with consistent API error format

## 6. Deployment model

- Docker Compose orchestrates frontend, backend, AI service, and MySQL
- Configuration via environment variables
- Storage path is configurable to support future AWS S3 or Cloudinary integration

## 7. Notes on scope

This project implements the essential architecture and working flows described in the SRS in a realistic, reproducible way. The AI parts use real services and processing pipelines where possible, while preserving a clean and local development-ready structure.
