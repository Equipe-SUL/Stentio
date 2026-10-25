from typing import Annotated, Literal

from fastapi import FastAPI, File, Form, UploadFile
from fastapi.concurrency import run_in_threadpool
from fastapi.exceptions import RequestValidationError
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from loguru import logger
from sqlalchemy.exc import DataError, IntegrityError, SQLAlchemyError

from src.config import settings
from src.import_plan import ImportErrorDetail, build_plan, read_dataframe
from src.process import process_plan
from src.schemas.upload_schema import UploadSummary, ValidationSummary

app = FastAPI(title="Stentio Import Service", version="0.2.0")
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.CORS_ALLOWED_ORIGINS,
    allow_credentials=True,
    allow_methods=["GET", "POST"],
    allow_headers=["Content-Type", "Authorization", "X-Stentio-Import"],
)


@app.exception_handler(ImportErrorDetail)
async def import_error(request, exc):
    return JSONResponse(status_code=exc.status, content={"detail": exc.detail})


@app.exception_handler(RequestValidationError)
async def request_error(request, exc):
    return JSONResponse(
        status_code=422,
        content={
            "detail": {
                "codigo": "REQUISICAO_INVALIDA",
                "mensagem": "Envie 'file' e, opcionalmente, 'separador' (; ou ,).",
            }
        },
    )


@app.exception_handler(Exception)
async def unexpected_error(request, exc):
    logger.error("Falha inesperada: {}", type(exc).__name__)
    return JSONResponse(
        status_code=500,
        content={
            "detail": {
                "codigo": "ERRO_INTERNO",
                "mensagem": "Não foi possível concluir a solicitação.",
            }
        },
    )


@app.get("/")
def inicio():
    return {"status": "Online!"}


@app.get("/api/v1/importacoes/configuracao")
def configuration():
    return {
        "max_upload_bytes": settings.MAX_UPLOAD_BYTES,
        "max_recursos": settings.MAX_IMPORT_RESOURCES,
        "max_linhas": settings.MAX_IMPORT_ROWS,
    }


async def prepare(file, separator):
    try:
        if not file.filename or not file.filename.lower().endswith(".csv"):
            raise ImportErrorDetail(
                "Envie um arquivo com extensão .csv.",
                code="EXTENSAO_INVALIDA",
                status=400,
            )
        if file.size is not None and file.size > settings.MAX_UPLOAD_BYTES:
            raise ImportErrorDetail(
                "Arquivo acima do limite configurado.",
                code="ARQUIVO_MUITO_GRANDE",
                status=413,
            )
        contents = bytearray()
        while chunk := await file.read(
            min(65536, settings.MAX_UPLOAD_BYTES - len(contents) + 1)
        ):
            contents.extend(chunk)
            if len(contents) > settings.MAX_UPLOAD_BYTES:
                raise ImportErrorDetail(
                    "Arquivo acima do limite configurado.",
                    code="ARQUIVO_MUITO_GRANDE",
                    status=413,
                )
        df = await run_in_threadpool(
            read_dataframe, bytes(contents), separator, settings.MAX_IMPORT_ROWS
        )
        plan = await run_in_threadpool(build_plan, df, settings.MAX_IMPORT_RESOURCES)
        return plan
    finally:
        await file.close()


@app.post("/upload-csv/validar", response_model=ValidationSummary)
async def validate_csv(
    file: Annotated[UploadFile, File()],
    separador: Annotated[Literal[";", ","], Form()] = ";",
):
    plan = await prepare(file, separador)
    return {"status": "Validado", "filename": file.filename, **plan.preview()}


@app.post("/upload-csv", response_model=UploadSummary)
async def upload_csv(
    file: Annotated[UploadFile, File()],
    separador: Annotated[Literal[";", ","], Form()] = ";",
):
    plan = await prepare(file, separador)
    try:
        report = await process_plan(plan)
    except IntegrityError as exc:
        raise ImportErrorDetail(
            "Conflito de integridade no banco. A transação foi revertida.",
            code="CONFLITO_BANCO",
            status=409,
        ) from exc
    except DataError as exc:
        raise ImportErrorDetail(
            "Dados incompatíveis com o banco. A transação foi revertida.",
            code="DADOS_BANCO_INVALIDOS",
            status=422,
        ) from exc
    except (SQLAlchemyError, OSError, TimeoutError) as exc:
        logger.error("Falha no banco durante importação: {}", type(exc).__name__)
        raise ImportErrorDetail(
            "Falha de acesso ao banco. A importação não foi confirmada.",
            code="BANCO_INDISPONIVEL",
            status=503,
        ) from exc
    return {
        "status": "Concluido",
        "filename": file.filename,
        "total_linhas": plan.lines,
        "total_recursos": len(plan.resources),
        "total_servicos": len(plan.services),
        **report,
    }
