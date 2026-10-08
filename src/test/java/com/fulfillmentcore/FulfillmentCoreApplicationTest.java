package com.fulfillmentcore;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;

class FulfillmentCoreApplicationTest {

    @Test
    void applicationIsBootstrappedAsSpringBootApplication() {
        assertThat(FulfillmentCoreApplication.class)
                .hasAnnotation(SpringBootApplication.class);
    }
}

