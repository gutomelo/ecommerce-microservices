package com.ecommerce.platform.common;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PageResponseTest {

    @Test
    void fromPageWithoutMapper() {
        var page = new PageImpl<>(List.of("a", "b"), PageRequest.of(0, 2), 5);

        PageResponse<String> response = PageResponse.from(page);

        assertThat(response.content()).containsExactly("a", "b");
        assertThat(response.page()).isZero();
        assertThat(response.size()).isEqualTo(2);
        assertThat(response.totalElements()).isEqualTo(5);
        assertThat(response.totalPages()).isEqualTo(3);
        assertThat(response.last()).isFalse();
    }

    @Test
    void fromPageWithMapper() {
        var page = new PageImpl<>(List.of(1, 2), PageRequest.of(0, 2), 2);

        PageResponse<String> response = PageResponse.from(page, i -> "item-" + i);

        assertThat(response.content()).containsExactly("item-1", "item-2");
        assertThat(response.last()).isTrue();
    }
}
