provider "azurerm" {
  features {}
}

resource "azurerm_resource_group" "this" {
  name     = "rg-sparkbatch"
  location = var.location
}

module "acr" {
  source              = "git::https://github.com/ovokpus/GKE-Cluster-Management.git//terraform/modules/azure/acr?ref=tf-modules-v0.1.0"
  resource_group_name = azurerm_resource_group.this.name
  location            = var.location
  name                = "${var.name_prefix}acr"
}

module "env" {
  source              = "git::https://github.com/ovokpus/GKE-Cluster-Management.git//terraform/modules/azure/container-apps-env?ref=tf-modules-v0.1.0"
  resource_group_name = azurerm_resource_group.this.name
  location            = var.location
  name                = "cae-sparkbatch"
  acr_id              = module.acr.id
}

module "db" {
  source              = "git::https://github.com/ovokpus/GKE-Cluster-Management.git//terraform/modules/azure/postgres-flexible?ref=tf-modules-v0.1.0"
  resource_group_name = azurerm_resource_group.this.name
  location            = var.location
  name                = "${var.name_prefix}-pg"
  database            = "batch"
  admin_user          = "batch"
}

module "api" {
  source              = "git::https://github.com/ovokpus/GKE-Cluster-Management.git//terraform/modules/azure/container-app?ref=tf-modules-v0.1.0"
  resource_group_name = azurerm_resource_group.this.name
  name                = "sparkbatch-api"
  environment_id      = module.env.id
  identity_id         = module.env.identity_id
  registry_server     = module.acr.login_server
  image               = "${module.acr.login_server}/viewer:${var.image_tag}"
  port                = 8000
  env                 = {}
  secret_env = {
    DATABASE_URL = "postgresql://${module.db.user}:${module.db.password}@${module.db.host}:5432/${module.db.database}?sslmode=require"
  }
}

module "frontend" {
  source              = "git::https://github.com/ovokpus/GKE-Cluster-Management.git//terraform/modules/azure/container-app?ref=tf-modules-v0.1.0"
  resource_group_name = azurerm_resource_group.this.name
  name                = "sparkbatch-frontend"
  environment_id      = module.env.id
  identity_id         = module.env.identity_id
  registry_server     = module.acr.login_server
  image               = "${module.acr.login_server}/frontend:${var.image_tag}"
  port                = 80
  env = {
    API_UPSTREAM = module.api.url
  }
  secret_env = {}
}

module "batch" {
  source              = "git::https://github.com/ovokpus/GKE-Cluster-Management.git//terraform/modules/azure/container-app-job?ref=tf-modules-v0.1.0"
  resource_group_name = azurerm_resource_group.this.name
  location            = var.location
  name                = "sparkbatch-batch"
  environment_id      = module.env.id
  identity_id         = module.env.identity_id
  registry_server     = module.acr.login_server
  image               = "${module.acr.login_server}/batch:${var.image_tag}"
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
    SPRING_DATASOURCE_URL      = "jdbc:postgresql://${module.db.host}:5432/${module.db.database}?sslmode=require"
    SPRING_DATASOURCE_PASSWORD = module.db.password
  }
}
