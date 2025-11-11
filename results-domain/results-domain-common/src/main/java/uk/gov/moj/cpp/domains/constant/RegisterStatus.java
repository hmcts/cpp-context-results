package uk.gov.moj.cpp.domains.constant;

public enum RegisterStatus {

    RECORDED("RECORDED"),
    GENERATED("GENERATED"),
    NOTIFIED("NOTIFIED");

    private String status;

    RegisterStatus(final String status) {
        this.status = status;
    }
}
