/*output "client_token" {
  sensitive = true
  value     = base64encode(data.google_client_config.default.access_token)
}
*/
output "service_account" {
  description = "The default service account used for running nodes."
  value       = "iam-kube-account@qrcode-374515.iam.gserviceaccount.com"
}

output "kubernetes_namespace" {
  description = "The default service account used for running nodes."
  value       = var.namespace
}

output "google_storage_bucket" {
  description = "The default service account used for running nodes."
  value       = "qrcode-tenantx-logo"
}

output "google_dns_record_set" {
  description = "The default service account used for running nodes."
  value       = "${var.namespace}.qreach.adamradvan.eu."
}

