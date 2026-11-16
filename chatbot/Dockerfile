FROM python:3.12-slim

WORKDIR /workspace

# Evita geração de .pyc e garante logs em tempo real no Docker
ENV PYTHONDONTWRITEBYTECODE=1
ENV PYTHONUNBUFFERED=1

# Instala dependências primeiro (aproveita cache do Docker)
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copia o código da aplicação e a base de conhecimento do RAG
COPY app/ ./app/
COPY docs/ ./docs/

WORKDIR /workspace/app

EXPOSE 8000

CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]