package com.akdemya.adapter.inbound.web;

import com.akdemya.adapter.inbound.web.dto.FlashcardDto;
import com.akdemya.domain.model.AppUser;
import com.akdemya.domain.model.Flashcard;
import com.akdemya.domain.model.Visibility;
import com.akdemya.domain.port.in.FlashcardImportExportUseCase;
import com.akdemya.domain.port.in.FlashcardManagementUseCase;
import com.akdemya.domain.port.in.FlashcardReviewUseCase;
import com.akdemya.domain.port.in.FlashcardStudyUseCase;
import com.akdemya.domain.port.out.FlashcardRepository;
import com.akdemya.domain.port.out.FlashcardReviewLogRepository;
import com.akdemya.domain.port.out.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests ownership-aware authorization for PUT/DELETE in FlashcardController.
 */
class FlashcardControllerAuthzTest {

    private final FlashcardStudyUseCase studyUseCase = mock(FlashcardStudyUseCase.class);
    private final FlashcardReviewUseCase reviewUseCase = mock(FlashcardReviewUseCase.class);
    private final FlashcardManagementUseCase managementUseCase = mock(FlashcardManagementUseCase.class);
    private final FlashcardImportExportUseCase importExportUseCase = mock(FlashcardImportExportUseCase.class);
    private final PrincipalResolver principalResolver = mock(PrincipalResolver.class);

    private final FlashcardController controller = new FlashcardController(
            studyUseCase, reviewUseCase, managementUseCase, importExportUseCase, principalResolver);

    private UUID setupUser(String email) {
        UUID userId = UUID.randomUUID();
        when(principalResolver.requireUserId(any())).thenReturn(userId);
        return userId;
    }

    private UUID setupAdmin(String email) {
        UUID adminId = UUID.randomUUID();
        when(principalResolver.requireUserId(any())).thenReturn(adminId);
        return adminId;
    }

    // ======================== UPDATE (PUT) ========================

    // Test 1: Owner can update their own PRIVATE flashcard → 200
    @Test
    void ownerCanUpdatePrivateFlashcard() {
        String email = "owner@example.com";
        UUID ownerId = setupUser(email);
        UUID flashcardId = UUID.randomUUID();
        UUID unitId = UUID.randomUUID();
        var principal = new User(email, "", List.of());

        Flashcard saved = new Flashcard(flashcardId, unitId, "updated front", "updated back",
                LocalDateTime.now(), LocalDateTime.now(), Visibility.PRIVATE, ownerId);
        when(managementUseCase.updateFlashcardIfAuthorized(any(), eq(ownerId), eq(false)))
                .thenReturn(saved);

        var req = new FlashcardDto.UpdateRequest(unitId, "updated front", "updated back");
        ResponseEntity<FlashcardDto.FlashcardResponse> response = controller.update(flashcardId, req, principal);

        assertEquals(200, response.getStatusCode().value());
        verify(managementUseCase).updateFlashcardIfAuthorized(any(), eq(ownerId), eq(false));
    }

    // Test 2: Non-owner update on PRIVATE flashcard → 403
    @Test
    void nonOwnerCannotUpdatePrivateFlashcard() {
        String email = "other@example.com";
        UUID otherId = setupUser(email);
        UUID flashcardId = UUID.randomUUID();
        UUID unitId = UUID.randomUUID();
        var principal = new User(email, "", List.of());

        doThrow(new AccessDeniedException("Cannot update another user's PRIVATE flashcard"))
                .when(managementUseCase).updateFlashcardIfAuthorized(any(), eq(otherId), eq(false));

        var req = new FlashcardDto.UpdateRequest(unitId, "front", "back");
        ResponseEntity<FlashcardDto.FlashcardResponse> response = controller.update(flashcardId, req, principal);

        assertEquals(403, response.getStatusCode().value());
    }

    // Test 3: Admin can update GLOBAL flashcard → 200
    @Test
    void adminCanUpdateGlobalFlashcard() {
        String email = "admin@example.com";
        UUID adminId = setupAdmin(email);
        UUID flashcardId = UUID.randomUUID();
        UUID unitId = UUID.randomUUID();
        var principal = new User(email, "", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        Flashcard saved = new Flashcard(flashcardId, unitId, "updated front", "updated back",
                LocalDateTime.now(), LocalDateTime.now(), Visibility.GLOBAL, null);
        when(managementUseCase.updateFlashcardIfAuthorized(any(), eq(adminId), eq(true)))
                .thenReturn(saved);

        var req = new FlashcardDto.UpdateRequest(unitId, "updated front", "updated back");
        ResponseEntity<FlashcardDto.FlashcardResponse> response = controller.update(flashcardId, req, principal);

        assertEquals(200, response.getStatusCode().value());
        verify(managementUseCase).updateFlashcardIfAuthorized(any(), eq(adminId), eq(true));
    }

    // Test 4: Non-admin user on GLOBAL flashcard → 403
    @Test
    void nonAdminCannotUpdateGlobalFlashcard() {
        String email = "user@example.com";
        UUID userId = setupUser(email);
        UUID flashcardId = UUID.randomUUID();
        UUID unitId = UUID.randomUUID();
        var principal = new User(email, "", List.of());

        doThrow(new AccessDeniedException("Only admins can update GLOBAL flashcards"))
                .when(managementUseCase).updateFlashcardIfAuthorized(any(), eq(userId), eq(false));

        var req = new FlashcardDto.UpdateRequest(unitId, "front", "back");
        ResponseEntity<FlashcardDto.FlashcardResponse> response = controller.update(flashcardId, req, principal);

        assertEquals(403, response.getStatusCode().value());
    }

    // ======================== DELETE ========================

    // Test 5: Owner can delete their own PRIVATE flashcard → 204
    @Test
    void ownerCanDeletePrivateFlashcard() {
        String email = "owner@example.com";
        UUID ownerId = setupUser(email);
        UUID flashcardId = UUID.randomUUID();
        var principal = new User(email, "", List.of());

        doNothing().when(managementUseCase).deleteFlashcardIfAuthorized(eq(flashcardId), eq(ownerId), eq(false));

        ResponseEntity<Void> response = controller.delete(flashcardId, principal);

        assertEquals(204, response.getStatusCode().value());
        verify(managementUseCase).deleteFlashcardIfAuthorized(eq(flashcardId), eq(ownerId), eq(false));
    }

    // Test 6: Non-owner delete on PRIVATE flashcard → 403
    @Test
    void nonOwnerCannotDeletePrivateFlashcard() {
        String email = "other@example.com";
        UUID otherId = setupUser(email);
        UUID flashcardId = UUID.randomUUID();
        var principal = new User(email, "", List.of());

        doThrow(new AccessDeniedException("Cannot delete another user's PRIVATE flashcard"))
                .when(managementUseCase).deleteFlashcardIfAuthorized(eq(flashcardId), eq(otherId), eq(false));

        ResponseEntity<Void> response = controller.delete(flashcardId, principal);

        assertEquals(403, response.getStatusCode().value());
    }

    // Test 7: Admin can delete GLOBAL flashcard → 204
    @Test
    void adminCanDeleteGlobalFlashcard() {
        String email = "admin@example.com";
        UUID adminId = setupAdmin(email);
        UUID flashcardId = UUID.randomUUID();
        var principal = new User(email, "", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        doNothing().when(managementUseCase).deleteFlashcardIfAuthorized(eq(flashcardId), eq(adminId), eq(true));

        ResponseEntity<Void> response = controller.delete(flashcardId, principal);

        assertEquals(204, response.getStatusCode().value());
        verify(managementUseCase).deleteFlashcardIfAuthorized(eq(flashcardId), eq(adminId), eq(true));
    }

    // Test 8: Non-admin user on GLOBAL flashcard delete → 403
    @Test
    void nonAdminCannotDeleteGlobalFlashcard() {
        String email = "user@example.com";
        UUID userId = setupUser(email);
        UUID flashcardId = UUID.randomUUID();
        var principal = new User(email, "", List.of());

        doThrow(new AccessDeniedException("Only admins can delete GLOBAL flashcards"))
                .when(managementUseCase).deleteFlashcardIfAuthorized(eq(flashcardId), eq(userId), eq(false));

        ResponseEntity<Void> response = controller.delete(flashcardId, principal);

        assertEquals(403, response.getStatusCode().value());
    }
}
