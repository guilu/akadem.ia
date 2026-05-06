package com.akdemya.adapter.infrastructure.security;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class OAuth2CodeStoreTest {

    private final OAuth2CodeStore store = new OAuth2CodeStore();

    @Test
    void storeAndExchangeReturnsCorrectJwtAndRole() {
        String jwt = "test.jwt.token";
        String refreshToken = "test-refresh-token";
        String role = "STUDENT";

        String code = store.store(jwt, refreshToken, role);

        assertNotNull(code);
        assertFalse(code.isBlank());

        Optional<OAuth2CodeStore.ExchangeResult> result = store.exchange(code);

        assertTrue(result.isPresent());
        assertEquals(jwt, result.get().accessToken());
        assertEquals(refreshToken, result.get().refreshToken());
        assertEquals(role, result.get().role());
    }

    @Test
    void secondExchangeOfSameCodeReturnsEmpty() {
        String code = store.store("jwt", "refresh", "ADMIN");

        store.exchange(code); // first use
        Optional<OAuth2CodeStore.ExchangeResult> second = store.exchange(code);

        assertTrue(second.isEmpty());
    }

    @Test
    void expiredCodeReturnsEmpty() throws Exception {
        String code = store.store("expired.jwt", "refresh", "STUDENT");

        // Use reflection to manipulate the stored entry's expiresAt to be in the past
        Field codesField = OAuth2CodeStore.class.getDeclaredField("codes");
        codesField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, OAuth2CodeStore.CodeEntry> codes =
            (Map<String, OAuth2CodeStore.CodeEntry>) codesField.get(store);

        OAuth2CodeStore.CodeEntry entry = codes.get(code);
        Field expiresAtField = OAuth2CodeStore.CodeEntry.class.getDeclaredField("expiresAt");
        expiresAtField.setAccessible(true);
        expiresAtField.set(entry, Instant.now().minusSeconds(1));

        Optional<OAuth2CodeStore.ExchangeResult> result = store.exchange(code);

        assertTrue(result.isEmpty());
    }

    @Test
    void unknownCodeReturnsEmpty() {
        Optional<OAuth2CodeStore.ExchangeResult> result = store.exchange("nonexistent-code");
        assertTrue(result.isEmpty());
    }
}
