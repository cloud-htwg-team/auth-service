terraform {
  required_providers {
    google = {
      source = "hashicorp/google"
      version = "4.47.0"
    }
    helm = {
      source = "hashicorp/helm"
      version = "2.8.0"
    }
  }
}

provider "google" {
  credentials = file("application_default_credentials.json")
  project = "qrcode-374515"
  region  = "europe-west3"
  zone    = "europe-west3-a"
}


provider "helm" {
  # Configuration options
  kubernetes {
    host                   = "https://${data.google_container_cluster.my_cluster.endpoint}"
  token = data.google_client_config.provider.access_token
  cluster_ca_certificate = base64decode(data.google_container_cluster.my_cluster.master_auth[0].cluster_ca_certificate)
  }
}


// data "google_service_account_access_token" "my_kubernetes_sa" {
//   provider = google
//   target_service_account = "terraform-service-account@qrcode-374515.iam.gserviceaccount.com"
//   scopes                 = ["userinfo-email", "cloud-platform"]
//   lifetime               = "3600s"
// }

data "google_client_config" "default" {}

data "google_container_cluster" "default" {
  #name     = "qr-code-app"
  name     = "qr-code-app"
  location = "europe-west3-a"
}

provider "kubernetes" {
  
    host  = "https://${data.google_container_cluster.default.endpoint}"
    token = data.google_client_config.default.access_token
    cluster_ca_certificate = base64decode(
      data.google_container_cluster.default.master_auth[0].cluster_ca_certificate,
    )
  
}


