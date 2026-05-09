package com.duoc.backend;

import io.jsonwebtoken.Jwts;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.Date;
import java.util.List;

import static com.duoc.backend.Constants.HEADER_AUTHORIZACION_KEY;
import static com.duoc.backend.Constants.SUPER_SECRET_KEY;
import static com.duoc.backend.Constants.TOKEN_BEARER_PREFIX;
import static com.duoc.backend.Constants.getSigningKey;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class JWTAuthorizationFilterTest {

    private JWTAuthorizationFilter filter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        filter = new JWTAuthorizationFilter();
    }

    @Test
    void shouldSetAuthenticationWhenJwtIsValidAndHasAuthorities() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        String token = createValidTokenWithAuthorities("test-user", List.of("ROLE_USER", "ROLE_ADMIN"));
        request.addHeader(HEADER_AUTHORIZACION_KEY, TOKEN_BEARER_PREFIX + token);

        filter.doFilter(request, response, chain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertInstanceOf(UsernamePasswordAuthenticationToken.class, authentication);
        assertEquals("test-user", authentication.getPrincipal());
        assertEquals(2, authentication.getAuthorities().size());
        assertNotNull(chain.getRequest());
    }

    @Test
    void shouldClearContextWhenAuthorizationHeaderIsMissing() throws ServletException, IOException {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("existing-user", null, List.of())
        );

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNotNull(chain.getRequest());
    }

    @Test
    void shouldClearContextWhenJwtHasNoAuthoritiesClaim() throws ServletException, IOException {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("existing-user", null, List.of())
        );

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        String token = createValidTokenWithoutAuthorities("test-user");
        request.addHeader(HEADER_AUTHORIZACION_KEY, TOKEN_BEARER_PREFIX + token);

        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNotNull(chain.getRequest());
    }

    @Test
    void shouldReturnForbiddenAndNotContinueChainWhenJwtIsMalformed() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        request.addHeader(HEADER_AUTHORIZACION_KEY, TOKEN_BEARER_PREFIX + "not-a-jwt");

        filter.doFilter(request, response, chain);

        assertEquals(MockHttpServletResponse.SC_FORBIDDEN, response.getStatus());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNull(chain.getRequest());
    }

    @Test
    void shouldReturnForbiddenAndNotContinueChainWhenJwtIsExpired() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        String token = createExpiredTokenWithAuthorities("test-user", List.of("ROLE_USER"));
        request.addHeader(HEADER_AUTHORIZACION_KEY, TOKEN_BEARER_PREFIX + token);

        filter.doFilter(request, response, chain);

        assertEquals(MockHttpServletResponse.SC_FORBIDDEN, response.getStatus());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNull(chain.getRequest());
    }

    private String createValidTokenWithAuthorities(String subject, List<String> authorities) {
        SecretKey key = (SecretKey) getSigningKey(SUPER_SECRET_KEY);
        return Jwts.builder()
                .subject(subject)
                .claim("authorities", authorities)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(key)
                .compact();
    }

    private String createValidTokenWithoutAuthorities(String subject) {
        SecretKey key = (SecretKey) getSigningKey(SUPER_SECRET_KEY);
        return Jwts.builder()
                .subject(subject)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(key)
                .compact();
    }

    private String createExpiredTokenWithAuthorities(String subject, List<String> authorities) {
        SecretKey key = (SecretKey) getSigningKey(SUPER_SECRET_KEY);
        return Jwts.builder()
                .subject(subject)
                .claim("authorities", authorities)
                .issuedAt(new Date(System.currentTimeMillis() - 120_000))
                .expiration(new Date(System.currentTimeMillis() - 60_000))
                .signWith(key)
                .compact();
    }
}
