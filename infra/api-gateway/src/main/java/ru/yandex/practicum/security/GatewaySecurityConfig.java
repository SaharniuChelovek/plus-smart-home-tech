package ru.yandex.practicum.security;


import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.security.web.server.savedrequest.NoOpServerRequestCache;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebFluxSecurity
@EnableConfigurationProperties(GatewaySecurityProperties.class)
public class GatewaySecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(Customizer.withDefaults())          // подхватит бин corsConfigurationSource
                .httpBasic(Customizer.withDefaults())
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .logout(ServerHttpSecurity.LogoutSpec::disable)
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
                .requestCache(c -> c.requestCache(NoOpServerRequestCache.getInstance()))
                .authorizeExchange(ex -> ex
                        // публичные
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .pathMatchers(HttpMethod.GET,
                                "/api/products/**",
                                "/api/categories/**",
                                "/api/inventory/**").permitAll()

                        // пользовательские (by-email и {id} покрываются вторым правилом)
                        .pathMatchers(HttpMethod.GET, "/api/orders", "/api/orders/").hasRole("ADMIN")
                        .pathMatchers(HttpMethod.GET, "/api/orders/{id}").hasRole("USER")
                        .pathMatchers(HttpMethod.POST, "/api/orders/**").hasRole("USER")

                        // административные
                        .pathMatchers(HttpMethod.POST, "/api/products/**", "/api/categories/**", "/api/inventory/**").hasRole("ADMIN")
                        .pathMatchers(HttpMethod.PUT, "/api/products/**", "/api/categories/**", "/api/inventory/**").hasRole("ADMIN")
                        .pathMatchers(HttpMethod.PATCH, "/api/products/**", "/api/categories/**", "/api/inventory/**").hasRole("ADMIN")
                        .pathMatchers(HttpMethod.DELETE, "/api/products/**", "/api/categories/**", "/api/inventory/**").hasRole("ADMIN")

                        // всё остальное закрыто
                        .anyExchange().denyAll())
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public MapReactiveUserDetailsService userDetailsService(GatewaySecurityProperties props) {
        UserDetails[] users = props.users().stream()
                .map(u -> User.withUsername(u.username())
                        .password(u.password())            // уже с префиксом {bcrypt}/{noop}
                        .roles(u.roles().toArray(String[]::new))
                        .build())
                .toArray(UserDetails[]::new);
        return new MapReactiveUserDetailsService(users);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(GatewaySecurityProperties props) {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(props.allowedOrigins());
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setExposedHeaders(List.of("Location"));
        cfg.setAllowCredentials(true);
        cfg.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }
}
