output "dns_name" {
  value = aws_lb.this.dns_name
}

output "target_group_arn" {
  value = aws_lb_target_group.gateway.arn
}

output "url" {
  value = local.enable_https ? "https://${var.domain_name}" : "http://${aws_lb.this.dns_name}"
}
