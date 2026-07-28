package com.ecommerce.platform.common;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class BaseEntityTest {

    static class SampleEntity extends BaseEntity {
        SampleEntity() {
            super();
        }

        SampleEntity(UUID id) {
            super(id);
        }
    }

    @Test
    void entitiesWithSameIdAreEqual() {
        UUID id = UUID.randomUUID();
        SampleEntity a = new SampleEntity(id);
        SampleEntity b = new SampleEntity(id);

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void entitiesWithDifferentIdAreNotEqual() {
        SampleEntity a = new SampleEntity(UUID.randomUUID());
        SampleEntity b = new SampleEntity(UUID.randomUUID());

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void entityWithNullIdIsNotEqualToOther() {
        SampleEntity a = new SampleEntity();
        SampleEntity b = new SampleEntity(UUID.randomUUID());

        assertThat(a).isNotEqualTo(b);
        assertThat(a).isEqualTo(a);
        assertThat(a).isNotEqualTo("not-an-entity");
        assertThat(a.hashCode()).isZero();
    }

    @Test
    void gettersAndSettersWork() {
        SampleEntity entity = new SampleEntity();
        UUID id = UUID.randomUUID();

        entity.setId(id);

        assertThat(entity.getId()).isEqualTo(id);
    }
}
