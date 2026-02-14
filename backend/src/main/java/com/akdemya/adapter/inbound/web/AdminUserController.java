package com.akdemya.adapter.inbound.web;

import com.akdemya.domain.model.AppUser;
import com.akdemya.domain.port.out.PasswordHasher;
import com.akdemya.domain.port.out.UserRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@CrossOrigin(origins = "*")
public class AdminUserController {
  private final UserRepository users;
  private final PasswordHasher hasher;

  public AdminUserController(UserRepository users, PasswordHasher hasher) {
    this.users = users;
    this.hasher = hasher;
  }

  @GetMapping
  public List<UserResponse> list() {
    return users.findAll().stream().map(UserResponse::from).toList();
  }

  @PostMapping
  public ResponseEntity<?> create(@RequestBody UserRequest req) {
    if (users.existsByEmail(req.email())) {
      return ResponseEntity.badRequest().body(java.util.Map.of("error", "email_in_use"));
    }
    String passwordHash = hasher.encode("demo1234");
    AppUser user = AppUser.create(req.email(), passwordHash, req.role(), req.firstName(), req.lastName(), req.occupation());
    return ResponseEntity.ok(UserResponse.from(users.save(user)));
  }

  @PutMapping("/{id}")
  public ResponseEntity<?> update(@PathVariable UUID id, @RequestBody UserRequest req) {
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

  record UserRequest(String email, String firstName, String lastName, String occupation, String role) {}
  record UserResponse(UUID id, String email, String firstName, String lastName, String occupation, String role) {
    static UserResponse from(AppUser u) {
      return new UserResponse(u.getId(), u.getEmail(), u.getFirstName(), u.getLastName(), u.getOccupation(), u.getRole());
    }
  }
}
