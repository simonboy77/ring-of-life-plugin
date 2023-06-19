package com.simonboy77;

import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Skill;

public class PlayerState {
    private static final int RESULT_SURVIVE = 0;
    private static final int RESULT_ESCAPE = 1;
    private static final int RESULT_DEATH = 2;
    private static final int USED_PHOENIX = 3;
    private static final int RESULT_AMOUNT = 4;
    private int[] resultOccurrences;
    private double[] resultChances;

    private final Client client;

    public boolean wearingRingOfLife;
    public boolean wearingDefenceCape;
    public boolean wearingPhoenixNecklace;

    public boolean inCombat;

    public Actor curOpponent;

    public PlayerState(Client client)
    {
        this.client = client;
    }

    private void log(String text)
    {
        this.client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", text, null);
    }

    public void setOpponent(Actor opponent)
    {
        if(opponent != curOpponent)
        {
            this.curOpponent = opponent;
            this.inCombat = (this.curOpponent != null);

            if(this.inCombat) {
                client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "fighting: " + this.curOpponent.getName(), null);
            }
            else {
                client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "out of combat", null);
            }
        }
    }

    private void hit_func(int hitNum, int hitAmount, int curDamage, int damageRange,
                          boolean phoenix, int escapeThreshold, int deathThreshold)
    {
        /* if phoenix gets triggered do:
            nextDamage -= phoenixHealAmount
            and pass false for the phoenix boolean, DONT CHANGE IT
        */

        if(hitNum < hitAmount)
        {
            for(int damage = 0; damage < damageRange; ++damage)
            {
                int nextDamage = curDamage + damage;

                if(nextDamage >= deathThreshold) {
                    this.resultOccurrences[RESULT_DEATH]++;
                }
                else if(nextDamage >= escapeThreshold) {
                    this.resultOccurrences[RESULT_ESCAPE]++;
                }
                else {
                    hit_func(hitNum + 1, hitAmount, nextDamage, damageRange, phoenix,
                            escapeThreshold, deathThreshold);
                }
            }
        }
        else if(hitAmount > 0)
        {
            if(curDamage >= deathThreshold) {
                this.resultOccurrences[RESULT_DEATH]++;
            }
            else if(curDamage >= escapeThreshold) {
                this.resultOccurrences[RESULT_ESCAPE]++;
            }
            else {
                this.resultOccurrences[RESULT_SURVIVE]++;
            }
        }
    }

    private double calcHitChance()
    {
        double chanceToHit = 0.0;

        int playerDefence = client.getBoostedSkillLevel(Skill.DEFENCE);
        int playerMagic = client.getBoostedSkillLevel(Skill.MAGIC);

        

        return chanceToHit;
    }

    public void calcSurvivalChance(int hitAmount, int maxHit)
    {
        if(this.inCombat)
        {
            double chanceToHit = calcHitChance();

            int maxHp = client.getRealSkillLevel(Skill.HITPOINTS);
            int curHp = client.getBoostedSkillLevel(Skill.HITPOINTS);

            // TODO: if wearing ring of life/defence cape
            int escapeThreshold = curHp - (int)Math.floor(maxHp / 10.0);
            this.resultOccurrences = new int[RESULT_AMOUNT];
            this.resultChances = new double[RESULT_AMOUNT];

            hit_func(0, hitAmount, 0, maxHit, false, escapeThreshold, curHp);

            // Parse results
            double totalHits = 0.0;

            for(int resultId = 0; resultId < RESULT_AMOUNT; ++resultId) {
                totalHits += this.resultOccurrences[resultId];
            }

            for(int resultId = 0; resultId < RESULT_AMOUNT; ++resultId) {
                double chance = ((double)this.resultOccurrences[resultId] / totalHits) * 100.0;
                this.resultChances[resultId] = chance;
            }

            log("safety: " + this.resultChances[RESULT_SURVIVE] +
                    ", escape: " + this.resultChances[RESULT_ESCAPE] +
                    ", die: " + this.resultChances[RESULT_DEATH]);
        }
    }
}
