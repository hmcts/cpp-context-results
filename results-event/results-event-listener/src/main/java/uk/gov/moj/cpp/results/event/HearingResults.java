package uk.gov.moj.cpp.results.event;

import uk.gov.justice.json.schemas.core.publichearingresulted.SharedHearing;
import uk.gov.justice.json.schemas.core.publichearingresulted.SharedVariant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"squid:S1068"})
public class HearingResults {

    private SharedHearing hearing;
    private List<SharedVariant> variants = new ArrayList<>();
    private ZonedDateTime sharedTime;
}
