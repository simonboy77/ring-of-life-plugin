package com.simonboy77;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import net.runelite.api.Client;
import net.runelite.api.ChatMessageType;

public class MonsterStats {
    JSONArray jsonArray;
    Client client;

    public MonsterStats(Client client)
    {
        this.client = client;

        InputStream is = ClassLoader.getSystemResourceAsStream("monster_essentials.json");
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String jsonString = reader.lines().collect(Collectors.joining(System.lineSeparator()));
        this.jsonArray = new JSONArray(jsonString);

        log("name: " + this.jsonArray.getJSONObject(0).getString("name"));
    }

    private void log(String text)
    {
        this.client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", text, null);
    }
}
