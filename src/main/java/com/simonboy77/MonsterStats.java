package com.simonboy77;

import org.json.JSONObject;
import lombok.extern.slf4j.Slf4j; // Logging

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

@Slf4j
public class MonsterStats {
    private JSONObject jsonObject;

    public MonsterStats()
    {
        InputStream is = ClassLoader.getSystemResourceAsStream("monster_essentials.json");
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String jsonString = reader.lines().collect(Collectors.joining(System.lineSeparator()));
        this.jsonObject = new JSONObject(jsonString);
    }

    public boolean containsId(int id)
    {
        return !(this.jsonObject.isNull(String.valueOf(id)));
    }

    public int getMaxHit(int id)
    {
        int maxHit = -1;

        if(this.containsId(id)) {
            String key = String.valueOf(id);
            maxHit = this.jsonObject.getJSONObject(key).getInt("max_hit");
        }

        return maxHit;
    }
}
