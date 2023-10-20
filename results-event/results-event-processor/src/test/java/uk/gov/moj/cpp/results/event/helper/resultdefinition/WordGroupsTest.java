package uk.gov.moj.cpp.results.event.helper.resultdefinition;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class WordGroupsTest {

    @Test
    public void shouldReturnWorldGroups() {
        final List<String> words = Arrays.asList("word1", "word2");
        final WordGroups wordGroups = WordGroups.wordGroups();
        wordGroups.setWordGroup(words);
        assertEquals(2,wordGroups.getWordGroup().size() );
        assertEquals("word1",wordGroups.getWordGroup().get(0) );
        assertEquals("word2",wordGroups.getWordGroup().get(1) );
    }


}