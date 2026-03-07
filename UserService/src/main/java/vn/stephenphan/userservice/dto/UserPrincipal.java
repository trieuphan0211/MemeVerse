package vn.stephenphan.userservice.dto;

import lombok.Getter;

import java.util.Set;

public record UserPrincipal(
        String userId,       // 'sub' claim trong JWT
        String username,     // 'preferred_username'
        String email,
        Set<String> roles,
        Boolean emailVerified,
        String fullName,
        String givenName,
        String familyName
) {}
