import requests

BASE_URL = "http://localhost:5000"

def test_health_check():
    response = requests.get(f"{BASE_URL}/health")  # שולח בקשה ל-API
    assert response.status_code == 200  # מוודא שהשרת מחזיר 200 OK
    assert response.json() == {"status": "ok"}  # מוודא שהתוכן נכון
