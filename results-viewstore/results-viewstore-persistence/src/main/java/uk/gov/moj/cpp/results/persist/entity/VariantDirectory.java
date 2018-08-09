package uk.gov.moj.cpp.results.persist.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import java.util.Objects;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

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

    @Column(name = "person_id")
    private UUID personId;

    @Column(name = "defendant_id")
    private UUID defendantId;

    @Column(name = "nows_type_id")
    private UUID nowsTypeId;

    @ElementCollection
    @CollectionTable(
            name = "variant_directory_usergroup",
            joinColumns = @JoinColumn(name = "variant_directory_id")
    )
    @Column(name = "user_group", nullable = false)
    private List<String> userGroup = new ArrayList<>();

    @Column(name = "material_id")
    private UUID materialId;

    @Column(name = "description")
    private String description;

    @Column(name = "template_name")
    private String templateName;

    @Column(name = "status")
    private String status;

    public VariantDirectory() {
        // for JPA
    }


    public VariantDirectory(final UUID id, final UUID hearingId, final UUID personId, final UUID defendantId, final UUID nowsTypeId,
                            final List<String> userGroup, final UUID materialId, final String description, final String templateName, final String status) {
        this.id = id;
        this.hearingId = hearingId;
        this.personId = personId;
        this.defendantId = defendantId;
        this.nowsTypeId = nowsTypeId;
        this.userGroup = userGroup;
        this.materialId = materialId;
        this.description = description;
        this.templateName = templateName;
        this.status = status;
    }

    public UUID getHearingId() {
        return hearingId;
    }

    public void setHearingId(final UUID hearingId) {
        this.hearingId = hearingId;
    }

    public UUID getPersonId() {
        return personId;
    }

    public void setPersonId(final UUID personId) {
        this.personId = personId;
    }

    public UUID getDefendantId() {
        return defendantId;
    }

    public void setDefendantId(final UUID defendantId) {
        this.defendantId = defendantId;
    }

    public UUID getNowsTypeId() {
        return nowsTypeId;
    }

    public void setNowsTypeId(final UUID nowsTypeId) {
        this.nowsTypeId = nowsTypeId;
    }

    public List<String> getUserGroup() {
        return userGroup;
    }

    public void setUserGroup(final List<String> userGroup) {
        this.userGroup = userGroup;
    }

    public UUID getMaterialId() {
        return materialId;
    }

    public void setMaterialId(final UUID materialId) {
        this.materialId = materialId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(final String templateName) {
        this.templateName = templateName;
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
