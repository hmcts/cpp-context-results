package uk.gov.moj.cpp.results.event.helper.sjp;

import static com.google.common.collect.ImmutableList.of;
import static java.math.BigDecimal.ROUND_DOWN;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import uk.gov.justice.sjp.results.Prompts;
import uk.gov.moj.cpp.results.event.helper.resultdefinition.Prompt;
import uk.gov.moj.cpp.results.event.helper.resultdefinition.ResultDefinition;

import java.math.BigDecimal;
import java.util.List;

public class ResultAmountSterling {

    private static final List<String> PROMPT_REFERENCE = of("AOBD", "AOCOM", "AOF", "AOS","AOC");
    private static final int MAXIMUM_DECIMAL_PLACES = 2;
    private Prompt promptDefinition;
    private Prompts prompt;
    private ResultDefinition resultDefinition;
    private Boolean isPresent;

    public ResultAmountSterling(final ResultDefinition resultDefinition, final Prompt promptDefinition, final Prompts prompt) {
        this.resultDefinition = resultDefinition;
        this.promptDefinition = promptDefinition;
        this.prompt = prompt;
        this.setIsPresent();
    }

    public boolean isPresent() {
        return this.isPresent;
    }

    public void setIsPresent() {
        this.isPresent = resultDefinition.isFinancial() && promptDefinition.getId().equals(prompt.getId()) && PROMPT_REFERENCE.contains(promptDefinition.getReference());
    }

    public String getAmount(){

        final String amountValue = this.prompt.getValue();
        if(isPresent() && isNotBlank(amountValue)) {
            final BigDecimal amount = new BigDecimal(amountValue).setScale(MAXIMUM_DECIMAL_PLACES, ROUND_DOWN);
            if(amount.doubleValue() > 0) {
                return "+" + amount.toString();
            } else {
                return amount.toString();
            }
        }
        return amountValue;
    }
}
