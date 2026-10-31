variable "region" {
  type    = string
  default = "us-east-1"
}

variable "image_tag" {
  description = "Tag you pushed for every image (see README: Deploy)."
  type        = string
  default     = "v1"
}
