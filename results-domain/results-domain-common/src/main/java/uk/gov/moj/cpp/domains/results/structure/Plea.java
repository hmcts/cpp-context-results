package uk.gov.moj.cpp.domains.results.structure;


import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;

public class Plea implements Serializable {
    private long serialVersionUID = -9176890205806560222L;
    private UUID id;
    private LocalDate date;
    private String value;
    private UUID enteredHearingId;

    public Plea(UUID id, LocalDate date, String value, UUID enteredHearingId) {
        this.id = id;
        this.date = date;
        this.value = value;
        this.enteredHearingId = enteredHearingId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final Plea plea = (Plea) o;

        if (id != null ? !id.equals(plea.id) : plea.id != null) {
            return false;
        }
        if (date != null ? !date.equals(plea.date) : plea.date != null) {
            return false;
        }
        if (value != null ? !value.equals(plea.value) : plea.value != null) {
            return false;
        }
        return enteredHearingId != null ? enteredHearingId.equals(plea.enteredHearingId) : plea.enteredHearingId == null;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (date != null ? date.hashCode() : 0);
        result = 31 * result + (value != null ? value.hashCode() : 0);
        result = 31 * result + (enteredHearingId != null ? enteredHearingId.hashCode() : 0);
        return result;
    }

    public UUID getId() {
        return id;
    }

    public LocalDate getDate() {
        return date;
    }

    public String getValue() {
        return value;
    }

    public UUID getEnteredHearingId() {
        return enteredHearingId;
    }
}
