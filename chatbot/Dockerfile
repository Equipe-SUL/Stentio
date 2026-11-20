FROM ghcr.io/astral-sh/uv:0.13.7-python3.12-trixie-slim AS build

WORKDIR /app

ENV UV_COMPILE_BYTECODE=1 \
    UV_LINK_MODE=copy \
    PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1

COPY pyproject.toml uv.lock ./
RUN --mount=type=cache,target=/root/.cache/uv \
    uv sync --locked --no-install-project

COPY app ./app
COPY docs ./docs
COPY tests ./tests

RUN DATABASE_URL=sqlite:// \
    GROQ_API_KEY=pytest-key \
    MONGO_URI=mongodb://localhost:27017 \
    MONGO_DB_NAME=stentio_chatbot_test \
    uv run --locked pytest -q

RUN --mount=type=cache,target=/root/.cache/uv \
    uv sync --locked --no-dev --no-install-project

FROM python:3.12-slim-trixie AS runtime

WORKDIR /app

ENV PATH="/app/.venv/bin:$PATH" \
    PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1

COPY --from=build /app/.venv /app/.venv
COPY app ./app
COPY docs ./docs

WORKDIR /app/app

EXPOSE 8000

CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]
