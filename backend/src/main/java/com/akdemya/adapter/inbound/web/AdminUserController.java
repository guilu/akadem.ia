package com.akdemya.adapter.inbound.web;

import com.akdemya.domain.model.AppUser;
import com.akdemya.domain.port.out.PasswordHasher;
import com.akdemya.domain.port.out.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {
  private static final String TEMP_PASSWORD_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$";
  private static final int TEMP_PASSWORD_LENGTH = 16;
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  private final UserRepository users;
  private final PasswordHasher hasher;

  public AdminUserController(UserRepository users, PasswordHasher hasher) {
    this.users = users;
    this.hasher = hasher;
  }

  private String generateTemporaryPassword() {
    StringBuilder sb = new StringBuilder(TEMP_PASSWORD_LENGTH);
    for (int i = 0; i < TEMP_PASSWORD_LENGTH; i++) {
      sb.append(TEMP_PASSWORD_CHARS.charAt(SECURE_RANDOM.nextInt(TEMP_PASSWORD_CHARS.length())));
    }
    return sb.toString();
  }

  @GetMapping
  public PageResponse<UserResponse> list(@RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "10") int size) {
    var data = users.findPage(page, size);
    return PageResponse.from(data, UserResponse::from);
  }

  @PostMapping
  public ResponseEntity<?> create(@Valid @RequestBody UserRequest req) {
    if (users.existsByEmail(req.email())) {
      return ResponseEntity.badRequest().body(Map.of("error", "email_in_use"));
    }
    String temporaryPassword = generateTemporaryPassword();
    String passwordHash = hasher.encode(temporaryPassword);
    AppUser user = AppUser.create(req.email(), passwordHash, req.role(), req.firstName(), req.lastName(), req.occupation());
    UserResponse saved = UserResponse.from(users.save(user));
    return ResponseEntity.ok(Map.of("user", saved, "temporaryPassword", temporaryPassword));
  }

  @PutMapping("/{id}")
  public ResponseEntity<?> update(@PathVariable UUID id, @Valid @RequestBody UserRequest req) {
    AppUser user = users.findById(id).orElse(null);
    if (user == null) return ResponseEntity.notFound().build();
    AppUser updated = new AppUser(id, req.email(), user.getPasswordHash(), req.role(), req.firstName(), req.lastName(), req.occupation());
    return ResponseEntity.ok(UserResponse.from(users.save(updated)));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<?> delete(@PathVariable UUID id) {
    users.deleteById(id);
    return ResponseEntity.ok().build();
  }

  record UserRequest(
      @NotBlank @Email String email,
      String firstName,
      String lastName,
      String occupation,
      @NotBlank @Pattern(regexp = "STUDENT|ADMIN") String role
  ) {}
  record UserResponse(UUID id, String email, String firstName, String lastName, String occupation, String role) {
    static UserResponse from(AppUser u) {
      return new UserResponse(u.getId(), u.getEmail(), u.getFirstName(), u.getLastName(), u.getOccupation(), u.getRole());
    }
  }

  record PageResponse<T>(java.util.List<T> items, int page, int size, long total, int totalPages) {
    static <T, R> PageResponse<R> from(org.springframework.data.domain.Page<T> data, java.util.function.Function<T, R> mapper) {
      return new PageResponse<>(
          data.getContent().stream().map(mapper).toList(),
          data.getNumber(),
          data.getSize(),
          data.getTotalElements(),
          data.getTotalPages()
      );
    }
  }
}
