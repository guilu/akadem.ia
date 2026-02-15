package com.akdemya.application.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExamScoringCalculatorTest {

  private final ExamScoringCalculator calculator = new ExamScoringCalculator();

  @Test
  void zeroWrongKeepsCorrect() {
    var result = calculator.compute(10, 7, 0);
    assertEquals(0, result.penalty());
    assertEquals(7, result.net());
  }

  @Test
  void wrongThreeRestOneCorrect() {
    var result = calculator.compute(10, 7, 3);
    assertEquals(1, result.penalty());
    assertEquals(6, result.net());
  }

  @Test
  void wrongFivePenaltyOne() {
    var result = calculator.compute(10, 7, 5);
    assertEquals(1, result.penalty());
    assertEquals(6, result.net());
  }

  @Test
  void netNeverBelowZero() {
    var result = calculator.compute(10, 0, 6);
    assertEquals(2, result.penalty());
    assertEquals(0, result.net());
  }
}
