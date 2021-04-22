package uk.gov.moj.cpp.results.it.utils;

public class UriConstants {

    public static final String HOST = System.getProperty("INTEGRATION_HOST_KEY", "localhost");
    public static final int PORT = 8080;
    public static final String BASE_URI = "http://" + HOST + ":" + PORT;
}
