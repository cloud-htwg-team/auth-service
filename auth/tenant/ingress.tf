resource "kubernetes_ingress_v1" "tenant_ingress" {
  metadata {
    name = "${var.namespace}-ingress"
    namespace = kubernetes_namespace.tenantx.metadata[0].name
  }

  spec {
    ingress_class_name = "nginx"
    #backend {
    #  service_name = "myapp-1"
   #   service_port = 8080
    #}
    #}
    rule {
      host = "${var.namespace}.qreach.adamradvan.eu"
      http {
        path {
          backend {
            service{
              name = kubernetes_service.qrcode.metadata[0].name
              port {
                number = 8080
              }
          }
          }
          #path_type = "Prefix"
          path = "/secure/qr-code"
        
        }
        path {
          backend {
            service{
              name = kubernetes_service.qrhistory.metadata[0].name
              port {
                number = 8888
              }
          }
          }
          #path_type = "Prefix"
          path = "/secure/history"
        }
        path {
          backend {
            service{
              name = kubernetes_service.qrhistory.metadata[0].name
              port {
                number = 8888
              }
          }
          }
          #path_type = "Prefix"
          path = "/secure/history/*"
        }
      }
    }
    rule {
      host = "${var.namespace}.qreach.adamradvan.eu"
      http {
        path {
          backend {
            service{
              name = "front-service-from-${var.namespace}"
              port {
                number = 80
              }
          }
          }
          #path_type = "Prefix"
          path = "/"
        
        }
      path {
          backend {
            service{
              name = "auth-service-from-${var.namespace}"
              port {
                number = 80
              }
          }
          }
          #path_type = "Prefix"
          path = "/login"
        
        }
      }
    }
  }
}
