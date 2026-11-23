FROM ghcr.io/astral-sh/uv:python3.14-trixie-slim AS builder

WORKDIR /app

COPY pyproject.toml uv.lock ./

RUN uv sync --frozen --no-install-project --no-dev

FROM python:3.14-slim AS final

WORKDIR /app

ENV PYTHONUNBUFFERED=1

ENV PATH="/app/.venv/bin:$PATH"

COPY --from=builder /app/.venv /app/.venv

COPY ./src ./src

EXPOSE 8000

CMD ["uvicorn", "src.app:app", "--host", "0.0.0.0", "--port", "8000"]