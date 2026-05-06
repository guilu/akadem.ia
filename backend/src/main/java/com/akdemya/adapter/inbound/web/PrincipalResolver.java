package com.akdemya.adapter.inbound.web;

import com.akdemya.domain.model.AppUser;
import com.akdemya.domain.port.out.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Component
public class PrincipalResolver {

    private final UserRepository userRepo;

    public PrincipalResolver(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    public UUID requireUserId(User principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return userRepo.findByEmail(principal.getUsername())
            .map(AppUser::getId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }
}
