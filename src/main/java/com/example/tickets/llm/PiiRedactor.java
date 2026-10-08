package com.example.tickets.llm;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Best-effort masking of emails, card-like numbers and phone numbers before text leaves our system.
 * Regex redaction is a safety net, not a guarantee (see README limitations).
 */
@Component
public class PiiRedactor {

    private static final Pattern EMAIL = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");
    private static final Pattern CARD = Pattern.compile("\\b(?:\\d[ -]?){13,19}\\b");
    private static final Pattern PHONE = Pattern.compile("(?<!\\w)\\+?\\d[\\d\\s().-]{7,}\\d(?!\\w)");

    public String redact(String text) {
        if (text == null) {
            return null;
        }
        String result = EMAIL.matcher(text).replaceAll("[EMAIL]");
        result = CARD.matcher(result).replaceAll("[CARD]");
        result = PHONE.matcher(result).replaceAll("[PHONE]");
        return result;
    }
}
