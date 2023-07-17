package uk.gov.moj.cpp.results.event.helper;

import static java.util.Arrays.asList;
import static java.util.Arrays.sort;
import static java.util.Comparator.comparingInt;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FixedListComparator {

    private FixedListComparator() {
    }

    private static final List<String> fixedList = asList("Adjourned", "Bail with conditions",
            "Bail without conditions", "Bail", "Remand", "Warrant", "Order",
            "Final Sentence", "Bail Conditions Cancelled", "Warrant Withdrawn");
    private static final Map<String, Integer> indexMap = new HashMap<>();

    static {
        for (int i = 0; i < fixedList.size(); i++) {
            indexMap.put(fixedList.get(i).toLowerCase(), i);
        }
    }

    public static String sortBasedOnFixedList(String subject) {
        final String[] input = subject.split(",");
        sort(input, comparingInt(value -> indexMap.getOrDefault(value.toLowerCase(), Integer.MAX_VALUE)));
        return String.join(",", input);
    }
}
