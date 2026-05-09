package com.duoc.backend;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginControllerTest {

    @Mock
    private JWTAuthenticationConfig jwtAuthenticationConfig;

    @Mock
    private UserService userService;

    @InjectMocks
    private LoginController loginController;

    @Test
    void loginShouldReturnOkWithTokenWhenCredentialsAreValid() {
        LoginRequest loginRequest = new LoginRequest("carlos", "secret");
        when(userService.authenticate("carlos", "secret")).thenReturn(true);
        when(jwtAuthenticationConfig.getJWTToken("carlos")).thenReturn("jwt-token");

        ResponseEntity<String> response = loginController.login(loginRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("jwt-token", response.getBody());
        verify(userService).authenticate("carlos", "secret");
        verify(jwtAuthenticationConfig).getJWTToken("carlos");
    }

    @Test
    void loginShouldReturnBadRequestWhenCredentialsAreInvalid() {
        LoginRequest loginRequest = new LoginRequest("carlos", "wrong");
        when(userService.authenticate("carlos", "wrong")).thenReturn(false);

        ResponseEntity<String> response = loginController.login(loginRequest);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Invalid credentials", response.getBody());
        verify(userService).authenticate("carlos", "wrong");
        verify(jwtAuthenticationConfig, never()).getJWTToken("carlos");
    }

    @Test
    void loginShouldReturnBadRequestWhenAuthenticationThrowsException() {
        LoginRequest loginRequest = new LoginRequest("carlos", "secret");
        when(userService.authenticate("carlos", "secret")).thenThrow(new RuntimeException("DB down"));

        ResponseEntity<String> response = loginController.login(loginRequest);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Login failed: DB down", response.getBody());
        verify(userService).authenticate("carlos", "secret");
    }

    @Test
    void loginShouldReturnBadRequestWhenTokenGenerationThrowsException() {
        LoginRequest loginRequest = new LoginRequest("carlos", "secret");
        when(userService.authenticate("carlos", "secret")).thenReturn(true);
        when(jwtAuthenticationConfig.getJWTToken("carlos")).thenThrow(new RuntimeException("JWT error"));

        ResponseEntity<String> response = loginController.login(loginRequest);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Login failed: JWT error", response.getBody());
        verify(userService).authenticate("carlos", "secret");
        verify(jwtAuthenticationConfig).getJWTToken("carlos");
    }
}
