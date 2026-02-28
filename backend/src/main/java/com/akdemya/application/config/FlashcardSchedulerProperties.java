package com.akdemya.application.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "app.flashcards")
public class FlashcardSchedulerProperties {

  private List<Integer> learningStepsMinutes = List.of(1, 10);
  private int easyIntervalDays = 4;
  private int reviewAgainRelearnMinutes = 10;

  public List<Integer> getLearningStepsMinutes() {
    return learningStepsMinutes;
  }

  public void setLearningStepsMinutes(List<Integer> learningStepsMinutes) {
    this.learningStepsMinutes = learningStepsMinutes;
  }

  public int getEasyIntervalDays() {
    return easyIntervalDays;
  }

  public void setEasyIntervalDays(int easyIntervalDays) {
    this.easyIntervalDays = easyIntervalDays;
  }

  public int getReviewAgainRelearnMinutes() {
    return reviewAgainRelearnMinutes;
  }

  public void setReviewAgainRelearnMinutes(int reviewAgainRelearnMinutes) {
    this.reviewAgainRelearnMinutes = reviewAgainRelearnMinutes;
  }
}
