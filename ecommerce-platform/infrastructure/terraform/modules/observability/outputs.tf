output "internal_alb_dns_name" {
  description = "DNS do ALB interno - so alcancavel de dentro da VPC ou de var.admin_cidr_blocks (VPN/bastion)"
  value       = aws_lb.internal.dns_name
}

output "grafana_url" {
  value = "http://${aws_lb.internal.dns_name}:3000"
}

output "prometheus_url" {
  value = "http://${aws_lb.internal.dns_name}:9090"
}

output "jaeger_url" {
  value = "http://${aws_lb.internal.dns_name}:16686"
}

output "grafana_admin_password_secret_arn" {
  value = aws_secretsmanager_secret.grafana_admin.arn
}
