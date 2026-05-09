package com.duoc.backend;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private UserRegistrationRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new UserRegistrationRequest();
        validRequest.setUsername("carlos");
        validRequest.setEmail("carlos@mail.com");
        validRequest.setPassword("secret123");
    }

    // ── registerUser ──────────────────────────────────────────────────────────

    @Test
    void registerUserShouldSaveAndReturnUserWhenRequestIsValid() {
        when(userRepository.existsByUsername("carlos")).thenReturn(false);
        when(userRepository.existsByEmail("carlos@mail.com")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("$2a$10$hashedpassword1234567890123456789012345678901234567890ab");
        User savedUser = new User();
        savedUser.setId(1);
        savedUser.setUsername("carlos");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        User result = userService.registerUser(validRequest);

        assertEquals(savedUser, result);
        verify(passwordEncoder).encode("secret123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void registerUserShouldTrimUsernameAndEmail() {
        validRequest.setUsername("  carlos  ");
        validRequest.setEmail("  carlos@mail.com  ");
        when(userRepository.existsByUsername("  carlos  ")).thenReturn(false);
        when(userRepository.existsByEmail("  carlos@mail.com  ")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.registerUser(validRequest);

        assertEquals("carlos", result.getUsername());
        assertEquals("carlos@mail.com", result.getEmail());
    }

    @Test
    void registerUserShouldThrowWhenRequestIsNull() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userService.registerUser(null));
        assertEquals("Request body is required", ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerUserShouldThrowWhenUsernameIsBlank() {
        validRequest.setUsername("  ");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userService.registerUser(validRequest));
        assertEquals("Username, email and password are required", ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerUserShouldThrowWhenEmailIsBlank() {
        validRequest.setEmail("");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userService.registerUser(validRequest));
        assertEquals("Username, email and password are required", ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerUserShouldThrowWhenPasswordIsNull() {
        validRequest.setPassword(null);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userService.registerUser(validRequest));
        assertEquals("Username, email and password are required", ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerUserShouldThrowWhenUsernameAlreadyExists() {
        when(userRepository.existsByUsername("carlos")).thenReturn(true);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userService.registerUser(validRequest));
        assertEquals("Username already exists", ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerUserShouldThrowWhenEmailAlreadyExists() {
        when(userRepository.existsByUsername("carlos")).thenReturn(false);
        when(userRepository.existsByEmail("carlos@mail.com")).thenReturn(true);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userService.registerUser(validRequest));
        assertEquals("Email already exists", ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    // ── authenticate ──────────────────────────────────────────────────────────

    @Test
    void authenticateShouldReturnTrueWhenHashedPasswordMatches() {
        User user = new User();
        user.setUsername("carlos");
        user.setPassword("$2a$10$abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1");
        when(userRepository.findByUsername("carlos")).thenReturn(user);
        when(passwordEncoder.matches("secret123", user.getPassword())).thenReturn(true);

        assertTrue(userService.authenticate("carlos", "secret123"));
        verify(passwordEncoder).matches("secret123", user.getPassword());
    }

    @Test
    void authenticateShouldReturnFalseWhenHashedPasswordDoesNotMatch() {
        User user = new User();
        user.setUsername("carlos");
        user.setPassword("$2a$10$abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1");
        when(userRepository.findByUsername("carlos")).thenReturn(user);
        when(passwordEncoder.matches("wrong", user.getPassword())).thenReturn(false);

        assertFalse(userService.authenticate("carlos", "wrong"));
        verify(passwordEncoder).matches("wrong", user.getPassword());
    }

    @Test
    void authenticateShouldReturnFalseWhenUserNotFound() {
        when(userRepository.findByUsername("unknown")).thenReturn(null);

        assertFalse(userService.authenticate("unknown", "secret123"));
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    void authenticateShouldReturnFalseWhenRawPasswordIsBlank() {
        User user = new User();
        user.setUsername("carlos");
        user.setPassword("plaintext");
        when(userRepository.findByUsername("carlos")).thenReturn(user);

        assertFalse(userService.authenticate("carlos", "  "));
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    void authenticateShouldReturnFalseWhenStoredPasswordIsBlank() {
        User user = new User();
        user.setUsername("carlos");
        user.setPassword("");
        when(userRepository.findByUsername("carlos")).thenReturn(user);

        assertFalse(userService.authenticate("carlos", "secret123"));
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    void authenticateShouldReturnFalseWhenPlainPasswordDoesNotMatch() {
        User user = new User();
        user.setUsername("carlos");
        user.setPassword("correctplain");
        when(userRepository.findByUsername("carlos")).thenReturn(user);

        assertFalse(userService.authenticate("carlos", "wrongplain"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void authenticateShouldMigratePlainPasswordOnSuccessfulMatch() {
        User user = new User();
        user.setUsername("carlos");
        user.setPassword("plaintext");
        when(userRepository.findByUsername("carlos")).thenReturn(user);
        when(passwordEncoder.encode("plaintext")).thenReturn("$2a$10$abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1");

        assertTrue(userService.authenticate("carlos", "plaintext"));
        verify(passwordEncoder).encode("plaintext");
        verify(userRepository).save(user);
    }

    // ── migratePlainTextPasswords ─────────────────────────────────────────────

    @Test
    void migratePlainTextPasswordsShouldMigrateAllPlainUsers() {
        User plain1 = new User();
        plain1.setPassword("pass1");
        User plain2 = new User();
        plain2.setPassword("pass2");
        when(userRepository.findAll()).thenReturn(List.of(plain1, plain2));
        when(passwordEncoder.encode("pass1")).thenReturn("$2a$10$hashed00000000000000000000000000000000000000000000000001ab");
        when(passwordEncoder.encode("pass2")).thenReturn("$2a$10$hashed00000000000000000000000000000000000000000000000002ab");

        int migrated = userService.migratePlainTextPasswords();

        assertEquals(2, migrated);
        verify(userRepository, times(2)).save(any(User.class));
    }

    @Test
    void migratePlainTextPasswordsShouldSkipAlreadyHashedPasswords() {
        User hashed = new User();
        hashed.setPassword("$2a$10$abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1");
        when(userRepository.findAll()).thenReturn(List.of(hashed));

        int migrated = userService.migratePlainTextPasswords();

        assertEquals(0, migrated);
        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void migratePlainTextPasswordsShouldSkipUsersWithBlankPassword() {
        User blank = new User();
        blank.setPassword("  ");
        when(userRepository.findAll()).thenReturn(List.of(blank));

        int migrated = userService.migratePlainTextPasswords();

        assertEquals(0, migrated);
        verify(userRepository, never()).save(any());
    }

    @Test
    void migratePlainTextPasswordsShouldReturnZeroWhenNoUsers() {
        when(userRepository.findAll()).thenReturn(List.of());

        int migrated = userService.migratePlainTextPasswords();

        assertEquals(0, migrated);
    }

    // ── isPasswordHashed ──────────────────────────────────────────────────────

    @Test
    void isPasswordHashedShouldReturnTrueForValidBcryptHash() {
        assertTrue(userService.isPasswordHashed("$2a$10$abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1"));
    }

    @Test
    void isPasswordHashedShouldReturnFalseForPlainText() {
        assertFalse(userService.isPasswordHashed("plaintext"));
    }

    @Test
    void isPasswordHashedShouldReturnFalseForNull() {
        assertFalse(userService.isPasswordHashed(null));
    }
}
