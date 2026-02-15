package com.akdemya.domain.port.in;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface ExamUseCase {
  StartResponse startExam(StartCommand command);

  SubmitResult submitExam(SubmitCommand command);

  record StartCommand(String userEmail, Map<UUID, Integer> unitCounts, int minutes) {
  }

  record StartResponse(UUID attemptId, int totalTimeSeconds, List<QuestionData> questions) {
  }

  record QuestionData(UUID id, String text, List<AnswerData> answers) {
  }

  record AnswerData(UUID id, String text) {
  }

  record SubmitCommand(UUID attemptId, Map<UUID, UUID> selections) {
  }

  record SubmitResult(int total, int correct, int wrong, int penalty, int net, double percentage) {
  }
}
