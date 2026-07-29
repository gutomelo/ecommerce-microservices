package com.ecommerce.platform.exception;

public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(String message) {
        super("RESOURCE_NOT_FOUND", message);
    }

    public static ResourceNotFoundException forId(String resourceName, Object id) {
        return new ResourceNotFoundException("%s nao encontrado(a) para o id: %s".formatted(resourceName, id));
    }
}
