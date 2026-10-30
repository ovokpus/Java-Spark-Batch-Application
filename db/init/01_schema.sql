-- PostgreSQL schema for the docker compose stack (the H2 equivalent lives in src/main/resources/schema.sql).
CREATE TABLE IF NOT EXISTS job_config (
  id SERIAL PRIMARY KEY,
  client_id VARCHAR(250) NOT NULL,
  job_name VARCHAR(250) NOT NULL,
  config_name VARCHAR(250) NOT NULL,
  value VARCHAR(250) NOT NULL
);

CREATE TABLE IF NOT EXISTS client_config (
  id SERIAL PRIMARY KEY,
  client_id VARCHAR(250),
  config_name VARCHAR(250),
  value VARCHAR(250)
);

CREATE TABLE IF NOT EXISTS sales (
  id SERIAL PRIMARY KEY,
  seller_id VARCHAR(250) NOT NULL,
  sales_date DATE NOT NULL,
  product VARCHAR(250) NOT NULL,
  quantity INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS sales_summary (
  summary_id SERIAL PRIMARY KEY,
  seller_id VARCHAR(250) NOT NULL,
  sales_date DATE NOT NULL,
  item VARCHAR(250) NOT NULL,
  total_quantity INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS job_run (
  id VARCHAR(36) PRIMARY KEY,
  job_name VARCHAR(250) NOT NULL,
  params VARCHAR(1000),
  status VARCHAR(20) NOT NULL,
  started_at TIMESTAMP NOT NULL,
  finished_at TIMESTAMP,
  rows_written BIGINT,
  error VARCHAR(1000)
);

-- IngesterJob reads its input folder from configuration (path inside the batch container).
INSERT INTO job_config (client_id, job_name, config_name, value)
VALUES ('client1', 'ingesterJob', 'inputPath', '/app/data/ingestion/');
