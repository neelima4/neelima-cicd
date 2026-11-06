
package com.neelima.devops;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HelloControllerTest {

    @Test
    void shouldReturnWelcomeMessage() {
        HelloController controller =
            new HelloController();

        String result = controller.hello();

        assertEquals(
            "Hello Neelima! CI/CD deployment successful!",
            result
        );
    }
}
