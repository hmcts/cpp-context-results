package uk.gov.moj.cpp.results.persist.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

@Entity
@IdClass(value = HearingKey.class)
@Table(name = "hearing")
@SuppressWarnings("squid:S1067")
public class Hearing {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Id
    @Column(name="person_id", nullable = false)
    private UUID personId;

    @Column(name = "hearing_type")
    private String hearingType;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "court_centre_name")
    private String courtCentreName;

    @Column(name = "court_code")
    private String courtCode;

    @Column(name = "judge_name")
    private String judgeName;

    @Column(name = "prosecutor_name")
    private String prosecutorName;

    @Column(name = "defence_name")
    private String defenceName;

    public Hearing() {
        // for JPA
    }

    public Hearing(final UUID id, final UUID personId, final String hearingType,
                   final LocalDate startDate, final String courtCentreName, final String courtCode,
                   final String judgeName, final String prosecutorName, final String defenceName) {
        this.id = id;
        this.personId = personId;
        this.hearingType = hearingType;
        this.startDate = startDate;
        this.courtCentreName = courtCentreName;
        this.courtCode = courtCode;
        this.judgeName = judgeName;
        this.prosecutorName = prosecutorName;
        this.defenceName = defenceName;
    }

    public UUID getId() {
        return id;
    }

    public UUID getPersonId() {
        return personId;
    }

    public String getHearingType() {
        return hearingType;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public String getCourtCentreName() {
        return courtCentreName;
    }

    public String getCourtCode() {
        return courtCode;
    }

    public String getJudgeName() {
        return judgeName;
    }

    public String getProsecutorName() {
        return prosecutorName;
    }

    public String getDefenceName() {
        return defenceName;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, personId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Hearing other = (Hearing) obj;
        return Objects.equals(this.id, other.id) && Objects.equals(this.personId, other.personId);
    }

}
