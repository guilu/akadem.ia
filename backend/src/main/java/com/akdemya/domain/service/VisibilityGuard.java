package com.akdemya.domain.service;

import com.akdemya.domain.model.Visibility;

import java.util.UUID;

/**
 * Pure domain service that enforces the visibility/scope access-control rule.
 * No Spring annotations — remains framework-agnostic.
 */
public class VisibilityGuard {

  /**
   * Returns true when the requesting user is allowed to access the resource.
   * GLOBAL resources are always accessible; PRIVATE resources are only accessible to the owner.
   */
  public boolean canAccess(Visibility visibility, UUID ownerId, UUID requestingUserId) {
    if (visibility == null || visibility == Visibility.GLOBAL) {
      return true;
    }
    // PRIVATE
    return ownerId != null && ownerId.equals(requestingUserId);
  }

  /**
   * Throws {@link SecurityException} when {@link #canAccess} returns false.
   */
  public void checkAccess(Visibility visibility, UUID ownerId, UUID requestingUserId) {
    if (!canAccess(visibility, ownerId, requestingUserId)) {
      throw new SecurityException("Access denied: resource is private");
    }
  }
}
