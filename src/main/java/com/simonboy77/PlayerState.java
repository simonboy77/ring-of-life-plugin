package com.simonboy77;

import lombok.extern.slf4j.Slf4j; // Logging

import net.runelite.api.*;

import net.runelite.client.game.ItemManager;
import net.runelite.http.api.item.ItemStats;
import net.runelite.http.api.item.ItemEquipmentStats;

import static net.runelite.api.ItemID.*;
import static net.runelite.api.EquipmentInventorySlot.*;

@Slf4j
public class PlayerState {
    private final Client client;
    private final SurvivalChanceConfig config;
    private final ItemManager itemManager;
    private final MonsterStats monsterStats;

    private NPC[] opponents;
    private int[] resultOccurrences;
    private double[] resultChances;

    private int hitpoints;
    private int defense;
    private int magic;
    // private int prayer;

    private int stabDefence;
    private int slashDefence;
    private int crushDefence;
    private int magicDefence;
    private int rangeDefence;

    private boolean wearingEscapeItem;
    private boolean wearingPhoenix;

    private int iterationsTest;

    public PlayerState(Client client, SurvivalChanceConfig config, ItemManager itemManager)
    {
        this.client = client;
        this.config = config;
        this.itemManager = itemManager;

        this.opponents = new NPC[0];
        this.monsterStats = new MonsterStats();

        this.hitpoints = this.client.getBoostedSkillLevel(Skill.HITPOINTS);
        this.defense = this.client.getBoostedSkillLevel(Skill.DEFENCE);
        this.magic = this.client.getBoostedSkillLevel(Skill.MAGIC);
    }

    public boolean isWearingEscapeItem()
    {
        return this.wearingEscapeItem;
    }

    public boolean isWearingPhoenix()
    {
        return this.wearingPhoenix;
    }

    public boolean isInCombat()
    {
        return (this.opponents.length > 0);
    }

    public double getResultChance(int result)
    {
        if(this.isInCombat() && result >= HitResult.RESULT_SURVIVE && result < HitResult.RESULT_AMOUNT) {
            return this.resultChances[result];
        }
        else {
            return 0.0;
        }
    }

    public void addOpponent(NPC newOpponent)
    {
        boolean isValid = this.monsterStats.containsId(newOpponent.getId());
        for(int opponentId = 0; (opponentId < this.opponents.length) && isValid; ++opponentId) {
            isValid &= (this.opponents[opponentId] != newOpponent);
        }

        if(isValid)
        {
            NPC newOpponents[] = new NPC[this.opponents.length + 1];

            for(int opponentId = 0; opponentId < this.opponents.length; ++opponentId)
            {
                newOpponents[opponentId] = this.opponents[opponentId];
            }

            newOpponents[this.opponents.length] = newOpponent;
            this.opponents = newOpponents;

            log.info("Added " + newOpponent.getName() + " to list of opponents");
            log.info("maxHit: " + this.monsterStats.getMaxHit(newOpponent.getId()));
            this.calcSurvivalChance();
        }
        else
        {
            if(!this.monsterStats.containsId(newOpponent.getId())) {
                log.info(newOpponent.getName() + " is not a monster");
            }
            else {
                log.info(newOpponent.getName() + " is already on list of opponents");
            }
        }
    }

    private void removeOpponent(NPC opponent)
    {
        boolean isPresent = false;
        for(int opponentId = 0; (opponentId < this.opponents.length) && !isPresent; ++opponentId)
        {
            isPresent |= (this.opponents[opponentId] == opponent);
        }

        if(isPresent)
        {
            NPC newOpponents[] = new NPC[this.opponents.length - 1];
            int newOpponentId = 0;

            for(int opponentId = 0; opponentId < this.opponents.length; ++opponentId)
            {
                if(this.opponents[opponentId] != opponent)
                {
                    newOpponents[newOpponentId++] = this.opponents[opponentId];
                }
            }

            this.opponents = newOpponents;
            log.info("Removed " + opponent.getName() + " from list of opponents");

            this.calcSurvivalChance();
        }
        else
        {
            log.info(opponent.getName() + " was not in list of opponents");
        }
    }

    public void updateOpponents()
    {
        for(int opponentId = 0; opponentId < this.opponents.length;)
        {
            NPC op = this.opponents[opponentId];

            if(op.getInteracting() != this.client.getLocalPlayer() || op.isDead()) {
                removeOpponent(op);
            }
            else {
                opponentId++;
            }
        }
    }

    public void updateEquipment(ItemContainer equipment)
    {
        this.wearingEscapeItem = equipment.contains(RING_OF_LIFE) || equipment.contains(DEFENCE_CAPE);
        this.wearingPhoenix = equipment.contains(PHOENIX_NECKLACE);

        this.stabDefence = 0; this.slashDefence = 0; this.crushDefence = 0;
        this.magicDefence = 0; this.rangeDefence = 0;

        for(EquipmentInventorySlot slot : EquipmentInventorySlot.values())
        {
            Item slotItem = equipment.getItem(slot.getSlotIdx());
            if(slotItem != null)
            {
                ItemStats itemStats = this.itemManager.getItemStats(slotItem.getId(), false);
                if(itemStats != null && itemStats.isEquipable())
                {
                    ItemEquipmentStats equipmentStats = itemStats.getEquipment();
                    if(equipmentStats != null)
                    {
                        this.stabDefence += equipmentStats.getDstab();
                        this.slashDefence += equipmentStats.getDslash();
                        this.crushDefence += equipmentStats.getDcrush();
                        this.magicDefence += equipmentStats.getDmagic();
                        this.rangeDefence += equipmentStats.getDrange();
                    }
                }
            }
        }
    }

    private void hit_func_slow(int hitNum, int hitAmount, int curDamage, int maxDamage,
                          boolean phoenix, int escapeDamage, int deathDamage)
    {
        ++this.iterationsTest;

        if(hitNum < hitAmount)
        {
            for(int damage = 0; damage <= maxDamage; ++damage)
            {
                int nextDamage = curDamage + damage;

                if(nextDamage >= deathDamage) {
                    this.resultOccurrences[HitResult.RESULT_DEATH]++;
                }
                else if(nextDamage >= escapeDamage) {
                    this.resultOccurrences[HitResult.RESULT_ESCAPE]++;
                }
                else {
                    hit_func_slow(hitNum + 1, hitAmount, nextDamage, maxDamage, phoenix, escapeDamage, deathDamage);
                }
            }
        }
        else if(hitAmount > 0)
        {
            if(curDamage >= deathDamage) {
                this.resultOccurrences[HitResult.RESULT_DEATH]++;
            }
            else if(curDamage >= escapeDamage) {
                this.resultOccurrences[HitResult.RESULT_ESCAPE]++;
            }
            else {
                this.resultOccurrences[HitResult.RESULT_SURVIVE]++;
            }
        }
    }

    private void hit_func(int hitNum, int hitAmount, int curDamage, int maxDamage,
                          int triggerDamage, int deathDamage, boolean escapeItem, boolean phoenix)
    {
        // TODO: Should I add minDamage? Now its assumed to always be zero

        /* if phoenix gets triggered do:
            nextDamage -= phoenixHealAmount
            and pass false for the phoenix boolean, DONT CHANGE IT
        */

        ++this.iterationsTest;

        if(hitNum < hitAmount)
        {
            /*
            Do the damage for loop from high to low

            When the maxTotalDamage can trigger an escape or kill, keep going with the recursion
            otherwise, calculate how many hits would follow the current one and add those to results[SURVIVAL]


            */

            for(int damage = maxDamage; damage >= 0; --damage)
            {
                boolean triggerItem = (escapeItem || phoenix);
                int nextDamage = curDamage + damage;

                if(nextDamage >= deathDamage)
                {
                    this.resultOccurrences[HitResult.RESULT_DEATH]++;
                }
                else if(triggerItem && nextDamage >= triggerDamage)
                {
                    // TODO: if phoenix
                    this.resultOccurrences[HitResult.RESULT_ESCAPE]++;
                }
                else // Check if all of the remaining hits are safe
                {
                    int remainingHits = hitAmount - (hitNum + 1);
                    int maxTotalDamage = nextDamage + (maxDamage * remainingHits);
                    //log.info("max total damage from " + nextDamage + ": " + maxTotalDamage);

                    if((escapeItem || phoenix) && (maxTotalDamage >= triggerDamage))
                    {
                        // TODO: if phoenix
                        hit_func(hitNum + 1, hitAmount, nextDamage, maxDamage, triggerDamage, deathDamage, escapeItem, phoenix);
                    }
                    else if(maxTotalDamage >= deathDamage)
                    {
                        hit_func(hitNum + 1, hitAmount, nextDamage, maxDamage, triggerDamage, deathDamage, escapeItem, phoenix);
                    }
                    else // All following hits are safe
                    {
                        int remainingHitOccurrences = (damage + 1) * (int)Math.pow((double)(maxDamage + 1), (double)remainingHits);
                        this.resultOccurrences[HitResult.RESULT_SURVIVE] += remainingHitOccurrences;
                        break;
                    }
                }
            }
        }
        else if(hitAmount > 0)
        {
            if(curDamage >= deathDamage) {
                this.resultOccurrences[HitResult.RESULT_DEATH]++;
            }
            else if(curDamage >= triggerDamage) {
                this.resultOccurrences[HitResult.RESULT_ESCAPE]++;
            }
            else {
                this.resultOccurrences[HitResult.RESULT_SURVIVE]++;
            }
        }
    }

    private double calcHitChance()
    {
        double chanceToHit = 1.0;

        //int playerDefence = this.client.getBoostedSkillLevel(Skill.DEFENCE);
        //int playerMagic = this.client.getBoostedSkillLevel(Skill.MAGIC);

        

        return chanceToHit;
    }

    public void calcSurvivalChance()
    {
        if(this.isInCombat())
        {
            double chanceToHit = calcHitChance();

            int hitAmount = this.config.hitTurns();
            int maxHit = this.monsterStats.getMaxHit(this.opponents[0].getId());
            int maxHp = this.client.getRealSkillLevel(Skill.HITPOINTS);

            // TODO: if wearing ring of life/defence cape
            int triggerDamage = this.hitpoints - (int)Math.floor(maxHp / 10.0);
            this.resultOccurrences = new int[HitResult.RESULT_AMOUNT];
            this.resultChances = new double[HitResult.RESULT_AMOUNT];

            this.iterationsTest = 0;

            hit_func(0, hitAmount, 0, maxHit, triggerDamage, this.hitpoints, true, false);
            //hit_func_slow(0, hitAmount, 0, maxHit, false, triggerDamage, this.hitpoints);

            // Parse results
            double totalHits = 0.0;

            for(int resultId = 0; resultId < HitResult.RESULT_AMOUNT; ++resultId) {
                totalHits += this.resultOccurrences[resultId];
            }

            for(int resultId = 0; resultId < HitResult.RESULT_AMOUNT; ++resultId) {
                double chance = ((double)this.resultOccurrences[resultId] / totalHits) * 100.0;
                this.resultChances[resultId] = chance;
            }

            log.info("safety: " + this.resultChances[HitResult.RESULT_SURVIVE] +
                    ", escape: " + this.resultChances[HitResult.RESULT_ESCAPE] +
                    ", die: " + this.resultChances[HitResult.RESULT_DEATH] +
                    ", totalHits: " + totalHits +
                    ", iterations: " + this.iterationsTest);
        }
    }

    public void statChanged(Skill skill)
    {
        switch(skill)
        {
            case HITPOINTS:
            {
                if(this.hitpoints != this.client.getBoostedSkillLevel(Skill.HITPOINTS))
                {
                    this.hitpoints = this.client.getBoostedSkillLevel(Skill.HITPOINTS);
                    log.info("hitpoints changed to " + this.hitpoints);
                    this.calcSurvivalChance();
                }
            } break;

            case DEFENCE:
            {
                if(this.defense != this.client.getBoostedSkillLevel(Skill.DEFENCE))
                {
                    this.defense = this.client.getBoostedSkillLevel(Skill.DEFENCE);
                    log.info("defense changed to " + this.defense);
                    this.calcSurvivalChance();
                }
            } break;

            case MAGIC:
            {
                if(this.magic != this.client.getBoostedSkillLevel(Skill.MAGIC))
                {
                    this.magic = this.client.getBoostedSkillLevel(Skill.MAGIC);
                    log.info("magic changed to " + this.magic);
                    this.calcSurvivalChance();
                }
            } break;

            default: break;
        }
    }
}
