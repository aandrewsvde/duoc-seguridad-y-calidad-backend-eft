package com.duoc.backend;

import com.duoc.testsupport.TestSecurityEndpointsController;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Date;
import java.util.List;

import static com.duoc.backend.Constants.HEADER_AUTHORIZACION_KEY;
import static com.duoc.backend.Constants.SUPER_SECRET_KEY;
import static com.duoc.backend.Constants.TOKEN_BEARER_PREFIX;
import static com.duoc.backend.Constants.getSigningKey;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TestSecurityEndpointsController.class)
@AutoConfigureMockMvc(addFilters = true)
@Import({WebSecurityConfig.class, JWTAuthorizationFilter.class, TestSecurityEndpointsController.class})
@ActiveProfiles("default")
class WebSecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithAnonymousUser
    void shouldAllowGetLoginWithoutAuthentication() throws Exception {
        mockMvc.perform(get(Constants.LOGIN_URL))
                .andExpect(status().isOk())
                .andExpect(content().string("login-get-ok"));
    }

    @Test
    @WithAnonymousUser
    void shouldAllowPostLoginWithoutAuthentication() throws Exception {
        mockMvc.perform(post(Constants.LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(content().string("login-post-ok"));
    }

    @Test
    @WithAnonymousUser
    void shouldAllowGetPetsPathWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/pets/test"))
                .andExpect(status().isOk())
                .andExpect(content().string("pets-ok"));
    }

    @Test
    @WithAnonymousUser
    void shouldDenyProtectedPathWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/private/test"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowProtectedPathWithValidJwt() throws Exception {
        String token = createValidTokenWithAuthorities("security-user", List.of("ROLE_USER"));

        mockMvc.perform(get("/private/test")
                        .header(HEADER_AUTHORIZACION_KEY, TOKEN_BEARER_PREFIX + token))
                .andExpect(status().isOk())
                .andExpect(content().string("private-ok"));
    }

    @Test
    void shouldReturnForbiddenForMalformedJwtOnProtectedPath() throws Exception {
        mockMvc.perform(get("/private/test")
                        .header(HEADER_AUTHORIZACION_KEY, TOKEN_BEARER_PREFIX + "not-a-jwt"))
                .andExpect(status().isForbidden());
    }

    private String createValidTokenWithAuthorities(String username, List<String> authorities) {
        return Jwts.builder()
                .subject(username)
                .claim("authorities", authorities)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(getSigningKey(SUPER_SECRET_KEY))
                .compact();
    }
}
