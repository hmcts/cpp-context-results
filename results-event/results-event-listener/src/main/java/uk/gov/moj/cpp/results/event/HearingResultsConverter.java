package uk.gov.moj.cpp.results.event;

import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.moj.cpp.domains.results.result.ResultLevel;
import uk.gov.moj.cpp.results.persist.entity.Hearing;
import uk.gov.moj.cpp.results.persist.entity.HearingResult;
import uk.gov.moj.cpp.results.persist.entity.Defendant;
import uk.gov.moj.cpp.results.persist.entity.ResultPrompt;
import uk.gov.moj.cpp.results.persist.entity.VariantDirectory;

import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.UUID.fromString;
import static java.util.stream.Collectors.toConcurrentMap;
import static java.util.stream.Collectors.toList;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonString;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@SuppressWarnings({"unchecked", "squid:S1845"})
final class HearingResultsConverter {

    public static final String HEARING_ID = "hearingId";
    public static final String VARIANTS = "variants";
    public static final String HEARING_RESULTS = "hearingResults";
    public static final String HEARINGS = "hearings";
    public static final String DEFENDANTS = "defendants";

    private final JsonObject hearing;
    private final UUID hearingId;
    private final JsonObject courtCentre;
    private final String judgeName;
    private final String prosecutorName;
    private final Map<UUID, String> defenceAdvocates;
    private final Map<UUID, JsonObject> defendants;
    private final Map<UUID, JsonObject> defendantAddresses;
    private final Map<UUID, String> caseUrns;
    private final Map<UUID, JsonObject> caseOffences;
    private final JsonArray variants;

    private HearingResultsConverter(final JsonObject source) {
        this.hearing = source.getJsonObject("hearing");
        this.hearingId = fromString(this.hearing.getString("id"));
        this.courtCentre = this.hearing.getJsonObject("courtCentre");
        final List<JsonObject> attendees = this.hearing.getJsonArray("attendees").getValuesAs(JsonObject.class);
        this.judgeName = attendees.stream().filter(a -> "JUDGE".equals(a.getString("type")))
                .map(v -> buildFullName(v))
                .findFirst().orElse("N/A");
        this.prosecutorName = attendees.stream()
                .filter(a -> "PROSECUTIONADVOCATE".equals(a.getString("type")))
                .map(v -> buildFullName(v))
                .findFirst().orElse("N/A");
        this.defendants = hearing.getJsonArray(DEFENDANTS).getValuesAs(JsonObject.class).stream()
                .distinct()
                .collect(toConcurrentMap(k -> fromString(k.getString("id")),
                        Function.identity()));
        this.defendantAddresses = hearing.getJsonArray(DEFENDANTS).getValuesAs(JsonObject.class).stream()
                .distinct()
                .collect(toConcurrentMap(k -> fromString(k.getJsonObject("person").getString("id")),
                        v -> v.getJsonObject("person").getJsonObject("address")));
        final List<JsonObject> advocates = attendees.stream()
                .filter(a -> "DEFENCEADVOCATE".equals(a.getString("type")))
                .distinct()
                .collect(toList());
        this.defenceAdvocates = new ConcurrentHashMap<>();
        this.defendants.values().stream()
                .forEach(defendant -> advocates.forEach(advocate -> advocate.getJsonArray("defendantIds").getValuesAs(JsonString.class)
                        .stream()
                        .forEach(defendantId -> {
                            if (defendant.getString("id").equalsIgnoreCase(defendantId.getString())) {
                                this.defenceAdvocates.putIfAbsent(
                                        fromString(defendant.getJsonObject("person").getString("id")), buildFullName(advocate));
                            }
                        })));
        final List<JsonObject> cases = defendants.values().stream()
                .flatMap(d -> d.getJsonArray("cases").getValuesAs(JsonObject.class).stream())
                .distinct()
                .collect(toList());

        this.caseUrns = cases.stream()
                .distinct()
                .collect(Collectors.toMap(key -> fromString(key.getString("id")), v -> v.getString("urn"), (k, v) -> k));

        this.caseOffences = cases.stream()
                .flatMap(c -> c.getJsonArray("offences").getValuesAs(JsonObject.class).stream())
                .distinct()
                .collect(toConcurrentMap(key -> fromString(key.getString("id")),
                        Function.identity()));
        this.variants = source.getJsonArray(VARIANTS);

    }

    public static HearingResultsConverter withJsonObject(final JsonObject source) {
        Objects.requireNonNull(source, "source");
        return new HearingResultsConverter(source);
    }

    public Map<String, Object> convert() {
        final Map<String, Object> hearingResults = new HashMap<>();
        hearingResults.put(HEARING_ID, this.hearingId);
        this.hearing.getJsonArray("sharedResultLines").getValuesAs(JsonObject.class).forEach(resultLine -> {
            ((Set<Defendant>) hearingResults.computeIfAbsent(DEFENDANTS, v -> new HashSet<>())).add(convertToPerson(resultLine));
            ((Set<Hearing>) hearingResults.computeIfAbsent(HEARINGS, v -> new HashSet<>())).add(convertToHearing(resultLine));
            ((List<HearingResult>) hearingResults.computeIfAbsent(HEARING_RESULTS, v -> new ArrayList<>())).add(convertToHearingResult(resultLine));
        });

        hearingResults.put("variants", convertToVariantDirectories(variants, defendants));
        return hearingResults;
    }

    private static List<VariantDirectory> convertToVariantDirectories(final JsonArray variants, Map<UUID, JsonObject> defendants) {

        final List<VariantDirectory> variantDirectories = new ArrayList<>();
        if (variants != null) {
            variants.getValuesAs(JsonObject.class).stream()
                    .forEach(v -> {
                                final JsonObject key = v.getJsonObject("key");
                                variantDirectories.add(new VariantDirectory(UUID.randomUUID(),
                                        fromString(key.getString("hearingId")),
                                        fromString(defendants.get(fromString(key.getString("defendantId"))).getJsonObject("person").getString("id")),
                                        fromString(key.getString("defendantId")),
                                        fromString(key.getString("nowsTypeId")),
                                        getUserGroupFromKey(key.getJsonArray("usergroups")),
                                        fromString(v.getString("materialId")),
                                        v.getString("description", null),
                                        v.getString("templateName", null),
                                        v.getString("status", "BUILDING").trim().toUpperCase()));
                            }
                    );
        }
        return variantDirectories;

    }

    private static List<String> getUserGroupFromKey(final JsonArray usergroups) {
        return usergroups.getValuesAs(JsonString.class).stream().map(jsonString -> jsonString.getString())
                .collect(Collectors.toList());
    }

    private Hearing convertToHearing(final JsonObject source) {
        final UUID personId = findPersonId(source);
        final String hearingType = hearing.getString("hearingType");
        final LocalDate startDate = ZonedDateTimes.fromJsonString(hearing.getJsonString("startDateTime")).toLocalDate();
        final String courtCentreName = courtCentre.getString("courtCentreName");
        final String courtCode = "433"; //TODO **GPE-3393 - OUT OF SCOPE**
        final String defenceName = defenceAdvocates.getOrDefault(personId, "N/A");
        return new Hearing(this.hearingId, personId, hearingType, startDate,
                courtCentreName, courtCode, this.judgeName, this.prosecutorName, defenceName);
    }

    private Defendant convertToPerson(final JsonObject source) {
        final JsonObject person = findPerson(source);
        final UUID personId = findPersonId(source);
        final String firstName = person.getString("firstName");
        final String lastName = person.getString("lastName");
        final LocalDate dateOfBirth = ofNullable(person.getString("dateOfBirth", null)).map(LocalDates::from).orElse(null);
        final String address1 = defendantAddresses.get(personId).getString("address1", null);
        final String address2 = defendantAddresses.get(personId).getString("address2", null);
        final String address3 = defendantAddresses.get(personId).getString("address3", null);
        final String address4 = defendantAddresses.get(personId).getString("address4", null);
        final String postCode = defendantAddresses.get(personId).getString("postCode", null);
        return Defendant.builder().withId(personId).withHearingId(this.hearingId).withFirstName(firstName)
                .withLastName(lastName).withDateOfBirth(dateOfBirth).withAddress1(address1).withAddress2(address2)
                .withAddress3(address3).withAddress4(address4).withPostCode(postCode).build();
    }

    private HearingResult convertToHearingResult(final JsonObject resultLine) {
        final UUID caseId = fromString(resultLine.getString("caseId"));
        final UUID personId = findPersonId(resultLine);
        final UUID offenceId = fromString(resultLine.getString("offenceId"));
        final JsonObject courtClerk = resultLine.getJsonObject("courtClerk");
        final HearingResult.Builder hearingResultBuilder = HearingResult.builder()
                .withId(fromString(resultLine.getString("id")))
                .withCaseId(caseId)
                .withUrn(caseUrns.get(caseId))
                .withHearingId(this.hearingId)
                .withPersonId(personId)
                .withOffenceId(offenceId)
                .withOffenceTitle(caseOffences.get(offenceId).getString("wording", null))
                .withResultLabel(resultLine.getString("label"))
                .withStartDate(ofNullable(caseOffences.get(offenceId).getString("startDate", null)).map(LocalDates::from).orElse(null))
                .withEndDate(ofNullable(caseOffences.get(offenceId).getString("endDate", null)).map(LocalDates::from).orElse(null))
                .withResultLevel(ResultLevel.valueOf(resultLine.getString("level")))
                .withCourt(courtCentre.getString("courtCentreName", null))
                .withCourtRoom(courtCentre.getString("courtRoomName", null))
                .withClerkOfTheCourtId(nonNull(courtClerk) ? fromString(courtClerk.getString("id")) : null)
                .withClerkOfTheCourtFirstName(nonNull(courtClerk) ? courtClerk.getString("firstName", null) : null)
                .withClerkOfTheCourtLastName(nonNull(courtClerk) ? courtClerk.getString("lastName", null) : null)
                .withResultPrompts(resultLine.getJsonArray("prompts")
                        .getValuesAs(JsonObject.class)
                        .stream()
                        .map(promptJson -> ResultPrompt.builder()
                                .withId(fromString(promptJson.getString("id")))
                                .withLabel(promptJson.getString("label"))
                                .withValue(promptJson.getString("value"))
                                .withHearingResultId(fromString(resultLine.getString("id")))
                                .build())
                        .collect(toList()))
                .withLastSharedDateTime(
                        ofNullable(resultLine.getString("lastSharedDateTime", null))
                                .map(ZonedDateTime::parse)
                                .orElse(null))
                .withOrderedDate(ofNullable(resultLine.getString("orderedDate",null)).map(LocalDates::from).orElse(null));
        if (caseOffences.get(offenceId).containsKey("plea")) {
            final JsonObject plea = caseOffences.get(offenceId).getJsonObject("plea");
            hearingResultBuilder.withPleaDate(LocalDates.from(plea.getString("date")));
            hearingResultBuilder.withPleaValue(plea.getString("value"));
        }
        if (caseOffences.get(offenceId).containsKey("convictionDate")) {
            hearingResultBuilder.withConvictionDate(LocalDates
                    .from(caseOffences.get(offenceId).getString("convictionDate")));
        }
        if (caseOffences.get(offenceId).containsKey("verdict")) {
            final JsonObject verdict = caseOffences.get(offenceId).getJsonObject("verdict");
            hearingResultBuilder.withVerdictDate(LocalDates.from(verdict.getString("verdictDate")));
            hearingResultBuilder.withVerdictCategory(verdict.getString("verdictCategory"));
            hearingResultBuilder
                            .withVerdictDescription(verdict.getString("verdictDescription", null));
        }
        return hearingResultBuilder.build();
    }

    private static String buildFullName(final JsonObject jsonObject) {
        return new StringBuilder(jsonObject.getString("firstName", ""))
                .append(" ")
                .append(jsonObject.getString("lastName", ""))
                .append(" ").append(jsonObject.getString("status", ""))
                .toString()
                .trim();
    }

    private UUID findPersonId(final JsonObject source) {
        return fromString(findPerson(source).getString("id"));
    }

    private JsonObject findPerson(final JsonObject source) {
        return defendants.get(fromString(source.getString("defendantId"))).getJsonObject("person");
    }
}
