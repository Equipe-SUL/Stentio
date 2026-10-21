from app import app
from fastapi.testclient import TestClient

client = TestClient(app)

def test_upload_success():
    csv_content = "nome,idade,cidade\nAlice,30,São Paulo\nBob,25,Rio de Janeiro"

    response = client.post(
        "/upload-csv/",
        files={"file": ("usuarios.csv", csv_content, "text/csv")}
    )

    assert response.status_code == 200
    
    data = response.json()
    assert data["filename"] == "usuarios.csv"
    assert data["total_linhas"] == 2
    assert data["amostra_dados"][0]["nome"] == "Alice"
    assert data["amostra_dados"][1]["cidade"] == "Rio de Janeiro"


def test_upload_invalid_file_extension():
    # Testa a validação de arquivo que não seja .csv
    txt_content = "este é um arquivo txt simples"
    
    response = client.post(
        "/upload-csv/",
        files={"file": ("texto.txt", txt_content, "text/plain")}
    )
    
    # Valida se retornou o erro 400 que definimos no endpoint
    assert response.status_code == 400
    assert response.json()["detail"] == "Apenas arquivos .csv são permitidos."