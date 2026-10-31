provider "aws" {
  region = var.region
  default_tags {
    tags = { app = "Java-Spark-Batch-Application", managed-by = "terraform" }
  }
}

# Default VPC keeps the example small. Use a dedicated VPC for production.
data "aws_vpc" "default" {
  default = true
}

data "aws_subnets" "default" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.default.id]
  }
}

resource "aws_security_group" "app" {
  name_prefix = "sparkbatch-app-"
  description = "Java-Spark-Batch-Application tasks"
  vpc_id      = data.aws_vpc.default.id
}

resource "aws_vpc_security_group_ingress_rule" "app_80" {
  security_group_id = aws_security_group.app.id
  description       = "load balancer to tasks"
  ip_protocol       = "tcp"
  from_port         = 80
  to_port           = 80
  cidr_ipv4         = data.aws_vpc.default.cidr_block
}

resource "aws_vpc_security_group_ingress_rule" "app_8000" {
  security_group_id = aws_security_group.app.id
  description       = "load balancer to tasks"
  ip_protocol       = "tcp"
  from_port         = 8000
  to_port           = 8000
  cidr_ipv4         = data.aws_vpc.default.cidr_block
}

resource "aws_vpc_security_group_egress_rule" "app_all" {
  security_group_id = aws_security_group.app.id
  description       = "outbound (image pulls, database, external APIs)"
  ip_protocol       = "-1"
  cidr_ipv4         = "0.0.0.0/0"
}

resource "aws_ecs_cluster" "this" {
  name = "sparkbatch"
  setting {
    name  = "containerInsights"
    value = "enabled"
  }
}

resource "aws_secretsmanager_secret" "dburl_py" {
  name_prefix = "sparkbatch-dburl-py-"
}

resource "aws_secretsmanager_secret_version" "dburl_py" {
  secret_id     = aws_secretsmanager_secret.dburl_py.id
  secret_string = "postgresql://${module.db.user}:${module.db.password}@${module.db.host}:5432/${module.db.database}?sslmode=require"
}

resource "aws_secretsmanager_secret" "dburl_jdbc" {
  name_prefix = "sparkbatch-dburl-jdbc-"
}

resource "aws_secretsmanager_secret_version" "dburl_jdbc" {
  secret_id     = aws_secretsmanager_secret.dburl_jdbc.id
  secret_string = "jdbc:postgresql://${module.db.host}:5432/${module.db.database}?sslmode=require"
}

resource "aws_secretsmanager_secret" "dbpass" {
  name_prefix = "sparkbatch-dbpass-"
}

resource "aws_secretsmanager_secret_version" "dbpass" {
  secret_id     = aws_secretsmanager_secret.dbpass.id
  secret_string = module.db.password
}

module "ecr" {
  source = "git::https://github.com/ovokpus/GKE-Cluster-Management.git//terraform/modules/aws/ecr?ref=tf-modules-v0.1.0"
  prefix = "sparkbatch"
  names  = ["batch", "frontend", "viewer"]
}

module "db" {
  source                = "git::https://github.com/ovokpus/GKE-Cluster-Management.git//terraform/modules/aws/rds-postgres?ref=tf-modules-v0.1.0"
  name                  = "sparkbatch-pg"
  database              = "batch"
  user                  = "batch"
  vpc_id                = data.aws_vpc.default.id
  subnet_ids            = data.aws_subnets.default.ids
  app_security_group_id = aws_security_group.app.id
}

module "api" {
  source             = "git::https://github.com/ovokpus/GKE-Cluster-Management.git//terraform/modules/aws/express-service?ref=tf-modules-v0.1.0"
  name               = "sparkbatch-api"
  cluster            = aws_ecs_cluster.this.name
  image              = "${module.ecr.repository_urls["viewer"]}:${var.image_tag}"
  port               = 8000
  health_path        = "/health"
  subnet_ids         = data.aws_subnets.default.ids
  security_group_ids = [aws_security_group.app.id]
  env                = {}
  secret_env = {
    DATABASE_URL = aws_secretsmanager_secret.dburl_py.arn
  }
}

module "frontend" {
  source             = "git::https://github.com/ovokpus/GKE-Cluster-Management.git//terraform/modules/aws/express-service?ref=tf-modules-v0.1.0"
  name               = "sparkbatch-frontend"
  cluster            = aws_ecs_cluster.this.name
  image              = "${module.ecr.repository_urls["frontend"]}:${var.image_tag}"
  port               = 80
  health_path        = "/"
  subnet_ids         = data.aws_subnets.default.ids
  security_group_ids = [aws_security_group.app.id]
  env = {
    API_UPSTREAM = module.api.url
  }
  secret_env = {}
}

module "batch" {
  source             = "git::https://github.com/ovokpus/GKE-Cluster-Management.git//terraform/modules/aws/fargate-job?ref=tf-modules-v0.1.0"
  name               = "sparkbatch-batch"
  cluster            = aws_ecs_cluster.this.name
  image              = "${module.ecr.repository_urls["batch"]}:${var.image_tag}"
  subnet_ids         = data.aws_subnets.default.ids
  security_group_ids = [aws_security_group.app.id]
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
    SPRING_DATASOURCE_URL      = aws_secretsmanager_secret.dburl_jdbc.arn
    SPRING_DATASOURCE_PASSWORD = aws_secretsmanager_secret.dbpass.arn
  }
}
