package com.team04.global.event;

public enum ReportTargetType {
    IDEA("아이디어"),
    FUNDING("펀딩"),
    DISPUTE("분쟁");

    private final String description;

    ReportTargetType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
