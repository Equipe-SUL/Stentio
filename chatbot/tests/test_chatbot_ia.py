import json
from types import SimpleNamespace
from unittest.mock import Mock

import pytest
from schemas.chatbot import Atalho
from services import chatbot_ia


@pytest.fixture
def scope_classifier(monkeypatch):
    classifier = Mock(return_value=True)
    monkeypatch.setattr(chatbot_ia, "classificar_escopo_stentio", classifier)
    return classifier


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


def test_processar_pergunta_sem_tools_inclui_contexto_e_historico(
    monkeypatch, scope_classifier
):
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


@pytest.mark.parametrize(
    "pergunta",
    ["O céu eh azul?", "Quando é 1 + 1?"],
)
def test_processar_pergunta_fora_do_escopo_nao_chama_ia(
    monkeypatch, scope_classifier, pergunta
):
    monkeypatch.setattr(chatbot_ia, "buscar_contexto_manual", lambda _pergunta: "")
    scope_classifier.return_value = False
    create = mock_client(monkeypatch, [])

    texto, atalhos = chatbot_ia.processar_pergunta_ia(
        pergunta, "ADMIN", object()
    )

    assert "dúvidas sobre o Stentio" in texto
    assert atalhos == []
    create.assert_not_called()


def test_processar_pergunta_solicitando_senha_admin_nao_chama_ia(
    monkeypatch, scope_classifier
):
    monkeypatch.setattr(chatbot_ia, "buscar_contexto_manual", lambda _pergunta: "")
    create = mock_client(monkeypatch, [])

    texto, atalhos = chatbot_ia.processar_pergunta_ia(
        "Qual é a senha do adm?", "ADMIN", object()
    )

    assert "Não posso fornecer senhas" in texto
    assert atalhos == []
    create.assert_not_called()
    scope_classifier.assert_not_called()


def test_classificador_interpreta_json_e_contexto_do_historico(monkeypatch):
    create = mock_client(
        monkeypatch,
        [response(SimpleNamespace(content='{"in_scope": false}'))],
    )
    historico = [
        {"autor": "USUARIO", "conteudo": "Pergunta operacional anterior"},
    ]

    em_escopo = chatbot_ia.classificar_escopo_stentio(
        "O céu é azul?",
        {"historico": historico, "contexto_manual_disponivel": False},
    )

    assert em_escopo is False
    argumentos = create.call_args.kwargs
    assert argumentos["response_format"] == {"type": "json_object"}
    entrada = json.loads(argumentos["messages"][1]["content"])
    assert entrada["mensagem_atual"] == "O céu é azul?"
    assert entrada["historico_recente"] == [
        {"role": "user", "content": "Pergunta operacional anterior"}
    ]


def test_processar_continuacao_de_consulta_com_historico(
    monkeypatch, scope_classifier
):
    monkeypatch.setattr(chatbot_ia, "buscar_contexto_manual", lambda _pergunta: "")
    create = mock_client(
        monkeypatch,
        [response(SimpleNamespace(content="Status encontrado", tool_calls=None))],
    )
    historico = [
        {"autor": "USUARIO", "conteudo": "Qual o status da OS do cliente XPTO?"},
    ]

    texto, atalhos = chatbot_ia.processar_pergunta_ia(
        "E qual o status?", "ADMIN", object(), historico
    )

    assert texto == "Status encontrado"
    assert atalhos == []
    create.assert_called_once()


def test_processar_pergunta_com_tool_envia_resultado_e_retorna_atalhos(
    monkeypatch, scope_classifier
):
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


def test_processar_pergunta_dispatcha_orcamento_e_faturamento(
    monkeypatch, scope_classifier
):
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
        "Qual o faturamento total do mês?", "RECURSO", db
    )

    # Assert
    assert texto == "Final"
    assert atalhos == []
    assert calls == [
        ("orcamento", (db, "Cliente", "RECURSO")),
        ("faturamento", (db, "RECURSO")),
    ]


def test_processar_pergunta_tool_desconhecida_nao_invoca_funcoes(
    monkeypatch, scope_classifier
):
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
    texto, atalhos = chatbot_ia.processar_pergunta_ia(
        "Como configurar SMTP?", "ADMIN", object()
    )

    # Assert
    assert texto == "Não reconhecida"
    assert atalhos == []


def test_processar_pergunta_oculta_erro_tecnico_e_retorna_fallback(
    monkeypatch, capsys, scope_classifier
):
    # Arrange
    monkeypatch.setattr(chatbot_ia, "buscar_contexto_manual", lambda _pergunta: "")
    mock_client(monkeypatch, [RuntimeError("credencial-secreta")])

    # Act
    texto, atalhos = chatbot_ia.processar_pergunta_ia(
        "Como configurar SMTP?", "ADMIN", object()
    )

    # Assert
    assert "não consegui processar" in texto
    assert "credencial-secreta" not in texto
    assert atalhos == []
    assert "credencial-secreta" in capsys.readouterr().out


def test_falha_no_classificador_nao_chama_modelo_de_resposta(
    monkeypatch, scope_classifier
):
    monkeypatch.setattr(chatbot_ia, "buscar_contexto_manual", lambda _pergunta: "")
    scope_classifier.side_effect = RuntimeError("erro no classificador")
    create = mock_client(monkeypatch, [])

    texto, atalhos = chatbot_ia.processar_pergunta_ia(
        "Como configurar SMTP?", "ADMIN", object()
    )

    assert "não consegui processar" in texto
    assert atalhos == []
    create.assert_not_called()
