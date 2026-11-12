from fastapi import FastAPI
from api.chatbot import router as chatbot_router

app = FastAPI(
    title="Chatbot TMS API",
    description="Microserviço de IA para o sistema TMS",
    version="1.0.0"
)

# Regista as rotas definidas na pasta api
app.include_router(chatbot_router)

@app.get("/")
async def root():
    return {"status": "Serviço de IA operacional"}