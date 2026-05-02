package com.akdemya.adapter.inbound.web.dto;

import java.util.UUID;

public record CreateIntentResponse(String clientSecret, UUID downloadToken) {}
