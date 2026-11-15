from fastapi import Depends, Security, HTTPException
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
    authorization: Optional[str] = Security(api_key_header),
    db: Session = Depends(get_db)
) -> dict:
    if not authorization:
        raise HTTPException(status_code=401, detail="Não autorizado: Token ausente.")

    # Atalho para testes rápidos no Swagger sem precisar gerar token no Java
    if authorization.upper() in ["ADMIN", "TRADUTOR"]:
        return {"usuario_id": "usuario-teste-swagger", "perfil": authorization.upper()}

    # Remove o prefixo "Bearer " enviado pelo client HTTP
    token = authorization.replace("Bearer ", "").replace("bearer ", "").strip()

    try:
        payload = jwt.decode(
            token,
            settings.JWT_SECRET,
            algorithms=[settings.JWT_ALGORITHM]
        )

        # No Spring Boot, o subject (sub) geralmente é o email ou o UUID do usuário
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

        # Valida se o usuário existe e está ativo no sistema
        if not usuario_db or not usuario_db.ativo:
            raise HTTPException(status_code=401, detail="Usuário inativo ou não encontrado.")

        # Normaliza o perfil vindo da coluna 'role' (ex: "ROLE_TRADUTOR" -> "TRADUTOR")
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
        print(f"Erro ao decodificar JWT do Spring Boot: {e}")
        raise HTTPException(status_code=401, detail="Token de autenticação inválido.")