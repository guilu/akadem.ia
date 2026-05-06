package com.akdemya.domain.port.in;

public interface RefreshTokenUseCase {
    record RefreshResult(String accessToken, String role, String error) {
        public static RefreshResult success(String accessToken, String role) {
            return new RefreshResult(accessToken, role, null);
        }
        public static RefreshResult failure(String error) {
            return new RefreshResult(null, null, error);
        }
        public boolean isSuccess() { return error == null; }
    }

    RefreshResult refresh(String rawToken);
    void revoke(String rawToken);
}
