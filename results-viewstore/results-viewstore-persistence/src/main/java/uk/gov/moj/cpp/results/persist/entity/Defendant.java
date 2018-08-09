package uk.gov.moj.cpp.results.persist.entity;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;

@Entity
@Table(name = "defendant")
@IdClass(value = PersonKey.class)
public class Defendant {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Id
    @Column(name = "hearing_id", nullable = false)
    private UUID hearingId;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "address_1", nullable = false)
    private String address1;

    @Column(name = "address_2")
    private String address2;

    @Column(name = "address_3")
    private String address3;

    @Column(name = "address_4")
    private String address4;

    @Column(name = "post_code")
    private String postCode;

    public Defendant() {
        // for JPA
    }

    private Defendant(final Builder builder) {
        this.id = builder.id;
        this.hearingId = builder.hearingId;
        this.firstName = builder.firstName;
        this.lastName = builder.lastName;
        this.dateOfBirth = builder.dateOfBirth;
        this.address1 = builder.address1;
        this.address2 = builder.address2;
        this.address3 = builder.address3;
        this.address4 = builder.address4;
        this.postCode = builder.postCode;
    }

    public UUID getId() {
        return id;
    }

    public UUID getHearingId() { return hearingId; }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public String getAddress1() {
        return address1;
    }

    public String getAddress2() {
        return address2;
    }

    public String getAddress3() {
        return address3;
    }

    public String getAddress4() {
        return address4;
    }

    public String getPostCode() {
        return postCode;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, hearingId);
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
        final Defendant other = (Defendant) obj;
        return Objects.equals(this.id, other.id) && Objects.equals(this.hearingId, other.hearingId);
    }

    public static Builder builder() {
        return new Builder();
    }
    public static final class Builder {

        private UUID id;
        private UUID hearingId;
        private String firstName;
        private String lastName;
        private LocalDate dateOfBirth;
        private String address1;
        private String address2;
        private String address3;
        private String address4;
        private String postCode;

        private Builder() {
        }
        
        public Builder withId(final UUID id) {
            this.id = id;
            return this;
        }
        public Builder withHearingId(final UUID hearingId) {
            this.hearingId = hearingId;
            return this;
        }
        public Builder withFirstName(final String firstName) {
            this.firstName = firstName;
            return this;
        }
        public Builder withLastName(final String lastName) {
            this.lastName = lastName;
            return this;
        }
        public Builder withDateOfBirth(final LocalDate dateOfBirth) {
            this.dateOfBirth = dateOfBirth;
            return this;
        }
        public Builder withAddress1(final String address1) {
            this.address1 = address1;
            return this;
        }
        public Builder withAddress2(final String address2) {
            this.address2 = address2;
            return this;
        }
        public Builder withAddress3(final String address3) {
            this.address3 = address3;
            return this;
        }
        public Builder withAddress4(final String address4) {
            this.address4 = address4;
            return this;
        }
        public Builder withPostCode(String postCode) {
            this.postCode = postCode;
            return this;
        }

        public Defendant build() {
            return new Defendant(this);
        }
    }
}