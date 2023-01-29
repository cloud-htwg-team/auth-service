variable "project_id" {
  description = "The project ID to host the cluster in"
  default   = "qrcode-374515"
}

variable "cluster_name" {
  description = "A suffix to append to the default cluster name"
  #default     = "qr-code-app"
  default     = "qr-code-app"
}

variable "region" {
  description = "The region to host the cluster in"
    default     = "europe-west3"
}


variable "porthis" {
  description = "The port to expose the service on"
  default     = "8888"
}

variable "portqr" {
  description = "The port to expose the service on"
  default     = "8080"
}

variable "namespace" {
  description = "The namespace to deploy the service into"
  default     = "tenanty"
}
