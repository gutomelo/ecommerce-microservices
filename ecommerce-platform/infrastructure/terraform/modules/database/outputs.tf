output "secret_arns" {
  description = "Mapa nome-do-servico -> ARN do Secrets Manager (chaves DB_HOST/DB_PORT/DB_NAME/DB_USERNAME/DB_PASSWORD)"
  value       = { for name, secret in aws_secretsmanager_secret.this : name => secret.arn }
}

output "db_endpoints" {
  value = { for name, db in aws_db_instance.this : name => db.address }
}

output "rds_security_group_id" {
  value = aws_security_group.rds.id
}
