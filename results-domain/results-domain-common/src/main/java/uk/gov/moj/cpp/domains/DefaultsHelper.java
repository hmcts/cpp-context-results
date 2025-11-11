package uk.gov.moj.cpp.domains;

import javax.json.JsonObject;

public class DefaultsHelper {

    private DefaultsHelper() {
    }

    public static boolean getBoolean(final JsonObject jsonObject, String key) {
        return jsonObject.containsKey(key) ? jsonObject.getBoolean(key) : false;
    }

}
