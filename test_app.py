"""
This script contains automated tests for the Flask application.
It verifies the availability of key API endpoints.
"""
import requests
import pytest

# Base URL of the Flask application
BASE_URL = "http://localhost:5000"

def test_health_check():
    """
    Test the /health endpoint to ensure the application is running correctly.
    """
    response = requests.get(f"{BASE_URL}/health")  # Send a request to the API
    assert response.status_code == 200  # Ensure the server returns HTTP 200 OK
    assert "✅ Status: OK" in response.text  # Verify the expected output in the new HTML design

def test_home_page():
    """
    Test the root (/) endpoint to ensure it returns a valid response.
    """
    response = requests.get(BASE_URL)  # Send a request to the root endpoint
    assert response.status_code == 200  # Ensure the response is HTTP 200 OK
    assert "Welcome to My Flask App!" in response.text  # Verify the expected output in the new HTML design

def test_404_page():
    """
    Test a non-existing page to verify the custom 404 page is returned.
    """
    response = requests.get(f"{BASE_URL}/nonexistentpage")  # Request a non-existing page
    assert response.status_code == 404  # Ensure the server returns HTTP 404 Not Found
    assert "🚫 404 - Page Not Found" in response.text  # Verify the expected output in the custom 404 page

def test_navigation_links():
    """
    Test that the navigation links exist in the home and health pages.
    """
    home_response = requests.get(BASE_URL)  # Get the home page
    health_response = requests.get(f"{BASE_URL}/health")  # Get the health page

    assert "🏠 Home" in home_response.text  # Ensure the navbar contains 'Home'
    assert "✅ Health Check" in home_response.text  # Ensure the navbar contains 'Health Check'
    assert "🚫 404 Test" in home_response.text  # Ensure the navbar contains '404 Test'

    assert "🏠 Home" in health_response.text  # Ensure the navbar contains 'Home' on the health page
    assert "✅ Health Check" in health_response.text  # Ensure the navbar contains 'Health Check' on the health page
    assert "🚫 404 Test" in health_response.text  # Ensure the navbar contains '404 Test' on the health page
