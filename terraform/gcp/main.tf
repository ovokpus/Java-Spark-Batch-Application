provider "google" {
  project = var.project_id
  region  = var.region
}

resource "google_project_service" "secretmanager" {
  service            = "secretmanager.googleapis.com"
  disable_on_destroy = false
}

resource "google_secret_manager_secret" "dburl_py" {
  secret_id = "sparkbatch-dburl-py"
  replication {
    auto {}
  }
  depends_on = [google_project_service.secretmanager]
}

resource "google_secret_manager_secret_version" "dburl_py" {
  secret      = google_secret_manager_secret.dburl_py.id
  secret_data = "postgresql://${module.db.user}:${module.db.password}@${module.db.private_ip}:5432/${module.db.database}?sslmode=require"
}

resource "google_secret_manager_secret" "dburl_jdbc" {
  secret_id = "sparkbatch-dburl-jdbc"
  replication {
    auto {}
  }
  depends_on = [google_project_service.secretmanager]
}

resource "google_secret_manager_secret_version" "dburl_jdbc" {
  secret      = google_secret_manager_secret.dburl_jdbc.id
  secret_data = "jdbc:postgresql://${module.db.private_ip}:5432/${module.db.database}?sslmode=require"
}

resource "google_secret_manager_secret" "dbpass" {
  secret_id = "sparkbatch-dbpass"
  replication {
    auto {}
  }
  depends_on = [google_project_service.secretmanager]
}

resource "google_secret_manager_secret_version" "dbpass" {
  secret      = google_secret_manager_secret.dbpass.id
  secret_data = module.db.password
}

module "registry" {
  source        = "git::https://github.com/ovokpus/GKE-Cluster-Management.git//terraform/modules/gcp/artifact-registry?ref=tf-modules-v0.1.0"
  project_id    = var.project_id
  region        = var.region
  repository_id = "sparkbatch"
}

module "network" {
  source     = "git::https://github.com/ovokpus/GKE-Cluster-Management.git//terraform/modules/gcp/network?ref=tf-modules-v0.1.0"
  project_id = var.project_id
  region     = var.region
  name       = "sparkbatch"
}

module "db" {
  source                      = "git::https://github.com/ovokpus/GKE-Cluster-Management.git//terraform/modules/gcp/cloud-sql-postgres?ref=tf-modules-v0.1.0"
  project_id                  = var.project_id
  region                      = var.region
  name                        = "sparkbatch-pg"
  network_id                  = module.network.network_id
  private_services_connection = module.network.private_services_connection
  database                    = "batch"
  user                        = "batch"
}

module "api" {
  source      = "git::https://github.com/ovokpus/GKE-Cluster-Management.git//terraform/modules/gcp/cloud-run-service?ref=tf-modules-v0.1.0"
  project_id  = var.project_id
  region      = var.region
  name        = "sparkbatch-api"
  image       = "${module.registry.repository_url}/viewer:${var.image_tag}"
  port        = 8000
  memory      = "512Mi"
  health_path = "/health"
  vpc         = { network = module.network.network_id, subnet = module.network.subnet_id }
  env         = {}
  secret_env = {
    DATABASE_URL = google_secret_manager_secret.dburl_py.secret_id
  }
}

module "frontend" {
  source      = "git::https://github.com/ovokpus/GKE-Cluster-Management.git//terraform/modules/gcp/cloud-run-service?ref=tf-modules-v0.1.0"
  project_id  = var.project_id
  region      = var.region
  name        = "sparkbatch-frontend"
  image       = "${module.registry.repository_url}/frontend:${var.image_tag}"
  port        = 80
  memory      = "512Mi"
  health_path = "/"
  env = {
    API_UPSTREAM = module.api.url
  }
  secret_env = {}
}

module "batch" {
  source     = "git::https://github.com/ovokpus/GKE-Cluster-Management.git//terraform/modules/gcp/cloud-run-job?ref=tf-modules-v0.1.0"
  project_id = var.project_id
  region     = var.region
  name       = "sparkbatch-batch"
  image      = "${module.registry.repository_url}/batch:${var.image_tag}"
  memory     = "2Gi"
  vpc        = { network = module.network.network_id, subnet = module.network.subnet_id }
  env = {
    SPRING_DATASOURCE_USERNAME          = "batch"
    SPRING_DATASOURCE_DRIVER_CLASS_NAME = "org.postgresql.Driver"
    SPRING_JPA_HIBERNATE_DDL_AUTO       = "none"
    SPARK_MASTER_MODE                   = "local[*]"
    SELLERS                             = "Joe,Beth,Janet"
    DATES                               = "20201111000000 20201112000000 20201113000000"
    PRODUCTS                            = "Beer Food Car"
  }
  secret_env = {
    SPRING_DATASOURCE_URL      = google_secret_manager_secret.dburl_jdbc.secret_id
    SPRING_DATASOURCE_PASSWORD = google_secret_manager_secret.dbpass.secret_id
  }
}
