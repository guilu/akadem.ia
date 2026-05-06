package com.akdemya.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VisibilityTest {

  @Test
  void enumHasTwoValues() {
    assertEquals(2, Visibility.values().length);
  }

  @Test
  void globalValueExists() {
    assertNotNull(Visibility.GLOBAL);
  }

  @Test
  void privateValueExists() {
    assertNotNull(Visibility.PRIVATE);
  }

  @Test
  void valueOfGlobal() {
    assertEquals(Visibility.GLOBAL, Visibility.valueOf("GLOBAL"));
  }

  @Test
  void valueOfPrivate() {
    assertEquals(Visibility.PRIVATE, Visibility.valueOf("PRIVATE"));
  }
}
