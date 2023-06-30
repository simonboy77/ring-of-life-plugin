package com.simonboy77;

import com.google.errorprone.annotations.Var;
import lombok.extern.slf4j.Slf4j; // Logging
import java.util.ArrayList;
import java.util.Comparator;

import net.runelite.api.*;

import net.runelite.api.Varbits;
import net.runelite.client.game.ItemManager;
import net.runelite.http.api.item.ItemStats;
import net.runelite.http.api.item.ItemEquipmentStats;

import static net.runelite.api.ItemID.*;

@Slf4j
public class PlayerState {
    private class AttackInfo {
        public int minDamage;
        public int maxDamage;
        public double hitChance;
        public int tick;

        public AttackInfo(int minDamage, int maxDamage, double hitChance, int tick)
        {
            this.minDamage = minDamage;
            this.maxDamage = maxDamage;
            this.hitChance = hitChance;
            this.tick = tick;
        }
    }

    private enum BoostType
    {
        DefenceBoost,
        MagicBoost
    }

    private final Client client;
    private final SurvivalChanceConfig config;
    private final ItemManager itemManager;
    private final MonsterStats monsterStats;

    private final double monsterStanceBonus = 9.0;

    private NPC[] opponents;
    private int[] resultOccurrences;
    private double[] resultWeights;
    private double[] resultChances;

    private int hitpoints;
    private double effectiveDefence;
    private double effectiveMagic;

    private int stabDefenceStat;
    private int slashDefenceStat;
    private int crushDefenceStat;
    private int magicDefenceStat;
    private int rangedDefenceStat;

    private boolean wearingEscapeItem;
    private boolean wearingPhoenix;
    private boolean redemptionActive;

    // TESTING
    private int iterationsTest;
    private int hitCounter;

    public PlayerState(Client client, SurvivalChanceConfig config, ItemManager itemManager)
    {
        this.client = client;
        this.config = config;
        this.itemManager = itemManager;

        this.opponents = new NPC[0];
        this.monsterStats = new MonsterStats();

        this.hitpoints = this.client.getBoostedSkillLevel(Skill.HITPOINTS);
        //this.calcEffectiveDefence();
        //this.calcEffectiveMagic();

        this.resultWeights = new double[HitResult.RESULT_AMOUNT];
        this.resultChances = new double[HitResult.RESULT_AMOUNT];
    }

    private void calcEffectiveDefence()
    {
        this.effectiveDefence = this.client.getBoostedSkillLevel(Skill.DEFENCE);

        if(this.client.getVarbitValue(Varbits.PRAYER_THICK_SKIN) == 1) {
            this.effectiveDefence *= 1.05;
        }
        else if(this.client.getVarbitValue(Varbits.PRAYER_ROCK_SKIN) == 1) {
            this.effectiveDefence *= 1.1;
        }
        else if(this.client.getVarbitValue(Varbits.PRAYER_STEEL_SKIN) == 1) {
            this.effectiveDefence *= 1.15;
        }
        else if(this.client.getVarbitValue(Varbits.PRAYER_CHIVALRY) == 1) {
            this.effectiveDefence *= 1.2;
        }
        else if(this.client.getVarbitValue(Varbits.PRAYER_PIETY) == 1) {
            this.effectiveDefence *= 1.25;
        }
        else if(this.client.getVarbitValue(Varbits.PRAYER_RIGOUR) == 1) {
            this.effectiveDefence *= 1.25;
        }
        else if(this.client.getVarbitValue(Varbits.PRAYER_AUGURY) == 1) {
            this.effectiveDefence *= 1.25;
        }

        this.effectiveDefence = Math.floor(this.effectiveDefence);

        int weaponType = this.client.getVarbitValue(Varbits.EQUIPPED_WEAPON_TYPE);
        int attackStyle = this.client.getVarpValue(VarPlayer.ATTACK_STYLE);
        this.effectiveDefence += this.getAttackStyleBoost(weaponType, attackStyle, BoostType.DefenceBoost);
        this.effectiveDefence += 8.0;
    }

    private void calcEffectiveMagic()
    {
        // NOTE: If this ever requires invisible magic boost from attack style, dont forget to call this changing
        double boostedDefence = this.client.getBoostedSkillLevel(Skill.DEFENCE);
        this.effectiveMagic = this.client.getBoostedSkillLevel(Skill.MAGIC);

        if(this.client.getVarbitValue(Varbits.PRAYER_MYSTIC_WILL) == 1) {
            this.effectiveMagic *= 1.05;
        }
        else if(this.client.getVarbitValue(Varbits.PRAYER_MYSTIC_LORE) == 1) {
            this.effectiveMagic *= 1.1;
        }
        else if(this.client.getVarbitValue(Varbits.PRAYER_MYSTIC_MIGHT) == 1) {
            this.effectiveMagic *= 1.15;
        }
        else if(this.client.getVarbitValue(Varbits.PRAYER_AUGURY) == 1) {
            this.effectiveMagic *= 1.25;
        }

        this.effectiveMagic = Math.floor(this.effectiveMagic);
        this.effectiveMagic = (this.effectiveMagic * 0.7) + (boostedDefence * 0.3);
        this.effectiveMagic += 8.0;
    }

    public boolean isWearingEscapeItem()
    {
        return this.wearingEscapeItem;
    }

    public boolean isWearingPhoenix()
    {
        return this.wearingPhoenix;
    }

    public boolean isRedemptionActive()
    {
        return this.redemptionActive;
    }

    public boolean isInCombat()
    {
        return (this.opponents.length > 0);
    }

    public double getResultChance(int result)
    {
        if(this.isInCombat() && result >= HitResult.RESULT_SURVIVE && result < HitResult.RESULT_AMOUNT) {
            if(!Double.isNaN(this.resultChances[result])) {
                return this.resultChances[result];
            }
        }

        return 0.0; // Or should i return 100%?
    }

    public void addOpponent(NPC newOpponent)
    {
        boolean isValid = this.monsterStats.containsId(newOpponent.getId());
        for(int opponentId = 0; (opponentId < this.opponents.length) && isValid; ++opponentId) {
            isValid = (this.opponents[opponentId] != newOpponent);
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

            this.hitCounter = 0;
            this.client.setTickCount(0);

            log.info("Reset hitCounter and gameTicks");
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
            if(op.getInteracting() != this.client.getLocalPlayer() || op.isDead() || op.getId() == -1) {
                removeOpponent(op);
            }
            else {
                opponentId++;
            }
        }
    }

    private void attack_func_slow(int attackNum, int attackAmount, int curDamage, int maxDamage,
                          boolean phoenix, int escapeDamage, int deathDamage)
    {
        ++this.iterationsTest;

        if(attackNum < attackAmount)
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
                    attack_func_slow(attackNum + 1, attackAmount, nextDamage, maxDamage, phoenix, escapeDamage, deathDamage);
                }
            }
        }
        else if(attackAmount > 0)
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

    private void attack_func_slow(int attackAmount, int maxDamage,
                               boolean phoenix, int escapeDamage, int deathDamage)
    {
        this.attack_func_slow(0, attackAmount, 0, maxDamage, phoenix, escapeDamage, deathDamage);
    }

    private void attack_func(int attackNum, int attackAmount, int curDamage, int maxDamage,
                          int triggerDamage, int deathDamage, boolean escapeItem, boolean phoenix)
    {
        // TODO: Should I add minDamage? Now its assumed to always be zero
        // TODO: add redemption boolean, works like phoenix

        /* if phoenix gets triggered do:
            nextDamage -= phoenixHealAmount
            and pass false for the phoenix boolean, DONT CHANGE IT
        */

        ++this.iterationsTest;

        if(attackNum < attackAmount)
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

                if(nextDamage >= deathDamage) {
                    this.resultOccurrences[HitResult.RESULT_DEATH]++;
                }
                else if(triggerItem && nextDamage >= triggerDamage) {
                    // TODO: if phoenix
                    this.resultOccurrences[HitResult.RESULT_ESCAPE]++;
                }
                else // Check if all of the remaining hits are safe
                {
                    int remainingHits = attackAmount - (attackNum + 1);
                    int maxTotalDamage = nextDamage + (maxDamage * remainingHits);

                    if((escapeItem || phoenix) && (maxTotalDamage >= triggerDamage))
                    {
                        // TODO: if phoenix
                        attack_func(attackNum + 1, attackAmount, nextDamage, maxDamage, triggerDamage, deathDamage, escapeItem, phoenix);
                    }
                    else if(maxTotalDamage >= deathDamage)
                    {
                        attack_func(attackNum + 1, attackAmount, nextDamage, maxDamage, triggerDamage, deathDamage, escapeItem, phoenix);
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
        else if(attackAmount > 0)
        {
            log.info("curDamage " + curDamage);

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

    private void attack_func(int attackAmount, int maxDamage, int triggerDamage,
                          int deathDamage, boolean escapeItem, boolean phoenix)
    {
        this.attack_func(0, attackAmount, 0, maxDamage, triggerDamage, deathDamage, escapeItem, phoenix);
    }

    private void attack_func_weighted(int attackNum, ArrayList<AttackInfo> attacks, double hitChance, int curDamage, int triggerDamage,
                                 int deathDamage, boolean escapeItem, boolean phoenix)
    {
        // TODO: If hitChance == 0.0 then what the fuck
        ++this.iterationsTest;

        if(attackNum < attacks.size())
        {
            AttackInfo curHitInfo = attacks.get(attackNum);
            double nextHitChance = hitChance * curHitInfo.hitChance;
            double missChance = hitChance * (1.0 - curHitInfo.hitChance);

            for(int damage = curHitInfo.maxDamage; damage >= curHitInfo.minDamage; --damage)
            {
                boolean triggerItem = (escapeItem || phoenix);
                int nextDamage = curDamage + damage;

                if(nextDamage >= deathDamage)
                {
                    this.resultWeights[HitResult.RESULT_DEATH] += nextHitChance;
                    attack_func_weighted(attackNum + 1, attacks, missChance, curDamage, triggerDamage, deathDamage, escapeItem, phoenix);
                }
                else if(triggerItem && (nextDamage >= triggerDamage))
                {
                    // TODO: if phoenix
                    this.resultWeights[HitResult.RESULT_ESCAPE] += nextHitChance;
                    attack_func_weighted(attackNum + 1, attacks, missChance, curDamage, triggerDamage, deathDamage, escapeItem, phoenix);
                }
                else // Check if all of the remaining hits are safe
                {
                    int remainingHits = attacks.size() - (attackNum + 1);
                    int maxTotalDamage = nextDamage + (curHitInfo.maxDamage * remainingHits);
                    //log.info("max total damage from " + nextDamage + ": " + maxTotalDamage);

                    if((escapeItem || phoenix) && (maxTotalDamage >= triggerDamage)) {
                        // TODO: if phoenix
                        attack_func_weighted(attackNum + 1, attacks, nextHitChance, nextDamage, triggerDamage, deathDamage, escapeItem, phoenix);
                        attack_func_weighted(attackNum + 1, attacks, missChance, curDamage, triggerDamage, deathDamage, escapeItem, phoenix);
                    }
                    else if(maxTotalDamage >= deathDamage) {
                        attack_func_weighted(attackNum + 1, attacks, nextHitChance, nextDamage, triggerDamage, deathDamage, escapeItem, phoenix);
                        attack_func_weighted(attackNum + 1, attacks, missChance, curDamage, triggerDamage, deathDamage, escapeItem, phoenix);
                    }
                    else // All following hits are safe
                    {
                        double curDamageVariation = (damage - curHitInfo.minDamage) + 1;
                        double fullDamageVariation = (curHitInfo.maxDamage - curHitInfo.minDamage) + 1;
                        int remainingHitOccurrences = (int)(curDamageVariation * Math.pow(fullDamageVariation, remainingHits));

                        //int remainingHitOccurrences = (damage + 1) * (int)Math.pow((double)(maxDamage + 1), (double)remainingHits);
                        this.resultWeights[HitResult.RESULT_SURVIVE] += remainingHitOccurrences;
                        break;
                    }
                }
            }
        }
        else if(attacks.size() > 0)
        {
            if(curDamage >= deathDamage) {
                this.resultWeights[HitResult.RESULT_DEATH] += hitChance;
            }
            else if(curDamage >= triggerDamage) {
                this.resultWeights[HitResult.RESULT_ESCAPE] += hitChance;
            }
            else {
                this.resultWeights[HitResult.RESULT_SURVIVE] += hitChance;
            }
        }
    }

    private void attack_func_weighted(ArrayList<AttackInfo> attacks, int triggerDamage, int deathDamage, boolean escapeItem, boolean phoenix)
    {
        attack_func_weighted(0, attacks, 1.0, 0, triggerDamage, deathDamage, escapeItem, phoenix);
    }

    private double calcHitChanceNonMagic(double mLvl, double mAttackStat, double pDefenceStat)
    {
        double mEffectiveLvl = mLvl + this.monsterStanceBonus;
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
        double mMagicAtkStat = this.monsterStats.getMagicStat(monsterId);
        double pMagicDefenceStat = this.magicDefenceStat;

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
            String attackStyle = attackStyles.get(0);
            if(attackStyle.equals("stab")) {
                if(this.client.getVarbitValue(Varbits.PRAYER_PROTECT_FROM_MELEE) == 1) {
                    hitChance = 0.0;
                }
                else {
                    hitChance = this.calcHitChanceNonMagic(this.monsterStats.getAttackLevel(monsterId),
                            this.monsterStats.getAttackStat(monsterId), this.stabDefenceStat);
                }
            }
            else if(attackStyle.equals("slash")) {
                if(this.client.getVarbitValue(Varbits.PRAYER_PROTECT_FROM_MELEE) == 1) {
                    hitChance = 0.0;
                }
                else {
                    hitChance = this.calcHitChanceNonMagic(this.monsterStats.getAttackLevel(monsterId),
                            this.monsterStats.getAttackStat(monsterId), this.slashDefenceStat);
                }
            }
            else if(attackStyle.equals("crush")) {
                if(this.client.getVarbitValue(Varbits.PRAYER_PROTECT_FROM_MELEE) == 1) {
                    hitChance = 0.0;
                }
                else {
                    hitChance = this.calcHitChanceNonMagic(this.monsterStats.getAttackLevel(monsterId),
                            this.monsterStats.getAttackStat(monsterId), this.crushDefenceStat);
                }
            }
            else if(attackStyle.equals("magic")) {
                if(this.client.getVarbitValue(Varbits.PRAYER_PROTECT_FROM_MAGIC) == 1) {
                    hitChance = 0.0;
                }
                else {
                    hitChance = calcHitChanceMagic(opponent);
                }
            }
            else if(attackStyle.equals("ranged")) {
                if(this.client.getVarbitValue(Varbits.PRAYER_PROTECT_FROM_MISSILES) == 1) {
                    hitChance = 0.0;
                }
                else {
                    hitChance = this.calcHitChanceNonMagic(this.monsterStats.getRangedLevel(monsterId),
                            this.monsterStats.getRangedStat(monsterId), this.rangedDefenceStat);
                }
            }
            // everything else we just give a chance of 1.0
        }
        else if(attackStyles.size() > 1) {
            /*
            If more than 1 attack style is used (melee/magic/ranged), we assume
            a protection prayer will only block (1 / styleAmount) of the damage
            */
        }

        // Remove zero hit from hitChance
        if(hitChance > 0.0) {
            double damageVariation = this.monsterStats.getMaxHit(monsterId) + 1;
            hitChance -= hitChance / damageVariation;
        }

        opponent.setOverheadText(String.format("%.1f%%", hitChance * 100.0));
        return hitChance;
    }

    private ArrayList<AttackInfo> prepare_attacks()
    {
        ArrayList<AttackInfo> attacks = new ArrayList<>();
        int ticksOfCombat = (int)Math.floor((double)this.config.secondsOfCombat() / 0.6);

        for (NPC opponent : this.opponents) {
            int id = opponent.getId();

            int minDamage = 1;
            int maxDamage = this.monsterStats.getMaxHit(id);
            int attackSpeed = this.monsterStats.getAttackSpeed(id);
            double hitChance = this.calcHitChance(opponent);

            for (int attackTick = 0; attackTick < ticksOfCombat; attackTick += attackSpeed) {
                attacks.add(new AttackInfo(minDamage, maxDamage, hitChance, attackTick));
            }
        }

        attacks.sort(new Comparator<AttackInfo>() {
            @Override
            public int compare(AttackInfo a, AttackInfo b) {
                return (a.tick < b.tick) ? -1 : ((a.tick > b.tick) ? 1 : 0);
            }
        });

        for(AttackInfo attack : attacks)
        {
            //log.info("tick: " + attack.tick + ", min: " + attack.minDamage + ", max: " + attack.maxDamage + ", chance: " + attack.hitChance);
        }

        return attacks;
    }

    public void calcSurvivalChance()
    {
        if(this.isInCombat())
        {
            ArrayList<AttackInfo> attacks = prepare_attacks();

            double hpLvl = this.client.getRealSkillLevel(Skill.HITPOINTS);
            int triggerDamage = this.hitpoints - (int)Math.floor(hpLvl / 10.0);
            this.resultWeights = new double[HitResult.RESULT_AMOUNT];
            this.resultChances = new double[HitResult.RESULT_AMOUNT];

            attack_func_weighted(attacks, triggerDamage, this.hitpoints, this.wearingEscapeItem, this.wearingPhoenix);

            // Parse results
            double totalWeight = 0.0;
            for(int resultId = 0; resultId < HitResult.RESULT_AMOUNT; ++resultId) {
                totalWeight += this.resultWeights[resultId];
            }

            for(int resultId = 0; resultId < HitResult.RESULT_AMOUNT; ++resultId) {
                double chance = (this.resultWeights[resultId] / totalWeight) * 100.0;
                this.resultChances[resultId] = chance;
            }

            log.info("safety: " + this.resultChances[HitResult.RESULT_SURVIVE] +
                    ", escape: " + this.resultChances[HitResult.RESULT_ESCAPE] +
                    ", die: " + this.resultChances[HitResult.RESULT_DEATH]);
        }
    }

    public void calcSurvivalChanceTest()
    {
        //double hitChance = this.calcHitChance(this.opponents[0]);
        //log.info("hitChance: " + hitChance);

//        int hitAmount = this.config.hitTurns();
//        //int maxHit = this.monsterStats.getMaxHit(this.opponents[0].getId());
//        int maxHp = this.client.getRealSkillLevel(Skill.HITPOINTS);
//
//        // TODO: if wearing ring of life/defence cape
//        int triggerDamage = this.hitpoints - (int)Math.floor((double)maxHp / 10.0);
//        this.resultOccurrences = new int[HitResult.RESULT_AMOUNT];
//        this.resultWeights = new double[HitResult.RESULT_AMOUNT];
//        this.resultChances = new double[HitResult.RESULT_AMOUNT];
//
//        this.iterationsTest = 0;
//
//        hitAmount = 1;
//        int maxHit = 12;
//
//        HitInfo hits[] = new HitInfo[1];
//        hits[0] = new HitInfo(0, maxHit, 0.5);
//
//        //hit_func_slow(hitAmount, maxHit, false, triggerDamage, this.hitpoints);
//        //hit_func(hitAmount, maxHit, triggerDamage, this.hitpoints, true, false);
//        hit_func_weighted(hits, triggerDamage, this.hitpoints, true, false);
//
//        // Parse results
//        double totalHits = 0.0;
//        double totalWeight = 0.0;
//
//        for(int resultId = 0; resultId < HitResult.RESULT_AMOUNT; ++resultId) {
//            totalHits += this.resultOccurrences[resultId];
//            totalWeight += this.resultWeights[resultId];
//        }
//
//        for(int resultId = 0; resultId < HitResult.RESULT_AMOUNT; ++resultId) {
//            //double chance = ((double)this.resultOccurrences[resultId] / totalHits) * 100.0;
//            double chance = (this.resultWeights[resultId] / totalWeight) * 100.0;
//
//            this.resultChances[resultId] = chance;
//        }
//
//        log.info("safety: " + this.resultChances[HitResult.RESULT_SURVIVE] +
//                ", escape: " + this.resultChances[HitResult.RESULT_ESCAPE] +
//                ", die: " + this.resultChances[HitResult.RESULT_DEATH] +
//                ", totalHits: " + totalHits +
//                ", iterations: " + this.iterationsTest);
    }

    private void test(int damage)
    {
        this.hitCounter++;
        log.info("hit " + this.hitCounter + " on tick " + this.client.getTickCount() + " for " + damage + " damage");
    }

    public void equipmentChanged(ItemContainer equipment)
    {
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

        this.calcEffectiveDefence(); // In case equipped weapon changed
        this.calcSurvivalChance();
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
                        int hitDamage = this.hitpoints - newHitpoints;
                        this.test(hitDamage);
                    }
                    this.hitpoints = newHitpoints;
                    this.calcSurvivalChance();
                }
            } break;

            case DEFENCE:
            {
                this.calcEffectiveDefence();
                this.calcEffectiveMagic(); // Effective magic uses defence level
                this.calcSurvivalChance();
            } break;

            case MAGIC:
            {
                this.calcEffectiveMagic();
                this.calcSurvivalChance();
            } break;

            default: break;
        }
    }

    public void varbitChanged(int varbitId, int varpId)
    {
        if(varbitId == Varbits.PRAYER_AUGURY)
        {
            this.calcEffectiveDefence();
            this.calcEffectiveMagic();
            this.calcSurvivalChance();
        }
        else if(varbitId == Varbits.PRAYER_THICK_SKIN ||
                varbitId == Varbits.PRAYER_ROCK_SKIN ||
                varbitId == Varbits.PRAYER_STEEL_SKIN ||
                varbitId == Varbits.PRAYER_CHIVALRY ||
                varbitId == Varbits.PRAYER_PIETY ||
                varbitId == Varbits.PRAYER_RIGOUR)
        {
            this.calcEffectiveDefence();
            this.calcSurvivalChance();
        }
        else if(varbitId == Varbits.PRAYER_MYSTIC_WILL ||
                varbitId == Varbits.PRAYER_MYSTIC_LORE ||
                varbitId == Varbits.PRAYER_MYSTIC_MIGHT)
        {
            this.calcEffectiveMagic();
            this.calcSurvivalChance();
        }
        else if(varbitId == Varbits.PRAYER_REDEMPTION) {
            this.redemptionActive = (this.client.getVarbitValue(Varbits.PRAYER_REDEMPTION) == 1);
            this.calcSurvivalChance();
        }
        else if(varbitId == Varbits.PRAYER_PROTECT_FROM_MAGIC ||
                varbitId == Varbits.PRAYER_PROTECT_FROM_MISSILES ||
                varbitId == Varbits.PRAYER_PROTECT_FROM_MELEE)
        {
            this.calcSurvivalChance();
        }

        if(varbitId == VarPlayer.ATTACK_STYLE || varpId == VarPlayer.ATTACK_STYLE) {
            this.calcEffectiveDefence();
            this.calcSurvivalChance();
        }
    }

    private int getAttackStyleBoost(int weaponType, int attackStyle, BoostType boostType)
    {
        int defenceBoost = 0;
        int magicBoost = 0;

        switch(weaponType)
        {
            case 0: { // UNARMED
                if(attackStyle == 3) { defenceBoost = 3; } // defensive
            } break;

            case 1: { // AXE
                if(attackStyle == 3) { defenceBoost = 3; } // defensive
            } break;

            case 2: { // BLUNT
                if(attackStyle == 3) { defenceBoost = 3; } // defensive
            } break;

            case 3: { // BOW
                if(attackStyle == 3) { defenceBoost = 3; } // longrange
            } break;

            case 4: { // CLAW
                if(attackStyle == 3) { defenceBoost = 3; } // defensive
            } break;

            case 5: { // CROSSBOW
                if(attackStyle == 3) { defenceBoost = 3; } // longrange
            } break;

            case 6: { // SALAMANDER
                if(attackStyle == 2) { defenceBoost = 3; } // defensive
            } break;

            case 7: { /* No relevant boosts */ } break; // CHINCHOMPA
            case 8: { /* No relevant boosts */ } break; // GUN

            case 9 : { // SLASH_SWORD
                if(attackStyle == 2) { defenceBoost = 1; } // controlled
                else if(attackStyle == 3) { defenceBoost = 3; } // defensive
            } break;

            case 10: { // TWO_HANDED_SWORD
                if(attackStyle == 3) { defenceBoost = 3; } // defensive
            } break;

            case 11: { // PICKAXE
                if(attackStyle == 3) { defenceBoost = 3; } // defensive
            } break;

            case 12: { // POLEARM
                if(attackStyle == 0) { defenceBoost = 1; } // controlled
                else if(attackStyle == 3) { defenceBoost = 3; } // defensive
            } break;

            case 13: { // POLESTAFF
                if(attackStyle == 3) { defenceBoost = 3; } // defensive
            } break;

            case 14: // Unkown
            {
                log.info("This is the unknown weapon style!");
            } break;

            case 15: { // SPEAR
                if(attackStyle == 0 || attackStyle == 1 || attackStyle == 2) { defenceBoost = 1; } // controlled
                else if(attackStyle == 3) { defenceBoost = 3; } // defensive
            } break;

            case 16: { // SPIKED
                if(attackStyle == 2) { defenceBoost = 1; } // controlled
                else if(attackStyle == 3) { defenceBoost = 3; } // defensive
            } break;

            case 17: { // STAB_SWORD
                if(attackStyle == 3) { defenceBoost = 3; } // defensive
            } break;

            case 18: { // STAFF
                if(attackStyle == 3) { defenceBoost = 3; } // defensive
            } break;

            case 19: { // THROWN
                if(attackStyle == 3) { defenceBoost = 3; } // longrange
            } break;

            case 20: { // WHIP
                if(attackStyle == 1) { defenceBoost = 1; } // controlled
                else if(attackStyle == 3) { defenceBoost = 3; } // defensive
            } break;

            case 21: { // BLADED_STAFF
                if(attackStyle == 3) { defenceBoost = 3; } // defensive
            } break;

            case 22: { // SCYTHE
                if(attackStyle == 3) { defenceBoost = 3; } // defensive
            } break;

            case 23: { // POWERED_STAFF
                if(attackStyle == 0 || attackStyle == 1) { magicBoost = 3; } // accurate
                else if(attackStyle == 3) { magicBoost = 1; defenceBoost = 3; } // longrange
            } break;

            case 24: { // BANNER
                if(attackStyle == 2) { defenceBoost = 1; } // controlled
                else if(attackStyle == 3) { defenceBoost = 3; } // defensive
            } break;

            case 25: { /* Honestly no clue */ } break; // BLASTER
            case 26: { /* No relevant boosts */ } break; // BLUDGEON
            case 27: { /* No relevant boosts */ } break; // BULWARK

            case 28: { // POWERED_WAND
                if(attackStyle == 0 || attackStyle == 1) { magicBoost = 3; } // accurate
                else if(attackStyle == 3) { magicBoost = 1; defenceBoost = 3; } // longrange
            } break;

            case 29: { // PARTISAN
                if(attackStyle == 3) { defenceBoost = 3; } // defensive
            } break;
        }

        if(boostType == BoostType.DefenceBoost) {
            return defenceBoost;
        }
        else if(boostType == BoostType.MagicBoost) {
            return magicBoost;
        }

        return 0;
    }
}
