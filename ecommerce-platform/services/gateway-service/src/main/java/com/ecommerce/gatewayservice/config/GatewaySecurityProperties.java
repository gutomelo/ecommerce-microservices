package com.ecommerce.gatewayservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Paths que nao exigem JWT (ver JwtAuthenticationGlobalFilter). /auth/** e publico
 * por definicao (login/registro/refresh) - o restante exige token valido.
 */
@Component
@Getter
@Setter
@ConfigurationProperties(prefix = "gateway.security")
public class GatewaySecurityProperties {

    private List<String> publicPaths = new ArrayList<>(List.of("/auth/**", "/actuator/**"));
}
