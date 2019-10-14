package uk.gov.moj.cpp.hearing.domain.transformation.ctl;

import uk.gov.justice.core.courts.HearingResultsAdded;
import uk.gov.moj.cpp.coredomain.transform.TransformQuery;
import uk.gov.moj.cpp.coredomain.transform.TransformReport;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ResultsTransformReport {


    public final static String[] EVENT_PACKAGES =  new String [] {
            "uk.gov.justice.core.courts"} ;
    public final static String MASTER_PACKAGE_PREFIX="resultsmaster";

    public TransformReport query() throws IOException {
        return query("../../..");
    }

    public TransformReport query(String projectRoot) throws IOException {
        Map<String, Class> eventName2Class = new HashMap<>();
        // class scanner not working - map classes manually
        eventName2Class.put("results.hearing-results-added", HearingResultsAdded.class);
        TransformReport report = (new TransformQuery()).compare(projectRoot,
                Arrays.asList(
                        "/results-event/results-event-listener/src/raml/results-event-listener.messaging.raml"),
                MASTER_PACKAGE_PREFIX, eventName2Class);
        System.out.println("\r\n\r\n*****************results::");
        report.printOut();
        return report;

    }

    public static void main(String[] args) throws IOException{
        (new ResultsTransformReport()).query(".");
    }

}
