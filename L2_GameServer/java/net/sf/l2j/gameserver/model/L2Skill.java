/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.l2j.gameserver.model;

import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javolution.util.FastList;
import net.sf.l2j.Config;
import net.sf.l2j.gameserver.GeoData;
import net.sf.l2j.gameserver.datatables.HeroSkillTable;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.datatables.SkillTreeTable;
import net.sf.l2j.gameserver.model.actor.instance.L2ArtefactInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2ChestInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2CubicInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2DoorInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2NpcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PetInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PlayableInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2SiegeFlagInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2SummonInstance;
import net.sf.l2j.gameserver.model.base.ClassId;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.serverpackets.EtcStatusUpdate;
import net.sf.l2j.gameserver.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.Env;
import net.sf.l2j.gameserver.skills.Stats;
import net.sf.l2j.gameserver.skills.conditions.Condition;
import net.sf.l2j.gameserver.skills.effects.EffectCharge;
import net.sf.l2j.gameserver.skills.effects.EffectTemplate;
import net.sf.l2j.gameserver.skills.funcs.Func;
import net.sf.l2j.gameserver.skills.funcs.FuncTemplate;
import net.sf.l2j.gameserver.skills.l2skills.L2SkillAgathion;
import net.sf.l2j.gameserver.skills.l2skills.L2SkillChangeWeapon;
import net.sf.l2j.gameserver.skills.l2skills.L2SkillCharge;
import net.sf.l2j.gameserver.skills.l2skills.L2SkillChargeDmg;
import net.sf.l2j.gameserver.skills.l2skills.L2SkillChargeEffect;
import net.sf.l2j.gameserver.skills.l2skills.L2SkillCreateItem;
import net.sf.l2j.gameserver.skills.l2skills.L2SkillDecoy;
import net.sf.l2j.gameserver.skills.l2skills.L2SkillDefault;
import net.sf.l2j.gameserver.skills.l2skills.L2SkillDrain;
import net.sf.l2j.gameserver.skills.l2skills.L2SkillSignet;
import net.sf.l2j.gameserver.skills.l2skills.L2SkillSignetCasttime;
import net.sf.l2j.gameserver.skills.l2skills.L2SkillSummon;
import net.sf.l2j.gameserver.skills.l2skills.L2SkillTrap;
import net.sf.l2j.gameserver.taskmanager.DecayTaskManager;
import net.sf.l2j.gameserver.templates.L2WeaponType;
import net.sf.l2j.gameserver.templates.StatsSet;
import net.sf.l2j.gameserver.util.Util;

/**
 * This class...
 *
 * @version $Revision: 1.3.2.8.2.22 $ $Date: 2005/04/06 16:13:42 $
 */
public abstract class L2Skill
{
    protected static final Logger _log = Logger.getLogger(L2Skill.class.getName());

    public static final int SKILL_CUBIC_MASTERY = 143;
    public static final int SKILL_LUCKY = 194;
    public static final int SKILL_CREATE_COMMON = 1320;
    public static final int SKILL_CREATE_DWARVEN = 172;
    public static final int SKILL_CRYSTALLIZE = 248;
    public static final int SKILL_DIVINE_INSPIRATION = 1405;
    public static final int SKILL_CLAN_LUCK = 390;

    public static final int SKILL_FAKE_INT = 9001;
    public static final int SKILL_FAKE_WIT = 9002;
    public static final int SKILL_FAKE_MEN = 9003;
    public static final int SKILL_FAKE_CON = 9004;
    public static final int SKILL_FAKE_DEX = 9005;
    public static final int SKILL_FAKE_STR = 9006;

    public static enum SkillOpType
    {
        OP_PASSIVE, OP_ACTIVE, OP_TOGGLE, OP_CHANCE
    }

    /** Target types of skills : SELF, PARTY, CLAN, PET... */
    public static enum SkillTargetType
    {
        TARGET_NONE,
        TARGET_SELF,
        TARGET_ONE,
        TARGET_PARTY,
        TARGET_ALLY,
        TARGET_CLAN,
        TARGET_PET,
        TARGET_AREA,
        TARGET_FRONT_AREA,
        TARGET_BEHIND_AREA,
        TARGET_AURA,
        TARGET_FRONT_AURA,
        TARGET_BEHIND_AURA,
        TARGET_CORPSE,
        TARGET_UNDEAD,
        TARGET_AREA_UNDEAD,
        TARGET_MULTIFACE,
        TARGET_CORPSE_ALLY,
        TARGET_CORPSE_CLAN,
        TARGET_CORPSE_PLAYER,
        TARGET_CORPSE_PET,
        TARGET_ITEM,
        TARGET_AREA_CORPSE_MOB,
        TARGET_CORPSE_MOB,
        TARGET_UNLOCKABLE,
        TARGET_HOLY,
        TARGET_FLAGPOLE,
        TARGET_PARTY_MEMBER,
        TARGET_PARTY_OTHER,
        TARGET_ENEMY_SUMMON,
        TARGET_OWNER_PET,
        TARGET_GROUND
    }

    public static enum SkillType
    {
    	// Damage
    	PDAM,
    	MDAM,
    	CPDAM,
    	MANADAM,
    	DOT,
    	MDOT,
    	DRAIN_SOUL,
    	DRAIN(L2SkillDrain.class),
    	DEATHLINK,
    	BLOW,
    	SIGNET(L2SkillSignet.class),
    	SIGNET_CASTTIME(L2SkillSignetCasttime.class),

    	// Disablers
    	BLEED,
    	POISON,
    	STUN,
    	ROOT,
    	CONFUSION,
    	FEAR,
    	SLEEP,
    	CONFUSE_MOB_ONLY,
    	MUTE,
    	PARALYZE,
    	WEAKNESS,
        DISARM,

    	// hp, mp, cp
    	HEAL,
    	HOT,
    	BALANCE_LIFE,
    	HEAL_PERCENT,
    	HEAL_STATIC,
    	COMBATPOINTHEAL,
    	CPHOT,
    	MANAHEAL,
    	MANA_BY_LEVEL,
    	MANAHEAL_PERCENT,
    	MANARECHARGE,
    	MPHOT,
    	
    	// sp
    	GIVE_SP,

    	// Aggro
    	AGGDAMAGE,
    	AGGREDUCE,
    	AGGREMOVE,
    	AGGREDUCE_CHAR,
    	AGGDEBUFF,

    	// Fishing
    	FISHING,
    	PUMPING,
    	REELING,

    	// MISC
    	UNLOCK,
    	ENCHANT_ARMOR,
    	ENCHANT_WEAPON,
    	SOULSHOT,
    	SPIRITSHOT,
    	SIEGEFLAG,
    	TAKECASTLE,
    	TAKEFORT,
    	WEAPON_SA,
    	DELUXE_KEY_UNLOCK,
    	SOW,
        HARVEST,
        GET_PLAYER,
        AGATHION(L2SkillAgathion.class),

    	// Creation
    	COMMON_CRAFT,
    	DWARVEN_CRAFT,
    	CREATE_ITEM(L2SkillCreateItem.class),
    	SUMMON_TREASURE_KEY,

    	// Summons
    	SUMMON(L2SkillSummon.class),
    	FEED_PET,
    	DEATHLINK_PET,
    	STRSIEGEASSAULT,
    	ERASE,
    	BETRAY,
    	DECOY(L2SkillDecoy.class),
    	
    	// Cancel
    	CANCEL,
    	CANCEL_DEBUFF,
    	MAGE_BANE,
    	WARRIOR_BANE,
    	NEGATE,

    	BUFF,
    	DEBUFF,
    	PASSIVE,
    	CONT,

    	RESURRECT,
    	CHARGE(L2SkillCharge.class),
    	CHARGE_EFFECT(L2SkillChargeEffect.class),
    	CHARGEDAM(L2SkillChargeDmg.class),
    	MHOT,
    	DETECT_WEAKNESS,
    	LUCK,
    	RECALL,
    	SUMMON_FRIEND,
    	REFLECT,
    	SPOIL,
    	SWEEP,
    	FAKE_DEATH,
    	UNBLEED,
    	UNPOISON,
    	UNDEAD_DEFENSE,
    	BEAST_FEED,
    	FORCE_BUFF,
        CHARGESOUL,
        TRANSFORM,
        TRANSFORMDISPEL,
        SUMMON_TRAP (L2SkillTrap.class),
        DETECT_TRAP,
        REMOVE_TRAP,
        SHIFT_TARGET,
        // Kamael WeaponChange
        CHANGEWEAPON (L2SkillChangeWeapon.class),

		// Skill is done within the core.
		COREDONE,
		
        // unimplemented
        NOTDONE;

        private final Class<? extends L2Skill> _class;

        public L2Skill makeSkill(StatsSet set)
        {
            try
            {
                Constructor<? extends L2Skill> c = _class.getConstructor(StatsSet.class);

                return c.newInstance(set);
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        }

        private SkillType()
        {
            _class = L2SkillDefault.class;
        }

        private SkillType(Class<? extends L2Skill> classType)
        {
            _class = classType;
        }
    }

    //elements
    public final static int ELEMENT_WIND = 1;
    public final static int ELEMENT_FIRE = 2;
    public final static int ELEMENT_WATER = 3;
    public final static int ELEMENT_EARTH = 4;
    public final static int ELEMENT_HOLY = 5;
    public final static int ELEMENT_DARK = 6;

    //save vs
    public final static int SAVEVS_INT = 1;
    public final static int SAVEVS_WIT = 2;
    public final static int SAVEVS_MEN = 3;
    public final static int SAVEVS_CON = 4;
    public final static int SAVEVS_DEX = 5;
    public final static int SAVEVS_STR = 6;

    //stat effected
    public final static int STAT_PATK = 301; // pAtk
    public final static int STAT_PDEF = 302; // pDef
    public final static int STAT_MATK = 303; // mAtk
    public final static int STAT_MDEF = 304; // mDef
    public final static int STAT_MAXHP = 305; // maxHp
    public final static int STAT_MAXMP = 306; // maxMp
    public final static int STAT_CURHP = 307;
    public final static int STAT_CURMP = 308;
    public final static int STAT_HPREGEN = 309; // regHp
    public final static int STAT_MPREGEN = 310; // regMp
    public final static int STAT_CASTINGSPEED = 311; // sCast
    public final static int STAT_ATKSPD = 312; // sAtk
    public final static int STAT_CRITDAM = 313; // critDmg
    public final static int STAT_CRITRATE = 314; // critRate
    public final static int STAT_FIRERES = 315; // fireRes
    public final static int STAT_WINDRES = 316; // windRes
    public final static int STAT_WATERRES = 317; // waterRes
    public final static int STAT_EARTHRES = 318; // earthRes
    public final static int STAT_HOLYRES = 336; // holyRes
    public final static int STAT_DARKRES = 337; // darkRes
    public final static int STAT_ROOTRES = 319; // rootRes
    public final static int STAT_SLEEPRES = 320; // sleepRes
    public final static int STAT_CONFUSIONRES = 321; // confusRes
    public final static int STAT_BREATH = 322; // breath
    public final static int STAT_AGGRESSION = 323; // aggr
    public final static int STAT_BLEED = 324; // bleed
    public final static int STAT_POISON = 325; // poison
    public final static int STAT_STUN = 326; // stun
    public final static int STAT_ROOT = 327; // root
    public final static int STAT_MOVEMENT = 328; // move
    public final static int STAT_EVASION = 329; // evas
    public final static int STAT_ACCURACY = 330; // accu
    public final static int STAT_COMBAT_STRENGTH = 331;
    public final static int STAT_COMBAT_WEAKNESS = 332;
    public final static int STAT_ATTACK_RANGE = 333; // rAtk
    public final static int STAT_NOAGG = 334; // noagg
    public final static int STAT_SHIELDDEF = 335; // sDef
    public final static int STAT_MP_CONSUME_RATE = 336; // Rate of mp consume per skill use
    public final static int STAT_HP_CONSUME_RATE = 337; // Rate of hp consume per skill use
    public final static int STAT_MCRITRATE = 338; // Magic Crit Rate

    //COMBAT DAMAGE MODIFIER SKILLS...DETECT WEAKNESS AND WEAKNESS/STRENGTH
    public final static int COMBAT_MOD_ANIMAL = 200;
    public final static int COMBAT_MOD_BEAST = 201;
    public final static int COMBAT_MOD_BUG = 202;
    public final static int COMBAT_MOD_DRAGON = 203;
    public final static int COMBAT_MOD_MONSTER = 204;
    public final static int COMBAT_MOD_PLANT = 205;
    public final static int COMBAT_MOD_HOLY = 206;
    public final static int COMBAT_MOD_UNHOLY = 207;
    public final static int COMBAT_MOD_BOW = 208;
    public final static int COMBAT_MOD_BLUNT = 209;
    public final static int COMBAT_MOD_DAGGER = 210;
    public final static int COMBAT_MOD_FIST = 211;
    public final static int COMBAT_MOD_DUAL = 212;
    public final static int COMBAT_MOD_SWORD = 213;
    public final static int COMBAT_MOD_POISON = 214;
    public final static int COMBAT_MOD_BLEED = 215;
    public final static int COMBAT_MOD_FIRE = 216;
    public final static int COMBAT_MOD_WATER = 217;
    public final static int COMBAT_MOD_EARTH = 218;
    public final static int COMBAT_MOD_WIND = 219;
    public final static int COMBAT_MOD_ROOT = 220;
    public final static int COMBAT_MOD_STUN = 221;
    public final static int COMBAT_MOD_CONFUSION = 222;
    public final static int COMBAT_MOD_DARK = 223;

    //conditional values
    public final static int COND_RUNNING = 0x0001;
    public final static int COND_WALKING = 0x0002;
    public final static int COND_SIT = 0x0004;
    public final static int COND_BEHIND = 0x0008;
    public final static int COND_CRIT = 0x0010;
    public final static int COND_LOWHP = 0x0020;
    public final static int COND_ROBES = 0x0040;
    public final static int COND_CHARGES = 0x0080;
    public final static int COND_SHIELD = 0x0100;
    public final static int COND_GRADEA = 0x010000;
    public final static int COND_GRADEB = 0x020000;
    public final static int COND_GRADEC = 0x040000;
    public final static int COND_GRADED = 0x080000;
    public final static int COND_GRADES = 0x100000;

    private static final Func[] _emptyFunctionSet = new Func[0];
    private static final L2Effect[] _emptyEffectSet = new L2Effect[0];

    // these two build the primary key
    private final int _id;
    private final int _level;

    /** Identifier for a skill that client can't display */
    private int _displayId;

    // not needed, just for easier debug
    private final String _name;
    private final SkillOpType _operateType;
    private final boolean _magic;
    private final int _mpConsume;
    private final int _mpInitialConsume;
    private final int _hpConsume;
    private final int _cpConsume;
    
    private final int _targetConsume;
    private final int _targetConsumeId;
    
    private final int _itemConsume;
    private final int _itemConsumeId;
    // item consume count over time
    private final int _itemConsumeOT;
    // item consume id over time
    private final int _itemConsumeIdOT;
    // how many times to consume an item
    private final int _itemConsumeSteps;
    // for summon spells:
    // a) What is the total lifetime of summons (in millisecs)
    private final int _summonTotalLifeTime;
    // b) how much lifetime is lost per second of idleness (non-fighting)
    private final int _summonTimeLostIdle;
    // c) how much time is lost per second of activity (fighting)
    private final int _summonTimeLostActive;
    
    private final boolean _isCubic;

    // cubic AI
    private final int _activationtime;
    private final int _activationchance;

    // item consume time in milliseconds
    private final int _itemConsumeTime;
    private final int _castRange;
    private final int _effectRange;
    
    // Remove Skill Effect
    private final int _cancelEffect;

    
    // kill by damage over time
    private final boolean _killByDOT;

    // all times in milliseconds
    private final int _hitTime;
    //private final int _skillInterruptTime;
    private final int _coolTime;
    private final int _reuseDelay;
    private final int _buffDuration;

    /** Target type of the skill : SELF, PARTY, CLAN, PET... */
    private final SkillTargetType _targetType;

    private final double _power;
    private final int _effectPoints;
    private final int _magicLevel;
    private final String[] _negateStats;
    private final float _negatePower;
    private final int _negateId;
    private final int _maxNegatedEffect;
    private final int _levelDepend;

    // Effecting area of the skill, in radius.
    // The radius center varies according to the _targetType:
    // "caster" if targetType = AURA/PARTY/CLAN or "target" if targetType = AREA
    private final int _skillRadius;

    private final SkillType _skillType;
    private final SkillType _effectType;
    private final int _effectPower;
    private final int _effectId;
    private final int _effectLvl;

    private final boolean _ispotion;
    private final int _element;
    private final int _savevs;
    
    private final boolean _isSuicideAttack;

    private final Stats _stat;

    private final int _condition;
    private final int _conditionValue;
    private final boolean _overhit;
    private final int _weaponsAllowed;
    private final int _armorsAllowed;

    private final int _addCrossLearn; // -1 disable, otherwice SP price for others classes, default 1000
    private final float _mulCrossLearn; // multiplay for others classes, default 2
    private final float _mulCrossLearnRace; // multiplay for others races, default 2
    private final float _mulCrossLearnProf; // multiplay for fighter/mage missmatch, default 3
    private final List<ClassId> _canLearn; // which classes can learn
    private final List<Integer> _teachers; // which NPC teaches
    private final int _minPledgeClass;

    private final boolean _isOffensive;
    private final int _numCharges;
    private final int _triggeredId;
    private final int _triggeredLevel;

    private final int _soulConsume;
    private final int _numSouls;
    private final int _expNeeded;
    private final int _critChance;
    
    private final int _transformId;
    private final int _transformDuration;

    private final boolean _isHeroSkill; // If true the skill is a Hero Skill

    private final int _baseCritRate;  // percent of success for skill critical hit (especially for PDAM & BLOW - they're not affected by rCrit values or buffs). Default loads -1 for all other skills but 0 to PDAM & BLOW
    private final int _lethalEffect1;     // percent of success for lethal 1st effect (hit cp to 1 or if mob hp to 50%) (only for PDAM skills)
    private final int _lethalEffect2;     // percent of success for lethal 2nd effect (hit cp,hp to 1 or if mob hp to 1) (only for PDAM skills)
    private final boolean _directHpDmg;  // If true then dmg is being make directly
    private final boolean _isDance;      // If true then casting more dances will cost more MP
    private final int _nextDanceCost;
    private final float _sSBoost;	//If true skill will have SoulShot boost (power*2)
    private final int _aggroPoints;

    protected Condition _preCondition;
    protected Condition _itemPreCondition;
    protected FuncTemplate[] _funcTemplates;
    protected EffectTemplate[] _effectTemplates;
    protected EffectTemplate[] _effectTemplatesSelf;

    protected ChanceCondition _chanceCondition = null;

    // Flying support
    private final String _flyType;
    private final int _flyRadius;
    private final float _flyCourse;

    private final boolean _isDebuff;

    protected L2Skill(StatsSet set)
    {
        _id = set.getInteger("skill_id");
        _level = set.getInteger("level");

        _displayId = set.getInteger("displayId", _id);
        _name = set.getString("name");
        _operateType = set.getEnum("operateType", SkillOpType.class);
        _magic = set.getBool("isMagic", false);
        _ispotion = set.getBool("isPotion", false);
        _mpConsume = set.getInteger("mpConsume", 0);
        _mpInitialConsume = set.getInteger("mpInitialConsume", 0);
        _hpConsume = set.getInteger("hpConsume", 0);
        _cpConsume = set.getInteger("cpConsume", 0);
        _targetConsume = set.getInteger("targetConsumeCount", 0);
        _targetConsumeId = set.getInteger("targetConsumeId", 0);
        _itemConsume = set.getInteger("itemConsumeCount", 0);
        _itemConsumeId = set.getInteger("itemConsumeId", 0);
        _itemConsumeOT = set.getInteger("itemConsumeCountOT", 0);
        _itemConsumeIdOT = set.getInteger("itemConsumeIdOT", 0);
        _itemConsumeTime = set.getInteger("itemConsumeTime", 0);
        _itemConsumeSteps = set.getInteger("itemConsumeSteps", 0);
        _summonTotalLifeTime= set.getInteger("summonTotalLifeTime", 1200000);  // 20 minutes default
        _summonTimeLostIdle= set.getInteger("summonTimeLostIdle", 0);
        _summonTimeLostActive= set.getInteger("summonTimeLostActive", 0);
        
        _isCubic    = set.getBool("isCubic", false);

        _activationtime= set.getInteger("activationtime", 8);
        _activationchance= set.getInteger("activationchance", 30);
        
        _castRange = set.getInteger("castRange", 0);
        _effectRange = set.getInteger("effectRange", -1);
        
        _cancelEffect = set.getInteger("cancelEffect", 0);
        
        _killByDOT = set.getBool("killByDOT", false);
        
        _hitTime = set.getInteger("hitTime", 0);
        _coolTime = set.getInteger("coolTime", 0);
        _isDebuff = set.getBool("isDebuff", false);
        
        if (Config.ENABLE_MODIFY_SKILL_REUSE && Config.SKILL_REUSE_LIST.containsKey(_id))
        {
                if ( Config.DEBUG )
                        _log.info("*** Skill " + _name + " (" + _level + ") changed reuse from " + set.getInteger("reuseDelay", 0) + " to " + Config.SKILL_REUSE_LIST.get(_id) + " seconds.");
                _reuseDelay = Config.SKILL_REUSE_LIST.get(_id);
        }
        else
        {
            _reuseDelay = set.getInteger("reuseDelay", 0);
        }
        
        _buffDuration = set.getInteger("buffDuration", 0);

        _skillRadius = set.getInteger("skillRadius", 80);

        _targetType = set.getEnum("target", SkillTargetType.class);
        _power = set.getFloat("power", 0.f);
        _effectPoints = set.getInteger("effectPoints", 0);
        _negateStats = set.getString("negateStats", "").split(" ");
        _negatePower = set.getFloat("negatePower", 0.f);
        _negateId = set.getInteger("negateId", 0);
        _maxNegatedEffect = set.getInteger("maxNegated", 0);
        _magicLevel = set.getInteger("magicLvl", SkillTreeTable.getInstance().getMinSkillLevel(_id, _level));
        _levelDepend = set.getInteger("lvlDepend", 0);
        _stat = set.getEnum("stat", Stats.class, null);

        _skillType = set.getEnum("skillType", SkillType.class);
        _effectType = set.getEnum("effectType", SkillType.class, null);
        _effectPower = set.getInteger("effectPower", 0);
        _effectId = set.getInteger("effectId", 0);
        _effectLvl = set.getInteger("effectLevel", 0);

        _element = set.getInteger("element", 0);
        _savevs = set.getInteger("save", 0);

        _condition = set.getInteger("condition", 0);
        _conditionValue = set.getInteger("conditionValue", 0);
        _overhit = set.getBool("overHit", false);
        _isSuicideAttack = set.getBool("isSuicideAttack", false);
        _weaponsAllowed = set.getInteger("weaponsAllowed", 0);
        _armorsAllowed = set.getInteger("armorsAllowed", 0);

        _addCrossLearn = set.getInteger("addCrossLearn", 1000);
        _mulCrossLearn = set.getFloat("mulCrossLearn", 2.f);
        _mulCrossLearnRace = set.getFloat("mulCrossLearnRace", 2.f);
        _mulCrossLearnProf = set.getFloat("mulCrossLearnProf", 3.f);
        _minPledgeClass = set.getInteger("minPledgeClass", 0);
        _isOffensive = set.getBool("offensive", isSkillTypeOffensive());
        _numCharges = set.getInteger("num_charges", getLevel());
        _triggeredId = set.getInteger("triggeredId", 0);
        _triggeredLevel = set.getInteger("triggeredLevel", 0);

        if (_operateType == SkillOpType.OP_CHANCE)
            _chanceCondition = ChanceCondition.parse(set);

        _numSouls = set.getInteger("num_souls", 0);
        _soulConsume = set.getInteger("soulConsumeCount", 0);
        _expNeeded = set.getInteger("expNeeded", 0);
        _critChance = set.getInteger("critChance", 0);
        
        _transformId = set.getInteger("transformId", 0);
        _transformDuration = set.getInteger("transformDuration", 0);

        _isHeroSkill = HeroSkillTable.isHeroSkill(_id);

        _baseCritRate = set.getInteger("baseCritRate", (_skillType == SkillType.PDAM  || _skillType == SkillType.BLOW) ? 0 : -1);
        _lethalEffect1 = set.getInteger("lethal1",0);
        _lethalEffect2 = set.getInteger("lethal2",0);

        _directHpDmg  = set.getBool("dmgDirectlyToHp",false);
        _isDance = set.getBool("isDance",false);
        _nextDanceCost = set.getInteger("nextDanceCost", 0);
        _sSBoost = set.getFloat("SSBoost", 0.f);
        _aggroPoints = set.getInteger("aggroPoints", 0);

        _flyType = set.getString("flyType", null);
        _flyRadius = set.getInteger("flyRadius", 200);
        _flyCourse = set.getFloat("flyCourse", 0);

        String canLearn = set.getString("canLearn", null);
        if (canLearn == null)
        {
            _canLearn = null;
        }
        else
        {
            _canLearn = new FastList<ClassId>();
            StringTokenizer st = new StringTokenizer(canLearn, " \r\n\t,;");
            while (st.hasMoreTokens())
            {
                String cls = st.nextToken();
                try
                {
                    _canLearn.add(ClassId.valueOf(cls));
                }
                catch (Throwable t)
                {
                    _log.log(Level.SEVERE, "Bad class " + cls + " to learn skill", t);
                }
            }
        }

        String teachers = set.getString("teachers", null);
        if (teachers == null)
        {
            _teachers = null;
        }
        else
        {
            _teachers = new FastList<Integer>();
            StringTokenizer st = new StringTokenizer(teachers, " \r\n\t,;");
            while (st.hasMoreTokens())
            {
                String npcid = st.nextToken();
                try
                {
                    _teachers.add(Integer.parseInt(npcid));
                }
                catch (Throwable t)
                {
                    _log.log(Level.SEVERE, "Bad teacher id " + npcid + " to teach skill", t);
                }
            }
        }
    }

    public abstract void useSkill(L2Character caster, L2Object[] targets);

    public final boolean isPotion()
    {
        return _ispotion;
    }

    public final int getArmorsAllowed()
    {
        return _armorsAllowed;
    }

    public final int getConditionValue()
    {
        return _conditionValue;
    }

    public final SkillType getSkillType()
    {
        return _skillType;
    }

    public final int getSavevs()
    {
        return _savevs;
    }

    public final int getElement()
    {
        return _element;
    }

    /**
     * Return the target type of the skill : SELF, PARTY, CLAN, PET...<BR><BR>
     *
     */
    public final SkillTargetType getTargetType()
    {
        return _targetType;
    }

    public final int getCondition()
    {
        return _condition;
    }

    public final boolean isOverhit()
    {
        return _overhit;
    }
    
    public final boolean killByDOT()
    {
        return _killByDOT;
    }
    
    public final int cancelEffect()
    {
        return _cancelEffect;
    }

    public final boolean isSuicideAttack()
    {
        return _isSuicideAttack;
    }

    /**
     * Return the power of the skill.<BR><BR>
     */
    public final double getPower(L2Character activeChar)
    {
        if (_skillType == SkillType.DEATHLINK && activeChar != null) return _power
            * Math.pow(1.7165 - activeChar.getCurrentHp() / activeChar.getMaxHp(), 2) * 0.577;
        else return _power;
    }

    public final double getPower()
    {
        return _power;
    }

    public final int getEffectPoints()
    {
        return _effectPoints;
    }

    public final String[] getNegateStats()
    {
        return _negateStats;
    }

    public final float getNegatePower()
    {
        return _negatePower;
    }

    public final int getNegateId()
    {
        return _negateId;
    }

    public final int getMagicLevel()
    {
        return _magicLevel;
    }
    
    public final int getMaxNegatedEffects()
	{
    	return _maxNegatedEffect;
	}

    public final int getLevelDepend()
    {
        return _levelDepend;
    }

    /**
     * Return the additional effect power or base probability.<BR><BR>
     */
    public final int getEffectPower()
    {
        return _effectPower;
    }

    /**
     * Return the additional effect Id.<BR><BR>
     */
    public final int getEffectId()
    {
        return _effectId;
    }
    /**
     * Return the additional effect level.<BR><BR>
     */
    public final int getEffectLvl()
    {
        return _effectLvl;
    }

    /**
     * Return the additional effect skill type (ex : STUN, PARALYZE,...).<BR><BR>
     */
    public final SkillType getEffectType()
    {
        return _effectType;
    }

    /**
     * @return Returns the buffDuration.
     */
    public final int getBuffDuration()
    {
        return _buffDuration;
    }

    /**
     * @return Returns the castRange.
     */
    public final int getCastRange()
    {
        return _castRange;
    }
    
    /**
     * @return Returns the cpConsume;
     */
    public final int getCpConsume()
    {
    	return _cpConsume;
    }

    /**
     * @return Returns the effectRange.
     */
    public final int getEffectRange()
    {
        return _effectRange;
    }

    /**
     * @return Returns the hpConsume.
     */
    public final int getHpConsume()
    {
        return _hpConsume;
    }

    /**
     * @return Returns the id.
     */
    public final int getId()
    {
        return _id;
    }

    /**
     * @return Returns the boolean _isDebuff.
     */
    public final boolean isDebuff()
    {
        return _isDebuff;
    }

    public int getDisplayId()
    {
        return _displayId;
    }

    public void setDisplayId(int id)
    {
        _displayId = id;
    }

    public int getTriggeredId()
    {
        return _triggeredId;
    }

    public int getTriggeredLevel()
    {
        return _triggeredLevel;
    }

    
    /**
     * Return the skill type (ex : BLEED, SLEEP, WATER...).<BR><BR>
     */
    public final Stats getStat()
    {
        return _stat;
    }
    
    /**
     * @return Returns the _targetConsumeId.
     */
    public final int getTargetConsumeId()
    {
        return _targetConsumeId;
    }
    
    /**
     * @return Returns the targetConsume.
     */
    public final int getTargetConsume()
    {
        return _targetConsume;
    }
    /**
     * @return Returns the itemConsume.
     */
    public final int getItemConsume()
    {
        return _itemConsume;
    }

    /**
     * @return Returns the itemConsumeId.
     */
    public final int getItemConsumeId()
    {
        return _itemConsumeId;
    }

    /**
     * @return Returns the itemConsume count over time.
     */
    public final int getItemConsumeOT()
    {
        return _itemConsumeOT;
    }

    /**
     * @return Returns the itemConsumeId over time.
     */
    public final int getItemConsumeIdOT()
    {
        return _itemConsumeIdOT;
    }

    /**
     * @return Returns the itemConsume count over time.
     */
    public final int getItemConsumeSteps()
    {
        return _itemConsumeSteps;
    }
    /**
     * @return Returns the itemConsume count over time.
     */
    public final int getTotalLifeTime()
    {
        return _summonTotalLifeTime;
    }
    /**
     * @return Returns the itemConsume count over time.
     */
    public final int getTimeLostIdle()
    {
        return _summonTimeLostIdle;
    }
    /**
     * @return Returns the itemConsumeId over time.
     */
    public final int getTimeLostActive()
    {
        return _summonTimeLostActive;
    }
    
    public final boolean isCubic()
    {
    	return _isCubic;
    }

    /**
     * @return Returns the itemConsume time in milliseconds.
     */
    public final int getItemConsumeTime()
    {
        return _itemConsumeTime;
    }

    /**
     * @return Returns the activation time for a cubic.
     */
    public final int getActivationTime()
    {
        return _activationtime;
    }

    /**
     * @return Returns the activation chance for a cubic.
     */
    public final int getActivationChance()
    {
        return _activationchance;
    }
    
    /**
     * @return Returns the level.
     */
    public final int getLevel()
    {
        return _level;
    }

    /**
     * @return Returns the magic.
     */
    public final boolean isMagic()
    {
        return _magic;
    }

    /**
     * @return Returns the mpConsume.
     */
    public final int getMpConsume()
    {
        return _mpConsume;
    }

    /**
     * @return Returns the mpInitialConsume.
     */
    public final int getMpInitialConsume()
    {
        return _mpInitialConsume;
    }

    /**
     * @return Returns the name.
     */
    public final String getName()
    {
        return _name;
    }

    /**
     * @return Returns the reuseDelay.
     */
    public final int getReuseDelay()
    {
        return _reuseDelay;
    }

    public final int getHitTime()
    {
        return _hitTime;
    }

    /**
     * @return Returns the coolTime.
     */
    public final int getCoolTime()
    {
        return _coolTime;
    }

    public final int getSkillRadius()
    {
        return _skillRadius;
    }

    public final boolean isActive()
    {
        return _operateType == SkillOpType.OP_ACTIVE;
    }

    public final boolean isPassive()
    {
        return _operateType == SkillOpType.OP_PASSIVE;
    }

    public final boolean isToggle()
    {
        return _operateType == SkillOpType.OP_TOGGLE;
    }

    public final boolean isChance()
    {
        return _operateType == SkillOpType.OP_CHANCE;
    }

    public ChanceCondition getChanceCondition()
    {
        return _chanceCondition;
    }

    public final boolean isDance()
    {
        return _isDance;
    }

    public final int getNextDanceMpCost()
    {
        return _nextDanceCost;
    }

    public final float getSSBoost()
    {
        return _sSBoost;
    }

    public final int getAggroPoints()
    {
        return _aggroPoints;
    }
    
    public final boolean useSoulShot()
    {
        return ((getSkillType() == SkillType.PDAM) || (getSkillType() == SkillType.STUN) || (getSkillType() == SkillType.CHARGEDAM));
    }

    public final boolean useSpiritShot()
    {
        return isMagic();
    }
    public final boolean useFishShot()
    {
        return ((getSkillType() == SkillType.PUMPING) || (getSkillType() == SkillType.REELING) );
    }
    public final int getWeaponsAllowed()
    {
        return _weaponsAllowed;
    }

    public final int getCrossLearnAdd()
    {
        return _addCrossLearn;
    }

    public final float getCrossLearnMul()
    {
        return _mulCrossLearn;
    }

    public final float getCrossLearnRace()
    {
        return _mulCrossLearnRace;
    }

    public final float getCrossLearnProf()
    {
        return _mulCrossLearnProf;
    }

    public final boolean getCanLearn(ClassId cls)
    {
        return _canLearn == null || _canLearn.contains(cls);
    }

    public final boolean canTeachBy(int npcId)
    {
        return _teachers == null || _teachers.contains(npcId);
    }

    public int getMinPledgeClass() { return _minPledgeClass;  }

    public final boolean isPvpSkill()
    {
        switch (_skillType)
        {
            case DOT:
            case BLEED:
            case CONFUSION:
            case POISON:
            case DEBUFF:
            case AGGDEBUFF:
            case STUN:
            case ROOT:
            case FEAR:
            case SLEEP:
            case MDOT:
            case MUTE:
            case WEAKNESS:
            case PARALYZE:
            case CANCEL:
            case MAGE_BANE:
            case WARRIOR_BANE:
            case BETRAY:
            case DISARM:
            case AGGDAMAGE:
                return true;
            default:
                return false;
        }
    }

    public final boolean isOffensive()
    {
        return _isOffensive;
    }

    public final boolean isHeroSkill()
    {
        return _isHeroSkill;
    }

    public final int getNumCharges()
    {
        return _numCharges;
    }

    public final int getNumSouls()
    {
        return _numSouls;
    }

    public final int getSoulConsumeCount()
    {
        return _soulConsume;
    }
    
    public final int getExpNeeded()
    {
        return _expNeeded;
    }

    public final int getCritChance()
    {
        return _critChance;
    }
    
    public final int getTransformId()
    {
        return _transformId;
    }
    
    public final int getTransformDuration()
    {
        return _transformDuration;
    }

    public final int getBaseCritRate()
    {
    	return _baseCritRate;
    }
    
    public final int getLethalChance1()
    {
    	return _lethalEffect1;
    }
    public final int getLethalChance2()
    {
    	return _lethalEffect2;
    }
    public final boolean getDmgDirectlyToHP()
    {
    	return _directHpDmg;
    }

    public final String getFlyType()
    {
    	return _flyType;
    }

    public final int getFlyRadius()
    {
    	return _flyRadius;
    }
    
    public final float getFlyCourse()
    {
    	return _flyCourse;
    }

    public final boolean isSkillTypeOffensive()
    {
        switch (_skillType)
        {
            case PDAM:
            case MDAM:
            case CPDAM:
            case DOT:
            case BLEED:
            case POISON:
            case AGGDAMAGE:
            case DEBUFF:
            case AGGDEBUFF:
            case STUN:
            case ROOT:
            case CONFUSION:
            case ERASE:
            case BLOW:
            case FEAR:
            case DRAIN:
            case SLEEP:
            case CHARGEDAM:
            case CONFUSE_MOB_ONLY:
            case DEATHLINK:
            case DETECT_WEAKNESS:
            case MANADAM:
            case MDOT:
            case MUTE:
            case SOULSHOT:
            case SPIRITSHOT:
            case SPOIL:
            case WEAKNESS:
            case MANA_BY_LEVEL:
            case SWEEP:
            case PARALYZE:
            case DRAIN_SOUL:
            case AGGREDUCE:
            case CANCEL:
            case MAGE_BANE:
            case WARRIOR_BANE:
            case AGGREMOVE:
            case AGGREDUCE_CHAR:
            case BETRAY:
            case DELUXE_KEY_UNLOCK:
            case SOW:
            case HARVEST:
            case DISARM:
                return true;
            default:
                return false;
        }
    }

    //	int weapons[] = {L2Weapon.WEAPON_TYPE_ETC, L2Weapon.WEAPON_TYPE_BOW,
    //	L2Weapon.WEAPON_TYPE_POLE, L2Weapon.WEAPON_TYPE_DUALFIST,
    //	L2Weapon.WEAPON_TYPE_DUAL, L2Weapon.WEAPON_TYPE_BLUNT,
    //	L2Weapon.WEAPON_TYPE_SWORD, L2Weapon.WEAPON_TYPE_DAGGER};

    public final boolean getWeaponDependancy(L2Character activeChar)
    {
        int weaponsAllowed = getWeaponsAllowed();
        //check to see if skill has a weapon dependency.
        if (weaponsAllowed == 0) return true;
        if (activeChar.getActiveWeaponItem() != null)
        {
            L2WeaponType playerWeapon;
            playerWeapon = activeChar.getActiveWeaponItem().getItemType();
            int mask = playerWeapon.mask();
            if ((mask & weaponsAllowed) != 0) return true;
            // can be on the secondary weapon
            if (activeChar.getSecondaryWeaponItem() != null)
            {
                playerWeapon = activeChar.getSecondaryWeaponItem().getItemType();
                mask = playerWeapon.mask();
                if ((mask & weaponsAllowed) != 0) return true;
            }
        }
        SystemMessage message = new SystemMessage(SystemMessageId.S1_CANNOT_BE_USED);
        message.addSkillName(this);
        activeChar.sendPacket(message);

        return false;
    }

    public boolean checkCondition(L2Character activeChar, L2Object target, boolean itemOrWeapon)
    {
        if ((getCondition() & L2Skill.COND_SHIELD) != 0)
        {
            /*
             L2Armor armorPiece;
             L2ItemInstance dummy;
             dummy = activeChar.getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);
             armorPiece = (L2Armor) dummy.getItem();
             */
            //TODO add checks for shield here.
        }

        Condition preCondition = _preCondition;
        if(itemOrWeapon) preCondition = _itemPreCondition;
        if (preCondition == null) return true;

        Env env = new Env();
        env.player = activeChar;
        if (target instanceof L2Character) // TODO: object or char?
        	env.target = (L2Character)target;
        env.skill = this;

        if (!preCondition.test(env))
        {
            String msg = preCondition.getMessage();
            if (msg != null)
            {
                activeChar.sendMessage(msg);
            }
            return false;
        }
        return true;
    }

    public final L2Object[] getTargetList(L2Character activeChar, boolean onlyFirst)
    {
        // Init to null the target of the skill
        L2Character target = null;

        // Get the L2Objcet targeted by the user of the skill at this moment
        L2Object objTarget = activeChar.getTarget();
        // If the L2Object targeted is a L2Character, it becomes the L2Character target
        if (objTarget instanceof L2Character)
        {
            target = (L2Character) objTarget;
        }

        return getTargetList(activeChar, onlyFirst, target);
    }

    /**
     * Return all targets of the skill in a table in function a the skill type.<BR><BR>
     *
     * <B><U> Values of skill type</U> :</B><BR><BR>
     * <li>ONE : The skill can only be used on the L2PcInstance targeted, or on the caster if it's a L2PcInstance and no L2PcInstance targeted</li>
     * <li>SELF</li>
     * <li>HOLY, UNDEAD</li>
     * <li>PET</li>
     * <li>AURA, AURA_CLOSE</li>
     * <li>AREA</li>
     * <li>MULTIFACE</li>
     * <li>PARTY, CLAN</li>
     * <li>CORPSE_PLAYER, CORPSE_MOB, CORPSE_CLAN</li>
     * <li>UNLOCKABLE</li>
     * <li>ITEM</li><BR><BR>
     *
     * @param activeChar The L2Character who use the skill
     *
     */
    public final L2Object[] getTargetList(L2Character activeChar, boolean onlyFirst, L2Character target)
    {
        List<L2Character> targetList = new FastList<L2Character>();

        // Get the target type of the skill
        // (ex : ONE, SELF, HOLY, PET, AURA, AURA_CLOSE, AREA, MULTIFACE, PARTY, CLAN, CORPSE_PLAYER, CORPSE_MOB, CORPSE_CLAN, UNLOCKABLE, ITEM, UNDEAD)
        SkillTargetType targetType = getTargetType();

        // Get the type of the skill
        // (ex : PDAM, MDAM, DOT, BLEED, POISON, HEAL, HOT, MANAHEAL, MANARECHARGE, AGGDAMAGE, BUFF, DEBUFF, STUN, ROOT, RESURRECT, PASSIVE...)
        SkillType skillType = getSkillType();

        switch (targetType)
        {
            // The skill can only be used on the L2Character targeted, or on the caster itself
            case TARGET_ONE:
            {
                boolean canTargetSelf = false;
                switch (skillType)
				{
					case BUFF:
					case HEAL:
					case HOT:
					case HEAL_PERCENT:
					case MANARECHARGE:
					case MANAHEAL:
					case NEGATE:
					case CANCEL:
					case CANCEL_DEBUFF:
					case REFLECT:
					case UNBLEED:
					case UNPOISON:
					case COMBATPOINTHEAL:
					case MAGE_BANE:
					case WARRIOR_BANE:
					case BETRAY:
					case BALANCE_LIFE:
					case FORCE_BUFF:
						canTargetSelf = true;
						break;
				}

                // Check for null target or any other invalid target
                if (target == null || target.isDead() || (target == activeChar && !canTargetSelf))
                {
                    activeChar.sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
                    return null;
                }

                // If a target is found, return it in a table else send a system message TARGET_IS_INCORRECT
                return new L2Character[] {target};
            }
            case TARGET_SELF:
            case TARGET_GROUND:
            {
                return new L2Character[] {activeChar};
            }
            case TARGET_HOLY:
            {
                if (activeChar instanceof L2PcInstance)
                {
                    if (target instanceof L2ArtefactInstance)
                        return new L2Character[] {target};
                }

                return null;
            }
            case TARGET_FLAGPOLE:
            {
                return new L2Character[] {activeChar};
            }
            case TARGET_PET:
            {
                target = activeChar.getPet();
                if (target != null && !target.isDead()) return new L2Character[] {target};

                return null;
            }
			case TARGET_OWNER_PET:
			{
				if (activeChar instanceof L2Summon)
				{
					target = ((L2Summon)activeChar).getOwner();
					if (target != null && !target.isDead())
						return new L2Character[]{target};
				}

				return null;
			}
			case TARGET_CORPSE_PET:
			{
				if (activeChar instanceof L2PcInstance)
				{
					target = activeChar.getPet();
					if (target != null && target.isDead())
						return new L2Character[]{target};
				}

				return null;
			}
            case TARGET_AURA:
            {
                int radius = getSkillRadius();
                boolean srcInArena = (activeChar.isInsideZone(L2Character.ZONE_PVP) && !activeChar.isInsideZone(L2Character.ZONE_SIEGE));

                L2PcInstance src = null;
                if (activeChar instanceof L2PcInstance) src = (L2PcInstance)activeChar;
                else if (activeChar instanceof L2Summon) src = ((L2Summon)activeChar).getOwner();
                else if (activeChar instanceof L2Decoy) src = ((L2Decoy)activeChar).getOwner();
                else if (activeChar instanceof L2Trap) src = ((L2Trap)activeChar).getOwner();

                // Go through the L2Character _knownList
                Collection<L2Object> objs = activeChar.getKnownList().getKnownObjects().values();
                //synchronized (activeChar.getKnownList().getKnownObjects())
				{
					for (L2Object obj : objs)
					{
						if (obj instanceof L2Attackable
						        || obj instanceof L2PlayableInstance)
						{
							// Don't add this target if this is a Pc->Pc pvp
							// casting and pvp condition not met
							if (obj == activeChar || obj == src
							        || ((L2Character) obj).isDead())
								continue;
							if (src != null)
							{
								if (!GeoData.getInstance().canSeeTarget(activeChar, obj))
									continue;
								// check if both attacker and target are
								// L2PcInstances and if they are in same party
								if (obj instanceof L2PcInstance)
								{
									if (!src.checkPvpSkill(obj, this))
										continue;
									if ((src.getParty() != null && ((L2PcInstance) obj).getParty() != null)
									        && src.getParty().getPartyLeaderOID() == ((L2PcInstance) obj).getParty().getPartyLeaderOID())
										continue;
									if (!srcInArena
									        && !(((L2Character) obj).isInsideZone(L2Character.ZONE_PVP) && !((L2Character) obj).isInsideZone(L2Character.ZONE_SIEGE)))
									{
										if (src.getAllyId() == ((L2PcInstance) obj).getAllyId()
										        && src.getAllyId() != 0)
											continue;
										
										if (src.getClanId() != 0
										        && src.getClanId() == ((L2PcInstance) obj).getClanId())
											continue;
									}
								}
								if (obj instanceof L2Summon)
								{
									L2PcInstance trg = ((L2Summon) obj).getOwner();
									if (trg == src)
										continue;
									if (!src.checkPvpSkill(trg, this))
										continue;
									if ((src.getParty() != null && trg.getParty() != null)
									        && src.getParty().getPartyLeaderOID() == trg.getParty().getPartyLeaderOID())
										continue;
									if (!srcInArena
									        && !(((L2Character) obj).isInsideZone(L2Character.ZONE_PVP) && !((L2Character) obj).isInsideZone(L2Character.ZONE_SIEGE)))
									{
										if (src.getAllyId() == trg.getAllyId()
										        && src.getAllyId() != 0)
											continue;
										
										if (src.getClanId() != 0
										        && src.getClanId() == trg.getClanId())
											continue;
									}
								}
							}
							else
							// Skill user is not L2PlayableInstance
							{
								if (!(obj instanceof L2PlayableInstance) // Target
																			// is
																			// not
																			// L2PlayableInstance
								        && !activeChar.isConfused()) // and
																		// caster
																		// not
																		// confused
																		// (?)
									continue;
							}
							if (!Util.checkIfInRange(radius, activeChar, obj, true))
								continue;
							
							if (onlyFirst == false)
								targetList.add((L2Character) obj);
							else
								return new L2Character[] { (L2Character) obj };
						}
					}
				}
                return targetList.toArray(new L2Character[targetList.size()]);
            }
            case TARGET_FRONT_AURA:
            {
                int radius = getSkillRadius();
                boolean srcInArena = (activeChar.isInsideZone(L2Character.ZONE_PVP) && !activeChar.isInsideZone(L2Character.ZONE_SIEGE));

                L2PcInstance src = null;
                if (activeChar instanceof L2PcInstance) src = (L2PcInstance)activeChar;
                else if (activeChar instanceof L2Summon) src = ((L2Summon)activeChar).getOwner();
                else if (activeChar instanceof L2Trap) src = ((L2Trap)activeChar).getOwner();
                
                // Go through the L2Character _knownList
                Collection<L2Object> objs = activeChar.getKnownList().getKnownObjects().values();
                //synchronized (activeChar.getKnownList().getKnownObjects())
				{
					for (L2Object obj : objs)
					{
						if (obj instanceof L2Attackable
						        || obj instanceof L2PlayableInstance)
						{
							// Don't add this target if this is a Pc->Pc pvp
							// casting and pvp condition not met
							if (obj == activeChar || obj == src
							        || ((L2Character) obj).isDead())
								continue;
							if (src != null)
							{
								if (!((L2Character) obj).isInFrontOf(activeChar))
									continue;
								
								if (!GeoData.getInstance().canSeeTarget(activeChar, obj))
									continue;
								
								// check if both attacker and target are
								// L2PcInstances and if they are in same party
								if (obj instanceof L2PcInstance)
								{
									if (!src.checkPvpSkill(obj, this))
										continue;
									if ((src.getParty() != null && ((L2PcInstance) obj).getParty() != null)
									        && src.getParty().getPartyLeaderOID() == ((L2PcInstance) obj).getParty().getPartyLeaderOID())
										continue;
									if (!srcInArena
									        && !(((L2Character) obj).isInsideZone(L2Character.ZONE_PVP) && !((L2Character) obj).isInsideZone(L2Character.ZONE_SIEGE)))
									{
										if (src.getAllyId() == ((L2PcInstance) obj).getAllyId()
										        && src.getAllyId() != 0)
											continue;
										
										if (src.getClanId() != 0
										        && src.getClanId() == ((L2PcInstance) obj).getClanId())
											continue;
									}
								}
								if (obj instanceof L2Summon)
								{
									L2PcInstance trg = ((L2Summon) obj).getOwner();
									if (trg == src)
										continue;
									if (!src.checkPvpSkill(trg, this))
										continue;
									if ((src.getParty() != null && trg.getParty() != null)
									        && src.getParty().getPartyLeaderOID() == trg.getParty().getPartyLeaderOID())
										continue;
									if (!srcInArena
									        && !(((L2Character) obj).isInsideZone(L2Character.ZONE_PVP) && !((L2Character) obj).isInsideZone(L2Character.ZONE_SIEGE)))
									{
										if (src.getAllyId() == trg.getAllyId()
										        && src.getAllyId() != 0)
											continue;
										
										if (src.getClanId() != 0
										        && src.getClanId() == trg.getClanId())
											continue;
									}
								}
							}
							else
							// Skill user is not L2PlayableInstance
							{
								if (!(obj instanceof L2PlayableInstance) // Target
																			// is
																			// not
																			// L2PlayableInstance
								        && !activeChar.isConfused()) // and
																		// caster
																		// not
																		// confused
																		// (?)
									continue;
							}
							if (!Util.checkIfInRange(radius, activeChar, obj, true))
								continue;
							
							if (onlyFirst == false)
								targetList.add((L2Character) obj);
							else
								return new L2Character[] { (L2Character) obj };
						}
					}
				}
                return targetList.toArray(new L2Character[targetList.size()]);
            }
            case TARGET_BEHIND_AURA:
            {
                int radius = getSkillRadius();
                boolean srcInArena = (activeChar.isInsideZone(L2Character.ZONE_PVP) && !activeChar.isInsideZone(L2Character.ZONE_SIEGE));

                L2PcInstance src = null;
                if (activeChar instanceof L2PcInstance) src = (L2PcInstance)activeChar;
                else if (activeChar instanceof L2Summon) src = ((L2Summon)activeChar).getOwner();
                else if (activeChar instanceof L2Trap) src = ((L2Trap)activeChar).getOwner();
                
                // Go through the L2Character _knownList
                Collection<L2Object> objs = activeChar.getKnownList().getKnownObjects().values();
                //synchronized (activeChar.getKnownList().getKnownObjects())
				{
					for (L2Object obj : objs)
					{
						if (obj instanceof L2Attackable
						        || obj instanceof L2PlayableInstance)
						{
							// Don't add this target if this is a Pc->Pc pvp
							// casting and pvp condition not met
							if (obj == activeChar || obj == src
							        || ((L2Character) obj).isDead())
								continue;
							if (src != null)
							{
								if (!((L2Character) obj).isBehind(activeChar))
									continue;
								
								if (!GeoData.getInstance().canSeeTarget(activeChar, obj))
									continue;
								
								// check if both attacker and target are
								// L2PcInstances and if they are in same party
								if (obj instanceof L2PcInstance)
								{
									if (!src.checkPvpSkill(obj, this))
										continue;
									if ((src.getParty() != null && ((L2PcInstance) obj).getParty() != null)
									        && src.getParty().getPartyLeaderOID() == ((L2PcInstance) obj).getParty().getPartyLeaderOID())
										continue;
									if (!srcInArena
									        && !(((L2Character) obj).isInsideZone(L2Character.ZONE_PVP) && !((L2Character) obj).isInsideZone(L2Character.ZONE_SIEGE)))
									{
										if (src.getAllyId() == ((L2PcInstance) obj).getAllyId()
										        && src.getAllyId() != 0)
											continue;
										
										if (src.getClanId() != 0
										        && src.getClanId() == ((L2PcInstance) obj).getClanId())
											continue;
									}
								}
								if (obj instanceof L2Summon)
								{
									L2PcInstance trg = ((L2Summon) obj).getOwner();
									if (trg == src)
										continue;
									if (!src.checkPvpSkill(trg, this))
										continue;
									if ((src.getParty() != null && trg.getParty() != null)
									        && src.getParty().getPartyLeaderOID() == trg.getParty().getPartyLeaderOID())
										continue;
									if (!srcInArena
									        && !(((L2Character) obj).isInsideZone(L2Character.ZONE_PVP) && !((L2Character) obj).isInsideZone(L2Character.ZONE_SIEGE)))
									{
										if (src.getAllyId() == trg.getAllyId()
										        && src.getAllyId() != 0)
											continue;
										
										if (src.getClanId() != 0
										        && src.getClanId() == trg.getClanId())
											continue;
									}
								}
							}
							else
							// Skill user is not L2PlayableInstance
							{
								if (!(obj instanceof L2PlayableInstance) // Target
																			// is
																			// not
																			// L2PlayableInstance
								        && !activeChar.isConfused()) // and
																		// caster
																		// not
																		// confused
																		// (?)
									continue;
							}
							if (!Util.checkIfInRange(radius, activeChar, obj, true))
								continue;
							
							if (onlyFirst == false)
								targetList.add((L2Character) obj);
							else
								return new L2Character[] { (L2Character) obj };
						}
					}
				}
                return targetList.toArray(new L2Character[targetList.size()]);
            }
            case TARGET_AREA:
            {
                if ((!(target instanceof L2Attackable || target instanceof L2PlayableInstance)) ||  // Target
																									// is
																									// not
																									// L2Attackable
																									// or
																									// L2PlayableInstance
                    (getCastRange() >= 0 && (target == null || target == activeChar || target.isAlikeDead()))) // target
																												// is
																												// null
																												// or
																												// self
																												// or
																												// dead/faking
                {
                    activeChar.sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
                    return null;
                }

                L2Character cha;

                if (getCastRange() >= 0)
                {
                    cha = target;

                    if(!onlyFirst) targetList.add(cha); // Add target to target list
                    else return new L2Character[]{cha};
                }
                else cha = activeChar;

                boolean effectOriginIsL2PlayableInstance = (cha instanceof L2PlayableInstance);

                L2PcInstance src = null;
                if (activeChar instanceof L2PcInstance) src = (L2PcInstance)activeChar;
                else if (activeChar instanceof L2Summon) src = ((L2Summon)activeChar).getOwner();

                int radius = getSkillRadius();

                boolean srcInArena = (activeChar.isInsideZone(L2Character.ZONE_PVP) && !activeChar.isInsideZone(L2Character.ZONE_SIEGE));

                Collection<L2Object> objs = activeChar.getKnownList().getKnownObjects().values();
                //synchronized (activeChar.getKnownList().getKnownObjects())
				{
					for (L2Object obj : objs)
					{
						if (!(obj instanceof L2Attackable || obj instanceof L2PlayableInstance))
							continue;
						if (obj == cha)
							continue;
						target = (L2Character) obj;
						
						if (!GeoData.getInstance().canSeeTarget(activeChar, target))
							continue;
						
						if (!target.isDead() && (target != activeChar))
						{
							if (!Util.checkIfInRange(radius, obj, cha, true))
								continue;
							
							if (src != null) // caster is l2playableinstance
												// and exists
							{
								
								if (obj instanceof L2PcInstance)
								{
									L2PcInstance trg = (L2PcInstance) obj;
									if (trg == src)
										continue;
									if ((src.getParty() != null && trg.getParty() != null)
									        && src.getParty().getPartyLeaderOID() == trg.getParty().getPartyLeaderOID())
										continue;
									
									if (trg.isInsideZone(L2Character.ZONE_PEACE))
										continue;
									
									if (!srcInArena
									        && !(trg.isInsideZone(L2Character.ZONE_PVP) && !trg.isInsideZone(L2Character.ZONE_SIEGE)))
									{
										if (src.getAllyId() == trg.getAllyId()
										        && src.getAllyId() != 0)
											continue;
										
										if (src.getClan() != null
										        && trg.getClan() != null)
										{
											if (src.getClan().getClanId() == trg.getClan().getClanId())
												continue;
										}
										
										if (!src.checkPvpSkill(obj, this))
											continue;
									}
								}
								if (obj instanceof L2Summon)
								{
									L2PcInstance trg = ((L2Summon) obj).getOwner();
									if (trg == src)
										continue;
									
									if ((src.getParty() != null && trg.getParty() != null)
									        && src.getParty().getPartyLeaderOID() == trg.getParty().getPartyLeaderOID())
										continue;
									
									if (!srcInArena
									        && !(trg.isInsideZone(L2Character.ZONE_PVP) && !trg.isInsideZone(L2Character.ZONE_SIEGE)))
									{
										if (src.getAllyId() == trg.getAllyId()
										        && src.getAllyId() != 0)
											continue;
										
										if (src.getClan() != null
										        && trg.getClan() != null)
										{
											if (src.getClan().getClanId() == trg.getClan().getClanId())
												continue;
										}
										
										if (!src.checkPvpSkill(trg, this))
											continue;
									}
									
									if (((L2Summon) obj).isInsideZone(L2Character.ZONE_PEACE))
										continue;
								}
							}
							else
							// Skill user is not L2PlayableInstance
							{
								if (effectOriginIsL2PlayableInstance && // If
																		// effect
																		// starts
																		// at
																		// L2PlayableInstance
																		// and
								        !(obj instanceof L2PlayableInstance)) // Object
																				// is
																				// not
																				// L2PlayableInstance
									continue;
							}
							
							targetList.add((L2Character) obj);
						}
					}
				}

                if (targetList.size() == 0)
                    return null;

                return targetList.toArray(new L2Character[targetList.size()]);
            }
            case TARGET_FRONT_AREA:
            {
                if ((!(target instanceof L2Attackable || target instanceof L2PlayableInstance)) ||  // Target
																									// is
																									// not
																									// L2Attackable
																									// or
																									// L2PlayableInstance
                    (getCastRange() >= 0 && (target == null || target == activeChar || target.isAlikeDead()))) // target
																												// is
																												// null
																												// or
																												// self
																												// or
																												// dead/faking
                {
                    activeChar.sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
                    return null;
                }

                L2Character cha;

                if (getCastRange() >= 0)
                {
                    cha = target;

                    if(!onlyFirst) targetList.add(cha); // Add target to target
														// list
                    else return new L2Character[]{cha};
                }
                else cha = activeChar;

                boolean effectOriginIsL2PlayableInstance = (cha instanceof L2PlayableInstance);

                L2PcInstance src = null;
                if (activeChar instanceof L2PcInstance) src = (L2PcInstance)activeChar;
                else if (activeChar instanceof L2Summon) src = ((L2Summon)activeChar).getOwner();

                int radius = getSkillRadius();

                boolean srcInArena = (activeChar.isInsideZone(L2Character.ZONE_PVP) && !activeChar.isInsideZone(L2Character.ZONE_SIEGE));

                Collection<L2Object> objs = activeChar.getKnownList().getKnownObjects().values();
                //synchronized (activeChar.getKnownList().getKnownObjects())
				{
					for (L2Object obj : objs)
					{
						if (obj == cha)
							continue;
						
						if (!(obj instanceof L2Attackable || obj instanceof L2PlayableInstance))
							continue;
						
						target = (L2Character) obj;
						
						if (!target.isDead() && (target != activeChar))
						{
							if (!Util.checkIfInRange(radius, obj, activeChar, true))
								continue;
							
							if (!((L2Character) obj).isInFrontOf(activeChar))
								continue;
							
							if (!GeoData.getInstance().canSeeTarget(activeChar, obj))
								continue;
							
							if (src != null) // caster is l2playableinstance
												// and exists
							{
								if (obj instanceof L2PcInstance)
								{
									L2PcInstance trg = (L2PcInstance) obj;
									if (trg == src)
										continue;
									if ((src.getParty() != null && trg.getParty() != null)
									        && src.getParty().getPartyLeaderOID() == trg.getParty().getPartyLeaderOID())
										continue;
									
									if (trg.isInsideZone(L2Character.ZONE_PEACE))
										continue;
									
									if (!srcInArena
									        && !(trg.isInsideZone(L2Character.ZONE_PVP) && !trg.isInsideZone(L2Character.ZONE_SIEGE)))
									{
										if (src.getAllyId() == trg.getAllyId()
										        && src.getAllyId() != 0)
											continue;
										
										if (src.getClan() != null
										        && trg.getClan() != null)
										{
											if (src.getClan().getClanId() == trg.getClan().getClanId())
												continue;
										}
										
										if (!src.checkPvpSkill(obj, this))
											continue;
									}
								}
								if (obj instanceof L2Summon)
								{
									L2PcInstance trg = ((L2Summon) obj).getOwner();
									if (trg == src)
										continue;
									
									if ((src.getParty() != null && trg.getParty() != null)
									        && src.getParty().getPartyLeaderOID() == trg.getParty().getPartyLeaderOID())
										continue;
									
									if (!srcInArena
									        && !(trg.isInsideZone(L2Character.ZONE_PVP) && !trg.isInsideZone(L2Character.ZONE_SIEGE)))
									{
										if (src.getAllyId() == trg.getAllyId()
										        && src.getAllyId() != 0)
											continue;
										
										if (src.getClan() != null
										        && trg.getClan() != null)
										{
											if (src.getClan().getClanId() == trg.getClan().getClanId())
												continue;
										}
										
										if (!src.checkPvpSkill(trg, this))
											continue;
									}
									
									if (((L2Summon) obj).isInsideZone(L2Character.ZONE_PEACE))
										continue;
								}
							}
							else
							// Skill user is not L2PlayableInstance
							{
								if (effectOriginIsL2PlayableInstance && // If
																		// effect
																		// starts
																		// at
																		// L2PlayableInstance
																		// and
								        !(obj instanceof L2PlayableInstance)) // Object
																				// is
																				// not
																				// L2PlayableInstance
									continue;
							}
							
							targetList.add((L2Character) obj);
						}
					}
				}

                if (targetList.size() == 0)
                    return null;

                return targetList.toArray(new L2Character[targetList.size()]);
            }
            case TARGET_BEHIND_AREA:
            {
                if ((!(target instanceof L2Attackable || target instanceof L2PlayableInstance)) ||  // Target
																									// is
																									// not
																									// L2Attackable
																									// or
																									// L2PlayableInstance
                    (getCastRange() >= 0 && (target == null || target == activeChar || target.isAlikeDead()))) // target
																												// is
																												// null
																												// or
																												// self
																												// or
																												// dead/faking
                {
                    activeChar.sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
                    return null;
                }

                L2Character cha;

                if (getCastRange() >= 0)
                {
                    cha = target;

                    if(!onlyFirst) targetList.add(cha); // Add target to target
														// list
                    else return new L2Character[]{cha};
                }
                else cha = activeChar;

                boolean effectOriginIsL2PlayableInstance = (cha instanceof L2PlayableInstance);

                L2PcInstance src = null;
                if (activeChar instanceof L2PcInstance) src = (L2PcInstance)activeChar;
                else if (activeChar instanceof L2Summon) src = ((L2Summon)activeChar).getOwner();

                int radius = getSkillRadius();

                boolean srcInArena = (activeChar.isInsideZone(L2Character.ZONE_PVP) && !activeChar.isInsideZone(L2Character.ZONE_SIEGE));

                Collection<L2Object> objs = activeChar.getKnownList().getKnownObjects().values();
                //synchronized (activeChar.getKnownList().getKnownObjects())
				{
					for (L2Object obj : objs)
					{
						if (!(obj instanceof L2Attackable || obj instanceof L2PlayableInstance))
							continue;
						if (obj == cha)
							continue;
						target = (L2Character) obj;
						
						if (!target.isDead() && (target != activeChar))
						{
							if (!Util.checkIfInRange(radius, obj, activeChar, true))
								continue;
							
							if (!((L2Character) obj).isBehind(activeChar))
								continue;
							
							if (!GeoData.getInstance().canSeeTarget(activeChar, obj))
								continue;
							
							if (src != null) // caster is l2playableinstance
												// and exists
							{
								if (obj instanceof L2PcInstance)
								{
									L2PcInstance trg = (L2PcInstance) obj;
									if (trg == src)
										continue;
									if ((src.getParty() != null && trg.getParty() != null)
									        && src.getParty().getPartyLeaderOID() == trg.getParty().getPartyLeaderOID())
										continue;
									
									if (trg.isInsideZone(L2Character.ZONE_PEACE))
										continue;
									
									if (!srcInArena
									        && !(trg.isInsideZone(L2Character.ZONE_PVP) && !trg.isInsideZone(L2Character.ZONE_SIEGE)))
									{
										if (src.getAllyId() == trg.getAllyId()
										        && src.getAllyId() != 0)
											continue;
										
										if (src.getClan() != null
										        && trg.getClan() != null)
										{
											if (src.getClan().getClanId() == trg.getClan().getClanId())
												continue;
										}
										
										if (!src.checkPvpSkill(obj, this))
											continue;
									}
								}
								if (obj instanceof L2Summon)
								{
									L2PcInstance trg = ((L2Summon) obj).getOwner();
									if (trg == src)
										continue;
									
									if ((src.getParty() != null && trg.getParty() != null)
									        && src.getParty().getPartyLeaderOID() == trg.getParty().getPartyLeaderOID())
										continue;
									
									if (!srcInArena
									        && !(trg.isInsideZone(L2Character.ZONE_PVP) && !trg.isInsideZone(L2Character.ZONE_SIEGE)))
									{
										if (src.getAllyId() == trg.getAllyId()
										        && src.getAllyId() != 0)
											continue;
										
										if (src.getClan() != null
										        && trg.getClan() != null)
										{
											if (src.getClan().getClanId() == trg.getClan().getClanId())
												continue;
										}
										
										if (!src.checkPvpSkill(trg, this))
											continue;
									}
									
									if (((L2Summon) obj).isInsideZone(L2Character.ZONE_PEACE))
										continue;
								}
							}
							else
							// Skill user is not L2PlayableInstance
							{
								if (effectOriginIsL2PlayableInstance && // If
																		// effect
																		// starts
																		// at
																		// L2PlayableInstance
																		// and
								        !(obj instanceof L2PlayableInstance)) // Object
																				// is
																				// not
																				// L2PlayableInstance
									continue;
							}
							
							targetList.add((L2Character) obj);
						}
					}
				}

                if (targetList.size() == 0)
                    return null;

                return targetList.toArray(new L2Character[targetList.size()]);
            }
            case TARGET_MULTIFACE:
            {
                if ((!(target instanceof L2Attackable) && !(target instanceof L2PcInstance)))
                {
                    activeChar.sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
                    return null;
                }

                if (onlyFirst == false) targetList.add(target);
                else return new L2Character[] {target};

                int radius = getSkillRadius();

                Collection<L2Object> objs = activeChar.getKnownList().getKnownObjects().values();
                //synchronized (activeChar.getKnownList().getKnownObjects())
				{
					for (L2Object obj : objs)
					{
						if (!Util.checkIfInRange(radius, activeChar, obj, true))
							continue;
						
						if (obj instanceof L2Attackable && obj != target)
							targetList.add((L2Character) obj);
						
						if (targetList.size() == 0)
						{
							activeChar.sendPacket(new SystemMessage(SystemMessageId.TARGET_CANT_FOUND));
							return null;
						}
					}
				}
                return targetList.toArray(new L2Character[targetList.size()]);
                //TODO multiface targets all around right now.  need it to just get targets
                //the character is facing.
            }
			case TARGET_PARTY:
			{
				if (onlyFirst)
                    return new L2Character[]{activeChar};

                targetList.add(activeChar);

                L2PcInstance player = null;

                if (activeChar instanceof L2Summon)
                {
                	player = ((L2Summon)activeChar).getOwner();
                    targetList.add(player);
                }
                else if (activeChar instanceof L2PcInstance)
                {
                	player = (L2PcInstance)activeChar;
                	if (activeChar.getPet() != null)
                		targetList.add(activeChar.getPet());
                }

				if (activeChar.getParty() != null)
				{
                    // Get all visible objects in a spherical area near the L2Character
					// Get a list of Party Members
					List<L2PcInstance> partyList = activeChar.getParty().getPartyMembers();

					for(L2PcInstance partyMember : partyList)
					{
						if (partyMember == null) continue;
						if (partyMember == player) continue;

						if (!partyMember.isDead()
								&& Util.checkIfInRange(getSkillRadius(), activeChar, partyMember, true))
						{
							targetList.add(partyMember);

							if (partyMember.getPet() != null && !partyMember.getPet().isDead())
		                    {
		                        targetList.add(partyMember.getPet());
		                    }
						}
					}
				}
				return targetList.toArray(new L2Character[targetList.size()]);
			}
			case TARGET_PARTY_MEMBER:
			{
				if ((target != null
						&& target == activeChar)
					|| (target != null
							&& activeChar.getParty() != null
							&& target.getParty() != null
							&& activeChar.getParty().getPartyLeaderOID() == target.getParty().getPartyLeaderOID())
					|| (target != null
							&& activeChar instanceof L2PcInstance
							&& target instanceof L2Summon
							&& activeChar.getPet() == target)
					|| (target != null
							&& activeChar instanceof L2Summon
							&& target instanceof L2PcInstance
							&& activeChar == target.getPet()))
				{
					if (!target.isDead())
					{
						// If a target is found, return it in a table else send a system message TARGET_IS_INCORRECT
						return new L2Character[]{target};
					}
					else
						return null;
				}
				else
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
					return null;
				}
			}
			case TARGET_PARTY_OTHER:
            {
                if (target != null && target != activeChar
                        && activeChar.getParty() != null && target.getParty() != null
                        && activeChar.getParty().getPartyLeaderOID() == target.getParty().getPartyLeaderOID())
                {
                    if (!target.isDead())
                    {
                        if (target instanceof L2PcInstance)
                        {
                            L2PcInstance player = (L2PcInstance)target;
                            switch (getId())
                            {
                            	// FORCE BUFFS may cancel here but there should be a proper condition
                            	case 426: 
                                    if (!player.isMageClass())
                                        return new L2Character[]{target};
                                    else
                                        return null;
                                case 427:
                                    if (player.isMageClass())
                                        return new L2Character[]{target};
                                    else
                                        return null;
                            }
                        }
                        return new L2Character[]{target};
                    }
                    else
                        return null;
                }
                else
                {
                    activeChar.sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
                    return null;
                }
            }
            case TARGET_CORPSE_ALLY:
            case TARGET_ALLY:
            {
                if (activeChar instanceof L2PcInstance)
                {
                    int radius = getSkillRadius();
                    L2PcInstance player = (L2PcInstance) activeChar;
                    L2Clan clan = player.getClan();

                    if (player.isInOlympiadMode())
                    	return new L2Character[] {player};

                    if (targetType != SkillTargetType.TARGET_CORPSE_ALLY)
                    {
                        if (onlyFirst == false) targetList.add(player);
                        else return new L2Character[] {player};
                    }
                    
                    if (activeChar.getPet() != null)
                    {
                    	if ((targetType != SkillTargetType.TARGET_CORPSE_ALLY) && !(activeChar.getPet().isDead()))
                    		targetList.add(activeChar.getPet());
                    }

                    if (clan != null)
                    {
                        // Get all visible objects in a spherical area near the L2Character
                        // Get Clan Members
                    	Collection<L2Object> objs = activeChar.getKnownList().getKnownObjects().values();
                    	//synchronized (activeChar.getKnownList().getKnownObjects())
						{
							for (L2Object newTarget : objs)
							{
								if (!(newTarget instanceof L2PcInstance))
									continue;
								if ((((L2PcInstance) newTarget).getAllyId() == 0 || ((L2PcInstance) newTarget).getAllyId() != player.getAllyId())
								        && (((L2PcInstance) newTarget).getClan() == null || ((L2PcInstance) newTarget).getClanId() != player.getClanId()))
									continue;
								if (player.isInDuel()
								        && (player.getDuelId() != ((L2PcInstance) newTarget).getDuelId() || (player.getParty() != null && !player.getParty().getPartyMembers().contains(newTarget))))
									continue;
								
								if (((L2PcInstance) newTarget).getPet() != null)
									if (Util.checkIfInRange(radius, activeChar, ((L2PcInstance) newTarget).getPet(), true))
										if ((targetType != SkillTargetType.TARGET_CORPSE_ALLY)
												&& !(((L2PcInstance) newTarget).getPet().isDead())
												&& player.checkPvpSkill(newTarget, this)
												&& onlyFirst == false)
											targetList.add(((L2PcInstance) newTarget).getPet());

								if (targetType == SkillTargetType.TARGET_CORPSE_ALLY)
								{
									if (!((L2PcInstance) newTarget).isDead())
										continue;
									if (getSkillType() == SkillType.RESURRECT)
									{
										// check target is not in a active siege
										// zone
										if (((L2PcInstance) newTarget).isInsideZone(L2Character.ZONE_SIEGE))
											continue;
									}
								}
								
								if (!Util.checkIfInRange(radius, activeChar, newTarget, true))
									continue;
								
								// Don't add this target if this is a Pc->Pc pvp
								// casting and pvp condition not met
								if (!player.checkPvpSkill(newTarget, this))
									continue;
								
								if (onlyFirst == false) targetList.add((L2Character) newTarget);
								else return new L2Character[] { (L2Character) newTarget };
								
							}
						}
                    }
                }
                return targetList.toArray(new L2Character[targetList.size()]);
            }
            case TARGET_CORPSE_CLAN:
            case TARGET_CLAN:
            {
                if (activeChar instanceof L2PlayableInstance)
                {
                    int radius = getSkillRadius();
                    L2PcInstance player = null;
                    if (activeChar instanceof L2Summon) 
                    	player = ((L2Summon)activeChar).getOwner();
                    else
                    	player = (L2PcInstance) activeChar;
                    if (player == null) return null;
                    L2Clan clan = player.getClan();

                    if (player.isInOlympiadMode())
                    	return new L2Character[] {player};

                    if (targetType != SkillTargetType.TARGET_CORPSE_CLAN)
                    {
                        if (onlyFirst == false) targetList.add(player);
                        else return new L2Character[] {player};
                    }

                    if (activeChar.getPet() != null)
                    {
                    	if ((targetType != SkillTargetType.TARGET_CORPSE_CLAN) && !(activeChar.getPet().isDead()))
                    		targetList.add(activeChar.getPet());
                    }

                    if (clan != null)
                    {
                        // Get all visible objects in a spheric area near the L2Character
                        // Get Clan Members
                        for (L2ClanMember member : clan.getMembers())
                        {
                            L2PcInstance newTarget = member.getPlayerInstance();

                            if (newTarget == null || newTarget == player) continue;

                            if (player.isInDuel() && (player.getDuelId() != newTarget.getDuelId() || (player.getParty() != null 
                            		&& !player.getParty().getPartyMembers().contains(newTarget))))
                            	continue;
                            
                        	if (newTarget.getPet() != null)
                        		if (Util.checkIfInRange(radius, activeChar, newTarget.getPet(), true))
                        			if ((targetType != SkillTargetType.TARGET_CORPSE_CLAN) && !(newTarget.getPet().isDead())
                        					&& player.checkPvpSkill(newTarget, this)
									        && onlyFirst == false)
                        				targetList.add(newTarget.getPet());

                            if (targetType == SkillTargetType.TARGET_CORPSE_CLAN)
                            {
                            	if (!newTarget.isDead())
                            		continue;
                            	if (getSkillType() == SkillType.RESURRECT)
                            	{
                            		// check target is not in a active siege zone
                                 	if (newTarget.isInsideZone(L2Character.ZONE_SIEGE))
                                 		continue;
                            	}
                            }
                            
                            if (!Util.checkIfInRange(radius, activeChar, newTarget, true)) continue;

                            // Don't add this target if this is a Pc->Pc pvp casting and pvp condition not met
                            if (!player.checkPvpSkill(newTarget, this)) continue;

                            if (onlyFirst == false) targetList.add(newTarget);
                            else return new L2Character[] {newTarget};

                        }
                    }
                }
                else if (activeChar instanceof L2NpcInstance)
                {
                	// for buff purposes, returns one unbuffed friendly mob nearby or mob itself?
                	L2NpcInstance npc = (L2NpcInstance) activeChar;
                	Collection<L2Object> objs = activeChar.getKnownList().getKnownObjects().values();
                	//synchronized (activeChar.getKnownList().getKnownObjects())
					{
						for (L2Object newTarget : objs)
						{
							if (newTarget instanceof L2NpcInstance
							        && ((L2NpcInstance) newTarget).getFactionId() == npc.getFactionId())
							{
								if (!Util.checkIfInRange(getCastRange(), activeChar, newTarget, true))
									continue;
								if (((L2NpcInstance) newTarget).getFirstEffect(this) != null)
								{
									targetList.add((L2NpcInstance) newTarget);
									break;
								}
							}
						}
					}
                	if (targetList.isEmpty())
                	{
                		targetList.add(activeChar);
                	}
                }

                return targetList.toArray(new L2Character[targetList.size()]);
            }
            case TARGET_CORPSE_PLAYER:
            {
                if (target != null && target.isDead())
                {
                    L2PcInstance player = null;

                    if (activeChar instanceof L2PcInstance) player = (L2PcInstance) activeChar;
                    L2PcInstance targetPlayer = null;

                    if (target instanceof L2PcInstance) targetPlayer = (L2PcInstance) target;
                    L2PetInstance targetPet = null;

                    if (target instanceof L2PetInstance) targetPet = (L2PetInstance) target;

                    if (player != null && (targetPlayer != null || targetPet != null))
                    {
                        boolean condGood = true;

                        if (getSkillType() == SkillType.RESURRECT)
                        {
                            // check target is not in a active siege zone
                        	if (target.isInsideZone(L2Character.ZONE_SIEGE))
                            {
                                condGood = false;
                                player.sendPacket(new SystemMessage(SystemMessageId.CANNOT_BE_RESURRECTED_DURING_SIEGE));
                            }

                            if (targetPlayer != null)
                            {
                            	if (targetPlayer.isReviveRequested())
                            	{
                            		if (targetPlayer.isRevivingPet())
                            			player.sendPacket(new SystemMessage(SystemMessageId.MASTER_CANNOT_RES)); // While a pet is attempting to resurrect, it cannot help in resurrecting its master.
                            		else
                            			player.sendPacket(new SystemMessage(SystemMessageId.RES_HAS_ALREADY_BEEN_PROPOSED)); // Resurrection is already been proposed.
                                    condGood = false;
                            	}
                            }
                            else if (targetPet != null)
                            {
                                if (targetPet.getOwner() != player)
                                {
                                    condGood = false;
                                    player.sendMessage("You are not the owner of this pet");
                                }
                            }
                        }

                        if (condGood)
                        {
                            if (onlyFirst == false)
                            {
                                targetList.add(target);
                                return targetList.toArray(new L2Object[targetList.size()]);
                            }
                            else return new L2Character[] {target};

                        }
                    }
                }
                activeChar.sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
                return null;
            }
            case TARGET_CORPSE_MOB:
            {
                if (!(target instanceof L2Attackable) || !target.isDead())
                {
                    activeChar.sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
                    return null;
                }
                
                // Corpse mob only available for half time
                switch (getSkillType())
                {
                	case DRAIN:
                	case SUMMON:
                	{
                		if (DecayTaskManager.getInstance().getTasks().containsKey(target) 
                        		&& (System.currentTimeMillis() - DecayTaskManager.getInstance().getTasks().get(target)) > DecayTaskManager.ATTACKABLE_DECAY_TIME / 2)
                        {
                        	activeChar.sendPacket(new SystemMessage(SystemMessageId.CORPSE_TOO_OLD_SKILL_NOT_USED));
                        	return null;
                        }
                	}
                }

                if (onlyFirst == false)
                {
                    targetList.add(target);
                    return targetList.toArray(new L2Object[targetList.size()]);
                }
                else return new L2Character[] {target};

            }
            case TARGET_AREA_CORPSE_MOB:
            {
                if ((!(target instanceof L2Attackable)) || !target.isDead())
                {
                    activeChar.sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
                    return null;
                }

                if (onlyFirst == false) targetList.add(target);
                else return new L2Character[] {target};

                boolean srcInArena = (activeChar.isInsideZone(L2Character.ZONE_PVP) && !activeChar.isInsideZone(L2Character.ZONE_SIEGE));
                L2PcInstance src = null;
                if (activeChar instanceof L2PcInstance)
                	src = (L2PcInstance)activeChar;
                L2PcInstance trg = null;

                int radius = getSkillRadius();
                Collection<L2Object> objs = activeChar.getKnownList().getKnownObjects().values();
                //synchronized (activeChar.getKnownList().getKnownObjects())
				{
					for (L2Object obj : objs)
					{
						if (!(obj instanceof L2Attackable || obj instanceof L2PlayableInstance)
						        || ((L2Character) obj).isDead()
						        || ((L2Character) obj) == activeChar)
							continue;
						
						if (!Util.checkIfInRange(radius, target, obj, true))
							continue;
						
						if (!GeoData.getInstance().canSeeTarget(activeChar, obj))
							continue;
						
						if (obj instanceof L2PcInstance && src != null)
						{
							trg = (L2PcInstance) obj;
							
							if ((src.getParty() != null && trg.getParty() != null)
							        && src.getParty().getPartyLeaderOID() == trg.getParty().getPartyLeaderOID())
								continue;
							
							if (trg.isInsideZone(L2Character.ZONE_PEACE))
								continue;
							
							if (!srcInArena
							        && !(trg.isInsideZone(L2Character.ZONE_PVP) && !trg.isInsideZone(L2Character.ZONE_SIEGE)))
							{
								if (src.getAllyId() == trg.getAllyId()
								        && src.getAllyId() != 0)
									continue;
								
								if (src.getClan() != null
								        && trg.getClan() != null)
								{
									if (src.getClan().getClanId() == trg.getClan().getClanId())
										continue;
								}
								
								if (!src.checkPvpSkill(obj, this))
									continue;
							}
						}
						if (obj instanceof L2Summon && src != null)
						{
							trg = ((L2Summon) obj).getOwner();
							
							if ((src.getParty() != null && trg.getParty() != null)
							        && src.getParty().getPartyLeaderOID() == trg.getParty().getPartyLeaderOID())
								continue;
							
							if (!srcInArena
							        && !(trg.isInsideZone(L2Character.ZONE_PVP) && !trg.isInsideZone(L2Character.ZONE_SIEGE)))
							{
								if (src.getAllyId() == trg.getAllyId()
								        && src.getAllyId() != 0)
									continue;
								
								if (src.getClan() != null
								        && trg.getClan() != null)
								{
									if (src.getClan().getClanId() == trg.getClan().getClanId())
										continue;
								}
								
								if (!src.checkPvpSkill(trg, this))
									continue;
							}
							
							if (((L2Summon) obj).isInsideZone(L2Character.ZONE_PEACE))
								continue;
						}
						
						targetList.add((L2Character) obj);
					}
				}

                if (targetList.size() == 0) return null;
                return targetList.toArray(new L2Character[targetList.size()]);
            }
            case TARGET_UNLOCKABLE:
            {
                if (!(target instanceof L2DoorInstance) && !(target instanceof L2ChestInstance))
                {
                	//activeChar.sendPacket(new SystemMessage(SystemMessage.TARGET_IS_INCORRECT));
                    return null;
                }

                if (onlyFirst == false)
                {
                    targetList.add(target);
                    return targetList.toArray(new L2Object[targetList.size()]);
                }
                else return new L2Character[] {target};

            }
            case TARGET_ITEM:
            {
                activeChar.sendMessage("Target type of skill is not currently handled");
                return null;
            }
            case TARGET_UNDEAD:
            {
                if (target instanceof L2NpcInstance || target instanceof L2SummonInstance)
                {
                    if (!target.isUndead() || target.isDead())
                    {
                        activeChar.sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
                        return null;
                    }

                    if (onlyFirst == false) targetList.add(target);
                    else return new L2Character[] {target};

                    return targetList.toArray(new L2Object[targetList.size()]);
                }
                else
                {
                    activeChar.sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
                    return null;
                }
            }
            case TARGET_AREA_UNDEAD:
            {
                L2Character cha;
                int radius = getSkillRadius();
                if (getCastRange() >= 0 && (target instanceof L2NpcInstance || target instanceof L2SummonInstance)
                		&& target.isUndead() && !target.isAlikeDead())
                {
                    cha = target;

                    if (onlyFirst == false) targetList.add(cha); // Add target to target list
                    else return new L2Character[] {cha};

                }
                else cha = activeChar;

                Collection<L2Object> objs = cha.getKnownList().getKnownObjects().values();
                //synchronized (cha.getKnownList().getKnownObjects())
				{
					for (L2Object obj : objs)
					{
						if (obj instanceof L2NpcInstance)
							target = (L2NpcInstance) obj;
						else if (obj instanceof L2SummonInstance)
							target = (L2SummonInstance) obj;
						else
							continue;
						
						if (!GeoData.getInstance().canSeeTarget(activeChar, target))
							continue;
						
						if (!target.isAlikeDead()) // If target is not
													// dead/fake death and not
													// self
						{
							if (!target.isUndead())
								continue;
							if (!Util.checkIfInRange(radius, cha, obj, true))
								continue;
							
							if (onlyFirst == false)
								targetList.add((L2Character) obj);
							else
								return new L2Character[] { (L2Character) obj };
						}
					}
				}

                if (targetList.size() == 0) return null;
                return targetList.toArray(new L2Character[targetList.size()]);
            }
            case TARGET_ENEMY_SUMMON:
            {
                if(target instanceof L2Summon)
                {
                    L2Summon targetSummon = (L2Summon)target;
                    if (activeChar instanceof L2PcInstance && activeChar.getPet() != targetSummon && !targetSummon.isDead()
                            && (targetSummon.getOwner().getPvpFlag() != 0 || targetSummon.getOwner().getKarma() > 0)
                            || (targetSummon.getOwner().isInsideZone(L2Character.ZONE_PVP) && ((L2PcInstance)activeChar).isInsideZone(L2Character.ZONE_PVP)))
                       return new L2Character[]{targetSummon};
                }
                return null;
            }
            default:
            {
                activeChar.sendMessage("Target type of skill is not currently handled");
                return null;
            }
        }//end switch
    }

    public final L2Object[] getTargetList(L2Character activeChar)
    {
        return getTargetList(activeChar, false);
    }

    public final L2Object getFirstOfTargetList(L2Character activeChar)
    {
        L2Object[] targets;

        targets = getTargetList(activeChar, true);

        if (targets == null || targets.length == 0) return null;
        else return targets[0];
    }

    public final Func[] getStatFuncs(@SuppressWarnings("unused")
    L2Effect effect, L2Character player)
    {
        if (!(player instanceof L2PcInstance) && !(player instanceof L2Attackable)
            && !(player instanceof L2Summon)) return _emptyFunctionSet;
        if (_funcTemplates == null) return _emptyFunctionSet;
        List<Func> funcs = new FastList<Func>();
        for (FuncTemplate t : _funcTemplates)
        {
            Env env = new Env();
            env.player = player;
            env.skill = this;
            Func f = t.getFunc(env, this); // skill is owner
            if (f != null) funcs.add(f);
        }
        if (funcs.size() == 0) return _emptyFunctionSet;
        return funcs.toArray(new Func[funcs.size()]);
    }

    public boolean hasEffects()
    {
        return (_effectTemplates != null && _effectTemplates.length > 0);
    }

    public final L2Effect[] getEffects(L2Character effector, L2Character effected)
    {
        if (isPassive()) return _emptyEffectSet;

        if (_effectTemplates == null)
        	return _emptyEffectSet;

        // doors and siege flags cannot receive any effects
        if (effected instanceof L2DoorInstance ||effected instanceof L2SiegeFlagInstance )
        	return _emptyEffectSet;

        if ((effector != effected) && effected.isInvul())
            return _emptyEffectSet;


        List<L2Effect> effects = new FastList<L2Effect>();

        for (EffectTemplate et : _effectTemplates)
        {
            Env env = new Env();
            env.player = effector;
            env.target = effected;
            env.skill = this;
            L2Effect e = et.getEffect(env);
            if (e != null) effects.add(e);
        }

        if (effects.size() == 0) return _emptyEffectSet;

        return effects.toArray(new L2Effect[effects.size()]);
    }
    
    public final L2Effect[] getEffects(L2CubicInstance effector, L2Character effected)
    {
        if (isPassive()) return _emptyEffectSet;

        if (_effectTemplates == null) 
        	return _emptyEffectSet;
        
        if ((!effector.equals(effected)) && effected.isInvul())
            return _emptyEffectSet;


        List<L2Effect> effects = new FastList<L2Effect>();

        for (EffectTemplate et : _effectTemplates)
        {
            Env env = new Env();
            env.player = effector.getOwner();
            env.cubic = effector;
            env.target = effected;
            env.skill = this;
            L2Effect e = et.getEffect(env);
            if (e != null) effects.add(e);
        }

        if (effects.size() == 0) return _emptyEffectSet;

        return effects.toArray(new L2Effect[effects.size()]);
    }

    public final L2Effect[] getEffectsSelf(L2Character effector)
    {
        if (isPassive()) return _emptyEffectSet;

        if (_effectTemplatesSelf == null) return _emptyEffectSet;

        List<L2Effect> effects = new FastList<L2Effect>();

        for (EffectTemplate et : _effectTemplatesSelf)
        {
            Env env = new Env();
            env.player = effector;
            env.target = effector;
            env.skill = this;
            L2Effect e = et.getEffect(env);
            if (e != null)
            {
                //Implements effect charge
                if (e.getEffectType()== L2Effect.EffectType.CHARGE)
                {
                	env.skill = SkillTable.getInstance().getInfo(8, effector.getSkillLevel(8));
                    EffectCharge effect = (EffectCharge) env.target.getFirstEffect(L2Effect.EffectType.CHARGE);
                    if (effect != null)
                    {
                    	int effectcharge = effect.getLevel();
                        if (effectcharge < _numCharges)
                        {
                        	effectcharge++;
                            effect.addNumCharges(effectcharge);
                            if (env.target instanceof L2PcInstance)
                            {
                            	env.target.sendPacket(new EtcStatusUpdate((L2PcInstance)env.target));
                                SystemMessage sm = new SystemMessage(SystemMessageId.FORCE_INCREASED_TO_S1);
                                sm.addNumber(effectcharge);
                                env.target.sendPacket(sm);
                            }
                        }
                    }
                    else effects.add(e);
                }
                else effects.add(e);
            }
        }
        if (effects.size() == 0) return _emptyEffectSet;

        return effects.toArray(new L2Effect[effects.size()]);
    }

    public final void attach(FuncTemplate f)
    {
        if (_funcTemplates == null)
        {
            _funcTemplates = new FuncTemplate[] {f};
        }
        else
        {
            int len = _funcTemplates.length;
            FuncTemplate[] tmp = new FuncTemplate[len + 1];
            System.arraycopy(_funcTemplates, 0, tmp, 0, len);
            tmp[len] = f;
            _funcTemplates = tmp;
        }
    }

    public final void attach(EffectTemplate effect)
    {
        if (_effectTemplates == null)
        {
            _effectTemplates = new EffectTemplate[] {effect};
        }
        else
        {
            int len = _effectTemplates.length;
            EffectTemplate[] tmp = new EffectTemplate[len + 1];
            System.arraycopy(_effectTemplates, 0, tmp, 0, len);
            tmp[len] = effect;
            _effectTemplates = tmp;
        }

    }
    public final void attachSelf(EffectTemplate effect)
    {
        if (_effectTemplatesSelf == null)
        {
            _effectTemplatesSelf = new EffectTemplate[] {effect};
        }
        else
        {
            int len = _effectTemplatesSelf.length;
            EffectTemplate[] tmp = new EffectTemplate[len + 1];
            System.arraycopy(_effectTemplatesSelf, 0, tmp, 0, len);
            tmp[len] = effect;
            _effectTemplatesSelf = tmp;
        }
    }

    public final void attach(Condition c, boolean itemOrWeapon)
    {
    	if(itemOrWeapon) _itemPreCondition = c;
    	else _preCondition = c;
    }

    @Override
	public String toString()
    {
        return "" + _name + "[id=" + _id + ",lvl=" + _level + "]";
    }
}
