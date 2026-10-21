import csv
import io
from typing import Annotated

from fastapi import FastAPI, File, HTTPException, UploadFile

app = FastAPI()


@app.get("/")
def inicio():
    return {"mensagem": "API funcionando!"}


# TODO: implementar o processamento adequado do arquivo usando polars
@app.post("/upload-csv/")
async def upload_csv(file: Annotated[UploadFile, File()]):
    if not file:
        raise HTTPException(status_code=400, datail="Nenhum arquivo enviado.")

    if file.filename is not None and not file.filename.endswith(".csv"):
        raise HTTPException(
            status_code=400, detail="Apenas arquivos .csv são permitidos."
        )

    try:
        contents = await file.read()
        decoded_content = contents.decode("utf-8")

        csv_file = io.StringIO(decoded_content)
        reader = csv.DictReader(csv_file)

        rows = [row for row in reader]

        return {
            "filename": file.filename,
            "total_linhas": len(rows),
            "amostra_dados": rows[:5],
        }

    except Exception as e:
        raise HTTPException(
            status_code=500, detail=f"Erro ao processar o arquivo: {e!s}"
        )
