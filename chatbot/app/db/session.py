from core.config import settings
from sqlalchemy import create_engine
from sqlalchemy.orm import declarative_base, sessionmaker

engine = create_engine(settings.DATABASE_URL, pool_pre_ping=True)
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)

Base = declarative_base()


# Dependência para injetar a sessão do banco nas rotas/tools
def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()
