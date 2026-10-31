variable "location" {
  type    = string
  default = "canadacentral"
}

variable "name_prefix" {
  description = "Lowercase letters/digits; used for globally unique names (ACR, Postgres)."
  type        = string
  default     = "sparkbatch"
}

variable "image_tag" {
  description = "Tag you pushed for every image (see README: Deploy)."
  type        = string
  default     = "v1"
}
