variable "project_id" {
  type = string
}

variable "region" {
  type    = string
  default = "us-central1"
}

variable "image_tag" {
  description = "Tag you pushed for every image (see README: Deploy)."
  type        = string
  default     = "v1"
}
