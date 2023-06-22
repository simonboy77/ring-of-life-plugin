package com.simonboy77;

import net.runelite.api.Client;
import net.runelite.api.Actor;
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

    private int hitpoints;
    private int defense;
    private int magic;

    private final Client client;

    public boolean wearingRingOfLife;
    public boolean wearingDefenceCape;
    public boolean wearingPhoenixNecklace;

    public Actor[] opponents;
    private MonsterStats monsterStats;

    public PlayerState(Client client)
    {
        this.client = client;
        this.opponents = new Actor[0];
        this.monsterStats = new MonsterStats(client);

        this.hitpoints = client.getBoostedSkillLevel(Skill.HITPOINTS);
        this.defense = client.getBoostedSkillLevel(Skill.DEFENCE);
        this.magic = client.getBoostedSkillLevel(Skill.MAGIC);
    }

    private void log(String text)
    {
        this.client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", text, null);
    }

    public void addOpponent(Actor newOpponent)
    {
        boolean isNew = true;
        for(int opponentId = 0; (opponentId < this.opponents.length) && isNew; ++opponentId)
        {
            isNew &= (this.opponents[opponentId] != newOpponent);
        }

        if(isNew)
        {
            Actor newOpponents[] = new Actor[this.opponents.length + 1];

            for(int opponentId = 0; opponentId < this.opponents.length; ++opponentId)
            {
                newOpponents[opponentId] = this.opponents[opponentId];
            }

            newOpponents[this.opponents.length] = newOpponent;
            this.opponents = newOpponents;

            log("Added " + newOpponent.getName() + " to list of opponents");
            this.calcSurvivalChance(4, 3);
        }
        else
        {
            log(newOpponent.getName() + " was already on list of opponents");
        }
    }

    private void removeOpponent(Actor opponent)
    {
        boolean isPresent = false;
        for(int opponentId = 0; (opponentId < this.opponents.length) && !isPresent; ++opponentId)
        {
            isPresent |= (this.opponents[opponentId] == opponent);
        }

        if(isPresent)
        {
            Actor newOpponents[] = new Actor[this.opponents.length - 1];
            int newOpponentId = 0;

            for(int opponentId = 0; opponentId < this.opponents.length; ++opponentId)
            {
                if(this.opponents[opponentId] != opponent)
                {
                    newOpponents[newOpponentId++] = this.opponents[opponentId];
                }
            }

            this.opponents = newOpponents;
            log("Removed " + opponent.getName() + " from list of opponents");

            this.calcSurvivalChance(4, 3);
        }
        else
        {
            log(opponent.getName() + " was not in list of opponents");
        }
    }

    public void updateOpponents()
    {
        for(int opponentId = 0; opponentId < this.opponents.length;)
        {
            Actor op = this.opponents[opponentId];

            if(op.getInteracting() != client.getLocalPlayer() || op.isDead()) {
                removeOpponent(op);
            }
            else {
                opponentId++;
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
        if(this.opponents.length > 0)
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

    public void statChanged(Skill skill)
    {
        switch(skill)
        {
            case HITPOINTS:
            {
                if(this.hitpoints != client.getBoostedSkillLevel(Skill.HITPOINTS))
                {
                    this.hitpoints = client.getBoostedSkillLevel(Skill.HITPOINTS);
                    log("hitpoints changed to " + this.hitpoints);
                    this.calcSurvivalChance(4, 3);
                }
            } break;

            case DEFENCE:
            {
                if(this.defense != client.getBoostedSkillLevel(Skill.DEFENCE))
                {
                    this.defense = client.getBoostedSkillLevel(Skill.DEFENCE);
                    log("defense changed to " + this.defense);
                    this.calcSurvivalChance(4, 3);
                }
            } break;

            case MAGIC:
            {
                if(this.magic != client.getBoostedSkillLevel(Skill.MAGIC))
                {
                    this.magic = client.getBoostedSkillLevel(Skill.MAGIC);
                    log("magic changed to " + this.magic);
                    this.calcSurvivalChance(4, 3);
                }
            } break;

            default: break;
        }
    }
}
