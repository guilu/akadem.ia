package com.akdemya.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.akdemya.domain.model.AppUser;
import com.akdemya.domain.port.in.AuthUseCase.AuthResponse;
import com.akdemya.domain.port.in.AuthUseCase.LoginCommand;
import com.akdemya.domain.port.in.AuthUseCase.RegisterCommand;
import com.akdemya.domain.port.out.PasswordHasher;
import com.akdemya.domain.port.out.TokenProvider;
import com.akdemya.domain.port.out.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

class AuthManagerTest {

  private InMemoryUserRepository userRepo;
  private FakePasswordHasher passwordHasher;
  private FakeTokenProvider tokenProvider;
  private AuthManager authManager;

  @BeforeEach
  void setUp() {
    userRepo = new InMemoryUserRepository();
    passwordHasher = new FakePasswordHasher();
    tokenProvider = new FakeTokenProvider();
    authManager = new AuthManager(userRepo, passwordHasher, tokenProvider);
  }

  // -------------------------------------------------------------------------
  // Scenario 1: First Google login → user does not exist → saved with STUDENT
  // -------------------------------------------------------------------------

  @Test
  void loginWithOAuth2_whenUserDoesNotExist_savesUserWithStudentRoleAndReturnsToken() {
    AuthResponse response = authManager.loginWithOAuth2("new@example.com", "John Doe");

    assertNotNull(response.accessToken());
    assertNull(response.error());
    assertEquals("STUDENT", response.role());

    Optional<AppUser> saved = userRepo.findByEmail("new@example.com");
    assertTrue(saved.isPresent());
    assertEquals("STUDENT", saved.get().getRole());
    assertTrue(tokenProvider.lastClaims().containsKey("role"));
    assertEquals("STUDENT", tokenProvider.lastClaims().get("role"));
  }

  // -------------------------------------------------------------------------
  // Scenario 2: Repeat Google login → user already exists → not re-saved
  // -------------------------------------------------------------------------

  @Test
  void loginWithOAuth2_whenUserAlreadyExists_doesNotReSaveUser() {
    AppUser existing = AppUser.create("repeat@example.com", null, "STUDENT", "Jane", "Doe", null);
    userRepo.save(existing);
    int saveCountBefore = userRepo.saveCount();

    AuthResponse response = authManager.loginWithOAuth2("repeat@example.com", "Jane Doe");

    assertNotNull(response.accessToken());
    assertNull(response.error());
    assertEquals(saveCountBefore, userRepo.saveCount());
  }

  // -------------------------------------------------------------------------
  // Scenario 3: Existing ADMIN user → Google login → JWT contains ADMIN role
  // -------------------------------------------------------------------------

  @Test
  void loginWithOAuth2_whenExistingUserHasAdminRole_jwtContainsAdminRole() {
    AppUser admin = AppUser.create("admin@example.com", null, "ADMIN", "Admin", "User", null);
    userRepo.save(admin);

    AuthResponse response = authManager.loginWithOAuth2("admin@example.com", "Admin User");

    assertNotNull(response.accessToken());
    assertEquals("ADMIN", response.role());
    assertEquals("ADMIN", tokenProvider.lastClaims().get("role"));
  }

  // -------------------------------------------------------------------------
  // Scenario 4: Full name "John Doe" → firstName="John", lastName="Doe"
  // -------------------------------------------------------------------------

  @Test
  void loginWithOAuth2_withFullName_splitsFirstAndLastName() {
    authManager.loginWithOAuth2("john.doe@example.com", "John Doe");

    AppUser saved = userRepo.findByEmail("john.doe@example.com").orElseThrow();
    assertEquals("John", saved.getFirstName());
    assertEquals("Doe", saved.getLastName());
  }

  // -------------------------------------------------------------------------
  // Scenario 5: Single name "João" → firstName="João", lastName null
  // -------------------------------------------------------------------------

  @Test
  void loginWithOAuth2_withSingleName_setsFirstNameAndNullLastName() {
    authManager.loginWithOAuth2("joao@example.com", "João");

    AppUser saved = userRepo.findByEmail("joao@example.com").orElseThrow();
    assertEquals("João", saved.getFirstName());
    assertNull(saved.getLastName());
  }

  // -------------------------------------------------------------------------
  // Scenario 6: Register happy path → user saved, JWT with STUDENT role
  // -------------------------------------------------------------------------

  @Test
  void register_happyPath_savesUserAndReturnsTokenWithStudentRole() {
    RegisterCommand command = new RegisterCommand(
        "newuser@example.com", "password123", "password123", "Alice", "Smith", "Developer"
    );

    AuthResponse response = authManager.register(command);

    assertNotNull(response.accessToken());
    assertNull(response.error());
    assertEquals("STUDENT", response.role());
    assertTrue(userRepo.findByEmail("newuser@example.com").isPresent());
  }

  // -------------------------------------------------------------------------
  // Scenario 7a: Register with null email → returns invalid_email error
  // -------------------------------------------------------------------------

  @Test
  void register_whenEmailIsNull_returnsInvalidEmailError() {
    RegisterCommand command = new RegisterCommand(
        null, "password123", "password123", "Alice", "Smith", "Developer"
    );

    AuthResponse response = authManager.register(command);

    assertNull(response.accessToken());
    assertEquals("invalid_email", response.error());
  }

  // -------------------------------------------------------------------------
  // Scenario 7b: Register with null password → returns password_too_short error
  // -------------------------------------------------------------------------

  @Test
  void register_whenPasswordIsNull_returnsPasswordTooShortError() {
    RegisterCommand command = new RegisterCommand(
        "user@example.com", null, null, "Alice", "Smith", "Developer"
    );

    AuthResponse response = authManager.register(command);

    assertNull(response.accessToken());
    assertEquals("password_too_short", response.error());
  }

  // -------------------------------------------------------------------------
  // Scenario 7c: Register with null confirmPassword → returns password_mismatch
  // -------------------------------------------------------------------------

  @Test
  void register_whenConfirmPasswordIsNull_returnsPasswordMismatchError() {
    RegisterCommand command = new RegisterCommand(
        "user@example.com", "password123", null, "Alice", "Smith", "Developer"
    );

    AuthResponse response = authManager.register(command);

    assertNull(response.accessToken());
    assertEquals("password_mismatch", response.error());
  }

  // -------------------------------------------------------------------------
  // Scenario 7d: Register with existing email → returns email_in_use error
  // -------------------------------------------------------------------------

  @Test
  void register_whenEmailAlreadyExists_returnsEmailInUseError() {
    AppUser existing = AppUser.create("taken@example.com", "hash", "STUDENT", "Existing", "User", null);
    userRepo.save(existing);

    RegisterCommand command = new RegisterCommand(
        "taken@example.com", "password123", "password123", "New", "User", null
    );

    AuthResponse response = authManager.register(command);

    assertNull(response.accessToken());
    assertEquals("email_in_use", response.error());
  }

  // -------------------------------------------------------------------------
  // Scenario 8: Login with correct credentials → JWT returned
  // -------------------------------------------------------------------------

  @Test
  void login_withCorrectCredentials_returnsToken() {
    String rawPassword = "correctPassword";
    AppUser user = AppUser.create(
        "user@example.com", passwordHasher.encode(rawPassword), "STUDENT", "User", "Name", null
    );
    userRepo.save(user);

    AuthResponse response = authManager.login(new LoginCommand("user@example.com", rawPassword));

    assertNotNull(response.accessToken());
    assertNull(response.error());
    assertEquals("STUDENT", response.role());
  }

  // -------------------------------------------------------------------------
  // Scenario 9: Login with wrong password → returns invalid_credentials error
  // -------------------------------------------------------------------------

  @Test
  void login_withWrongPassword_returnsInvalidCredentialsError() {
    AppUser user = AppUser.create(
        "user@example.com", passwordHasher.encode("correctPassword"), "STUDENT", "User", "Name", null
    );
    userRepo.save(user);

    AuthResponse response = authManager.login(new LoginCommand("user@example.com", "wrongPassword"));

    assertNull(response.accessToken());
    assertEquals("invalid_credentials", response.error());
  }

  // =========================================================================
  // In-memory fakes
  // =========================================================================

  static class InMemoryUserRepository implements UserRepository {
    private final Map<String, AppUser> dataByEmail = new ConcurrentHashMap<>();
    private int saveCallCount = 0;

    @Override
    public Optional<AppUser> findByEmail(String email) {
      return Optional.ofNullable(dataByEmail.get(email));
    }

    @Override
    public AppUser save(AppUser user) {
      saveCallCount++;
      dataByEmail.put(user.getEmail(), user);
      return user;
    }

    @Override
    public Optional<AppUser> findById(UUID id) {
      return dataByEmail.values().stream().filter(u -> u.getId().equals(id)).findFirst();
    }

    @Override
    public boolean existsByEmail(String email) {
      return dataByEmail.containsKey(email);
    }

    @Override
    public List<AppUser> findAll() {
      return new ArrayList<>(dataByEmail.values());
    }

    @Override
    public Page<AppUser> findPage(int page, int size) {
      List<AppUser> all = findAll();
      int from = Math.min(page * size, all.size());
      int to = Math.min(from + size, all.size());
      return new PageImpl<>(all.subList(from, to), PageRequest.of(page, size), all.size());
    }

    @Override
    public void deleteById(UUID id) {
      dataByEmail.values().removeIf(u -> u.getId().equals(id));
    }

    int saveCount() {
      return saveCallCount;
    }
  }

  static class FakePasswordHasher implements PasswordHasher {
    @Override
    public String encode(String rawPassword) {
      return "hashed:" + rawPassword;
    }

    @Override
    public boolean matches(String rawPassword, String encodedPassword) {
      return encodedPassword != null && encodedPassword.equals("hashed:" + rawPassword);
    }
  }

  static class FakeTokenProvider implements TokenProvider {
    private Map<String, Object> lastClaims;

    @Override
    public String generate(String subject, Map<String, Object> claims) {
      this.lastClaims = claims;
      return "fake-token-for-" + subject;
    }

    Map<String, Object> lastClaims() {
      return lastClaims;
    }
  }
}
