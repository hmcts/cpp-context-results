package uk.gov.moj.cpp.results.event.helper.resultdefinition;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ResultDefinition {

    public static final String YES = "Y";

    private UUID id;

    private String label;

    private String shortCode;

    private String level;

    private Integer rank;

    private List<WordGroups> wordGroups;

    private List<String> userGroups = new ArrayList<>();

    private String version;

    private List<Prompt> prompts = new ArrayList<>();

    private Date startDate;

    private Date endDate;

    private String welshLabel;

    private Boolean isAvailableForCourtExtract;

    private String financial;

    private String category;

    private String cjsCode;

    private String adjournment;

    private String convicted;

    private Boolean terminatesOffenceProceedings;

    private Boolean lifeDuration;

    private Boolean publishedAsAPrompt;

    private Boolean excludedFromResults;

    private Boolean alwaysPublished;

    private Boolean urgent;

    private Boolean d20;

    public static ResultDefinition resultDefinition() {
        return new ResultDefinition();
    }

    public UUID getId() {
        return this.id;
    }

    public ResultDefinition setId(UUID id) {
        this.id = id;
        return this;
    }

    public String getLabel() {
        return this.label;
    }

    public ResultDefinition setLabel(String label) {
        this.label = label;
        return this;
    }

    public String getShortCode() {
        return this.shortCode;
    }

    public ResultDefinition setShortCode(String shortCode) {
        this.shortCode = shortCode;
        return this;
    }

    public String getLevel() {
        return this.level;
    }

    public ResultDefinition setLevel(String level) {
        this.level = level;
        return this;
    }

    public Integer getRank() {
        return this.rank;
    }

    public ResultDefinition setRank(Integer rank) {
        this.rank = rank;
        return this;
    }

    public List<WordGroups> getWordGroups() {
        return this.wordGroups;
    }

    public ResultDefinition setWordGroups(List<WordGroups> wordGroups) {
        this.wordGroups = wordGroups;
        return this;
    }

    public List<String> getUserGroups() {
        return this.userGroups;
    }

    public ResultDefinition setUserGroups(List<String> userGroups) {
        this.userGroups = userGroups;
        return this;
    }

    public List<Prompt> getPrompts() {
        return this.prompts;
    }

    public ResultDefinition setPrompts(List<Prompt> prompts) {
        this.prompts = prompts;
        return this;
    }

    public Date getStartDate() {
        return this.startDate;
    }

    public ResultDefinition setStartDate(Date startDate) {
        this.startDate = startDate;
        return this;
    }

    public Date getEndDate() {
        return this.endDate;
    }

    public ResultDefinition setEndDate(Date endDate) {
        this.endDate = endDate;
        return this;
    }

    public String getVersion() {
        return version;
    }

    public ResultDefinition setVersion(String version) {
        this.version = version;
        return this;
    }

    public boolean isFinancial() {
        return financial != null && financial.equalsIgnoreCase(YES);
    }

    public ResultDefinition setFinancial(final String financial) {
        this.financial = financial;
        return this;
    }

    public Boolean getIsAvailableForCourtExtract() {
        return isAvailableForCourtExtract;
    }

    public ResultDefinition setIsAvailableForCourtExtract(final Boolean isAvailableForCourtExtract) {
        this.isAvailableForCourtExtract = isAvailableForCourtExtract;
        return this;
    }

    public String getWelshLabel() {
        return welshLabel;
    }

    public ResultDefinition setWelshLabel(String welshLabel) {
        this.welshLabel = welshLabel;
        return this;
    }

    public ResultDefinition setCategory(final String category) {
        this.category = category;
        return this;
    }

    public String getCategory() {
        return this.category;
    }

    public ResultDefinition setCjsCode(final String cjsCode) {
        this.cjsCode = cjsCode;
        return this;
    }

    public String getCjsCode() {
        return this.cjsCode;
    }

    public boolean isAdjournment() {
        return adjournment != null && adjournment.equalsIgnoreCase(YES);
    }

    public ResultDefinition setAdjournment(final String adjournment) {
        this.adjournment = adjournment;
        return this;
    }

    public boolean isConvicted() {
        return convicted != null && convicted.equalsIgnoreCase(YES);
    }

    public ResultDefinition setConvicted(final String convicted) {
        this.convicted = convicted;
        return this;
    }

    public Boolean getTerminatesOffenceProceedings() {
        return terminatesOffenceProceedings;
    }

    public ResultDefinition setTerminatesOffenceProceedings(final Boolean terminatesOffenceProceedings) {
        this.terminatesOffenceProceedings = terminatesOffenceProceedings;
        return this;
    }

    public Boolean getLifeDuration() {
        return lifeDuration;
    }

    public ResultDefinition setLifeDuration(final Boolean lifeDuration) {
        this.lifeDuration = lifeDuration;
        return this;
    }

    public Boolean getPublishedAsAPrompt() {
        return publishedAsAPrompt;
    }

    public ResultDefinition setPublishedAsAPrompt(final Boolean publishedAsAPrompt) {
        this.publishedAsAPrompt = publishedAsAPrompt;
        return this;
    }

    public Boolean getExcludedFromResults() {
        return excludedFromResults;
    }

    public ResultDefinition setExcludedFromResults(final Boolean excludedFromResults) {
        this.excludedFromResults = excludedFromResults;
        return this;
    }

    public Boolean getAlwaysPublished() {
        return alwaysPublished;
    }

    public ResultDefinition setAlwaysPublished(final Boolean alwaysPublished) {
        this.alwaysPublished = alwaysPublished;
        return this;
    }

    public Boolean getUrgent() {
        return urgent;
    }

    public ResultDefinition setUrgent(final Boolean urgent) {
        this.urgent = urgent;
        return this;
    }

    public Boolean getD20() {
        return d20;
    }

    public ResultDefinition setD20(final Boolean d20) {
        this.d20 = d20;
        return this;
    }
}
