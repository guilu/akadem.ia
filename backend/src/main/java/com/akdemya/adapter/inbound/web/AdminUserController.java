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
  public PageResponse<UserResponse> list(@RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "10") int size) {
    var data = users.findPage(page, size);
    return PageResponse.from(data, UserResponse::from);
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
