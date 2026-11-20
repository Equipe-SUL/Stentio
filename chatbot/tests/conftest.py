import importlib
import os
import sys
from pathlib import Path
from unittest.mock import patch

import pytest

APP_DIR = Path(__file__).resolve().parents[1] / "app"
sys.path.insert(0, str(APP_DIR))

os.environ.setdefault("DATABASE_URL", "sqlite://")
os.environ.setdefault("GROQ_API_KEY", "pytest-key")
os.environ.setdefault("MONGO_URI", "mongodb://localhost:27017")
os.environ.setdefault("MONGO_DB_NAME", "stentio_chatbot_test")

with patch("pymongo.MongoClient"):
    chatbot_api = importlib.import_module("api.chatbot")


@pytest.fixture
def api_module():
    return chatbot_api
