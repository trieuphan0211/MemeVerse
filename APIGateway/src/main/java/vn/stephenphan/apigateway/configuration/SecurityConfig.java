package vn.stephenphan.apigateway.configuration;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Collection;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@Log4j2
public class SecurityConfig {
    @Value("${user.username}")
    private String userName;

    @Value("${user.password}")
    private String userPassword;

    @Value("${user.role}")
    private String userRole;
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        // Specific rules first
                        .requestMatchers("/eureka/**").hasRole("SUPERADMIN")
                        // Re-enable these as needed, ensuring they follow a hierarchy
                        // .requestMatchers("/v1/admin/**").hasRole("ADMIN")
                        // .requestMatchers("/v1/internal/**").hasAnyRole("SERVICE", "ADMIN")

                        // Catch-all
                        .anyRequest().authenticated()
                )
                // Standard Basic Auth (often used for Eureka/Internal)
                .httpBasic(Customizer.withDefaults())
                // JWT Resource Server for User Tokens
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                );
                // Enable if this service also acts as a client to call other services
//                .oauth2Client(Customizer.withDefaults());

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

    @Bean
    public InMemoryUserDetailsManager userDetailsService() {
        log.info("Creating user for Eureka with name: {}, password: {}, role: {}", userName, userPassword, userRole);
        UserDetails user = User.withDefaultPasswordEncoder()
                .username(userName)
                .password(userPassword)
                .roles(userRole)
                .build();
        return new InMemoryUserDetailsManager(user);
    }
}