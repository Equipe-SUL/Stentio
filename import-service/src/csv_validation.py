import csv
import io


class CsvUploadError(Exception):
    def __init__(
        self, status_code: int, codigo: str, mensagem: str, *, linha: int | None = None
    ):
        super().__init__(mensagem)
        self.status_code = status_code
        self.detail: dict[str, str | int] = {"codigo": codigo, "mensagem": mensagem}
        if linha is not None:
            self.detail["linha"] = linha


def _detect_separator(text: str) -> str:
    """Compare the first nonblank logical record using the two supported dialects."""
    candidates = []
    for separator in (",", ";"):
        reader = csv.reader(
            io.StringIO(text, newline=""), delimiter=separator, strict=True
        )
        try:
            header = next((row for row in reader if row), [])
        except csv.Error:
            continue
        if len(header) > 1:
            candidates.append(separator)
    if len(candidates) == 2:
        raise CsvUploadError(
            422,
            "SEPARADOR_AMBIGUO",
            "Não foi possível definir o separador. Envie o campo 'separador' com ',' ou ';'.",
        )
    return candidates[0] if candidates else ","


def validate_csv(contents: bytes, separator: str | None = None) -> dict:
    if not contents:
        raise CsvUploadError(422, "ARQUIVO_VAZIO", "O arquivo está vazio.")
    try:
        # utf-8-sig also handles ordinary UTF-8 and removes an optional leading BOM.
        text = contents.decode("utf-8-sig")
    except UnicodeDecodeError as exc:
        raise CsvUploadError(
            422, "CODIFICACAO_INVALIDA", "Use um arquivo CSV com codificação UTF-8."
        ) from exc

    if not text.strip():
        raise CsvUploadError(422, "ARQUIVO_VAZIO", "O arquivo está vazio.")
    if any(
        (ord(char) < 32 and char not in "\t\r\n") or char == "\x7f" for char in text
    ):
        raise CsvUploadError(
            422,
            "CONTEUDO_INVALIDO",
            "O arquivo contém caracteres de controle não permitidos.",
        )

    separator = separator or _detect_separator(text)
    reader = csv.reader(io.StringIO(text, newline=""), delimiter=separator, strict=True)
    try:
        raw_headers = next((row for row in reader if row), [])
        headers = [header.strip() for header in raw_headers]
        if not headers or any(not header for header in headers):
            raise CsvUploadError(
                422,
                "CABECALHO_INVALIDO",
                "Todas as colunas precisam ter um nome.",
                linha=reader.line_num,
            )
        if len(set(headers)) != len(headers):
            raise CsvUploadError(
                422,
                "CABECALHO_DUPLICADO",
                "O cabeçalho contém nomes de colunas repetidos.",
                linha=reader.line_num,
            )

        total = 0
        sample = []
        for row in reader:
            if not row:  # Ignore physical blank lines; do not ignore records like ',,'.
                continue
            if len(row) != len(headers):
                raise CsvUploadError(
                    422,
                    "QUANTIDADE_COLUNAS_INVALIDA",
                    f"Esperadas {len(headers)} colunas; recebidas {len(row)}.",
                    linha=reader.line_num,
                )
            if all(not value.strip() for value in row):
                raise CsvUploadError(
                    422,
                    "REGISTRO_VAZIO",
                    "O registro não contém nenhum valor.",
                    linha=reader.line_num,
                )
            total += 1
            if len(sample) < 5:
                sample.append(dict(zip(headers, row, strict=True)))
    except csv.Error as exc:
        raise CsvUploadError(
            422,
            "CSV_MALFORMADO",
            "O CSV tem sintaxe inválida ou um campo excede o limite do leitor.",
            linha=reader.line_num,
        ) from exc

    if total == 0:
        raise CsvUploadError(
            422,
            "SEM_REGISTROS",
            "O CSV precisa conter cabeçalho e pelo menos um registro.",
        )
    return {"total_linhas": total, "amostra_dados": sample}
