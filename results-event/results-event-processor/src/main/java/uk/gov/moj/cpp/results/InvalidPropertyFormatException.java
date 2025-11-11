package uk.gov.moj.cpp.results;

public class InvalidPropertyFormatException extends RuntimeException {
    public InvalidPropertyFormatException(Exception e) {
        super(e);
    }
}
