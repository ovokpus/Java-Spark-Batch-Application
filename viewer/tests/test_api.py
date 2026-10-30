from datetime import date, datetime

from fastapi.testclient import TestClient

from api import main

client = TestClient(main.app)


def test_health_degraded_without_database(monkeypatch):
    def boom(*a, **k):
        raise ConnectionError("no db")
    monkeypatch.setattr(main, "query", boom)
    body = client.get("/health").json()
    assert body == {"status": "degraded", "database": False, "error": "ConnectionError"}


def test_summary_flags_mismatches(monkeypatch):
    monkeypatch.setattr(main, "query", lambda *a, **k: [
        {"seller_id": "Joe", "sales_date": date(2020, 11, 12), "item": "Beer", "total_quantity": 49, "sql_total": 49},
        {"seller_id": "Beth", "sales_date": date(2020, 11, 12), "item": "Beer", "total_quantity": 10, "sql_total": 15},
    ])
    rows = client.get("/api/summary").json()
    assert [r["matches"] for r in rows] == [True, False]


def test_runs_limit_is_clamped_and_seconds_float(monkeypatch):
    seen = {}

    def fake(sql, params=()):
        seen["params"] = params
        return [{"id": "1", "job_name": "ingesterJob", "params": "", "status": "SUCCEEDED",
                 "started_at": datetime(2026, 1, 1), "finished_at": datetime(2026, 1, 1, 0, 0, 9),
                 "rows_written": 24, "error": None, "seconds": 9}]
    monkeypatch.setattr(main, "query", fake)
    body = client.get("/api/runs?limit=100000").json()
    assert seen["params"] == (500,)
    assert body[0]["seconds"] == 9.0
