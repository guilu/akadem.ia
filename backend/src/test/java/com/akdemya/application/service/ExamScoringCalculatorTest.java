package com.akdemya.application.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExamScoringCalculatorTest {

  private final ExamScoringCalculator calculator = new ExamScoringCalculator();

  @Test
  void zeroWrongKeepsCorrect() {
    var result = calculator.compute(10, 7, 0, 3);
    assertEquals(0, result.penalty());
    assertEquals(7, result.net());
  }

  @Test
  void wrongThreeRestOneCorrect() {
    var result = calculator.compute(10, 7, 3, 3);
    assertEquals(1, result.penalty());
    assertEquals(6, result.net());
  }

  @Test
  void wrongFivePenaltyOne() {
    var result = calculator.compute(10, 7, 5, 3);
    assertEquals(1, result.penalty());
    assertEquals(6, result.net());
  }

  @Test
  void netNeverBelowZero() {
    var result = calculator.compute(10, 0, 6, 3);
    assertEquals(2, result.penalty());
    assertEquals(0, result.net());
  }

  @Test
  void wrongSixRatioTwoPenaltyThree() {
    var result = calculator.compute(10, 5, 6, 2);
    assertEquals(3, result.penalty());
    assertEquals(2, result.net());
  }

  @Test
  void wrongOneRatioOnePenaltyOne() {
    var result = calculator.compute(10, 5, 1, 1);
    assertEquals(1, result.penalty());
    assertEquals(4, result.net());
  }

  @Test
  void wrongFourRatioFivePenaltyZero() {
    var result = calculator.compute(10, 5, 4, 5);
    assertEquals(0, result.penalty());
    assertEquals(5, result.net());
  }
}
