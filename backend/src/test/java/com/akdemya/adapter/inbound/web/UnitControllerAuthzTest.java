package com.akdemya.adapter.inbound.web;

import com.akdemya.application.service.ContentManagement;
import com.akdemya.domain.model.AppUser;
import com.akdemya.domain.port.out.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests ownership-aware authorization for DELETE in UnitController.
 */
class UnitControllerAuthzTest {

    private final ContentManagement contentService = mock(ContentManagement.class);
    private final UserRepository userRepo = mock(UserRepository.class);

    private final UnitController controller = new UnitController(contentService, userRepo);

    private UUID setupUser(String email) {
        UUID userId = UUID.randomUUID();
        when(userRepo.findByEmail(email))
                .thenReturn(Optional.of(new AppUser(userId, email, "", "USER", null, null, null)));
        return userId;
    }

    private UUID setupAdmin(String email) {
        UUID adminId = UUID.randomUUID();
        when(userRepo.findByEmail(email))
                .thenReturn(Optional.of(new AppUser(adminId, email, "", "ADMIN", null, null, null)));
        return adminId;
    }

    // Test 1: Owner can delete their own PRIVATE unit → 200
    @Test
    void ownerCanDeleteOwnPrivateUnit() {
        String email = "owner@example.com";
        UUID ownerId = setupUser(email);
        UUID unitId = UUID.randomUUID();
        var principal = new User(email, "", List.of());

        doNothing().when(contentService).deleteUnitIfAuthorized(eq(unitId), eq(ownerId), eq(false));

        ResponseEntity<?> response = controller.delete(unitId, principal);

        assertEquals(200, response.getStatusCode().value());
        verify(contentService).deleteUnitIfAuthorized(eq(unitId), eq(ownerId), eq(false));
    }

    // Test 2: Non-owner delete on PRIVATE unit → 403
    @Test
    void nonOwnerCannotDeletePrivateUnit() {
        String email = "other@example.com";
        UUID otherId = setupUser(email);
        UUID unitId = UUID.randomUUID();
        var principal = new User(email, "", List.of());

        doThrow(new AccessDeniedException("Cannot delete another user's PRIVATE unit"))
                .when(contentService).deleteUnitIfAuthorized(eq(unitId), eq(otherId), eq(false));

        ResponseEntity<?> response = controller.delete(unitId, principal);

        assertEquals(403, response.getStatusCode().value());
    }

    // Test 3: Admin can delete GLOBAL unit → 200
    @Test
    void adminCanDeleteGlobalUnit() {
        String email = "admin@example.com";
        UUID adminId = setupAdmin(email);
        UUID unitId = UUID.randomUUID();
        var principal = new User(email, "", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        doNothing().when(contentService).deleteUnitIfAuthorized(eq(unitId), eq(adminId), eq(true));

        ResponseEntity<?> response = controller.delete(unitId, principal);

        assertEquals(200, response.getStatusCode().value());
        verify(contentService).deleteUnitIfAuthorized(eq(unitId), eq(adminId), eq(true));
    }

    // Test 4: Non-admin user on GLOBAL unit → 403
    @Test
    void nonAdminCannotDeleteGlobalUnit() {
        String email = "user@example.com";
        UUID userId = setupUser(email);
        UUID unitId = UUID.randomUUID();
        var principal = new User(email, "", List.of());

        doThrow(new AccessDeniedException("Only admins can delete GLOBAL units"))
                .when(contentService).deleteUnitIfAuthorized(eq(unitId), eq(userId), eq(false));

        ResponseEntity<?> response = controller.delete(unitId, principal);

        assertEquals(403, response.getStatusCode().value());
    }

    // Test 5: Unauthenticated delete → 401
    @Test
    void unauthenticatedDeleteReturns401() {
        ResponseEntity<?> response = controller.delete(UUID.randomUUID(), null);
        assertEquals(401, response.getStatusCode().value());
    }

    // Test 6: Delete non-existent unit → 404
    @Test
    void deleteNonExistentUnitReturns404() {
        String email = "user@example.com";
        UUID userId = setupUser(email);
        UUID unitId = UUID.randomUUID();
        var principal = new User(email, "", List.of());

        doThrow(new IllegalArgumentException("Unit not found"))
                .when(contentService).deleteUnitIfAuthorized(eq(unitId), eq(userId), eq(false));

        ResponseEntity<?> response = controller.delete(unitId, principal);

        assertEquals(404, response.getStatusCode().value());
    }
}
