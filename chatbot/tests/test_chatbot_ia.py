import json
from types import SimpleNamespace
from unittest.mock import Mock

from schemas.chatbot import Atalho
from services import chatbot_ia


def response(message):
    return SimpleNamespace(choices=[SimpleNamespace(message=message)])


def tool_call(name, arguments, call_id="call-1"):
    return SimpleNamespace(
        id=call_id,
        function=SimpleNamespace(name=name, arguments=json.dumps(arguments)),
    )


def mock_client(monkeypatch, responses):
    create = Mock(side_effect=responses)
    monkeypatch.setattr(
        chatbot_ia,
        "groq_client",
        SimpleNamespace(
            chat=SimpleNamespace(completions=SimpleNamespace(create=create))
        ),
    )
    return create


def test_processar_pergunta_sem_tools_inclui_contexto_e_historico(monkeypatch):
    # Arrange
    monkeypatch.setattr(
        chatbot_ia, "buscar_contexto_manual", lambda _pergunta: "Trecho do manual"
    )
    create = mock_client(
        monkeypatch,
        [response(SimpleNamespace(content="Resposta direta", tool_calls=None))],
    )
    historico = [
        {"autor": "USUARIO", "conteudo": "Pergunta anterior"},
        {"autor": "IA", "conteudo": "Resposta anterior"},
    ]

    # Act
    texto, atalhos = chatbot_ia.processar_pergunta_ia(
        "Nova pergunta", "ADMIN", object(), historico
    )

    # Assert
    mensagens = create.call_args.kwargs["messages"]
    assert texto == "Resposta direta"
    assert atalhos == []
    assert mensagens[0]["role"] == "system"
    assert "Perfil do usuário atual: ADMIN" in mensagens[0]["content"]
    assert "Trecho do manual" in mensagens[0]["content"]
    assert [mensagem["role"] for mensagem in mensagens[1:]] == [
        "user",
        "assistant",
        "user",
    ]
    assert create.call_count == 1


def test_processar_pergunta_com_tool_envia_resultado_e_retorna_atalhos(monkeypatch):
    # Arrange
    monkeypatch.setattr(chatbot_ia, "buscar_contexto_manual", lambda _pergunta: "")
    atalho = Atalho(tipo="OS", id="os-1", titulo="Abrir OS")
    first_message = SimpleNamespace(
        content=None,
        tool_calls=[tool_call("consultar_os_no_banco", {"termo_busca": "Stentio"})],
    )
    create = mock_client(
        monkeypatch,
        [
            response(first_message),
            response(SimpleNamespace(content="Status encontrado", tool_calls=None)),
        ],
    )
    lookup = Mock(return_value=("OS em andamento", [atalho]))
    monkeypatch.setattr(chatbot_ia, "consultar_os_no_banco", lookup)
    db = object()

    # Act
    texto, atalhos = chatbot_ia.processar_pergunta_ia(
        "Status da OS?", "GESTOR_PROJETO", db
    )

    # Assert
    lookup.assert_called_once_with(db, "Stentio")
    assert texto == "Status encontrado"
    assert atalhos == [atalho]
    assert create.call_count == 2
    tool_messages = create.call_args_list[1].kwargs["messages"]
    assert tool_messages[-1]["role"] == "tool"
    assert tool_messages[-1]["content"] == "OS em andamento"


def test_processar_pergunta_dispatcha_orcamento_e_faturamento(monkeypatch):
    # Arrange
    monkeypatch.setattr(chatbot_ia, "buscar_contexto_manual", lambda _pergunta: "")
    calls = []

    def capture(name, result):
        def run(*args):
            calls.append((name, args))
            return result, []

        return run

    monkeypatch.setattr(
        chatbot_ia,
        "consultar_orcamentos_no_banco",
        capture("orcamento", "orçamento protegido"),
    )
    monkeypatch.setattr(
        chatbot_ia,
        "consultar_faturamento",
        capture("faturamento", "bloqueado"),
    )
    message = SimpleNamespace(
        content=None,
        tool_calls=[
            tool_call(
                "consultar_orcamentos_no_banco",
                {"termo_busca": "Cliente"},
                "call-1",
            ),
            tool_call("consultar_faturamento", {}, "call-2"),
        ],
    )
    mock_client(
        monkeypatch,
        [
            response(message),
            response(SimpleNamespace(content="Final", tool_calls=None)),
        ],
    )
    db = object()

    # Act
    texto, atalhos = chatbot_ia.processar_pergunta_ia(
        "Dados financeiros", "RECURSO", db
    )

    # Assert
    assert texto == "Final"
    assert atalhos == []
    assert calls == [
        ("orcamento", (db, "Cliente", "RECURSO")),
        ("faturamento", (db, "RECURSO")),
    ]


def test_processar_pergunta_tool_desconhecida_nao_invoca_funcoes(monkeypatch):
    # Arrange
    monkeypatch.setattr(chatbot_ia, "buscar_contexto_manual", lambda _pergunta: "")
    mock_client(
        monkeypatch,
        [
            response(
                SimpleNamespace(
                    content=None,
                    tool_calls=[tool_call("nao_existe", {})],
                )
            ),
            response(SimpleNamespace(content="Não reconhecida", tool_calls=None)),
        ],
    )

    # Act
    texto, atalhos = chatbot_ia.processar_pergunta_ia("Pergunta", "ADMIN", object())

    # Assert
    assert texto == "Não reconhecida"
    assert atalhos == []


def test_processar_pergunta_oculta_erro_tecnico_e_retorna_fallback(monkeypatch, capsys):
    # Arrange
    monkeypatch.setattr(chatbot_ia, "buscar_contexto_manual", lambda _pergunta: "")
    mock_client(monkeypatch, [RuntimeError("credencial-secreta")])

    # Act
    texto, atalhos = chatbot_ia.processar_pergunta_ia("Pergunta", "ADMIN", object())

    # Assert
    assert "não consegui processar" in texto
    assert "credencial-secreta" not in texto
    assert atalhos == []
    assert "credencial-secreta" in capsys.readouterr().out
