package uk.gov.moj.cpp.domains.results.structure;

import uk.gov.justice.core.courts.VehicleCode;

import java.io.Serializable;

public class OffenceFacts implements Serializable {
    private long serialVersionUID = -9176890205806560222L;
    private Integer alcoholReadingAmount;

    private String alcoholReadingMethod;

    private VehicleCode vehicleCode;

    private String vehicleRegistration;

    public OffenceFacts(Integer alcoholReadingAmount, String alcoholReadingMethod, VehicleCode vehicleCode, String vehicleRegistration) {
        this.alcoholReadingAmount = alcoholReadingAmount;
        this.alcoholReadingMethod = alcoholReadingMethod;
        this.vehicleCode = vehicleCode;
        this.vehicleRegistration = vehicleRegistration;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final OffenceFacts that = (OffenceFacts) o;

        if (alcoholReadingAmount != null ? !alcoholReadingAmount.equals(that.alcoholReadingAmount) : that.alcoholReadingAmount != null) {
            return false;
        }
        if (alcoholReadingMethod != null ? !alcoholReadingMethod.equals(that.alcoholReadingMethod) : that.alcoholReadingMethod != null) {
            return false;
        }
        if (vehicleCode != that.vehicleCode) {
            return false;
        }
        return vehicleRegistration != null ? vehicleRegistration.equals(that.vehicleRegistration) : that.vehicleRegistration == null;
    }

    @Override
    public int hashCode() {
        int result = alcoholReadingAmount != null ? alcoholReadingAmount.hashCode() : 0;
        result = 31 * result + (alcoholReadingMethod != null ? alcoholReadingMethod.hashCode() : 0);
        result = 31 * result + (vehicleCode != null ? vehicleCode.hashCode() : 0);
        result = 31 * result + (vehicleRegistration != null ? vehicleRegistration.hashCode() : 0);
        return result;
    }
}
