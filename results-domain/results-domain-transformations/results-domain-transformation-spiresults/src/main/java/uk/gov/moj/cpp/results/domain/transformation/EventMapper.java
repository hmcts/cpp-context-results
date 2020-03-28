package uk.gov.moj.cpp.results.domain.transformation;

import static com.google.common.collect.Lists.newArrayList;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EventMapper {

    private EventMapper() {
    }

    private static final Map<String, List<String>> EVENT_MAP = new HashMap();

    static {
        EVENT_MAP.put("results.hearing-results-added", newArrayList(
                "$.hearing.id"));
    }

    public static Collection getEventNames() {
        return EVENT_MAP.keySet();
    }

    public static List<String> getMappedJsonPaths(String eventName) {
        return EVENT_MAP.get(eventName);
    }

}
