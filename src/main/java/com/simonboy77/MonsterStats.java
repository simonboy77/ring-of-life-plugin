package com.simonboy77;

import org.json.JSONObject;
import lombok.extern.slf4j.Slf4j; // Logging

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.util.stream.Collectors;
import java.util.List;
import java.util.ArrayList;

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

    private int getMonsterInt(int id, String key)
    {
        int result = -1;

        if(this.containsId(id))
        {
            String idString = String.valueOf(id);
            result = this.jsonObject.getJSONObject(idString).getInt(key);
        }

        return result;
    }

    public int getMaxHit(int id) { return this.getMonsterInt(id, "max_hit"); }

    // TODO: Fix this json mess. magicAttack should be magicStat, same fro ranged
    public int getAttackLevel(int id) { return this.getMonsterInt(id, "attack_level"); }
    public int getAttackStat(int id) { return this.getMonsterInt(id, "attack_bonus"); }
    public int getAttackSpeed(int id) { return this.getMonsterInt(id, "attack_speed"); }

    public int getMagicLevel(int id) { return this.getMonsterInt(id, "magic_level"); }
    public int getMagicAttack(int id) { return this.getMonsterInt(id, "magic_attack"); }

    public int getRangedLevel(int id) { return this.getMonsterInt(id, "ranged_level"); }
    public int getRangedAttack(int id) { return this.getMonsterInt(id, "ranged_attack"); }

    public ArrayList<String> getAttackStyles(int id)
    {
        // Can be: melee, ranged, magic, crush, stab, typeless, slash
        // melee and typeless are saying something about the attack style after it
        // just melee or typeless on its own is unusable and we assume 100% hit chance
        ArrayList<String> attackStyles = new ArrayList<String>();

        if(this.containsId(id)) {
            String key = String.valueOf(id);
            for(Object object : this.jsonObject.getJSONObject(key).getJSONArray("attack_type").toList()) {
                attackStyles.add(object.toString());
            }
        }

        return attackStyles;
    }
}
