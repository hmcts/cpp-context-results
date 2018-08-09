package uk.gov.moj.cpp.results.persist.entity;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;

public class HearingTest {

    @Test
    public void shouldHaveANoArgsConstructor() {

        assertThat(Hearing.class, hasValidBeanConstructor());
    }

    @Test
    public void noArgsConstructorShouldInstantiateAnObject() {
        assertThat(new Hearing(), not(nullValue()));
    }

}