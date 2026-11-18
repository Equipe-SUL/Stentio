from fastapi import Depends, Security, HTTPException, Request
from fastapi.security import APIKeyHeader
from sqlalchemy.orm import Session
from typing import Optional
import jwt
import uuid

from core.config import settings
from db.session import get_db
from db.models import Usuario

api_key_header = APIKeyHeader(name="Authorization", auto_error=False)

def obter_usuario_logado(
    request: Request,
    authorization: Optional[str] = Security(api_key_header),
    db: Session = Depends(get_db)
) -> dict:
    # 1. Tenta ler do Header Authorization; se não vier, lê do cookie 'stentio_token'
    raw_token = authorization or request.cookies.get("stentio_token")
    if not raw_token:
        raise HTTPException(status_code=401, detail="Não autorizado: Token ausente.")

    # Atalho para testes rápidos no Swagger (usando raw_token, nunca authorization!)
    if raw_token.upper() in ["ADMIN", "TRADUTOR"]:
        return {"usuario_id": "usuario-teste-swagger", "perfil": raw_token.upper()}

    # Remove o prefixo "Bearer " caso venha no header
    token = raw_token.replace("Bearer ", "").replace("bearer ", "").strip()

    try:
        payload = jwt.decode(
            token,
            settings.JWT_SECRET,
            algorithms=[settings.JWT_ALGORITHM]
        )

        identificador = payload.get("sub") or payload.get("email") or payload.get("id")
        if not identificador:
            raise HTTPException(status_code=401, detail="Token inválido: identificador ausente.")

        # Busca o usuário no PostgreSQL pelo UUID ou pelo e-mail
        usuario_db = None
        try:
            user_uuid = uuid.UUID(str(identificador))
            usuario_db = db.query(Usuario).filter(Usuario.id == user_uuid).first()
        except ValueError:
            usuario_db = db.query(Usuario).filter(Usuario.email == str(identificador)).first()

        # Fallback: se o usuário estiver no banco do Auth separado ou já vier com role no JWT
        if not usuario_db:
            role_jwt = payload.get("role") or payload.get("perfil") or "TRADUTOR"
            return {
                "usuario_id": str(identificador),
                "perfil": str(role_jwt).upper().replace("ROLE_", ""),
                "nome": str(identificador)
            }

        if not usuario_db.ativo:
            raise HTTPException(status_code=401, detail="Usuário inativo.")

        perfil_formatado = (
            str(usuario_db.role).upper().replace("ROLE_", "")
            if usuario_db.role
            else "TRADUTOR"
        )

        return {
            "usuario_id": str(usuario_db.id),
            "perfil": perfil_formatado,
            "nome": usuario_db.nome
        }

    except jwt.ExpiredSignatureError:
        raise HTTPException(status_code=401, detail="Sessão expirada. Faça login novamente.")
    except jwt.InvalidTokenError as e:
        print(f"Erro ao decodificar JWT: {e}")
        raise HTTPException(status_code=401, detail="Token de autenticação inválido.")