package uk.gov.moj.cpp.results.test;

import java.util.function.Consumer;

public class TestUtilities {

    private TestUtilities() {}

    public static <T> T with(T object, Consumer<T> consumer) {
        consumer.accept(object);
        return object;
    }
}