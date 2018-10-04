package uk.gov.moj.cpp.results.persist.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import java.util.Objects;

import java.util.UUID;

@SuppressWarnings("squid:S00107")
@Entity
@Table(name = "variant_directory")
public class VariantDirectory {

    @Id
    @Column(name = "id", unique = true, nullable = false)
    private UUID id;

    @Column(name = "hearing_id")
    private UUID hearingId;

    @Column(name = "material_id")
    private UUID materialId;

    @Column(name = "status")
    private String status;

    public VariantDirectory() {
        // for JPA
    }


    public VariantDirectory(final UUID id, final UUID hearingId, final UUID materialId, final String status) {
        this.id = id;
        this.hearingId = hearingId;
        this.materialId = materialId;
        this.status = status;
    }

    public UUID getHearingId() {
        return hearingId;
    }

    public void setHearingId(final UUID hearingId) {
        this.hearingId = hearingId;
    }

    public UUID getMaterialId() {
        return materialId;
    }

    public void setMaterialId(final UUID materialId) {
        this.materialId = materialId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(materialId);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final VariantDirectory other = (VariantDirectory) obj;
        return Objects.equals(materialId, other.materialId);
    }
}
