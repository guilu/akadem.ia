package com.akdemya.adapter.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtAuthFilterCookieTest {

    private JwtService jwtService;
    private SecurityConfig.JwtAuthFilter filter;

    @BeforeEach
    void setUp() {
        jwtService = mock(JwtService.class);
        filter = new SecurityConfig.JwtAuthFilter(jwtService);
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_withCookieOnly_authenticatesUser() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setCookies(new Cookie("ak_token", "cookie.jwt.token"));
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        @SuppressWarnings("unchecked")
        Jws<Claims> jws = mock(Jws.class);
        Claims claims = mock(Claims.class);
        when(jws.getBody()).thenReturn(claims);
        when(claims.getSubject()).thenReturn("user@example.com");
        when(claims.get("role")).thenReturn("STUDENT");
        when(jwtService.parse("cookie.jwt.token")).thenReturn(jws);

        filter.doFilterInternal(req, res, chain);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth, "authentication must be set");
        assertEquals("user@example.com", auth.getName());
        verify(chain).doFilter(req, res);
    }

    @Test
    void doFilterInternal_withBearerHeader_authenticatesUser() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer bearer.jwt.token");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        @SuppressWarnings("unchecked")
        Jws<Claims> jws = mock(Jws.class);
        Claims claims = mock(Claims.class);
        when(jws.getBody()).thenReturn(claims);
        when(claims.getSubject()).thenReturn("admin@example.com");
        when(claims.get("role")).thenReturn("ADMIN");
        when(jwtService.parse("bearer.jwt.token")).thenReturn(jws);

        filter.doFilterInternal(req, res, chain);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertEquals("admin@example.com", auth.getName());
        verify(chain).doFilter(req, res);
    }

    @Test
    void doFilterInternal_withInvalidCookie_doesNotAuthenticate() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setCookies(new Cookie("ak_token", "bad.token"));
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        when(jwtService.parse("bad.token")).thenThrow(new RuntimeException("invalid"));

        filter.doFilterInternal(req, res, chain);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertNull(auth, "authentication must NOT be set for invalid token");
        verify(chain).doFilter(req, res);
    }

    @Test
    void doFilterInternal_withNoCookieAndNoHeader_doesNotAuthenticate() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(req, res, chain);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertNull(auth, "authentication must NOT be set when no token provided");
        verify(chain).doFilter(req, res);
        verifyNoInteractions(jwtService);
    }
}
