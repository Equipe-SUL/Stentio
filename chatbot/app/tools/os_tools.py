from sqlalchemy.orm import Session
from sqlalchemy import func
from db.models import OrdemServico, Orcamento, Cliente
from schemas.chatbot import Atalho
from typing import Tuple, List
import uuid

def consultar_os_no_banco(db: Session, termo_busca: str) -> Tuple[str, List[Atalho]]:
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


def consultar_orcamentos_no_banco(db: Session, termo_busca: str, perfil: str) -> Tuple[str, List[Atalho]]:
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
        
        # RN.1 - Tradutor não pode ver dados financeiros
        info_valor = f" | Valor: R$ {orc_obj.valor_total}" if perfil != "TRADUTOR" else ""
        
        linhas.append(f"- Orçamento {orc_id_str[:8]} ({nome_cli}): Status '{orc_obj.status}'{info_valor}")
        atalhos.append(Atalho(tipo="ORCAMENTO", id=orc_id_str, titulo=f"Abrir Orçamento ({nome_cli})"))

    return "Orçamentos encontrados:\n" + "\n".join(linhas), atalhos


def consultar_faturamento(db: Session, perfil: str) -> Tuple[str, List[Atalho]]:
    """Calcula o faturamento somando orçamentos aprovados (bloqueado para Tradutor - Cenário 2 BDD)."""
    if perfil == "TRADUTOR":
        return "BLOQUEIO_RBAC: O perfil de Tradutor não possui permissão para visualizar dados financeiros ou de faturamento consolidado.", []

    total = db.query(func.sum(Orcamento.valor_total)).scalar() or 0.0
    return f"O valor total consolidado em orçamentos no sistema é de R$ {total:,.2f}.", []