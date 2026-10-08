package com.example.tickets.domain;

/** Allow-list of teams the LLM may recommend. Anything else is rejected. */
public enum RecommendedTeam {
    IMPORT_ENGINEERING,
    BILLING_SUPPORT,
    ACCOUNT_SUPPORT,
    PLATFORM_ENGINEERING,
    GENERAL_SUPPORT
}
