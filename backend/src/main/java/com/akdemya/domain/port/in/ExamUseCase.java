package com.akdemya.domain.port.in;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface ExamUseCase {
  StartResponse startExam(StartCommand command);

  StartResponse startRandomExam(StartRandomCommand command);

  StartResponse startExamFromSyllabus(StartFromSyllabusCommand command);

  SubmitResult submitExam(SubmitCommand command, String userEmail);

  AttemptResponse getAttempt(UUID attemptId, String userEmail);

  List<AttemptSummary> listAttempts(String userEmail);

  void updateAnswer(UpdateAnswerCommand command, String userEmail);

  SubmitResult getResult(UUID attemptId, String userEmail);

  record StartCommand(String userEmail, UUID userId, Map<UUID, Integer> unitCounts, int minutes, String difficulty) {
  }

  record StartRandomCommand(String userEmail, UUID userId, UUID subjectId, int count, int minutes, String difficulty) {
  }

  record StartFromSyllabusCommand(String userEmail, UUID userId, UUID syllabusId, List<UUID> subjectIds, int count,
      int minutes, String difficulty) {
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

  record AttemptQuestionData(UUID id, String text, List<AnswerData> answers, UUID selectedAnswerId) {
  }

  record AttemptResponse(UUID attemptId, int totalTimeSeconds, List<AttemptQuestionData> questions,
      int nextQuestionIndex, boolean finished) {
  }

  record AttemptSummary(UUID attemptId, String subjectName, java.time.OffsetDateTime startedAt,
      java.time.OffsetDateTime finishedAt, int totalTimeSeconds, Integer score, int totalQuestions,
      int correctCount, int wrongCount, double percent) {
  }

  record UpdateAnswerCommand(UUID attemptId, UUID questionId, UUID selectedAnswerId) {
  }
}
