package uk.gov.moj.cpp.results.event.helper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static uk.gov.moj.cpp.results.event.helper.FixedListComparator.sortBasedOnFixedList;

import org.junit.jupiter.api.Test;

public class FixedListComparatorTest {

    @Test
    public void shouldSortTheSubjectBasedOnFixedList() {
        // given
        String policeSubjectLineSubject = "Warrant,Order,Remand,Bail without conditions,Bail with conditions";
        // when
        String sortedSubjectBasedOnFixedList = sortBasedOnFixedList(policeSubjectLineSubject);
        // then
        assertThat(sortedSubjectBasedOnFixedList, is("Bail with conditions,Bail without conditions,Remand,Warrant,Order"));

    }

    @Test
    public void shouldReturnEmptyStringWhenSubjectIsEmpty() {
        // given
        String policeSubjectLineSubject = "";
        // when
        String sortedSubjectBasedOnFixedList = sortBasedOnFixedList(policeSubjectLineSubject);
        // then
        System.out.println(sortedSubjectBasedOnFixedList);
        assertThat(sortedSubjectBasedOnFixedList, is(""));

    }
}
