package uk.gov.moj.cpp.results.domain.transformation.transform;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TransformFactory {
    private Map<String, List<ResultsEventTransformer>> transformEventMap;


    public TransformFactory() {
        transformEventMap = new HashMap<>();

        addInstance(MasterDefendantIdEventTransformer.getEventAndJsonPaths().keySet(), new MasterDefendantIdEventTransformer());
        addInstance(CourtProceedingsInitiatedEventTransformer.getEventAndJsonPaths().keySet(), new CourtProceedingsInitiatedEventTransformer());

    }

    private void addInstance(final Set<String> keySet, final ResultsEventTransformer eventTransformer) {
        keySet.forEach(key -> transformEventMap.compute(key, (s, hearingEventTransformers) -> {
                    if (hearingEventTransformers == null) {
                        hearingEventTransformers = new ArrayList<>();
                    }
                    hearingEventTransformers.add(eventTransformer);
                    return hearingEventTransformers;
                }
        ));
    }

    public List<ResultsEventTransformer> getEventTransformer(String eventName) {
        return transformEventMap.get(eventName);
    }
}
