package uk.gov.moj.cpp.domains.results.structure;

import java.io.Serializable;

public class ContactNumber implements Serializable {

    private long serialVersionUID = -9176890205806560222L;
    private String home;

    private String mobile;

    private String work;

    private String primaryEmail;

    private String secondaryEmail;

    public String getHome() {
        return home;
    }

    public String getMobile() {
        return mobile;
    }

    public String getWork() {
        return work;
    }

    public String getPrimaryEmail() {
        return primaryEmail;
    }

    public String getSecondaryEmail() {
        return secondaryEmail;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final ContactNumber that = (ContactNumber) o;

        if (home != null ? !home.equals(that.home) : that.home != null) {
            return false;
        }
        if (mobile != null ? !mobile.equals(that.mobile) : that.mobile != null) {
            return false;
        }
        if (work != null ? !work.equals(that.work) : that.work != null) {
            return false;
        }
        if (primaryEmail != null ? !primaryEmail.equals(that.primaryEmail) : that.primaryEmail != null) {
            return false;
        }
        return secondaryEmail != null ? secondaryEmail.equals(that.secondaryEmail) : that.secondaryEmail == null;
    }

    @Override
    public int hashCode() {
        int result = home != null ? home.hashCode() : 0;
        result = 31 * result + (mobile != null ? mobile.hashCode() : 0);
        result = 31 * result + (work != null ? work.hashCode() : 0);
        result = 31 * result + (primaryEmail != null ? primaryEmail.hashCode() : 0);
        result = 31 * result + (secondaryEmail != null ? secondaryEmail.hashCode() : 0);
        return result;
    }
}
