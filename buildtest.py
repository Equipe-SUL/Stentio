#!/usr/bin/env python3
"""Recria um conjunto amplo de dados de teste usando as APIs do Stentio.

Por segurança, o modo padrão apenas mostra o plano. --apply limpa as tabelas
públicas do banco *_test local antes de criar os dados. As contas de teste usam
a senha compartilhada Teste123!.
"""

from __future__ import annotations

import argparse
import getpass
import ipaddress
import json
import os
import secrets
import sys
import subprocess
import time
import uuid
from datetime import datetime, timedelta, timezone
from pathlib import Path
from urllib.error import HTTPError, URLError
from urllib.parse import urlencode, urlparse
from urllib.request import Request, urlopen


QUANTIDADE = 20
QUANTIDADE_RECURSOS_PORTAL = 4
SENHA_TESTE = "Teste123!"
PAPEIS = ("ADMIN", "ATENDENTE", "GESTOR_PROJETO", "FINANCEIRO")
UNIDADES = ("PALAVRA", "HORA", "PAGINA")
CATEGORIAS_PROJETO = (
    "Tradução Simples",
    "Tradução Juramentada",
    "Legenda",
    "Revisão",
    "Dublagem",
)
EMPRESAS_TESTE = (
    "XPTO Ltda.",
    "Esperança Ltda.",
    "Plantas ME",
    "Horizonte Traduções Ltda.",
    "Alfa Comércio Ltda.",
    "Beta Serviços ME",
    "Casa Verde Ltda.",
    "Sol Nascente ME",
    "Nova Era Consultoria Ltda.",
    "Ponto Sul Ltda.",
    "Aurora Idiomas Ltda.",
    "Via Global ME",
    "Estrela do Norte Ltda.",
    "Raízes do Brasil Ltda.",
    "Prisma Consultoria Ltda.",
    "Atlas Tecnologia Ltda.",
    "Caminho das Letras Ltda.",
    "Mar Azul Serviços ME",
    "Campo Belo Ltda.",
    "Vale das Flores Ltda.",
)
NOMES_EMAIL_TESTE = (
    "ana", "bruno", "caio", "daniela", "eduardo",
    "fernanda", "gabriel", "helena", "igor", "juliana",
    "kaua", "larissa", "marcos", "natalia", "otavio",
    "paula", "rafael", "sabrina", "tiago", "valeria",
)
SOBRENOMES_EMAIL_TESTE = (
    "silva", "santos", "oliveira", "souza", "rocha",
    "almeida", "ferreira", "costa", "ribeiro", "carvalho",
    "lima", "gomes", "martins", "araujo", "barbosa",
    "melo", "freitas", "dias", "castro", "teixeira",
)
# O sufixo .test evita a entrega de e-mails a caixas reais durante os testes.
SERVIDORES_EMAIL_TESTE = (
    "gmail.com.test",
    "outlook.com.test",
    "yahoo.com.test",
    "hotmail.com.test",
    "icloud.com.test",
)
IDIOMAS_OFICIAIS = (
    ("en-US", "Inglês (Estados Unidos)"),
    ("en-GB", "Inglês (Reino Unido)"),
    ("en-CA", "Inglês (Canadá)"),
    ("en-AU", "Inglês (Austrália)"),
    ("pt-BR", "Português (Brasil)"),
    ("pt-PT", "Português (Portugal)"),
    ("es-ES", "Espanhol (Espanha)"),
    ("es-MX", "Espanhol (México)"),
    ("es-AR", "Espanhol (Argentina)"),
    ("es-CO", "Espanhol (Colômbia)"),
    ("fr-FR", "Francês (França)"),
    ("fr-CA", "Francês (Canadá)"),
    ("fr-BE", "Francês (Bélgica)"),
    ("de-DE", "Alemão (Alemanha)"),
    ("de-AT", "Alemão (Áustria)"),
    ("de-CH", "Alemão (Suíça)"),
    ("it-IT", "Italiano (Itália)"),
    ("it-CH", "Italiano (Suíça)"),
    ("nl-NL", "Neerlandês (Países Baixos)"),
    ("nl-BE", "Neerlandês (Bélgica)"),
    ("sv-SE", "Sueco (Suécia)"),
    ("da-DK", "Dinamarquês (Dinamarca)"),
    ("nb-NO", "Norueguês Bokmål (Noruega)"),
    ("fi-FI", "Finlandês (Finlândia)"),
    ("pl-PL", "Polonês (Polônia)"),
    ("cs-CZ", "Tcheco (Tchéquia)"),
    ("sk-SK", "Eslovaco (Eslováquia)"),
    ("hu-HU", "Húngaro (Hungria)"),
    ("ro-RO", "Romeno (Romênia)"),
    ("bg-BG", "Búlgaro (Bulgária)"),
    ("el-GR", "Grego (Grécia)"),
    ("hr-HR", "Croata (Croácia)"),
    ("sr-RS", "Sérvio (Sérvia)"),
    ("uk-UA", "Ucraniano (Ucrânia)"),
    ("ru-RU", "Russo (Rússia)"),
    ("tr-TR", "Turco (Turquia)"),
    ("ar-SA", "Árabe (Arábia Saudita)"),
    ("ar-EG", "Árabe (Egito)"),
    ("he-IL", "Hebraico (Israel)"),
    ("fa-IR", "Persa (Irã)"),
    ("hi-IN", "Hindi (Índia)"),
    ("bn-BD", "Bengali (Bangladesh)"),
    ("ur-PK", "Urdu (Paquistão)"),
    ("zh-CN", "Chinês (China)"),
    ("zh-TW", "Chinês (Taiwan)"),
    ("zh-HK", "Chinês (Hong Kong)"),
    ("ja-JP", "Japonês (Japão)"),
    ("ko-KR", "Coreano (Coreia do Sul)"),
    ("th-TH", "Tailandês (Tailândia)"),
    ("vi-VN", "Vietnamita (Vietnã)"),
)
QUANTIDADE_IDIOMAS = len(IDIOMAS_OFICIAIS)


class ErroBuildtest(RuntimeError):
    """Falha explícita na preparação ou criação dos dados de teste."""


def executar_sql_banco(database: str, usuario_db: str, sql: str) -> None:
    comando = [
        "docker",
        "compose",
        "exec",
        "-T",
        "postgres",
        "psql",
        "-X",
        "-q",
        "-v",
        "ON_ERROR_STOP=1",
        "-U",
        usuario_db,
        "-d",
        database,
    ]
    try:
        resultado = subprocess.run(
            comando,
            cwd=Path(__file__).resolve().parent,
            input=sql,
            capture_output=True,
            check=False,
            text=True,
        )
    except FileNotFoundError as erro:
        raise ErroBuildtest("Docker Compose não está disponível para preparar o banco de teste.") from erro
    if resultado.returncode:
        detalhe = resultado.stderr.strip() or "erro não detalhado"
        raise ErroBuildtest(f"Falha ao preparar o banco {database}: {detalhe}")


def limpar_dados_de_teste(database_auth: str, database_core: str, usuario_db: str) -> None:
    bancos = {database_auth, database_core}
    if any(not banco.endswith("_test") for banco in bancos):
        raise ErroBuildtest(
            "O modo idempotente só pode limpar bancos cujo nome termina em '_test'. "
            "Configure POSTGRES_DB e CORE_POSTGRES_DB no .env."
        )

    limpar_core = """\
DO $$
DECLARE tabelas text;
BEGIN
    SELECT string_agg(format('%I.%I', schemaname, tablename), ', ')
    INTO tabelas
    FROM pg_tables
    WHERE schemaname = 'public'
      AND tablename NOT IN (
          'usuarios',
          'flyway_schema_history',
          'databasechangelog',
          'databasechangeloglock'
      );
    IF tabelas IS NOT NULL THEN
        EXECUTE 'TRUNCATE TABLE ' || tabelas || ' RESTART IDENTITY CASCADE';
    END IF;
END $$;
"""
    limpar_usuarios_buildtest = """\
DELETE FROM public.usuarios
WHERE lower(email) LIKE 'buildtest.%@stentio.test';
"""

    if database_auth == database_core:
        executar_sql_banco(
            database_auth,
            usuario_db,
            limpar_core + limpar_usuarios_buildtest,
        )
    else:
        executar_sql_banco(database_core, usuario_db, limpar_core)
        executar_sql_banco(database_auth, usuario_db, limpar_usuarios_buildtest)


def ler_env_raiz() -> dict[str, str]:
    valores: dict[str, str] = {}
    caminho = Path(__file__).resolve().parent / ".env"
    if not caminho.is_file():
        return valores

    for linha in caminho.read_text(encoding="utf-8").splitlines():
        texto = linha.strip()
        if not texto or texto.startswith("#") or "=" not in texto:
            continue
        chave, valor = texto.split("=", 1)
        valores[chave.strip()] = valor.strip().strip("\"'")
    return valores


def valor_config(nome: str, env_raiz: dict[str, str], padrao: str) -> str:
    return os.environ.get(nome, env_raiz.get(nome, padrao))


def host_local(url: str) -> bool:
    host = urlparse(url).hostname
    if not host:
        return False
    if host.lower() == "localhost":
        return True
    try:
        return ipaddress.ip_address(host).is_loopback
    except ValueError:
        return False


def smtp_local(host: str) -> bool:
    host = host.strip().lower()
    if host in {"localhost", "mailhog"}:
        return True
    try:
        return ipaddress.ip_address(host).is_loopback
    except ValueError:
        return False


def requisitar(
    base_url: str,
    metodo: str,
    caminho: str,
    *,
    token: str | None = None,
    dados: dict | None = None,
    campos: dict[str, str] | None = None,
) -> dict | list | None:
    cabecalhos = {"Accept": "application/json"}
    corpo: bytes | None = None

    if dados is not None:
        corpo = json.dumps(dados, ensure_ascii=False).encode("utf-8")
        cabecalhos["Content-Type"] = "application/json"
    elif campos is not None:
        limite = "----Buildtest" + uuid.uuid4().hex
        partes = []
        for nome, valor in campos.items():
            partes.extend(
                (
                    f"--{limite}\r\n".encode(),
                    f'Content-Disposition: form-data; name="{nome}"\r\n\r\n'.encode(),
                    str(valor).encode("utf-8"),
                    b"\r\n",
                )
            )
        partes.append(f"--{limite}--\r\n".encode())
        corpo = b"".join(partes)
        cabecalhos["Content-Type"] = f"multipart/form-data; boundary={limite}"

    if token:
        cabecalhos["Authorization"] = f"Bearer {token}"

    url = base_url.rstrip("/") + caminho
    pedido = Request(url, data=corpo, headers=cabecalhos, method=metodo)
    try:
        with urlopen(pedido, timeout=30) as resposta:
            payload = resposta.read()
    except HTTPError as erro:
        detalhe = erro.read().decode("utf-8", errors="replace")
        raise ErroBuildtest(
            f"{metodo} {caminho} respondeu HTTP {erro.code}: {detalhe[:500]}"
        ) from erro
    except URLError as erro:
        raise ErroBuildtest(f"Não foi possível acessar {url}: {erro.reason}") from erro

    if not payload:
        return None
    try:
        return json.loads(payload)
    except json.JSONDecodeError as erro:
        raise ErroBuildtest(f"Resposta não JSON em {metodo} {caminho}") from erro


def exigir_id(resposta: dict | None, contexto: str) -> str:
    if not isinstance(resposta, dict) or not resposta.get("id"):
        raise ErroBuildtest(f"A resposta de {contexto} não trouxe o campo id.")
    return str(resposta["id"])


def digitos_cnpj(base: str) -> str:
    pesos_primeiro = (5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2)
    soma = sum(int(digito) * peso for digito, peso in zip(base, pesos_primeiro))
    resto = soma % 11
    primeiro = 0 if resto < 2 else 11 - resto

    pesos_segundo = (6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2)
    parcial = base + str(primeiro)
    soma = sum(int(digito) * peso for digito, peso in zip(parcial, pesos_segundo))
    resto = soma % 11
    segundo = 0 if resto < 2 else 11 - resto
    return base + str(primeiro) + str(segundo)


def novo_cnpj() -> str:
    while True:
        base = str(secrets.randbelow(900_000_000_000) + 100_000_000_000)
        cnpj = digitos_cnpj(base)
        if len(set(cnpj)) > 1:
            return cnpj


def gerar_emails_clientes(quantidade: int) -> list[str]:
    emails = [
        f"{nome}.{sobrenome}@{servidor}"
        for nome in NOMES_EMAIL_TESTE
        for sobrenome in SOBRENOMES_EMAIL_TESTE
        for servidor in SERVIDORES_EMAIL_TESTE
    ]
    if quantidade > len(emails):
        raise ErroBuildtest("Não há combinações de e-mail suficientes para os clientes.")
    secrets.SystemRandom().shuffle(emails)
    return emails[:quantidade]


def obter_ordens(core_url: str, token: str) -> list[dict]:
    resposta = requisitar(
        core_url,
        "GET",
        "/api/v1/ordens-servico?" + urlencode({"pagina": 1, "tamanho_pagina": 100}),
        token=token,
    )
    if not isinstance(resposta, dict) or not isinstance(resposta.get("conteudo"), list):
        raise ErroBuildtest("A listagem de ordens de serviço veio em formato inesperado.")
    return resposta["conteudo"]


def criar_dados(
    auth_url: str,
    core_url: str,
    admin_email: str,
    admin_senha: str,
    run_id: str,
) -> None:
    login = requisitar(
        auth_url,
        "POST",
        "/api/v1/usuarios/login",
        dados={"email": admin_email, "senha": admin_senha},
    )
    if not isinstance(login, dict) or not login.get("token") or login.get("role") != "ADMIN":
        raise ErroBuildtest("O login administrativo não retornou um token.")
    token = str(login["token"])

    requisitar(core_url, "GET", "/actuator/health")
    ordens_anteriores = {str(ordem["id"]) for ordem in obter_ordens(core_url, token)}

    print(f"Iniciando conjunto de teste {run_id}; todos os registros terão identificadores exclusivos.")

    usuarios: list[str] = []
    for indice in range(QUANTIDADE):
        email = f"buildtest.{run_id}.{indice + 1:02d}@stentio.test"
        resposta = requisitar(
            auth_url,
            "POST",
            "/api/v1/usuarios",
            token=token,
            dados={
                "nome": f"Usuário Buildtest {run_id} {indice + 1:02d}",
                "email": email,
                "senha": SENHA_TESTE,
                "role": PAPEIS[indice % len(PAPEIS)],
            },
        )
        exigir_id(resposta, f"usuário {indice + 1}")
        usuarios.append(email)

    tokens_por_papel = {"ADMIN": token}
    for papel in ("ATENDENTE", "GESTOR_PROJETO"):
        email = usuarios[PAPEIS.index(papel)]
        sessao = requisitar(
            auth_url,
            "POST",
            "/api/v1/usuarios/login",
            dados={"email": email, "senha": SENHA_TESTE},
        )
        if not isinstance(sessao, dict) or not sessao.get("token") or sessao.get("role") != papel:
            raise ErroBuildtest(f"Não foi possível autenticar a conta de teste {papel}.")
        tokens_por_papel[papel] = str(sessao["token"])

    token_atendente = tokens_por_papel["ATENDENTE"]
    token_gestor = tokens_por_papel["GESTOR_PROJETO"]

    categorias: list[str] = []
    for nome in CATEGORIAS_PROJETO:
        resposta = requisitar(
            core_url,
            "POST",
            "/api/v1/categorias-projeto",
            token=token,
            dados={"nome": nome},
        )
        categorias.append(exigir_id(resposta, f"categoria {nome}"))

    idiomas: list[str] = []
    for codigo_iso, nome in IDIOMAS_OFICIAIS:
        resposta = requisitar(
            core_url,
            "POST",
            "/api/v1/idiomas",
            token=token,
            dados={"nome": nome, "codigoIso": codigo_iso},
        )
        idiomas.append(exigir_id(resposta, f"idioma {codigo_iso}"))

    tipos: list[tuple[str, str]] = []
    for indice in range(QUANTIDADE):
        nome = f"BT-{run_id}-Servico-{indice + 1:02d}"
        resposta = requisitar(
            core_url,
            "POST",
            "/api/v1/tipos-servico",
            token=token,
            dados={"nome": nome},
        )
        tipos.append((exigir_id(resposta, f"tipo de serviço {indice + 1}"), nome))

    clientes: list[str] = []
    emails_clientes = gerar_emails_clientes(QUANTIDADE)
    for indice in range(QUANTIDADE):
        resposta = requisitar(
            core_url,
            "POST",
            "/api/v1/clientes",
            token=token,
            dados={
                "nomeEmpresa": EMPRESAS_TESTE[indice],
                "nomeRepresentante": f"Representante {indice + 1:02d}",
                "emailRepresentante": emails_clientes[indice],
                "telefone": f"+55119999{indice:06d}",
                "cpfCnpj": novo_cnpj(),
            },
        )
        clientes.append(exigir_id(resposta, f"cliente {indice + 1}"))

    precos_tabela: list[str] = []
    for indice in range(QUANTIDADE):
        resposta = requisitar(
            core_url,
            "POST",
            "/api/v1/admin/tabelas-preco",
            token=token,
            dados={
                "tipoServicoId": tipos[indice][0],
                "idiomaOrigemId": idiomas[indice],
                "idiomaDestinoId": idiomas[(indice + 1) % QUANTIDADE_IDIOMAS],
                "unidade": UNIDADES[indice % len(UNIDADES)],
                "valorUnitario": f"{0.15 + indice:.4f}",
            },
        )
        precos_tabela.append(exigir_id(resposta, f"tabela de preço {indice + 1}"))

    recursos: list[str] = []
    for indice in range(QUANTIDADE):
        resposta = requisitar(
            core_url,
            "POST",
            "/api/v1/recursos",
            token=token,
            dados={
                "nome": f"Recurso Buildtest {run_id} {indice + 1:02d}",
                "email": f"recurso.{run_id}.{indice + 1:02d}@stentio.test",
                "telefone": f"+55118888{indice:06d}",
                "tiposServicoIds": [tipos[indice][0]],
                "precos": [
                    {
                        "idiomaOrigemId": idiomas[indice],
                        "idiomaDestinoId": idiomas[(indice + 1) % QUANTIDADE_IDIOMAS],
                        "unidade": UNIDADES[indice % len(UNIDADES)],
                        "valor": f"{0.10 + indice:.4f}",
                    }
                ],
            },
        )
        recurso = resposta.get("recurso") if isinstance(resposta, dict) else None
        recurso_id = exigir_id(recurso, f"recurso {indice + 1}")
        requisitar(
            core_url,
            "PATCH",
            f"/api/v1/recursos/{recurso_id}/verificacao",
            token=token,
            dados={"verificado": True},
        )
        recursos.append(recurso_id)

    recursos_portal: list[dict[str, str]] = []
    for indice in range(QUANTIDADE_RECURSOS_PORTAL):
        email = f"buildtest.recurso.{run_id}.{indice + 1:02d}@stentio.test"
        conta = requisitar(
            auth_url,
            "POST",
            "/api/v1/public/usuarios",
            dados={
                "nome": f"Recurso Portal Buildtest {run_id} {indice + 1:02d}",
                "email": email,
                "senha": SENHA_TESTE,
            },
        )
        if not isinstance(conta, dict) or conta.get("role") != "RECURSO":
            raise ErroBuildtest(f"A conta de teste de recurso {indice + 1} não recebeu o papel RECURSO.")

        sessao = requisitar(
            auth_url,
            "POST",
            "/api/v1/usuarios/login",
            dados={"email": email, "senha": SENHA_TESTE},
        )
        if not isinstance(sessao, dict) or not sessao.get("token") or sessao.get("role") != "RECURSO":
            raise ErroBuildtest(f"Não foi possível autenticar a conta de portal {indice + 1}.")
        token_recurso = str(sessao["token"])

        perfil = requisitar(
            core_url,
            "POST",
            "/api/v1/recursos/auto-cadastro",
            token=token_recurso,
            dados={
                "nome": f"Recurso Portal Buildtest {run_id} {indice + 1:02d}",
                "email": email,
                "telefone": f"+55117777{indice:06d}",
                "tiposServicoIds": [tipos[indice][0]],
                "precos": [
                    {
                        "idiomaOrigemId": idiomas[indice],
                        "idiomaDestinoId": idiomas[(indice + 1) % QUANTIDADE_IDIOMAS],
                        "unidade": UNIDADES[indice % len(UNIDADES)],
                        "valor": f"{0.20 + indice:.4f}",
                    }
                ],
            },
        )
        recursos_portal.append(
            {"id": exigir_id(perfil, f"perfil do recurso {indice + 1}"), "email": email, "token": token_recurso}
        )

    requisitar(
        core_url,
        "PATCH",
        f"/api/v1/recursos/{recursos_portal[0]['id']}/verificacao",
        token=token,
        dados={"verificado": True},
    )

    modelos: list[str] = []
    for indice in range(QUANTIDADE):
        etapas = [
            {
                "ordem": 1,
                "nome": "Tradução de teste",
                "tipo_servico_id": tipos[indice][0],
                "prazo_estimado_horas": 24,
            },
            {
                "ordem": 2,
                "nome": "Revisão de teste",
                "tipo_servico_id": tipos[(indice + 1) % QUANTIDADE][0],
                "prazo_estimado_horas": 12,
            },
        ]
        resposta = requisitar(
            core_url,
            "POST",
            "/api/v1/modelos-workflow",
            token=token,
            dados={
                "nome": f"BT-{run_id}-Workflow-{indice + 1:02d}",
                "descricao": "Workflow automatizado para testes locais.",
                "etapas": etapas,
            },
        )
        modelos.append(exigir_id(resposta, f"modelo de workflow {indice + 1}"))

    solicitacoes: list[tuple[str, int]] = []
    for indice in range(5 * QUANTIDADE):
        par = indice % QUANTIDADE
        resposta = requisitar(
            core_url,
            "POST",
            "/api/v1/solicitacoes",
            token=token_atendente,
            campos={
                "clienteId": clientes[par],
                "categoriaProjetoId": categorias[par % len(categorias)],
                "idiomaOrigemId": idiomas[par],
                "idiomaDestinoId": idiomas[(par + 1) % QUANTIDADE_IDIOMAS],
                "descricao": f"Solicitação de teste {run_id} {indice + 1:03d}",
            },
        )
        solicitacoes.append((exigir_id(resposta, f"solicitação {indice + 1}"), par))

    agora = datetime.now(timezone.utc)
    orcamentos: list[str] = []
    clientes_aprovados: dict[str, int] = {}
    for indice, (solicitacao_id, cliente_indice) in enumerate(solicitacoes[QUANTIDADE:], start=QUANTIDADE):
        expiracao = (agora + timedelta(days=30)).isoformat(timespec="seconds").replace("+00:00", "Z")
        resposta = requisitar(
            core_url,
            "POST",
            "/api/v1/orcamentos",
            token=token_atendente,
            dados={
                "solicitacao_id": solicitacao_id,
                "cliente_id": clientes[cliente_indice],
                "data_expiracao": expiracao,
            },
        )
        orcamento_id = exigir_id(resposta, f"orçamento {indice + 1}")
        orcamentos.append(orcamento_id)
        requisitar(
            core_url,
            "POST",
            f"/api/v1/orcamentos/{orcamento_id}/itens",
            token=token_atendente,
            dados={
                "itens": [
                    {
                        "ordem": 1,
                        "idioma_origem_id": idiomas[cliente_indice],
                        "idioma_destino_id": idiomas[(cliente_indice + 1) % QUANTIDADE_IDIOMAS],
                        "servicos": [
                            {
                                "tipo_servico": tipos[cliente_indice][1],
                                "quantidade": 100 + cliente_indice,
                                "unidade": UNIDADES[cliente_indice % len(UNIDADES)],
                                "valor_unitario": f"{1.25 + cliente_indice:.4f}",
                            }
                        ],
                    }
                ]
            },
        )

        grupo = indice // QUANTIDADE
        if grupo >= 2:
            envio = requisitar(
                core_url,
                "POST",
                f"/api/v1/orcamentos/{orcamento_id}/enviar-aprovacao",
                token=token_atendente,
            )
            if not isinstance(envio, dict) or not envio.get("token_aprovacao"):
                raise ErroBuildtest(f"O envio do orçamento {indice + 1} não retornou token.")
            link_token = str(envio["token_aprovacao"])
            if grupo == 3:
                requisitar(
                    core_url,
                    "POST",
                    "/api/v1/public/orcamentos/rejeitar",
                    dados={
                        "token": link_token,
                        "motivo": "Rejeição gerada para validar o fluxo de teste.",
                    },
                )
            elif grupo == 4:
                requisitar(
                    core_url,
                    "POST",
                    "/api/v1/public/orcamentos/aprovar",
                    dados={"token": link_token},
                )
                clientes_aprovados[clientes[cliente_indice]] = indice

    ordens_novas: dict[str, dict] = {}
    limite = time.monotonic() + 60
    while time.monotonic() < limite and len(ordens_novas) < QUANTIDADE:
        for ordem in obter_ordens(core_url, token_gestor):
            ordem_id = str(ordem.get("id", ""))
            cliente_id = str(ordem.get("cliente_id", ""))
            if ordem_id not in ordens_anteriores and cliente_id in clientes_aprovados:
                ordens_novas[cliente_id] = ordem
        if len(ordens_novas) < QUANTIDADE:
            time.sleep(2)

    if len(ordens_novas) != QUANTIDADE:
        raise ErroBuildtest(
            f"Foram encontrados {len(ordens_novas)} de {QUANTIDADE} OS aprovadas. "
            "Verifique RabbitMQ, o outbox e os logs do Core; os cadastros anteriores "
            "permanecem gravados."
        )

    etapas_planejadas = 0
    alocacoes = 0
    for cliente_id, indice_aprovado in clientes_aprovados.items():
        ordem = ordens_novas[cliente_id]
        if indice_aprovado >= 90:
            continue
        ordem_id = str(ordem["id"])
        etapas = requisitar(
            core_url,
            "POST",
            f"/api/v1/ordens-servico/{ordem_id}/etapas/aplicar-modelo/{modelos[indice_aprovado % QUANTIDADE]}",
            token=token_gestor,
        )
        if not isinstance(etapas, dict) or not etapas.get("etapas"):
            raise ErroBuildtest(f"A OS {ordem_id} não retornou etapas após aplicar o modelo.")
        etapas_planejadas += 1
        primeira_etapa = etapas["etapas"][0]
        prazo = (datetime.now(timezone.utc) + timedelta(days=7)).isoformat(timespec="seconds").replace("+00:00", "Z")
        requisitar(
            core_url,
            "POST",
            f"/api/v1/ordens-servico/{ordem_id}/etapas/{primeira_etapa['id']}/alocacao",
            token=token_gestor,
            dados={
                "recurso_id": (
                    recursos_portal[0]["id"]
                    if alocacoes == 0
                    else recursos[indice_aprovado % QUANTIDADE]
                ),
                "data_limite_entrega": prazo,
            },
        )
        alocacoes += 1

    for indice in range(QUANTIDADE_IDIOMAS - 4, QUANTIDADE_IDIOMAS):
        requisitar(
            core_url,
            "PATCH",
            f"/api/v1/idiomas/{idiomas[indice]}/status",
            token=token,
            dados={"ativo": False},
        )

    for indice in range(QUANTIDADE - 4, QUANTIDADE):
        for caminho, identificador in (
            (f"/api/v1/tipos-servico/{tipos[indice][0]}/status", {"ativo": False}),
            (f"/api/v1/recursos/{recursos[indice]}", {"ativo": False}),
            (f"/api/v1/modelos-workflow/{modelos[indice]}", {"ativo": False}),
        ):
            requisitar(core_url, "PATCH", caminho, token=token, dados=identificador)

    perfil_portal = requisitar(
        core_url,
        "GET",
        "/api/v1/recurso/perfil",
        token=recursos_portal[0]["token"],
    )
    demandas_portal = requisitar(
        core_url,
        "GET",
        "/api/v1/recurso/etapas",
        token=recursos_portal[0]["token"],
    )
    if not isinstance(perfil_portal, dict) or perfil_portal.get("id") != recursos_portal[0]["id"]:
        raise ErroBuildtest("A conta RECURSO não conseguiu recuperar o próprio perfil.")
    if perfil_portal.get("verificado") is not True:
        raise ErroBuildtest("O perfil RECURSO alocado não está verificado.")
    if not isinstance(demandas_portal, list) or not demandas_portal:
        raise ErroBuildtest("O portal RECURSO não retornou a etapa alocada.")

    print("\nSeed concluído:")
    print(f"  usuários de equipe: {len(usuarios)} (roles variadas)")
    print(f"  contas/perfis RECURSO: {len(recursos_portal)} (3 pendentes de verificação)")
    print(f"  clientes: {len(clientes)}")
    print(f"  categorias: {len(categorias)} padronizadas")
    print(f"  idiomas: {len(idiomas)} oficiais (4 inativos)")
    print(f"  tipos de serviço: {len(tipos)} (4 inativos)")
    print(f"  recursos: {len(recursos)} (4 inativos)")
    print(f"  pares de idiomas de recursos: {len(recursos)}")
    print(f"  tabelas de preço: {len(precos_tabela)}")
    print(f"  modelos de workflow: {len(modelos)} (4 inativos)")
    print(f"  solicitações: {len(solicitacoes)} (20 sem orçamento)")
    print(f"  orçamentos: {len(orcamentos)} (20 em elaboração, 20 aguardando, 20 rejeitados, 20 aprovados)")
    print(f"  ordens de serviço: {len(ordens_novas)} (10 pendentes de alocação, 10 planejadas)")
    print(f"  etapas planejadas/alocadas: {etapas_planejadas}/{alocacoes}")
    print("  trilha de auditoria: ações de ATENDENTE, GESTOR_PROJETO, TOKEN_PUBLICO e SISTEMA")
    print(f"\nSenha compartilhada das contas de teste: {SENHA_TESTE}")
    print("Contas:")
    for indice, email in enumerate(usuarios):
        print(f"  {email} ({PAPEIS[indice % len(PAPEIS)]})")
    for recurso in recursos_portal:
        print(f"  {recurso['email']} (RECURSO)")


def main() -> int:
    parser = argparse.ArgumentParser(description="Gera massa de teste para Auth e Core.")
    parser.add_argument(
        "--apply",
        action="store_true",
        help="limpa e recria os dados em um banco local *_test",
    )
    parser.add_argument("--auth-url", help="URL base do Auth (padrão: localhost:SERVER_PORT)")
    parser.add_argument("--core-url", help="URL base do Core (padrão: localhost:CORE_SERVER_PORT)")
    parser.add_argument("--email-url", help="URL base do email-service (padrão: localhost:EMAIL_SERVICE_PORT)")
    parser.add_argument("--admin-email", help="e-mail de administrador; também aceita BUILDTEST_ADMIN_EMAIL")
    args = parser.parse_args()

    env_raiz = ler_env_raiz()
    auth_url = args.auth_url or valor_config(
        "BUILDTEST_AUTH_URL",
        env_raiz,
        f"http://127.0.0.1:{valor_config('SERVER_PORT', env_raiz, '8081')}",
    )
    core_url = args.core_url or valor_config(
        "BUILDTEST_CORE_URL",
        env_raiz,
        f"http://127.0.0.1:{valor_config('CORE_SERVER_PORT', env_raiz, '8082')}",
    )
    email_url = args.email_url or valor_config(
        "BUILDTEST_EMAIL_URL",
        env_raiz,
        f"http://127.0.0.1:{valor_config('EMAIL_SERVICE_PORT', env_raiz, '8083')}",
    )
    run_id = datetime.now(timezone.utc).strftime("%Y%m%d%H%M%S") + secrets.token_hex(2)

    print("Plano de massa de teste:")
    print(f"  {QUANTIDADE} usuários de equipe e {QUANTIDADE_RECURSOS_PORTAL} contas/perfis RECURSO.")
    print(f"  {QUANTIDADE} clientes, tipos de serviço e recursos de equipe; {len(CATEGORIAS_PROJETO)} categorias.")
    print(f"  {QUANTIDADE_IDIOMAS} idiomas oficiais com códigos ISO/BCP 47,")
    print("  pares de idiomas, tabelas de preço e workflows.")
    print("  100 solicitações: 20 pendentes de orçamento e 80 com orçamento.")
    print("  80 orçamentos: 20 em elaboração, 20 aguardando, 20 rejeitados e 20 aprovados.")
    print("  20 ordens de serviço: 10 pendentes de alocação e 10 planejadas/alocadas.")
    print(
        "  --apply limpa e recria as tabelas públicas do banco *_test; preserva usuarios, "
        "exceto contas buildtest anteriores."
    )
    print("  Recursos usados em OS são verificados; 3 perfis RECURSO ficam pendentes e 1 recebe uma etapa.")
    print("  A trilha de auditoria inclui ATENDENTE, GESTOR_PROJETO, TOKEN_PUBLICO e SISTEMA.")
    print("  --apply aceita somente APIs locais, bancos terminados em _test e SMTP local.")
    print("  --apply exige SMTP local (MailHog recomendado); não envia e-mails para servidores externos.")
    print("  Envios acionam o outbox de e-mail, sempre com destinatários no domínio reservado .test.")

    if not args.apply:
        print("\nSimulação: nenhuma conexão ou gravação realizada. Use --apply para executar.")
        return 0

    if not host_local(auth_url) or not host_local(core_url) or not host_local(email_url):
        raise ErroBuildtest(
            "Por segurança, --apply idempotente aceita somente URLs localhost/loopback."
        )

    admin_email = (
        args.admin_email
        or os.environ.get("BUILDTEST_ADMIN_EMAIL")
        or env_raiz.get("ADMIN_INITIAL_EMAIL")
        or input("E-mail de administrador: ").strip()
    )
    admin_senha = (
        os.environ.get("BUILDTEST_ADMIN_PASSWORD")
        or env_raiz.get("BUILDTEST_ADMIN_PASSWORD")
        or env_raiz.get("ADMIN_INITIAL_PASSWORD")
    )
    if not admin_senha:
        admin_senha = getpass.getpass("Senha atual do administrador (não será exibida): ")
    if not admin_email or not admin_senha:
        raise ErroBuildtest(
            "Informe BUILDTEST_ADMIN_EMAIL se necessário e use o prompt ou "
            "BUILDTEST_ADMIN_PASSWORD para a senha atual."
        )

    login = requisitar(
        auth_url,
        "POST",
        "/api/v1/usuarios/login",
        dados={"email": admin_email, "senha": admin_senha},
    )
    if not isinstance(login, dict) or not login.get("token") or login.get("role") != "ADMIN":
        raise ErroBuildtest("O login informado não autenticou uma conta ADMIN.")
    token = str(login["token"])

    requisitar(email_url, "GET", "/actuator/health")
    smtp = requisitar(email_url, "GET", "/api/v1/smtp", token=token)
    if not isinstance(smtp, dict) or not smtp_local(str(smtp.get("host", ""))):
        raise ErroBuildtest(
            "Configure um SMTP local antes do seed (MailHog recomendado em mailhog:1025). "
            "Nenhum dado foi limpo."
        )
    if str(smtp.get("host", "")).lower() == "mailhog" and smtp.get("port") != 1025:
        raise ErroBuildtest(
            "MailHog no Docker Compose deve usar a porta 1025. Nenhum dado foi limpo."
        )

    database_auth = valor_config("POSTGRES_DB", env_raiz, "stentio")
    database_core = valor_config("CORE_POSTGRES_DB", env_raiz, "") or database_auth
    usuario_db = valor_config("POSTGRES_USER", env_raiz, "postgres")

    print(
        f"\nATENÇÃO: --apply limpa os dados de {database_core} e remove usuários "
        "buildtest anteriores, preservando a conta administradora."
    )
    limpar_dados_de_teste(database_auth, database_core, usuario_db)
    criar_dados(auth_url, core_url, admin_email, admin_senha, run_id)
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except ErroBuildtest as erro:
        print(f"Erro: {erro}", file=sys.stderr)
        raise SystemExit(1)
