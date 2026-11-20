import re
from pathlib import Path

STOPWORDS = {
    "a",
    "o",
    "as",
    "os",
    "um",
    "uma",
    "uns",
    "umas",
    "de",
    "do",
    "da",
    "dos",
    "das",
    "em",
    "no",
    "na",
    "nos",
    "nas",
    "por",
    "para",
    "com",
    "sem",
    "sob",
    "sobre",
    "e",
    "ou",
    "mas",
    "que",
    "qual",
    "quais",
    "como",
    "onde",
    "quando",
    "porque",
    "eu",
    "ele",
    "ela",
    "eles",
    "elas",
    "meu",
    "minha",
    "seu",
    "sua",
    "fazer",
    "posso",
    "consigo",
    "sistema",
    "stentio",
    "tela",
    "esta",
    "este",
    "isso",
}


def _carregar_manual() -> str:
    base_dir = Path(__file__).resolve().parent.parent
    diretorios_possiveis = [base_dir / "docs", base_dir.parent / "docs"]
    nomes_possiveis = [
        "manual_stentio.md",
        "manual_operacional_do_sistema_stentio.md",
    ]
    for diretorio in diretorios_possiveis:
        for nome in nomes_possiveis:
            caminho = diretorio / nome
            if caminho.exists():
                return caminho.read_text(encoding="utf-8")
    return ""


def _tokenizar(texto: str) -> set[str]:
    palavras = re.findall(r"\b[a-zà-ú0-9]{3,}\b", texto.lower())
    return {p for p in palavras if p not in STOPWORDS}


def buscar_contexto_manual(pergunta: str, top_k: int = 3) -> str:
    """
    Busca as seções mais relevantes do manual oficial para responder
    dúvidas operacionais e regras de negócio (CA.2).
    """
    conteudo = _carregar_manual()
    if not conteudo:
        return ""

    # Divide o manual pelas seções principais (## )
    secoes = re.split(r"(?=^##\s+)", conteudo, flags=re.MULTILINE)
    secoes = [s.strip() for s in secoes if s.strip() and not s.startswith("# Manual")]

    tokens_pergunta = _tokenizar(pergunta)
    if not tokens_pergunta:
        return ""

    ranqueadas = []
    for secao in secoes:
        linhas = secao.splitlines()
        titulo = linhas[0] if linhas else ""
        tokens_titulo = _tokenizar(titulo)
        tokens_corpo = _tokenizar(secao)

        # Peso maior (3x) quando a palavra da pergunta aparece no título da seção
        score_titulo = len(tokens_pergunta & tokens_titulo) * 3
        score_corpo = len(tokens_pergunta & tokens_corpo)
        score_total = score_titulo + score_corpo

        if score_total > 0:
            ranqueadas.append((score_total, secao))

    ranqueadas.sort(key=lambda item: item[0], reverse=True)
    melhores = [secao for _, secao in ranqueadas[:top_k]]

    return "\n\n---\n\n".join(melhores)
