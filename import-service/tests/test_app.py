import asyncio
import io

import pytest
from fastapi.testclient import TestClient
from starlette.datastructures import UploadFile

import app as app_module
from app import app, read_limited
from config import Settings, settings
from csv_validation import CsvUploadError

client = TestClient(app)


def upload(content, *, filename="usuarios.csv", data=None, mime="text/csv"):
    return client.post(
        "/upload-csv/", files={"file": (filename, content, mime)}, data=data
    )


def test_upload_success():
    response = upload("nome,idade,cidade\nAlice,30,São Paulo\nBob,25,Rio de Janeiro")
    assert response.status_code == 200
    assert response.json() == {
        "filename": "usuarios.csv",
        "total_linhas": 2,
        "amostra_dados": [
            {"nome": "Alice", "idade": "30", "cidade": "São Paulo"},
            {"nome": "Bob", "idade": "25", "cidade": "Rio de Janeiro"},
        ],
    }


def test_upload_invalid_file_extension():
    response = upload("nome\nAna", filename="texto.txt", mime="text/plain")
    assert response.status_code == 400
    assert response.json()["detail"]["codigo"] == "EXTENSAO_INVALIDA"


@pytest.mark.parametrize("filename", ["DADOS.CSV", "dados.CsV"])
def test_uppercase_extension(filename):
    assert upload("nome\nAna", filename=filename).status_code == 200


@pytest.mark.parametrize(
    "content,code",
    [
        (b"", "ARQUIVO_VAZIO"),
        (b"\xef\xbb\xbf \r\n", "ARQUIVO_VAZIO"),
        (b"nome,email\n", "SEM_REGISTROS"),
        (b"nome,nome\nAna,Bia", "CABECALHO_DUPLICADO"),
        (b" nome ,nome\nAna,Bia", "CABECALHO_DUPLICADO"),
        (b"nome,\nAna,a@exemplo.com", "CABECALHO_INVALIDO"),
        (b"nome,email\nAna", "QUANTIDADE_COLUNAS_INVALIDA"),
        (b"nome,email\nAna,a@exemplo.com,extra", "QUANTIDADE_COLUNAS_INVALIDA"),
        (b'nome,email\n"Ana,a@exemplo.com', "CSV_MALFORMADO"),
        (b'nome,email\n"Ana"errado,a@exemplo.com', "CSV_MALFORMADO"),
        (b"nome,email\n,", "REGISTRO_VAZIO"),
        (b"nome,email\n\x00Ana,a@exemplo.com", "CONTEUDO_INVALIDO"),
        (b"nome,email\n\x1bAna,a@exemplo.com", "CONTEUDO_INVALIDO"),
        ("nome\nJoão".encode("cp1252"), "CODIFICACAO_INVALIDA"),
    ],
)
def test_invalid_csv(content, code):
    response = upload(content)
    assert response.status_code == 422
    assert response.json()["detail"]["codigo"] == code


def test_invalid_row_reports_physical_line():
    response = upload("nome,email\nAna,a@exemplo.com\nBia")
    assert response.json()["detail"]["linha"] == 3


def test_utf8_bom_and_header_whitespace():
    response = upload(b"\xef\xbb\xbf nome , email \r\nAna,a@exemplo.com\r\n")
    assert response.status_code == 200
    assert response.json()["amostra_dados"] == [
        {"nome": "Ana", "email": "a@exemplo.com"}
    ]


@pytest.mark.parametrize("data", [None, {"separador": ";"}])
def test_semicolon_separator(data):
    response = upload("nome;valor\nAna;12,50", data=data)
    assert response.status_code == 200
    assert response.json()["amostra_dados"] == [{"nome": "Ana", "valor": "12,50"}]


def test_ambiguous_separator_requires_explicit_choice():
    response = upload("nome,email;contato\nAna,a@exemplo.com;123")
    assert response.status_code == 422
    assert response.json()["detail"]["codigo"] == "SEPARADOR_AMBIGUO"
    response = upload(
        "nome,email;contato\nAna,a@exemplo.com;123", data={"separador": ","}
    )
    assert response.status_code == 200


def test_quoted_delimiters_multiline_and_escaped_quotes():
    response = upload('nome,obs\n"Ana; Maria","linha 1\nlinha 2, com ""aspas"""')
    assert response.status_code == 200
    assert response.json()["amostra_dados"] == [
        {
            "nome": "Ana; Maria",
            "obs": 'linha 1\nlinha 2, com "aspas"',
        }
    ]


def test_preserves_leading_zeroes_spaces_and_empty_optional_value():
    response = upload("codigo,nome,email\n00123, Ana ,")
    assert response.status_code == 200
    assert response.json()["amostra_dados"] == [
        {"codigo": "00123", "nome": " Ana ", "email": ""}
    ]


def test_blank_lines_are_ignored():
    response = upload("\r\nnome,email\r\n\r\nAna,a@exemplo.com\r\n\r\n")
    assert response.status_code == 200
    assert response.json()["total_linhas"] == 1


def test_preview_is_limited_but_total_counts_all_records():
    response = upload("nome\n" + "\n".join(f"Pessoa {i}" for i in range(12)))
    assert response.status_code == 200
    assert response.json()["total_linhas"] == 12
    assert len(response.json()["amostra_dados"]) == 5


def test_records_after_preview_are_also_validated():
    response = upload("nome,email\n" + "Ana,a@exemplo.com\n" * 6 + "Bia")
    assert response.status_code == 422
    assert response.json()["detail"]["linha"] == 8


@pytest.mark.parametrize("request_kwargs", [{}, {"data": {"file": "texto"}}])
def test_missing_or_invalid_file_field(request_kwargs):
    response = client.post("/upload-csv/", **request_kwargs)
    assert response.status_code == 422
    assert response.json()["detail"]["codigo"] == "REQUISICAO_INVALIDA"


def test_invalid_separator():
    response = upload("nome\nAna", data={"separador": "|"})
    assert response.status_code == 422
    assert response.json()["detail"]["codigo"] == "REQUISICAO_INVALIDA"


def test_whitespace_filename():
    response = upload("nome\nAna", filename=" ")
    assert response.status_code == 400
    assert response.json()["detail"]["codigo"] == "NOME_ARQUIVO_INVALIDO"


def test_limit_rejects_one_byte_over_and_accepts_exact_limit(monkeypatch):
    content = b"nome\nAna"
    monkeypatch.setattr(settings, "MAX_UPLOAD_BYTES", len(content))
    assert upload(content).status_code == 200
    response = upload(content + b"a")
    assert response.status_code == 413
    assert response.json()["detail"]["codigo"] == "ARQUIVO_MUITO_GRANDE"


@pytest.mark.parametrize("reported_size", [None, 0])
def test_read_limit_without_trusting_reported_size(monkeypatch, reported_size):
    monkeypatch.setattr(app_module, "READ_CHUNK_BYTES", 3)
    file = UploadFile(io.BytesIO(b"1234567"), filename="dados.csv", size=reported_size)
    with pytest.raises(CsvUploadError) as error:
        asyncio.run(read_limited(file, 6))
    assert error.value.status_code == 413
    asyncio.run(file.close())


def test_oversized_field_is_a_validation_error():
    response = upload("nome\n" + "a" * 131073)
    assert response.status_code == 422
    assert response.json()["detail"]["codigo"] == "CSV_MALFORMADO"


def test_default_limit_and_positive_configuration():
    assert (
        Settings(_env_file=None, MAX_UPLOAD_BYTES=10 * 1024 * 1024).MAX_UPLOAD_BYTES
        == 10485760
    )
    with pytest.raises(ValueError):
        Settings(_env_file=None, MAX_UPLOAD_BYTES=0)


def test_internal_error_hides_details_and_is_logged(monkeypatch, caplog):
    def fail(*args):
        raise RuntimeError("senha-interna-secreta")

    monkeypatch.setattr(app_module, "validate_csv", fail)
    response = upload("nome\nAna")
    assert response.status_code == 500
    assert response.json()["detail"]["codigo"] == "ERRO_INTERNO"
    assert "senha-interna-secreta" not in response.text
    assert "Falha inesperada" in caplog.text


@pytest.mark.parametrize("content", [b"nome\nAna", b"", b"nome,email\nAna"])
def test_file_is_closed_after_processing(content):
    file = UploadFile(io.BytesIO(content), filename="dados.csv")
    asyncio.run(app_module.upload_csv(file, None))
    assert file.file.closed
