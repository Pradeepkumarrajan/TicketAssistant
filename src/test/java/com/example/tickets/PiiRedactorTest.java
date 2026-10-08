package com.example.tickets;

import com.example.tickets.llm.PiiRedactor;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PiiRedactorTest {

    @Test
    void masksEmailCardAndPhone() {
        String out = new PiiRedactor().redact(
                "Mail pradee@gmail.com, call +91 98765 43210, card 4111 1111 1111 1111. Import fails at 80%.");
        assertThat(out).doesNotContain("jane@example.com", "4111", "98765 43210");
        assertThat(out).contains("[EMAIL]", "[CARD]", "[PHONE]", "Import fails at 80%");
    }
}
