import logging
from collections.abc import AsyncIterator
from uuid import UUID

import httpx

from config import settings
from resource_import import ImportPlan

logger = logging.getLogger(__name__)


async def get_core_client() -> AsyncIterator[httpx.AsyncClient]:
    async with httpx.AsyncClient(
        base_url=str(settings.CORE_API_URL).rstrip("/") + "/",
        timeout=settings.CORE_TIMEOUT_SECONDS,
        follow_redirects=False,
    ) as client:
        yield client


def _business_message(response: httpx.Response, fallback: str) -> str:
    try:
        message = response.json().get("message")
    except ValueError, AttributeError:
        return fallback
    return message[:300] if isinstance(message, str) and message else fallback


def _report(filename: str, plan: ImportPlan, results: list[dict]) -> dict:
    counts = {
        name: sum(row["status"] == name for row in results)
        for name in (
            "IMPORTADO",
            "CONFLITO",
            "REJEITADO",
            "INDETERMINADO",
            "NAO_PROCESSADO",
            "FALHA_CONEXAO",
        )
    }
    failed = (
        counts["REJEITADO"]
        + counts["INDETERMINADO"]
        + counts["NAO_PROCESSADO"]
        + counts["FALHA_CONEXAO"]
    )
    status = "CONCLUIDA"
    if counts["INDETERMINADO"]:
        status = "INDETERMINADA"
    elif failed:
        status = "PARCIAL" if counts["IMPORTADO"] else "FALHOU"
    elif counts["CONFLITO"]:
        status = "CONCLUIDA_COM_CONFLITOS"
    return {
        "filename": filename,
        "status": status,
        "total_linhas": plan.total_rows,
        "total_recursos": len(plan.resources),
        "importados": counts["IMPORTADO"],
        "conflitos": counts["CONFLITO"],
        "rejeitados": counts["REJEITADO"],
        "indeterminados": counts["INDETERMINADO"],
        "nao_processados": counts["NAO_PROCESSADO"],
        "falhas_conexao": counts["FALHA_CONEXAO"],
        "resultados": results,
    }


async def persist_resources(
    filename: str,
    plan: ImportPlan,
    client: httpx.AsyncClient,
    authorization: str,
) -> tuple[int, dict]:
    """Each Core POST owns its transaction. Never retry a write automatically."""
    results = []
    fatal = None
    http_status = 200
    for resource in plan.resources:
        result = {"email": resource.payload.email, "linhas": resource.linhas}
        if fatal:
            results.append(
                {
                    **result,
                    "status": "NAO_PROCESSADO",
                    "mensagem": "Importação interrompida após falha no Core.",
                }
            )
            continue
        try:
            response = await client.post(
                "api/v1/recursos",
                headers={
                    "Authorization": authorization,
                    "Content-Type": "application/json",
                },
                # Decimal becomes a decimal string, accepted by Core's BigDecimal DTO.
                content=resource.payload.model_dump_json(),
            )
        except httpx.ConnectError, httpx.ConnectTimeout, httpx.PoolTimeout:
            results.append(
                {
                    **result,
                    "status": "FALHA_CONEXAO",
                    "mensagem": "Não foi possível conectar ao Core.",
                }
            )
            http_status, fatal = (
                503,
                ("CORE_INDISPONIVEL", "Não foi possível conectar ao Core."),
            )
            continue
        except httpx.RequestError as exc:
            logger.warning(
                "Comunicação interrompida no POST de recurso: %s", type(exc).__name__
            )
            results.append(
                {
                    **result,
                    "status": "INDETERMINADO",
                    "mensagem": "Confirme o cadastro no Core antes de reenviar.",
                }
            )
            http_status = 504 if isinstance(exc, httpx.TimeoutException) else 502
            fatal = (
                "RESULTADO_INDETERMINADO",
                "O Core pode ter gravado o recurso; consulte o relatório.",
            )
            continue

        if response.status_code == 201:
            try:
                raw_id = response.json()["recurso"]["id"]
                if not isinstance(raw_id, str):
                    raise TypeError("ID must be a UUID string")
                resource_id = str(UUID(raw_id))
            except ValueError, KeyError, TypeError:
                results.append(
                    {
                        **result,
                        "status": "INDETERMINADO",
                        "mensagem": "O Core respondeu sucesso sem um ID válido; confirme o cadastro.",
                    }
                )
                http_status, fatal = (
                    502,
                    ("RESPOSTA_CORE_INVALIDA", "Resposta de criação fora do contrato."),
                )
            else:
                results.append({**result, "status": "IMPORTADO", "id": resource_id})
        elif response.status_code == 409:
            results.append(
                {
                    **result,
                    "status": "CONFLITO",
                    "mensagem": _business_message(
                        response, "Cadastro em conflito com os dados existentes."
                    ),
                }
            )
        elif response.status_code in (400, 422):
            results.append(
                {
                    **result,
                    "status": "REJEITADO",
                    "mensagem": _business_message(
                        response, "O Core rejeitou os dados deste recurso."
                    ),
                }
            )
        elif response.status_code in (401, 403):
            code, message = (
                ("NAO_AUTORIZADO", "Token ausente, inválido ou expirado.")
                if response.status_code == 401
                else (
                    "SEM_PERMISSAO",
                    "O Core exige ADMIN ou GESTOR_PROJETO para cadastrar recursos.",
                )
            )
            results.append({**result, "status": "REJEITADO", "mensagem": message})
            http_status, fatal = response.status_code, (code, message)
        else:
            # Even an upstream 500 can follow an uncertain write; don't promise rollback.
            results.append(
                {
                    **result,
                    "status": "INDETERMINADO",
                    "mensagem": "Resposta inesperada do Core; confirme o cadastro antes de reenviar.",
                }
            )
            http_status, fatal = (
                502,
                ("FALHA_CORE", "Resposta inesperada do Core; consulte o relatório."),
            )

    report = _report(filename, plan, results)
    if fatal:
        code, message = fatal
        return http_status, {
            "detail": {"codigo": code, "mensagem": message},
            "relatorio": report,
        }
    return 200, report
