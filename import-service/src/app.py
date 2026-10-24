import sys
from typing import Annotated

import polars as pl
from fastapi import BackgroundTasks, FastAPI, File, HTTPException, UploadFile, status
from loguru import logger

from src.config import settings
from src.process import process_dataframe
from src.schemas.upload_schema import UploadSummary

logger.remove()

logger.add(
    sys.stdout,
    level=settings.LOGGER_LEVEL,
    colorize=True,
    enqueue=True,
)

app = FastAPI()


@app.get("/")
def inicio():
    return {"status": "Online!"}


@app.post(
    "/upload-csv",
    status_code=status.HTTP_202_ACCEPTED,
    response_model=UploadSummary,
    responses={
        status.HTTP_400_BAD_REQUEST: {
            "description": "Arquivo inválido ou extensão incorreta"
        },
        status.HTTP_413_CONTENT_TOO_LARGE: {
            "descripton": "O arquivo enviado ultrapassou o limite"
        },
    },
)
async def upload_csv(
    file: Annotated[UploadFile, File()], background_task: BackgroundTasks
):
    if not file:
        raise HTTPException(status_code=400, datail="Nenhum arquivo enviado.")

    if file.filename is not None and not file.filename.endswith(".csv"):
        raise HTTPException(
            status_code=400, detail="Apenas arquivos .csv são permitidos."
        )

    if file.size and file.size > settings.MAX_UPLOAD_BYTES:
        raise HTTPException(
            status_code=413, detail="O arquivo excede o limite permitido de 50MB."
        )

    try:
        contents = await file.read()
        df = pl.read_csv(
            contents,
            schema={
                "ativo": pl.Int8,
                "e-mail": pl.String,
                "nome": pl.String,
                "telefone": pl.String,
            },
            extra_columns="ignore",
        ).rename({"e-mail": "email"})
        background_task.add_task(process_dataframe, df)

        return UploadSummary(
            status="Received",
            filename=file.filename or "",
            size=str(file.size),
            lines=df.height,
        )
    except pl.exceptions.SchemaError as e:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=str(e))

    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Erro ao processar o arquivo: {e!s}",
        )
