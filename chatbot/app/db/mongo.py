from core.config import settings
from pymongo import ASCENDING, MongoClient

client = MongoClient(settings.MONGO_URI)
mongo_db = client[settings.MONGO_DB_NAME]

# Coleção onde o histórico será armazenado
mensagens_collection = mongo_db["chatbot_mensagens"]

# Cria índices para deixar a consulta de histórico instantânea
mensagens_collection.create_index([("usuario_id", ASCENDING), ("conversa_id", ASCENDING)])
