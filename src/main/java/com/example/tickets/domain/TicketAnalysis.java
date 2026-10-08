package com.example.tickets.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class TicketAnalysis {

    @Column(name = "analysis_category", length = 50)
    private String category;

    @Column(name = "analysis_summary", length = 1000)
    private String summary;

    @Column(name = "analysis_suggested_response", length = 4000)
    private String suggestedResponse;

    @Column(name = "analysis_recommended_team", length = 50)
    private String recommendedTeam;

    @Column(name = "analysis_confidence")
    private Double confidence;

    protected TicketAnalysis() { }

    public TicketAnalysis(String category, String summary, String suggestedResponse,
                          String recommendedTeam, Double confidence) {
        this.category = category;
        this.summary = summary;
        this.suggestedResponse = suggestedResponse;
        this.recommendedTeam = recommendedTeam;
        this.confidence = confidence;
    }

    public String getCategory() { return category; }
    public String getSummary() { return summary; }
    public String getSuggestedResponse() { return suggestedResponse; }
    public String getRecommendedTeam() { return recommendedTeam; }
    public Double getConfidence() { return confidence; }
}
