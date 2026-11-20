import uuid

from db.models import Cliente, Orcamento, OrdemServico
from schemas.chatbot import Atalho
from sqlalchemy import func
from sqlalchemy.orm import Session

PERFIS_SEM_ACESSO_FINANCEIRO = {"TRADUTOR", "RECURSO"}


def consultar_os_no_banco(db: Session, termo_busca: str) -> tuple[str, list[Atalho]]:
    """Busca OS por UUID parcial/total ou pelo nome da empresa/cliente."""
    atalhos = []

    # Faz o JOIN: OrdemServico -> Orcamento -> Cliente
    query = (
        db.query(OrdemServico, Orcamento, Cliente)
        .outerjoin(Orcamento, OrdemServico.orcamento_id == Orcamento.id)
        .outerjoin(Cliente, Orcamento.cliente_id == Cliente.id)
    )

    # Verifica se o usuário digitou um UUID válido ou o nome do cliente
    try:
        uuid_obj = uuid.UUID(termo_busca.strip())
        resultados = query.filter(OrdemServico.id == uuid_obj).all()
    except ValueError:
        resultados = query.filter(
            (Cliente.nome_empresa.ilike(f"%{termo_busca}%")) |
            (Cliente.nome_representante.ilike(f"%{termo_busca}%"))
        ).all()

    if not resultados:
        return f"Nenhuma Ordem de Serviço encontrada para '{termo_busca}'.", []

    linhas_resposta = []
    for os_obj, orc_obj, cli_obj in resultados:
        os_id_str = str(os_obj.id)
        id_curto = os_id_str[:8]
        nome_cli = cli_obj.nome_empresa if cli_obj else "Cliente não vinculado"

        linhas_resposta.append(
            f"- OS {id_curto} (Cliente: {nome_cli}): Status '{os_obj.status}'"
        )
        atalhos.append(
            Atalho(tipo="OS", id=os_id_str, titulo=f"Abrir OS ({nome_cli})")
        )

    return "Ordens de Serviço encontradas:\n" + "\n".join(linhas_resposta), atalhos


def consultar_orcamentos_no_banco(db: Session, termo_busca: str, perfil: str) -> tuple[str, list[Atalho]]:
    """Busca orçamentos pelo nome do cliente ou ID, ocultando valores para Tradutores."""
    atalhos = []
    query = db.query(Orcamento, Cliente).outerjoin(Cliente, Orcamento.cliente_id == Cliente.id)

    try:
        uuid_obj = uuid.UUID(termo_busca.strip())
        resultados = query.filter(Orcamento.id == uuid_obj).all()
    except ValueError:
        resultados = query.filter(Cliente.nome_empresa.ilike(f"%{termo_busca}%")).all()

    if not resultados:
        return f"Nenhum orçamento encontrado para '{termo_busca}'.", []

    linhas = []
    for orc_obj, cli_obj in resultados:
        orc_id_str = str(orc_obj.id)
        nome_cli = cli_obj.nome_empresa if cli_obj else "Sem cliente"

        # RN.1 - Tradutor e recurso não podem ver dados financeiros
        pode_ver_valor = perfil.strip().upper() not in PERFIS_SEM_ACESSO_FINANCEIRO
        info_valor = f" | Valor: R$ {orc_obj.valor_total}" if pode_ver_valor else ""

        linhas.append(f"- Orçamento {orc_id_str[:8]} ({nome_cli}): Status '{orc_obj.status}'{info_valor}")
        atalhos.append(Atalho(tipo="ORCAMENTO", id=orc_id_str, titulo=f"Abrir Orçamento ({nome_cli})"))

    return "Orçamentos encontrados:\n" + "\n".join(linhas), atalhos


def consultar_faturamento(db: Session, perfil: str) -> tuple[str, list[Atalho]]:
    """Calcula o faturamento somando orçamentos aprovados (bloqueado para Tradutores/Recursos)."""
    if perfil.strip().upper() in PERFIS_SEM_ACESSO_FINANCEIRO:
        return (
            "BLOQUEIO_RBAC: O perfil operacional não possui permissão para visualizar dados financeiros ou de faturamento consolidado.",
            [],
        )

    total = (
        db.query(func.sum(Orcamento.valor_total))
        .filter(Orcamento.status == "APROVADO")
        .scalar()
        or 0.0
    )
    return (
        f"O valor total consolidado em orçamentos aprovados é de R$ {total:,.2f}.",
        [],
    )
