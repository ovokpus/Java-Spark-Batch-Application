"""Read-only API over the batch job's PostgreSQL tables: run history, ingested sales and Spark summaries."""
from __future__ import annotations

import os
from contextlib import contextmanager

import psycopg
from fastapi import FastAPI
from psycopg.rows import dict_row

DATABASE_URL = os.environ.get("DATABASE_URL", "postgresql://batch:batch@localhost:5433/batch")
app = FastAPI(title="Spark batch results viewer")


@contextmanager
def cursor():
    with psycopg.connect(DATABASE_URL, row_factory=dict_row, connect_timeout=3) as conn:
        with conn.cursor() as cur:
            yield cur


def query(sql: str, params: tuple = ()) -> list[dict]:
    with cursor() as cur:
        cur.execute(sql, params)
        return cur.fetchall()


@app.get("/health")
def health():
    try:
        counts = query("SELECT (SELECT COUNT(*) FROM sales) AS sales, (SELECT COUNT(*) FROM sales_summary) AS summaries,"
                       " (SELECT COUNT(*) FROM job_run) AS runs")[0]
        return {"status": "ok", "database": True, **counts}
    except Exception as e:  # surfaced in the UI header
        return {"status": "degraded", "database": False, "error": type(e).__name__}


@app.get("/api/runs")
def runs(limit: int = 50):
    rows = query("SELECT id, job_name, params, status, started_at, finished_at, rows_written, error,"
                 " EXTRACT(EPOCH FROM (finished_at - started_at)) AS seconds"
                 " FROM job_run ORDER BY started_at DESC LIMIT %s", (min(max(limit, 1), 500),))
    for r in rows:
        r["seconds"] = float(r["seconds"]) if r["seconds"] is not None else None
    return rows


@app.get("/api/sales")
def sales():
    return query("SELECT seller_id, sales_date, product, quantity FROM sales ORDER BY sales_date, seller_id, product, id")


@app.get("/api/summary")
def summary():
    """Spark's SALES_SUMMARY next to the same totals computed directly in SQL, so the job can be checked."""
    rows = query(
        "SELECT s.seller_id, s.sales_date, s.item, s.total_quantity,"
        " (SELECT SUM(quantity) FROM sales x WHERE x.seller_id = s.seller_id AND x.sales_date = s.sales_date"
        "  AND x.product = s.item) AS sql_total"
        " FROM sales_summary s ORDER BY s.sales_date, s.item, s.seller_id")
    for r in rows:
        r["sql_total"] = int(r["sql_total"]) if r["sql_total"] is not None else None
        r["matches"] = r["sql_total"] == r["total_quantity"]
    return rows
