from fastapi.testclient import TestClient

from src.app import app

client = TestClient(app)


def test_upload_success():
    response = client.upload("nome,idade,cidade\nAlice,30,São Paulo\nBob,25,Rio de Janeiro")
    assert response.status_code == 200
    assert response.json() == {
        "filename": "usuarios.csv",
        "total_linhas": 2,
        "amostra_dados": [
            {"nome": "Alice", "idade": "30", "cidade": "São Paulo"},
            {"nome": "Bob", "idade": "25", "cidade": "Rio de Janeiro"},
        ],
    }
