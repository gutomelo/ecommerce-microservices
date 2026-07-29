output "vpc_id" {
  value = aws_vpc.this.id
}

output "vpc_cidr_block" {
  value = aws_vpc.this.cidr_block
}

output "public_subnet_ids" {
  value = aws_subnet.public[*].id
}

output "private_subnet_ids" {
  value = aws_subnet.private[*].id
}

output "alb_security_group_id" {
  description = "SG do ALB publico - usar como security_groups do ALB do gateway-service"
  value       = aws_security_group.alb_public.id
}

output "ecs_security_group_id" {
  description = "SG compartilhado por todas as tasks ECS Fargate"
  value       = aws_security_group.ecs_tasks.id
}
