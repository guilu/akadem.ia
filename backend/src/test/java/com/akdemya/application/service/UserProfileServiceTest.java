package com.akdemya.application.service;

import com.akdemya.domain.model.AppUser;
import com.akdemya.domain.port.in.UserProfileUseCase.GetProfileResponse;
import com.akdemya.domain.port.in.UserProfileUseCase.UpdateProfileCommand;
import com.akdemya.domain.port.out.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

class UserProfileServiceTest {

    private InMemoryUserRepository userRepo;
    private UserProfileService service;

    @BeforeEach
    void setUp() {
        userRepo = new InMemoryUserRepository();
        service = new UserProfileService(userRepo);
    }

    // -------------------------------------------------------------------------
    // getProfile — happy path
    // -------------------------------------------------------------------------

    @Test
    void getProfile_happyPath_returnsUserData() {
        AppUser user = AppUser.create("alice@example.com", "hash", "STUDENT", "Alice", "Smith", null);
        userRepo.save(user);

        GetProfileResponse response = service.getProfile(user.getId());

        assertEquals(user.getId(), response.id());
        assertEquals("alice@example.com", response.email());
        assertEquals("Alice", response.firstName());
        assertEquals("Smith", response.lastName());
    }

    // -------------------------------------------------------------------------
    // getProfile — user not found
    // -------------------------------------------------------------------------

    @Test
    void getProfile_whenUserNotFound_throwsException() {
        UUID unknownId = UUID.randomUUID();

        assertThrows(NoSuchElementException.class, () -> service.getProfile(unknownId));
    }

    // -------------------------------------------------------------------------
    // updateProfile — happy path
    // -------------------------------------------------------------------------

    @Test
    void updateProfile_happyPath_persistsNewNamesAndReturnsResponse() {
        AppUser user = AppUser.create("bob@example.com", "hash", "STUDENT", "Bob", "Old", null);
        userRepo.save(user);

        GetProfileResponse response = service.updateProfile(
            user.getId(),
            new UpdateProfileCommand("Robert", "New")
        );

        assertEquals("Robert", response.firstName());
        assertEquals("New", response.lastName());

        AppUser persisted = userRepo.findById(user.getId()).orElseThrow();
        assertEquals("Robert", persisted.getFirstName());
        assertEquals("New", persisted.getLastName());
    }

    // -------------------------------------------------------------------------
    // updateProfile — preserves unchanged fields
    // -------------------------------------------------------------------------

    @Test
    void updateProfile_preservesUnchangedFieldsLikeEmailAndRole() {
        AppUser user = AppUser.create("carol@example.com", "secret-hash", "ADMIN", "Carol", "Doe", "Engineer");
        userRepo.save(user);

        service.updateProfile(user.getId(), new UpdateProfileCommand("Caroline", null));

        AppUser persisted = userRepo.findById(user.getId()).orElseThrow();
        assertEquals("carol@example.com", persisted.getEmail());
        assertEquals("secret-hash", persisted.getPasswordHash());
        assertEquals("ADMIN", persisted.getRole());
        assertEquals("Engineer", persisted.getOccupation());
        assertEquals("Caroline", persisted.getFirstName());
        assertNull(persisted.getLastName());
    }

    // -------------------------------------------------------------------------
    // updateProfile — user not found
    // -------------------------------------------------------------------------

    @Test
    void updateProfile_whenUserNotFound_throwsException() {
        UUID unknownId = UUID.randomUUID();

        assertThrows(NoSuchElementException.class, () ->
            service.updateProfile(unknownId, new UpdateProfileCommand("X", "Y"))
        );
    }

    // -------------------------------------------------------------------------
    // updateProfile — firstName too long
    // -------------------------------------------------------------------------

    @Test
    void updateProfile_whenFirstNameExceeds100Chars_throwsIllegalArgumentException() {
        AppUser user = AppUser.create("dave@example.com", "hash", "STUDENT", "Dave", "Short", null);
        userRepo.save(user);

        String tooLong = "A".repeat(101);

        assertThrows(IllegalArgumentException.class, () ->
            service.updateProfile(user.getId(), new UpdateProfileCommand(tooLong, "Valid"))
        );
    }

    // =========================================================================
    // In-memory fake
    // =========================================================================

    static class InMemoryUserRepository implements UserRepository {
        private final Map<UUID, AppUser> store = new ConcurrentHashMap<>();

        @Override
        public Optional<AppUser> findByEmail(String email) {
            return store.values().stream().filter(u -> u.getEmail().equals(email)).findFirst();
        }

        @Override
        public AppUser save(AppUser user) {
            store.put(user.getId(), user);
            return user;
        }

        @Override
        public Optional<AppUser> findById(UUID id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public boolean existsByEmail(String email) {
            return store.values().stream().anyMatch(u -> u.getEmail().equals(email));
        }

        @Override
        public List<AppUser> findAll() {
            return new ArrayList<>(store.values());
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
            store.remove(id);
        }
    }
}
