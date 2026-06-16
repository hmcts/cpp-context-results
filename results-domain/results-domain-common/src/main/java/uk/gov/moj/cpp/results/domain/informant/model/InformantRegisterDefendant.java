package uk.gov.moj.cpp.results.domain.informant.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class InformantRegisterDefendant {

    private final String name;
    private final String address1;
    private final String address2;
    private final String address3;
    private final String address4;
    private final String address5;
    private final String postCode;
    private final String dateOfBirth;
    private final String nationality;
    private final String title;
    private final String firstName;
    private final String lastName;
    private final List<InformantRegisterCaseOrApplication> prosecutionCasesOrApplications;
    private final List<InformantRegisterResult> results;

    @JsonCreator
    public InformantRegisterDefendant(
            @JsonProperty("name") final String name,
            @JsonProperty("address1") final String address1,
            @JsonProperty("address2") final String address2,
            @JsonProperty("address3") final String address3,
            @JsonProperty("address4") final String address4,
            @JsonProperty("address5") final String address5,
            @JsonProperty("postCode") final String postCode,
            @JsonProperty("dateOfBirth") final String dateOfBirth,
            @JsonProperty("nationality") final String nationality,
            @JsonProperty("title") final String title,
            @JsonProperty("firstName") final String firstName,
            @JsonProperty("lastName") final String lastName,
            @JsonProperty("prosecutionCasesOrApplications") final List<InformantRegisterCaseOrApplication> prosecutionCasesOrApplications,
            @JsonProperty("results") final List<InformantRegisterResult> results) {
        this.name = name;
        this.address1 = address1;
        this.address2 = address2;
        this.address3 = address3;
        this.address4 = address4;
        this.address5 = address5;
        this.postCode = postCode;
        this.dateOfBirth = dateOfBirth;
        this.nationality = nationality;
        this.title = title;
        this.firstName = firstName;
        this.lastName = lastName;
        this.prosecutionCasesOrApplications = prosecutionCasesOrApplications;
        this.results = results;
    }

    public String getName() { return name; }
    public String getAddress1() { return address1; }
    public String getAddress2() { return address2; }
    public String getAddress3() { return address3; }
    public String getAddress4() { return address4; }
    public String getAddress5() { return address5; }
    public String getPostCode() { return postCode; }
    public String getDateOfBirth() { return dateOfBirth; }
    public String getNationality() { return nationality; }
    public String getTitle() { return title; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public List<InformantRegisterCaseOrApplication> getProsecutionCasesOrApplications() { return prosecutionCasesOrApplications; }
    public List<InformantRegisterResult> getResults() { return results; }

    public static Builder informantRegisterDefendant() {
        return new Builder();
    }

    public static class Builder {
        private String name;
        private String address1;
        private String address2;
        private String address3;
        private String address4;
        private String address5;
        private String postCode;
        private String dateOfBirth;
        private String nationality;
        private String title;
        private String firstName;
        private String lastName;
        private List<InformantRegisterCaseOrApplication> prosecutionCasesOrApplications;
        private List<InformantRegisterResult> results;

        public Builder withName(final String name) { this.name = name; return this; }
        public Builder withAddress1(final String address1) { this.address1 = address1; return this; }
        public Builder withAddress2(final String address2) { this.address2 = address2; return this; }
        public Builder withAddress3(final String address3) { this.address3 = address3; return this; }
        public Builder withAddress4(final String address4) { this.address4 = address4; return this; }
        public Builder withAddress5(final String address5) { this.address5 = address5; return this; }
        public Builder withPostCode(final String postCode) { this.postCode = postCode; return this; }
        public Builder withDateOfBirth(final String dateOfBirth) { this.dateOfBirth = dateOfBirth; return this; }
        public Builder withNationality(final String nationality) { this.nationality = nationality; return this; }
        public Builder withTitle(final String title) { this.title = title; return this; }
        public Builder withFirstName(final String firstName) { this.firstName = firstName; return this; }
        public Builder withLastName(final String lastName) { this.lastName = lastName; return this; }
        public Builder withProsecutionCasesOrApplications(final List<InformantRegisterCaseOrApplication> prosecutionCasesOrApplications) {
            this.prosecutionCasesOrApplications = prosecutionCasesOrApplications; return this;
        }
        public Builder withResults(final List<InformantRegisterResult> results) { this.results = results; return this; }

        public InformantRegisterDefendant build() {
            return new InformantRegisterDefendant(name, address1, address2, address3, address4, address5,
                    postCode, dateOfBirth, nationality, title, firstName, lastName,
                    prosecutionCasesOrApplications, results);
        }
    }
}
