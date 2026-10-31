# Business case: Java Spark Batch Application

**A Spring Boot and Spark template for nightly sales roll-ups, with a run history.**

## Who buys this

The buyer is the data-platform lead at a Java-first company (a retailer, distributor or bank) that wants batch jobs in the language its team already knows. The users are Java engineers writing jobs and analysts reading the summaries.

## The problem and what it costs

Batch jobs written as one-off scripts are hard to test, re-run and audit. A template with a fixed Reader, Processor and Writer pattern, a job-config table and a run log makes every new job look the same. Apache Spark provides the distributed engine ([Apache Spark](https://spark.apache.org/)). I found no reliable figure for the cost of inconsistent batch jobs, so the model uses assumptions.

## How the repo solves it

| Need | What in the repo does it |
|---|---|
| Ingest | `ingesterJob`: reads nested seller, day and item JSON and `flatMap`s it into `SALES` |
| Summarise | `salesSummaryJob`: filters by seller, date and product and writes totals to `SALES_SUMMARY` |
| Configure | A `JOB_CONFIG` table holds paths and parameters |
| Audit | `RunRecorder` writes every run, its parameters, status and error to `JOB_RUN`. Re-runs replace totals, so there are no duplicates. |
| Check | A FastAPI + React viewer compares Spark's totals with plain SQL |
| Deploy | Postgres 16, Docker, and Terraform for Cloud Run, Azure Container Apps and AWS ECS |

## Use-case scenarios

All of these organisations are fictional.

1. **A beverage distributor** rolls up each seller's daily sales by product every night.
2. **A Java-based retailer** gives new data engineers this template so each job has the same structure and run log.
3. **A finance team** uses the viewer's Spark-versus-SQL check as a reconciliation step before month end.

## ROI: a worked model (illustrative)

These figures are **illustrative assumptions** unless a source is linked.

| Input | Value | Basis |
|---|---|---|
| New batch jobs a year | 10 | Assumption |
| Days saved per job by starting from a tested template | 2 | Assumption |
| Engineer cost a day | US$665 | BLS May 2024 median for software developers, $63.98 an hour ([BLS OEWS](https://www.bls.gov/news.release/archives/ocwage_04022025.pdf)), multiplied by an assumed 1.3 for overhead, 8 hours |

Saving: 10 × 2 × $665 = **US$13,300 a year**, plus fewer duplicate-load incidents.

## Path to production

1. Point the job config at real sources, and move the database to a managed service.
2. Schedule runs with an orchestrator, and alert on `FAILED` rows in `JOB_RUN`.
3. Run on a Spark cluster when data outgrows one machine.

## Risks and limits

- This is a course template, and the sample data has 24 sales rows.
- Spark is heavy for small data. A plain SQL job may be enough.

**Case strength:** **Weak to moderate.** The pattern (repeatable jobs with a run log) has value, but the code is a small template.

## Sources

- Apache Spark project home page: https://spark.apache.org/
- U.S. Bureau of Labor Statistics, OEWS May 2024: https://www.bls.gov/news.release/archives/ocwage_04022025.pdf
