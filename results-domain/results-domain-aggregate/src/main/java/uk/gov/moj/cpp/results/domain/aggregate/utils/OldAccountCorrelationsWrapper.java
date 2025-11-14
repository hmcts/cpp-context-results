package uk.gov.moj.cpp.results.domain.aggregate.utils;

import static java.util.Comparator.comparing;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class OldAccountCorrelationsWrapper {

    private final List<OldAccountCorrelation> oldAccountCorrelationsList;

    public OldAccountCorrelationsWrapper(final List<OldAccountCorrelation> oldAccountCorrelationsList) {
        this.oldAccountCorrelationsList = oldAccountCorrelationsList;
    }

    public List<OldAccountCorrelation> getOldAccountCorrelationsList() {
        return oldAccountCorrelationsList;
    }

    public String getOldGobAccounts() {
        if (isNotEmpty(oldAccountCorrelationsList)) {
            final List<String> oldGobAccounts = oldAccountCorrelationsList.stream()
                    .map(OldAccountCorrelation::getGobAccountNumber)
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
        if (isNotEmpty(oldAccountCorrelationsList)) {
            final List<String> oldDivisionCodes = oldAccountCorrelationsList.stream()
                    .map(OldAccountCorrelation::getDivisionCode)
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

        if (isNotEmpty(oldAccountCorrelationsList)) {
            List<OldAccountCorrelation> oldAccountCorrelationsCopy = new ArrayList<>(oldAccountCorrelationsList);
            oldAccountCorrelationsCopy.sort(comparing(OldAccountCorrelation::getCreatedTime).reversed());
            return oldAccountCorrelationsCopy.get(0).getAccountCorrelationId();
        }
        return null;
    }
}
