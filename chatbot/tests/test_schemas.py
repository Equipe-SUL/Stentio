from datetime import datetime, timezone

import pytest
from pydantic import ValidationError
from schemas.chatbot import (
    Atalho,
    HistoricoResponse,
    MensagemHistorico,
    MensagemRequest,
    MensagemResponse,
)


@pytest.mark.parametrize("tipo", ["OS", "ORCAMENTO", "CLIENTE"])
def test_atalho_accepts_supported_types(tipo):
    # Arrange
    data = {"tipo": tipo, "id": "registro-1", "titulo": "Abrir registro"}

    # Act
    atalho = Atalho.model_validate(data)

    # Assert
    assert atalho.tipo == tipo
    assert atalho.id == "registro-1"


def test_atalho_rejects_unsupported_type():
    # Arrange
    data = {"tipo": "FATURA", "id": "registro-1", "titulo": "Abrir fatura"}

    # Act / Assert
    with pytest.raises(ValidationError):
        Atalho.model_validate(data)


def test_mensagem_request_requires_message_and_allows_optional_conversation():
    # Arrange / Act
    request = MensagemRequest.model_validate({"mensagem": "Olá"})

    # Assert
    assert request.mensagem == "Olá"
    assert request.conversaId is None


def test_message_responses_default_to_no_shortcuts():
    # Arrange
    response = MensagemResponse(
        resposta="Olá",
        conversaId="conversa-1",
        mensagemId="mensagem-1",
    )

    # Assert
    assert response.atalhos == []


def test_historico_response_validates_author_and_shortcuts():
    # Arrange
    created_at = datetime.now(timezone.utc)
    data = {
        "conversaId": "conversa-1",
        "mensagens": [
            {
                "id": "mensagem-1",
                "autor": "IA",
                "conteudo": "Resposta",
                "criadaEm": created_at,
                "atalhos": [{"tipo": "OS", "id": "os-1", "titulo": "Abrir OS"}],
            }
        ],
    }

    # Act
    result = HistoricoResponse.model_validate(data)

    # Assert
    assert isinstance(result.mensagens[0], MensagemHistorico)
    assert result.mensagens[0].criadaEm == created_at
    assert result.mensagens[0].atalhos[0].tipo == "OS"
