output "registry" {
  description = "Push images here (see README: Deploy)."
  value       = module.acr.login_server
}

output "api_url" {
  value = module.api.url
}

output "frontend_url" {
  value = module.frontend.url
}

output "batch_command" {
  value = module.batch.start_command
}
