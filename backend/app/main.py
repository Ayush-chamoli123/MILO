
from fastapi import FastAPI

app = FastAPI(
    title="MILO API",
    description="Backend API for the MILO application",
    version="1.0.0",
)


@app.get("/")
def root():
    return {
        "message": "Welcome to MILO API",
        "status": "running",
    }


@app.get("/api/health")
def health_check():
    return {
        "status": "healthy",
        "service": "MILO Backend",
    }
