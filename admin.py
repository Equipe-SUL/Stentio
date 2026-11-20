#!/usr/bin/env python3
"""Redefine a senha da conta administradora no banco local de desenvolvimento."""

from __future__ import annotations

import argparse
import getpass
import os
import subprocess
import sys
from pathlib import Path


class ErroAdmin(RuntimeError):
    """Falha ao redefinir a senha da conta administradora."""


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


def valor_sql(texto: str) -> str:
    return "'" + texto.replace("'", "''") + "'"


def atualizar_senha(database: str, usuario_db: str, email: str, senha_nova: str) -> None:
    try:
        import bcrypt
    except ImportError as erro:
        raise ErroAdmin("O pacote bcrypt do Python é necessário para gerar o hash da senha.") from erro

    senha_hash = bcrypt.hashpw(
        senha_nova.encode("utf-8"),
        bcrypt.gensalt(rounds=10, prefix=b"2a"),
    ).decode("ascii")
    sql = f"""\
DO $$
DECLARE linhas_alteradas integer;
BEGIN
    UPDATE public.usuarios
    SET senha = {valor_sql(senha_hash)}
    WHERE lower(email) = lower({valor_sql(email)})
      AND role = 'ADMIN';
    GET DIAGNOSTICS linhas_alteradas = ROW_COUNT;
    IF linhas_alteradas <> 1 THEN
        RAISE EXCEPTION 'Esperada exatamente uma conta ADMIN para o e-mail informado';
    END IF;
END $$;
"""
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
        raise ErroAdmin("Docker Compose não está disponível para acessar o banco de desenvolvimento.") from erro
    if resultado.returncode:
        detalhe = resultado.stderr.strip() or "erro não detalhado"
        raise ErroAdmin(f"Falha ao redefinir a senha: {detalhe}")


def main() -> int:
    parser = argparse.ArgumentParser(
        description=(
            "Redefine a senha do administrador sem pedir a senha antiga; "
            "aceita apenas banco local cujo nome termine em _test."
        )
    )
    parser.add_argument("--database", help="banco de Auth (padrão: POSTGRES_DB do .env)")
    parser.add_argument("--email", help="e-mail da conta (padrão: ADMIN_INITIAL_EMAIL)")
    args = parser.parse_args()

    env_raiz = ler_env_raiz()
    database = args.database or os.environ.get("POSTGRES_DB") or env_raiz.get("POSTGRES_DB", "stentio")
    email = (
        args.email
        or os.environ.get("BUILDTEST_ADMIN_EMAIL")
        or env_raiz.get("ADMIN_INITIAL_EMAIL")
        or "admin@stentio.com"
    )
    usuario_db = os.environ.get("POSTGRES_USER") or env_raiz.get("POSTGRES_USER", "postgres")
    if not database.endswith("_test"):
        raise ErroAdmin("Por segurança, a redefinição só aceita bancos cujo nome termina em '_test'.")

    senha_nova = getpass.getpass("Nova senha (mínimo 8 caracteres): ")
    confirmacao = getpass.getpass("Confirme a nova senha: ")
    if len(senha_nova) < 8:
        raise ErroAdmin("A nova senha deve ter no mínimo 8 caracteres.")
    if len(senha_nova.encode("utf-8")) > 72:
        raise ErroAdmin("A senha deve ter no máximo 72 bytes para ser compatível com BCrypt.")
    if senha_nova != confirmacao:
        raise ErroAdmin("A confirmação não corresponde à nova senha.")

    atualizar_senha(database, usuario_db, email, senha_nova)
    print(f"Senha de {email} redefinida com sucesso.")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except ErroAdmin as erro:
        print(f"Erro: {erro}", file=sys.stderr)
        raise SystemExit(1)
