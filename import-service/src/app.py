import logging
from typing import Annotated, Literal

import httpx
from fastapi import Depends, FastAPI, File, Form, Request, UploadFile
from fastapi.concurrency import run_in_threadpool
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer

from config import settings
from core_import import get_core_client, persist_resources
from csv_validation import CsvUploadError, validate_csv
from resource_import import build_import_plan

logger = logging.getLogger(__name__)
app = FastAPI()
READ_CHUNK_BYTES = 64 * 1024
bearer = HTTPBearer(auto_error=False)


@app.exception_handler(CsvUploadError)
async def csv_validation_error(request: Request, exc: CsvUploadError):
    headers = {"WWW-Authenticate": "Bearer"} if exc.status_code == 401 else None
    return JSONResponse(
        status_code=exc.status_code, content={"detail": exc.detail}, headers=headers
    )


@app.exception_handler(Exception)
async def unexpected_error(request: Request, exc: Exception):
    logger.error(
        "Falha inesperada no Import Service",
        exc_info=(type(exc), exc, exc.__traceback__),
    )
    return JSONResponse(
        status_code=500,
        content={
            "detail": {
                "codigo": "ERRO_INTERNO",
                "mensagem": "Não foi possível processar a solicitação. Tente novamente mais tarde.",
            }
        },
    )


@app.exception_handler(RequestValidationError)
async def request_validation_error(request: Request, exc: RequestValidationError):
    return JSONResponse(
        status_code=422,
        content={
            "detail": {
                "codigo": "REQUISICAO_INVALIDA",
                "mensagem": "Envie um arquivo no campo 'file' e, opcionalmente, separador ',' ou ';'.",
            }
        },
    )


@app.get("/")
def inicio():
    return {"mensagem": "API funcionando!"}


async def read_limited(file: UploadFile, limit: int) -> bytes:
    # UploadFile.size is calculated by the multipart parser, not the client header.
    if file.size is not None and file.size > limit:
        raise CsvUploadError(
            413, "ARQUIVO_MUITO_GRANDE", f"O limite do arquivo é {limit} bytes."
        )

    contents = bytearray()
    while chunk := await file.read(min(READ_CHUNK_BYTES, limit - len(contents) + 1)):
        if len(contents) + len(chunk) > limit:
            raise CsvUploadError(
                413, "ARQUIVO_MUITO_GRANDE", f"O limite do arquivo é {limit} bytes."
            )
        contents.extend(chunk)
    return bytes(contents)


def validate_filename(file: UploadFile) -> str:
    filename = file.filename
    if not filename or not filename.strip():
        raise CsvUploadError(
            400, "NOME_ARQUIVO_INVALIDO", "O arquivo precisa ter um nome."
        )
    if not filename.lower().endswith(".csv"):
        raise CsvUploadError(
            400, "EXTENSAO_INVALIDA", "Apenas arquivos .csv são permitidos."
        )
    return filename


@app.post(
    "/upload-csv/",
    responses={
        400: {"description": "Nome ou extensão do arquivo inválidos."},
        413: {"description": "Arquivo acima de MAX_UPLOAD_BYTES."},
        422: {"description": "Requisição, codificação ou estrutura CSV inválida."},
        500: {"description": "Falha interna; detalhes registrados apenas no servidor."},
    },
)
async def upload_csv(
    file: Annotated[UploadFile, File()],
    separador: Annotated[Literal[",", ";"] | None, Form()] = None,
):
    try:
        filename = validate_filename(file)

        contents = await read_limited(file, settings.MAX_UPLOAD_BYTES)
        # Parsing is synchronous; run it outside the application's event loop.
        preview = await run_in_threadpool(validate_csv, contents, separador)
        return {"filename": filename, **preview}
    except CsvUploadError as exc:
        return JSONResponse(status_code=exc.status_code, content={"detail": exc.detail})
    except Exception:
        logger.exception("Falha inesperada ao validar upload CSV")
        return JSONResponse(
            status_code=500,
            content={
                "detail": {
                    "codigo": "ERRO_INTERNO",
                    "mensagem": "Não foi possível processar o arquivo. Tente novamente mais tarde.",
                }
            },
        )
    finally:
        await file.close()


def require_bearer(
    credentials: Annotated[HTTPAuthorizationCredentials | None, Depends(bearer)],
) -> str:
    if credentials is None:
        raise CsvUploadError(
            401,
            "NAO_AUTORIZADO",
            "Envie o token de acesso em Authorization: Bearer <token>.",
        )
    # The Core verifies the signature, expiry and roles; do not trust decoded JWT claims here.
    return f"Bearer {credentials.credentials}"


async def prepare_resources(file: UploadFile, separator: str | None):
    try:
        filename = validate_filename(file)
        contents = await read_limited(file, settings.MAX_UPLOAD_BYTES)
        plan = await run_in_threadpool(
            build_import_plan,
            contents,
            separator,
            settings.MAX_IMPORT_RESOURCES,
            settings.MAX_IMPORT_ROWS,
        )
        return filename, plan
    finally:
        await file.close()


@app.post("/api/v1/importacoes/recursos/validar", tags=["Importação de recursos"])
async def validate_resources(
    file: Annotated[UploadFile, File()],
    separador: Annotated[Literal[",", ";"] | None, Form()] = None,
):
    filename, plan = await prepare_resources(file, separador)
    return {"filename": filename, **plan.preview()}


@app.post(
    "/api/v1/importacoes/recursos",
    tags=["Importação de recursos"],
    responses={
        200: {"description": "Relatório por recurso; confira status e contadores."},
        401: {"description": "Token ausente ou rejeitado pelo Core."},
        403: {"description": "Perfil sem permissão no Core."},
        413: {"description": "Arquivo acima do limite."},
        422: {
            "description": "Arquivo ou regras locais inválidos; nenhuma chamada de gravação."
        },
        502: {
            "description": "Falha ou resposta inesperada do Core; consulte relatorio."
        },
        503: {"description": "Core indisponível; consulte relatorio."},
        504: {
            "description": "Timeout com resultado indeterminado; consulte relatorio."
        },
    },
)
async def import_resources(
    file: Annotated[UploadFile, File()],
    authorization: Annotated[str, Depends(require_bearer)],
    client: Annotated[httpx.AsyncClient, Depends(get_core_client)],
    separador: Annotated[Literal[",", ";"] | None, Form()] = None,
):
    filename, plan = await prepare_resources(file, separador)
    status_code, report = await persist_resources(filename, plan, client, authorization)
    headers = {"WWW-Authenticate": "Bearer"} if status_code == 401 else None
    return JSONResponse(status_code=status_code, content=report, headers=headers)
