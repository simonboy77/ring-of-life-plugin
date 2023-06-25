package com.simonboy77;

import com.google.errorprone.annotations.Var;
import lombok.extern.slf4j.Slf4j; // Logging
import java.util.ArrayList;

import net.runelite.api.*;

import net.runelite.api.Varbits;
import net.runelite.client.game.ItemManager;
import net.runelite.http.api.item.ItemStats;
import net.runelite.http.api.item.ItemEquipmentStats;

import static net.runelite.api.ItemID.*;

@Slf4j
public class PlayerState {
    private final Client client;
    private final SurvivalChanceConfig config;
    private final ItemManager itemManager;
    private final MonsterStats monsterStats;

    private final double monsterStanceBonus = 9.0;

    private NPC[] opponents;
    private int[] resultOccurrences;
    private double[] resultChances;

    private int hitpoints;
    private double effectiveDefence;
    private double effectiveMagic;
    // private int prayer;

    private int stabDefenceStat;
    private int slashDefenceStat;
    private int crushDefenceStat;
    private int magicDefenceStat;
    private int rangedDefenceStat;

    private boolean wearingEscapeItem;
    private boolean wearingPhoenix;

    // TESTING
    private int iterationsTest;
    private int hitCounter;
    private int attackStat;

    public PlayerState(Client client, SurvivalChanceConfig config, ItemManager itemManager)
    {
        this.client = client;
        this.config = config;
        this.itemManager = itemManager;

        this.opponents = new NPC[0];
        this.monsterStats = new MonsterStats();

        this.hitpoints = this.client.getBoostedSkillLevel(Skill.HITPOINTS);
        this.calcEffectiveDefence();
        this.calcEffectiveMagic();
    }

    private void calcEffectiveDefence()
    {
        this.effectiveDefence = this.client.getBoostedSkillLevel(Skill.DEFENCE);

        // TODO: Prayer boost
        // TODO: Round down
        // TODO: Attack style bonus
        // TODO: Round down
        this.effectiveDefence += 3.0; // Block attack style TEMPTEMPTEMPTEMP
        this.effectiveDefence += 8.0;

        this.calcEffectiveMagic(); // effectiveDefence has an impact on effectiveMagic
    }

    private void calcEffectiveMagic()
    {
        this.effectiveMagic = this.client.getBoostedSkillLevel(Skill.MAGIC);

        // TODO: prayer boost
        // TODO: round down

        this.effectiveMagic = Math.floor(this.effectiveMagic * 0.7);
        this.effectiveMagic += Math.floor(this.effectiveDefence * 0.3);
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
            opponent.setOverheadText("");

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
        this.hitCounter = 0;
        log.info("hitCounter: " + this.hitCounter);

        this.wearingEscapeItem = equipment.contains(RING_OF_LIFE) || equipment.contains(DEFENCE_CAPE);
        this.wearingPhoenix = equipment.contains(PHOENIX_NECKLACE);

        this.stabDefenceStat = 0; this.slashDefenceStat = 0; this.crushDefenceStat = 0;
        this.magicDefenceStat = 0; this.rangedDefenceStat = 0;

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
                        this.stabDefenceStat += equipmentStats.getDstab();
                        this.slashDefenceStat += equipmentStats.getDslash();
                        this.crushDefenceStat += equipmentStats.getDcrush();
                        this.magicDefenceStat += equipmentStats.getDmagic();
                        this.rangedDefenceStat += equipmentStats.getDrange();
                    }
                }
            }
        }

        this.calcSurvivalChance();
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
        // TODO: add redemption boolean, works like phoenix

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

    private double calcHitChanceNonMagic(double mLvl, double mAttackStat, double pDefenceStat)
    {
        double mEffectiveLvl = mLvl + this.monsterStanceBonus + 8.0;
        double attackRoll = mEffectiveLvl * (mAttackStat + 64.0);

        double defenceRoll = this.effectiveDefence * (pDefenceStat + 64.0);

        if(attackRoll > defenceRoll) {
            return (1.0 - (defenceRoll + 2.0) / (2.0 * (attackRoll + 1.0)));
        }
        else {
            return (attackRoll / (2.0 * (defenceRoll + 1.0)));
        }
    }

    private double calcHitChanceMagic(NPC opponent)
    {
        int monsterId = opponent.getId();

        double mMagicLvl = this.monsterStats.getMagicLevel(monsterId);
        double mMagicAtkStat = this.monsterStats.getMagicAttack(monsterId);
        double pMagicDefenceStat = this.magicDefenceStat;

        //double mEffectiveLvl = mMagicLvl + this.monsterStanceBonus + 8.0;
        double mEffectiveLvl = mMagicLvl + this.monsterStanceBonus;
        double attackRoll = mEffectiveLvl * (mMagicAtkStat + 64.0);

        double pEffectiveLvl = this.effectiveMagic;
        double defenceRoll = pEffectiveLvl * (pMagicDefenceStat + 64.0);

        if(attackRoll > defenceRoll) {
            return (1.0 - (defenceRoll + 2.0) / (2.0 * (attackRoll + 1.0)));
        }
        else {
            return (attackRoll / (2.0 * (defenceRoll + 1.0)));
        }
    }

    private double calcHitChance(NPC opponent)
    {
        // TODO: We dont need to recalc everything on hp change
        double hitChance = 1.0;
        int monsterId = opponent.getId();

        ArrayList<String> attackStyles = this.monsterStats.getAttackStyles(monsterId);
        int attackStyleAmount = attackStyles.size();

        if(attackStyleAmount == 1) {
            log.info("1 attack style: " + attackStyles.get(0));

            String attackStyle = attackStyles.get(0);
            if(attackStyle.equals("stab")) {
                hitChance = this.calcHitChanceNonMagic(this.monsterStats.getAttackLevel(monsterId),
                        this.monsterStats.getAttackStat(monsterId), this.stabDefenceStat);
            }
            else if(attackStyle.equals("slash")) {
                hitChance = this.calcHitChanceNonMagic(this.monsterStats.getAttackLevel(monsterId),
                        this.monsterStats.getAttackStat(monsterId), this.slashDefenceStat);
            }
            else if(attackStyle.equals("crush")) {
                hitChance = this.calcHitChanceNonMagic(this.monsterStats.getAttackLevel(monsterId),
                        this.monsterStats.getAttackStat(monsterId), this.crushDefenceStat);
            }
            else if(attackStyle.equals("magic")) {
                hitChance = calcHitChanceMagic(opponent);
            }
            else if(attackStyle.equals("ranged")) {
                hitChance = this.calcHitChanceNonMagic(this.monsterStats.getRangedLevel(monsterId),
                        this.monsterStats.getRangedAttack(monsterId), this.rangedDefenceStat);
            }
            // everything else we just give a chance of 1.0
        }
        else if(attackStyles.size() > 1) {
            /*
            If more than 1 attack style is used (melee/magic/ranged), we assume
            a protection prayer will only block (1 / styleAmount) of the damage
            */
        }
        else
        {

        }

        opponent.setOverheadText(String.format("%.1f%%", hitChance * 100.0));
        return hitChance;
    }

    public void calcSurvivalChance()
    {
        if(this.isInCombat())
        {
            double hitChance = this.calcHitChance(this.opponents[0]);
            log.info("hitChance: " + hitChance);

            int hitAmount = this.config.hitTurns();
            int maxHit = this.monsterStats.getMaxHit(this.opponents[0].getId());
            int maxHp = this.client.getRealSkillLevel(Skill.HITPOINTS);

            // TODO: if wearing ring of life/defence cape
            int triggerDamage = this.hitpoints - (int)Math.floor((double)maxHp / 10.0);
            this.resultOccurrences = new int[HitResult.RESULT_AMOUNT];
            this.resultChances = new double[HitResult.RESULT_AMOUNT];

            this.iterationsTest = 0;

            //hit_func(0, hitAmount, 0, maxHit, triggerDamage, this.hitpoints, true, false);
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

            //log.info("safety: " + this.resultChances[HitResult.RESULT_SURVIVE] +
                    //", escape: " + this.resultChances[HitResult.RESULT_ESCAPE] +
                    //", die: " + this.resultChances[HitResult.RESULT_DEATH] +
                    //", totalHits: " + totalHits +
                    //", iterations: " + this.iterationsTest);
        }
    }

    public void statChanged(Skill skill)
    {
        switch(skill)
        {
            case HITPOINTS:
            {
                // Also get's called when player is hit for a zero
                int newHitpoints = this.client.getBoostedSkillLevel(Skill.HITPOINTS);
                if(this.hitpoints != newHitpoints)
                {
                    if(this.hitpoints > newHitpoints)
                    {
                        this.hitCounter++;
                        log.info("hitCounter: " + this.hitCounter);
                    }

                    this.hitpoints = newHitpoints;
                    this.calcSurvivalChance();
                }
            } break;

            case DEFENCE:
            {
                this.calcEffectiveDefence();
                this.calcSurvivalChance();
            } break;

            case MAGIC:
            {
                this.calcEffectiveMagic();
                this.calcSurvivalChance();
            } break;

            case ATTACK:
            {
                int newAttack = client.getBoostedSkillLevel(Skill.ATTACK);
                if(this.attackStat > newAttack)
                {
                    this.hitCounter++;
                    log.info("hitCounter: " + this.hitCounter);
                }

                this.attackStat = client.getBoostedSkillLevel(Skill.ATTACK);
            }

            default: break;
        }
    }

    public void varbitChanged(int varbitId)
    {
        if(varbitId == Varbits.PRAYER_THICK_SKIN ||
            varbitId == Varbits.PRAYER_ROCK_SKIN ||
            varbitId == Varbits.PRAYER_STEEL_SKIN ||

            varbitId == Varbits.PRAYER_MYSTIC_WILL ||
            varbitId == Varbits.PRAYER_MYSTIC_LORE ||
            varbitId == Varbits.PRAYER_MYSTIC_MIGHT ||

            varbitId == Varbits.PRAYER_PROTECT_FROM_MAGIC ||
            varbitId == Varbits.PRAYER_PROTECT_FROM_MISSILES ||
            varbitId == Varbits.PRAYER_PROTECT_FROM_MELEE ||

            varbitId == Varbits.PRAYER_REDEMPTION ||
            varbitId == Varbits.PRAYER_CHIVALRY ||
            varbitId == Varbits.PRAYER_PIETY ||
            varbitId == Varbits.PRAYER_AUGURY)
        {
            this.calcSurvivalChance();
        }

        //Varbits.ATT
    }
}
