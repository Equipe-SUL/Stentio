from unittest.mock import Mock

import pytest
from db import session


def test_get_db_yields_session_and_closes_it(monkeypatch):
    # Arrange
    db = Mock()
    monkeypatch.setattr(session, "SessionLocal", Mock(return_value=db))
    dependency = session.get_db()

    # Act
    yielded_db = next(dependency)
    with pytest.raises(StopIteration):
        next(dependency)

    # Assert
    assert yielded_db is db
    db.close.assert_called_once()


def test_get_db_closes_session_when_consumer_raises(monkeypatch):
    # Arrange
    db = Mock()
    monkeypatch.setattr(session, "SessionLocal", Mock(return_value=db))
    dependency = session.get_db()
    next(dependency)

    # Act / Assert
    with pytest.raises(RuntimeError, match="query failed"):
        dependency.throw(RuntimeError("query failed"))
    db.close.assert_called_once()
