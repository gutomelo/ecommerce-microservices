output "service_name" {
  value = aws_ecs_service.this.name
}

output "task_definition_arn" {
  value = aws_ecs_task_definition.this.arn
}

output "discovery_service_arn" {
  value = aws_service_discovery_service.this.arn
}

output "log_group_name" {
  value = aws_cloudwatch_log_group.this.name
}
