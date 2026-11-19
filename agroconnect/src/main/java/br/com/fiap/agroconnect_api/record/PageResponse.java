package br.com.fiap.agroconnect_api.record;

import java.util.List;
import org.springframework.data.domain.Page;

public record PageResponse<T>(List<T> items, PageMeta page) {

	public static <T> PageResponse<T> from(Page<T> result) {
		return new PageResponse<>(result.getContent(), new PageMeta(result.getNumber() + 1,
				result.getSize(), result.getTotalElements(), Math.max(1, result.getTotalPages())));
	}
}
