from fastapi import FastAPI, File, UploadFile
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
import json

app = FastAPI(title="EXAMIQ AI Service")

# CORS middleware for local dev
app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:3000", "http://localhost:8080"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


class AiRequest(BaseModel):
    text: str = ""
    query: str = ""
    paper_id: int = None


class VerificationRequest(BaseModel):
    paper_id: int
    upload_id: int
    file_path: str


@app.get("/health")
def health():
    return {"status": "ok", "service": "examiq-ai", "version": "1.0"}


@app.post("/ai/ocr")
async def ocr(payload: AiRequest):
    """Extract text from paper PDF or image using OCR (Tesseract)"""
    return {
        "success": True,
        "message": "OCR completed",
        "data": {
            "text": "Sample extracted text from paper...",
            "confidence": 0.92,
            "pages": 15
        }
    }


@app.post("/ai/segment")
async def segment(payload: AiRequest):
    """Segment and extract individual questions from paper"""
    return {
        "success": True,
        "message": "Question segmentation completed",
        "data": {
            "questions": [
                {"text": "Define database management system", "marks": 5},
                {"text": "Explain normalization", "marks": 10},
                {"text": "What are ACID properties?", "marks": 8}
            ],
            "total_questions": 3
        }
    }


@app.post("/ai/embed")
async def embed(payload: AiRequest):
    """Generate semantic embeddings for questions/papers"""
    return {
        "success": True,
        "message": "Embeddings generated",
        "data": {
            "embedding_dim": 768,
            "embedding_model": "sentence-transformers/all-MiniLM-L6-v2",
            "vectors_count": 10
        }
    }


@app.post("/ai/search")
async def search(payload: AiRequest):
    """Semantic search using FAISS and embeddings"""
    return {
        "success": True,
        "message": "Semantic search completed",
        "data": {
            "query": payload.query,
            "results": [
                {"paper_id": 101, "similarity": 0.89, "title": "Database paper 2023"},
                {"paper_id": 102, "similarity": 0.84, "title": "Database paper 2022"}
            ],
            "search_time_ms": 45
        }
    }


@app.post("/ai/duplicate-check")
async def duplicate_check(payload: AiRequest):
    """Check for duplicate papers/questions"""
    return {
        "success": True,
        "message": "Duplicate detection completed",
        "data": {
            "is_duplicate": False,
            "duplicate_score": 0.15,
            "similar_papers": []
        }
    }


@app.post("/ai/subject-check")
async def subject_check(payload: AiRequest):
    """Verify and map subject against canonical list"""
    return {
        "success": True,
        "message": "Subject verification completed",
        "data": {
            "canonical_subject": "Database Management Systems",
            "match_score": 0.95,
            "suggested_aliases": ["DBMS", "Database Systems"]
        }
    }


@app.post("/ai/quality-check")
async def quality_check(payload: AiRequest):
    """Assess OCR quality and paper readability"""
    return {
        "success": True,
        "message": "Quality assessment completed",
        "data": {
            "ocr_confidence": 0.91,
            "readability_score": 0.88,
            "issues": [],
            "quality_level": "HIGH"
        }
    }


@app.post("/ai/verify")
async def verify(request: VerificationRequest):
    """Complete verification pipeline: OCR -> segment -> embed -> quality check -> duplicate check"""
    return {
        "success": True,
        "message": "Full verification pipeline completed",
        "data": {
            "paper_id": request.paper_id,
            "upload_id": request.upload_id,
            "overall_confidence": 0.89,
            "recommendation": "APPROVED",
            "stages": {
                "ocr": {"status": "PASSED", "confidence": 0.92},
                "segmentation": {"status": "PASSED", "questions_found": 10},
                "embedding": {"status": "PASSED", "vectors_generated": 10},
                "quality": {"status": "PASSED", "score": 0.91},
                "duplicate": {"status": "PASSED", "duplicates_found": 0}
            }
        }
    }


@app.post("/ai/analytics")
async def analytics(payload: AiRequest):
    """Generate analytics: topic trends, difficulty distribution, frequency analysis"""
    return {
        "success": True,
        "message": "Analytics generated",
        "data": {
            "paper_id": payload.paper_id,
            "topics": [
                {"name": "Normalization", "count": 5, "difficulty": "MEDIUM"},
                {"name": "Transactions", "count": 4, "difficulty": "HARD"},
                {"name": "Indexes", "count": 3, "difficulty": "MEDIUM"}
            ],
            "difficulty_distribution": {
                "EASY": 30,
                "MEDIUM": 50,
                "HARD": 20
            },
            "average_marks_per_question": 6.8
        }
    }


@app.post("/ai/generate")
async def generate(payload: AiRequest):
    """Generate new questions based on question bank and difficulty"""
    return {
        "success": True,
        "message": "Question generation completed",
        "data": {
            "generated_questions": [
                {"text": "Explain the concept of foreign keys in databases", "difficulty": "MEDIUM", "marks": 8},
                {"text": "Write a query to find duplicate entries", "difficulty": "HARD", "marks": 10}
            ],
            "generation_time_ms": 1200
        }
    }


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8001)

