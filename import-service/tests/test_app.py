from unittest.mock import AsyncMock

import pytest
from fastapi.testclient import TestClient

from src.app import app
from src.config import Settings, settings

HEADER = "ativo;e-mail;nome;telefone;tipo_servico;descricao_servico\n"
ROW = "1;ana@example.com;Ana;01199990000;Tradução;Textos\n"


def post(
    client, content, path="/upload-csv/validar", filename="recursos.csv", **kwargs
):
    return client.post(path, files={"file": (filename, content, "text/csv")}, **kwargs)


def test_logger_and_url_encoding():
    cfg = Settings(LOGGER_LEVEL="info", POSTGRES_PASSWORD="@:/#senha")
    assert cfg.LOGGER_LEVEL == "INFO"
    assert cfg.database_url.password == "@:/#senha"
    assert "@:/#senha" not in str(cfg.database_url)


def test_preview_and_configuration():
    with TestClient(app) as client:
        result = post(client, (HEADER + ROW).encode())
        assert result.status_code == 200
        body = result.json()
        assert (
            body["total_recursos"]
            == body["total_servicos"]
            == body["total_vinculos"]
            == 1
        )
        assert body["amostra_recursos"][0]["telefone"] == "01199990000"
        assert (
            client.get("/api/v1/importacoes/configuracao").json()["max_upload_bytes"]
            > 0
        )


@pytest.mark.parametrize(
    "content,filename,status",
    [
        (b"", "vazio.csv", 422),
        (b"arquivo", "arquivo.txt", 400),
        (HEADER.encode(), "cabecalho.csv", 422),
        (b"\xff", "encoding.csv", 422),
        ((HEADER + "1;ana@example.com\n").encode(), "colunas.csv", 422),
        ((HEADER + ROW.replace("1;", "2;", 1)).encode(), "ativo.csv", 422),
        (
            (HEADER + ROW.replace("ana@example.com", "invalido")).encode(),
            "email.csv",
            422,
        ),
        ((HEADER + ROW.replace(";Tradução;", ";;")).encode(), "servico.csv", 422),
        ((HEADER + ROW.replace(";Ana;", ";;")).encode(), "nome.csv", 422),
        ((HEADER + ROW.replace("Textos", "X" * 501)).encode(), "descricao.csv", 422),
        ((HEADER + ROW.replace("Textos", "texto\x00")).encode(), "controle.csv", 422),
        ((HEADER + ROW + ROW.replace("Ana;", "Outro;")).encode(), "conflito.csv", 422),
        (
            (HEADER + ROW + ROW.replace("Textos", "Outros")).encode(),
            "descricao_conflito.csv",
            422,
        ),
        ((HEADER.replace("nome", "telefone") + ROW).encode(), "duplicado.csv", 422),
        ((HEADER + ROW + '"aspas abertas').encode(), "aspas.csv", 422),
    ],
)
def test_invalid_files_never_write(monkeypatch, content, filename, status):
    writer = AsyncMock()
    monkeypatch.setattr("src.app.process_plan", writer)
    with TestClient(app) as client:
        response = post(client, content, "/upload-csv", filename)
    assert response.status_code == status
    writer.assert_not_called()


def test_uppercase_bom_and_reordered_columns():
    content = "\ufeffnome;tipo_servico;ativo;e-mail;descricao_servico;telefone\nAna;Tradução;1;ana@example.com;;0012345678\n"
    with TestClient(app) as client:
        result = post(client, content.encode(), filename="RECURSOS.CSV")
    assert result.status_code == 200
    assert result.json()["amostra_recursos"][0]["telefone"] == "0012345678"


def test_explicit_comma_separator():
    with TestClient(app) as client:
        response = post(
            client, (HEADER + ROW).replace(";", ",").encode(), data={"separador": ","}
        )
    assert response.status_code == 200


def test_exact_upload_limit_and_one_byte_over(monkeypatch):
    data = (HEADER + ROW).encode()
    monkeypatch.setattr(settings, "MAX_UPLOAD_BYTES", len(data))
    with TestClient(app) as client:
        assert post(client, data).status_code == 200
        assert post(client, data + b"\n").status_code == 413


def test_row_and_resource_limits(monkeypatch):
    monkeypatch.setattr(settings, "MAX_IMPORT_ROWS", 1)
    with TestClient(app) as client:
        assert post(client, (HEADER + ROW * 2).encode()).status_code == 422
    monkeypatch.setattr(settings, "MAX_IMPORT_ROWS", 10)
    monkeypatch.setattr(settings, "MAX_IMPORT_RESOURCES", 1)
    with TestClient(app) as client:
        assert (
            post(
                client,
                (
                    HEADER + ROW + ROW.replace("ana@example.com", "bia@example.com")
                ).encode(),
            ).status_code
            == 422
        )


def test_missing_file_and_invalid_separator():
    with TestClient(app) as client:
        assert client.post("/upload-csv").status_code == 422
        assert (
            post(client, (HEADER + ROW).encode(), data={"separador": "|"}).status_code
            == 422
        )


def test_error_message_does_not_expose_sql_or_password(monkeypatch):
    from sqlalchemy.exc import IntegrityError

    writer = AsyncMock(
        side_effect=IntegrityError(
            "SQL SENSIVEL", {"senha": "SECRETO"}, Exception("SECRETO")
        )
    )
    monkeypatch.setattr("src.app.process_plan", writer)
    with TestClient(app) as client:
        response = post(client, (HEADER + ROW).encode(), "/upload-csv")
    assert response.status_code == 409
    assert "SECRETO" not in response.text and "SQL SENSIVEL" not in response.text


def test_cors_for_local_frontend():
    with TestClient(app) as client:
        response = client.options(
            "/upload-csv",
            headers={
                "Origin": "http://localhost",
                "Access-Control-Request-Method": "POST",
                "Access-Control-Request-Headers": "content-type,x-stentio-import",
            },
        )
    assert response.status_code == 200
    assert response.headers["access-control-allow-origin"] == "http://localhost"


def test_database_offline_returns_503(monkeypatch):
    monkeypatch.setattr(
        "src.app.process_plan", AsyncMock(side_effect=ConnectionRefusedError())
    )
    with TestClient(app) as client:
        response = post(client, (HEADER + ROW).encode(), "/upload-csv")
    assert response.status_code == 503
    assert response.json()["detail"]["codigo"] == "BANCO_INDISPONIVEL"


def test_preview_never_calls_writer(monkeypatch):
    writer = AsyncMock()
    monkeypatch.setattr("src.app.process_plan", writer)
    with TestClient(app) as client:
        assert post(client, (HEADER + ROW).encode()).status_code == 200
    writer.assert_not_called()
