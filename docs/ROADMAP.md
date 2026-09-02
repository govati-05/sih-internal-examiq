# EXAMIQ Development Roadmap

## Phase 1 – Project setup

- Create monorepo structure
- Add environment configuration
- Add Docker and CI-friendly conventions

## Phase 2 – MySQL + JPA

- Add entity model and repositories
- Configure datasource and flyway-friendly schema

## Phase 3 – Authentication

- User registration and login
- JWT issuance and validation
- Role-based authorization

## Phase 4 – Student portal

- Dashboard, smart search, subject pages, paper details, bookmarks

## Phase 5 – Faculty portal

- Dashboard, upload flows, faculty analytics, question generation

## Phase 6 – Admin portal

- Review queues, moderation, user management, analytics

## Phase 7 – Upload and storage

- File validation and local storage abstraction

## Phase 8 – Python AI service

- FastAPI service skeleton and endpoints for OCR and verification workflow

## Phase 9 – OCR

- PDF extraction and image preprocessing for Tesseract

## Phase 10 – Segmentation

- Question detection and marking extraction

## Phase 11 – Semantic search

- Embeddings and FAISS-based retrieval

## Phase 12 – Duplicate detection

- Hash, similarity, and semantic duplicate detection

## Phase 13 – Subject verification

- Alias mapping and canonical subject checks

## Phase 14 – Quality validation

- OCR confidence and readability checks

## Phase 15 – AI verification pipeline

- Confidence formula and admin review logic

## Phase 16 – Faculty analytics

- Topic trends, repeated question analysis, difficulty distribution

## Phase 17 – AI question paper generator

- Balanced generation based on existing question bank

## Phase 18 – Notifications + reputation

- Event-driven notifications and contributor scoring

## Phase 19 – Testing

- Unit, integration, and API tests

## Phase 20 – Docker deployment

- Compose orchestration and environment-driven startup
