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
 * Tests ownership-aware authorization for DELETE in QuestionController.
 */
class QuestionControllerAuthzTest {

    private final ContentManagement contentService = mock(ContentManagement.class);
    private final UserRepository userRepo = mock(UserRepository.class);

    private final QuestionController controller = new QuestionController(contentService, userRepo);

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

    // Test 1: Owner can delete their own PRIVATE question → 200
    @Test
    void ownerCanDeleteOwnPrivateQuestion() {
        String email = "owner@example.com";
        UUID ownerId = setupUser(email);
        UUID questionId = UUID.randomUUID();
        var principal = new User(email, "", List.of());

        doNothing().when(contentService).deleteQuestionIfAuthorized(eq(questionId), eq(ownerId), eq(false));
        doNothing().when(contentService).deleteAnswersByQuestion(eq(questionId));

        ResponseEntity<?> response = controller.delete(questionId, principal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(contentService).deleteQuestionIfAuthorized(eq(questionId), eq(ownerId), eq(false));
        verify(contentService).deleteAnswersByQuestion(eq(questionId));
    }

    // Test 2: Non-owner delete on PRIVATE question → 403
    @Test
    void nonOwnerCannotDeletePrivateQuestion() {
        String email = "other@example.com";
        UUID otherId = setupUser(email);
        UUID questionId = UUID.randomUUID();
        var principal = new User(email, "", List.of());

        doThrow(new AccessDeniedException("Cannot delete another user's PRIVATE question"))
                .when(contentService).deleteQuestionIfAuthorized(eq(questionId), eq(otherId), eq(false));

        ResponseEntity<?> response = controller.delete(questionId, principal);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(contentService, never()).deleteAnswersByQuestion(any());
    }

    // Test 3: Admin can delete GLOBAL question → 200
    @Test
    void adminCanDeleteGlobalQuestion() {
        String email = "admin@example.com";
        UUID adminId = setupAdmin(email);
        UUID questionId = UUID.randomUUID();
        var principal = new User(email, "", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        doNothing().when(contentService).deleteQuestionIfAuthorized(eq(questionId), eq(adminId), eq(true));
        doNothing().when(contentService).deleteAnswersByQuestion(eq(questionId));

        ResponseEntity<?> response = controller.delete(questionId, principal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(contentService).deleteQuestionIfAuthorized(eq(questionId), eq(adminId), eq(true));
        verify(contentService).deleteAnswersByQuestion(eq(questionId));
    }

    // Test 4: Non-admin user on GLOBAL question → 403
    @Test
    void nonAdminCannotDeleteGlobalQuestion() {
        String email = "user@example.com";
        UUID userId = setupUser(email);
        UUID questionId = UUID.randomUUID();
        var principal = new User(email, "", List.of());

        doThrow(new AccessDeniedException("Only admins can delete GLOBAL questions"))
                .when(contentService).deleteQuestionIfAuthorized(eq(questionId), eq(userId), eq(false));

        ResponseEntity<?> response = controller.delete(questionId, principal);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(contentService, never()).deleteAnswersByQuestion(any());
    }

    // Test 5: Unauthenticated delete → 401
    @Test
    void unauthenticatedDeleteReturns401() {
        ResponseEntity<?> response = controller.delete(UUID.randomUUID(), null);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    // Test 6: Delete non-existent question → 404
    @Test
    void deleteNonExistentQuestionReturns404() {
        String email = "user@example.com";
        UUID userId = setupUser(email);
        UUID questionId = UUID.randomUUID();
        var principal = new User(email, "", List.of());

        doThrow(new IllegalArgumentException("Question not found"))
                .when(contentService).deleteQuestionIfAuthorized(eq(questionId), eq(userId), eq(false));

        ResponseEntity<?> response = controller.delete(questionId, principal);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}
