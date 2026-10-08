package com.example.tickets;

import com.example.tickets.domain.TicketAnalysis;
import com.example.tickets.exception.InvalidLlmOutputException;
import com.example.tickets.llm.AnalysisParser;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AnalysisParserTest {

    private final AnalysisParser parser = new AnalysisParser();

    private static String json(String category, String team, String confidence) {
        return """
                {"category":%s,"summary":"Import fails.","suggestedResponse":"We are on it.",
                 "recommendedTeam":%s,"confidence":%s}""".formatted(category, team, confidence);
    }

    @Test
    void parsesValidJson() {
        TicketAnalysis a = parser.parse(json("\"IMPORT_FAILURE\"", "\"IMPORT_ENGINEERING\"", "0.87"));
        assertThat(a.getCategory()).isEqualTo("IMPORT_FAILURE");
        assertThat(a.getRecommendedTeam()).isEqualTo("IMPORT_ENGINEERING");
        assertThat(a.getConfidence()).isEqualTo(0.87);
    }

    @Test
    void toleratesCodeFencesAndSurroundingText() {
        String wrapped = "Here you go:\n```json\n" + json("\"IMPORT_FAILURE\"", "\"IMPORT_ENGINEERING\"", "0.5") + "\n```";
        assertThat(parser.parse(wrapped).getCategory()).isEqualTo("IMPORT_FAILURE");
    }

    @Test
    void rejectsNonJson() {
        assertThatThrownBy(() -> parser.parse("I cannot help with that"))
                .isInstanceOf(InvalidLlmOutputException.class);
    }

    @Test
    void rejectsBlankAndNull() {
        assertThatThrownBy(() -> parser.parse("  ")).isInstanceOf(InvalidLlmOutputException.class);
        assertThatThrownBy(() -> parser.parse(null)).isInstanceOf(InvalidLlmOutputException.class);
    }

    @Test
    void rejectsMissingField() {
        String missing = "{\"category\":\"IMPORT_FAILURE\",\"summary\":\"x\",\"recommendedTeam\":\"GENERAL_SUPPORT\",\"confidence\":0.5}";
        assertThatThrownBy(() -> parser.parse(missing)).isInstanceOf(InvalidLlmOutputException.class);
    }

    @Test
    void rejectsUnexpectedFields() {
        String extra = json("\"IMPORT_FAILURE\"", "\"GENERAL_SUPPORT\"", "0.5").replace("}", ",\"hack\":\"x\"}");
        assertThatThrownBy(() -> parser.parse(extra)).isInstanceOf(InvalidLlmOutputException.class);
    }

    @Test
    void rejectsConfidenceOutOfRangeOrWrongType() {
        assertThatThrownBy(() -> parser.parse(json("\"A_B\"", "\"GENERAL_SUPPORT\"", "1.7")))
                .isInstanceOf(InvalidLlmOutputException.class);
        assertThatThrownBy(() -> parser.parse(json("\"A_B\"", "\"GENERAL_SUPPORT\"", "-0.1")))
                .isInstanceOf(InvalidLlmOutputException.class);
        assertThatThrownBy(() -> parser.parse(json("\"A_B\"", "\"GENERAL_SUPPORT\"", "\"high\"")))
                .isInstanceOf(InvalidLlmOutputException.class);
    }

    @Test
    void rejectsUnknownTeamAndBadCategory() {
        assertThatThrownBy(() -> parser.parse(json("\"IMPORT_FAILURE\"", "\"HACKERS\"", "0.5")))
                .isInstanceOf(InvalidLlmOutputException.class);
        assertThatThrownBy(() -> parser.parse(json("\"not snake case\"", "\"GENERAL_SUPPORT\"", "0.5")))
                .isInstanceOf(InvalidLlmOutputException.class);
    }
}
