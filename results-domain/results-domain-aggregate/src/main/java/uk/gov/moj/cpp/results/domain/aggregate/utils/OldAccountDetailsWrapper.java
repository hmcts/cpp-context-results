package uk.gov.moj.cpp.results.domain.aggregate.utils;

import static java.util.Comparator.comparing;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;

import uk.gov.moj.cpp.results.domain.event.OldAccountDetails;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class OldAccountDetailsWrapper {

    private final List<OldAccountDetails> oldAccountDetails;

    public OldAccountDetailsWrapper(final List<OldAccountDetails> oldAccountDetails) {
        this.oldAccountDetails = oldAccountDetails;
    }

    public List<OldAccountDetails> getOldAccountDetails() {
        return oldAccountDetails;
    }

    public String getOldGobAccounts() {
        if (isNotEmpty(oldAccountDetails)) {
            final List<String> oldGobAccounts = oldAccountDetails.stream()
                    .map(OldAccountDetails::getGobAccountNumber)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();

            if (isNotEmpty(oldGobAccounts)) {
                return oldGobAccounts.size() > 1 ? String.join(",", oldGobAccounts) : oldGobAccounts.get(0);
            }
        }
        return null;
    }

    public String getOldDivisionCodes() {
        if (isNotEmpty(oldAccountDetails)) {
            final List<String> oldDivisionCodes = oldAccountDetails.stream()
                    .map(OldAccountDetails::getDivisionCode)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();

            if (isNotEmpty(oldDivisionCodes)) {
                return oldDivisionCodes.size() > 1 ? String.join(",", oldDivisionCodes) : oldDivisionCodes.get(0);
            }
        }
        return null;
    }

    public UUID getRecentAccountCorrelationId() {
        if (isNotEmpty(oldAccountDetails)) {
            List<OldAccountDetails> oldAccountCorrelationsCopy = new ArrayList<>(oldAccountDetails);
            oldAccountCorrelationsCopy.sort(comparing(OldAccountDetails::getCreatedTime).reversed());
            return oldAccountCorrelationsCopy.get(0).getAccountCorrelationId();
        }
        return null;
    }
}
