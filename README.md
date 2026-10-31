# ⚡ Java Spark Batch Application

**A Spark (Java API) batch template, now running end to end with one command, with run history you can see.**

`docker compose up` → PostgreSQL → **Spark ingestion job** (JSON → `SALES`) → **9 Spark summary jobs** (per seller, date and product → `SALES_SUMMARY`) → a read-only **FastAPI + React** viewer on `:8080` that shows every run and checks Spark's totals against plain SQL.

![Run dashboard](docs/screenshots/01-runs-dashboard.png)

---

## 1. The project

This is a template from the *Apache Spark for Java Developers* course ([original README](docs/ORIGINAL_README.md)). It's a Spring Boot command-line app where each job follows the **Reader → Processor → Writer** pattern on Spark:

| Job | Reads | Spark work | Writes |
|---|---|---|---|
| `ingesterJob` | `data/ingestion/sales.json` (path from the `JOB_CONFIG` table) | `flatMap` explodes nested seller → day → items into rows | `SALES` |
| `salesSummaryJob` | `SALES` through Spark JDBC, filtered by `sellerId`, `date` and `product` | `filter` + `reduce` per seller | `SALES_SUMMARY` |

Originally it only ran inside the course sandbox (`legacy/input.sh`), against an in-memory H2 database that disappeared after each run.

## 2. Architecture

```mermaid
flowchart LR
    J[sales.json] --> I[ingesterJob<br/>Spark flatMap]
    I --> S[(SALES)]
    S -->|Spark JDBC read| M[salesSummaryJob<br/>filter + reduce]
    M --> SS[(SALES_SUMMARY)]
    I & M -.->|RunRecorder| R[(JOB_RUN)]
    subgraph PostgreSQL 16
      S
      SS
      R
      C[(JOB_CONFIG)]
    end
    C -->|inputPath| I
    S & SS & R --> A[FastAPI viewer<br/>:8000] --> U[React UI<br/>nginx :8080]
```

```mermaid
sequenceDiagram
    participant C as docker compose
    participant B as batch container (JRE 11)
    participant P as PostgreSQL
    C->>P: start, run db/init (schema + JOB_CONFIG)
    C->>B: run-pipeline (after the DB is healthy)
    B->>P: ingesterJob mode=overwrite → 24 rows in SALES
    loop 3 dates × 3 products
      B->>P: salesSummaryJob → replace (seller, date, item) totals
    end
    B-->>C: exit 0 (every run is recorded in JOB_RUN)
```

## 3. Quick start

```bash
cp .env.example .env          # local-only defaults, no secrets
docker compose up -d --build  # db + batch (runs once, then exits) + api + frontend
open http://localhost:8080
docker compose logs batch     # Spark output for each job
```

Run one job yourself (it shows up in the run history):

```bash
docker compose run --rm batch jobName=salesSummaryJob sellerId=Joe,Beth date=20201111000000 product=Beer
docker compose run --rm batch      # re-run the whole pipeline (idempotent)
docker compose down                # add -v to reset the database
```

Without Docker (JDK 11 + Maven): `mvn test` runs every job on local Spark against in-memory H2.

## 4. Real results

The screenshots and samples below come from the stack running on this branch. The data is the sample sales file that came with the original course project.

| Run history | |
|---|---|
| ![runs](docs/screenshots/02-run-history.png) | **12 runs:** 1 ingestion + 9 summaries from the pipeline, 1 manual re-run (same totals, no duplicates), and 1 deliberately invalid date (`2020-13-45`), recorded as **FAILED** with its error message. All **19/19** Spark totals match `SUM(quantity)` computed directly in SQL. Each job takes about 2 seconds on local Spark. |

```json
GET /api/runs?limit=1
{ "job_name": "salesSummaryJob", "params": "jobName=salesSummaryJob sellerId=Joe date=2020-13-45 product=Beer",
  "status": "FAILED", "error": "IllegalArgumentException: Invalid date '2020-13-45' (expected yyyyMMddHHmmss or yyyy-MM-dd)" }
```

More samples are in [docs/screenshots/api-samples.json](docs/screenshots/api-samples.json).

## 5. Viewer API (read-only, no auth)

| Endpoint | Returns |
|---|---|
| `GET /health` | DB status plus row counts (`degraded` if Postgres is unreachable) |
| `GET /api/runs?limit=` | `JOB_RUN`: job, parameters, status, rows written, duration, error |
| `GET /api/sales` | the ingested `SALES` rows |
| `GET /api/summary` | `SALES_SUMMARY`, with a `sql_total` and a `matches` flag for each row |

## Deploy to the cloud (Terraform)

`terraform/` has the same deployment for three clouds: **Google Cloud Run**, **Azure Container Apps** and **AWS ECS Express Mode**. Each is a root module that calls the shared modules in [ovokpus/GKE-Cluster-Management](https://github.com/ovokpus/GKE-Cluster-Management/tree/main/terraform), pinned to tag `tf-modules-v0.1.0`. Each folder deploys `api`, `frontend`, a `batch` job and a managed Postgres 16 database.

> [!CAUTION]
> **This costs real money.** The database bills by the hour even when idle (Cloud SQL, Postgres Flexible Server, RDS). On AWS, each ECS Express service runs at least one Fargate task plus a share of an Application Load Balancer, all the time. Cloud Run and Container Apps scale to zero, but their registries and logs still cost a little. Use a sandbox project or account, set a budget alert, and run `terraform destroy` when you finish. Database passwords are generated by Terraform, so they are also stored in its state file. Keep state somewhere private (each `versions.tf` has a commented remote backend).

CI (`.github/workflows/terraform.yml`) only runs `fmt`, `validate`, `tflint` and `checkov`. It never runs plan or apply.

### Google Cloud (Cloud Run)

```mermaid
flowchart LR
    user([Browser])
    subgraph cloud["Google Cloud"]
        reg[(Artifact Registry)]
        sec[Secret Manager]
        api["api<br/>Cloud Run"]
        frontend["frontend<br/>Cloud Run"]
        batch["batch job<br/>Cloud Run job"]
        db[("Cloud SQL Postgres 16<br/>(private IP)")]
    end
    user --> frontend --> api
    api --> db
    batch --> db
    reg -. images .-> frontend
    sec -. secrets .-> api
```

```bash
gcloud auth application-default login
gcloud config set project my-gcp-project
cd terraform/gcp
cp terraform.tfvars.example terraform.tfvars   # edit it
terraform init

# 1. Create the registry first, then push the images
terraform apply -target=module.registry
REG=$(terraform output -raw registry)
gcloud auth configure-docker "${REG%%/*}"
cd ../..
docker build --platform linux/amd64 -t $REG/viewer:v1 viewer
docker push $REG/viewer:v1
docker build --platform linux/amd64 -t $REG/frontend:v1 frontend
docker push $REG/frontend:v1
docker build --platform linux/amd64 -t $REG/batch:v1 .
docker push $REG/batch:v1
cd terraform/gcp

# 2. Everything else
terraform apply

# 3. Open the app
terraform output frontend_url

# 4. Run the batch job once
eval "$(terraform output -raw batch_command)"

# Tear it all down
terraform destroy
```

### Azure (Container Apps)

```mermaid
flowchart LR
    user([Browser])
    subgraph cloud["Azure"]
        reg[(ACR)]
        sec[Container Apps secrets]
        api["api<br/>Container Apps"]
        frontend["frontend<br/>Container Apps"]
        batch["batch job<br/>Container Apps job"]
        db[("Postgres Flexible Server 16")]
    end
    user --> frontend --> api
    api --> db
    batch --> db
    reg -. images .-> frontend
    sec -. secrets .-> api
```

```bash
az login
az account set --subscription <subscription-id>
cd terraform/azure
cp terraform.tfvars.example terraform.tfvars   # edit it
# name_prefix must be globally unique (it names the registry): 3-20 lowercase letters/digits
terraform init

# 1. Create the registry first, then push the images
terraform apply -target=module.acr
REG=$(terraform output -raw registry)
az acr login --name "${REG%%.*}"
cd ../..
docker build --platform linux/amd64 -t $REG/viewer:v1 viewer
docker push $REG/viewer:v1
docker build --platform linux/amd64 -t $REG/frontend:v1 frontend
docker push $REG/frontend:v1
docker build --platform linux/amd64 -t $REG/batch:v1 .
docker push $REG/batch:v1
cd terraform/azure

# 2. Everything else
terraform apply

# 3. Open the app
terraform output frontend_url

# 4. Run the batch job once
eval "$(terraform output -raw batch_command)"

# Tear it all down
terraform destroy
```

### AWS (ECS Express Mode)

```mermaid
flowchart LR
    user([Browser])
    subgraph cloud["AWS"]
        reg[(ECR)]
        sec[Secrets Manager]
        api["api<br/>ECS Express Mode<br/>(Fargate + ALB)"]
        frontend["frontend<br/>ECS Express Mode<br/>(Fargate + ALB)"]
        batch["batch job<br/>Fargate task"]
        db[("RDS Postgres 16<br/>(private)")]
    end
    user --> frontend --> api
    api --> db
    batch --> db
    reg -. images .-> frontend
    sec -. secrets .-> api
```

```bash
aws configure sso   # or any profile with admin rights in a sandbox account
export AWS_PROFILE=<profile>
cd terraform/aws
cp terraform.tfvars.example terraform.tfvars   # edit it
terraform init

# 1. Create the registry first, then push the images
terraform apply -target=module.ecr
REG=$(terraform output -raw registry)
aws ecr get-login-password --region "${AWS_REGION:-us-east-1}" \
  | docker login --username AWS --password-stdin "$REG"
cd ../..
docker build --platform linux/amd64 -t $REG/sparkbatch/viewer:v1 viewer
docker push $REG/sparkbatch/viewer:v1
docker build --platform linux/amd64 -t $REG/sparkbatch/frontend:v1 frontend
docker push $REG/sparkbatch/frontend:v1
docker build --platform linux/amd64 -t $REG/sparkbatch/batch:v1 .
docker push $REG/sparkbatch/batch:v1
cd terraform/aws

# 2. Everything else
terraform apply

# 3. Open the app
terraform output frontend_url

# 4. Run the batch job once
eval "$(terraform output -raw batch_command)"

# Tear it all down
terraform destroy
```

## 6. What changed on this branch

**Running it**
- Multi-stage `Dockerfile`: Maven build, then a JRE 11 image that runs on an exploded classpath.
- `docker/run-pipeline.sh` runs the whole pipeline, or one job when you pass arguments.
- `docker-compose.yml` brings up Postgres, the batch job, the API and the UI, using **PostgreSQL** in place of throwaway H2. H2 is still the default for `mvn test`.
- The Java build moved from 1.8 to **11**.

**Bugs fixed**
- The writers always reported **0 rows**, because the partition mappers returned an iterator they had already used up.
- `salesSummaryJob` **crashed** when a seller had no sales for the date/product, because `reduce` ran on an empty Dataset.
- The SQL relied on H2-only `PARSEDATETIME`. It now uses portable `DATE 'yyyy-mm-dd'`, and the date is checked in Java first.
- Quotes in parameters are escaped, so a value can't break out of the SQL string.
- Spark JDBC column names are upper-cased, because PostgreSQL returns them in lower case and `Row.getAs` is case-sensitive.
- A new connection pool was created on every partition. There's now one per JVM.
- The app crashed when started without `jobName=` (which also broke `contextLoads`). It now prints usage and exits.
- The process didn't exit after Spark finished. It now stops the session and exits with the right code.

**New features**
- `JOB_RUN` run history, written by `RunRecorder`, with rows written from `JobMetrics`.
- `mode=overwrite` for ingestion, and idempotent summaries, so re-running doesn't double count.
- Spark settings for small local jobs: 4 shuffle partitions and no Spark UI port.

**Tests**
- **10 JUnit tests**, including an end-to-end pipeline test on real local Spark.
- The commented-out reducer test was moved from `src/main` to `src/test` and switched back on.
- 3 pytest tests for the viewer.
- CI runs the Maven tests, the viewer tests and the frontend build, then `docker compose build`.

**Moves, no deletes**
- `README.md` → `docs/ORIGINAL_README.md`
- `input.sh` → `legacy/`
- the committed `target/` build output → `legacy/target-committed-build-output/`

**Config**
- `.env.example` holds local defaults only.
- There's no `PRODUCTIONIZING.md`, because the repo has no notebooks.
