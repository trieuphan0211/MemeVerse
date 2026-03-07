package vn.stephenphan.memeservice.config;

import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Collection;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@Log4j2
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers("/api/v1/memes/public/**").permitAll()
                        .requestMatchers("/api/v1/memes/*/comments").permitAll()
                        .requestMatchers("/api/v1/comments/*/replies").permitAll()
                        .requestMatchers("/api/v1/memes/*/vote").authenticated()
                        // Authenticated endpoints
                        .requestMatchers("/api/v1/memes/following").authenticated()
                        .requestMatchers("/api/v1/memes").authenticated()
                        .requestMatchers("/api/v1/memes/**").authenticated()
                        // Catch-all
                        .anyRequest().authenticated()
                )
                // JWT Resource Server
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                );

        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        grantedAuthoritiesConverter.setAuthorityPrefix("SCOPE_");

        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(jwt -> {
            // 1. Lấy các quyền mặc định từ scope
            Collection<GrantedAuthority> authorities = grantedAuthoritiesConverter.convert(jwt);
            // 2. Trích xuất field "role" trực tiếp từ root payload
            String customRole = jwt.getClaimAsString("role");
            if (customRole != null && !customRole.isEmpty()) {
                // Thêm tiền tố ROLE_ để sử dụng được với .hasRole("USER") hoặc @PreAuthorize("hasRole('USER')")
                authorities.add(new SimpleGrantedAuthority("ROLE_" + customRole));
            }
            return authorities;
        });
        return jwtAuthenticationConverter;
    }
}
