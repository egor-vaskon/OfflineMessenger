package com.golden_apps.offlinemessenger;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import org.junit.Test;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() {
        Gson gson = new Gson();

        String json =
            "{\"hello\": 12} 12nfkfkkf";

        JsonElement el = JsonParser.parseString(json);

        int i = 1;
    }

    @Test
    public void deleteAllFilesInAppStorage(){

    }
}