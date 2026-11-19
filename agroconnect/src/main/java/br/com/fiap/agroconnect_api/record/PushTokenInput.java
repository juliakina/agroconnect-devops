package br.com.fiap.agroconnect_api.record;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Locale;

public record PushTokenInput(@NotBlank @Size(max = 512) @Pattern(regexp = "(ExpoPushToken|ExponentPushToken)\\[[A-Za-z0-9_-]+\\]") String token, @NotBlank @Pattern(regexp = "android|ios") String platform) {

	public PushTokenInput {
		token = token == null ? null : token.trim();
		platform = platform == null ? null : platform.trim().toLowerCase(Locale.ROOT);
	}
}
