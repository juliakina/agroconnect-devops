package br.com.fiap.agroconnect_api.service;

import java.util.UUID;

public record AlertCreated(UUID userId, UUID alertId, String title, String message, String severity) {}
