package com.ecommerce.platform.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BaseAuditEntityTest {

    static class SampleAuditEntity extends BaseAuditEntity {
    }

    @Test
    void onCreateSetsCreatedAndUpdatedAt() {
        SampleAuditEntity entity = new SampleAuditEntity();

        entity.onCreate();

        assertThat(entity.getCreatedAt()).isNotNull();
        assertThat(entity.getUpdatedAt()).isNotNull();
        assertThat(entity.getCreatedAt()).isEqualTo(entity.getUpdatedAt());
    }

    @Test
    void onUpdateChangesOnlyUpdatedAt() throws InterruptedException {
        SampleAuditEntity entity = new SampleAuditEntity();
        entity.onCreate();
        var createdAt = entity.getCreatedAt();

        Thread.sleep(5);
        entity.onUpdate();

        assertThat(entity.getCreatedAt()).isEqualTo(createdAt);
        assertThat(entity.getUpdatedAt()).isAfterOrEqualTo(createdAt);
    }
}
