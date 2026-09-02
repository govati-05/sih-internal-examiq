# EXAMIQ API Design

## Authentication

- POST /api/auth/register
- POST /api/auth/login

## Student APIs

- GET /api/student/dashboard
- GET /api/papers/search
- GET /api/papers/{id}
- GET /api/papers/{id}/download
- POST /api/papers/upload
- GET /api/papers/my-uploads
- POST /api/papers/{id}/rate
- POST /api/papers/{id}/bookmark
- POST /api/papers/{id}/report

## Faculty APIs

- GET /api/faculty/dashboard
- GET /api/faculty/analytics
- POST /api/faculty/papers/upload
- GET /api/faculty/papers
- POST /api/faculty/question-generator

## Admin APIs

- GET /api/admin/dashboard
- GET /api/admin/pending
- GET /api/admin/flagged
- GET /api/admin/papers/{id}/verification
- POST /api/admin/papers/{id}/approve
- POST /api/admin/papers/{id}/reject
- POST /api/admin/papers/{id}/reupload
- GET /api/admin/users
- POST /api/admin/users/{id}/warn
- POST /api/admin/users/{id}/suspend
- POST /api/admin/users/{id}/ban
- POST /api/admin/faculty/{id}/verify

## AI Service APIs

- POST /ai/ocr
- POST /ai/segment
- POST /ai/embed
- POST /ai/search
- POST /ai/duplicate-check
- POST /ai/subject-check
- POST /ai/quality-check
- POST /ai/verify

## Response model

Every API response is structured consistently. Successful responses use:

{
  "success": true,
  "message": "Operation successful",
  "data": { }
}

Errors use:

{
  "success": false,
  "message": "Paper not found",
  "data": null
}

## Security rules

- STUDENT cannot access FACULTY or ADMIN endpoints
- FACULTY cannot access ADMIN endpoints
- ADMIN can access administrative functionality
- JWT is required for protected endpoints

## Validation

- File type enforcement
- File size restrictions
- Role validation
- Input validation via Bean Validation
- Global exception handling for 400, 401, 403, 404, 409, 422, and 500
