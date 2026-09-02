# EXAMIQ Database ER Design

## Entity groups

### Core identity

- Users
- Roles
- Universities

### Academic catalog

- Subjects
- SubjectAliases
- Papers
- Questions
- TopicMappings

### Interaction and moderation

- Uploads
- Ratings
- Bookmarks
- Reports
- Notifications
- VerificationLogs
- ContributorScores
- AdminActions
- FacultyVerification

## Key relationships

- Users -> Roles (many-to-one)
- Users -> Universities (many-to-one)
- Universities -> Papers (one-to-many)
- Subjects -> Papers (one-to-many)
- Subjects -> SubjectAliases (one-to-many)
- Papers -> Uploads (one-to-many)
- Papers -> Questions (one-to-many)
- Questions -> TopicMappings (one-to-many)
- Papers -> Ratings (one-to-many)
- Users <-> Papers via Bookmarks (many-to-many)
- Papers -> Reports (one-to-many)
- Users -> Notifications (one-to-many)
- Papers -> VerificationLogs (one-to-many)
- Users -> ContributorScores (one-to-one or one-to-many)
- Admin -> AdminActions (one-to-many)
- Faculty -> FacultyVerification (one-to-many)

## Core tables

### Users

- id
- username
- email
- password
- full_name
- role_id
- university_id
- status
- created_at
- updated_at

### Roles

- id
- name

### Universities

- id
- name
- code
- country

### Subjects

- id
- name
- canonical_name

### SubjectAliases

- id
- subject_id
- alias

### Papers

- id
- title
- subject_id
- university_id
- uploader_id
- year
- exam_type
- author
- status
- file_url
- metadata_json
- created_at

### Uploads

- id
- paper_id
- uploaded_by
- original_file_name
- stored_path
- mime_type
- file_size
- upload_status
- created_at

### Questions

- id
- paper_id
- question_text
- topic_id
- marks
- difficulty_level

### TopicMappings

- id
- question_id
- topic_name
- confidence

### Ratings

- id
- paper_id
- user_id
- score
- comment

### Bookmarks

- id
- user_id
- paper_id

### Reports

- id
- paper_id
- user_id
- report_type
- description
- status

### Notifications

- id
- user_id
- title
- message
- type
- is_read
- created_at

### VerificationLogs

- id
- paper_id
- upload_id
- stage
- score
- details_json
- created_at

### ContributorScores

- id
- user_id
- score
- tier
- updated_at

### AdminActions

- id
- admin_id
- paper_id
- action_type
- reason
- created_at

### FacultyVerification

- id
- faculty_id
- university_id
- verification_status
- documents_url
- reviewed_by
- reviewed_at

## Notes

The database is normalized and intentionally separated from file storage. OCR text and vector metadata are handled in the AI service layer, while MySQL keeps relational metadata and system state.
