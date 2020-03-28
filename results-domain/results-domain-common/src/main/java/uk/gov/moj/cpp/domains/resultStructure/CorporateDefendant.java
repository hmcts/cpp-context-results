package uk.gov.moj.cpp.domains.resultStructure;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.ContactNumber;

import java.io.Serializable;

public class CorporateDefendant implements Serializable {

    private long serialVersionUID = -9176890205806560222L;
    private String incorporationNumber;

    private String name;

    private ContactNumber contact;

    private Address address;

    public String getIncorporationNumber() {
        return incorporationNumber;
    }

    public String getName() {
        return name;
    }

    public ContactNumber getContact() {
        return contact;
    }

    public Address getAddress() {
        return address;
    }

    public CorporateDefendant(String incorporationNumber, String name, ContactNumber contact, Address address) {
        this.incorporationNumber = incorporationNumber;
        this.name = name;
        this.contact = contact;
        this.address = address;
    }

    public static CorporateDefendant.Builder corporateDefendant() {
        return new CorporateDefendant.Builder();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        CorporateDefendant that = (CorporateDefendant) o;

        if (incorporationNumber != null ? !incorporationNumber.equals(that.incorporationNumber) : that.incorporationNumber != null) {
            return false;
        }
        if (name != null ? !name.equals(that.name) : that.name != null) {
            return false;
        }
        if (contact != null ? !contact.equals(that.contact) : that.contact != null) {
            return false;
        }
        return address != null ? address.equals(that.address) : that.address == null;
    }

    @Override
    public int hashCode() {
        int result = incorporationNumber != null ? incorporationNumber.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (contact != null ? contact.hashCode() : 0);
        result = 31 * result + (address != null ? address.hashCode() : 0);
        return result;
    }

    public static class Builder {
        private uk.gov.justice.core.courts.Address address;

        private uk.gov.justice.core.courts.ContactNumber contact;

        private String incorporationNumber;

        private String name;

        public CorporateDefendant.Builder withAddress(final uk.gov.justice.core.courts.Address address) {
            this.address = address;
            return this;
        }

        public CorporateDefendant.Builder withContact(final uk.gov.justice.core.courts.ContactNumber contact) {
            this.contact = contact;
            return this;
        }

        public CorporateDefendant.Builder withIncorporationNumber(final String incorporationNumber) {
            this.incorporationNumber = incorporationNumber;
            return this;
        }

        public CorporateDefendant.Builder withName(final String name) {
            this.name = name;
            return this;
        }

        public CorporateDefendant build() {
            return new CorporateDefendant(incorporationNumber, name, contact, address);
        }
    }
}
