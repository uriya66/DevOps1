import requests
import pytest

# 🌍 Define the base URL for the Flask application
BASE_URL = "http://localhost:5000"

def test_health_check():
    """
    ✅ Test the /health endpoint to ensure the application is running correctly.
    """
    response = requests.get(f"{BASE_URL}/health")
    assert response.status_code == 200
    assert "✅ Application is Healthy!" in response.text

def test_home_page():
    """
    ✅ Test the root (/) endpoint to ensure it returns a valid response.
    """
    response = requests.get(BASE_URL)
    assert response.status_code == 200
    assert "Welcome to My Flask App!" in response.text

def test_404_page():
    """
    ❌ Test a non-existing page to verify the custom 404 page is returned.
    """
    response = requests.get(f"{BASE_URL}/nonexistentpage")
    assert response.status_code == 404
    assert "🚫 404 - Page Not Found" in response.text

def test_api_response_format():
    """
    ✅ Test the response format to ensure it is a valid JSON structure.
    """
    response = requests.get(f"{BASE_URL}/health")
    json_data = response.json()
    assert "status" in json_data
    assert json_data["status"] == "ok"
