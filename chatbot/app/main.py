from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from api.chatbot import router as chatbot_router
from db.session import engine


app = FastAPI(
    title="Chatbot TMS API",
    description="Microserviço de IA para o sistema TMS",
    version="1.0.0"
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(chatbot_router)

@app.get("/")
async def root():
    return {"status": "Serviço de IA operacional"}