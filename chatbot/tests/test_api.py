from datetime import datetime, timezone
from unittest.mock import Mock
from uuid import UUID

from db.session import get_db
from fastapi import FastAPI
from fastapi.testclient import TestClient
from schemas.chatbot import Atalho


class FakeCursor:
    def __init__(self, documents):
        self.documents = documents
        self.sort_args = None
        self.limit_value = None

    def sort(self, *args):
        self.sort_args = args
        return self

    def limit(self, value):
        self.limit_value = value
        return self

    def __iter__(self):
        return iter(self.documents)


def create_client(api_module, usuario=None):
    app = FastAPI()
    app.include_router(api_module.router)
    app.dependency_overrides[api_module.obter_usuario_logado] = lambda: (
        usuario
        or {
            "usuario_id": "usuario-1",
            "perfil": "ADMIN",
        }
    )
    app.dependency_overrides[get_db] = lambda: object()
    return TestClient(app)


def test_post_mensagem_persiste_pergunta_resposta_e_atalhos(monkeypatch, api_module):
    # Arrange
    chatbot_api = api_module
    collection = Mock()
    history = [
        {"autor": "USUARIO", "conteudo": "Pergunta anterior"},
        {"autor": "IA", "conteudo": "Resposta anterior"},
    ]
    cursor = FakeCursor(history)
    collection.find.return_value = cursor
    monkeypatch.setattr(chatbot_api, "mensagens_collection", collection)
    shortcut = Atalho(tipo="OS", id="os-1", titulo="Abrir OS")
    process = Mock(return_value=("OS em andamento", [shortcut]))
    monkeypatch.setattr(chatbot_api, "processar_pergunta_ia", process)
    client = create_client(chatbot_api)

    # Act
    response = client.post(
        "/api/v1/ia/chatbot/mensagens",
        json={"mensagem": "Status da OS?", "conversaId": "conversa-1"},
    )

    # Assert
    assert response.status_code == 200
    body = response.json()
    assert body["resposta"] == "OS em andamento"
    assert body["conversaId"] == "conversa-1"
    assert body["atalhos"][0]["id"] == "os-1"
    assert cursor.limit_value == 10
    assert collection.find.call_args.args[0] == {
        "conversa_id": "conversa-1",
        "usuario_id": "usuario-1",
    }
    assert cursor.sort_args == ("criada_em", 1)
    assert collection.insert_one.call_count == 2
    question, answer = [call.args[0] for call in collection.insert_one.call_args_list]
    assert question["autor"] == "USUARIO"
    assert question["conteudo"] == "Status da OS?"
    assert answer["autor"] == "IA"
    assert answer["atalhos"] == [shortcut.model_dump()]
    process.assert_called_once()
    assert process.call_args.kwargs["historico_conversa"] == history


def test_post_mensagem_cria_conversa_quando_nao_informada(monkeypatch, api_module):
    # Arrange
    chatbot_api = api_module
    collection = Mock()
    collection.find.return_value = FakeCursor([])
    monkeypatch.setattr(chatbot_api, "mensagens_collection", collection)
    monkeypatch.setattr(
        chatbot_api, "processar_pergunta_ia", Mock(return_value=("Olá", []))
    )
    client = create_client(chatbot_api)

    # Act
    response = client.post("/api/v1/ia/chatbot/mensagens", json={"mensagem": "Olá"})

    # Assert
    assert response.status_code == 200
    assert UUID(response.json()["conversaId"])
    assert (
        collection.insert_one.call_args_list[0].args[0]["conversa_id"]
        == response.json()["conversaId"]
    )


def test_get_historico_filtra_conversa_e_formata_mensagens(monkeypatch, api_module):
    # Arrange
    chatbot_api = api_module
    created_at = datetime.now(timezone.utc)
    collection = Mock()
    collection.find.return_value = FakeCursor(
        [
            {
                "_id": "mensagem-1",
                "autor": "IA",
                "conteudo": "Resposta",
                "criada_em": created_at,
                "atalhos": [{"tipo": "ORCAMENTO", "id": "orc-1", "titulo": "Abrir"}],
            }
        ]
    )
    monkeypatch.setattr(chatbot_api, "mensagens_collection", collection)
    client = create_client(chatbot_api)

    # Act
    response = client.get(
        "/api/v1/ia/chatbot/historico", params={"conversaId": "conversa-1"}
    )

    # Assert
    assert response.status_code == 200
    assert response.json()["conversaId"] == "conversa-1"
    assert response.json()["mensagens"][0]["id"] == "mensagem-1"
    assert response.json()["mensagens"][0]["atalhos"][0]["tipo"] == "ORCAMENTO"
    collection.find_one.assert_not_called()
    assert collection.find.call_args.args[0] == {
        "conversa_id": "conversa-1",
        "usuario_id": "usuario-1",
    }


def test_get_historico_usa_conversa_mais_recente_do_usuario(monkeypatch, api_module):
    # Arrange
    chatbot_api = api_module
    collection = Mock()
    collection.find_one.return_value = {"conversa_id": "mais-recente"}
    collection.find.return_value = FakeCursor([])
    monkeypatch.setattr(chatbot_api, "mensagens_collection", collection)
    client = create_client(chatbot_api)

    # Act
    response = client.get("/api/v1/ia/chatbot/historico")

    # Assert
    assert response.status_code == 200
    assert response.json() == {"conversaId": "mais-recente", "mensagens": []}
    assert collection.find_one.call_args.args[0] == {"usuario_id": "usuario-1"}
    assert collection.find.call_args.args[0] == {
        "conversa_id": "mais-recente",
        "usuario_id": "usuario-1",
    }


def test_get_historico_sem_mensagens_gera_conversa_vazia(monkeypatch, api_module):
    # Arrange
    chatbot_api = api_module
    collection = Mock()
    collection.find_one.return_value = None
    collection.find.return_value = FakeCursor([])
    monkeypatch.setattr(chatbot_api, "mensagens_collection", collection)
    client = create_client(chatbot_api)

    # Act
    response = client.get("/api/v1/ia/chatbot/historico")

    # Assert
    assert response.status_code == 200
    assert UUID(response.json()["conversaId"])
    assert response.json()["mensagens"] == []
