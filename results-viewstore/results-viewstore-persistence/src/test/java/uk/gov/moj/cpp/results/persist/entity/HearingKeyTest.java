package uk.gov.moj.cpp.results.persist.entity;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;

public class HearingKeyTest {

    @Test
    public void shouldHaveANoArgsConstructor() {
        assertThat(HearingKey.class, hasValidBeanConstructor());
    }
}
