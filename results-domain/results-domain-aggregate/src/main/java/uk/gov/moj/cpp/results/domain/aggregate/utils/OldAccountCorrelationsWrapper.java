package uk.gov.moj.cpp.results.domain.aggregate.utils;

import static java.util.Comparator.comparing;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;

import uk.gov.moj.cpp.results.domain.event.OldAccountCorrelations;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class OldAccountCorrelationsWrapper {

    private final List<OldAccountCorrelations> oldAccountCorrelationsList;

    public OldAccountCorrelationsWrapper(final List<OldAccountCorrelations> oldAccountCorrelationsList) {
        this.oldAccountCorrelationsList = oldAccountCorrelationsList;
    }

    public List<OldAccountCorrelations> getOldAccountCorrelationsList() {
        return oldAccountCorrelationsList;
    }

    public String getOldGobAccounts() {
        if (isNotEmpty(oldAccountCorrelationsList)) {
            final List<String> oldGobAccounts = oldAccountCorrelationsList.stream()
                    .map(OldAccountCorrelations::getGobAccountNumber)
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
                    .map(OldAccountCorrelations::getDivisionCode)
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
            List<OldAccountCorrelations> oldAccountCorrelationsCopy = new ArrayList<>(oldAccountCorrelationsList);
            oldAccountCorrelationsCopy.sort(comparing(OldAccountCorrelations::getCreatedTime).reversed());
            return oldAccountCorrelationsCopy.get(0).getAccountCorrelationId();
        }
        return null;
    }
}
