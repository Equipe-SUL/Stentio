import csv
import io
import json
from uuid import UUID

import httpx
import pytest
from fastapi.testclient import TestClient

from app import app
from config import settings
from core_import import get_core_client

VALIDATE = "/api/v1/importacoes/recursos/validar"
IMPORT = "/api/v1/importacoes/recursos"
COLUMNS = [
    "nome",
    "email",
    "telefone",
    "tipos_servico_ids",
    "idioma_origem_id",
    "idioma_destino_id",
    "unidade",
    "valor",
]
SERVICE = str(UUID(int=1))
ORIGIN = str(UUID(int=2))
DESTINATION = str(UUID(int=3))


def row(**changes):
    result = {
        "nome": "Ana",
        "email": "ana@example.com",
        "telefone": "(12) 99999-0000",
        "tipos_servico_ids": SERVICE,
        "idioma_origem_id": ORIGIN,
        "idioma_destino_id": DESTINATION,
        "unidade": "PALAVRA",
        "valor": "0,1234",
    }
    result.update(changes)
    return result


def make_csv(rows=None, columns=None):
    stream = io.StringIO(newline="")
    writer = csv.DictWriter(stream, fieldnames=columns or COLUMNS, delimiter=";")
    writer.writeheader()
    writer.writerows(rows if rows is not None else [row()])
    return stream.getvalue().encode("utf-8")


class FakeCore:
    def __init__(self):
        self.requests = []
        self.handler = None

    def __call__(self, request):
        self.requests.append(request)
        assert request.method == "POST"
        assert request.url.path == "/api/v1/recursos"
        if self.handler:
            return self.handler(request)
        return httpx.Response(
            201, json={"recurso": {"id": str(UUID(int=100 + len(self.requests)))}}
        )


@pytest.fixture
def core():
    fake = FakeCore()

    async def override():
        async with httpx.AsyncClient(
            base_url="http://core/",
            transport=httpx.MockTransport(fake),
        ) as client:
            yield client

    app.dependency_overrides[get_core_client] = override
    yield fake
    app.dependency_overrides.pop(get_core_client, None)


@pytest.fixture
def client():
    return TestClient(app)


def upload(
    client,
    endpoint=VALIDATE,
    *,
    rows=None,
    content=None,
    token="token-do-usuario",
    filename="recursos.csv",
):
    headers = {"Authorization": f"Bearer {token}"} if token else {}
    return client.post(
        endpoint,
        files={
            "file": (
                filename,
                content if content is not None else make_csv(rows),
                "text/csv",
            )
        },
        headers=headers,
    )


def test_validation_groups_prices_and_normalizes_fields_without_writing(client, core):
    response = upload(
        client,
        rows=[
            row(nome=" Ana ", email=" ANA@EXAMPLE.COM "),
            row(
                idioma_origem_id=DESTINATION,
                idioma_destino_id=ORIGIN,
                unidade="hora",
                valor="12.50",
            ),
        ],
    )
    assert response.status_code == 200
    data = response.json()
    assert data["validacao"] == "LOCAL"
    assert data["total_linhas"] == 2
    assert data["total_recursos"] == 1
    sample = data["amostra_recursos"][0]
    assert sample["linhas"] == [2, 3]
    assert sample["dados"]["nome"] == "Ana"
    assert sample["dados"]["email"] == "ana@example.com"
    assert len(sample["dados"]["precos"]) == 2
    assert sample["dados"]["precos"][0]["valor"] == "0.1234"
    assert sample["dados"]["precos"][1]["unidade"] == "HORA"
    assert not core.requests


def test_import_passes_token_and_correct_core_payload(client, core):
    response = upload(client, IMPORT)
    assert response.status_code == 200
    assert response.json()["status"] == "CONCLUIDA"
    assert response.json()["importados"] == 1
    request = core.requests[0]
    assert request.headers["Authorization"] == "Bearer token-do-usuario"
    payload = json.loads(request.content)
    assert payload["tiposServicoIds"] == [SERVICE]
    assert payload["precos"][0] == {
        "idiomaOrigemId": ORIGIN,
        "idiomaDestinoId": DESTINATION,
        "unidade": "PALAVRA",
        "valor": "0.1234",
    }
    assert "ativo" not in payload


@pytest.mark.parametrize(
    "changes",
    [
        {"nome": " "},
        {"nome": "n" * 101},
        {"email": "email-invalido"},
        {"email": "a" * 145 + "@example.com"},
        {"telefone": "abc"},
        {"tipos_servico_ids": "uuid-invalido"},
        {"tipos_servico_ids": ""},
        {"tipos_servico_ids": SERVICE + "|" + SERVICE},
        {"tipos_servico_ids": "|".join(str(UUID(int=i)) for i in range(1, 22))},
        {"idioma_origem_id": "invalido"},
        {"idioma_destino_id": ORIGIN},
        {"unidade": "PAGINA"},
        {"valor": "0"},
        {"valor": "-1"},
        {"valor": "1.234,56"},
        {"valor": "0.00001"},
        {"valor": "100000000"},
        {"valor": "NaN"},
        {"valor": "1e3"},
    ],
)
def test_local_business_errors_prevent_all_writes(client, core, changes):
    response = upload(
        client, IMPORT, rows=[row(email="outra@example.com"), row(**changes)]
    )
    assert response.status_code == 422
    assert response.json()["detail"]["linha"] == 3
    assert not core.requests


def test_unknown_columns_are_not_silently_discarded(client, core):
    response = upload(
        client, IMPORT, content=make_csv([row(ativo="false")], COLUMNS + ["ativo"])
    )
    assert response.status_code == 422
    assert response.json()["detail"]["codigo"] == "LAYOUT_RECURSOS_INVALIDO"
    assert not core.requests


def test_missing_columns(client, core):
    response = upload(client, IMPORT, content=b"nome,email\nAna,ana@example.com")
    assert response.status_code == 422
    assert response.json()["detail"]["codigo"] == "LAYOUT_RECURSOS_INVALIDO"
    assert not core.requests


def test_phone_column_is_optional(client, core):
    data = row()
    data.pop("telefone")
    response = upload(
        client, content=make_csv([data], [col for col in COLUMNS if col != "telefone"])
    )
    assert response.status_code == 200
    assert response.json()["amostra_recursos"][0]["dados"]["telefone"] is None


@pytest.mark.parametrize(
    "changes,code",
    [
        ({"nome": "Outra pessoa"}, "DADOS_RECURSO_DIVERGENTES"),
        ({"telefone": "(11) 99999-9999"}, "DADOS_RECURSO_DIVERGENTES"),
        ({"tipos_servico_ids": str(UUID(int=9))}, "DADOS_RECURSO_DIVERGENTES"),
        ({}, "PRECO_DUPLICADO"),
    ],
)
def test_conflicting_grouped_rows(client, core, changes, code):
    response = upload(client, IMPORT, rows=[row(), row(**changes)])
    assert response.status_code == 422
    assert response.json()["detail"]["codigo"] == code
    assert not core.requests


def test_resource_and_row_limits(client, core, monkeypatch):
    monkeypatch.setattr(settings, "MAX_IMPORT_RESOURCES", 1)
    response = upload(client, IMPORT, rows=[row(), row(email="bia@example.com")])
    assert response.json()["detail"]["codigo"] == "LIMITE_RECURSOS"
    monkeypatch.setattr(settings, "MAX_IMPORT_RESOURCES", 100)
    monkeypatch.setattr(settings, "MAX_IMPORT_ROWS", 1)
    response = upload(client, IMPORT, rows=[row(), row(unidade="HORA")])
    assert response.json()["detail"]["codigo"] == "LIMITE_REGISTROS"
    assert not core.requests


def test_maximum_fifty_prices_per_resource(client, core):
    rows = [row(idioma_destino_id=str(UUID(int=i))) for i in range(10, 61)]
    response = upload(client, IMPORT, rows=rows)
    assert response.status_code == 422
    assert response.json()["detail"]["codigo"] == "LIMITE_PRECOS"
    assert not core.requests


def test_existing_resource_is_reported_without_overwriting(client, core):
    core.handler = lambda request: httpx.Response(
        409, json={"message": "E-mail já cadastrado"}
    )
    response = upload(client, IMPORT)
    assert response.status_code == 200
    assert response.json()["status"] == "CONCLUIDA_COM_CONFLITOS"
    assert response.json()["conflitos"] == 1
    assert response.json()["importados"] == 0
    assert len(core.requests) == 1  # No PUT, PATCH, DELETE or automatic retry.


def test_reupload_does_not_update_and_core_owns_uniqueness(client, core):
    seen = set()

    def create(request):
        email = json.loads(request.content)["email"]
        if email in seen:
            return httpx.Response(409, json={"message": "E-mail já cadastrado"})
        seen.add(email)
        return httpx.Response(201, json={"recurso": {"id": str(UUID(int=123))}})

    core.handler = create
    assert upload(client, IMPORT).json()["importados"] == 1
    assert upload(client, IMPORT).json()["conflitos"] == 1
    assert len(seen) == 1


def test_core_business_rejection_does_not_hide_other_results(client, core):
    def create(request):
        if len(core.requests) == 1:
            return httpx.Response(400, json={"message": "Idioma não encontrado"})
        return httpx.Response(201, json={"recurso": {"id": str(UUID(int=123))}})

    core.handler = create
    response = upload(client, IMPORT, rows=[row(), row(email="bia@example.com")])
    assert response.status_code == 200
    assert response.json()["status"] == "PARCIAL"
    assert response.json()["rejeitados"] == 1
    assert response.json()["importados"] == 1


def test_missing_token_prevents_writes(client, core):
    response = upload(client, IMPORT, token=None)
    assert response.status_code == 401
    assert response.headers["WWW-Authenticate"] == "Bearer"
    assert not core.requests


@pytest.mark.parametrize("status", [401, 403])
def test_core_authorization_failure_stops_batch(client, core, status):
    core.handler = lambda request: httpx.Response(status)
    response = upload(client, IMPORT, rows=[row(), row(email="bia@example.com")])
    assert response.status_code == status
    assert response.json()["relatorio"]["nao_processados"] == 1
    assert len(core.requests) == 1


@pytest.mark.parametrize(
    "error,status,outcome",
    [
        (httpx.ConnectError, 503, "FALHA_CONEXAO"),
        (httpx.ConnectTimeout, 503, "FALHA_CONEXAO"),
        (httpx.ReadTimeout, 504, "INDETERMINADO"),
        (httpx.WriteTimeout, 504, "INDETERMINADO"),
        (httpx.ReadError, 502, "INDETERMINADO"),
    ],
)
def test_network_error_stops_without_retry(client, core, error, status, outcome):
    def fail(request):
        raise error("falha", request=request)

    core.handler = fail
    response = upload(client, IMPORT, rows=[row(), row(email="bia@example.com")])
    assert response.status_code == status
    report = response.json()["relatorio"]
    assert report["resultados"][0]["status"] == outcome
    assert report["nao_processados"] == 1
    assert len(core.requests) == 1


@pytest.mark.parametrize(
    "status,body",
    [
        (201, {}),
        (201, {"recurso": {"id": 123}}),
        (201, {"recurso": {"id": "invalido"}}),
        (500, {"trace": "segredo"}),
        (302, {}),
        (429, {}),
    ],
)
def test_unexpected_core_response_is_indeterminate(client, core, status, body):
    core.handler = lambda request: httpx.Response(status, json=body)
    response = upload(client, IMPORT, rows=[row(), row(email="bia@example.com")])
    assert response.status_code == 502
    report = response.json()["relatorio"]
    assert report["indeterminados"] == 1
    assert report["nao_processados"] == 1
    assert "segredo" not in response.text
    assert len(core.requests) == 1


def test_progress_is_preserved_when_later_call_times_out(client, core):
    def create(request):
        if len(core.requests) == 2:
            raise httpx.ReadTimeout("resposta perdida", request=request)
        return httpx.Response(201, json={"recurso": {"id": str(UUID(int=123))}})

    core.handler = create
    response = upload(
        client,
        IMPORT,
        rows=[row(), row(email="bia@example.com"), row(email="leo@example.com")],
    )
    assert response.status_code == 504
    report = response.json()["relatorio"]
    assert report["status"] == "INDETERMINADA"
    assert report["importados"] == 1
    assert report["indeterminados"] == 1
    assert report["nao_processados"] == 1
    assert len(core.requests) == 2


def test_file_limit_applies_to_resource_routes(client, core, monkeypatch):
    monkeypatch.setattr(settings, "MAX_UPLOAD_BYTES", 10)
    for route in (VALIDATE, IMPORT):
        response = upload(client, route)
        assert response.status_code == 413
    assert not core.requests
