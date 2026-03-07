package vn.stephenphan.userservice.utils;

import lombok.extern.log4j.Log4j2;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import vn.stephenphan.userservice.dto.UserPrincipal;

import java.util.Set;
import java.util.stream.Collectors;

@Component
@Log4j2
public class SecurityUtils {

    public static UserPrincipal getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication instanceof JwtAuthenticationToken jwtToken) {
            Jwt jwt = jwtToken.getToken();
            String userId = jwt.getClaimAsString("sub");
            String username = jwt.getClaimAsString("preferred_username");
            String email = jwt.getClaimAsString("email");
            Boolean emailVerified = jwt.getClaimAsBoolean("email_verified");
            String fullName = jwt.getClaimAsString("name");
            String givenName = jwt.getClaimAsString("given_name");
            String familyName = jwt.getClaimAsString("family_name");

            Set<String> roles = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toSet());

            return new UserPrincipal(userId, username, email, roles, emailVerified, fullName, givenName, familyName);
        }
        throw new IllegalStateException("Không tìm thấy thông tin xác thực hợp lệ.");
    }
}