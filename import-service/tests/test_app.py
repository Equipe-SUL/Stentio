from unittest.mock import patch

from fastapi.testclient import TestClient
from src.config import Settings
from src.app import app

client = TestClient(app)


def test_logger_level_is_case_insensitive():
    assert Settings(LOGGER_LEVEL="info").LOGGER_LEVEL == "INFO"


def test_upload_success():
    csv_content = "ativo,e-mail,nome,telefone\n1,alice@example.com,Alice,123456789\n"
    filename = "recursos.csv"

    with patch("src.app.process_dataframe") as process_dataframe:
        response = client.post(
            "/upload-csv",
            files={"file": (filename, csv_content, "text/csv")},
        )

    assert response.status_code == 202
    assert response.json() == {
        "status": "Received",
        "filename": filename,
        "size": str(len(csv_content.encode("utf-8"))),
        "lines": 1,
    }
    process_dataframe.assert_called_once()
