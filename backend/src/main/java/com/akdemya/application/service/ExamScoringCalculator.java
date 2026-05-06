package com.akdemya.application.service;

public class ExamScoringCalculator {

  public record ScoringResult(int total, int correct, int wrong, int penalty, int net, double percentage) {
  }

  public ScoringResult compute(int total, int correct, int wrong, int penaltyRatio) {
    int penalty = penaltyRatio > 0 ? wrong / penaltyRatio : 0;
    int net = Math.max(0, correct - penalty);
    double percentage = total == 0 ? 0.0 : (net * 100.0 / total);
    return new ScoringResult(total, correct, wrong, penalty, net, percentage);
  }
}
