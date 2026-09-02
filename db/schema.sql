CREATE TABLE IF NOT EXISTS roles (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(50) NOT NULL UNIQUE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS universities (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(255) NOT NULL,
  code VARCHAR(50),
  country VARCHAR(100),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(100) NOT NULL UNIQUE,
  email VARCHAR(150) NOT NULL UNIQUE,
  password VARCHAR(255) NOT NULL,
  full_name VARCHAR(150) NOT NULL,
  role_id BIGINT NOT NULL,
  university_id BIGINT,
  status VARCHAR(30) DEFAULT 'ACTIVE',
  profile_picture_url VARCHAR(500),
  branch VARCHAR(150),
  academic_year INT,
  section VARCHAR(30),
  bio TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_users_role FOREIGN KEY (role_id) REFERENCES roles(id),
  CONSTRAINT fk_users_university FOREIGN KEY (university_id) REFERENCES universities(id)
);

CREATE TABLE IF NOT EXISTS subjects (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(255) NOT NULL,
  canonical_name VARCHAR(255),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS subject_aliases (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  subject_id BIGINT NOT NULL,
  alias VARCHAR(255) NOT NULL,
  CONSTRAINT fk_alias_subject FOREIGN KEY (subject_id) REFERENCES subjects(id)
);

CREATE TABLE IF NOT EXISTS papers (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(255) NOT NULL,
  subject_id BIGINT NOT NULL,
  university_id BIGINT NOT NULL,
  uploader_id BIGINT NOT NULL,
  paper_year INT,
  student_year INT,
  access_type VARCHAR(30) DEFAULT 'PUBLIC',
  view_count BIGINT DEFAULT 0,
  download_count BIGINT DEFAULT 0,
  exam_type VARCHAR(80),
  author VARCHAR(150),
  status VARCHAR(30) DEFAULT 'PENDING',
  file_url VARCHAR(500),
  file_hash VARCHAR(64),
  ocr_confidence DOUBLE,
  duplicate_score DOUBLE,
  ai_confidence_score DOUBLE,
  quality_score DOUBLE,
  ocr_text TEXT,
  metadata_json JSON,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_papers_subject FOREIGN KEY (subject_id) REFERENCES subjects(id),
  CONSTRAINT fk_papers_university FOREIGN KEY (university_id) REFERENCES universities(id),
  CONSTRAINT fk_papers_uploader FOREIGN KEY (uploader_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS uploads (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  paper_id BIGINT,
  uploaded_by BIGINT NOT NULL,
  original_file_name VARCHAR(255),
  stored_path VARCHAR(500),
  file_hash VARCHAR(64),
  mime_type VARCHAR(100),
  file_size BIGINT,
  upload_status VARCHAR(30) DEFAULT 'PENDING',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_uploads_paper FOREIGN KEY (paper_id) REFERENCES papers(id),
  CONSTRAINT fk_uploads_user FOREIGN KEY (uploaded_by) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS questions (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  paper_id BIGINT NOT NULL,
  question_text TEXT NOT NULL,
  topic_id BIGINT,
  marks INT,
  difficulty_level VARCHAR(30),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_questions_paper FOREIGN KEY (paper_id) REFERENCES papers(id)
);

CREATE TABLE IF NOT EXISTS topic_mappings (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  question_id BIGINT NOT NULL,
  topic_name VARCHAR(200),
  confidence DOUBLE,
  CONSTRAINT fk_topic_question FOREIGN KEY (question_id) REFERENCES questions(id)
);

CREATE TABLE IF NOT EXISTS ratings (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  paper_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  score INT NOT NULL,
  comment VARCHAR(500),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_ratings_paper FOREIGN KEY (paper_id) REFERENCES papers(id),
  CONSTRAINT fk_ratings_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS bookmarks (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  paper_id BIGINT NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_bookmarks_user FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT fk_bookmarks_paper FOREIGN KEY (paper_id) REFERENCES papers(id),
  UNIQUE KEY uk_bookmark_user_paper (user_id, paper_id)
);

CREATE TABLE IF NOT EXISTS reports (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  paper_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  report_type VARCHAR(50),
  description VARCHAR(500),
  status VARCHAR(30) DEFAULT 'OPEN',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_reports_paper FOREIGN KEY (paper_id) REFERENCES papers(id),
  CONSTRAINT fk_reports_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS notifications (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  title VARCHAR(200) NOT NULL,
  message TEXT NOT NULL,
  type VARCHAR(50),
  is_read BOOLEAN DEFAULT FALSE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS verification_logs (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  paper_id BIGINT,
  upload_id BIGINT,
  stage VARCHAR(100) NOT NULL,
  score DOUBLE,
  details_json JSON,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_verification_paper FOREIGN KEY (paper_id) REFERENCES papers(id),
  CONSTRAINT fk_verification_upload FOREIGN KEY (upload_id) REFERENCES uploads(id)
);

CREATE TABLE IF NOT EXISTS contributor_scores (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  score INT DEFAULT 0,
  tier VARCHAR(30) DEFAULT 'NEW_CONTRIBUTOR',
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_contributor_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS admin_actions (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  admin_id BIGINT NOT NULL,
  paper_id BIGINT,
  action_type VARCHAR(50) NOT NULL,
  reason VARCHAR(500),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_admin_actions_admin FOREIGN KEY (admin_id) REFERENCES users(id),
  CONSTRAINT fk_admin_actions_paper FOREIGN KEY (paper_id) REFERENCES papers(id)
);

CREATE TABLE IF NOT EXISTS faculty_verification (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  faculty_id BIGINT NOT NULL,
  university_id BIGINT,
  verification_status VARCHAR(30) DEFAULT 'PENDING',
  documents_url VARCHAR(500),
  reviewed_by BIGINT,
  reviewed_at TIMESTAMP,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_faculty_verif_user FOREIGN KEY (faculty_id) REFERENCES users(id),
  CONSTRAINT fk_faculty_verif_university FOREIGN KEY (university_id) REFERENCES universities(id),
  CONSTRAINT fk_faculty_verif_reviewer FOREIGN KEY (reviewed_by) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS access_requests (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  paper_id BIGINT NOT NULL,
  requester_id BIGINT NOT NULL,
  permission_level VARCHAR(30) DEFAULT 'VIEW',
  message VARCHAR(500),
  status VARCHAR(30) DEFAULT 'PENDING',
  decided_by BIGINT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_access_requests_paper FOREIGN KEY (paper_id) REFERENCES papers(id),
  CONSTRAINT fk_access_requests_requester FOREIGN KEY (requester_id) REFERENCES users(id),
  CONSTRAINT fk_access_requests_decided_by FOREIGN KEY (decided_by) REFERENCES users(id)
);

INSERT INTO roles (name) VALUES ('STUDENT'), ('FACULTY'), ('ADMIN')
ON DUPLICATE KEY UPDATE name=name;

INSERT INTO subjects (name, canonical_name) VALUES
('Database Management Systems', 'Database Management Systems'),
('Operating Systems', 'Operating Systems'),
('Computer Networks', 'Computer Networks')
ON DUPLICATE KEY UPDATE name=name;

SET @dbms_id = (SELECT id FROM subjects WHERE canonical_name='Database Management Systems' LIMIT 1);
SET @os_id = (SELECT id FROM subjects WHERE canonical_name='Operating Systems' LIMIT 1);
SET @cn_id = (SELECT id FROM subjects WHERE canonical_name='Computer Networks' LIMIT 1);

INSERT INTO subject_aliases (subject_id, alias) VALUES
(@dbms_id, 'DBMS'),
(@dbms_id, 'Database Systems'),
(@os_id, 'OS'),
(@cn_id, 'CN');
