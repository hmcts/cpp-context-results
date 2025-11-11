package uk.gov.moj.cpp.results.query.view.service;

import uk.gov.moj.cpp.results.persist.DefendantTrackingStatusRepository;
import uk.gov.moj.cpp.results.persist.entity.DefendantTrackingStatus;

import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

public class DefendantTrackingStatusService {

    @Inject
    private DefendantTrackingStatusRepository defendantTrackingStatusRepository;

    public List<DefendantTrackingStatus> findDefendantTrackingStatus(final List<UUID> defendantIds) {
        return defendantTrackingStatusRepository.findActiveDefendantTrackingStatusByDefendantIds(defendantIds);
    }
}
