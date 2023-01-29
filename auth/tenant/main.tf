/*
module "gke" {
  source     = "terraform-google-modules/kubernetes-engine/google//modules/private-cluster"
  project_id = var.project_id
  name       = var.cluster_name
  region     = var.region
  network    = var.network
  subnetwork = var.subnetwork

  ip_range_pods          = var.ip_range_pods
  ip_range_services      = var.ip_range_services
  create_service_account = false
  service_account        = var.compute_engine_service_account
}
*/
resource "google_storage_bucket" "static-site" {
  name          = "${var.namespace}-logo"
  location      = "europe-west3"
  storage_class = "REGIONAL"
  force_destroy = true
}

resource "kubernetes_namespace" "tenantx" {
  metadata {
    annotations = {
      name = var.namespace
    }

    labels = {
      namespace = var.namespace
    }

    name = var.namespace
  }
}

resource "google_dns_record_set" "tenantx" {
  name         = "${var.namespace}.qreach.adamradvan.eu."
  managed_zone = "adamradvan"
  type         = "CNAME"
  ttl          = 300

  rrdatas = ["qreach.adamradvan.eu."]
}
/*
resource "google_dns_managed_zone" "adamradvan" {
  name     = "adamradvan"
  dns_name = "adamradvan.eu."
}
*/


