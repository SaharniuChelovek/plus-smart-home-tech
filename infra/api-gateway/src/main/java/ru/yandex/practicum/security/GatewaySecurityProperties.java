package ru.yandex.practicum.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "gateway.security")
public record GatewaySecurityProperties(List<UserProps> users,
                                        List<String> allowedOrigins) {
    public record UserProps(String username, String password,
                            List<String> roles) {
    }
}
