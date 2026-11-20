from types import SimpleNamespace
from unittest.mock import Mock
from uuid import uuid4

import pytest
from tools.os_tools import (
    consultar_faturamento,
    consultar_orcamentos_no_banco,
    consultar_os_no_banco,
)


class FakeQuery:
    def __init__(self, rows=None, scalar=None):
        self.rows = rows or []
        self.scalar_result = scalar
        self.filters = []

    def outerjoin(self, *_args):
        return self

    def filter(self, expression):
        self.filters.append(expression)
        return self

    def all(self):
        return self.rows

    def scalar(self):
        return self.scalar_result


def make_db(query):
    db = Mock()
    db.query.return_value = query
    return db


def setup_os_query(rows):
    query = FakeQuery(rows=rows)
    db = make_db(query)
    os_id = uuid4()
    cliente = SimpleNamespace(nome_empresa="Stentio", nome_representante="Ana")
    orcamento = SimpleNamespace(id=uuid4(), cliente_id=uuid4())
    ordem = SimpleNamespace(id=os_id, status="EM_ANDAMENTO")
    query.rows = [(ordem, orcamento, cliente)]
    return db, query, ordem, cliente


def test_consultar_os_por_uuid_retorna_status_e_atalho():
    # Arrange
    db, query, ordem, cliente = setup_os_query([])

    # Act
    texto, atalhos = consultar_os_no_banco(db, str(ordem.id))

    # Assert
    assert "EM_ANDAMENTO" in texto
    assert cliente.nome_empresa in texto
    assert len(atalhos) == 1
    assert atalhos[0].tipo == "OS"
    assert atalhos[0].id == str(ordem.id)
    assert query.filters[0].compile().params["id_1"] == ordem.id


def test_consultar_os_por_nome_usa_nome_da_empresa_e_representante():
    # Arrange
    db, query, _ordem, _cliente = setup_os_query([])

    # Act
    texto, _atalhos = consultar_os_no_banco(db, "Stentio")

    # Assert
    assert query.filters[0].compile().params == {
        "nome_empresa_1": "%Stentio%",
        "nome_representante_1": "%Stentio%",
    }
    assert "Ordens de Serviço encontradas" in texto


def test_consultar_os_sem_resultado_nao_retorna_atalhos():
    # Arrange
    db = make_db(FakeQuery())

    # Act
    texto, atalhos = consultar_os_no_banco(db, "inexistente")

    # Assert
    assert "Nenhuma Ordem de Serviço" in texto
    assert "inexistente" in texto
    assert atalhos == []


@pytest.mark.parametrize("perfil", ["TRADUTOR", "RECURSO", "tradutor", " recurso "])
def test_consultar_orcamento_oculta_valor_de_perfis_operacionais(perfil):
    # Arrange
    orcamento = SimpleNamespace(id=uuid4(), valor_total="1234.56", status="APROVADO")
    cliente = SimpleNamespace(nome_empresa="Stentio")
    db = make_db(FakeQuery(rows=[(orcamento, cliente)]))

    # Act
    texto, atalhos = consultar_orcamentos_no_banco(db, "Stentio", perfil)

    # Assert
    assert "1234.56" not in texto
    assert "Valor:" not in texto
    assert len(atalhos) == 1
    assert atalhos[0].tipo == "ORCAMENTO"


def test_consultar_orcamento_exibe_valor_para_admin():
    # Arrange
    orcamento = SimpleNamespace(id=uuid4(), valor_total="1234.56", status="APROVADO")
    cliente = SimpleNamespace(nome_empresa="Stentio")
    query = FakeQuery(rows=[(orcamento, cliente)])
    db = make_db(query)

    # Act
    texto, atalhos = consultar_orcamentos_no_banco(db, "Stentio", "ADMIN")

    # Assert
    assert "Valor: R$ 1234.56" in texto
    assert len(atalhos) == 1
    assert query.filters[0].compile().params["nome_empresa_1"] == "%Stentio%"


def test_consultar_orcamento_por_uuid_e_sem_cliente():
    # Arrange
    orcamento_id = uuid4()
    orcamento = SimpleNamespace(
        id=orcamento_id, valor_total="50.00", status="EM_ELABORACAO"
    )
    query = FakeQuery(rows=[(orcamento, None)])
    db = make_db(query)

    # Act
    texto, atalhos = consultar_orcamentos_no_banco(db, str(orcamento_id), "ADMIN")

    # Assert
    assert query.filters[0].compile().params["id_1"] == orcamento_id
    assert "Sem cliente" in texto
    assert atalhos[0].titulo == "Abrir Orçamento (Sem cliente)"


def test_consultar_orcamento_sem_resultado():
    # Arrange
    db = make_db(FakeQuery())

    # Act
    texto, atalhos = consultar_orcamentos_no_banco(db, "ausente", "ADMIN")

    # Assert
    assert "Nenhum orçamento encontrado" in texto
    assert atalhos == []


@pytest.mark.parametrize("perfil", ["TRADUTOR", "RECURSO", "recurso"])
def test_faturamento_bloqueia_perfis_operacionais_sem_consultar_banco(perfil):
    # Arrange
    db = Mock()

    # Act
    texto, atalhos = consultar_faturamento(db, perfil)

    # Assert
    assert texto.startswith("BLOQUEIO_RBAC:")
    assert atalhos == []
    db.query.assert_not_called()


def test_faturamento_soma_somente_orcamentos_aprovados():
    # Arrange
    query = FakeQuery(scalar=1234.5)
    db = make_db(query)

    # Act
    texto, atalhos = consultar_faturamento(db, "ADMIN")

    # Assert
    assert texto.endswith("R$ 1,234.50.")
    assert "aprovados" in texto
    assert query.filters[0].compile().params["status_1"] == "APROVADO"
    assert atalhos == []


def test_faturamento_sem_orcamentos_aprovados_retorna_zero():
    # Arrange
    db = make_db(FakeQuery(scalar=None))

    # Act
    texto, _atalhos = consultar_faturamento(db, "FINANCEIRO")

    # Assert
    assert texto.endswith("R$ 0.00.")
