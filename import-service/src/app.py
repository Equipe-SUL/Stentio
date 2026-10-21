import logging
from typing import Annotated, Literal

from fastapi import FastAPI, File, Form, Request, UploadFile
from fastapi.concurrency import run_in_threadpool
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse

from config import settings
from csv_validation import CsvUploadError, validate_csv

logger = logging.getLogger(__name__)
app = FastAPI()
READ_CHUNK_BYTES = 64 * 1024


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
        filename = file.filename
        if not filename or not filename.strip():
            raise CsvUploadError(
                400, "NOME_ARQUIVO_INVALIDO", "O arquivo precisa ter um nome."
            )
        if not filename.lower().endswith(".csv"):
            raise CsvUploadError(
                400, "EXTENSAO_INVALIDA", "Apenas arquivos .csv são permitidos."
            )

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
