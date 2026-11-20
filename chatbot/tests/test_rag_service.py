from services import rag_service


def test_buscar_contexto_prioriza_palavras_no_titulo(monkeypatch):
    # Arrange
    manual = """# Manual
Introdução

## Configuração geral
O SMTP pode ser configurado nesta área.

## SMTP
Configure o servidor e valide a conexão.

## Outros tópicos
Conteúdo sem relação.
"""
    monkeypatch.setattr(rag_service, "_carregar_manual", lambda: manual)

    # Act
    contexto = rag_service.buscar_contexto_manual("Como configuro SMTP?", top_k=2)

    # Assert
    assert contexto.startswith("## SMTP")
    assert "## Configuração geral" in contexto
    assert "## Outros tópicos" not in contexto


def test_buscar_contexto_retorna_vazio_sem_manual_ou_tokens(monkeypatch):
    # Arrange
    monkeypatch.setattr(rag_service, "_carregar_manual", lambda: "")

    # Act
    sem_manual = rag_service.buscar_contexto_manual("SMTP")
    monkeypatch.setattr(rag_service, "_carregar_manual", lambda: "## Manual\nConteúdo.")
    sem_tokens = rag_service.buscar_contexto_manual("e um")

    # Assert
    assert sem_manual == ""
    assert sem_tokens == ""


def test_buscar_contexto_aplica_limite_de_resultados(monkeypatch):
    # Arrange
    manual = """## SMTP
Configuração SMTP e servidor de e-mail.

## SMTP erro
Correção de erro SMTP no servidor.

## SMTP Gmail
Configuração SMTP com Gmail.
"""
    monkeypatch.setattr(rag_service, "_carregar_manual", lambda: manual)

    # Act
    contexto = rag_service.buscar_contexto_manual("SMTP", top_k=1)

    # Assert
    assert contexto.count("## ") == 1


def test_carregar_manual_localiza_documentacao_existente():
    # Arrange / Act
    conteudo = rag_service._carregar_manual()

    # Assert
    assert "Manual Operacional do Sistema Stentio" in conteudo
