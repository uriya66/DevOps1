import requests

BASE_URL = "http://localhost:5000"

def test_health_check():
    response = requests.get(f"{BASE_URL}/health")  # Sends a request to the API
    assert response.status_code == 200  # Verify that the server returns 200 OK
    assert response.json() == {"status": "ok"}  # Verify that the content is correct
