import re
import unicodedata
from collections.abc import Callable, Mapping, Sequence
from typing import Any

from guardrails import Guard, OnFailAction, register_validator
from guardrails.validator_base import (
    FailResult,
    PassResult,
    ValidationResult,
    Validator,
)

RESPOSTA_FORA_ESCOPO = (
    "Posso ajudar com dúvidas sobre o Stentio e suas operações, como clientes, "
    "recursos, ordens de serviço, orçamentos e configurações. O que você gostaria "
    "de saber sobre o sistema?"
)

RESPOSTA_CREDENCIAIS = (
    "Não posso fornecer senhas, tokens ou outras credenciais. Para recuperar ou "
    "alterar uma senha, use o fluxo de redefinição ou peça ajuda a um administrador."
)

_TERMOS_SEGREDOS = (
    r"senhas?",
    r"passwords?",
    r"tokens?",
    r"chaves?(?: de acesso| secreta| api)?",
    r"credenciais?",
    r"api keys?",
    r"secrets?",
)
_SEGREDO = rf"(?:{'|'.join(_TERMOS_SEGREDOS)})"

_PADROES_PEDIDO_SEGREDO = (
    re.compile(
        rf"\b(?:qual|quais|me diga|me informe|informe|revele|mostre|"
        rf"me passe|me forneca|envie|passe)\b.{{0,60}}\b{_SEGREDO}\b"
    ),
    re.compile(
        rf"\b{_SEGREDO}\b.{{0,40}}\b(?:do|de|da|dos|das)?\s*"
        r"(?:admin|adm|administrador|administradora)\b"
    ),
    re.compile(
        rf"\b(?:admin|adm|administrador|administradora)\b.{{0,40}}\b{_SEGREDO}\b"
    ),
)

_PADRAO_GERENCIAR_SEGREDO = re.compile(
    r"^(?:como|onde|passo a passo|instrucoes|procedimento|qual o procedimento)"
    r"\b.{0,60}\b(?:redefinir|alterar|trocar|recuperar|resetar)\b"
)


def _normalizar(texto: str) -> str:
    decomposicao = unicodedata.normalize("NFKD", texto.casefold())
    sem_acentos = "".join(
        caractere
        for caractere in decomposicao
        if not unicodedata.combining(caractere)
    )
    return " ".join(re.sub(r"[^a-z0-9]+", " ", sem_acentos).split())


def _solicita_credencial(texto: str) -> bool:
    if _PADRAO_GERENCIAR_SEGREDO.search(texto):
        return False
    return any(padrao.search(texto) for padrao in _PADROES_PEDIDO_SEGREDO)


@register_validator(name="stentio/scope", data_type="string")
class StentioScopeValidator(Validator):
    def __init__(
        self,
        classificador: Callable[[str, Mapping[str, Any]], bool],
        on_fail: OnFailAction = OnFailAction.NOOP,
    ) -> None:
        self._classificador = classificador
        super().__init__(on_fail=on_fail)

    def _validate(self, value: str, metadata: dict[str, Any]) -> ValidationResult:
        if self._classificador(value, metadata):
            return PassResult()
        return FailResult(error_message="Mensagem fora do escopo do Stentio.")


def resposta_de_guardrail(
    mensagem: str,
    classificador: Callable[[str, Mapping[str, Any]], bool],
    contexto_manual: str = "",
    historico: Sequence[Mapping[str, str]] | None = None,
) -> str | None:
    if _solicita_credencial(_normalizar(mensagem)):
        return RESPOSTA_CREDENCIAIS

    guard = Guard().use(StentioScopeValidator(classificador=classificador))
    guard.configure(allow_metrics_collection=False)
    resultado = guard.validate(
        mensagem,
        metadata={
            "contexto_manual_disponivel": bool(contexto_manual.strip()),
            "historico": historico or [],
        },
    )
    if resultado.validation_passed:
        return None
    return RESPOSTA_FORA_ESCOPO
