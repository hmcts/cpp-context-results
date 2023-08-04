package uk.gov.moj.cpp.results.event.helper;

import static java.time.LocalDate.of;
import static java.util.Objects.nonNull;
import static java.util.Optional.empty;
import static java.util.UUID.randomUUID;
import static javax.json.JsonValue.NULL;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.core.courts.BailStatus;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.results.event.helper.resultdefinition.AllResultDefinitions;
import uk.gov.moj.cpp.results.event.helper.resultdefinition.ResultDefinition;
import uk.gov.moj.cpp.results.event.service.ReferenceDataService;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.ejb.Startup;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.JsonObject;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"squid:S00112", "squid:S2139"})
@Startup
@ApplicationScoped
public class ReferenceCache {

    private static final String RESULTS_HEARING_RESULTS_ADDED = "results.hearing-results-added";
    private static final Logger LOGGER = LoggerFactory.getLogger(ReferenceCache.class);
    protected static final String UNRECOVERABLE_SYSTEM_ERROR = "unrecoverable system error";
    private final ThreadLocal<JsonEnvelope> context = new ThreadLocal<>();
    @Inject
    ReferenceDataService referenceDataService;
    private final LoadingCache<CacheKey, Object> cache = CacheBuilder.newBuilder()
            .refreshAfterWrite(1, TimeUnit.DAYS)
            .expireAfterAccess(4, TimeUnit.HOURS)
            .concurrencyLevel(20)
            .maximumSize(100)
            .build(new CacheLoader<CacheKey, Object>() {
                @Override
                public Object load(final CacheKey key) {

                    if (Type.RESULT_DEFINITIONS.equals(key.getType())) {
                        return referenceDataService.loadAllResultDefinitions(context.get(), key.getReferenceDate());
                    } else if (Type.NATIONALITY.equals(key.getType())) {
                        return referenceDataService.getAllNationality(envelopeFrom(metadataBuilder().withName(RESULTS_HEARING_RESULTS_ADDED).withId(randomUUID()).build(), NULL));
                    } else if (Type.BAIL_STATUSES.equals(key.getType())) {
                        return referenceDataService.getAllBailStatuses(context.get());
                    } else if (Type.MODE_OF_TRIAL_REASONS.equals(key.getType())) {
                        return referenceDataService.getAllModeOfTrialReasons(context.get());
                    }
                    return null;
                }
            });


    public ResultDefinition getResultDefinitionById(final JsonEnvelope context, final LocalDate referenceDate, final UUID resultDefinitionId) {
        try {
            this.context.set(context);
            final AllResultDefinitions allResultDefinitions = (AllResultDefinitions) cache.get(new CacheKey(Type.RESULT_DEFINITIONS, referenceDate));

            return allResultDefinitions.getResultDefinitions().stream()
                    .filter(rd -> resultDefinitionId.equals(rd.getId()))
                    .findFirst()
                    .orElse(null);
        } catch (final ExecutionException executionException) {
            LOGGER.error("getResultDefinitionById reference data service not available", executionException);
            throw new RuntimeException(UNRECOVERABLE_SYSTEM_ERROR, executionException);
        } finally {
            this.context.remove();
        }
    }

    public Optional<JsonObject> getNationalityById(final UUID nationalityId) {
        try {
            final JsonObject response = (JsonObject) cache.get(new CacheKey(Type.NATIONALITY, of(2019, 1, 1)));

            return response.isEmpty() ? empty() : response.getJsonArray("countryNationality")
                    .getValuesAs(JsonObject.class).stream()
                    .filter(nationality -> nonNull(nationality.getString("id", null)))
                    .filter(nationality -> nationality.getString("id").equals(nationalityId.toString()))
                    .findFirst();
        } catch (final ExecutionException executionException) {
            LOGGER.error("getResultDefinitionById reference data service not available", executionException);
            throw new RuntimeException(UNRECOVERABLE_SYSTEM_ERROR, executionException);
        } finally {
            this.context.remove();
        }
    }

    public Optional<BailStatus> getBailStatusObjectByCode(final JsonEnvelope context, final String sjpBailStatus) {
        try {
            this.context.set(context);
            final List<BailStatus> bailStatuses = (List<BailStatus>) cache.get(new CacheKey(Type.BAIL_STATUSES, null));

            return bailStatuses
                    .stream()
                    .filter(bs -> sjpBailStatus.equals(bs.getCode()))
                    .findFirst();

        } catch (final ExecutionException executionException) {
            LOGGER.error("getBailStatusObjectByCode reference data service not available", executionException);
            throw new RuntimeException(UNRECOVERABLE_SYSTEM_ERROR, executionException);
        } finally {
            this.context.remove();
        }
    }

    private enum Type {
        RESULT_DEFINITIONS,
        NATIONALITY,
        BAIL_STATUSES,
        MODE_OF_TRIAL_REASONS
    }

    private class CacheKey {
        private final Type type;
        private final LocalDate referenceDate;

        private CacheKey(final Type type, final LocalDate referenceDate) {
            this.type = type;
            this.referenceDate = referenceDate;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final CacheKey cacheKey = (CacheKey) o;

            if (type != cacheKey.type) {
                return false;
            }
            return referenceDate != null ? referenceDate.equals(cacheKey.referenceDate) : cacheKey.referenceDate == null;
        }

        @Override
        public int hashCode() {
            int result = type != null ? type.hashCode() : 0;
            result = 31 * result + (referenceDate != null ? referenceDate.hashCode() : 0);
            return result;
        }

        public Type getType() {
            return type;
        }

        public LocalDate getReferenceDate() {
            return referenceDate;
        }
    }
}
