import uuid
from types import SimpleNamespace
from unittest.mock import Mock

import jwt
import pytest
from core import security
from db.models import Usuario
from fastapi import HTTPException
from starlette.requests import Request


def request_with_cookies(cookie_header=b""):
    return Request(
        {
            "type": "http",
            "http_version": "1.1",
            "method": "GET",
            "scheme": "http",
            "path": "/",
            "raw_path": b"/",
            "query_string": b"",
            "headers": [(b"cookie", cookie_header)] if cookie_header else [],
            "server": ("testserver", 80),
            "client": ("testclient", 50000),
        }
    )


def mock_decode(monkeypatch, payload):
    monkeypatch.setattr(security.jwt, "decode", lambda *_args, **_kwargs: payload)


def test_obter_usuario_rejeita_token_ausente():
    # Arrange / Act
    with pytest.raises(HTTPException) as error:
        security.obter_usuario_logado(request_with_cookies(), None, Mock())

    # Assert
    assert error.value.status_code == 401
    assert "Token ausente" in error.value.detail


@pytest.mark.parametrize("perfil", ["ADMIN", "TRADUTOR"])
def test_obter_usuario_permite_perfis_de_teste_do_swagger(perfil):
    # Arrange / Act
    usuario = security.obter_usuario_logado(
        request_with_cookies(), perfil.lower(), Mock()
    )

    # Assert
    assert usuario == {
        "usuario_id": "usuario-teste-swagger",
        "perfil": perfil,
    }


def test_obter_usuario_aceita_token_no_cookie(monkeypatch):
    # Arrange
    mock_decode(monkeypatch, {"sub": "usuario-1", "role": "ROLE_ADMIN"})
    db = Mock()
    db.query.return_value.filter.return_value.first.return_value = None

    # Act
    usuario = security.obter_usuario_logado(
        request_with_cookies(b"stentio_token=cookie-token"), None, db
    )

    # Assert
    assert usuario["usuario_id"] == "usuario-1"
    assert usuario["perfil"] == "ADMIN"


def test_obter_usuario_consulta_postgres_por_uuid_e_normaliza_role(monkeypatch):
    # Arrange
    usuario_id = uuid.uuid4()
    mock_decode(monkeypatch, {"sub": str(usuario_id)})
    usuario_db = SimpleNamespace(
        id=usuario_id, ativo=True, role="ROLE_GESTOR_PROJETO", nome="Ana"
    )
    query = Mock()
    query.filter.return_value.first.return_value = usuario_db
    db = Mock()
    db.query.return_value = query

    # Act
    usuario = security.obter_usuario_logado(request_with_cookies(), "Bearer token", db)

    # Assert
    db.query.assert_called_once_with(Usuario)
    assert usuario == {
        "usuario_id": str(usuario_id),
        "perfil": "GESTOR_PROJETO",
        "nome": "Ana",
    }


def test_obter_usuario_consulta_por_email_quando_sub_nao_e_uuid(monkeypatch):
    # Arrange
    mock_decode(monkeypatch, {"email": "ana@example.com"})
    usuario_db = SimpleNamespace(
        id=uuid.uuid4(), ativo=True, role="ATENDENTE", nome="Ana"
    )
    query = Mock()
    query.filter.return_value.first.return_value = usuario_db
    db = Mock()
    db.query.return_value = query

    # Act
    usuario = security.obter_usuario_logado(request_with_cookies(), "token", db)

    # Assert
    assert usuario["perfil"] == "ATENDENTE"
    assert query.filter.call_count == 1
    assert (
        query.filter.call_args.args[0].compile().params["email_1"] == "ana@example.com"
    )


def test_obter_usuario_sem_role_no_banco_usa_tradutor(monkeypatch):
    # Arrange
    usuario_id = uuid.uuid4()
    mock_decode(monkeypatch, {"sub": str(usuario_id)})
    db = Mock()
    db.query.return_value.filter.return_value.first.return_value = SimpleNamespace(
        id=usuario_id, ativo=True, role=None, nome="Ana"
    )

    # Act
    usuario = security.obter_usuario_logado(request_with_cookies(), "token", db)

    # Assert
    assert usuario["perfil"] == "TRADUTOR"


def test_obter_usuario_faz_fallback_para_role_do_jwt(monkeypatch):
    # Arrange
    mock_decode(monkeypatch, {"sub": "auth-id", "perfil": "ROLE_FINANCEIRO"})
    db = Mock()
    db.query.return_value.filter.return_value.first.return_value = None

    # Act
    usuario = security.obter_usuario_logado(request_with_cookies(), "token", db)

    # Assert
    assert usuario == {
        "usuario_id": "auth-id",
        "perfil": "FINANCEIRO",
        "nome": "auth-id",
    }


def test_obter_usuario_sem_role_no_jwt_usa_tradutor(monkeypatch):
    # Arrange
    mock_decode(monkeypatch, {"sub": "auth-id"})
    db = Mock()
    db.query.return_value.filter.return_value.first.return_value = None

    # Act
    usuario = security.obter_usuario_logado(request_with_cookies(), "token", db)

    # Assert
    assert usuario["perfil"] == "TRADUTOR"


def test_obter_usuario_inativo_e_rejeitado(monkeypatch):
    # Arrange
    usuario_id = uuid.uuid4()
    mock_decode(monkeypatch, {"sub": str(usuario_id)})
    db = Mock()
    db.query.return_value.filter.return_value.first.return_value = SimpleNamespace(
        ativo=False
    )

    # Act / Assert
    with pytest.raises(HTTPException) as error:
        security.obter_usuario_logado(request_with_cookies(), "token", db)
    assert error.value.status_code == 401
    assert error.value.detail == "Usuário inativo."


def test_obter_usuario_sem_identificador_no_jwt_e_rejeitado(monkeypatch):
    # Arrange
    mock_decode(monkeypatch, {})

    # Act / Assert
    with pytest.raises(HTTPException) as error:
        security.obter_usuario_logado(request_with_cookies(), "token", Mock())
    assert error.value.status_code == 401
    assert "identificador ausente" in error.value.detail


@pytest.mark.parametrize(
    "exception,detail",
    [
        (jwt.ExpiredSignatureError("expirado"), "Sessão expirada"),
        (jwt.InvalidTokenError("inválido"), "Token de autenticação inválido"),
    ],
)
def test_obter_usuario_rejeita_tokens_invalidos(monkeypatch, exception, detail):
    # Arrange
    monkeypatch.setattr(security.jwt, "decode", Mock(side_effect=exception))

    # Act / Assert
    with pytest.raises(HTTPException) as error:
        security.obter_usuario_logado(request_with_cookies(), "token", Mock())
    assert error.value.status_code == 401
    assert detail in error.value.detail
