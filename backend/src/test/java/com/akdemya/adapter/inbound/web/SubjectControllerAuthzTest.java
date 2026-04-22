package com.akdemya.adapter.inbound.web;

import com.akdemya.application.service.ContentManagement;
import com.akdemya.domain.model.AppUser;
import com.akdemya.domain.port.out.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
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
 * Tests ownership-aware authorization for DELETE in SubjectController.
 */
class SubjectControllerAuthzTest {

    private final ContentManagement contentService = mock(ContentManagement.class);
    private final UserRepository userRepo = mock(UserRepository.class);

    private final SubjectController controller = new SubjectController(contentService, userRepo);

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

    // Test 1: Owner can delete their own PRIVATE subject → 200
    @Test
    void ownerCanDeleteOwnPrivateSubject() {
        String email = "owner@example.com";
        UUID ownerId = setupUser(email);
        UUID subjectId = UUID.randomUUID();
        var principal = new User(email, "", List.of());

        doNothing().when(contentService).deleteSubjectIfAuthorized(eq(subjectId), eq(ownerId), eq(false));

        ResponseEntity<?> response = controller.delete(subjectId, principal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(contentService).deleteSubjectIfAuthorized(eq(subjectId), eq(ownerId), eq(false));
    }

    // Test 2: Non-owner delete on PRIVATE subject → 403
    @Test
    void nonOwnerCannotDeletePrivateSubject() {
        String email = "other@example.com";
        UUID otherId = setupUser(email);
        UUID subjectId = UUID.randomUUID();
        var principal = new User(email, "", List.of());

        doThrow(new AccessDeniedException("Cannot delete another user's PRIVATE subject"))
                .when(contentService).deleteSubjectIfAuthorized(eq(subjectId), eq(otherId), eq(false));

        ResponseEntity<?> response = controller.delete(subjectId, principal);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    // Test 3: Admin can delete GLOBAL subject → 200
    @Test
    void adminCanDeleteGlobalSubject() {
        String email = "admin@example.com";
        UUID adminId = setupAdmin(email);
        UUID subjectId = UUID.randomUUID();
        var principal = new User(email, "", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        doNothing().when(contentService).deleteSubjectIfAuthorized(eq(subjectId), eq(adminId), eq(true));

        ResponseEntity<?> response = controller.delete(subjectId, principal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(contentService).deleteSubjectIfAuthorized(eq(subjectId), eq(adminId), eq(true));
    }

    // Test 4: Non-admin user on GLOBAL subject → 403
    @Test
    void nonAdminCannotDeleteGlobalSubject() {
        String email = "user@example.com";
        UUID userId = setupUser(email);
        UUID subjectId = UUID.randomUUID();
        var principal = new User(email, "", List.of());

        doThrow(new AccessDeniedException("Only admins can delete GLOBAL subjects"))
                .when(contentService).deleteSubjectIfAuthorized(eq(subjectId), eq(userId), eq(false));

        ResponseEntity<?> response = controller.delete(subjectId, principal);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    // Test 5: Unauthenticated delete → 401
    @Test
    void unauthenticatedDeleteReturns401() {
        ResponseEntity<?> response = controller.delete(UUID.randomUUID(), null);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    // Test 6: Delete non-existent subject → 404
    @Test
    void deleteNonExistentSubjectReturns404() {
        String email = "user@example.com";
        UUID userId = setupUser(email);
        UUID subjectId = UUID.randomUUID();
        var principal = new User(email, "", List.of());

        doThrow(new IllegalArgumentException("Subject not found"))
                .when(contentService).deleteSubjectIfAuthorized(eq(subjectId), eq(userId), eq(false));

        ResponseEntity<?> response = controller.delete(subjectId, principal);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}
