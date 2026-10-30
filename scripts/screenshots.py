"""Capture README screenshots from the running stack (docker compose up). Real job runs, real data."""
import json
from pathlib import Path
from urllib.request import urlopen

from playwright.sync_api import sync_playwright

OUT = Path(__file__).resolve().parent.parent / "docs" / "screenshots"
OUT.mkdir(parents=True, exist_ok=True)

with sync_playwright() as p:
    b = p.chromium.launch()
    pg = b.new_page(viewport={"width": 1400, "height": 1000})
    pg.goto("http://localhost:8080"); pg.wait_for_selector("tbody tr"); pg.wait_for_load_state("networkidle"); pg.wait_for_timeout(800)
    pg.screenshot(path=OUT / "01-runs-dashboard.png", full_page=True)
    pg.locator("section.card:has-text('Run history')").screenshot(path=OUT / "02-run-history.png")
    b.close()

api = "http://localhost:8000"
samples = {path: json.load(urlopen(api + path)) for path in ["/health", "/api/runs?limit=3", "/api/summary"]}
samples["/api/summary"] = samples["/api/summary"][:4]
(OUT / "api-samples.json").write_text(json.dumps(samples, indent=2, default=str) + "\n")
print("ok")
