package com.akdemya.adapter.inbound.web.dto;

import java.util.List;
import java.util.UUID;

public record QuestionDTO(UUID id, String text, List<AnswerDTO> answers) {
    public record AnswerDTO(UUID id, String text) {
    }
}
