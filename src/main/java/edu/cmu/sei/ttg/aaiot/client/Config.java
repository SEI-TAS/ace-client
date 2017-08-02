package edu.cmu.sei.ttg.aaiot.client;

import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by sebastianecheverria on 8/2/17.
 */
public class Config
{
    public static Map<String, String> data = new HashMap<>();

    public static void load(String configurationFile) throws IOException
    {
        FileInputStream fs = new FileInputStream(configurationFile);
        JSONTokener parser = new JSONTokener(fs);
        JSONObject config = new JSONObject(parser);

        Iterator<String> configData = config.keys();
        while(configData.hasNext())
        {
            String paramName = configData.next();
            String paramValue = config.getString(paramName);
            data.put(paramName, paramValue);
        }
    }
}
