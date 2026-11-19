package br.com.fiap.agroconnect_api.record;

import java.time.Instant;

public record ApiError(String code, String message, Object details, Instant timestamp) {

}
