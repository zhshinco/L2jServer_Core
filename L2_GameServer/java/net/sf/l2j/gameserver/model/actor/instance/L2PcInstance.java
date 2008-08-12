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
package net.sf.l2j.gameserver.model.actor.instance;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Calendar;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;

import javolution.util.FastList;
import javolution.util.FastMap;
import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.GameTimeController;
import net.sf.l2j.gameserver.GeoData;
import net.sf.l2j.gameserver.GmListTable;
import net.sf.l2j.gameserver.ItemsAutoDestroy;
import net.sf.l2j.gameserver.LoginServerThread;
import net.sf.l2j.gameserver.Olympiad;
import net.sf.l2j.gameserver.RecipeController;
import net.sf.l2j.gameserver.SevenSigns;
import net.sf.l2j.gameserver.SevenSignsFestival;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.Universe;
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.ai.L2CharacterAI;
import net.sf.l2j.gameserver.ai.L2PlayerAI;
import net.sf.l2j.gameserver.ai.L2SummonAI;
import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.cache.WarehouseCacheManager;
import net.sf.l2j.gameserver.communitybbs.BB.Forum;
import net.sf.l2j.gameserver.communitybbs.Manager.ForumsBBSManager;
import net.sf.l2j.gameserver.datatables.AccessLevel;
import net.sf.l2j.gameserver.datatables.AccessLevels;
import net.sf.l2j.gameserver.datatables.CharTemplateTable;
import net.sf.l2j.gameserver.datatables.ClanTable;
import net.sf.l2j.gameserver.datatables.FishTable;
import net.sf.l2j.gameserver.datatables.HennaTable;
import net.sf.l2j.gameserver.datatables.HeroSkillTable;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.datatables.MapRegionTable;
import net.sf.l2j.gameserver.datatables.NobleSkillTable;
import net.sf.l2j.gameserver.datatables.NpcTable;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.datatables.SkillTreeTable;
import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.handler.ISkillHandler;
import net.sf.l2j.gameserver.handler.ItemHandler;
import net.sf.l2j.gameserver.handler.SkillHandler;
import net.sf.l2j.gameserver.handler.skillhandlers.SiegeFlag;
import net.sf.l2j.gameserver.handler.skillhandlers.StrSiegeAssault;
import net.sf.l2j.gameserver.handler.skillhandlers.TakeCastle;
import net.sf.l2j.gameserver.handler.skillhandlers.TakeFort;
import net.sf.l2j.gameserver.instancemanager.CastleManager;
import net.sf.l2j.gameserver.instancemanager.CoupleManager;
import net.sf.l2j.gameserver.instancemanager.CursedWeaponsManager;
import net.sf.l2j.gameserver.instancemanager.DimensionalRiftManager;
import net.sf.l2j.gameserver.instancemanager.DuelManager;
import net.sf.l2j.gameserver.instancemanager.FortSiegeManager;
import net.sf.l2j.gameserver.instancemanager.ItemsOnGroundManager;
import net.sf.l2j.gameserver.instancemanager.QuestManager;
import net.sf.l2j.gameserver.instancemanager.SiegeManager;
import net.sf.l2j.gameserver.model.BlockList;
import net.sf.l2j.gameserver.model.FishData;
import net.sf.l2j.gameserver.model.Inventory;
import net.sf.l2j.gameserver.model.ItemContainer;
import net.sf.l2j.gameserver.model.L2Attackable;
import net.sf.l2j.gameserver.model.L2CharPosition;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.L2ClanMember;
import net.sf.l2j.gameserver.model.L2Decoy;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.L2Fishing;
import net.sf.l2j.gameserver.model.L2HennaInstance;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Macro;
import net.sf.l2j.gameserver.model.L2ManufactureList;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Party;
import net.sf.l2j.gameserver.model.L2Radar;
import net.sf.l2j.gameserver.model.L2RecipeList;
import net.sf.l2j.gameserver.model.L2Request;
import net.sf.l2j.gameserver.model.L2ShortCut;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2SkillLearn;
import net.sf.l2j.gameserver.model.L2Summon;
import net.sf.l2j.gameserver.model.L2Transformation;
import net.sf.l2j.gameserver.model.L2Trap;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.L2WorldRegion;
import net.sf.l2j.gameserver.model.MacroList;
import net.sf.l2j.gameserver.model.PcFreight;
import net.sf.l2j.gameserver.model.PcInventory;
import net.sf.l2j.gameserver.model.PcWarehouse;
import net.sf.l2j.gameserver.model.PetInventory;
import net.sf.l2j.gameserver.model.ShortCuts;
import net.sf.l2j.gameserver.model.TradeList;
import net.sf.l2j.gameserver.model.L2Skill.SkillTargetType;
import net.sf.l2j.gameserver.model.L2Skill.SkillType;
import net.sf.l2j.gameserver.model.actor.appearance.PcAppearance;
import net.sf.l2j.gameserver.model.actor.knownlist.PcKnownList;
import net.sf.l2j.gameserver.model.actor.stat.PcStat;
import net.sf.l2j.gameserver.model.actor.status.PcStatus;
import net.sf.l2j.gameserver.model.base.ClassId;
import net.sf.l2j.gameserver.model.base.ClassLevel;
import net.sf.l2j.gameserver.model.base.Experience;
import net.sf.l2j.gameserver.model.base.PlayerClass;
import net.sf.l2j.gameserver.model.base.Race;
import net.sf.l2j.gameserver.model.base.SubClass;
import net.sf.l2j.gameserver.model.entity.Castle;
import net.sf.l2j.gameserver.model.entity.Duel;
import net.sf.l2j.gameserver.model.entity.L2Event;
import net.sf.l2j.gameserver.model.entity.Siege;
import net.sf.l2j.gameserver.model.entity.TvTEvent;
import net.sf.l2j.gameserver.model.quest.Quest;
import net.sf.l2j.gameserver.model.quest.QuestState;
import net.sf.l2j.gameserver.model.quest.State;
import net.sf.l2j.gameserver.network.L2GameClient;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.serverpackets.AbnormalStatusUpdate;
import net.sf.l2j.gameserver.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.serverpackets.ChangeWaitType;
import net.sf.l2j.gameserver.serverpackets.CharInfo;
import net.sf.l2j.gameserver.serverpackets.ConfirmDlg;
import net.sf.l2j.gameserver.serverpackets.EtcStatusUpdate;
import net.sf.l2j.gameserver.serverpackets.ExAutoSoulShot;
import net.sf.l2j.gameserver.serverpackets.ExDuelUpdateUserInfo;
import net.sf.l2j.gameserver.serverpackets.ExFishingEnd;
import net.sf.l2j.gameserver.serverpackets.ExFishingStart;
import net.sf.l2j.gameserver.serverpackets.ExOlympiadMode;
import net.sf.l2j.gameserver.serverpackets.ExOlympiadSpelledInfo;
import net.sf.l2j.gameserver.serverpackets.ExOlympiadUserInfoSpectator;
import net.sf.l2j.gameserver.serverpackets.ExSetCompassZoneCode;
import net.sf.l2j.gameserver.serverpackets.ExSpawnEmitter;
import net.sf.l2j.gameserver.serverpackets.GameGuardQuery;
import net.sf.l2j.gameserver.serverpackets.HennaInfo;
import net.sf.l2j.gameserver.serverpackets.InventoryUpdate;
import net.sf.l2j.gameserver.serverpackets.ItemList;
import net.sf.l2j.gameserver.serverpackets.L2GameServerPacket;
import net.sf.l2j.gameserver.serverpackets.LeaveWorld;
import net.sf.l2j.gameserver.serverpackets.MagicSkillCanceld;
import net.sf.l2j.gameserver.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.serverpackets.MyTargetSelected;
import net.sf.l2j.gameserver.serverpackets.NicknameChanged;
import net.sf.l2j.gameserver.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.serverpackets.ObservationMode;
import net.sf.l2j.gameserver.serverpackets.ObservationReturn;
import net.sf.l2j.gameserver.serverpackets.PartySmallWindowUpdate;
import net.sf.l2j.gameserver.serverpackets.PartySpelled;
import net.sf.l2j.gameserver.serverpackets.PetInfo;
import net.sf.l2j.gameserver.serverpackets.PetInventoryUpdate;
import net.sf.l2j.gameserver.serverpackets.PledgeShowInfoUpdate;
import net.sf.l2j.gameserver.serverpackets.PledgeShowMemberListDelete;
import net.sf.l2j.gameserver.serverpackets.PledgeShowMemberListUpdate;
import net.sf.l2j.gameserver.serverpackets.PrivateStoreListBuy;
import net.sf.l2j.gameserver.serverpackets.PrivateStoreListSell;
import net.sf.l2j.gameserver.serverpackets.PrivateStoreManageListBuy;
import net.sf.l2j.gameserver.serverpackets.PrivateStoreManageListSell;
import net.sf.l2j.gameserver.serverpackets.QuestList;
import net.sf.l2j.gameserver.serverpackets.RecipeShopSellList;
import net.sf.l2j.gameserver.serverpackets.RelationChanged;
import net.sf.l2j.gameserver.serverpackets.Ride;
import net.sf.l2j.gameserver.serverpackets.SetupGauge;
import net.sf.l2j.gameserver.serverpackets.ShortBuffStatusUpdate;
import net.sf.l2j.gameserver.serverpackets.ShortCutInit;
import net.sf.l2j.gameserver.serverpackets.SkillCoolTime;
import net.sf.l2j.gameserver.serverpackets.SkillList;
import net.sf.l2j.gameserver.serverpackets.Snoop;
import net.sf.l2j.gameserver.serverpackets.SocialAction;
import net.sf.l2j.gameserver.serverpackets.StatusUpdate;
import net.sf.l2j.gameserver.serverpackets.StopMove;
import net.sf.l2j.gameserver.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.serverpackets.TargetSelected;
import net.sf.l2j.gameserver.serverpackets.TargetUnselected;
import net.sf.l2j.gameserver.serverpackets.TradeDone;
import net.sf.l2j.gameserver.serverpackets.TradeStart;
import net.sf.l2j.gameserver.serverpackets.UserInfo;
import net.sf.l2j.gameserver.serverpackets.ValidateLocation;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.skills.Stats;
import net.sf.l2j.gameserver.templates.L2Armor;
import net.sf.l2j.gameserver.templates.L2ArmorType;
import net.sf.l2j.gameserver.templates.L2EtcItemType;
import net.sf.l2j.gameserver.templates.L2Henna;
import net.sf.l2j.gameserver.templates.L2Item;
import net.sf.l2j.gameserver.templates.L2PcTemplate;
import net.sf.l2j.gameserver.templates.L2Weapon;
import net.sf.l2j.gameserver.templates.L2WeaponType;
import net.sf.l2j.gameserver.util.Broadcast;
import net.sf.l2j.gameserver.util.FloodProtector;
import net.sf.l2j.util.Point3D;
import net.sf.l2j.util.Rnd;

/**
 * This class represents all player characters in the world.
 * There is always a client-thread connected to this (except if a player-store is activated upon logout).<BR><BR>
 *
 * @version $Revision: 1.66.2.41.2.33 $ $Date: 2005/04/11 10:06:09 $
 */
public final class L2PcInstance extends L2PlayableInstance
{
    // Character Skill SQL String Definitions:
	private static final String RESTORE_SKILLS_FOR_CHAR = "SELECT skill_id,skill_level FROM character_skills WHERE charId=? AND class_index=?";
	private static final String ADD_NEW_SKILL = "INSERT INTO character_skills (charId,skill_id,skill_level,skill_name,class_index) VALUES (?,?,?,?,?)";
	private static final String UPDATE_CHARACTER_SKILL_LEVEL = "UPDATE character_skills SET skill_level=? WHERE skill_id=? AND charId=? AND class_index=?";
	private static final String DELETE_SKILL_FROM_CHAR = "DELETE FROM character_skills WHERE skill_id=? AND charId=? AND class_index=?";
	private static final String DELETE_CHAR_SKILLS = "DELETE FROM character_skills WHERE charId=? AND class_index=?";

	// Character Skill Save SQL String Definitions:
	private static final String ADD_SKILL_SAVE = "INSERT INTO character_skills_save (charId,skill_id,skill_level,effect_count,effect_cur_time,reuse_delay,restore_type,class_index,buff_index) VALUES (?,?,?,?,?,?,?,?,?)";
	private static final String RESTORE_SKILL_SAVE = "SELECT skill_id,skill_level,effect_count,effect_cur_time, reuse_delay FROM character_skills_save WHERE charId=? AND class_index=? AND restore_type=? ORDER BY buff_index ASC";
	private static final String DELETE_SKILL_SAVE = "DELETE FROM character_skills_save WHERE charId=? AND class_index=?";

	// Character Character SQL String Definitions:
    private static final String INSERT_CHARACTER = "INSERT INTO characters (account_name,charId,char_name,level,maxHp,curHp,maxCp,curCp,maxMp,curMp,face,hairStyle,hairColor,sex,exp,sp,karma,pvpkills,pkkills,clanid,race,classid,deletetime,cancraft,title,accesslevel,online,isin7sdungeon,clan_privs,wantspeace,base_class,newbie,nobless,power_grade,last_recom_date) values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
	private static final String UPDATE_CHARACTER = "UPDATE characters SET level=?,maxHp=?,curHp=?,maxCp=?,curCp=?,maxMp=?,curMp=?,face=?,hairStyle=?,hairColor=?,heading=?,x=?,y=?,z=?,exp=?,expBeforeDeath=?,sp=?,karma=?,pvpkills=?,pkkills=?,rec_have=?,rec_left=?,clanid=?,race=?,classid=?,deletetime=?,title=?,accesslevel=?,online=?,isin7sdungeon=?,clan_privs=?,wantspeace=?,base_class=?,onlinetime=?,in_jail=?,jail_timer=?,newbie=?,nobless=?,power_grade=?,subpledge=?,last_recom_date=?,lvl_joined_academy=?,apprentice=?,sponsor=?,varka_ketra_ally=?,clan_join_expiry_time=?,clan_create_expiry_time=?,char_name=?,death_penalty_level=? WHERE charId=?";
    private static final String RESTORE_CHARACTER = "SELECT account_name, charId, char_name, level, maxHp, curHp, maxCp, curCp, maxMp, curMp, face, hairStyle, hairColor, sex, heading, x, y, z, exp, expBeforeDeath, sp, karma, pvpkills, pkkills, clanid, race, classid, deletetime, cancraft, title, rec_have, rec_left, accesslevel, online, char_slot, lastAccess, clan_privs, wantspeace, base_class, onlinetime, isin7sdungeon, in_jail, jail_timer, newbie, nobless, power_grade, subpledge, last_recom_date, lvl_joined_academy, apprentice, sponsor, varka_ketra_ally,clan_join_expiry_time,clan_create_expiry_time,death_penalty_level FROM characters WHERE charId=?";

    // Character Subclass SQL String Definitions:
    private static final String RESTORE_CHAR_SUBCLASSES = "SELECT class_id,exp,sp,level,class_index FROM character_subclasses WHERE charId=? ORDER BY class_index ASC";
    private static final String ADD_CHAR_SUBCLASS = "INSERT INTO character_subclasses (charId,class_id,exp,sp,level,class_index) VALUES (?,?,?,?,?,?)";
    private static final String UPDATE_CHAR_SUBCLASS = "UPDATE character_subclasses SET exp=?,sp=?,level=?,class_id=? WHERE charId=? AND class_index =?";
    private static final String DELETE_CHAR_SUBCLASS = "DELETE FROM character_subclasses WHERE charId=? AND class_index=?";

    // Character Henna SQL String Definitions:
	private static final String RESTORE_CHAR_HENNAS = "SELECT slot,symbol_id FROM character_hennas WHERE charId=? AND class_index=?";
	private static final String ADD_CHAR_HENNA = "INSERT INTO character_hennas (charId,symbol_id,slot,class_index) VALUES (?,?,?,?)";
	private static final String DELETE_CHAR_HENNA = "DELETE FROM character_hennas WHERE charId=? AND slot=? AND class_index=?";
	private static final String DELETE_CHAR_HENNAS = "DELETE FROM character_hennas WHERE charId=? AND class_index=?";

	// Character Shortcut SQL String Definitions:
	private static final String DELETE_CHAR_SHORTCUTS = "DELETE FROM character_shortcuts WHERE charId=? AND class_index=?";

	// Character Recommendation SQL String Definitions:
	private static final String RESTORE_CHAR_RECOMS = "SELECT charId,target_id FROM character_recommends WHERE charId=?";
	private static final String ADD_CHAR_RECOM = "INSERT INTO character_recommends (charId,target_id) VALUES (?,?)";
	private static final String DELETE_CHAR_RECOMS = "DELETE FROM character_recommends WHERE charId=?";

	// Character Transformation SQL String Definitions:
	private static final String SELECT_CHAR_TRANSFORM = "SELECT transform_id FROM characters WHERE charId=?";
	private static final String UPDATE_CHAR_TRANSFORM = "UPDATE characters SET transform_id=? WHERE charId=?";

	public static final int REQUEST_TIMEOUT = 15;
	public static final int STORE_PRIVATE_NONE = 0;
	public static final int STORE_PRIVATE_SELL = 1;
	public static final int STORE_PRIVATE_BUY = 3;
	public static final int STORE_PRIVATE_MANUFACTURE = 5;
	public static final int STORE_PRIVATE_PACKAGE_SELL = 8;

	/** The table containing all minimum level needed for each Expertise (None, D, C, B, A, S, S80)*/
    private static final int[] EXPERTISE_LEVELS =
    {
	    SkillTreeTable.getInstance().getExpertiseLevel(0), //NONE
	    SkillTreeTable.getInstance().getExpertiseLevel(1), //D
	    SkillTreeTable.getInstance().getExpertiseLevel(2), //C
	    SkillTreeTable.getInstance().getExpertiseLevel(3), //B
	    SkillTreeTable.getInstance().getExpertiseLevel(4), //A
	    SkillTreeTable.getInstance().getExpertiseLevel(5), //S
	    SkillTreeTable.getInstance().getExpertiseLevel(6), //S80
    };

	private static final int[] COMMON_CRAFT_LEVELS =
	{
	 5,20,28,36,43,49,55,62
	};

	//private static Logger _log = Logger.getLogger(L2PcInstance.class.getName());

	public class AIAccessor extends L2Character.AIAccessor
	{
		protected AIAccessor() {}
		public L2PcInstance getPlayer() { return L2PcInstance.this; }
		public void doPickupItem(L2Object object) {
			L2PcInstance.this.doPickupItem(object);
		}
		public void doInteract(L2Character target) {
			L2PcInstance.this.doInteract(target);
		}

		@Override
		public void doAttack(L2Character target)
        {
			super.doAttack(target);

			// cancel the recent fake-death protection instantly if the player attacks or casts spells
			getPlayer().setRecentFakeDeath(false);
		}

		@Override
		public void doCast(L2Skill skill)
        {
			super.doCast(skill);

			// cancel the recent fake-death protection instantly if the player attacks or casts spells
			getPlayer().setRecentFakeDeath(false);
		}
	}

	private L2GameClient _client;

	private String _accountName;
	private long _deleteTimer;

	private boolean _isOnline = false;
	private long _onlineTime;
	private long _onlineBeginTime;
	private long _lastAccess;
	private long _uptime;

	protected int _baseClass;
	protected int _activeClass;
	protected int _classIndex = 0;

	/** The list of sub-classes this character has. */
    private Map<Integer, SubClass> _subClasses;

	private PcAppearance _appearance;

	/** The Identifier of the L2PcInstance */
	private int _charId = 0x00030b7a;

	/** The Experience of the L2PcInstance before the last Death Penalty */
	private long _expBeforeDeath;

	/** The Karma of the L2PcInstance (if higher than 0, the name of the L2PcInstance appears in red) */
	private int _karma;

	/** The number of player killed during a PvP (the player killed was PvP Flagged) */
	private int _pvpKills;

	/** The PK counter of the L2PcInstance (= Number of non PvP Flagged player killed) */
	private int _pkKills;

	/** The PvP Flag state of the L2PcInstance (0=White, 1=Purple) */
	private byte _pvpFlag;

	/** The Siege state of the L2PcInstance */
	private byte _siegeState = 0;

	private int _curWeightPenalty = 0;

	private int _lastCompassZone; // the last compass zone update send to the client
	private byte _zoneValidateCounter = 4;

	private boolean _isIn7sDungeon = false;

    private boolean _inJail = false;
    private long _jailTimer = 0;
    private ScheduledFuture<?> _jailTask;

    /** Olympiad */
    private boolean _inOlympiadMode = false;
    private boolean _OlympiadStart = false;
    private int _olympiadGameId = -1;
    private int _olympiadSide = -1;
    public int olyBuff = 0;

    /** Duel */
    private boolean _isInDuel = false;
    private int _duelState = Duel.DUELSTATE_NODUEL;
    private int _duelId = 0;
    private SystemMessageId _noDuelReason = SystemMessageId.THERE_IS_NO_OPPONENT_TO_RECEIVE_YOUR_CHALLENGE_FOR_A_DUEL;

	/** Boat */
	private boolean _inBoat;
    private L2BoatInstance _boat;
    private Point3D _inBoatPosition;

	private int _mountType;
    private int _mountNpcId;
    /** Store object used to summon the strider you are mounting **/
	private int _mountObjectID = 0;

	public int _telemode = 0;

	public boolean _exploring = false;

	

	private boolean _inCrystallize;

	private boolean _inCraftMode;
    
    private L2Transformation _transformation;
    
    private static int _transformationId = 0;

	/** The table containing all L2RecipeList of the L2PcInstance */
	private Map<Integer, L2RecipeList> _dwarvenRecipeBook = new FastMap<Integer, L2RecipeList>();
	private Map<Integer, L2RecipeList> _commonRecipeBook = new FastMap<Integer, L2RecipeList>();

		/** True if the L2PcInstance is sitting */
	private boolean _waitTypeSitting;

	/** True if the L2PcInstance is using the relax skill */
	private boolean _relax;

	/** Location before entering Observer Mode */
	private int _obsX;
	private int _obsY;
	private int _obsZ;
	private boolean _observerMode = false;

	/** Stored from last ValidatePosition **/
	private Point3D _lastClientPosition = new Point3D(0, 0, 0);
	private Point3D _lastServerPosition = new Point3D(0, 0, 0);

	/** The number of recommandation obtained by the L2PcInstance */
	private int _recomHave; // how much I was recommended by others

	/** The number of recommandation that the L2PcInstance can give */
	private int _recomLeft; // how many recomendations I can give to others

	/** Date when recom points were updated last time */
	private long _lastRecomUpdate;
	/** List with the recomendations that I've give */
	private List<Integer> _recomChars = new FastList<Integer>();

	/** The random number of the L2PcInstance */
	//private static final Random _rnd = new Random();

	private PcInventory _inventory = new PcInventory(this);
	private PcWarehouse _warehouse;
	private PcFreight _freight = new PcFreight(this);

	/** The Private Store type of the L2PcInstance (STORE_PRIVATE_NONE=0, STORE_PRIVATE_SELL=1, sellmanage=2, STORE_PRIVATE_BUY=3, buymanage=4, STORE_PRIVATE_MANUFACTURE=5) */
	private int _privatestore;

	private TradeList _activeTradeList;
	private ItemContainer _activeWarehouse;
	private L2ManufactureList _createList;
	private TradeList _sellList;
	private TradeList _buyList;

	/** Bitmask used to keep track of one-time/newbie quest rewards*/
	private int _newbie;

	private boolean _noble = false;
	private boolean _hero = false;

	/** The L2FolkInstance corresponding to the last Folk wich one the player talked. */
	private L2FolkInstance _lastFolkNpc = null;

	/** Last NPC Id talked on a quest */
	private int _questNpcObject = 0;

	/** The table containing all Quests began by the L2PcInstance */
	private Map<String, QuestState> _quests = new FastMap<String, QuestState>();

	/** The list containing all shortCuts of this L2PcInstance */
	private ShortCuts _shortCuts = new ShortCuts(this);

	/** The list containing all macroses of this L2PcInstance */
	private MacroList _macroses = new MacroList(this);

	private List<L2PcInstance> _snoopListener = new FastList<L2PcInstance>();
	private List<L2PcInstance> _snoopedPlayer = new FastList<L2PcInstance>();

	private ClassId _skillLearningClassId;

	// hennas
	private final L2HennaInstance[] _henna = new L2HennaInstance[3];
	private int _hennaSTR;
	private int _hennaINT;
	private int _hennaDEX;
	private int _hennaMEN;
	private int _hennaWIT;
	private int _hennaCON;

	/** The L2Summon of the L2PcInstance */
	private L2Summon _summon = null;
	/** The L2Decoy of the L2PcInstance */
	private L2Decoy _decoy = null;
	/** The L2Trap of the L2PcInstance */
	private L2Trap _trap = null;
	/** The L2Agathion of the L2PcInstance */
	private int _agathionId = 0;
	// apparently, a L2PcInstance CAN have both a summon AND a tamed beast at the same time!!
	private L2TamedBeastInstance _tamedBeast = null;

	// client radar
	//TODO: This needs to be better intergrated and saved/loaded
	private L2Radar _radar;

	// these values are only stored temporarily
	private boolean _partyMatchingAutomaticRegistration;
	private boolean _partyMatchingShowLevel;
	private boolean _partyMatchingShowClass;
	private String _partyMatchingMemo;

	// Clan related attributes
	/** The Clan Identifier of the L2PcInstance */
	private int _clanId;

	/** The Clan object of the L2PcInstance */
	private L2Clan _clan;

	/** Apprentice and Sponsor IDs */
    private int _apprentice = 0;
    private int _sponsor = 0;

	private long _clanJoinExpiryTime;
	private long _clanCreateExpiryTime;

	private int _powerGrade = 0;
	private int _clanPrivileges = 0;

    /** L2PcInstance's pledge class (knight, Baron, etc.)*/
    private int _pledgeClass = 0;
    private int _pledgeType = 0;

    /** Level at which the player joined the clan as an academy member*/
    private int _lvlJoinedAcademy = 0;

	private int _wantsPeace = 0;

	//Death Penalty Buff Level
	private int _deathPenaltyBuffLevel = 0;

	// Absorbed Souls
    private int _souls = 0;
    private ScheduledFuture<?> _soulTask = null;

    // WorldPosition used by TARGET_SIGNET_GROUND
    private Point3D _currentSkillWorldPosition;
    
    private AccessLevel _accessLevel;

	private boolean _chatBanned = false; 		// Chat Banned
    private ScheduledFuture<?> _chatUnbanTask = null;
	private boolean _messageRefusal = false;    // message refusal mode
	private boolean _dietMode = false;          // ignore weight penalty
	private boolean _tradeRefusal = false;       // Trade refusal
	private boolean _exchangeRefusal = false;   // Exchange refusal

	private L2Party _party;

	// this is needed to find the inviting player for Party response
	// there can only be one active party request at once
	private L2PcInstance _activeRequester;
	private long _requestExpireTime = 0;
	private L2Request _request = new L2Request(this);
	private L2ItemInstance _arrowItem;
    private L2ItemInstance _boltItem;

	// Used for protection after teleport
	private long _protectEndTime = 0;

	// protects a char from agro mobs when getting up from fake death
	private long _recentFakeDeathEndTime = 0;

	/** The fists L2Weapon of the L2PcInstance (used when no weapon is equiped) */
	private L2Weapon _fistsWeaponItem;

	private final Map<Integer, String> _chars = new FastMap<Integer, String>();

	//private byte _updateKnownCounter = 0;

	/** The current higher Expertise of the L2PcInstance (None=0, D=1, C=2, B=3, A=4, S=5)*/
	private int _expertiseIndex; // index in EXPERTISE_LEVELS
	private int _expertisePenalty = 0;

	private L2ItemInstance _activeEnchantItem = null;

	protected boolean _inventoryDisable = false;

	protected Map<Integer, L2CubicInstance> _cubics = new FastMap<Integer, L2CubicInstance>().setShared(true);

	/** Active shots. A FastSet variable would actually suffice but this was changed to fix threading stability... */
	protected Map<Integer, Integer> _activeSoulShots = new FastMap<Integer, Integer>().setShared(true);

	public final ReentrantLock soulShotLock = new ReentrantLock();

	/** Event parameters */
	public int eventX;
	public int eventY;
	public int eventZ;
	public int eventkarma;
	public int eventpvpkills;
	public int eventpkkills;
	public String eventTitle;
	public LinkedList<String> kills = new LinkedList<String>();
	public boolean eventSitForced = false;
	public boolean atEvent = false;

	/** new loto ticket **/
	private int _loto[] = new int[5];
	//public static int _loto_nums[] = {0,1,2,3,4,5,6,7,8,9,};
	/** new race ticket **/
	private int _race[] = new int[2];

	private final BlockList _blockList = new BlockList();

	private int _team = 0;

    /** lvl of alliance with ketra orcs or varka silenos, used in quests and aggro checks
     *  [-5,-1] varka, 0 neutral, [1,5] ketra
     * */
    private int _alliedVarkaKetra = 0;

    private L2Fishing _fishCombat;
    private boolean _fishing = false;
    private int _fishx = 0;
    private int _fishy = 0;
    private int _fishz = 0;

    private ScheduledFuture<?> _taskRentPet;
    private ScheduledFuture<?> _taskWater;

	/** Bypass validations */
	private List<String> _validBypass = new FastList<String>();
	private List<String> _validBypass2 = new FastList<String>();

	private Forum _forumMail;
	private Forum _forumMemo;

    /** Current skill in use */
    private SkillDat _currentSkill;

    /** Skills queued because a skill is already in progress */
    private SkillDat _queuedSkill;

    /* Flag to disable equipment/skills while wearing formal wear **/
    private boolean _IsWearingFormalWear = false;

	private int _cursedWeaponEquippedId = 0;
	private boolean _combatFlagEquippedId = false;

	private int _reviveRequested = 0;
	private double _revivePower = 0;
	private boolean _revivePet = false;

	private double _cpUpdateIncCheck = .0;
	private double _cpUpdateDecCheck = .0;
	private double _cpUpdateInterval = .0;
	private double _mpUpdateIncCheck = .0;
	private double _mpUpdateDecCheck = .0;
	private double _mpUpdateInterval = .0;

	/** Herbs Task Time **/
	private int _herbstask = 0;
	/** Task for Herbs */
	public class HerbTask implements Runnable
	{
		private String _process;
		private int _itemId;
		private int _count;
		private L2Object _reference;
		private boolean _sendMessage;
		HerbTask(String process, int itemId, int count, L2Object reference, boolean sendMessage)
		{
			_process = process;
			_itemId = itemId;
			_count = count;
			_reference = reference;
			_sendMessage = sendMessage;
		}
		@SuppressWarnings("synthetic-access")
		public void run()
		{
			try
			{
				addItem(_process, _itemId, _count, _reference, _sendMessage);
			}
			catch (Throwable t)
			{
				_log.log(Level.WARNING, "", t);
			}
		}
	}

	/** ShortBuff clearing Task */
	private ScheduledFuture<?> _shortBuffTask = null;

	private class ShortBuffTask implements Runnable
	{
		private L2PcInstance _player = null;

        public ShortBuffTask(L2PcInstance activeChar)
        {
	        _player = activeChar;
        }

		public void run()
		{
			if (_player == null)
				return;
			
			_player.sendPacket(new ShortBuffStatusUpdate(0, 0, 0));
		}
	}

	// L2JMOD Wedding
	private boolean _married = false;
	private int _partnerId = 0;
	private int _coupleId = 0;
	private boolean _engagerequest = false;
	private int _engageid = 0;
	private boolean _marryrequest = false;
	private boolean _marryaccepted = false;

    /** Skill casting information (used to queue when several skills are cast in a short time) **/
    public class SkillDat
    {
        private L2Skill _skill;
        private boolean _ctrlPressed;
        private boolean _shiftPressed;

        protected SkillDat(L2Skill skill, boolean ctrlPressed, boolean shiftPressed)
        {
            _skill = skill;
            _ctrlPressed = ctrlPressed;
            _shiftPressed = shiftPressed;
        }

        public boolean isCtrlPressed()
        {
            return _ctrlPressed;
        }

        public boolean isShiftPressed()
        {
            return _shiftPressed;
        }

        public L2Skill getSkill()
        {
            return _skill;
        }

        public int getSkillId()
        {
            return (getSkill() != null) ? getSkill().getId() : -1;
        }
    }

	/**
	 * Create a new L2PcInstance and add it in the characters table of the database.<BR><BR>
	 *
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Create a new L2PcInstance with an account name </li>
	 * <li>Set the name, the Hair Style, the Hair Color and  the Face type of the L2PcInstance</li>
	 * <li>Add the player in the characters table of the database</li><BR><BR>
	 *
	 * @param objectId Identifier of the object to initialized
	 * @param template The L2PcTemplate to apply to the L2PcInstance
	 * @param accountName The name of the L2PcInstance
	 * @param name The name of the L2PcInstance
	 * @param hairStyle The hair style Identifier of the L2PcInstance
	 * @param hairColor The hair color Identifier of the L2PcInstance
	 * @param face The face type Identifier of the L2PcInstance
	 *
	 * @return The L2PcInstance added to the database or null
	 *
	 */
	public static L2PcInstance create(int objectId, L2PcTemplate template, String accountName,
	                                  String name, byte hairStyle, byte hairColor, byte face, boolean sex)
	{
		// Create a new L2PcInstance with an account name
		PcAppearance app = new PcAppearance(face, hairColor, hairStyle, sex);
		L2PcInstance player = new L2PcInstance(objectId, template, accountName, app);

		// Set the name of the L2PcInstance
		player.setName(name);

		// Set the base class ID to that of the actual class ID.
		player.setBaseClass(player.getClassId());
		//Kept for backwards compabitility.
		player.setNewbie(1);
		// Add the player in the characters table of the database
		boolean ok = player.createDb();

		if (!ok)
			return null;

		return player;
	}

	public static L2PcInstance createDummyPlayer(int objectId, String name)
	{
		// Create a new L2PcInstance with an account name
		L2PcInstance player = new L2PcInstance(objectId);
		player.setName(name);

		return player;
	}

	public String getAccountName()
	{
		return getClient().getAccountName();
	}

	public Map<Integer, String> getAccountChars()
	{
		return _chars;
	}

	public int getRelation(L2PcInstance target)
	{
		int result = 0;

		// karma and pvp may not be required
		if (getPvpFlag() != 0)
			result |= RelationChanged.RELATION_PVP_FLAG;
		if (getKarma() > 0)
			result |= RelationChanged.RELATION_HAS_KARMA;

		if (isClanLeader())
			result |= RelationChanged.RELATION_LEADER;

		if (getSiegeState() != 0)
		{
			result |= RelationChanged.RELATION_INSIEGE;
			if (getSiegeState() != target.getSiegeState())
				result |= RelationChanged.RELATION_ENEMY;
			else
				result |= RelationChanged.RELATION_ALLY;
			if (getSiegeState() == 1)
				result |= RelationChanged.RELATION_ATTACKER;
		}

		if (getClan() != null && target.getClan() != null)
		{
			if (target.getPledgeType() != L2Clan.SUBUNIT_ACADEMY
				&& target.getClan().isAtWarWith(getClan().getClanId()))
			{
				result |= RelationChanged.RELATION_1SIDED_WAR;
				if (getClan().isAtWarWith(target.getClan().getClanId()))
					result |= RelationChanged.RELATION_MUTUAL_WAR;
			}
		}
		return result;
	}

	/**
	 * Retrieve a L2PcInstance from the characters table of the database and add it in _allObjects of the L2world (call restore method).<BR><BR>
	 *
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Retrieve the L2PcInstance from the characters table of the database </li>
	 * <li>Add the L2PcInstance object in _allObjects </li>
	 * <li>Set the x,y,z position of the L2PcInstance and make it invisible</li>
	 * <li>Update the overloaded status of the L2PcInstance</li><BR><BR>
	 *
	 * @param objectId Identifier of the object to initialized
	 *
	 * @return The L2PcInstance loaded from the database
	 *
	 */
	public static L2PcInstance load(int objectId)
	{
		return restore(objectId);
	}

	private void initPcStatusUpdateValues()
	{
		_cpUpdateInterval = getMaxCp() / 352.0;
		_cpUpdateIncCheck = getMaxCp();
		_cpUpdateDecCheck = getMaxCp() - _cpUpdateInterval;
		_mpUpdateInterval = getMaxMp() / 352.0;
		_mpUpdateIncCheck = getMaxMp();
		_mpUpdateDecCheck = getMaxMp() - _mpUpdateInterval;
	}

	/**
	 * Constructor of L2PcInstance (use L2Character constructor).<BR><BR>
	 *
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Call the L2Character constructor to create an empty _skills slot and copy basic Calculator set to this L2PcInstance </li>
	 * <li>Set the name of the L2PcInstance</li><BR><BR>
	 *
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method SET the level of the L2PcInstance to 1</B></FONT><BR><BR>
	 *
	 * @param objectId Identifier of the object to initialized
	 * @param template The L2PcTemplate to apply to the L2PcInstance
	 * @param accountName The name of the account including this L2PcInstance
	 *
	 */
	private L2PcInstance(int objectId, L2PcTemplate template, String accountName, PcAppearance app)
	{
		super(objectId, template);
		getKnownList();	// init knownlist
        getStat();			// init stats
        getStatus();		// init status
        super.initCharStatusUpdateValues();
        initPcStatusUpdateValues();

		_accountName  = accountName;
        app.setOwner(this);
		_appearance   = app;

		// Create an AI
		_ai = new L2PlayerAI(new L2PcInstance.AIAccessor());

		// Create a L2Radar object
		_radar = new L2Radar(this);

		// Retrieve from the database all skills of this L2PcInstance and add them to _skills
		// Retrieve from the database all items of this L2PcInstance and add them to _inventory
		getInventory().restore();
		if (!Config.WAREHOUSE_CACHE)
			getWarehouse();
		getFreight().restore();
	}

	private L2PcInstance(int objectId)
	{
		super(objectId, null);
		getKnownList();	// init knownlist
        getStat();			// init stats
        getStatus();		// init status
        super.initCharStatusUpdateValues();
        initPcStatusUpdateValues();
	}

	@Override
	public final PcKnownList getKnownList()
	{
		if(!(super.getKnownList() instanceof PcKnownList))
    		setKnownList(new PcKnownList(this));
		return (PcKnownList)super.getKnownList();
	}

	@Override
	public final PcStat getStat()
	{
		if(!(super.getStat() instanceof PcStat))
    		setStat(new PcStat(this));
		return (PcStat)super.getStat();
	}

	@Override
	public final PcStatus getStatus()
	{
		if(!(super.getStatus() instanceof PcStatus))
    		setStatus(new PcStatus(this));
		return (PcStatus)super.getStatus();
	}

	public final PcAppearance getAppearance()
	{
		return _appearance;
	}

	/**
	 * Return the base L2PcTemplate link to the L2PcInstance.<BR><BR>
	 */
	public final L2PcTemplate getBaseTemplate()
	{
		return CharTemplateTable.getInstance().getTemplate(_baseClass);
	}

	/** Return the L2PcTemplate link to the L2PcInstance. */
	@Override
	public final L2PcTemplate getTemplate() { return (L2PcTemplate)super.getTemplate(); }

	public void setTemplate(ClassId newclass) { super.setTemplate(CharTemplateTable.getInstance().getTemplate(newclass)); }

	/**
	 * Return the AI of the L2PcInstance (create it if necessary).<BR><BR>
	 */
	@Override
	public L2CharacterAI getAI()
	{
		if (_ai == null)
		{
			_ai = new L2PlayerAI(new L2PcInstance.AIAccessor());
		}

		return _ai;
	}


	/**
	 * Calculate a destination to explore the area and set the AI Intention to AI_INTENTION_MOVE_TO.<BR><BR>
	 */
	public void explore()
	{
		if (!_exploring) return;

		if(getMountType() == 2)
			return;

		// Calculate the destination point (random)

		int x = getX()+Rnd.nextInt(6000)-3000;
		int y = getY()+Rnd.nextInt(6000)-3000;

		if (x > Universe.MAX_X) x = Universe.MAX_X;
		if (x < Universe.MIN_X) x = Universe.MIN_X;
		if (y > Universe.MAX_Y) y = Universe.MAX_Y;
		if (y < Universe.MIN_Y) y = Universe.MIN_Y;

		int z = getZ();

		L2CharPosition pos = new L2CharPosition(x,y,z,0);

		// Set the AI Intention to AI_INTENTION_MOVE_TO
		getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO,pos);

	}

	/** Return the Level of the L2PcInstance. */
	@Override
	public final int getLevel() { return getStat().getLevel(); }
	
	/**
     * Return the _newbie state of the L2PcInstance.<BR><BR>
     * @deprecated Use {@link #getNewbie()} instead
     */
    public int isNewbie()
    {
        return getNewbie();
    }

	/**
	 * Return the _newbie rewards state of the L2PcInstance.<BR><BR>
	 */
	public int getNewbie()
	{
		return _newbie;
	}

	/**
	 * Set the _newbie rewards state of the L2PcInstance.<BR><BR>
	 *
	 * @param newbieRewards The Identifier of the _newbie state<BR><BR>
	 *
	 */
	public void setNewbie(int newbieRewards)
	{
		_newbie = newbieRewards;
	}


	public void setBaseClass(int baseClass)
	{
		_baseClass = baseClass;
	}

	public void setBaseClass(ClassId classId)
	{
		_baseClass = classId.ordinal();
	}

	public boolean isInStoreMode() { return (getPrivateStoreType() > 0); }
//	public boolean isInCraftMode() { return (getPrivateStoreType() == STORE_PRIVATE_MANUFACTURE); }

	public boolean isInCraftMode()
	{
		return _inCraftMode;
	}

	public void isInCraftMode(boolean b)
	{
		_inCraftMode = b;
	}

	/**
	 * Manage Logout Task.<BR><BR>
	 */
	public void logout()
	{
		closeNetConnection();
	}

	/**
	 * Return a table containing all Common L2RecipeList of the L2PcInstance.<BR><BR>
	 */
	public L2RecipeList[] getCommonRecipeBook()
	{
		return _commonRecipeBook.values().toArray(new L2RecipeList[_commonRecipeBook.values().size()]);
	}

	/**
	 * Return a table containing all Dwarf L2RecipeList of the L2PcInstance.<BR><BR>
	 */
	public L2RecipeList[] getDwarvenRecipeBook()
	{
		return _dwarvenRecipeBook.values().toArray(new L2RecipeList[_dwarvenRecipeBook.values().size()]);
	}

	/**
	 * Add a new L2RecipList to the table _commonrecipebook containing all L2RecipeList of the L2PcInstance <BR><BR>
	 *
	 * @param recipe The L2RecipeList to add to the _recipebook
	 *
	 */
	public void registerCommonRecipeList(L2RecipeList recipe)
	{
		_commonRecipeBook.put(recipe.getId(), recipe);
	}

	/**
	 * Add a new L2RecipList to the table _recipebook containing all L2RecipeList of the L2PcInstance <BR><BR>
	 *
	 * @param recipe The L2RecipeList to add to the _recipebook
	 *
	 */
	public void registerDwarvenRecipeList(L2RecipeList recipe)
	{
		_dwarvenRecipeBook.put(recipe.getId(), recipe);
	}

	/**
	 * @param RecipeID The Identifier of the L2RecipeList to check in the player's recipe books
	 *
	 * @return
	 * <b>TRUE</b> if player has the recipe on Common or Dwarven Recipe book else returns <b>FALSE</b>
	 */
	public boolean hasRecipeList(int recipeId)
	{
		if (_dwarvenRecipeBook.containsKey(recipeId))
			return true;
		else if(_commonRecipeBook.containsKey(recipeId))
			return true;
		else
			return false;
	}

	/**
	 * Tries to remove a L2RecipList from the table _DwarvenRecipeBook or from table _CommonRecipeBook, those table contain all L2RecipeList of the L2PcInstance <BR><BR>
	 *
	 * @param RecipeID The Identifier of the L2RecipeList to remove from the _recipebook
	 *
	 */
	public void unregisterRecipeList(int recipeId)
	{
		if (_dwarvenRecipeBook.containsKey(recipeId))
			_dwarvenRecipeBook.remove(recipeId);
		else if(_commonRecipeBook.containsKey(recipeId))
			_commonRecipeBook.remove(recipeId);
		else
			_log.warning("Attempted to remove unknown RecipeList: "+recipeId);

		L2ShortCut[] allShortCuts = getAllShortCuts();

		for (L2ShortCut sc : allShortCuts)
		{
			if (sc != null && sc.getId() == recipeId && sc.getType() == L2ShortCut.TYPE_RECIPE)
				deleteShortCut(sc.getSlot(), sc.getPage());
		}
	}

	/**
	 * Returns the Id for the last talked quest NPC.<BR><BR>
	 */
	public int getLastQuestNpcObject()
	{
		return _questNpcObject;
	}

	public void setLastQuestNpcObject(int npcId)
	{
		_questNpcObject = npcId;
	}

	/**
	 * Return the QuestState object corresponding to the quest name.<BR><BR>
	 *
	 * @param quest The name of the quest
	 *
	 */
	public QuestState getQuestState(String quest)
	{
		return _quests.get(quest);
	}

	/**
	 * Add a QuestState to the table _quest containing all quests began by the L2PcInstance.<BR><BR>
	 *
	 * @param qs The QuestState to add to _quest
	 *
	 */
	public void setQuestState(QuestState qs)
	{
		_quests.put(qs.getQuestName(), qs);
	}


	/**
	 * Remove a QuestState from the table _quest containing all quests began by the L2PcInstance.<BR><BR>
	 *
	 * @param quest The name of the quest
	 *
	 */
	public void delQuestState(String quest)
	{
		_quests.remove(quest);
	}

	private QuestState[] addToQuestStateArray(QuestState[] questStateArray, QuestState state) {
		int len = questStateArray.length;
		QuestState[] tmp = new QuestState[len+1];
		for (int i=0; i < len; i++)
			tmp[i] = questStateArray[i];
		tmp[len] = state;
		return tmp;
	}

	/**
	 * Return a table containing all Quest in progress from the table _quests.<BR><BR>
	 */
	public Quest[] getAllActiveQuests()
	{
		FastList<Quest> quests = new FastList<Quest>();

		for (QuestState qs : _quests.values())
		{
		    int questId = qs.getQuest().getQuestIntId();
			if ((questId>999) || (questId<1))
				continue;

			if (!qs.isStarted() && !Config.DEVELOPER)
				continue;

			quests.add(qs.getQuest());
		}

		return quests.toArray(new Quest[quests.size()]);
	}

	/**
	 * Return a table containing all QuestState to modify after a L2Attackable killing.<BR><BR>
	 *
	 * @param npcId The Identifier of the L2Attackable attacked
	 *
	 */
	public QuestState[] getQuestsForAttacks(L2NpcInstance npc)
	{
		// Create a QuestState table that will contain all QuestState to modify
		QuestState[] states = null;

		// Go through the QuestState of the L2PcInstance quests
		for (Quest quest : npc.getTemplate().getEventQuests(Quest.QuestEventType.ON_ATTACK))
		{
			// Check if the Identifier of the L2Attackable attck is needed for the current quest
			if (getQuestState(quest.getName())!=null)
			{
				// Copy the current L2PcInstance QuestState in the QuestState table
				if (states == null)
					states = new QuestState[]{getQuestState(quest.getName())};
				else
					states = addToQuestStateArray(states, getQuestState(quest.getName()));
			}
		}

		// Return a table containing all QuestState to modify
		return states;
	}

	/**
	 * Return a table containing all QuestState to modify after a L2Attackable killing.<BR><BR>
	 *
	 * @param npcId The Identifier of the L2Attackable killed
	 *
	 */
	public QuestState[] getQuestsForKills(L2NpcInstance npc)
	{
		// Create a QuestState table that will contain all QuestState to modify
		QuestState[] states = null;

		// Go through the QuestState of the L2PcInstance quests
		for (Quest quest : npc.getTemplate().getEventQuests(Quest.QuestEventType.ON_KILL))
		{
			// Check if the Identifier of the L2Attackable killed is needed for the current quest
			if (getQuestState(quest.getName())!=null)
			{
				// Copy the current L2PcInstance QuestState in the QuestState table
				if (states == null)
					states = new QuestState[]{getQuestState(quest.getName())};
				else
					states = addToQuestStateArray(states, getQuestState(quest.getName()));
			}
		}

		// Return a table containing all QuestState to modify
		return states;
	}

	/**
	 * Return a table containing all QuestState from the table _quests in which the L2PcInstance must talk to the NPC.<BR><BR>
	 *
	 * @param npcId The Identifier of the NPC
	 *
	 */
	public QuestState[] getQuestsForTalk(int npcId)
	{
		// Create a QuestState table that will contain all QuestState to modify
		QuestState[] states = null;

		// Go through the QuestState of the L2PcInstance quests
		Quest[] quests = NpcTable.getInstance().getTemplate(npcId).getEventQuests(Quest.QuestEventType.ON_TALK);
		if (quests != null)
		{
			for (Quest quest: quests)
			{
				if (quest != null)
				{
					// Copy the current L2PcInstance QuestState in the QuestState table
					if (getQuestState(quest.getName())!=null)
					{
						if (states == null)
							states = new QuestState[]{getQuestState(quest.getName())};
						else
							states = addToQuestStateArray(states, getQuestState(quest.getName()));
					}
				}
			}
		}

		// Return a table containing all QuestState to modify
		return states;
	}

	public QuestState processQuestEvent(String quest, String event)
	{
		QuestState retval = null;
		if (event == null)
			event = "";
		if (!_quests.containsKey(quest))
			return retval;
		QuestState qs = getQuestState(quest);
		if (qs == null && event.length() == 0)
			return retval;
		if (qs == null) {
			Quest q = QuestManager.getInstance().getQuest(quest);
			if (q == null)
				return retval;
			qs = q.newQuestState(this);
		}
		if (qs != null) {
			if (getLastQuestNpcObject() > 0)
			{
				L2Object object = L2World.getInstance().findObject(getLastQuestNpcObject());
				if (object instanceof L2NpcInstance && isInsideRadius(object, L2NpcInstance.INTERACTION_DISTANCE, false, false))
                {
					L2NpcInstance npc = (L2NpcInstance)object;
					QuestState[] states = getQuestsForTalk(npc.getNpcId());

					if (states != null)
					{
						for (QuestState state : states)
						{
							if ((state.getQuest().getQuestIntId() == qs.getQuest().getQuestIntId()) && !qs.isCompleted())
							{
								if (qs.getQuest().notifyEvent(event, npc, this))
									showQuestWindow(quest, State.getStateName(qs.getState()));

								retval = qs;
							}
						}
						sendPacket(new QuestList());
					}
				}
			}
		}

		return retval;
	}

	private void showQuestWindow(String questId, String stateId)
	{
		String path = "data/scripts/quests/"+questId+"/"+stateId+".htm";
		String content = HtmCache.getInstance().getHtm(path);  //TODO path for quests html

		if (content != null)
		{
			if (Config.DEBUG)
				_log.fine("Showing quest window for quest "+questId+" state "+stateId+" html path: " + path);

			NpcHtmlMessage npcReply = new NpcHtmlMessage(5);
			npcReply.setHtml(content);
			sendPacket(npcReply);
		}

		sendPacket( ActionFailed.STATIC_PACKET );
	}

	/**
	 * Return a table containing all L2ShortCut of the L2PcInstance.<BR><BR>
	 */
	public L2ShortCut[] getAllShortCuts()
	{
		return _shortCuts.getAllShortCuts();
	}

	/**
	 * Return the L2ShortCut of the L2PcInstance corresponding to the position (page-slot).<BR><BR>
	 *
	 * @param slot The slot in wich the shortCuts is equiped
	 * @param page The page of shortCuts containing the slot
	 *
	 */
	public L2ShortCut getShortCut(int slot, int page)
	{
		return _shortCuts.getShortCut(slot, page);
	}

	/**
	 * Add a L2shortCut to the L2PcInstance _shortCuts<BR><BR>
	 */
	public void registerShortCut(L2ShortCut shortcut)
	{
		_shortCuts.registerShortCut(shortcut);
	}

	/**
	 * Delete the L2ShortCut corresponding to the position (page-slot) from the L2PcInstance _shortCuts.<BR><BR>
	 */
	public void deleteShortCut(int slot, int page)
	{
		_shortCuts.deleteShortCut(slot, page);
	}

	/**
	 * Add a L2Macro to the L2PcInstance _macroses<BR><BR>
	 */
	public void registerMacro(L2Macro macro)
	{
		_macroses.registerMacro(macro);
	}

	/**
	 * Delete the L2Macro corresponding to the Identifier from the L2PcInstance _macroses.<BR><BR>
	 */
	public void deleteMacro(int id)
	{
		_macroses.deleteMacro(id);
	}

	/**
	 * Return all L2Macro of the L2PcInstance.<BR><BR>
	 */
	public MacroList getMacroses()
	{
		return _macroses;
	}

	/**
	 * Set the siege state of the L2PcInstance.<BR><BR>
	 * 1 = attacker, 2 = defender, 0 = not involved
	 */
	public void setSiegeState(byte siegeState)
	{
		_siegeState = siegeState;
	}
	/**
	 * Get the siege state of the L2PcInstance.<BR><BR>
	 * 1 = attacker, 2 = defender, 0 = not involved
	 */
	public byte getSiegeState()
	{
		return _siegeState;
	}

	/**
	 * Set the PvP Flag of the L2PcInstance.<BR><BR>
	 */
	public void setPvpFlag(int pvpFlag)
	{
		_pvpFlag = (byte)pvpFlag;
	}

	public byte getPvpFlag()
	{
		return _pvpFlag;
	}

	@Override
	public void updatePvPFlag(int value)
	{
		if (getPvpFlag() == value)
			return;
		setPvpFlag(value);

		sendPacket(new UserInfo(this));

		// If this player has a pet update the pets pvp flag as well
		if (getPet() != null) sendPacket(new RelationChanged(getPet(), getRelation(this), false));

		Collection<L2PcInstance> plrs = getKnownList().getKnownPlayers().values();
		//synchronized (getKnownList().getKnownPlayers())
		{
			for (L2PcInstance target : plrs)
			{
				target.sendPacket(new RelationChanged(this, getRelation(this), isAutoAttackable(target)));
				if (getPet() != null)
					target.sendPacket(new RelationChanged(getPet(), getRelation(this), isAutoAttackable(target)));
			}
		}
	}

    public void revalidateZone(boolean force)
    {
    	// Cannot validate if not in  a world region (happens during teleport)
    	if (getWorldRegion() == null) return;

    	// This function is called very often from movement code
    	if (force) _zoneValidateCounter = 4;
    	else
    	{
    		_zoneValidateCounter--;
    		if (_zoneValidateCounter < 0)
    			_zoneValidateCounter = 4;
    		else return;
    	}

    	getWorldRegion().revalidateZones(this);
    	
        if (Config.ALLOW_WATER)
            checkWaterState();

        if (isInsideZone(ZONE_SIEGE))
        {
        	if (_lastCompassZone == ExSetCompassZoneCode.SIEGEWARZONE2) return;
        	_lastCompassZone = ExSetCompassZoneCode.SIEGEWARZONE2;
        	ExSetCompassZoneCode cz = new ExSetCompassZoneCode(ExSetCompassZoneCode.SIEGEWARZONE2);
        	sendPacket(cz);
        }
        else if (isInsideZone(ZONE_PVP))
        {
        	if (_lastCompassZone == ExSetCompassZoneCode.PVPZONE) return;
        	_lastCompassZone = ExSetCompassZoneCode.PVPZONE;
        	ExSetCompassZoneCode cz = new ExSetCompassZoneCode(ExSetCompassZoneCode.PVPZONE);
        	sendPacket(cz);
        }
        else if (isIn7sDungeon())
        {
        	if (_lastCompassZone == ExSetCompassZoneCode.SEVENSIGNSZONE) return;
        	_lastCompassZone = ExSetCompassZoneCode.SEVENSIGNSZONE;
        	ExSetCompassZoneCode cz = new ExSetCompassZoneCode(ExSetCompassZoneCode.SEVENSIGNSZONE);
        	sendPacket(cz);
        }
        else if (isInsideZone(ZONE_PEACE))
        {
        	if (_lastCompassZone == ExSetCompassZoneCode.PEACEZONE) return;
        	_lastCompassZone = ExSetCompassZoneCode.PEACEZONE;
        	ExSetCompassZoneCode cz = new ExSetCompassZoneCode(ExSetCompassZoneCode.PEACEZONE);
        	sendPacket(cz);
        }
        else
        {
        	if (_lastCompassZone == ExSetCompassZoneCode.GENERALZONE) return;
        	if (_lastCompassZone == ExSetCompassZoneCode.SIEGEWARZONE2) updatePvPStatus();
        	_lastCompassZone = ExSetCompassZoneCode.GENERALZONE;
        	ExSetCompassZoneCode cz = new ExSetCompassZoneCode(ExSetCompassZoneCode.GENERALZONE);
        	sendPacket(cz);
        }
    }

	/**
	 * Return True if the L2PcInstance can Craft Dwarven Recipes.<BR><BR>
	 */
	public boolean hasDwarvenCraft()
	{
		return getSkillLevel(L2Skill.SKILL_CREATE_DWARVEN) >= 1;
	}

	public int getDwarvenCraft()
	{
		return getSkillLevel(L2Skill.SKILL_CREATE_DWARVEN);
	}

	/**
	 * Return True if the L2PcInstance can Craft Dwarven Recipes.<BR><BR>
	 */
	public boolean hasCommonCraft()
	{
		return getSkillLevel(L2Skill.SKILL_CREATE_COMMON) >= 1;
	}

	public int getCommonCraft()
	{
		return getSkillLevel(L2Skill.SKILL_CREATE_COMMON);
	}

	/**
	 * Return the PK counter of the L2PcInstance.<BR><BR>
	 */
	public int getPkKills()
	{
		return _pkKills;
	}

	/**
	 * Set the PK counter of the L2PcInstance.<BR><BR>
	 */
	public void setPkKills(int pkKills)
	{
		_pkKills = pkKills;
	}

	/**
	 * Return the _deleteTimer of the L2PcInstance.<BR><BR>
	 */
	public long getDeleteTimer()
	{
		return _deleteTimer;
	}

	/**
	 * Set the _deleteTimer of the L2PcInstance.<BR><BR>
	 */
	public void setDeleteTimer(long deleteTimer)
	{
		_deleteTimer = deleteTimer;
	}

	/**
	 * Return the current weight of the L2PcInstance.<BR><BR>
	 */
	public int getCurrentLoad()
	{
		return _inventory.getTotalWeight();
	}
	/**
	 * Return date of las update of recomPoints
	 */
	public long getLastRecomUpdate()
	{
		return _lastRecomUpdate;
	}
	public void setLastRecomUpdate(long date)
	{
		_lastRecomUpdate = date;
	}
	/**
	 * Return the number of recommandation obtained by the L2PcInstance.<BR><BR>
	 */
	public int getRecomHave()
	{
		return _recomHave;
	}

	/**
	 * Increment the number of recommandation obtained by the L2PcInstance (Max : 255).<BR><BR>
	 */
	protected void incRecomHave()
	{
		if (_recomHave < 255)
			_recomHave++;
	}

	/**
	 * Set the number of recommandation obtained by the L2PcInstance (Max : 255).<BR><BR>
	 */
	public void setRecomHave(int value)
	{
		if (value > 255)
			_recomHave = 255;
		else if (value < 0)
			_recomHave = 0;
		else
			_recomHave = value;
	}



	/**
	 * Return the number of recommandation that the L2PcInstance can give.<BR><BR>
	 */
	public int getRecomLeft()
	{
		return _recomLeft;
	}

	/**
	 * Increment the number of recommandation that the L2PcInstance can give.<BR><BR>
	 */
	protected void decRecomLeft()
	{
		if (_recomLeft > 0)
			_recomLeft--;
	}

	public void giveRecom(L2PcInstance target)
	{
		if (Config.ALT_RECOMMEND)
		{
			Connection con = null;
			try
			{
				con = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement statement = con.prepareStatement(ADD_CHAR_RECOM);
				statement.setInt(1, getObjectId());
				statement.setInt(2, target.getObjectId());
				statement.execute();
				statement.close();
			}
			catch (Exception e)
			{
				_log.warning("could not update char recommendations:"+e);
			}
			finally
			{
				try { con.close(); } catch (Exception e) {}
			}
		}
		target.incRecomHave();
		decRecomLeft();
		_recomChars.add(target.getObjectId());
	}

	public boolean canRecom(L2PcInstance target)
	{
		return !_recomChars.contains(target.getObjectId());
	}

	/**
	 * Set the exp of the L2PcInstance before a death
	 * @param exp
	 */
	public void setExpBeforeDeath(long exp)
	{
		_expBeforeDeath = exp;
	}

	public long getExpBeforeDeath()
	{
		return _expBeforeDeath;
	}

	/**
	 * Return the Karma of the L2PcInstance.<BR><BR>
	 */
	public int getKarma()
	{
		return _karma;
	}

	/**
	 * Set the Karma of the L2PcInstance and send a Server->Client packet StatusUpdate (broadcast).<BR><BR>
	 */
	public void setKarma(int karma)
	{
		if (karma < 0) karma = 0;
		if (_karma == 0 && karma > 0)
		{
			Collection<L2Object> objs = getKnownList().getKnownObjects().values();
			//synchronized (getKnownList().getKnownObjects())
			{
				for (L2Object object : objs)
				{
					if (!(object instanceof L2GuardInstance))
						continue;
					
					if (((L2GuardInstance) object).getAI().getIntention() == CtrlIntention.AI_INTENTION_IDLE)
						((L2GuardInstance) object).getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE, null);
				}
			}
		}
		else if (_karma > 0 && karma == 0)
		{
			// Send a Server->Client StatusUpdate packet with Karma and PvP Flag to the L2PcInstance and all L2PcInstance to inform (broadcast)
			setKarmaFlag(0);
		}

		_karma = karma;
		broadcastKarma();
	}

	/**
	 * Return the max weight that the L2PcInstance can load.<BR><BR>
	 */
	public int getMaxLoad()
	{
		// Weight Limit = (CON Modifier*69000)*Skills
		// Source http://l2p.bravehost.com/weightlimit.html (May 2007)
		// Fitted exponential curve to the data
		int con = getCON();
		if (con < 1) return 31000;
		if (con > 59) return 176000;
		double baseLoad = Math.pow(1.029993928, con)*30495.627366;
		return (int)calcStat(Stats.MAX_LOAD, baseLoad*Config.ALT_WEIGHT_LIMIT, this, null);
	}

	public int getExpertisePenalty()
	{
		return _expertisePenalty;
	}

	public int getWeightPenalty()
	{
		if (_dietMode)
			return 0;
		return _curWeightPenalty;
	}

	/**
	 * Update the overloaded status of the L2PcInstance.<BR><BR>
	 */
	public void refreshOverloaded()
	{
		int maxLoad = getMaxLoad();
		if (maxLoad > 0)
		{
			setIsOverloaded(getCurrentLoad() > maxLoad);
			int weightproc = getCurrentLoad() * 1000 / maxLoad;
			int newWeightPenalty;
			if (weightproc < 500 || _dietMode)
			{
				newWeightPenalty = 0;
			}
			else if (weightproc < 666)
			{
				newWeightPenalty = 1;
			}
			else if ( weightproc < 800)
			{
				newWeightPenalty = 2;
			}
			else if (weightproc < 1000)
			{
				newWeightPenalty = 3;
			}
			else
			{
				newWeightPenalty = 4;
			}

			if (_curWeightPenalty != newWeightPenalty)
			{
				_curWeightPenalty = newWeightPenalty;
				if (newWeightPenalty > 0 && !_dietMode)
				{
					super.addSkill(SkillTable.getInstance().getInfo(4270,newWeightPenalty));
				}
				else
				{
					super.removeSkill(getKnownSkill(4270));
				}

				sendPacket(new EtcStatusUpdate(this));
				Broadcast.toKnownPlayers(this, new CharInfo(this));
			}
		}
	}

	public void refreshExpertisePenalty()
	{
		int newPenalty = 0;

		for (L2ItemInstance item : getInventory().getItems())
		{
			if (item != null && item.isEquipped())
			{
				int crystaltype = item.getItem().getCrystalType();

				if (crystaltype > newPenalty)
					newPenalty = crystaltype;
			}
		}

		newPenalty = newPenalty - getExpertiseIndex();

		if (newPenalty <= 0)
            newPenalty = 0;

		if (getExpertisePenalty() != newPenalty)
		{
			_expertisePenalty = newPenalty;

			if (newPenalty > 0)
				super.addSkill(SkillTable.getInstance().getInfo(4267, 1)); // level used to be newPenalty
			else
                super.removeSkill(getKnownSkill(4267));

			sendPacket(new EtcStatusUpdate(this));
		}
	}

    public void checkIfWeaponIsAllowed()
    {
        // Override for Gamemasters
        if (isGM())
            return;

        // Iterate through all effects currently on the character.
        for (L2Effect currenteffect : getAllEffects())
        {
            L2Skill effectSkill = currenteffect.getSkill();

            // Ignore all buff skills that are party related (ie. songs, dances) while still remaining weapon dependant on cast though.
            if (!effectSkill.isOffensive() && !(effectSkill.getTargetType() == SkillTargetType.TARGET_PARTY && effectSkill.getSkillType() == SkillType.BUFF))
            {
                // Check to rest to assure current effect meets weapon requirements.
            	if (!effectSkill.getWeaponDependancy(this))
                {
                    sendMessage(effectSkill.getName() + " cannot be used with this weapon.");

                    if (Config.DEBUG)
                        _log.info("   | Skill "+effectSkill.getName()+" has been disabled for ("+getName()+"); Reason: Incompatible Weapon Type.");

                    currenteffect.exit();
                }
            }

            continue;
        }
    }

    public void checkSSMatch(L2ItemInstance equipped, L2ItemInstance unequipped)
    {
    	if (unequipped == null)
    		return;

		if (unequipped.getItem().getType2() == L2Item.TYPE2_WEAPON &&
				(equipped == null ? true : equipped.getItem().getCrystalType() != unequipped.getItem().getCrystalType()))
		{
			for (L2ItemInstance ss : getInventory().getItems())
			{
				int _itemId = ss.getItemId();

				if (((_itemId >= 2509 && _itemId <= 2514) ||
						(_itemId >= 3947 && _itemId <= 3952) ||
						(_itemId <= 1804 && _itemId >= 1808) ||
						_itemId == 5789 || _itemId == 5790 || _itemId == 1835) &&
						ss.getItem().getCrystalType() == unequipped.getItem().getCrystalType())
				{
                    sendPacket(new ExAutoSoulShot(_itemId, 0));

                    SystemMessage sm = new SystemMessage(SystemMessageId.AUTO_USE_OF_S1_CANCELLED);
                    sm.addString(ss.getItemName());
                    sendPacket(sm);
				}
			}
		}
    }

	/**
	 * Return the the PvP Kills of the L2PcInstance (Number of player killed during a PvP).<BR><BR>
	 */
	public int getPvpKills()
	{
		return _pvpKills;
	}

	/**
	 * Set the the PvP Kills of the L2PcInstance (Number of player killed during a PvP).<BR><BR>
	 */
	public void setPvpKills(int pvpKills)
	{
		_pvpKills = pvpKills;
	}

	/**
	 * Return the ClassId object of the L2PcInstance contained in L2PcTemplate.<BR><BR>
	 */
	public ClassId getClassId()
	{
		return getTemplate().classId;
	}

	/**
	 * Set the template of the L2PcInstance.<BR><BR>
	 *
	 * @param Id The Identifier of the L2PcTemplate to set to the L2PcInstance
	 *
	 */
	public void setClassId(int Id)
	{

		if (getLvlJoinedAcademy() != 0 && _clan != null && PlayerClass.values()[Id].getLevel() == ClassLevel.Third)
        {
			if(getLvlJoinedAcademy() <= 16) _clan.setReputationScore(_clan.getReputationScore()+400, true);
            else if(getLvlJoinedAcademy() >= 39) _clan.setReputationScore(_clan.getReputationScore()+170, true);
            else _clan.setReputationScore(_clan.getReputationScore()+(400-(getLvlJoinedAcademy()-16)*10), true);
			_clan.broadcastToOnlineMembers(new PledgeShowInfoUpdate(_clan));
			setLvlJoinedAcademy(0);
            //oust pledge member from the academy, cuz he has finished his 2nd class transfer
            SystemMessage msg = new SystemMessage(SystemMessageId.CLAN_MEMBER_S1_EXPELLED);
            msg.addPcName(this);
            _clan.broadcastToOnlineMembers(msg);
            _clan.broadcastToOnlineMembers(new PledgeShowMemberListDelete(getName()));
            _clan.removeClanMember(getObjectId(), 0);
            sendPacket(new SystemMessage(SystemMessageId.ACADEMY_MEMBERSHIP_TERMINATED));

            // receive graduation gift
            getInventory().addItem("Gift",8181,1,this,null); // give academy circlet
        }
		if (isSubClassActive())
		{
			getSubClasses().get(_classIndex).setClassId(Id);
		}
		setTarget(this);
		broadcastPacket(new MagicSkillUse(this, 5103, 1, 1000, 0));
		setClassTemplate(Id);
		
		// Update class icon in party and clan
		if (isInParty())
			getParty().broadcastToPartyMembers(new PartySmallWindowUpdate(this));

		if (getClan() != null)
			getClan().broadcastToOnlineMembers(new PledgeShowMemberListUpdate(this));
	}

	/** Return the Experience of the L2PcInstance. */
	public long getExp() { return getStat().getExp(); }


	public void setActiveEnchantItem(L2ItemInstance scroll)
	{
		_activeEnchantItem = scroll;
	}

	public L2ItemInstance getActiveEnchantItem()
	{
		return _activeEnchantItem;
	}

	/**
	 * Set the fists weapon of the L2PcInstance (used when no weapon is equiped).<BR><BR>
	 *
	 * @param weaponItem The fists L2Weapon to set to the L2PcInstance
	 *
	 */
	public void setFistsWeaponItem(L2Weapon weaponItem)
	{
		_fistsWeaponItem = weaponItem;
	}

	/**
	 * Return the fists weapon of the L2PcInstance (used when no weapon is equiped).<BR><BR>
	 */
	public L2Weapon getFistsWeaponItem()
	{
		return _fistsWeaponItem;
	}

	/**
	 * Return the fists weapon of the L2PcInstance Class (used when no weapon is equiped).<BR><BR>
	 */
	public L2Weapon findFistsWeaponItem(int classId)
	{
		L2Weapon weaponItem = null;
		if ((classId >= 0x00) && (classId <= 0x09))
		{
			//human fighter fists
			L2Item temp = ItemTable.getInstance().getTemplate(246);
			weaponItem = (L2Weapon)temp;
		}
		else if ((classId >= 0x0a) && (classId <= 0x11))
		{
			//human mage fists
			L2Item temp = ItemTable.getInstance().getTemplate(251);
			weaponItem = (L2Weapon)temp;
		}
		else if ((classId >= 0x12) && (classId <= 0x18))
		{
			//elven fighter fists
			L2Item temp = ItemTable.getInstance().getTemplate(244);
			weaponItem = (L2Weapon)temp;
		}
		else if ((classId >= 0x19) && (classId <= 0x1e))
		{
			//elven mage fists
			L2Item temp = ItemTable.getInstance().getTemplate(249);
			weaponItem = (L2Weapon)temp;
		}
		else if ((classId >= 0x1f) && (classId <= 0x25))
		{
			//dark elven fighter fists
			L2Item temp = ItemTable.getInstance().getTemplate(245);
			weaponItem = (L2Weapon)temp;
		}
		else if ((classId >= 0x26) && (classId <= 0x2b))
		{
			//dark elven mage fists
			L2Item temp = ItemTable.getInstance().getTemplate(250);
			weaponItem = (L2Weapon)temp;
		}
		else if ((classId >= 0x2c) && (classId <= 0x30))
		{
			//orc fighter fists
			L2Item temp = ItemTable.getInstance().getTemplate(248);
			weaponItem = (L2Weapon)temp;
		}
		else if ((classId >= 0x31) && (classId <= 0x34))
		{
			//orc mage fists
			L2Item temp = ItemTable.getInstance().getTemplate(252);
			weaponItem = (L2Weapon)temp;
		}
		else if ((classId >= 0x35) && (classId <= 0x39))
		{
			//dwarven fists
			L2Item temp = ItemTable.getInstance().getTemplate(247);
			weaponItem = (L2Weapon)temp;
		}

		return weaponItem;
	}

	/**
	 * Give Expertise skill of this level and remove beginner Lucky skill.<BR><BR>
	 *
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Get the Level of the L2PcInstance </li>
	 * <li>If L2PcInstance Level is 5, remove beginner Lucky skill </li>
	 * <li>Add the Expertise skill corresponding to its Expertise level</li>
	 * <li>Update the overloaded status of the L2PcInstance</li><BR><BR>
	 *
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T give other free skills (SP needed = 0)</B></FONT><BR><BR>
	 *
	 */
	public void rewardSkills()
	{
		// Get the Level of the L2PcInstance
		int lvl = getLevel();

		// Remove beginner Lucky skill
		if (lvl == 10)
		{
			L2Skill skill = SkillTable.getInstance().getInfo(194, 1);
			skill = removeSkill(skill);

			if (Config.DEBUG && skill != null) _log.fine("removed skill 'Lucky' from "+getName());
		}

		// Calculate the current higher Expertise of the L2PcInstance
		for (int i=0; i < EXPERTISE_LEVELS.length; i++)
		{
			if (lvl >= EXPERTISE_LEVELS[i])
				setExpertiseIndex(i);
		}

		// Add the Expertise skill corresponding to its Expertise level
		if (getExpertiseIndex() > 0)
		{
			L2Skill skill = SkillTable.getInstance().getInfo(239, getExpertiseIndex());
			addSkill(skill, true);

			if (Config.DEBUG) _log.fine("awarded "+getName()+" with new expertise.");

		}
		else
		{
			if (Config.DEBUG) _log.fine("No skills awarded at lvl: "+lvl);
		}

		//Active skill dwarven craft

		if (getSkillLevel(1321) < 1 && getRace() == Race.Dwarf)
		{
			L2Skill skill = SkillTable.getInstance().getInfo(1321,1);
			addSkill(skill, true);
		}

		//Active skill common craft
		if (getSkillLevel(1322) < 1)
		{
			L2Skill skill = SkillTable.getInstance().getInfo(1322,1);
			addSkill(skill, true);
		}

		for(int i = 0; i < COMMON_CRAFT_LEVELS.length; i++)
		{
			if(lvl >= COMMON_CRAFT_LEVELS[i] && getSkillLevel(1320) < (i+1))
			{
				L2Skill skill = SkillTable.getInstance().getInfo(1320, (i+1));
				addSkill(skill, true);
			}
		}

		// Auto-Learn skills if activated
		if (Config.AUTO_LEARN_SKILLS)
		{
		    if (this.isTransformed() || this.isCursedWeaponEquipped())
		        return;
			giveAvailableSkills();
		}
		sendSkillList(); 
		// This function gets called on login, so not such a bad place to check weight
		refreshOverloaded();		// Update the overloaded status of the L2PcInstance
		refreshExpertisePenalty();  // Update the expertise status of the L2PcInstance
	}

	/**
	 * Regive all skills which aren't saved to database, like Noble, Hero, Clan Skills<BR><BR>
	 *
	 */
	public void regiveTemporarySkills()
	{
		// Do not call this on enterworld or char load

		// Add noble skills if noble
		if (isNoble())
			setNoble(true);

		// Add Hero skills if hero
		if (isHero())
			setHero(true);

		// Add clan skills 
		if (getClan() != null)
		{
			L2Clan clan = getClan();
			if (clan.getReputationScore() >= 0)
			{
				L2Skill[] skills = getClan().getAllSkills();
				for (L2Skill sk : skills)
				{
					if(sk.getMinPledgeClass() <= getPledgeClass())
						addSkill(sk, false);
				}
			}
			if (clan.getLevel() >= SiegeManager.getInstance().getSiegeClanMinLevel() &&
					isClanLeader())
				SiegeManager.getInstance().addSiegeSkills(this);
		}
		
		// Reload passive skills from armors / jewels / weapons
		getInventory().reloadEquippedItems();
		
		// Add Death Penalty Buff Level
		restoreDeathPenaltyBuffLevel();
	}

	/**
	 * Give all available skills to the player.<br><br>
	 *
	 */
	private void giveAvailableSkills()
	{
		int unLearnable = 0;
		int skillCounter = 0;

		// Get available skills
		L2SkillLearn[] skills = SkillTreeTable.getInstance().getAvailableSkills(this, getClassId());
		while (skills.length > unLearnable)
		{
			for (int i = 0; i < skills.length; i++)
			{
				L2SkillLearn s = skills[i];
				L2Skill sk = SkillTable.getInstance().getInfo(s.getId(), s.getLevel());
				if (sk == null || !sk.getCanLearn(getClassId()) || (sk.getId() == L2Skill.SKILL_DIVINE_INSPIRATION && !Config.AUTO_LEARN_DIVINE_INSPIRATION))
				{
					unLearnable++;
					continue;
				}

				if (getSkillLevel(sk.getId()) == -1)
					skillCounter++;

				addSkill(sk, true);
			}

			// Get new available skills
			skills = SkillTreeTable.getInstance().getAvailableSkills(this, getClassId());
		}

		sendMessage("You have learned " + skillCounter + " new skills.");
	}

	/** Set the Experience value of the L2PcInstance. */
	public void setExp(long exp) { getStat().setExp(exp); }

	/**
	 * Return the Race object of the L2PcInstance.<BR><BR>
	 */
	public Race getRace()
	{
		if (!isSubClassActive())
			return getTemplate().race;

		L2PcTemplate charTemp = CharTemplateTable.getInstance().getTemplate(_baseClass);
		return charTemp.race;
	}

    public L2Radar getRadar()
    {
        return _radar;
    }

	/** Return the SP amount of the L2PcInstance. */
	public int getSp() { return getStat().getSp(); }

	/** Set the SP amount of the L2PcInstance. */
	public void setSp(int sp) { super.getStat().setSp(sp); }

	/**
	 * Return true if this L2PcInstance is a clan leader in
	 * ownership of the passed castle
	 */
	public boolean isCastleLord(int castleId)
	{
		L2Clan clan = getClan();

		// player has clan and is the clan leader, check the castle info
		if ((clan != null) && (clan.getLeader().getPlayerInstance() == this))
		{
			// if the clan has a castle and it is actually the queried castle, return true
			Castle castle = CastleManager.getInstance().getCastleByOwner(clan);
			if ((castle != null) && (castle == CastleManager.getInstance().getCastleById(castleId)))
				return true;
		}

		return false;
	}
	/**
	 * Return the Clan Identifier of the L2PcInstance.<BR><BR>
	 */
	public int getClanId()
	{
		return _clanId;
	}

	/**
	 * Return the Clan Crest Identifier of the L2PcInstance or 0.<BR><BR>
	 */
	public int getClanCrestId()
	{
		if (_clan != null && _clan.hasCrest())
			return _clan.getCrestId();

		return 0;
	}

	/**
	 * @return The Clan CrestLarge Identifier or 0
	 */
	public int getClanCrestLargeId()
	{
		if (_clan != null && _clan.hasCrestLarge())
			return _clan.getCrestLargeId();

		return 0;
	}

	public long getClanJoinExpiryTime()
	{
		return _clanJoinExpiryTime;
	}

	public void setClanJoinExpiryTime(long time)
	{
		_clanJoinExpiryTime = time;
	}

	public long getClanCreateExpiryTime()
	{
		return _clanCreateExpiryTime;
	}

	public void setClanCreateExpiryTime(long time)
	{
		_clanCreateExpiryTime = time;
	}

	public void setOnlineTime(long time)
	{
		_onlineTime = time;
		_onlineBeginTime = System.currentTimeMillis();
	}

	/**
	 * Return the PcInventory Inventory of the L2PcInstance contained in _inventory.<BR><BR>
	 */
	@Override
	public PcInventory getInventory()
	{
		return _inventory;
	}

	/**
	 * Delete a ShortCut of the L2PcInstance _shortCuts.<BR><BR>
	 */
	public void removeItemFromShortCut(int objectId)
	{
		_shortCuts.deleteShortCutByObjectId(objectId);
	}

	/**
	 * Return True if the L2PcInstance is sitting.<BR><BR>
	 */
	public boolean isSitting()
	{
		return _waitTypeSitting;
	}

	/**
	 * Set _waitTypeSitting to given value
	 */
	public void setIsSitting(boolean state)
	{
		_waitTypeSitting = state;
	}
	/**
	 * Sit down the L2PcInstance, set the AI Intention to AI_INTENTION_REST and send a Server->Client ChangeWaitType packet (broadcast)<BR><BR>
	 */
	public void sitDown()
	{
		if (isCastingNow() && !_relax)
		{
			sendMessage("Cannot sit while casting");
			return;
		}

		if (!_waitTypeSitting && !isAttackingDisabled() && !isOutOfControl() && !isImmobilized())
		{
			breakAttack();
			setIsSitting(true);
			broadcastPacket(new ChangeWaitType (this, ChangeWaitType.WT_SITTING));
			// Schedule a sit down task to wait for the animation to finish
			ThreadPoolManager.getInstance().scheduleGeneral(new SitDownTask(this), 2500);
			setIsParalyzed(true);
		}
	}
	/**
	 * Sit down Task
	 */
	class SitDownTask implements Runnable
	{
		L2PcInstance _player;
		SitDownTask(L2PcInstance player)
		{
			_player = player;
		}
		public void run()
		{
			_player.setIsParalyzed(false);
			_player.getAI().setIntention(CtrlIntention.AI_INTENTION_REST);
		}
	}
	/**
	 * Stand up Task
	 */
	class StandUpTask implements Runnable
	{
		L2PcInstance _player;
		StandUpTask(L2PcInstance player)
		{
			_player = player;
		}
		public void run()
		{
			_player.setIsSitting(false);
			_player.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
		}
	}
	/**
	 * Stand up the L2PcInstance, set the AI Intention to AI_INTENTION_IDLE and send a Server->Client ChangeWaitType packet (broadcast)<BR><BR>
	 */
	public void standUp()
	{
		if (L2Event.active && eventSitForced)
		{
			sendMessage("A dark force beyond your mortal understanding makes your knees to shake when you try to stand up ...");
		}
		else if (_waitTypeSitting && !isInStoreMode() && !isAlikeDead())
		{
			if (_relax)
			{
				setRelax(false);
				stopEffects(L2Effect.EffectType.RELAXING);
			}

			broadcastPacket(new ChangeWaitType (this, ChangeWaitType.WT_STANDING));
			// Schedule a stand up task to wait for the animation to finish
			ThreadPoolManager.getInstance().scheduleGeneral(new StandUpTask(this), 2500);
		}
	}

	/**
	 * Set the value of the _relax value. Must be True if using skill Relax and False if not.
	 */
	public void setRelax(boolean val)
	{
		_relax = val;
	}

	/**
	 * Return the PcWarehouse object of the L2PcInstance.<BR><BR>
	 */
	public PcWarehouse getWarehouse()
	{
		if (_warehouse == null)
		{
			_warehouse = new PcWarehouse(this);
			_warehouse.restore();
		}
		if (Config.WAREHOUSE_CACHE)
			WarehouseCacheManager.getInstance().addCacheTask(this);
		return _warehouse;
	}

	/**
	 * Free memory used by Warehouse
	 */
	public void clearWarehouse()
	{
		if (_warehouse != null)
			_warehouse.deleteMe();
		_warehouse = null;
	}

	/**
	 * Return the PcFreight object of the L2PcInstance.<BR><BR>
	 */
	public PcFreight getFreight()
	{
		return _freight;
	}

	/**
	 * Return the Identifier of the L2PcInstance.<BR><BR>
	 */
	public int getCharId()
	{
		return _charId;
	}

	/**
	 * Set the Identifier of the L2PcInstance.<BR><BR>
	 */
	public void setCharId(int charId)
	{
		_charId = charId;
	}

	/**
	 * Return the Adena amount of the L2PcInstance.<BR><BR>
	 */
	public int getAdena()
	{
		return _inventory.getAdena();
	}

    /**
     * Return the Ancient Adena amount of the L2PcInstance.<BR><BR>
     */
	public int getAncientAdena()
	{
	    return _inventory.getAncientAdena();
	}

	/**
	 * Add adena to Inventory of the L2PcInstance and send a Server->Client InventoryUpdate packet to the L2PcInstance.
	 * @param process : String Identifier of process triggering this action
	 * @param count : int Quantity of adena to be added
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 */
	public void addAdena(String process, int count, L2Object reference, boolean sendMessage)
	{
		if (sendMessage)
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.EARNED_ADENA);
			sm.addNumber(count);
			sendPacket(sm);
		}

		if (count > 0)
		{
			_inventory.addAdena(process, count, this, reference);

			// Send update packet
			if (!Config.FORCE_INVENTORY_UPDATE)
			{
				InventoryUpdate iu = new InventoryUpdate();
				iu.addItem(_inventory.getAdenaInstance());
				sendPacket(iu);
			}
			else sendPacket(new ItemList(this, false));
		}
	}

	/**
	 * Reduce adena in Inventory of the L2PcInstance and send a Server->Client InventoryUpdate packet to the L2PcInstance.
	 * @param process : String Identifier of process triggering this action
	 * @param count : int Quantity of adena to be reduced
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 * @return boolean informing if the action was successfull
	 */
	public boolean reduceAdena(String process, int count, L2Object reference, boolean sendMessage)
	{
		if (count > getAdena())
		{
			if (sendMessage) sendPacket(new SystemMessage(SystemMessageId.YOU_NOT_ENOUGH_ADENA));
			return false;
		}

		if (count > 0)
		{
			L2ItemInstance adenaItem = _inventory.getAdenaInstance();
			_inventory.reduceAdena(process, count, this, reference);

			// Send update packet
			if (!Config.FORCE_INVENTORY_UPDATE)
			{
				InventoryUpdate iu = new InventoryUpdate();
				iu.addItem(adenaItem);
				sendPacket(iu);
			}
			else sendPacket(new ItemList(this, false));

			if (sendMessage)
			{
				SystemMessage sm = new SystemMessage(SystemMessageId.DISAPPEARED_ADENA);
				sm.addNumber(count);
				sendPacket(sm);
			}
		}

		return true;
	}

	/**
	 * Add ancient adena to Inventory of the L2PcInstance and send a Server->Client InventoryUpdate packet to the L2PcInstance.
	 *
	 * @param process : String Identifier of process triggering this action
	 * @param count : int Quantity of ancient adena to be added
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 */
	public void addAncientAdena(String process, int count, L2Object reference, boolean sendMessage)
	{
	    if (sendMessage)
	    {
	        SystemMessage sm = new SystemMessage(SystemMessageId.EARNED_S2_S1_S);
	        sm.addItemName(PcInventory.ANCIENT_ADENA_ID);
            sm.addNumber(count);
	        sendPacket(sm);
	    }

	    if (count > 0)
	    {
	        _inventory.addAncientAdena(process, count, this, reference);

	        if (!Config.FORCE_INVENTORY_UPDATE)
	        {
	            InventoryUpdate iu = new InventoryUpdate();
	            iu.addItem(_inventory.getAncientAdenaInstance());
	            sendPacket(iu);
	        }
	        else sendPacket(new ItemList(this, false));
	    }
	}

	/**
	 * Reduce ancient adena in Inventory of the L2PcInstance and send a Server->Client InventoryUpdate packet to the L2PcInstance.
	 * @param process : String Identifier of process triggering this action
	 * @param count : int Quantity of ancient adena to be reduced
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 * @return boolean informing if the action was successfull
	 */
	public boolean reduceAncientAdena(String process, int count, L2Object reference, boolean sendMessage)
	{
	    if (count > getAncientAdena())
	    {
	        if (sendMessage)
	            sendPacket(new SystemMessage(SystemMessageId.YOU_NOT_ENOUGH_ADENA));

	        return false;
	    }

	    if (count > 0)
	    {
	        L2ItemInstance ancientAdenaItem = _inventory.getAncientAdenaInstance();
	        _inventory.reduceAncientAdena(process, count, this, reference);

	        if (!Config.FORCE_INVENTORY_UPDATE)
	        {
	            InventoryUpdate iu = new InventoryUpdate();
	            iu.addItem(ancientAdenaItem);
	            sendPacket(iu);
	        }
	        else
	        {
	            sendPacket(new ItemList(this, false));
	        }

	        if (sendMessage)
	        {
	            SystemMessage sm = new SystemMessage(SystemMessageId.S2_S1_DISAPPEARED);
	            sm.addItemName(PcInventory.ANCIENT_ADENA_ID);
	            sm.addNumber(count);
	            sendPacket(sm);
	        }
	    }

	    return true;
	}

	/**
	 * Adds item to inventory and send a Server->Client InventoryUpdate packet to the L2PcInstance.
	 * @param process : String Identifier of process triggering this action
	 * @param item : L2ItemInstance to be added
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 */
	public void addItem(String process, L2ItemInstance item, L2Object reference, boolean sendMessage)
	{
		if (item.getCount() > 0)
		{
			// Sends message to client if requested
			if (sendMessage)
			{
				if (item.getCount() > 1)
				{
					SystemMessage sm = new SystemMessage(SystemMessageId.YOU_PICKED_UP_S1_S2);
					sm.addItemName(item);
					sm.addNumber(item.getCount());
					sendPacket(sm);
				}
				else if (item.getEnchantLevel() > 0)
				{
					SystemMessage sm = new SystemMessage(SystemMessageId.YOU_PICKED_UP_A_S1_S2);
					sm.addNumber(item.getEnchantLevel());
					sm.addItemName(item);
					sendPacket(sm);
				}
				else
				{
					SystemMessage sm = new SystemMessage(SystemMessageId.YOU_PICKED_UP_S1);
					sm.addItemName(item);
					sendPacket(sm);
				}
			}

			// Add the item to inventory
			L2ItemInstance newitem = _inventory.addItem(process, item, this, reference);

			// Send inventory update packet
			if (!Config.FORCE_INVENTORY_UPDATE)
			{
				InventoryUpdate playerIU = new InventoryUpdate();
				playerIU.addItem(newitem);
				sendPacket(playerIU);
			}
			else
			{
			    sendPacket(new ItemList(this, false));
			}

			// Update current load as well
			StatusUpdate su = new StatusUpdate(getObjectId());
			su.addAttribute(StatusUpdate.CUR_LOAD, getCurrentLoad());
			sendPacket(su);

            // If over capacity, drop the item
            if (!isGM() && !_inventory.validateCapacity(0))
                dropItem("InvDrop", newitem, null, true);

			// Cursed Weapon
            else if(CursedWeaponsManager.getInstance().isCursed(newitem.getItemId()))
			{
				CursedWeaponsManager.getInstance().activate(this, newitem);
			}
			
			// Combat Flag
            else if(FortSiegeManager.getInstance().isCombat(item.getItemId()))
                FortSiegeManager.getInstance().activateCombatFlag(this, item);

		}
	}

	/**
	 * Adds item to Inventory and send a Server->Client InventoryUpdate packet to the L2PcInstance.
	 * @param process : String Identifier of process triggering this action
	 * @param itemId : int Item Identifier of the item to be added
	 * @param count : int Quantity of items to be added
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 */
	public void addItem(String process, int itemId, int count, L2Object reference, boolean sendMessage)
	{
		if (count > 0)
		{
			L2ItemInstance item = ItemTable.getInstance().createDummyItem(itemId);
			// Sends message to client if requested
			if (sendMessage && ((!isCastingNow() && item.getItemType() == L2EtcItemType.HERB)
					|| item.getItemType() != L2EtcItemType.HERB))
			{
				if (count > 1)
				{
					if (process.equalsIgnoreCase("sweep") || process.equalsIgnoreCase("Quest"))
					{
						SystemMessage sm = new SystemMessage(SystemMessageId.EARNED_S2_S1_S);
						sm.addItemName(itemId);
						sm.addNumber(count);
						sendPacket(sm);
					}
					else
					{
						SystemMessage sm = new SystemMessage(SystemMessageId.YOU_PICKED_UP_S1_S2);
						sm.addItemName(itemId);
						sm.addNumber(count);
						sendPacket(sm);
					}
				}
				else
				{
					if (process.equalsIgnoreCase("sweep") || process.equalsIgnoreCase("Quest"))
					{
						SystemMessage sm = new SystemMessage(SystemMessageId.EARNED_ITEM);
						sm.addItemName(itemId);
						sendPacket(sm);
					}
					else
					{
						SystemMessage sm = new SystemMessage(SystemMessageId.YOU_PICKED_UP_S1);
						sm.addItemName(itemId);
						sendPacket(sm);
					}
				}
			}
			//Auto use herbs - autoloot
			if (item.getItemType() == L2EtcItemType.HERB) //If item is herb dont add it to iv :]
			{
				if(!isCastingNow())
				{
					L2ItemInstance herb = new L2ItemInstance(_charId, itemId);
					IItemHandler handler = ItemHandler.getInstance().getItemHandler(herb.getItemId());
					if (handler == null)
						_log.warning("No item handler registered for Herb - item ID " + herb.getItemId() + ".");
					else
					{
						handler.useItem(this, herb);
						if(_herbstask>=100)
							_herbstask -=100;
					}
				}
				else
				{
					_herbstask += 100;
					ThreadPoolManager.getInstance().scheduleAi(new HerbTask(process, itemId, count, reference, sendMessage), _herbstask);
				}
			}
			else
			{
				// Add the item to inventory
				L2ItemInstance createdItem = _inventory.addItem(process, itemId, count, this, reference);

				// Send inventory update packet
				if (!Config.FORCE_INVENTORY_UPDATE)
				{
					InventoryUpdate playerIU = new InventoryUpdate();
					playerIU.addItem(createdItem);
					sendPacket(playerIU);
				}
				else
					sendPacket(new ItemList(this, false));

				// Update current load as well
				StatusUpdate su = new StatusUpdate(getObjectId());
				su.addAttribute(StatusUpdate.CUR_LOAD, getCurrentLoad());
				sendPacket(su);

				// If over capacity, drop the item
				if (!isGM() && !_inventory.validateCapacity(0))
					dropItem("InvDrop", createdItem, null, true);

				// Cursed Weapon
				else if(CursedWeaponsManager.getInstance().isCursed(createdItem.getItemId()))
					CursedWeaponsManager.getInstance().activate(this, createdItem);

				// Combat Flag
				else if(FortSiegeManager.getInstance().isCombat(createdItem.getItemId()))
					FortSiegeManager.getInstance().activateCombatFlag(this, createdItem);
			}
		}
	}

	/**
	 * Destroy item from inventory and send a Server->Client InventoryUpdate packet to the L2PcInstance.
	 * @param process : String Identifier of process triggering this action
	 * @param item : L2ItemInstance to be destroyed
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 * @return boolean informing if the action was successfull
	 */
	public boolean destroyItem(String process, L2ItemInstance item, L2Object reference, boolean sendMessage)
	{
		return this.destroyItem(process, item, item.getCount(), reference, sendMessage);
	}
    
    /**
     * Destroy item from inventory and send a Server->Client InventoryUpdate packet to the L2PcInstance.
     * @param process : String Identifier of process triggering this action
     * @param item : L2ItemInstance to be destroyed
     * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
     * @param sendMessage : boolean Specifies whether to send message to Client about this action
     * @return boolean informing if the action was successfull
     */
    public boolean destroyItem(String process, L2ItemInstance item, int count, L2Object reference, boolean sendMessage)
    {
        item = _inventory.destroyItem(process, item, count, this, reference);

        if (item == null)
        {
            if (sendMessage)
            {
                sendPacket(new SystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
            }
            return false;
        }

        // Send inventory update packet
        if (!Config.FORCE_INVENTORY_UPDATE)
        {
            InventoryUpdate playerIU = new InventoryUpdate();
            playerIU.addItem(item);
            sendPacket(playerIU);
        }
        else 
        {
            sendPacket(new ItemList(this, false));
        }

        // Update current load as well
        StatusUpdate su = new StatusUpdate(getObjectId());
        su.addAttribute(StatusUpdate.CUR_LOAD, getCurrentLoad());
        sendPacket(su);

        // Sends message to client if requested
        if (sendMessage)
        {
            SystemMessage sm = new SystemMessage(SystemMessageId.S2_S1_DISAPPEARED);
            sm.addItemName(item);
            sm.addNumber(count);
            sendPacket(sm);
        }

        return true;
    }

	/**
	 * Destroys item from inventory and send a Server->Client InventoryUpdate packet to the L2PcInstance.
	 * @param process : String Identifier of process triggering this action
	 * @param objectId : int Item Instance identifier of the item to be destroyed
	 * @param count : int Quantity of items to be destroyed
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 * @return boolean informing if the action was successfull
	 */
	@Override
	public boolean destroyItem(String process, int objectId, int count, L2Object reference, boolean sendMessage)
	{
        L2ItemInstance item = _inventory.getItemByObjectId(objectId);

		if (item == null)
		{
			if (sendMessage)
            {
                sendPacket(new SystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
            }
            
			return false;
		}
        return this.destroyItem(process, item, count, reference, sendMessage);
	}

	/**
	 * Destroys shots from inventory without logging and only occasional saving to database.
	 * Sends a Server->Client InventoryUpdate packet to the L2PcInstance.
	 * @param process : String Identifier of process triggering this action
	 * @param objectId : int Item Instance identifier of the item to be destroyed
	 * @param count : int Quantity of items to be destroyed
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 * @return boolean informing if the action was successfull
	 */
	public boolean destroyItemWithoutTrace(String process, int objectId, int count, L2Object reference, boolean sendMessage)
	{
        L2ItemInstance item = _inventory.getItemByObjectId(objectId);

        if (item == null || item.getCount() < count)
		{
			if (sendMessage)
            {
                sendPacket(new SystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
            }
            
			return false;
		}
        
        return this.destroyItem(null, item, count, reference, sendMessage);
	}

	/**
	 * Destroy item from inventory by using its <B>itemId</B> and send a Server->Client InventoryUpdate packet to the L2PcInstance.
	 * @param process : String Identifier of process triggering this action
	 * @param itemId : int Item identifier of the item to be destroyed
	 * @param count : int Quantity of items to be destroyed
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 * @return boolean informing if the action was successfull
	 */
	@Override
	public boolean destroyItemByItemId(String process, int itemId, int count, L2Object reference, boolean sendMessage)
	{
        L2ItemInstance item = _inventory.getItemByItemId(itemId);

        if (item == null || item.getCount() < count || _inventory.destroyItemByItemId(process, itemId, count, this, reference) == null)
		{
			if (sendMessage)
                sendPacket(new SystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));

			return false;
		}

		// Send inventory update packet
		if (!Config.FORCE_INVENTORY_UPDATE)
		{
			InventoryUpdate playerIU = new InventoryUpdate();
			playerIU.addItem(item);
			sendPacket(playerIU);
		}
        else sendPacket(new ItemList(this, false));

		// Update current load as well
		StatusUpdate su = new StatusUpdate(getObjectId());
		su.addAttribute(StatusUpdate.CUR_LOAD, getCurrentLoad());
		sendPacket(su);

		// Sends message to client if requested
		if (sendMessage)
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.S2_S1_DISAPPEARED);
            sm.addItemName(itemId);
            sm.addNumber(count);
			sendPacket(sm);
		}

		return true;
	}

	/**
	 * Destroy all weared items from inventory and send a Server->Client InventoryUpdate packet to the L2PcInstance.
	 * @param process : String Identifier of process triggering this action
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 * @return boolean informing if the action was successfull
	 */
	public void destroyWearedItems(String process, L2Object reference, boolean sendMessage)
	{

        // Go through all Items of the inventory
        for (L2ItemInstance item : getInventory().getItems())
		{
            // Check if the item is a Try On item in order to remove it
			if (item.isWear())
			{
                if (item.isEquipped())
                    getInventory().unEquipItemInSlotAndRecord(item.getLocationSlot());

				if (_inventory.destroyItem(process, item, this, reference) == null)
				{
					_log.warning("Player " + getName() + " can't destroy weared item: " + item.getName() + "[ " + item.getObjectId() + " ]");
					continue;
				}

				// Send an Unequipped Message in system window of the player for each Item
				SystemMessage sm = new SystemMessage(SystemMessageId.S1_DISARMED);
				sm.addItemName(item);
				sendPacket(sm);

			}
		}


        // Send the StatusUpdate Server->Client Packet to the player with new CUR_LOAD (0x0e) information
		StatusUpdate su = new StatusUpdate(getObjectId());
		su.addAttribute(StatusUpdate.CUR_LOAD, getCurrentLoad());
		sendPacket(su);

        // Send the ItemList Server->Client Packet to the player in order to refresh its Inventory
        ItemList il = new ItemList(getInventory().getItems(), true);
        sendPacket(il);

        // Send a Server->Client packet UserInfo to this L2PcInstance and CharInfo to all L2PcInstance in its _KnownPlayers
		broadcastUserInfo();

		// Sends message to client if requested
		sendMessage("Trying-on mode has ended.");

	}

	/**
	 * Transfers item to another ItemContainer and send a Server->Client InventoryUpdate packet to the L2PcInstance.
	 * @param process : String Identifier of process triggering this action
	 * @param itemId : int Item Identifier of the item to be transfered
	 * @param count : int Quantity of items to be transfered
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @return L2ItemInstance corresponding to the new item or the updated item in inventory
	 */
	public L2ItemInstance transferItem(String process, int objectId, int count, Inventory target, L2Object reference)
	{
		L2ItemInstance oldItem = checkItemManipulation(objectId, count, "transfer");
		if (oldItem == null) return null;
		L2ItemInstance newItem = getInventory().transferItem(process, objectId, count, target, this, reference);
		if (newItem == null) return null;

		// Send inventory update packet
		if (!Config.FORCE_INVENTORY_UPDATE)
		{
			InventoryUpdate playerIU = new InventoryUpdate();

			if (oldItem.getCount() > 0 && oldItem != newItem)
                playerIU.addModifiedItem(oldItem);
			else
                playerIU.addRemovedItem(oldItem);

			sendPacket(playerIU);
		}
		else sendPacket(new ItemList(this, false));

		// Update current load as well
		StatusUpdate playerSU = new StatusUpdate(getObjectId());
		playerSU.addAttribute(StatusUpdate.CUR_LOAD, getCurrentLoad());
		sendPacket(playerSU);

		// Send target update packet
		if (target instanceof PcInventory)
		{
			L2PcInstance targetPlayer = ((PcInventory)target).getOwner();

			if (!Config.FORCE_INVENTORY_UPDATE)
			{
				InventoryUpdate playerIU = new InventoryUpdate();

				if (newItem.getCount() > count)
                    playerIU.addModifiedItem(newItem);
				else
                    playerIU.addNewItem(newItem);

				targetPlayer.sendPacket(playerIU);
			}
			else targetPlayer.sendPacket(new ItemList(targetPlayer, false));

			// Update current load as well
			playerSU = new StatusUpdate(targetPlayer.getObjectId());
			playerSU.addAttribute(StatusUpdate.CUR_LOAD, targetPlayer.getCurrentLoad());
			targetPlayer.sendPacket(playerSU);
		}
		else if (target instanceof PetInventory)
		{
			PetInventoryUpdate petIU = new PetInventoryUpdate();

			if (newItem.getCount() > count)
                petIU.addModifiedItem(newItem);
			else
                petIU.addNewItem(newItem);

			((PetInventory)target).getOwner().getOwner().sendPacket(petIU);
		}

		return newItem;
	}

	/**
	 * Drop item from inventory and send a Server->Client InventoryUpdate packet to the L2PcInstance.
	 * @param process : String Identifier of process triggering this action
	 * @param item : L2ItemInstance to be dropped
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 * @return boolean informing if the action was successfull
	 */
	public boolean dropItem(String process, L2ItemInstance item, L2Object reference, boolean sendMessage)
	{
		item = _inventory.dropItem(process, item, this, reference);

		if (item == null)
		{
			if (sendMessage)
                sendPacket(new SystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));

			return false;
		}

		item.dropMe(this, getClientX() + Rnd.get(50) - 25, getClientY() + Rnd.get(50) - 25, getClientZ() + 20);

		 if (Config.AUTODESTROY_ITEM_AFTER >0 && Config.DESTROY_DROPPED_PLAYER_ITEM && !Config.LIST_PROTECTED_ITEMS.contains(item.getItemId()))
			 {
			 if ( (item.isEquipable() && Config.DESTROY_EQUIPABLE_PLAYER_ITEM) || !item.isEquipable())
			 ItemsAutoDestroy.getInstance().addItem(item);
			 }
		 if (Config.DESTROY_DROPPED_PLAYER_ITEM){
		 	if (!item.isEquipable() || (item.isEquipable()  && Config.DESTROY_EQUIPABLE_PLAYER_ITEM ))
		 		item.setProtected(false);
			else
				item.setProtected(true);
		 	}
	   		else
	   			item.setProtected(true);

		// Send inventory update packet
		if (!Config.FORCE_INVENTORY_UPDATE)
		{
			InventoryUpdate playerIU = new InventoryUpdate();
			playerIU.addItem(item);
			sendPacket(playerIU);
		}
		else sendPacket(new ItemList(this, false));

		// Update current load as well
		StatusUpdate su = new StatusUpdate(getObjectId());
		su.addAttribute(StatusUpdate.CUR_LOAD, getCurrentLoad());
		sendPacket(su);

		// Sends message to client if requested
		if (sendMessage)
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.YOU_DROPPED_S1);
			sm.addItemName(item);
			sendPacket(sm);
		}

		return true;
	}

	/**
	 * Drop item from inventory by using its <B>objectID</B> and send a Server->Client InventoryUpdate packet to the L2PcInstance.
	 * @param process : String Identifier of process triggering this action
	 * @param objectId : int Item Instance identifier of the item to be dropped
	 * @param count : int Quantity of items to be dropped
	 * @param x : int coordinate for drop X
	 * @param y : int coordinate for drop Y
	 * @param z : int coordinate for drop Z
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 * @return L2ItemInstance corresponding to the new item or the updated item in inventory
	 */
	public L2ItemInstance dropItem(String process, int objectId, int count, int x, int y, int z, L2Object reference, boolean sendMessage)
	{
		L2ItemInstance invitem = _inventory.getItemByObjectId(objectId);
		L2ItemInstance item = _inventory.dropItem(process, objectId, count, this, reference);

		if (item == null)
		{
			if (sendMessage)
                sendPacket(new SystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));

			return null;
		}

		item.dropMe(this, x, y, z);

		 if (Config.AUTODESTROY_ITEM_AFTER >0 && Config.DESTROY_DROPPED_PLAYER_ITEM && !Config.LIST_PROTECTED_ITEMS.contains(item.getItemId()))
			 {
			 if ( (item.isEquipable() && Config.DESTROY_EQUIPABLE_PLAYER_ITEM) || !item.isEquipable())
			 ItemsAutoDestroy.getInstance().addItem(item);
			 }
		 if (Config.DESTROY_DROPPED_PLAYER_ITEM){
		 	if (!item.isEquipable() || (item.isEquipable()  && Config.DESTROY_EQUIPABLE_PLAYER_ITEM ))
		 		item.setProtected(false);
		 	else
		 		item.setProtected(true);
		 }
	   	 else
	   		 item.setProtected(true);

		// Send inventory update packet
		if (!Config.FORCE_INVENTORY_UPDATE)
		{
			InventoryUpdate playerIU = new InventoryUpdate();
			playerIU.addItem(invitem);
			sendPacket(playerIU);
		}
		else
		{
		    sendPacket(new ItemList(this, false));
		}

		// Update current load as well
		StatusUpdate su = new StatusUpdate(getObjectId());
		su.addAttribute(StatusUpdate.CUR_LOAD, getCurrentLoad());
		sendPacket(su);

		// Sends message to client if requested
		if (sendMessage)
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.YOU_DROPPED_S1);
			sm.addItemName(item);
			sendPacket(sm);
		}

		return item;
	}

	public L2ItemInstance checkItemManipulation(int objectId, int count, String action)
	{
        //TODO: if we remove objects that are not visisble from the L2World, we'll have to remove this check
		if (L2World.getInstance().findObject(objectId) == null)
		{
			_log.finest(getObjectId()+": player tried to " + action + " item not available in L2World");
			return null;
		}

		L2ItemInstance item = getInventory().getItemByObjectId(objectId);

		if (item == null || item.getOwnerId() != getObjectId())
		{
			_log.finest(getObjectId()+": player tried to " + action + " item he is not owner of");
			return null;
		}

		if (count < 0 || (count > 1 && !item.isStackable()))
		{
			_log.finest(getObjectId()+": player tried to " + action + " item with invalid count: "+ count);
			return null;
		}

		if (count > item.getCount())
		{
			_log.finest(getObjectId()+": player tried to " + action + " more items than he owns");
			return null;
		}

		// Pet is summoned and not the item that summoned the pet AND not the buggle from strider you're mounting
		if (getPet() != null && getPet().getControlItemId() == objectId || getMountObjectID() == objectId)
		{
			if (Config.DEBUG)
				_log.finest(getObjectId()+": player tried to " + action + " item controling pet");

			return null;
		}

		if (getActiveEnchantItem() != null && getActiveEnchantItem().getObjectId() == objectId)
		{
			if (Config.DEBUG)
				_log.finest(getObjectId()+":player tried to " + action + " an enchant scroll he was using");

			return null;
		}

		if (item.isWear())
		{
			// cannot drop/trade wear-items
			return null;
		}
		
		// We cannot put a Weapon with Augmention in WH while casting (Possible Exploit)
		if (item.isAugmented() && isCastingNow())
			return null;

		return item;
	}

	/**
	 * Set _protectEndTime according settings.
	 */
	public void setProtection(boolean protect)
	{
		if (Config.DEVELOPER && (protect || _protectEndTime > 0))
            _log.warning(getName() + ": Protection " + (protect?"ON " + (GameTimeController.getGameTicks() + Config.PLAYER_SPAWN_PROTECTION * GameTimeController.TICKS_PER_SECOND) :"OFF") + " (currently " + GameTimeController.getGameTicks() + ")");

		_protectEndTime = protect ? GameTimeController.getGameTicks() + Config.PLAYER_SPAWN_PROTECTION * GameTimeController.TICKS_PER_SECOND : 0;
	}

	/**
	 * Set protection from agro mobs when getting up from fake death, according settings.
	 */
	public void setRecentFakeDeath(boolean protect)
	{
		_recentFakeDeathEndTime = protect ? GameTimeController.getGameTicks() + Config.PLAYER_FAKEDEATH_UP_PROTECTION * GameTimeController.TICKS_PER_SECOND : 0;
	}

	public boolean isRecentFakeDeath()
	{
		return _recentFakeDeathEndTime > GameTimeController.getGameTicks();
	}

	/**
	 * Get the client owner of this char.<BR><BR>
	 */
	public L2GameClient getClient()
	{
		return _client;
	}

	public void setClient(L2GameClient client)
	{
		_client = client;
	}

	/**
	 * Close the active connection with the client.<BR><BR>
	 */
	public void closeNetConnection()
	{
		L2GameClient client = _client;
		if (client != null && !client.getConnection().isClosed())
		{
			client.close(new LeaveWorld());
		}
	}
	
	public Point3D getCurrentSkillWorldPosition()
	{
		return _currentSkillWorldPosition;
	}
	 
	public void setCurrentSkillWorldPosition(Point3D worldPosition)
	{
		_currentSkillWorldPosition = worldPosition;
	}

	/**
	 * Manage actions when a player click on this L2PcInstance.<BR><BR>
	 *
	 * <B><U> Actions on first click on the L2PcInstance (Select it)</U> :</B><BR><BR>
	 * <li>Set the target of the player</li>
	 * <li>Send a Server->Client packet MyTargetSelected to the player (display the select window)</li><BR><BR>
	 *
	 * <B><U> Actions on second click on the L2PcInstance (Follow it/Attack it/Intercat with it)</U> :</B><BR><BR>
	 * <li>Send a Server->Client packet MyTargetSelected to the player (display the select window)</li>
	 * <li>If this L2PcInstance has a Private Store, notify the player AI with AI_INTENTION_INTERACT</li>
	 * <li>If this L2PcInstance is autoAttackable, notify the player AI with AI_INTENTION_ATTACK</li><BR><BR>
	 * <li>If this L2PcInstance is NOT autoAttackable, notify the player AI with AI_INTENTION_FOLLOW</li><BR><BR>
	 *
	 * <B><U> Example of use </U> :</B><BR><BR>
	 * <li> Client packet : Action, AttackRequest</li><BR><BR>
	 *
	 * @param player The player that start an action on this L2PcInstance
	 *
	 */
	@Override
	public void onAction(L2PcInstance player)
	{
		// See description in TvTEvent.java
		if (!TvTEvent.onAction( player, getObjectId()))
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		// Check if the L2PcInstance is confused
		if (player.isOutOfControl())
		{
			// Send a Server->Client packet ActionFailed to the player
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		// Check if the player already target this L2PcInstance
		if (player.getTarget() != this)
		{
			// Set the target of the player
			player.setTarget(this);

			// Send a Server->Client packet MyTargetSelected to the player
			// The color to display in the select window is White
			player.sendPacket(new MyTargetSelected(getObjectId(), 0));
			if (player != this) player.sendPacket(new ValidateLocation(this));
		}
		else
		{
			if (player != this) player.sendPacket(new ValidateLocation(this));
			// Check if this L2PcInstance has a Private Store
			if (getPrivateStoreType() != 0)
            {
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);
            }
			else
			{
				// Check if this L2PcInstance is autoAttackable
				if (isAutoAttackable(player))
				{
					// Player with lvl < 21 can't attack a cursed weapon holder
					// And a cursed weapon holder  can't attack players with lvl < 21
					if ((isCursedWeaponEquipped() && player.getLevel() < 21)
							|| (player.isCursedWeaponEquipped() && getLevel() < 21))
					{
						player.sendPacket(ActionFailed.STATIC_PACKET);
					} else
					{
						if (Config.GEODATA > 0)
						{
							if (GeoData.getInstance().canSeeTarget(player, this))
							{
								player.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, this);
								player.onActionRequest();
							}
						}
						else
						{
							player.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, this);
							player.onActionRequest();
						}
					}
				} else
				{
					// This Action Failed packet avoids player getting stuck when clicking three or more times
					player.sendPacket(ActionFailed.STATIC_PACKET);
					if (Config.GEODATA > 0)
					{
						if(GeoData.getInstance().canSeeTarget(player, this))
							player.getAI().setIntention(CtrlIntention.AI_INTENTION_FOLLOW, this);
					}
					else
						player.getAI().setIntention(CtrlIntention.AI_INTENTION_FOLLOW, this);
				}
			}
		}
	}

	/**
	 * Returns true if cp update should be done, false if not
	 * @return boolean
	 */
	private boolean needCpUpdate(int barPixels)
	{
		double currentCp = getCurrentCp();

	    if (currentCp <= 1.0 || getMaxCp() < barPixels)
	        return true;

	    if (currentCp <= _cpUpdateDecCheck || currentCp >= _cpUpdateIncCheck)
	    {
	    	if (currentCp == getMaxCp())
	    	{
	    		_cpUpdateIncCheck = currentCp + 1;
	    		_cpUpdateDecCheck = currentCp - _cpUpdateInterval;
	    	}
	    	else
	    	{
	    		double doubleMulti = currentCp / _cpUpdateInterval;
		    	int intMulti = (int)doubleMulti;

	    		_cpUpdateDecCheck = _cpUpdateInterval * (doubleMulti < intMulti ? intMulti-- : intMulti);
	    		_cpUpdateIncCheck = _cpUpdateDecCheck + _cpUpdateInterval;
	    	}

	    	return true;
	    }

	    return false;
	}

	/**
	 * Returns true if mp update should be done, false if not
	 * @return boolean
	 */
	private boolean needMpUpdate(int barPixels)
	{
		double currentMp = getCurrentMp();

	    if (currentMp <= 1.0 || getMaxMp() < barPixels)
	        return true;

	    if (currentMp <= _mpUpdateDecCheck || currentMp >= _mpUpdateIncCheck)
	    {
	    	if (currentMp == getMaxMp())
	    	{
	    		_mpUpdateIncCheck = currentMp + 1;
	    		_mpUpdateDecCheck = currentMp - _mpUpdateInterval;
	    	}
	    	else
	    	{
	    		double doubleMulti = currentMp / _mpUpdateInterval;
		    	int intMulti = (int)doubleMulti;

	    		_mpUpdateDecCheck = _mpUpdateInterval * (doubleMulti < intMulti ? intMulti-- : intMulti);
	    		_mpUpdateIncCheck = _mpUpdateDecCheck + _mpUpdateInterval;
	    	}

	    	return true;
	    }

	    return false;
	}

	/**
	 * Send packet StatusUpdate with current HP,MP and CP to the L2PcInstance and only current HP, MP and Level to all other L2PcInstance of the Party.<BR><BR>
	 *
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Send the Server->Client packet StatusUpdate with current HP, MP and CP to this L2PcInstance </li><BR>
	 * <li>Send the Server->Client packet PartySmallWindowUpdate with current HP, MP and Level to all other L2PcInstance of the Party </li><BR><BR>
	 *
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T SEND current HP and MP to all L2PcInstance of the _statusListener</B></FONT><BR><BR>
	 *
	 */
	@Override
	public void broadcastStatusUpdate()
	{
		//TODO We mustn't send these informations to other players
		// Send the Server->Client packet StatusUpdate with current HP and MP to all L2PcInstance that must be informed of HP/MP updates of this L2PcInstance
		//super.broadcastStatusUpdate();

		// Send the Server->Client packet StatusUpdate with current HP, MP and CP to this L2PcInstance
		StatusUpdate su = new StatusUpdate(getObjectId());
		su.addAttribute(StatusUpdate.CUR_HP, (int) getCurrentHp());
		su.addAttribute(StatusUpdate.CUR_MP, (int) getCurrentMp());
		su.addAttribute(StatusUpdate.CUR_CP, (int) getCurrentCp());
		su.addAttribute(StatusUpdate.MAX_CP, 	   getMaxCp());
		sendPacket(su);

		// Check if a party is in progress and party window update is usefull
		if (isInParty() && (needCpUpdate(352) || super.needHpUpdate(352) || needMpUpdate(352)))
		{
			if (Config.DEBUG)
				_log.fine("Send status for party window of " + getObjectId() + "(" + getName() + ") to his party. CP: " + getCurrentCp() + " HP: " + getCurrentHp() + " MP: " + getCurrentMp());
			// Send the Server->Client packet PartySmallWindowUpdate with current HP, MP and Level to all other L2PcInstance of the Party
			PartySmallWindowUpdate update = new PartySmallWindowUpdate(this);
			getParty().broadcastToPartyMembers(this, update);
		}

        if (isInOlympiadMode())
        {
        	// TODO: implement new OlympiadUserInfo
        	Collection<L2PcInstance> plrs = getKnownList().getKnownPlayers().values();
        	//synchronized (getKnownList().getKnownPlayers())
			{
				for (L2PcInstance player : plrs)
				{
					if (player.getOlympiadGameId() == getOlympiadGameId()
					        && player.isOlympiadStart())
					{
						if (Config.DEBUG)
							_log.fine("Send status for Olympia window of "
							        + getObjectId() + "(" + getName() + ") to "
							        + player.getObjectId() + "("
							        + player.getName() + "). CP: "
							        + getCurrentCp() + " HP: " + getCurrentHp()
							        + " MP: " + getCurrentMp());
						player.sendPacket(new ExOlympiadUserInfoSpectator(this, 1));
					}
				}
			}
            if(Olympiad.getInstance().getSpectators(_olympiadGameId) != null && this.isOlympiadStart())
            {
                for(L2PcInstance spectator : Olympiad.getInstance().getSpectators(_olympiadGameId))
                {
                    if (spectator == null) continue;
                    spectator.sendPacket(new ExOlympiadUserInfoSpectator(this, getOlympiadSide()));
                }
            }
        }
        if (isInDuel())
        {
        	ExDuelUpdateUserInfo update = new ExDuelUpdateUserInfo(this);
        	DuelManager.getInstance().broadcastToOppositTeam(this, update);
        }
	}
    
    @Override
    public final void updateEffectIcons(boolean partyOnly)
    {
        // Create the main packet if needed
        AbnormalStatusUpdate mi = null;
        if (!partyOnly)
        {
            mi = new AbnormalStatusUpdate();
        }
        
        PartySpelled ps = null;
        if (this.isInParty())
        {
            ps = new PartySpelled(this);
        }
        
        // Create the olympiad spectator packet if needed
        ExOlympiadSpelledInfo os = null;
        if (this.isInOlympiadMode() && this.isOlympiadStart())
        {
            os = new ExOlympiadSpelledInfo(this);
        }
        
        // Go through all effects if any
        L2Effect[] effects = getAllEffects();
        if (effects != null && effects.length > 0)
        {
            for (int i = 0; i < effects.length; i++)
            {
                L2Effect effect = effects[i];

                if (effect == null || !effect.getShowIcon())
                {
                    continue;
                }
                
                switch (effect.getEffectType())
                {
                	case CHARGE: // handled by EtcStatusUpdate
                	case SIGNET_GROUND:
                		continue;
                }
                
                if (effect.getInUse())
                {
                    if (mi != null)
                        effect.addIcon(mi);
                    if (ps != null)
                        effect.addPartySpelledIcon(ps);
                    if (os != null)
                        effect.addOlympiadSpelledIcon(os);
                }
            }
        }
            
        // Send the packets if needed
        if (mi != null)
            sendPacket(mi);
        if (ps != null)
        {
            // summon info only needs to go to the owner, not to the whole party
            // player info: if in party, send to all party members except one's self.
            //              if not in party, send to self.
            if (this.isInParty())
            {
                this.getParty().broadcastToPartyMembers(this, ps);
            }
        }
        
        if (os != null)
        {
            if (Olympiad.getInstance().getSpectators(this.getOlympiadGameId()) != null)
            {
                for (L2PcInstance spectator : Olympiad.getInstance().getSpectators(this.getOlympiadGameId()))
                {
                    if (spectator != null) 
                    {
                        spectator.sendPacket(os);
                    }
                }
            }
        }
    }

	/**
	 * Send a Server->Client packet UserInfo to this L2PcInstance and CharInfo to all L2PcInstance in its _KnownPlayers.<BR><BR>
	 *
	 * <B><U> Concept</U> :</B><BR><BR>
	 * Others L2PcInstance in the detection area of the L2PcInstance are identified in <B>_knownPlayers</B>.
	 * In order to inform other players of this L2PcInstance state modifications, server just need to go through _knownPlayers to send Server->Client Packet<BR><BR>
	 *
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Send a Server->Client packet UserInfo to this L2PcInstance (Public and Private Data)</li>
	 * <li>Send a Server->Client packet CharInfo to all L2PcInstance in _KnownPlayers of the L2PcInstance (Public data only)</li><BR><BR>
	 *
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : DON'T SEND UserInfo packet to other players instead of CharInfo packet.
	 * Indeed, UserInfo packet contains PRIVATE DATA as MaxHP, STR, DEX...</B></FONT><BR><BR>
	 *
	 */
	public final void broadcastUserInfo()
	{
		// Send a Server->Client packet UserInfo to this L2PcInstance
		sendPacket(new UserInfo(this));

		// Send a Server->Client packet CharInfo to all L2PcInstance in _KnownPlayers of the L2PcInstance
		if (Config.DEBUG)
            _log.fine("players to notify:" + getKnownList().getKnownPlayers().size() + " packet: [S] 03 CharInfo");

		Broadcast.toKnownPlayers(this, new CharInfo(this));
	}

	public final void broadcastTitleInfo()
	{
		// Send a Server->Client packet UserInfo to this L2PcInstance
		sendPacket(new UserInfo(this));

		// Send a Server->Client packet TitleUpdate to all L2PcInstance in _KnownPlayers of the L2PcInstance
		if (Config.DEBUG)
            _log.fine("players to notify:" + getKnownList().getKnownPlayers().size() + " packet: [S] cc TitleUpdate");

		Broadcast.toKnownPlayers(this, new NicknameChanged(this));
	}

	@Override
	public final void broadcastPacket(L2GameServerPacket mov)
	{
		if (!(mov instanceof CharInfo))
			sendPacket(mov);

		Collection<L2PcInstance> plrs = getKnownList().getKnownPlayers().values();
		//synchronized (getKnownList().getKnownPlayers())
		{
			for (L2PcInstance player : plrs)
			{
				player.sendPacket(mov);
				if (mov instanceof CharInfo)
				{
					int relation = getRelation(player);
					if (getKnownList().getKnownRelations().get(player.getObjectId()) != null
					        && getKnownList().getKnownRelations().get(player.getObjectId()) != relation)
					{
						player.sendPacket(new RelationChanged(this, relation, player.isAutoAttackable(this)));
						if (getPet() != null)
							player.sendPacket(new RelationChanged(getPet(), relation, player.isAutoAttackable(this)));
					}
				}
			}
		}
	}

	@Override
	public void broadcastPacket(L2GameServerPacket mov, int radiusInKnownlist)
	{
		if (!(mov instanceof CharInfo))
			sendPacket(mov);

		Collection<L2PcInstance> plrs = getKnownList().getKnownPlayers().values();
		//synchronized (getKnownList().getKnownPlayers())
		{
			for (L2PcInstance player : plrs)
			{
				if (isInsideRadius(player, radiusInKnownlist, false, false))
				{
					player.sendPacket(mov);
					if (mov instanceof CharInfo)
					{
						int relation = getRelation(player);
						if (getKnownList().getKnownRelations().get(player.getObjectId()) != null
						        && getKnownList().getKnownRelations().get(player.getObjectId()) != relation)
						{
							player.sendPacket(new RelationChanged(this, relation, player.isAutoAttackable(this)));
							if (getPet() != null)
								player.sendPacket(new RelationChanged(getPet(), relation, player.isAutoAttackable(this)));
						}
					}
				}
			}
		}
	}

	/**
	 * Return the Alliance Identifier of the L2PcInstance.<BR><BR>
	 */
	public int getAllyId()
	{
		if (_clan == null)
			return 0;
		else
			return _clan.getAllyId();
	}

	public int getAllyCrestId()
	{
		if (getClanId() == 0)
		{
			return 0;
		}
		if (getClan().getAllyId() == 0)
		{
			return 0;
		}
		return getClan().getAllyCrestId();
	}

	/**
	 * Manage hit process (called by Hit Task of L2Character).<BR><BR>
	 *
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>If the attacker/target is dead or use fake death, notify the AI with EVT_CANCEL and send a Server->Client packet ActionFailed (if attacker is a L2PcInstance)</li>
	 * <li>If attack isn't aborted, send a message system (critical hit, missed...) to attacker/target if they are L2PcInstance </li>
	 * <li>If attack isn't aborted and hit isn't missed, reduce HP of the target and calculate reflection damage to reduce HP of attacker if necessary </li>
	 * <li>if attack isn't aborted and hit isn't missed, manage attack or cast break of the target (calculating rate, sending message...) </li><BR><BR>
	 *
	 * @param target The L2Character targeted
	 * @param damage Nb of HP to reduce
	 * @param crit True if hit is critical
	 * @param miss True if hit is missed
	 * @param soulshot True if SoulShot are charged
	 * @param shld True if shield is efficient
	 *
	 */
	@Override
	protected void onHitTimer(L2Character target, int damage, boolean crit, boolean miss, boolean soulshot, boolean shld)
	{
		super.onHitTimer(target, damage, crit, miss, soulshot, shld);
	}
    
    public void queryGameGuard()
    {
        this.getClient().setGameGuardOk(false);
        this.sendPacket(new GameGuardQuery());
        if (Config.GAMEGUARD_ENFORCE)
        {
            ThreadPoolManager.getInstance().scheduleGeneral(new GameGuardCheck(), 30*1000);
        }
    }
    
    class GameGuardCheck implements Runnable
    {
        
        /**
         * @see java.lang.Runnable#run()
         */
        @SuppressWarnings("synthetic-access")
        public void run()
        {
            L2GameClient client = L2PcInstance.this.getClient();
            if (client != null && !client.isAuthedGG() && L2PcInstance.this.isOnline() == 1)
            {
                GmListTable.broadcastMessageToGMs("Client "+client+" failed to reply GameGuard query and is being kicked!");
                _log.info("Client "+client+" failed to reply GameGuard query and is being kicked!");
                client.close(new LeaveWorld());
            }
        }
    }

	/**
	 * Send a Server->Client packet StatusUpdate to the L2PcInstance.<BR><BR>
	 */
	@Override
	public void sendPacket(L2GameServerPacket packet)
	{
		if (_client != null)
		{
			_client.sendPacket(packet);
		}
	}

	/**
	 * Manage Interact Task with another L2PcInstance.<BR><BR>
	 *
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>If the private store is a STORE_PRIVATE_SELL, send a Server->Client PrivateBuyListSell packet to the L2PcInstance</li>
	 * <li>If the private store is a STORE_PRIVATE_BUY, send a Server->Client PrivateBuyListBuy packet to the L2PcInstance</li>
	 * <li>If the private store is a STORE_PRIVATE_MANUFACTURE, send a Server->Client RecipeShopSellList packet to the L2PcInstance</li><BR><BR>
	 *
	 * @param target The L2Character targeted
	 *
	 */
	public void doInteract(L2Character target)
	{
		if (target instanceof L2PcInstance)
		{
			L2PcInstance temp = (L2PcInstance) target;
			sendPacket(ActionFailed.STATIC_PACKET);

			if (temp.getPrivateStoreType() == STORE_PRIVATE_SELL || temp.getPrivateStoreType() == STORE_PRIVATE_PACKAGE_SELL)
				sendPacket(new PrivateStoreListSell(this, temp));
			else if (temp.getPrivateStoreType() == STORE_PRIVATE_BUY)
				sendPacket(new PrivateStoreListBuy(this, temp));
			else if (temp.getPrivateStoreType() == STORE_PRIVATE_MANUFACTURE)
				sendPacket(new RecipeShopSellList(this, temp));

		}
		else
		{
			// _interactTarget=null should never happen but one never knows ^^;
			if (target != null)
				target.onAction(this);
		}
	}

	/**
	 * Manage AutoLoot Task.<BR><BR>
	 *
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Send a System Message to the L2PcInstance : YOU_PICKED_UP_S1_ADENA or YOU_PICKED_UP_S1_S2</li>
	 * <li>Add the Item to the L2PcInstance inventory</li>
	 * <li>Send a Server->Client packet InventoryUpdate to this L2PcInstance with NewItem (use a new slot) or ModifiedItem (increase amount)</li>
	 * <li>Send a Server->Client packet StatusUpdate to this L2PcInstance with current weight</li><BR><BR>
	 *
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : If a Party is in progress, distribute Items between party members</B></FONT><BR><BR>
	 *
	 * @param target The L2ItemInstance dropped
	 *
	 */
	public void doAutoLoot(L2Attackable target, L2Attackable.RewardItem item)
	{
		if (isInParty()&&ItemTable.getInstance().getTemplate(item.getItemId()).getItemType() != L2EtcItemType.HERB)  
			getParty().distributeItem(this, item, false, target);
		else if (item.getItemId() == 57) addAdena("Loot", item.getCount(), target, true);
		else addItem("Loot", item.getItemId(), item.getCount(), target, true);
	}


	/**
	 * Manage Pickup Task.<BR><BR>
	 *
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Send a Server->Client packet StopMove to this L2PcInstance </li>
	 * <li>Remove the L2ItemInstance from the world and send server->client GetItem packets </li>
	 * <li>Send a System Message to the L2PcInstance : YOU_PICKED_UP_S1_ADENA or YOU_PICKED_UP_S1_S2</li>
	 * <li>Add the Item to the L2PcInstance inventory</li>
	 * <li>Send a Server->Client packet InventoryUpdate to this L2PcInstance with NewItem (use a new slot) or ModifiedItem (increase amount)</li>
	 * <li>Send a Server->Client packet StatusUpdate to this L2PcInstance with current weight</li><BR><BR>
	 *
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : If a Party is in progress, distribute Items between party members</B></FONT><BR><BR>
	 *
	 * @param object The L2ItemInstance to pick up
	 *
	 */
	protected void doPickupItem(L2Object object)
	{
		if (isAlikeDead() || isFakeDeath()) return;

		// Set the AI Intention to AI_INTENTION_IDLE
		getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);

		// Check if the L2Object to pick up is a L2ItemInstance
		if (! (object instanceof L2ItemInstance))
		{
			// dont try to pickup anything that is not an item :)
			_log.warning("trying to pickup wrong target."+getTarget());
			return;
		}

		L2ItemInstance target = (L2ItemInstance) object;

		// Send a Server->Client packet ActionFailed to this L2PcInstance
		sendPacket(ActionFailed.STATIC_PACKET);

		// Send a Server->Client packet StopMove to this L2PcInstance
		StopMove sm = new StopMove(getObjectId(), getX(), getY(), getZ(), getHeading());
		if (Config.DEBUG) _log.fine("pickup pos: "+ target.getX() + " "+target.getY()+ " "+target.getZ() );
		sendPacket(sm);

		synchronized (target)
		{
			// Check if the target to pick up is visible
			if (!target.isVisible())
			{
				// Send a Server->Client packet ActionFailed to this L2PcInstance
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}

			if ( ((isInParty() && getParty().getLootDistribution() == L2Party.ITEM_LOOTER) || !isInParty()) && !_inventory.validateCapacity(target))
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				sendPacket(new SystemMessage(SystemMessageId.SLOTS_FULL));
				return;
			}

            if (isInvul() && !isGM())
            {
                sendPacket(ActionFailed.STATIC_PACKET);
                SystemMessage smsg = new SystemMessage(SystemMessageId.FAILED_TO_PICKUP_S1);
                smsg.addItemName(target);
                sendPacket(smsg);
                return;
            }

	        if (target.getOwnerId() != 0 && target.getOwnerId() != getObjectId() && !isInLooterParty(target.getOwnerId()))
            {
                sendPacket(ActionFailed.STATIC_PACKET);

                if (target.getItemId() == 57)
                {
                    SystemMessage smsg = new SystemMessage(SystemMessageId.FAILED_TO_PICKUP_S1_ADENA);
                    smsg.addNumber(target.getCount());
                    sendPacket(smsg);
                }
                else if (target.getCount() > 1)
                {
                    SystemMessage smsg = new SystemMessage(SystemMessageId.FAILED_TO_PICKUP_S2_S1_S);
                    smsg.addItemName(target);
                    smsg.addNumber(target.getCount());
                    sendPacket(smsg);
                }
                else
                {
                    SystemMessage smsg = new SystemMessage(SystemMessageId.FAILED_TO_PICKUP_S1);
                    smsg.addItemName(target);
                    sendPacket(smsg);
                }

                return;
            }
            
            // You can pickup only 1 combat flag
            if(FortSiegeManager.getInstance().isCombat(target.getItemId()) )
            {
                if (!FortSiegeManager.getInstance().checkIfCanPickup(this))
                    return ;
            }
            
	        if(target.getItemLootShedule() != null
	        		&& (target.getOwnerId() == getObjectId() || isInLooterParty(target.getOwnerId())))
	        	target.resetOwnerTimer();

			// Remove the L2ItemInstance from the world and send server->client GetItem packets
			target.pickupMe(this);
			 if(Config.SAVE_DROPPED_ITEM) // item must be removed from ItemsOnGroundManager if is active
				 ItemsOnGroundManager.getInstance().removeObject(target);

		}

        //Auto use herbs - pick up
		if (target.getItemType() == L2EtcItemType.HERB)
        {
			IItemHandler handler = ItemHandler.getInstance().getItemHandler(target.getItemId());
			if (handler == null)
				_log.fine("No item handler registered for item ID " + target.getItemId() + ".");
			else
				handler.useItem(this, target);
            	ItemTable.getInstance().destroyItem("Consume", target, this, null);
        }
		// Cursed Weapons are not distributed
		else if(CursedWeaponsManager.getInstance().isCursed(target.getItemId()))
		{
			addItem("Pickup", target, null, true);
		}
		else if(FortSiegeManager.getInstance().isCombat(target.getItemId()) )
        {
            addItem("Pickup", target, null, true);
        }
		else
		{
			// if item is instance of L2ArmorType or L2WeaponType broadcast an "Attention" system message
			if(target.getItemType() instanceof L2ArmorType || target.getItemType() instanceof L2WeaponType)
			{
				if (target.getEnchantLevel() > 0)
				{
					SystemMessage msg = new SystemMessage(SystemMessageId.ATTENTION_S1_PICKED_UP_S2_S3);
					msg.addPcName(this);
					msg.addNumber(target.getEnchantLevel());
					msg.addItemName(target.getItemId());
					broadcastPacket(msg, 1400);
				}
				else
				{
					SystemMessage msg = new SystemMessage(SystemMessageId.ATTENTION_S1_PICKED_UP_S2);
					msg.addPcName(this);
					msg.addItemName(target.getItemId());
					broadcastPacket(msg, 1400);
				}
			}

			// Check if a Party is in progress
			if (isInParty()) getParty().distributeItem(this, target);
			// Target is adena
			else if (target.getItemId() == 57 && getInventory().getAdenaInstance() != null)
			{
				addAdena("Pickup", target.getCount(), null, true);
				ItemTable.getInstance().destroyItem("Pickup", target, this, null);
			}
			// Target is regular item
			else addItem("Pickup", target, null, true);
		}
	}
    
    public boolean canOpenPrivateStore()
    {
        return !this.isAlikeDead() && !this.isInOlympiadMode() && !this.isMounted();
    }
    
    public void tryOpenPrivateBuyStore()
    {
        // Player shouldn't be able to set stores if he/she is alike dead (dead or fake death)
        if (this.canOpenPrivateStore())
        {
            if (this.getPrivateStoreType() == L2PcInstance.STORE_PRIVATE_BUY || this.getPrivateStoreType() == L2PcInstance.STORE_PRIVATE_BUY +1)
            {
                this.setPrivateStoreType(L2PcInstance.STORE_PRIVATE_NONE);
            }
            if (this.getPrivateStoreType() == L2PcInstance.STORE_PRIVATE_NONE)
            {
                if (this.isSitting())
                {
                    this.standUp();
                }
                this.setPrivateStoreType(L2PcInstance.STORE_PRIVATE_BUY +1);
                this.sendPacket(new PrivateStoreManageListBuy(this));
            }
        }
        else
        {
            this.sendPacket(ActionFailed.STATIC_PACKET);
        }
    }
    
    public void tryOpenPrivateSellStore(boolean isPackageSale)
    {
        // Player shouldn't be able to set stores if he/she is alike dead (dead or fake death)
        if (this.canOpenPrivateStore())
        {
            if (this.getPrivateStoreType() == L2PcInstance.STORE_PRIVATE_SELL
                    || this.getPrivateStoreType() == L2PcInstance.STORE_PRIVATE_SELL + 1
                    || this.getPrivateStoreType() == L2PcInstance.STORE_PRIVATE_PACKAGE_SELL)
            {
                this.setPrivateStoreType(L2PcInstance.STORE_PRIVATE_NONE);
            }
            

            if (this.getPrivateStoreType() == L2PcInstance.STORE_PRIVATE_NONE)
            {
                if (this.isSitting())
                {
                    this.standUp();
                }
                this.setPrivateStoreType(L2PcInstance.STORE_PRIVATE_SELL + 1);
                this.sendPacket(new PrivateStoreManageListSell(this, isPackageSale));
            }
        }
        else
        {
            this.sendPacket(ActionFailed.STATIC_PACKET);
        }
    }
    
    public boolean isTransformed()
    {
        return _transformation != null;
    }
    
    public void transform(L2Transformation transformation)
    {
        if (this.isTransformed())
        {
            // You already polymorphed and cannot polymorph again.
            SystemMessage msg = new SystemMessage(SystemMessageId.YOU_ALREADY_POLYMORPHED_AND_CANNOT_POLYMORPH_AGAIN);
            this.sendPacket(msg);
            return;
        }
        _transformation = transformation;
        transformation.onTransform();
        this.broadcastUserInfo();
    }
    
    public void untransform()
    {
        if (this.isTransformed())
        {
            restoreSkills();
            regiveTemporarySkills();
            _transformation.onUntransform();
            _transformation = null;
            broadcastUserInfo();
            sendPacket(new SkillCoolTime(this));
        }
    }
    
    public L2Transformation getTransformation()
    {
        return _transformation;
    }
    
    /**
     * This returns the transformation Id of the current transformation.
     * For example, if a player is transformed as a Buffalo, and then picks up the Zariche,
     * the transform Id returned will be that of the Zariche, and NOT the Buffalo.
     * @return Transformation Id
     */
    public int getTranformationId()
    {
        L2Transformation transformation = this.getTransformation();
        if (transformation == null)
        {
            return 0;
        }
        return transformation.getId();
    }
    
    /**
     * This returns the transformation Id stored inside the character table, selected by the method: transformSelectInfo()
     * For example, if a player is transformed as a Buffalo, and then picks up the Zariche,
     * the transform Id returned will be that of the Buffalo, and NOT the Zariche.
     * @return Transformation Id
     */
    public int transformId()
    {
       return _transformationId;
    }
    
    /**
     * This is a simple query that inserts the transform Id into the character table for future reference.
     */
    public void transformInsertInfo()
    {
    	_transformationId = getTranformationId();
    	
    	if (_transformationId == L2Transformation.TRANSFORM_AKAMANAH
    			|| _transformationId == L2Transformation.TRANSFORM_ZARICHE)
    		return;
    	
    	Connection con = null;
        try
        {
            con = L2DatabaseFactory.getInstance().getConnection();
            PreparedStatement statement = con.prepareStatement(UPDATE_CHAR_TRANSFORM);
            
            statement.setInt(1, _transformationId);
            statement.setInt(2, getObjectId());
            
            statement.execute();
            statement.close();
        }
        catch (Exception e)
        {
        	_log.log(Level.SEVERE, "Transformation insert info: ",e);
        }
        finally
        {
            try { con.close(); } catch (Exception e) {}
        }
    }
    
    /**
     * This selects the current 
     * @return transformation Id
     */
    public int transformSelectInfo()
    {
    	Connection con = null;
        try
        {
            con = L2DatabaseFactory.getInstance().getConnection();
            PreparedStatement statement = con.prepareStatement(SELECT_CHAR_TRANSFORM); 
            
            statement.setInt(1, getObjectId());
            ResultSet rset = statement.executeQuery();
            rset.next();
            _transformationId = rset.getInt("transform_id");
            
            rset.close();
            statement.close();
        }
        catch (Exception e)
        {
        	_log.log(Level.SEVERE, "Transformation select info: ",e);
        }
        finally
        {
            try { con.close(); } catch (Exception e) {}
        }
        return _transformationId;
    }

	/**
	 * Set a target.<BR><BR>
	 *
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Remove the L2PcInstance from the _statusListener of the old target if it was a L2Character </li>
	 * <li>Add the L2PcInstance to the _statusListener of the new target if it's a L2Character </li>
	 * <li>Target the new L2Object (add the target to the L2PcInstance _target, _knownObject and L2PcInstance to _KnownObject of the L2Object)</li><BR><BR>
	 *
	 * @param newTarget The L2Object to target
	 *
	 */
	@Override
	public void setTarget(L2Object newTarget)
	{
		
		if(newTarget!=null)
		{
			boolean isParty=(( (newTarget instanceof L2PcInstance) && isInParty() && getParty().getPartyMembers().contains(newTarget)));
		
			// Check if the new target is visible
			if (!isParty && !newTarget.isVisible())
				newTarget = null;

			// Prevents /target exploiting
			if (newTarget != null && !isParty && Math.abs(newTarget.getZ() - getZ()) > 1000)
				newTarget = null;
		}
		if(!isGM())
		{
			// Can't target and attack festival monsters if not participant
			if((newTarget instanceof L2FestivalMonsterInstance) && !isFestivalParticipant())
				newTarget = null;

			// Can't target and attack rift invaders if not in the same room
			else if(isInParty() && getParty().isInDimensionalRift())
			{
				byte riftType = getParty().getDimensionalRift().getType();
				byte riftRoom = getParty().getDimensionalRift().getCurrentRoom();

				if (newTarget != null && !DimensionalRiftManager.getInstance().getRoom(riftType, riftRoom).checkIfInZone(newTarget.getX(), newTarget.getY(), newTarget.getZ()))
					newTarget = null;
			}
		}

		// Get the current target
		L2Object oldTarget = getTarget();

		if (oldTarget != null)
		{
			if (oldTarget.equals(newTarget))
				return; // no target change

			// Remove the L2PcInstance from the _statusListener of the old target if it was a L2Character
			if (oldTarget instanceof L2Character)
				((L2Character) oldTarget).removeStatusListener(this);
		}

		// Add the L2PcInstance to the _statusListener of the new target if it's a L2Character
		if (newTarget instanceof L2Character)
		{
			((L2Character) newTarget).addStatusListener(this);
			TargetSelected my = new TargetSelected(getObjectId(), newTarget.getObjectId(), getX(), getY(), getZ());
			broadcastPacket(my);
		}
		if (newTarget == null && getTarget() != null)
		{
			broadcastPacket(new TargetUnselected(this));
		}
		
		// Target the new L2Object (add the target to the L2PcInstance _target, _knownObject and L2PcInstance to _KnownObject of the L2Object)
		super.setTarget(newTarget);
	}

	/**
	 * Return the active weapon instance (always equiped in the right hand).<BR><BR>
	 */
	@Override
	public L2ItemInstance getActiveWeaponInstance()
	{
		return getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);
	}

	/**
	 * Return the active weapon item (always equiped in the right hand).<BR><BR>
	 */
	@Override
	public L2Weapon getActiveWeaponItem()
	{
		L2ItemInstance weapon = getActiveWeaponInstance();

		if (weapon == null)
			return getFistsWeaponItem();

		return (L2Weapon) weapon.getItem();
	}

	public L2ItemInstance getChestArmorInstance()
	{
		return getInventory().getPaperdollItem(Inventory.PAPERDOLL_CHEST);
	}

    public L2ItemInstance getLegsArmorInstance()
    {
        return getInventory().getPaperdollItem(Inventory.PAPERDOLL_LEGS);
    }

	public L2Armor getActiveChestArmorItem()
	{
		L2ItemInstance armor = getChestArmorInstance();

		if (armor == null)
			return null;

		return (L2Armor) armor.getItem();
	}

    public L2Armor getActiveLegsArmorItem()
    {
        L2ItemInstance legs = getLegsArmorInstance();

        if (legs == null)
            return null;

        return (L2Armor) legs.getItem();
    }

	public boolean isWearingHeavyArmor()
	{
        L2ItemInstance legs = getLegsArmorInstance();
        L2ItemInstance armor = getChestArmorInstance();
        
	    if (armor != null && legs != null)
        {
            if ((L2ArmorType)legs.getItemType() == L2ArmorType.HEAVY 
                    && ((L2ArmorType)armor.getItemType() == L2ArmorType.HEAVY))
                return true;
        }
	    if (armor != null)
	    {
	        if ((getInventory().getPaperdollItem(Inventory.PAPERDOLL_CHEST).getItem().getBodyPart() == L2Item.SLOT_FULL_ARMOR 
	                && (L2ArmorType)armor.getItemType() == L2ArmorType.HEAVY))
	            return true;
	    }
		return false;
	}

	public boolean isWearingLightArmor()
	{
        L2ItemInstance legs = getLegsArmorInstance();
        L2ItemInstance armor = getChestArmorInstance();

	    if (armor != null && legs != null)
        {
            if ((L2ArmorType)legs.getItemType() == L2ArmorType.LIGHT 
                    && ((L2ArmorType)armor.getItemType() == L2ArmorType.LIGHT))
                return true;
        }
        if (armor != null)
        {
            if ((getInventory().getPaperdollItem(Inventory.PAPERDOLL_CHEST).getItem().getBodyPart() == L2Item.SLOT_FULL_ARMOR 
                    && (L2ArmorType)armor.getItemType() == L2ArmorType.LIGHT))
                return true;
        }
		return false;
	}

	public boolean isWearingMagicArmor()
	{
        L2ItemInstance legs = getLegsArmorInstance();
        L2ItemInstance armor = getChestArmorInstance();

	    if (armor != null && legs != null)
        {
            if ((L2ArmorType)legs.getItemType() == L2ArmorType.MAGIC 
                    && ((L2ArmorType)armor.getItemType() == L2ArmorType.MAGIC))
                return true;
        }
        if (armor != null)
        {
            if ((getInventory().getPaperdollItem(Inventory.PAPERDOLL_CHEST).getItem().getBodyPart() == L2Item.SLOT_FULL_ARMOR 
                    && (L2ArmorType)armor.getItemType() == L2ArmorType.MAGIC))
                return true;
        }
		return false;
	}

	public boolean isWearingFormalWear()
    {
        return _IsWearingFormalWear;
    }

    public void setIsWearingFormalWear(boolean value)
    {
        _IsWearingFormalWear = value;
    }
    public boolean isMarried()
    {
        return _married;
    }

    public void setMarried(boolean state)
    {
        _married = state;
    }

    public boolean isEngageRequest()
    {
        return _engagerequest;
    }

    public void setEngageRequest(boolean state,int playerid)
    {
        _engagerequest = state;
        _engageid = playerid;
    }

    public void setMaryRequest(boolean state)
    {
        _marryrequest = state;
    }

    public boolean isMaryRequest()
    {
        return _marryrequest;
    }

    public void setMarryAccepted(boolean state)
    {
        _marryaccepted = state;
    }

    public boolean isMarryAccepted()
    {
        return _marryaccepted;
    }

    public int getEngageId()
    {
        return _engageid;
    }

    public int getPartnerId()
    {
        return _partnerId;
    }

    public void setPartnerId(int partnerid)
    {
        _partnerId = partnerid;
    }

    public int getCoupleId()
    {
        return _coupleId;
    }

    public void setCoupleId(int coupleId)
    {
        _coupleId = coupleId;
    }

    public void EngageAnswer(int answer)
    {
        if(_engagerequest==false)
            return;
        else if(_engageid==0)
            return;
        else
        {
            L2PcInstance ptarget = (L2PcInstance)L2World.getInstance().findObject(_engageid);
            setEngageRequest(false,0);
            if(ptarget!=null)
            {
                if (answer == 1)
                {
                    CoupleManager.getInstance().createCouple(ptarget, L2PcInstance.this);
                    ptarget.sendMessage("Request to Engage has been >ACCEPTED<");
                }
                else
                    ptarget.sendMessage("Request to Engage has been >DENIED<!");
            }
        }
    }

	/**
	 * Return the secondary weapon instance (always equiped in the left hand).<BR><BR>
	 */
	@Override
	public L2ItemInstance getSecondaryWeaponInstance()
	{
		return getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);
	}

	/**
	 * Return the secondary weapon item (always equiped in the left hand) or the fists weapon.<BR><BR>
	 */
	@Override
	public L2Weapon getSecondaryWeaponItem()
	{
		L2ItemInstance weapon = getSecondaryWeaponInstance();

		if (weapon == null)
			return getFistsWeaponItem();

		L2Item item = weapon.getItem();

		if (item instanceof L2Weapon)
			return (L2Weapon) item;

		return null;
	}


	/**
	 *
     * Kill the L2Character, Apply Death Penalty, Manage gain/loss Karma and Item Drop.<BR><BR>
     *
     * <B><U> Actions</U> :</B><BR><BR>
     * <li>Reduce the Experience of the L2PcInstance in function of the calculated Death Penalty </li>
     * <li>If necessary, unsummon the Pet of the killed L2PcInstance </li>
     * <li>Manage Karma gain for attacker and Karam loss for the killed L2PcInstance </li>
     * <li>If the killed L2PcInstance has Karma, manage Drop Item</li>
     * <li>Kill the L2PcInstance </li><BR><BR>
     *
     *
     * @param i The HP decrease value
     * @param attacker The L2Character who attacks
     *
     *
	 * @see net.sf.l2j.gameserver.model.actor.instance.L2PlayableInstance#doDie(net.sf.l2j.gameserver.model.L2Character)
	 */
	@Override
	public boolean doDie(L2Character killer)
	{
		/* Since L2Character.doDie() calls stopAllEffects(), which includes
		 * setting charm of curage and other blessings as false, this stores value 
		 * before calling superclass method
		 */
		boolean charmOfCourage = getCharmOfCourage();
		
		// Kill the L2PcInstance
		if (!super.doDie(killer))
			return false;

		if (killer != null)
		{
			L2PcInstance pk = null;
			if (killer instanceof L2PcInstance)
				pk = (L2PcInstance) killer;

			TvTEvent.onKill(killer, this);

			if (atEvent && pk != null)
			{
				pk.kills.add(getName());
			}

			// Clear resurrect xp calculation
			setExpBeforeDeath(0);
			
			// Issues drop of Cursed Weapon.
			if (isCursedWeaponEquipped())
			{
				CursedWeaponsManager.getInstance().drop(_cursedWeaponEquippedId, killer);
			}
			else if (isCombatFlagEquipped())
            {
                FortSiegeManager.getInstance().dropCombatFlag(this);
                System.out.println("Player with combat flag die");
            }
			else
			{
				if (pk == null || !pk.isCursedWeaponEquipped())
				{
					//if (getKarma() > 0)
						onDieDropItem(killer);  // Check if any item should be dropped

					if (!(isInsideZone(ZONE_PVP) && !isInsideZone(ZONE_SIEGE)))
					{
						boolean isKillerPc = (killer instanceof L2PcInstance);
		                if (isKillerPc && ((L2PcInstance)killer).getClan() != null
		                               && getClan() != null
                                   && !isAcademyMember()
                                   && !(((L2PcInstance)killer).isAcademyMember())
		                               && _clan.isAtWarWith(((L2PcInstance) killer).getClanId())
		                               && ((L2PcInstance)killer).getClan().isAtWarWith(_clan.getClanId()))
		                {	
		                	
		                	// when your reputation score is 0 or below, the other clan cannot acquire any reputation points
		                    if (getClan().getReputationScore() > 0) {
		                		((L2PcInstance) killer).getClan().setReputationScore(((L2PcInstance) killer).getClan().getReputationScore()+Config.ALT_REPUTATION_SCORE_PER_KILL, true);
		                		getClan().broadcastToOnlineMembers(new PledgeShowInfoUpdate(_clan));
		                		((L2PcInstance) killer).getClan().broadcastToOnlineMembers(new PledgeShowInfoUpdate(((L2PcInstance) killer).getClan()));
		                    }
		                 // when the opposing sides reputation score is 0 or below, your clans reputation score does not decrease
		                    if (((L2PcInstance)killer).getClan().getReputationScore() > 0) {
		                    	_clan.setReputationScore(_clan.getReputationScore()-Config.ALT_REPUTATION_SCORE_PER_KILL, true);
		                    	getClan().broadcastToOnlineMembers(new PledgeShowInfoUpdate(_clan));
		                		((L2PcInstance) killer).getClan().broadcastToOnlineMembers(new PledgeShowInfoUpdate(((L2PcInstance) killer).getClan()));
		                    }
		                }
						if (Config.ALT_GAME_DELEVEL)
						{
							// Reduce the Experience of the L2PcInstance in function of the calculated Death Penalty
							// NOTE: deathPenalty +- Exp will update karma
							// Penalty is lower if the player is at war with the pk (war has to be declared)
							if (getSkillLevel(L2Skill.SKILL_LUCKY) < 0 || getStat().getLevel() > 9)
								deathPenalty(pk != null && getClan() != null && getClan().isAtWarWith(pk.getClanId()), pk != null, charmOfCourage);										

						} else
						{
							onDieUpdateKarma(); // Update karma if delevel is not allowed
						}
					}
				}
			}
		}
		
		// Untransforms character.
	    if (isTransformed())
	        untransform();


		setPvpFlag(0);              // Clear the pvp flag


		// Unsummon Cubics
		if (_cubics.size() > 0)
        {
			for (L2CubicInstance cubic : _cubics.values())
			{
				cubic.stopAction();
				cubic.cancelDisappear();
			}

			_cubics.clear();
		}

		if (_forceBuff != null)
			abortCast();

		for (L2Character character : getKnownList().getKnownCharacters())
			if (character.getForceBuff() != null && character.getForceBuff().getTarget() == this)
				character.abortCast();

		if (isInParty() && getParty().isInDimensionalRift())
			getParty().getDimensionalRift().getDeadMemberList().add(this);

		// calculate death penalty buff
		calculateDeathPenaltyBuffLevel(killer);

		stopRentPet();
		stopWaterTask();
		return true;
	}

	private void onDieDropItem(L2Character killer)
	{
		if (atEvent || killer == null)
            return;

		if (getKarma()<=0
                && killer instanceof L2PcInstance
                && ((L2PcInstance)killer).getClan()!=null
                && getClan()!=null
                && (
                        ((L2PcInstance)killer).getClan().isAtWarWith(getClanId())
//                      || this.getClan().isAtWarWith(((L2PcInstance)killer).getClanId())
                   )
           )
			return;

		if (!isInsideZone(ZONE_PVP) && (!isGM() || Config.KARMA_DROP_GM))
		{
			boolean isKarmaDrop = false;
			boolean isKillerNpc = (killer instanceof L2NpcInstance);
			int pkLimit = Config.KARMA_PK_LIMIT;;

			int dropEquip           = 0;
			int dropEquipWeapon     = 0;
			int dropItem            = 0;
			int dropLimit           = 0;
			int dropPercent         = 0;

			if (getKarma() > 0 && getPkKills() >= pkLimit)
			{
				isKarmaDrop = true;
				dropPercent = Config.KARMA_RATE_DROP;
				dropEquip = Config.KARMA_RATE_DROP_EQUIP;
				dropEquipWeapon = Config.KARMA_RATE_DROP_EQUIP_WEAPON;
				dropItem = Config.KARMA_RATE_DROP_ITEM;
				dropLimit = Config.KARMA_DROP_LIMIT;
			}
			else if (isKillerNpc && getLevel() > 4 && !isFestivalParticipant())
			{
				dropPercent = Config.PLAYER_RATE_DROP;
				dropEquip = Config.PLAYER_RATE_DROP_EQUIP;
				dropEquipWeapon = Config.PLAYER_RATE_DROP_EQUIP_WEAPON;
				dropItem = Config.PLAYER_RATE_DROP_ITEM;
				dropLimit = Config.PLAYER_DROP_LIMIT;
			}

            int dropCount = 0;
			while (dropPercent > 0 && Rnd.get(100) < dropPercent && dropCount < dropLimit)
			{
                int itemDropPercent = 0;
				List<Integer> nonDroppableList = new FastList<Integer>();
				List<Integer> nonDroppableListPet = new FastList<Integer>();

				nonDroppableList = Config.KARMA_LIST_NONDROPPABLE_ITEMS;
				nonDroppableListPet = Config.KARMA_LIST_NONDROPPABLE_ITEMS;

				for (L2ItemInstance itemDrop : getInventory().getItems())
				{
					// Don't drop
					if (
							itemDrop.isShadowItem() || // Dont drop Shadow Items
							!itemDrop.isDropable() ||
							itemDrop.getItemId() == 57 || // Adena
							itemDrop.getItem().getType2() == L2Item.TYPE2_QUEST ||                  // Quest Items
							nonDroppableList.contains(itemDrop.getItemId()) ||                      // Item listed in the non droppable item list
							nonDroppableListPet.contains(itemDrop.getItemId()) ||                   // Item listed in the non droppable pet item list
							getPet() != null && getPet().getControlItemId() == itemDrop.getItemId() // Control Item of active pet
					) continue;

					if (itemDrop.isEquipped())
					{
						// Set proper chance according to Item type of equipped Item
                        itemDropPercent = itemDrop.getItem().getType2() == L2Item.TYPE2_WEAPON ? dropEquipWeapon : dropEquip;
						getInventory().unEquipItemInSlotAndRecord(itemDrop.getLocationSlot());
					}
					else itemDropPercent = dropItem; // Item in inventory

					// NOTE: Each time an item is dropped, the chance of another item being dropped gets lesser (dropCount * 2)
					if (Rnd.get(100) < itemDropPercent)
					{
						dropItem("DieDrop", itemDrop, killer, true);

						if (isKarmaDrop)
							_log.warning(getName() + " has karma and dropped id = " + itemDrop.getItemId() + ", count = " + itemDrop.getCount());
						else
							_log.warning(getName() + " dropped id = " + itemDrop.getItemId() + ", count = " + itemDrop.getCount());

						dropCount++;
						break;
					}
				}
			}
		}
	}

	private void onDieUpdateKarma()
	{
		// Karma lose for server that does not allow delevel
		if ( getKarma() > 0 )
		{
			// this formula seems to work relatively well:
			// baseKarma * thisLVL * (thisLVL/100)
			// Calculate the new Karma of the attacker : newKarma = baseKarma*pkCountMulti*lvlDiffMulti
			double karmaLost = Config.KARMA_LOST_BASE;
			karmaLost *= getLevel(); // multiply by char lvl
			karmaLost *= (getLevel() / 100.0); // divide by 0.charLVL
			karmaLost = Math.round(karmaLost);
			if ( karmaLost < 0 ) karmaLost = 1;

			// Decrease Karma of the L2PcInstance and Send it a Server->Client StatusUpdate packet with Karma and PvP Flag if necessary
			setKarma(getKarma() - (int)karmaLost);
		}
	}

	public void onKillUpdatePvPKarma(L2Character target)
	{
		if (target == null) return;
		if (!(target instanceof L2PlayableInstance)) return;

		L2PcInstance targetPlayer = null;
		if (target instanceof L2PcInstance)
			targetPlayer = (L2PcInstance)target;
		else if (target instanceof L2Summon)
			targetPlayer = ((L2Summon)target).getOwner();

		if (targetPlayer == null) return;                                          // Target player is null
		if (targetPlayer == this) return;                                          // Target player is self

		if (isCursedWeaponEquipped())
		{
			CursedWeaponsManager.getInstance().increaseKills(_cursedWeaponEquippedId);
			// Custom message for time left
			// CursedWeapon cw = CursedWeaponsManager.getInstance().getCursedWeapon(_cursedWeaponEquipedId);
			// SystemMessage msg = new SystemMessage(SystemMessageId.THERE_IS_S1_HOUR_AND_S2_MINUTE_LEFT_OF_THE_FIXED_USAGE_TIME);
			// int timeLeftInHours = (int)(((cw.getTimeLeft()/60000)/60));
			// msg.addItemName(_cursedWeaponEquipedId);
			// msg.addNumber(timeLeftInHours);
			// sendPacket(msg);
			return;
		}

		// If in duel and you kill (only can kill l2summon), do nothing
		if (isInDuel() && targetPlayer.isInDuel()) return;

        // If in Arena, do nothing
		if (isInsideZone(ZONE_PVP) || targetPlayer.isInsideZone(ZONE_PVP))
			return;

        // Check if it's pvp
		if (
				(
						checkIfPvP(target) &&            //   Can pvp and
						targetPlayer.getPvpFlag() != 0   //   Target player has pvp flag set
				) ||                                     // or
				(
						isInsideZone(ZONE_PVP) &&         		 //   Player is inside pvp zone and
						targetPlayer.isInsideZone(ZONE_PVP) 	 //   Target player is inside pvp zone
				)
		)
		{
            increasePvpKills();
		}
		else                                                                        // Target player doesn't have pvp flag set
		{
            // check about wars
            if (targetPlayer.getClan() != null && getClan() != null)
            {
                if (getClan().isAtWarWith(targetPlayer.getClanId()))
                {
                    if (targetPlayer.getClan().isAtWarWith(getClanId()))
                    {
                        // 'Both way war' -> 'PvP Kill'
                        increasePvpKills();
                        return;
                    }
                }
            }

            // 'No war' or 'One way war' -> 'Normal PK'
			if (targetPlayer.getKarma() > 0)                                        // Target player has karma
			{
				if ( Config.KARMA_AWARD_PK_KILL )
				{
                    increasePvpKills();
				}
			}
			else if (targetPlayer.getPvpFlag() == 0)                                                                    // Target player doesn't have karma
			{
                increasePkKillsAndKarma(targetPlayer.getLevel());
				//Unequip adventurer items
				if(getInventory().getPaperdollItemId(7) >= 7816 && getInventory().getPaperdollItemId(7) <= 7831) 
				{
					L2ItemInstance invItem = getInventory().getItemByItemId(getInventory().getPaperdollItemId(7));
					if(invItem.isEquipped()) 
					{
						L2ItemInstance unequiped[] = getInventory().unEquipItemInSlotAndRecord(invItem.getLocationSlot());
						InventoryUpdate iu = new InventoryUpdate();
						for(int i = 0; i < unequiped.length; i++)
							iu.addModifiedItem(unequiped[i]);
						sendPacket(iu);
					}
					refreshExpertisePenalty();			
					sendPacket(new SystemMessage(SystemMessageId.YOU_ARE_UNABLE_TO_EQUIP_THIS_ITEM_WHEN_YOUR_PK_COUNT_IS_GREATER_THAN_OR_EQUAL_TO_ONE));
				}
			}
		}
	}

    /**
     * Increase the pvp kills count and send the info to the player
     *
     */
    public void increasePvpKills()
    {
        // Add karma to attacker and increase its PK counter
        setPvpKills(getPvpKills() + 1);

        // Send a Server->Client UserInfo packet to attacker with its Karma and PK Counter
        sendPacket(new UserInfo(this));
    }

    /**
     * Increase pk count, karma and send the info to the player
     *
     * @param targLVL : level of the killed player
     */
    public void increasePkKillsAndKarma(int targLVL)
    {
        int baseKarma           = Config.KARMA_MIN_KARMA;
        int newKarma            = baseKarma;
        int karmaLimit          = Config.KARMA_MAX_KARMA;

        int pkLVL               = getLevel();
        int pkPKCount           = getPkKills();

        int lvlDiffMulti = 0;
        int pkCountMulti = 0;

        // Check if the attacker has a PK counter greater than 0
        if ( pkPKCount > 0 )
            pkCountMulti = pkPKCount / 2;
        else
            pkCountMulti = 1;
        if ( pkCountMulti < 1 ) pkCountMulti = 1;

        // Calculate the level difference Multiplier between attacker and killed L2PcInstance
        if ( pkLVL > targLVL )
            lvlDiffMulti = pkLVL / targLVL;
        else
            lvlDiffMulti = 1;
        if ( lvlDiffMulti < 1 ) lvlDiffMulti = 1;

        // Calculate the new Karma of the attacker : newKarma = baseKarma*pkCountMulti*lvlDiffMulti
        newKarma *= pkCountMulti;
        newKarma *= lvlDiffMulti;

        // Make sure newKarma is less than karmaLimit and higher than baseKarma
        if ( newKarma < baseKarma ) newKarma = baseKarma;
        if ( newKarma > karmaLimit ) newKarma = karmaLimit;

        // Fix to prevent overflow (=> karma has a  max value of 2 147 483 647)
        if (getKarma() > (Integer.MAX_VALUE - newKarma))
            newKarma = Integer.MAX_VALUE - getKarma();

        // Add karma to attacker and increase its PK counter
        setPkKills(getPkKills() + 1);
        setKarma(getKarma() + newKarma);

        // Send a Server->Client UserInfo packet to attacker with its Karma and PK Counter
        sendPacket(new UserInfo(this));
    }

	public int calculateKarmaLost(long exp)
	{
		// KARMA LOSS
		// When a PKer gets killed by another player or a L2MonsterInstance, it loses a certain amount of Karma based on their level.
		// this (with defaults) results in a level 1 losing about ~2 karma per death, and a lvl 70 loses about 11760 karma per death...
		// You lose karma as long as you were not in a pvp zone and you did not kill urself.
		// NOTE: exp for death (if delevel is allowed) is based on the players level

		long expGained = Math.abs(exp);
		expGained /= Config.KARMA_XP_DIVIDER;

		// FIXME Micht : Maybe this code should be fixed and karma set to a long value
		int karmaLost = 0;
		if (expGained > Integer.MAX_VALUE)
			karmaLost = Integer.MAX_VALUE;
		else
			karmaLost = (int)expGained;

		if (karmaLost < Config.KARMA_LOST_BASE) karmaLost = Config.KARMA_LOST_BASE;
		if (karmaLost > getKarma()) karmaLost = getKarma();

		return karmaLost;
	}

	public void updatePvPStatus()
	{
		if (isInsideZone(ZONE_PVP)) return;
		setPvpFlagLasts(System.currentTimeMillis() + Config.PVP_NORMAL_TIME);

		if (getPvpFlag() == 0)
			startPvPFlag();
	}

	public void updatePvPStatus(L2Character target)
	{
		L2PcInstance player_target = null;

        if (target instanceof L2PcInstance)
        	player_target = (L2PcInstance)target;
        else if (target instanceof L2Summon)
        	player_target = ((L2Summon) target).getOwner();

        if (player_target == null)
           return;

        if ((isInDuel() && player_target.getDuelId() == getDuelId())) return;
        if ((!isInsideZone(ZONE_PVP) || !player_target.isInsideZone(ZONE_PVP)) && player_target.getKarma() == 0)
        {
        	if (checkIfPvP(player_target))
        		setPvpFlagLasts(System.currentTimeMillis() + Config.PVP_PVP_TIME);
        	else
        		setPvpFlagLasts(System.currentTimeMillis() + Config.PVP_NORMAL_TIME);
			if (getPvpFlag() == 0)
				startPvPFlag();
        }
	}

	/**
	 * Restore the specified % of experience this L2PcInstance has
	 * lost and sends a Server->Client StatusUpdate packet.<BR><BR>
	 */
	public void restoreExp(double restorePercent)
	{
		if (getExpBeforeDeath() > 0)
		{
			// Restore the specified % of lost experience.
			getStat().addExp((int)Math.round((getExpBeforeDeath() - getExp()) * restorePercent / 100));
			setExpBeforeDeath(0);
		}
	}

	/**
	 * Reduce the Experience (and level if necessary) of the L2PcInstance in function of the calculated Death Penalty.<BR><BR>
	 *
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Calculate the Experience loss </li>
	 * <li>Set the value of _expBeforeDeath </li>
	 * <li>Set the new Experience value of the L2PcInstance and Decrease its level if necessary </li>
	 * <li>Send a Server->Client StatusUpdate packet with its new Experience </li><BR><BR>
	 *
	 */
	public void deathPenalty(boolean atwar, boolean killed_by_pc, boolean charmOfCourage)
	{
		// TODO Need Correct Penalty
		// Get the level of the L2PcInstance
		final int lvl = getLevel();

		byte level = (byte)getLevel();
		
		int clan_luck = getSkillLevel(L2Skill.SKILL_CLAN_LUCK);
		
		double clan_luck_modificator = 1.0;	
		
		if (!killed_by_pc)
		{
			switch (clan_luck)
			{
				case 3:
					clan_luck_modificator = 0.8;
					break;
				case 2:
					clan_luck_modificator = 0.8;
					break;
				case 1:
					clan_luck_modificator = 0.88;
					break;
				default:
					clan_luck_modificator = 1.0;
					break;				
			}
		}
		else
		{
			switch (clan_luck)
			{
				case 3:
					clan_luck_modificator = 0.5;
					break;
				case 2:
					clan_luck_modificator = 0.5;
					break;
				case 1:
					clan_luck_modificator = 0.5;
					break;
				default:
					clan_luck_modificator = 1.0;
					break;				
			}
		}	
		
		//The death steal you some Exp
		double percentLost = (1.0*clan_luck_modificator);
		
		switch (level)
		{
			case 78:
				percentLost = (1.5*clan_luck_modificator);
				break;
			case 77:
				percentLost = (2.0*clan_luck_modificator);
				break;
			case 76:
				percentLost = (2.5*clan_luck_modificator);
				break;
			default:
				if (level < 40)
					percentLost = (7.0*clan_luck_modificator);
				else if (level >= 40 && level <= 75)
					percentLost = (4.0*clan_luck_modificator);
				
				break;
		}

		if (getKarma() > 0)
            percentLost *= Config.RATE_KARMA_EXP_LOST;

		if (isFestivalParticipant() || atwar || isInsideZone(ZONE_SIEGE))
            percentLost /= 4.0;

		// Calculate the Experience loss
		long lostExp = 0;
		if (!atEvent)
			if (lvl < Experience.MAX_LEVEL)
				lostExp = Math.round((getStat().getExpForLevel(lvl+1) - getStat().getExpForLevel(lvl)) * percentLost /100);
			else
				lostExp = Math.round((getStat().getExpForLevel(Experience.MAX_LEVEL) - getStat().getExpForLevel(Experience.MAX_LEVEL - 1)) * percentLost /100);

		// Get the Experience before applying penalty
		setExpBeforeDeath(getExp());

		if(charmOfCourage)
		{
		    if (getSiegeState() > 0 && isInsideZone(ZONE_SIEGE))
		    	lostExp = 0;
		}

        if (Config.DEBUG)
            _log.fine(getName() + " died and lost " + lostExp + " experience.");

		// Set the new Experience value of the L2PcInstance
		getStat().addExp(-lostExp);
	}
	
	public void deathPenalty(boolean atwar, boolean killed_by_pc)
	{
		deathPenalty(atwar, killed_by_pc, getCharmOfCourage());
	}

	/**
	 * @param b
	 */
	public void setPartyMatchingAutomaticRegistration(boolean b)
	{
		_partyMatchingAutomaticRegistration = b;
	}

	/**
	 * @param b
	 */
	public void setPartyMatchingShowLevel(boolean b)
	{
		_partyMatchingShowLevel = b;
	}

	/**
	 * @param b
	 */
	public void setPartyMatchingShowClass(boolean b)
	{
		_partyMatchingShowClass = b;
	}

	/**
	 * @param memo
	 */
	public void setPartyMatchingMemo(String memo)
	{
		_partyMatchingMemo = memo;
	}

	public boolean isPartyMatchingAutomaticRegistration()
	{
		return _partyMatchingAutomaticRegistration;
	}

	public String getPartyMatchingMemo()
	{
		return _partyMatchingMemo;
	}

	public boolean isPartyMatchingShowClass()
	{
		return _partyMatchingShowClass;
	}

	public boolean isPartyMatchingShowLevel()
	{
		return _partyMatchingShowLevel;
	}

	/**
	 * Manage the increase level task of a L2PcInstance (Max MP, Max MP, Recommandation, Expertise and beginner skills...).<BR><BR>
	 *
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Send a Server->Client System Message to the L2PcInstance : YOU_INCREASED_YOUR_LEVEL </li>
	 * <li>Send a Server->Client packet StatusUpdate to the L2PcInstance with new LEVEL, MAX_HP and MAX_MP </li>
	 * <li>Set the current HP and MP of the L2PcInstance, Launch/Stop a HP/MP/CP Regeneration Task and send StatusUpdate packet to all other L2PcInstance to inform (exclusive broadcast)</li>
	 * <li>Recalculate the party level</li>
	 * <li>Recalculate the number of Recommandation that the L2PcInstance can give</li>
	 * <li>Give Expertise skill of this level and remove beginner Lucky skill</li><BR><BR>
	 *
	 */
	public void increaseLevel()
	{
		// Set the current HP and MP of the L2Character, Launch/Stop a HP/MP/CP Regeneration Task and send StatusUpdate packet to all other L2PcInstance to inform (exclusive broadcast)
		setCurrentHpMp(getMaxHp(),getMaxMp());
		setCurrentCp(getMaxCp());
	}

	/**
	 * Stop the HP/MP/CP Regeneration task.<BR><BR>
	 *
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Set the RegenActive flag to False </li>
	 * <li>Stop the HP/MP/CP Regeneration task </li><BR><BR>
	 *
	 */
	public void stopAllTimers()
	{
		stopHpMpRegeneration();
		stopWarnUserTakeBreak();
		stopWaterTask();
		stopRentPet();
		stopPvpRegTask();
		stopJailTask(true);
		stopSoulTask();
	}

	/**
	 * Return the L2Summon of the L2PcInstance or null.<BR><BR>
	 */
	@Override
	public L2Summon getPet()
	{
		return _summon;
	}
	
	/**
     * Return the L2Decoy of the L2PcInstance or null.<BR><BR>
     */
    public L2Decoy getDecoy()
    {
        return _decoy;
    }
    
     /**
     * Return the L2Trap of the L2PcInstance or null.<BR><BR>
     */
    public L2Trap getTrap()
    {
    	return _trap;
    }

	/**
	 * Set the L2Summon of the L2PcInstance.<BR><BR>
	 */
	public void setPet(L2Summon summon)
	{
		_summon = summon;
	}
    /**
     * Set the L2Decoy of the L2PcInstance.<BR><BR>
     */
    public void setDecoy(L2Decoy decoy)
    {
        _decoy = decoy;
    }
    
    /**
     * Set the L2Trap of this L2PcInstance<BR><BR>
     * @param trap
     */
    public void setTrap(L2Trap trap)
    {
    	_trap = trap;
    }
    
	/**
	 * Return the L2Summon of the L2PcInstance or null.<BR><BR>
	 */
	public L2TamedBeastInstance getTrainedBeast()
	{
		return _tamedBeast;
	}

	/**
	 * Set the L2Summon of the L2PcInstance.<BR><BR>
	 */
	public void setTrainedBeast(L2TamedBeastInstance tamedBeast)
	{
		_tamedBeast = tamedBeast;
	}

	/**
	 * Return the L2PcInstance requester of a transaction (ex : FriendInvite, JoinAlly, JoinParty...).<BR><BR>
	 */
	public L2Request getRequest()
	{
		return _request;
	}

	/**
	 * Set the L2PcInstance requester of a transaction (ex : FriendInvite, JoinAlly, JoinParty...).<BR><BR>
	 */
	public synchronized void setActiveRequester(L2PcInstance requester)
	{
		_activeRequester = requester;
	}

	/**
	 * Return the L2PcInstance requester of a transaction (ex : FriendInvite, JoinAlly, JoinParty...).<BR><BR>
	 */
	public L2PcInstance getActiveRequester()
	{
		return _activeRequester;
	}

	/**
	 * Return True if a transaction is in progress.<BR><BR>
	 */
	public boolean isProcessingRequest()
	{
		return _activeRequester != null || _requestExpireTime > GameTimeController.getGameTicks();
	}

	/**
	 * Return True if a transaction is in progress.<BR><BR>
	 */
	public boolean isProcessingTransaction() { return _activeRequester != null || _activeTradeList != null || _requestExpireTime > GameTimeController.getGameTicks(); }

	/**
	 * Select the Warehouse to be used in next activity.<BR><BR>
	 */
	public void onTransactionRequest(L2PcInstance partner)
	{
		_requestExpireTime = GameTimeController.getGameTicks() + REQUEST_TIMEOUT * GameTimeController.TICKS_PER_SECOND;
		partner.setActiveRequester(this);
	}

	/**
	 * Select the Warehouse to be used in next activity.<BR><BR>
	 */
	public void onTransactionResponse()
    {
        _requestExpireTime = 0;
    }

	/**
	 * Select the Warehouse to be used in next activity.<BR><BR>
	 */
	public void setActiveWarehouse(ItemContainer warehouse)
    {
        _activeWarehouse = warehouse;
    }

	/**
	 * Return active Warehouse.<BR><BR>
	 */
	public ItemContainer getActiveWarehouse()
    {
        return _activeWarehouse;
    }

	/**
	 * Select the TradeList to be used in next activity.<BR><BR>
	 */
	public void setActiveTradeList(TradeList tradeList)
    {
        _activeTradeList = tradeList;
    }

	/**
	 * Return active TradeList.<BR><BR>
	 */
	public TradeList getActiveTradeList()
    {
        return _activeTradeList;
    }

	public void onTradeStart(L2PcInstance partner)
	{
		_activeTradeList = new TradeList(this);
		_activeTradeList.setPartner(partner);

		SystemMessage msg = new SystemMessage(SystemMessageId.BEGIN_TRADE_WITH_S1);
		msg.addPcName(partner);
		sendPacket(msg);
		sendPacket(new TradeStart(this));
	}

	public void onTradeConfirm(L2PcInstance partner)
	{
		SystemMessage msg = new SystemMessage(SystemMessageId.S1_CONFIRMED_TRADE);
		msg.addPcName(partner);
		sendPacket(msg);
	}

	public void onTradeCancel(L2PcInstance partner)
	{
		if (_activeTradeList == null)
            return;

		_activeTradeList.lock();
		_activeTradeList = null;

		sendPacket(new TradeDone(0));
		SystemMessage msg = new SystemMessage(SystemMessageId.S1_CANCELED_TRADE);
		msg.addPcName(partner);
		sendPacket(msg);
	}

	public void onTradeFinish(boolean successfull)
	{
		_activeTradeList = null;
		sendPacket(new TradeDone(1));
        if (successfull)
            sendPacket(new SystemMessage(SystemMessageId.TRADE_SUCCESSFUL));
	}

	public void startTrade(L2PcInstance partner)
	{
		onTradeStart(partner);
		partner.onTradeStart(this);
	}

	public void cancelActiveTrade()
	{
		if (_activeTradeList == null)
            return;

		L2PcInstance partner = _activeTradeList.getPartner();
		if (partner != null) partner.onTradeCancel(this);
		onTradeCancel(this);
	}

	/**
	 * Return the _createList object of the L2PcInstance.<BR><BR>
	 */
	public L2ManufactureList getCreateList()
	{
		return _createList;
	}

	/**
	 * Set the _createList object of the L2PcInstance.<BR><BR>
	 */
	public void setCreateList(L2ManufactureList x)
	{
		_createList = x;
	}

	/**
	 * Return the _buyList object of the L2PcInstance.<BR><BR>
	 */
	public TradeList getSellList()
	{
		if (_sellList == null) _sellList = new TradeList(this);
		return _sellList;
	}

	/**
	 * Return the _buyList object of the L2PcInstance.<BR><BR>
	 */
	public TradeList getBuyList()
	{
		if (_buyList == null) _buyList = new TradeList(this);
		return _buyList;
	}

	/**
	 * Set the Private Store type of the L2PcInstance.<BR><BR>
	 *
	 * <B><U> Values </U> :</B><BR><BR>
	 * <li>0 : STORE_PRIVATE_NONE</li>
	 * <li>1 : STORE_PRIVATE_SELL</li>
	 * <li>2 : sellmanage</li><BR>
	 * <li>3 : STORE_PRIVATE_BUY</li><BR>
	 * <li>4 : buymanage</li><BR>
	 * <li>5 : STORE_PRIVATE_MANUFACTURE</li><BR>
	 *
	 */
	public void setPrivateStoreType(int type)
	{
		_privatestore = type;
	}

	/**
	 * Return the Private Store type of the L2PcInstance.<BR><BR>
	 *
	 * <B><U> Values </U> :</B><BR><BR>
	 * <li>0 : STORE_PRIVATE_NONE</li>
	 * <li>1 : STORE_PRIVATE_SELL</li>
	 * <li>2 : sellmanage</li><BR>
	 * <li>3 : STORE_PRIVATE_BUY</li><BR>
	 * <li>4 : buymanage</li><BR>
	 * <li>5 : STORE_PRIVATE_MANUFACTURE</li><BR>
	 *
	 */
	public int getPrivateStoreType()
	{
		return _privatestore;
	}

	/**
	 * Set the _skillLearningClassId object of the L2PcInstance.<BR><BR>
	 */
	public void setSkillLearningClassId(ClassId classId)
	{
		_skillLearningClassId = classId;
	}

	/**
	 * Return the _skillLearningClassId object of the L2PcInstance.<BR><BR>
	 */
	public ClassId getSkillLearningClassId()
	{
		return _skillLearningClassId;
	}

	/**
	 * Set the _clan object, _clanId, _clanLeader Flag and title of the L2PcInstance.<BR><BR>
	 */
	public void setClan(L2Clan clan)
	{
		_clan = clan;
		setTitle("");

		if (clan == null)
		{
			_clanId = 0;
			_clanPrivileges = 0;
			_pledgeType = 0;
			_powerGrade = 0;
			_lvlJoinedAcademy = 0;
			_apprentice = 0;
			_sponsor = 0;
			return;
		}

		if (!clan.isMember(getObjectId()))
		{
			// char has been kicked from clan
			setClan(null);
			return;
		}

		_clanId = clan.getClanId();
	}

	/**
	 * Return the _clan object of the L2PcInstance.<BR><BR>
	 */
	public L2Clan getClan()
	{
		return _clan;
	}

	/**
	 * Return True if the L2PcInstance is the leader of its clan.<BR><BR>
	 */
	public boolean isClanLeader()
	{
		if (getClan() == null)
		{
			return false;
		}
		else
		{
			return getObjectId() == getClan().getLeaderId();
		}
	}

	/**
	 * Reduce the number of arrows/bolts owned by the L2PcInstance and send it Server->Client Packet InventoryUpdate or ItemList (to unequip if the last arrow was consummed).<BR><BR>
	 */
	@Override
	protected void reduceArrowCount(boolean bolts)
	{
	    L2ItemInstance arrows = getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);

	    if (arrows == null)
        {
            getInventory().unEquipItemInSlot(Inventory.PAPERDOLL_LHAND);
            if (bolts) 
                _boltItem = null;
            else
                _arrowItem = null;
            sendPacket(new ItemList(this,false));
            return;
        }
	    
	    // Adjust item quantity
        if (arrows.getCount() > 1)
        {
            synchronized(arrows)
            {
                arrows.changeCountWithoutTrace(-1, this, null);
                arrows.setLastChange(L2ItemInstance.MODIFIED);

                // could do also without saving, but let's save approx 1 of 10
                if(GameTimeController.getGameTicks() % 10 == 0)
                    arrows.updateDatabase();
                _inventory.refreshWeight();
            }
        }
        else
        {
            // Destroy entire item and save to database
            _inventory.destroyItem("Consume", arrows, this, null);
            
            getInventory().unEquipItemInSlot(Inventory.PAPERDOLL_LHAND);
            if (bolts) 
                _boltItem = null;
            else
                _arrowItem = null;

            if (Config.DEBUG) _log.fine("removed arrows count");
            sendPacket(new ItemList(this,false));
            return;
        }
	    
		if (!Config.FORCE_INVENTORY_UPDATE)
		{
		    InventoryUpdate iu = new InventoryUpdate();
		    iu.addModifiedItem(arrows);
		    sendPacket(iu);
		}
		else sendPacket(new ItemList(this, false));
	}

	/**
	 * Equip arrows needed in left hand and send a Server->Client packet ItemList to the L2PcINstance then return True.<BR><BR>
	 */
	@Override
	protected boolean checkAndEquipArrows()
	{
		// Check if nothing is equiped in left hand
		if (getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND) == null)
		{
			// Get the L2ItemInstance of the arrows needed for this bow
			_arrowItem = getInventory().findArrowForBow(getActiveWeaponItem());

			if (_arrowItem != null)
			{
				// Equip arrows needed in left hand
				getInventory().setPaperdollItem(Inventory.PAPERDOLL_LHAND, _arrowItem);

				// Send a Server->Client packet ItemList to this L2PcINstance to update left hand equipement
				ItemList il = new ItemList(this, false);
				sendPacket(il);
			}
		}
		else
		{
			// Get the L2ItemInstance of arrows equiped in left hand
			_arrowItem = getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);
		}

		return _arrowItem != null;
	}

    /**
     * Equip bolts needed in left hand and send a Server->Client packet ItemList to the L2PcINstance then return True.<BR><BR>
     */
    @Override
    protected boolean checkAndEquipBolts()
    {
        // Check if nothing is equiped in left hand
        if (getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND) == null)
        {
            // Get the L2ItemInstance of the arrows needed for this bow
            _boltItem = getInventory().findBoltForCrossBow(getActiveWeaponItem());

            if (_boltItem != null)
            {
                // Equip arrows needed in left hand
                getInventory().setPaperdollItem(Inventory.PAPERDOLL_LHAND, _boltItem);

                // Send a Server->Client packet ItemList to this L2PcINstance to update left hand equipement
                ItemList il = new ItemList(this, false);
                sendPacket(il);
            }
        }
        else
        {
            // Get the L2ItemInstance of arrows equiped in left hand
            _boltItem = getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);
        }

        return _boltItem != null;
    }
	/**
	 * Disarm the player's weapon and shield.<BR><BR>
	 */
	public boolean disarmWeapons()
	{
        // Don't allow disarming a cursed weapon
        if (isCursedWeaponEquipped()) return false;

        // Unequip the weapon
        L2ItemInstance wpn = getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);
        if (wpn == null) wpn = getInventory().getPaperdollItem(Inventory.PAPERDOLL_LRHAND);
        if (wpn != null)
        {
        	if (wpn.isWear())
        		return false;

            L2ItemInstance[] unequiped = getInventory().unEquipItemInBodySlotAndRecord(wpn.getItem().getBodyPart());
            InventoryUpdate iu = new InventoryUpdate();
            for (int i = 0; i < unequiped.length; i++)
                iu.addModifiedItem(unequiped[i]);
            sendPacket(iu);

            abortAttack();
            broadcastUserInfo();

            // this can be 0 if the user pressed the right mousebutton twice very fast
            if (unequiped.length > 0)
            {
                SystemMessage sm = null;
                if (unequiped[0].getEnchantLevel() > 0)
                {
                    sm = new SystemMessage(SystemMessageId.EQUIPMENT_S1_S2_REMOVED);
                    sm.addNumber(unequiped[0].getEnchantLevel());
                    sm.addItemName(unequiped[0]);
                }
                else
                {
                    sm = new SystemMessage(SystemMessageId.S1_DISARMED);
                    sm.addItemName(unequiped[0]);
                }
                sendPacket(sm);
            }
        }

        // Unequip the shield
        L2ItemInstance sld = getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);
        if (sld != null)
        {
        	if (sld.isWear())
        		return false;

            L2ItemInstance[] unequiped = getInventory().unEquipItemInBodySlotAndRecord(sld.getItem().getBodyPart());
            InventoryUpdate iu = new InventoryUpdate();
            for (int i = 0; i < unequiped.length; i++)
                iu.addModifiedItem(unequiped[i]);
            sendPacket(iu);

            abortAttack();
            broadcastUserInfo();

            // this can be 0 if the user pressed the right mousebutton twice very fast
            if (unequiped.length > 0)
            {
                SystemMessage sm = null;
                if (unequiped[0].getEnchantLevel() > 0)
                {
                    sm = new SystemMessage(SystemMessageId.EQUIPMENT_S1_S2_REMOVED);
                    sm.addNumber(unequiped[0].getEnchantLevel());
                    sm.addItemName(unequiped[0]);
                }
                else
                {
                    sm = new SystemMessage(SystemMessageId.S1_DISARMED);
                    sm.addItemName(unequiped[0]);
                }
                sendPacket(sm);
            }
        }
        return true;
	}
    
    public boolean mount(L2Summon pet)
    {
        if (!disarmWeapons())
            return false;
        if (isTransformed())
        	return false;
        
        Ride mount = new Ride(this, true, pet.getTemplate().npcId);
        this.setMount(pet.getTemplate().npcId, mount.getMountType());
        this.setMountObjectID(pet.getControlItemId());
        this.broadcastPacket(mount);
        
        // Notify self and others about speed change
        this.broadcastUserInfo();
        
        pet.unSummon(this);
        
        return true;
    }
    
    public boolean mount(int npcId, int controlItemObjId)
    {
        if (!disarmWeapons())
            return false;
        if (isTransformed())
        	return false;
        
        Ride mount = new Ride(this, true, npcId);
        if (setMount(npcId, mount.getMountType()))
        {
        	setMountObjectID(controlItemObjId);
        	broadcastPacket(mount);
        
        	// Notify self and others about speed change
        	broadcastUserInfo();
        	return true;
        }
        return false;
    }
	
	public boolean dismount()
	{
	    if (setMount(0, 0))
	    {
	        if (isFlying()) 
	            removeSkill(SkillTable.getInstance().getInfo(4289, 1));
	        Ride dismount = new Ride(this, false, 0);
	        broadcastPacket(dismount);
	        setMountObjectID(0);
            
            // Notify self and others about speed change
            this.broadcastUserInfo();
	        return true;
	    }
        return false;
	}

	/**
	 * Return True if the L2PcInstance use a dual weapon.<BR><BR>
	 */
	@Override
	public boolean isUsingDualWeapon()
	{
		L2Weapon weaponItem = getActiveWeaponItem();
		if (weaponItem == null) return false;

		if (weaponItem.getItemType() == L2WeaponType.DUAL)
			return true;
		else if (weaponItem.getItemType() == L2WeaponType.DUALFIST)
			return true;
		else if (weaponItem.getItemId() == 248) // orc fighter fists
			return true;
		else if (weaponItem.getItemId() == 252) // orc mage fists
			return true;
		else
			return false;
	}

	public void setUptime(long time)
	{
		_uptime = time;
	}

	public long getUptime()
	{
		return System.currentTimeMillis()-_uptime;
	}

	/**
	 * Return True if the L2PcInstance is invulnerable.<BR><BR>
	 */
	@Override
	public boolean isInvul()
	{
		return _isInvul  || _isTeleporting ||  _protectEndTime > GameTimeController.getGameTicks();
	}

	/**
	 * Return True if the L2PcInstance has a Party in progress.<BR><BR>
	 */
	@Override
	public boolean isInParty()
	{
		return _party != null;
	}

	/**
	 * Set the _party object of the L2PcInstance (without joining it).<BR><BR>
	 */
	public void setParty(L2Party party)
	{
		_party = party;
	}

	/**
	 * Set the _party object of the L2PcInstance AND join it.<BR><BR>
	 */
	public void joinParty(L2Party party)
	{
		if (party != null)
		{
            // First set the party otherwise this wouldn't be considered
            // as in a party into the L2Character.updateEffectIcons() call.
            _party = party;
			party.addPartyMember(this);
		}
	}


	/**
	 * Manage the Leave Party task of the L2PcInstance.<BR><BR>
	 */
	public void leaveParty()
	{
		if (isInParty())
		{
			_party.removePartyMember(this);
			_party = null;
		}
	}

	/**
	 * Return the _party object of the L2PcInstance.<BR><BR>
	 */
	@Override
	public L2Party getParty()
	{
		return _party;
	}

	/**
	 * Return True if the L2PcInstance is a GM.<BR><BR>
	 */
	public boolean isGM()
	{
		return getAccessLevel().isGm();
	}

	/**
	 * Manage a cancel cast task for the L2PcInstance.<BR><BR>
	 *
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Set the Intention of the AI to AI_INTENTION_IDLE </li>
	 * <li>Enable all skills (set _allSkillsDisabled to False) </li>
	 * <li>Send a Server->Client Packet MagicSkillCanceld to the L2PcInstance and all L2PcInstance in the _KnownPlayers of the L2Character (broadcast) </li><BR><BR>
	 *
	 */
	public void cancelCastMagic()
	{
		// Set the Intention of the AI to AI_INTENTION_IDLE
		getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);

		// Enable all skills (set _allSkillsDisabled to False)
		enableAllSkills();

		// Send a Server->Client Packet MagicSkillCanceld to the L2PcInstance and all L2PcInstance in the _KnownPlayers of the L2Character (broadcast)
		MagicSkillCanceld msc = new MagicSkillCanceld(getObjectId());

        // Broadcast the packet to self and known players.
        Broadcast.toSelfAndKnownPlayersInRadius(this, msc, 810000/*900*/);
	}

	/**
	 * Set the _accessLevel of the L2PcInstance.<BR><BR>
	 */
	public void setAccessLevel(int level)
	{
		if (level == AccessLevels._masterAccessLevelNum)
		{
			_log.warning( "Master access level set for character " + getName() + "! Just a warning to be careful ;)" );
			_accessLevel = AccessLevels._masterAccessLevel;
		}
		else if (level == AccessLevels._userAccessLevelNum)
			_accessLevel = AccessLevels._userAccessLevel;
		else
		{
			AccessLevel accessLevel = AccessLevels.getInstance().getAccessLevel(level);

			if (accessLevel == null)
			{
				if (level < 0)
				{
					AccessLevels.getInstance().addBanAccessLevel(level);
					_accessLevel = AccessLevels.getInstance().getAccessLevel(level);
				}
				else
				{
					_log.warning( "Tryed to set unregistered access level " + level + " to character " + getName() + ". Setting access level without privileges!" );
					_accessLevel = AccessLevels._userAccessLevel;
				}
			}
			else
				_accessLevel = accessLevel;
		}

		if (_accessLevel != AccessLevels._userAccessLevel)
		{
			getAppearance().setNameColor(_accessLevel.getNameColor());
			getAppearance().setTitleColor(_accessLevel.getTitleColor());
			broadcastUserInfo();
		}
	}

	public void setAccountAccesslevel(int level)
	{
		LoginServerThread.getInstance().sendAccessLevel(getAccountName(),level);
	}

	/**
	 * Return the _accessLevel of the L2PcInstance.<BR><BR>
	 */
	public AccessLevel getAccessLevel()
	{
		if (Config.EVERYBODY_HAS_ADMIN_RIGHTS)
			return AccessLevels._masterAccessLevel;
		else if ( _accessLevel == null ) /* This is here because inventory etc. is loaded before access level on login, so it is not null */
			setAccessLevel(AccessLevels._userAccessLevelNum);

		return _accessLevel;
	}

	@Override
	public double getLevelMod()
	{
		return (100.0 - 11 + getLevel()) / 100.0;
	}

	/**
	 * Update Stats of the L2PcInstance client side by sending Server->Client packet UserInfo/StatusUpdate to this L2PcInstance and CharInfo/StatusUpdate to all L2PcInstance in its _KnownPlayers (broadcast).<BR><BR>
	 */
	public void updateAndBroadcastStatus(int broadcastType)
	{
		refreshOverloaded();
		refreshExpertisePenalty();
		// Send a Server->Client packet UserInfo to this L2PcInstance and CharInfo to all L2PcInstance in its _KnownPlayers (broadcast)
		if (broadcastType == 1) this.sendPacket(new UserInfo(this));
		if (broadcastType == 2) broadcastUserInfo();
	}

	/**
	 * Send a Server->Client StatusUpdate packet with Karma and PvP Flag to the L2PcInstance and all L2PcInstance to inform (broadcast).<BR><BR>
	 */
	public void setKarmaFlag(int flag)
	{
		sendPacket(new UserInfo(this));
		Collection<L2PcInstance> plrs = getKnownList().getKnownPlayers().values();
		//synchronized (getKnownList().getKnownPlayers())
		{
			for (L2PcInstance player : plrs)
			{
				player.sendPacket(new RelationChanged(this, getRelation(player), isAutoAttackable(player)));
				if (getPet() != null)
					player.sendPacket(new RelationChanged(getPet(), getRelation(player), isAutoAttackable(player)));
			}
		}
	}

	/**
	 * Send a Server->Client StatusUpdate packet with Karma to the L2PcInstance and all L2PcInstance to inform (broadcast).<BR><BR>
	 */
	public void broadcastKarma()
	{
        StatusUpdate su = new StatusUpdate(getObjectId());
        su.addAttribute(StatusUpdate.KARMA, getKarma());
        sendPacket(su);

        Collection<L2PcInstance> plrs = getKnownList().getKnownPlayers().values();
        //synchronized (getKnownList().getKnownPlayers())
		{
			for (L2PcInstance player : plrs)
			{
				player.sendPacket(new RelationChanged(this, getRelation(player), isAutoAttackable(player)));
				if (getPet() != null)
					player.sendPacket(new RelationChanged(getPet(), getRelation(player), isAutoAttackable(player)));
			}
		}
	}

	/**
	 * Set the online Flag to True or False and update the characters table of the database with online status and lastAccess (called when login and logout).<BR><BR>
	 */
	public void setOnlineStatus(boolean isOnline)
	{
		if (_isOnline != isOnline)
			_isOnline = isOnline;

		// Update the characters table of the database with online status and lastAccess (called when login and logout)
		updateOnlineStatus();
	}
    
    public void setIsIn7sDungeon(boolean isIn7sDungeon)
    {
        _isIn7sDungeon = isIn7sDungeon;
    }

	/**
	 * Update the characters table of the database with online status and lastAccess of this L2PcInstance (called when login and logout).<BR><BR>
	 */
	public void updateOnlineStatus()
	{
		Connection con = null;
		try
        {
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("UPDATE characters SET online=?, lastAccess=? WHERE charId=?");
			statement.setInt(1, isOnline());
			statement.setLong(2, System.currentTimeMillis());
			statement.setInt(3, getObjectId());
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			_log.warning("could not set char online status:"+e);
		}
		finally
		{
			try { con.close(); } catch (Exception e) {}
		}
	}

	/**
	 * Create a new player in the characters table of the database.<BR><BR>
	 */
	private boolean createDb()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(INSERT_CHARACTER);
			
			statement.setString(1, _accountName);
			statement.setInt(2, getObjectId());
			statement.setString(3, getName());
			statement.setInt(4, getLevel());
			statement.setInt(5, getMaxHp());
			statement.setDouble(6, getCurrentHp());
			statement.setInt(7, getMaxCp());
			statement.setDouble(8, getCurrentCp());
			statement.setInt(9, getMaxMp());
			statement.setDouble(10, getCurrentMp());
			statement.setInt(11, getAppearance().getFace());
			statement.setInt(12, getAppearance().getHairStyle());
			statement.setInt(13, getAppearance().getHairColor());
			statement.setInt(14, getAppearance().getSex()? 1 : 0);
			statement.setLong(15, getExp());
			statement.setInt(16, getSp());
			statement.setInt(17, getKarma());
			statement.setInt(18, getPvpKills());
			statement.setInt(19, getPkKills());
			statement.setInt(20, getClanId());
			statement.setInt(21, getRace().ordinal());
			statement.setInt(22, getClassId().getId());
			statement.setLong(23, getDeleteTimer());
			statement.setInt(24, hasDwarvenCraft() ? 1 : 0);
			statement.setString(25, getTitle());
			statement.setInt(26, getAccessLevel().getLevel());
			statement.setInt(27, isOnline());
            statement.setInt(28, isIn7sDungeon() ? 1 : 0);
			statement.setInt(29, getClanPrivileges());
			statement.setInt(30, getWantsPeace());
			statement.setInt(31, getBaseClass());
			statement.setInt(32, getNewbie());
			statement.setInt(33, isNoble() ? 1 :0);
			statement.setLong(34, 0);
			statement.setLong(35,System.currentTimeMillis());
			
			statement.executeUpdate();
			statement.close();
		}
		catch (Exception e)
		{
			_log.severe("Could not insert char data: " + e);
			return false;
		}
		finally
		{
			try { con.close(); } catch (Exception e) {}
		}
		return true;
	}

	/**
	 * Retrieve a L2PcInstance from the characters table of the database and add it in _allObjects of the L2world.<BR><BR>
	 *
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Retrieve the L2PcInstance from the characters table of the database </li>
	 * <li>Add the L2PcInstance object in _allObjects </li>
	 * <li>Set the x,y,z position of the L2PcInstance and make it invisible</li>
	 * <li>Update the overloaded status of the L2PcInstance</li><BR><BR>
	 *
	 * @param objectId Identifier of the object to initialized
	 *
	 * @return The L2PcInstance loaded from the database
	 *
	 */
	private static L2PcInstance restore(int objectId)
	{
		L2PcInstance player = null;
		Connection con = null;

		try
		{
			// Retrieve the L2PcInstance from the characters table of the database
			con = L2DatabaseFactory.getInstance().getConnection();

			PreparedStatement statement = con.prepareStatement(RESTORE_CHARACTER);
			statement.setInt(1, objectId);
			ResultSet rset = statement.executeQuery();

			double currentCp = 0;
			double currentHp = 0;
			double currentMp = 0;

			while (rset.next())
			{
				final int activeClassId = rset.getInt("classid");
				final boolean female = rset.getInt("sex")!=0;
				final L2PcTemplate template = CharTemplateTable.getInstance().getTemplate(activeClassId);
				PcAppearance app = new PcAppearance(rset.getByte("face"), rset.getByte("hairColor"), rset.getByte("hairStyle"), female);

				player = new L2PcInstance(objectId, template, rset.getString("account_name"), app);
				player.setName(rset.getString("char_name"));
				player._lastAccess = rset.getLong("lastAccess");

				player.getStat().setExp(rset.getLong("exp"));
				player.setExpBeforeDeath(rset.getLong("expBeforeDeath"));
				player.getStat().setLevel(rset.getByte("level"));
				player.getStat().setSp(rset.getInt("sp"));

				player.setWantsPeace(rset.getInt("wantspeace"));

				player.setHeading(rset.getInt("heading"));

				player.setKarma(rset.getInt("karma"));
				player.setPvpKills(rset.getInt("pvpkills"));
				player.setPkKills(rset.getInt("pkkills"));
				player.setOnlineTime(rset.getLong("onlinetime"));
				player.setNewbie(rset.getInt("newbie"));
				player.setNoble(rset.getInt("nobless")==1);

				player.setClanJoinExpiryTime(rset.getLong("clan_join_expiry_time"));
				if (player.getClanJoinExpiryTime() < System.currentTimeMillis())
				{
					player.setClanJoinExpiryTime(0);
				}
				player.setClanCreateExpiryTime(rset.getLong("clan_create_expiry_time"));
				if (player.getClanCreateExpiryTime() < System.currentTimeMillis())
				{
					player.setClanCreateExpiryTime(0);
				}

				int clanId	= rset.getInt("clanid");
				player.setPowerGrade((int)rset.getLong("power_grade"));
				player.setPledgeType(rset.getInt("subpledge"));
				player.setLastRecomUpdate(rset.getLong("last_recom_date"));
				//player.setApprentice(rset.getInt("apprentice"));

				if (clanId > 0)
				{
					player.setClan(ClanTable.getInstance().getClan(clanId));
				}

				if (player.getClan() != null)
                {
                    if (player.getClan().getLeaderId() != player.getObjectId())
                    {
                        if (player.getPowerGrade() == 0)
                        {
                        	player.setPowerGrade(5);
                        }
                    	player.setClanPrivileges(player.getClan().getRankPrivs(player.getPowerGrade()));
                    }
                    else
                    {
                    	player.setClanPrivileges(L2Clan.CP_ALL);
                    	player.setPowerGrade(1);
                    }
                }
                else
            	{
                	player.setClanPrivileges(L2Clan.CP_NOTHING);
            	}

				player.setDeleteTimer(rset.getLong("deletetime"));

				player.setTitle(rset.getString("title"));
				player.setAccessLevel(rset.getInt("accesslevel"));
				player.setFistsWeaponItem(player.findFistsWeaponItem(activeClassId));
				player.setUptime(System.currentTimeMillis());

				currentHp = rset.getDouble("curHp");
				currentCp = rset.getDouble("curCp");
				currentMp = rset.getDouble("curMp");

				//Check recs
				player.checkRecom(rset.getInt("rec_have"),rset.getInt("rec_left"));

				player._classIndex = 0;
				try { player.setBaseClass(rset.getInt("base_class")); }
				catch (Exception e) { player.setBaseClass(activeClassId); }

				// Restore Subclass Data (cannot be done earlier in function)
                if (restoreSubClassData(player))
                {
                	if (activeClassId != player.getBaseClass())
                	{
                    	for (SubClass subClass : player.getSubClasses().values())
                    		if (subClass.getClassId() == activeClassId)
                    			player._classIndex = subClass.getClassIndex();
                	}
                }
                if (player.getClassIndex() == 0 && activeClassId != player.getBaseClass())
                {
                	// Subclass in use but doesn't exist in DB -
                	// a possible restart-while-modifysubclass cheat has been attempted.
                	// Switching to use base class
                	player.setClassId(player.getBaseClass());
                	_log.warning("Player "+player.getName()+" reverted to base class. Possibly has tried a relogin exploit while subclassing.");
                }
                else player._activeClass = activeClassId;

                player.setApprentice(rset.getInt("apprentice"));
                player.setSponsor(rset.getInt("sponsor"));
                player.setLvlJoinedAcademy(rset.getInt("lvl_joined_academy"));
                player.setIsIn7sDungeon(rset.getInt("isin7sdungeon") == 1);
                player.setInJail(rset.getInt("in_jail") == 1);
                if (player.isInJail())
                	player.setJailTimer(rset.getLong("jail_timer"));
                else
                	player.setJailTimer(0);

                CursedWeaponsManager.getInstance().checkPlayer(player);

                player.setAllianceWithVarkaKetra(rset.getInt("varka_ketra_ally"));

                player.setDeathPenaltyBuffLevel(rset.getInt("death_penalty_level"));

				// Add the L2PcInstance object in _allObjects
				//L2World.getInstance().storeObject(player);

				// Set the x,y,z position of the L2PcInstance and make it invisible
				player.setXYZInvisible(rset.getInt("x"), rset.getInt("y"), rset.getInt("z"));

				// Retrieve the name and ID of the other characters assigned to this account.
				PreparedStatement stmt = con.prepareStatement("SELECT charId, char_name FROM characters WHERE account_name=? AND charId<>?");
				stmt.setString(1, player._accountName);
				stmt.setInt(2, objectId);
				ResultSet chars = stmt.executeQuery();

				while (chars.next())
				{
					Integer charId = chars.getInt("charId");
					String charName = chars.getString("char_name");
					player._chars.put(charId, charName);
				}

				chars.close();
				stmt.close();
				break;
			}

			rset.close();
			statement.close();

			// Retrieve from the database all secondary data of this L2PcInstance
			// and reward expertise/lucky skills if necessary.
			// Note that Clan, Noblesse and Hero skills are given separately and not here.
			player.restoreCharData();
			player.rewardSkills();
			
		    // buff and status icons
	        if (Config.STORE_SKILL_COOLTIME)
	            player.restoreEffects();

			// Restore current Cp, HP and MP values
			player.setCurrentCp(currentCp);
			player.setCurrentHp(currentHp);
			player.setCurrentMp(currentMp);
			
			if (currentHp < 0.5) {
				player.setIsDead(true);
				player.stopHpMpRegeneration();
			}

			// Restore pet if exists in the world
			player.setPet(L2World.getInstance().getPet(player.getObjectId()));
			if(player.getPet() != null) player.getPet().setOwner(player);

			// Update the overloaded status of the L2PcInstance
			player.refreshOverloaded();
		}
		catch (Exception e)
		{
			_log.severe("Could not restore char data: " + e);
		}
		finally
		{
			try { con.close(); } catch (Exception e) {}
		}

		return player;
	}

	/**
	 * @return
	 */
	public Forum getMail()
	{
		if (_forumMail == null)
		{
			setMail(ForumsBBSManager.getInstance().getForumByName("MailRoot").getChildByName(getName()));

			if (_forumMail == null)
			{
				ForumsBBSManager.getInstance().createNewForum(getName(),ForumsBBSManager.getInstance().getForumByName("MailRoot"),Forum.MAIL,Forum.OWNERONLY,getObjectId());
				setMail(ForumsBBSManager.getInstance().getForumByName("MailRoot").getChildByName(getName()));
			}
		}

		return _forumMail;
	}

	/**
	 * @param forum
	 */
	public void setMail(Forum forum)
	{
		_forumMail = forum;
	}

	/**
	 * @return
	 */
	public Forum getMemo()
	{
		if (_forumMemo == null)
		{
			setMemo(ForumsBBSManager.getInstance().getForumByName("MemoRoot").getChildByName(_accountName));

        	if (_forumMemo == null)
        	{
        		ForumsBBSManager.getInstance().createNewForum(_accountName,ForumsBBSManager.getInstance().getForumByName("MemoRoot"),Forum.MEMO,Forum.OWNERONLY,getObjectId());
        		setMemo(ForumsBBSManager.getInstance().getForumByName("MemoRoot").getChildByName(_accountName));
        	}
		}

		return _forumMemo;
	}

	/**
	 * @param forum
	 */
	public void setMemo(Forum forum)
	{
		_forumMemo = forum;
	}

    /**
     * Restores sub-class data for the L2PcInstance, used to check the current
     * class index for the character.
     */
    private static boolean restoreSubClassData(L2PcInstance player)
    {
        Connection con = null;

        try
        {
            con = L2DatabaseFactory.getInstance().getConnection();
            PreparedStatement statement = con.prepareStatement(RESTORE_CHAR_SUBCLASSES);
            statement.setInt(1, player.getObjectId());

            ResultSet rset = statement.executeQuery();

            while (rset.next())
            {
                SubClass subClass = new SubClass();
                subClass.setClassId(rset.getInt("class_id"));
                subClass.setLevel(rset.getByte("level"));
                subClass.setExp(rset.getLong("exp"));
                subClass.setSp(rset.getInt("sp"));
                subClass.setClassIndex(rset.getInt("class_index"));

                // Enforce the correct indexing of _subClasses against their class indexes.
                player.getSubClasses().put(subClass.getClassIndex(), subClass);
            }

            statement.close();
        }
        catch (Exception e) {
            _log.warning("Could not restore classes for " + player.getName() + ": " + e);
            e.printStackTrace();
        }
        finally {
            try { con.close(); } catch (Exception e) {}
        }

        return true;
    }

	/**
	 * Restores secondary data for the L2PcInstance, based on the current class index.
	 */
	private void restoreCharData()
	{
		// Retrieve from the database all skills of this L2PcInstance and add them to _skills.
		restoreSkills();

		// Retrieve from the database all macroses of this L2PcInstance and add them to _macroses.
		_macroses.restore();

		// Retrieve from the database all shortCuts of this L2PcInstance and add them to _shortCuts.
		_shortCuts.restore();

		// Retrieve from the database all henna of this L2PcInstance and add them to _henna.
		restoreHenna();

		// Retrieve from the database all recom data of this L2PcInstance and add to _recomChars.
		if (Config.ALT_RECOMMEND) restoreRecom();

		// Retrieve from the database the recipe book of this L2PcInstance.
		if (!isSubClassActive())
			restoreRecipeBook();
	}

	/**
	 * Store recipe book data for this L2PcInstance, if not on an active sub-class.
	 */
	private void storeRecipeBook()
	{
		// If the player is on a sub-class don't even attempt to store a recipe book.
		if (isSubClassActive())
			return;
		if (getCommonRecipeBook().length == 0 && getDwarvenRecipeBook().length == 0)
			return;

		Connection con = null;

		try {
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("DELETE FROM character_recipebook WHERE charId=?");
			statement.setInt(1, getObjectId());
			statement.execute();
			statement.close();

			L2RecipeList[] recipes = getCommonRecipeBook();

			for (int count = 0; count < recipes.length; count++)
			{
				statement = con.prepareStatement("INSERT INTO character_recipebook (charId, id, type) values(?,?,0)");
				statement.setInt(1, getObjectId());
				statement.setInt(2, recipes[count].getId());
				statement.execute();
				statement.close();
			}

			recipes = getDwarvenRecipeBook();
			for (int count = 0; count < recipes.length; count++)
			{
				statement = con.prepareStatement("INSERT INTO character_recipebook (charId, id, type) values(?,?,1)");
				statement.setInt(1, getObjectId());
				statement.setInt(2, recipes[count].getId());
				statement.execute();
				statement.close();
			}
		}
		catch (Exception e) {
			_log.warning("Could not store recipe book data: " + e);
		}
		finally {
			try { con.close(); } catch (Exception e) {}
		}
	}

	/**
	 * Restore recipe book data for this L2PcInstance.
	 */
	private void restoreRecipeBook()
	{
		Connection con = null;

		try {
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT id, type FROM character_recipebook WHERE charId=?");
			statement.setInt(1, getObjectId());
			ResultSet rset = statement.executeQuery();

			L2RecipeList recipe;
			while (rset.next()) {
				recipe = RecipeController.getInstance().getRecipeList(rset.getInt("id") - 1);

				if (rset.getInt("type") == 1)
					registerDwarvenRecipeList(recipe);
				else
					registerCommonRecipeList(recipe);
			}

			rset.close();
			statement.close();
		}
		catch (Exception e) {
			_log.warning("Could not restore recipe book data:" + e);
		}
		finally {
			try { con.close(); } catch (Exception e) {}
		}
	}

	/**
	 * Update L2PcInstance stats in the characters table of the database.<BR><BR>
	 */
	public synchronized void store()
	{
		//update client coords, if these look like true
        if (isInsideRadius(getClientX(), getClientY(), 1000, true))
            setXYZ(getClientX(), getClientY(), getClientZ());

		storeCharBase();
		storeCharSub();
		storeEffect();
		storeRecipeBook();
		transformInsertInfo();
	}

	private void storeCharBase()
	{
		Connection con = null;

		try
		{
			// Get the exp, level, and sp of base class to store in base table
			int currentClassIndex = getClassIndex();
			_classIndex = 0;
			long exp     = getStat().getExp();
			int level   = getStat().getLevel();
			int sp      = getStat().getSp();
			_classIndex = currentClassIndex;

			con = L2DatabaseFactory.getInstance().getConnection();
			// Update base class
			PreparedStatement statement = con.prepareStatement(UPDATE_CHARACTER);
			
			statement.setInt(1, level);
			statement.setInt(2, getMaxHp());
			statement.setDouble(3, getCurrentHp());
			statement.setInt(4, getMaxCp());
			statement.setDouble(5, getCurrentCp());
			statement.setInt(6, getMaxMp());
			statement.setDouble(7, getCurrentMp());
			statement.setInt(8, getAppearance().getFace());
			statement.setInt(9, getAppearance().getHairStyle());
			statement.setInt(10, getAppearance().getHairColor());
			statement.setInt(11, getHeading());
			statement.setInt(12, _observerMode ? _obsX : getX());
			statement.setInt(13, _observerMode ? _obsY : getY());
			statement.setInt(14, _observerMode ? _obsZ : getZ());
			statement.setLong(15, exp);
			statement.setLong(16, getExpBeforeDeath());
			statement.setInt(17, sp);
			statement.setInt(18, getKarma());
			statement.setInt(19, getPvpKills());
			statement.setInt(20, getPkKills());
			statement.setInt(21, getRecomHave());
			statement.setInt(22, getRecomLeft());
			statement.setInt(23, getClanId());
			statement.setInt(24, getRace().ordinal());
			statement.setInt(25, getClassId().getId());
			statement.setLong(26, getDeleteTimer());
			statement.setString(27, getTitle());
			statement.setInt(28, getAccessLevel().getLevel());
			statement.setInt(29, isOnline());
            statement.setInt(30, isIn7sDungeon() ? 1 : 0);
			statement.setInt(31, getClanPrivileges());
			statement.setInt(32, getWantsPeace());
			statement.setInt(33, getBaseClass());

			long totalOnlineTime = _onlineTime;

			if (_onlineBeginTime > 0)
				totalOnlineTime += (System.currentTimeMillis()-_onlineBeginTime)/1000;

            statement.setLong(34, totalOnlineTime);
            statement.setInt(35, isInJail() ? 1 : 0);
            statement.setLong(36, getJailTimer());
            statement.setInt(37, getNewbie());
            statement.setInt(38, isNoble() ? 1 : 0);
            statement.setLong(39, getPowerGrade());
            statement.setInt(40, getPledgeType());
            statement.setLong(41,getLastRecomUpdate());
            statement.setInt(42,getLvlJoinedAcademy());
            statement.setLong(43,getApprentice());
            statement.setLong(44,getSponsor());
            statement.setInt(45, getAllianceWithVarkaKetra());
			statement.setLong(46, getClanJoinExpiryTime());
			statement.setLong(47, getClanCreateExpiryTime());
			statement.setString(48, getName());
			statement.setLong(49, getDeathPenaltyBuffLevel());
            statement.setInt(50, getObjectId());

			statement.execute();
			statement.close();
		}
		catch (Exception e) { _log.warning("Could not store char base data: "+ e); }
		finally { try { con.close(); } catch (Exception e) {} }
	}

    private void storeCharSub()
    {
        Connection con = null;

        try
        {
            con = L2DatabaseFactory.getInstance().getConnection();
            PreparedStatement statement = con.prepareStatement(UPDATE_CHAR_SUBCLASS);
            
            if (getTotalSubClasses() > 0)
            {
            	for (SubClass subClass : getSubClasses().values())
            	{
                    statement.setLong(1, subClass.getExp());
                    statement.setInt(2, subClass.getSp());
                    statement.setInt(3, subClass.getLevel());
                    statement.setInt(4, subClass.getClassId());
                    statement.setInt(5, getObjectId());
                    statement.setInt(6, subClass.getClassIndex());

                    statement.execute();
                }
            	statement.close();
            }
        }
        catch (Exception e) {
        	_log.warning("Could not store sub class data for " + getName() + ": "+ e);
		}
        finally { try { con.close(); } catch (Exception e) {} }
    }
    
    private void storeEffect()
	{
		if (!Config.STORE_SKILL_COOLTIME)
			return;
		
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			
			// Delete all current stored effects for char to avoid dupe
			PreparedStatement statement = con.prepareStatement(DELETE_SKILL_SAVE);
			
			statement.setInt(1, getObjectId());
			statement.setInt(2, getClassIndex());
			statement.execute();
			statement.close();
			
			int buff_index = 0;
			
			// Store all effect data along with calulated remaining
			// reuse delays for matching skills. 'restore_type'= 0.
			statement = con.prepareStatement(ADD_SKILL_SAVE);
			
			List<L2Skill> storedSkills = new FastList<L2Skill>();
			
			for (L2Effect effect : getAllEffects())
			{
				L2Skill skill = effect.getSkill();
				
				if (storedSkills.contains(skill))
					continue;
				
				storedSkills.add(skill);
				
				if (effect != null && !effect.isHerbEffect() && effect.getInUse() && !skill.isToggle())
				{
					int skillId = skill.getId();
					buff_index++;
					
					statement.setInt(1, getObjectId());
					statement.setInt(2, skillId);
					statement.setInt(3, skill.getLevel());
					statement.setInt(4, effect.getCount());
					statement.setInt(5, effect.getTime());
					
					if (_reuseTimeStamps.containsKey(skillId))
					{
						TimeStamp t = _reuseTimeStamps.remove(skillId);
						statement.setLong(6, t.hasNotPassed() ? t.getReuse() : 0);
					}
					else
					{
						statement.setLong(6, 0);
					}
					
					statement.setInt(7, 0);
					statement.setInt(8, getClassIndex());
					statement.setInt(9, buff_index);
					statement.execute();
				}
			}
			
			// Store the reuse delays of remaining skills which
			// lost effect but still under reuse delay. 'restore_type' 1.
			for (TimeStamp t : _reuseTimeStamps.values())
			{
				if (t.hasNotPassed())
				{
					buff_index++;
					statement.setInt(1, getObjectId());
					statement.setInt(2, t.getSkill());
					statement.setInt(3, -1);
					statement.setInt(4, -1);
					statement.setInt(5, -1);
					statement.setLong(6, t.getReuse());
					statement.setInt(7, 1);
					statement.setInt(8, getClassIndex());
					statement.setInt(9, buff_index);
					statement.execute();
					
				}
			}
			_reuseTimeStamps.clear();
			
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "Could not store char effect data: ", e);
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{
			}
		}
	}

	/**
	 * Return True if the L2PcInstance is on line.<BR>
	 * <BR>
	 */
	public int isOnline()
	{
		return (_isOnline ? 1 : 0);
	}

    public boolean isIn7sDungeon()
    {
        return _isIn7sDungeon;
    }

	/**
	 * Add a skill to the L2PcInstance _skills and its Func objects to the calculator set of the L2PcInstance and save update in the character_skills table of the database.<BR><BR>
	 *
	 * <B><U> Concept</U> :</B><BR><BR>
	 * All skills own by a L2PcInstance are identified in <B>_skills</B><BR><BR>
	 *
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Replace oldSkill by newSkill or Add the newSkill </li>
	 * <li>If an old skill has been replaced, remove all its Func objects of L2Character calculator set</li>
	 * <li>Add Func objects of newSkill to the calculator set of the L2Character </li><BR><BR>
	 *
	 * @param newSkill The L2Skill to add to the L2Character
	 *
	 * @return The L2Skill replaced or null if just added a new L2Skill
	 *
	 */
	public L2Skill addSkill(L2Skill newSkill, boolean store)
	{
		// Add a skill to the L2PcInstance _skills and its Func objects to the calculator set of the L2PcInstance
		L2Skill oldSkill = super.addSkill(newSkill);

		// Add or update a L2PcInstance skill in the character_skills table of the database
		if (store) storeSkill(newSkill, oldSkill, -1);

		return oldSkill;
	}
	public L2Skill removeSkill(L2Skill skill, boolean store)
	{
		if (store)
			return removeSkill(skill);
		else
			return super.removeSkill(skill);
	}

	/**
	 * Remove a skill from the L2Character and its Func objects from calculator set of the L2Character and save update in the character_skills table of the database.<BR><BR>
	 *
	 * <B><U> Concept</U> :</B><BR><BR>
	 * All skills own by a L2Character are identified in <B>_skills</B><BR><BR>
	 *
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Remove the skill from the L2Character _skills </li>
	 * <li>Remove all its Func objects from the L2Character calculator set</li><BR><BR>
	 *
	 * <B><U> Overridden in </U> :</B><BR><BR>
	 * <li> L2PcInstance : Save update in the character_skills table of the database</li><BR><BR>
	 *
	 * @param skill The L2Skill to remove from the L2Character
	 *
	 * @return The L2Skill removed
	 *
	 */
	@Override
	public L2Skill removeSkill(L2Skill skill)
	{
		// Remove a skill from the L2Character and its Func objects from calculator set of the L2Character
		L2Skill oldSkill = super.removeSkill(skill);

		Connection con = null;

		try
		{
			// Remove or update a L2PcInstance skill from the character_skills table of the database
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(DELETE_SKILL_FROM_CHAR);

			if (oldSkill != null)
			{
				statement.setInt(1, oldSkill.getId());
				statement.setInt(2, getObjectId());
				statement.setInt(3, getClassIndex());
				statement.execute();
			}
			statement.close();
		}
		catch (Exception e)
		{
			_log.warning("Error could not delete skill: " + e);
		}
		finally
		{
			try { con.close(); } catch (Exception e) {}
		}

		if (this.transformId() > 0 || this.isCursedWeaponEquipped())
			return oldSkill;

		    L2ShortCut[] allShortCuts = getAllShortCuts();

		    for (L2ShortCut sc : allShortCuts)
		    {
			    if (sc != null && skill != null && sc.getId() == skill.getId() && sc.getType() == L2ShortCut.TYPE_SKILL)
			        deleteShortCut(sc.getSlot(), sc.getPage());
		    }

		return oldSkill;
	}

	/**
	 * Add or update a L2PcInstance skill in the character_skills table of the database.
	 * <BR><BR>
	 * If newClassIndex > -1, the skill will be stored with that class index, not the current one.
	 */
	private void storeSkill(L2Skill newSkill, L2Skill oldSkill, int newClassIndex)
	{
		int classIndex = _classIndex;

		if (newClassIndex > -1)
			classIndex = newClassIndex;

		Connection con = null;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;

			if (oldSkill != null && newSkill != null)
			{
				statement = con.prepareStatement(UPDATE_CHARACTER_SKILL_LEVEL);
				statement.setInt(1, newSkill.getLevel());
				statement.setInt(2, oldSkill.getId());
				statement.setInt(3, getObjectId());
				statement.setInt(4, classIndex);
				statement.execute();
				statement.close();
			}
			else if (newSkill != null)
			{
				statement = con.prepareStatement(ADD_NEW_SKILL);
				statement.setInt(1, getObjectId());
				statement.setInt(2, newSkill.getId());
				statement.setInt(3, newSkill.getLevel());
				statement.setString(4, newSkill.getName());
				statement.setInt(5, classIndex);
				statement.execute();
				statement.close();
			}
			else
			{
				_log.warning("could not store new skill. its NULL");
			}
		}
		catch (Exception e)
		{
			_log.warning("Error could not store char skills: " + e);
		}
		finally
		{
			try { con.close(); } catch (Exception e) {}
		}
	}

	/**
	 * Retrieve from the database all skills of this L2PcInstance and add them to _skills.<BR><BR>
	 */
	private void restoreSkills()
	{
		Connection con = null;

		try
		{
			// Retrieve all skills of this L2PcInstance from the database
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(RESTORE_SKILLS_FOR_CHAR);
			
			statement.setInt(1, getObjectId());
			statement.setInt(2, getClassIndex());
			ResultSet rset = statement.executeQuery();

			// Go though the recordset of this SQL query
			while (rset.next())
			{
				int id = rset.getInt("skill_id");
				int level = rset.getInt("skill_level");

				if (id > 9000)
					continue; // fake skills for base stats

				// Create a L2Skill object for each record
				L2Skill skill = SkillTable.getInstance().getInfo(id, level);

				// Add the L2Skill object to the L2Character _skills and its Func objects to the calculator set of the L2Character
				super.addSkill(skill);
			}

			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.warning("Could not restore character skills: " + e);
		}
		finally
		{
			try { con.close(); } catch (Exception e) {}
		}
	}

    /**
     * Retrieve from the database all skill effects of this L2PcInstance and add them to the player.<BR><BR>
     */
	public void restoreEffects()
	{
		L2Object[] targets = new L2Character[]{this};
		Connection con = null;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;
			ResultSet rset;

			/**
			*  Restore Type 0
			*  These skill were still in effect on the character
			*  upon logout. Some of which were self casted and
			*  might still have had a long reuse delay which also
			*  is restored.
			*/
			statement = con.prepareStatement(RESTORE_SKILL_SAVE);
			statement.setInt(1, getObjectId());
			statement.setInt(2, getClassIndex());
			statement.setInt(3, 0);
			rset = statement.executeQuery();

			while (rset.next())
			{
				int skillId = rset.getInt("skill_id");
				int skillLvl = rset.getInt("skill_level");
				int effectCount = rset.getInt("effect_count");
				int effectCurTime = rset.getInt("effect_cur_time");
				long reuseDelay = rset.getLong("reuse_delay");

				// Just incase the admin minipulated this table incorrectly :x
				if(skillId == -1 || effectCount == -1 || effectCurTime == -1 || reuseDelay < 0) continue;

				L2Skill skill = SkillTable.getInstance().getInfo(skillId, skillLvl);
				ISkillHandler IHand = SkillHandler.getInstance().getSkillHandler(skill.getSkillType());
				if (IHand != null)
					IHand.useSkill(this, skill, targets);
				else
					skill.useSkill(this, targets);

				if (reuseDelay > 10)
				{
					disableSkill(skillId, reuseDelay);
					addTimeStamp(new TimeStamp(skillId, reuseDelay));
				}

				for (L2Effect effect : getAllEffects())
                {
					if (effect.getSkill().getId() == skillId)
					{
						effect.setCount(effectCount);
						effect.setFirstTime(effectCurTime);
					}
                }
			}
			rset.close();
			statement.close();

			/**
			 * Restore Type 1
			 * The remaning skills lost effect upon logout but
			 * were still under a high reuse delay.
			 */
			statement = con.prepareStatement(RESTORE_SKILL_SAVE);
			statement.setInt(1, getObjectId());
			statement.setInt(2, getClassIndex());
			statement.setInt(3, 1);
			rset = statement.executeQuery();

			while (rset.next())
			{
				int skillId = rset.getInt("skill_id");
				long reuseDelay = rset.getLong("reuse_delay");

				if (reuseDelay <= 0) continue;

				disableSkill(skillId, reuseDelay);
				addTimeStamp(new TimeStamp(skillId, reuseDelay));
			}
			rset.close();
			statement.close();

			statement = con.prepareStatement(DELETE_SKILL_SAVE);
			statement.setInt(1, getObjectId());
			statement.setInt(2, getClassIndex());
			statement.executeUpdate();
			statement.close();
		}
		catch (Exception e) {
			_log.warning("Could not restore active effect data: " + e);
		}
		finally {
			try {con.close();} catch (Exception e) {}
		}
	}

	/**
	 * Retrieve from the database all Henna of this L2PcInstance, add them to _henna and calculate stats of the L2PcInstance.<BR><BR>
	 */
	private void restoreHenna()
	{
		Connection con = null;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(RESTORE_CHAR_HENNAS);
			statement.setInt(1, getObjectId());
			statement.setInt(2, getClassIndex());
			ResultSet rset = statement.executeQuery();

			for (int i=0;i<3;i++)
				_henna[i]=null;

			while (rset.next())
			{
				int slot = rset.getInt("slot");

				if (slot<1 || slot>3)
                    continue;

				int symbol_id = rset.getInt("symbol_id");

				L2HennaInstance sym = null;

				if (symbol_id != 0)
				{
					L2Henna tpl = HennaTable.getInstance().getTemplate(symbol_id);

					if (tpl != null)
					{
						sym = new L2HennaInstance(tpl);
						_henna[slot-1] = sym;
					}
				}
			}

			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.warning("could not restore henna: "+e);
		}
		finally
		{
			try { con.close(); } catch (Exception e) {}
		}

		// Calculate Henna modifiers of this L2PcInstance
		recalcHennaStats();
	}

	/**
	 * Retrieve from the database all Recommendation data of this L2PcInstance, add to _recomChars and calculate stats of the L2PcInstance.<BR><BR>
	 */
	private void restoreRecom()
	{
		Connection con = null;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(RESTORE_CHAR_RECOMS);
			statement.setInt(1, getObjectId());
			ResultSet rset = statement.executeQuery();
			while (rset.next())
			{
				_recomChars.add(rset.getInt("target_id"));
			}

			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.warning("could not restore recommendations: "+e);
		}
		finally
		{
			try { con.close(); } catch (Exception e) {}
		}
	}

	/**
	 * Return the number of Henna empty slot of the L2PcInstance.<BR><BR>
	 */
	public int getHennaEmptySlots()
	{
		int totalSlots = 1 + getClassId().level();

		for (int i = 0; i < 3; i++)
			if (_henna[i] != null)
                totalSlots--;

		if (totalSlots <= 0)
            return 0;

		return totalSlots;
	}

	/**
	 * Remove a Henna of the L2PcInstance, save update in the character_hennas table of the database and send Server->Client HennaInfo/UserInfo packet to this L2PcInstance.<BR><BR>
	 */
	public boolean removeHenna(int slot)
	{
		if (slot<1 || slot>3)
			return false;

		slot--;

		if (_henna[slot]==null)
			return false;

		L2HennaInstance henna = _henna[slot];
		_henna[slot] = null;

		Connection con = null;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(DELETE_CHAR_HENNA);
			
			statement.setInt(1, getObjectId());
			statement.setInt(2, slot+1);
			statement.setInt(3, getClassIndex());
			
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			_log.warning("could not remove char henna: "+e);
		}
		finally
		{
			try { con.close(); } catch (Exception e) {}
		}

		// Calculate Henna modifiers of this L2PcInstance
		recalcHennaStats();

		// Send Server->Client HennaInfo packet to this L2PcInstance
		sendPacket(new HennaInfo(this));

		// Send Server->Client UserInfo packet to this L2PcInstance
		sendPacket(new UserInfo(this));

		// Add the recovered dyes to the player's inventory and notify them.
		getInventory().addItem("Henna", henna.getItemIdDye(), henna.getAmountDyeRequire() / 2, this, null);

		SystemMessage sm = new SystemMessage(SystemMessageId.EARNED_S2_S1_S);
        sm.addItemName(henna.getItemIdDye());
        sm.addNumber(henna.getAmountDyeRequire() / 2);
		sendPacket(sm);

		return true;
	}

	/**
	 * Add a Henna to the L2PcInstance, save update in the character_hennas table of the database and send Server->Client HennaInfo/UserInfo packet to this L2PcInstance.<BR><BR>
	 */
	public boolean addHenna(L2HennaInstance henna)
	{
		if (getHennaEmptySlots()==0)
		{
			sendMessage("You may not have more than three equipped symbols at a time.");
			return false;
		}

		// int slot = 0;
		for (int i=0;i<3;i++)
		{
			if (_henna[i]==null)
			{
				_henna[i]=henna;

				// Calculate Henna modifiers of this L2PcInstance
				recalcHennaStats();

				Connection con = null;

				try
				{
					con = L2DatabaseFactory.getInstance().getConnection();
					PreparedStatement statement = con.prepareStatement(ADD_CHAR_HENNA);
					
					statement.setInt(1, getObjectId());
					statement.setInt(2, henna.getSymbolId());
					statement.setInt(3, i+1);
					statement.setInt(4, getClassIndex());
					
					statement.execute();
					statement.close();
				}
				catch (Exception e)
				{
					_log.warning("could not save char henna: "+e);
				}
				finally
				{
					try { con.close(); } catch (Exception e) {}
				}

				// Send Server->Client HennaInfo packet to this L2PcInstance
				HennaInfo hi = new HennaInfo(this);
				sendPacket(hi);

				// Send Server->Client UserInfo packet to this L2PcInstance
				UserInfo ui = new UserInfo(this);
				sendPacket(ui);

				return true;
			}
		}

		return false;
	}

	/**
	 * Calculate Henna modifiers of this L2PcInstance.<BR><BR>
	 */
	private void recalcHennaStats()
	{
		_hennaINT = 0;
		_hennaSTR = 0;
		_hennaCON = 0;
		_hennaMEN = 0;
		_hennaWIT = 0;
		_hennaDEX = 0;

		for (int i=0;i<3;i++)
		{
			if (_henna[i]==null)continue;
			_hennaINT += _henna[i].getStatINT();
			_hennaSTR += _henna[i].getStatSTR();
			_hennaMEN += _henna[i].getStatMEM();
			_hennaCON += _henna[i].getStatCON();
			_hennaWIT += _henna[i].getStatWIT();
			_hennaDEX += _henna[i].getStatDEX();
		}

		if (_hennaINT>5)_hennaINT=5;
		if (_hennaSTR>5)_hennaSTR=5;
		if (_hennaMEN>5)_hennaMEN=5;
		if (_hennaCON>5)_hennaCON=5;
		if (_hennaWIT>5)_hennaWIT=5;
		if (_hennaDEX>5)_hennaDEX=5;
	}

	/**
	 * Return the Henna of this L2PcInstance corresponding to the selected slot.<BR><BR>
	 */
	public L2HennaInstance getHenna(int slot)
	{
		if (slot < 1 || slot > 3)
            return null;

		return _henna[slot - 1];
	}

	/**
	 * Return the INT Henna modifier of this L2PcInstance.<BR><BR>
	 */
	public int getHennaStatINT()
	{
		return _hennaINT;
	}

	/**
	 * Return the STR Henna modifier of this L2PcInstance.<BR><BR>
	 */
	public int getHennaStatSTR()
	{
		return _hennaSTR;
	}

	/**
	 * Return the CON Henna modifier of this L2PcInstance.<BR><BR>
	 */
	public int getHennaStatCON()
	{
		return _hennaCON;
	}

	/**
	 * Return the MEN Henna modifier of this L2PcInstance.<BR><BR>
	 */
	public int getHennaStatMEN()
	{
		return _hennaMEN;
	}

	/**
	 * Return the WIT Henna modifier of this L2PcInstance.<BR><BR>
	 */
	public int getHennaStatWIT()
	{
		return _hennaWIT;
	}

	/**
	 * Return the DEX Henna modifier of this L2PcInstance.<BR><BR>
	 */
	public int getHennaStatDEX()
	{
		return _hennaDEX;
	}

	/**
	 * Return True if the L2PcInstance is autoAttackable.<BR><BR>
	 *
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Check if the attacker isn't the L2PcInstance Pet </li>
	 * <li>Check if the attacker is L2MonsterInstance</li>
	 * <li>If the attacker is a L2PcInstance, check if it is not in the same party </li>
	 * <li>Check if the L2PcInstance has Karma </li>
	 * <li>If the attacker is a L2PcInstance, check if it is not in the same siege clan (Attacker, Defender) </li><BR><BR>
	 *
	 */
	@Override
	public boolean isAutoAttackable(L2Character attacker)
	{
		// Check if the attacker isn't the L2PcInstance Pet
		if (attacker == this || attacker == getPet())
			return false;

		// TODO: check for friendly mobs
		// Check if the attacker is a L2MonsterInstance
		if (attacker instanceof L2MonsterInstance)
			return true;

		// Check if the attacker is not in the same party
		if (getParty() != null && getParty().getPartyMembers().contains(attacker))
			return false;

		// Check if the attacker is in olympia and olympia start
		if (attacker instanceof L2PcInstance && ((L2PcInstance)attacker).isInOlympiadMode() ){
			if (isInOlympiadMode() && isOlympiadStart() && ((L2PcInstance)attacker).getOlympiadGameId()==getOlympiadGameId())
				return true;
			else
				return false;
		}

		// Check if the attacker is not in the same clan
		if (getClan() != null && attacker != null && getClan().isMember(attacker.getObjectId()))
			return false;

        if(attacker instanceof L2PlayableInstance && isInsideZone(ZONE_PEACE))
            return false;

		// Check if the L2PcInstance has Karma
		if (getKarma() > 0 || getPvpFlag() > 0)
			return true;

		// Check if the attacker is a L2PcInstance
		if (attacker instanceof L2PcInstance)
		{
			// is AutoAttackable if both players are in the same duel and the duel is still going on
			if ( getDuelState() == Duel.DUELSTATE_DUELLING
					&& getDuelId() == ((L2PcInstance)attacker).getDuelId() )
				return true;
			// Check if the L2PcInstance is in an arena or a siege area
			if (isInsideZone(ZONE_PVP) && ((L2PcInstance)attacker).isInsideZone(ZONE_PVP))
				return true;

			if (getClan() != null)
			{
				Siege siege = SiegeManager.getInstance().getSiege(getX(), getY(), getZ());
				if (siege != null)
				{
					// Check if a siege is in progress and if attacker and the L2PcInstance aren't in the Defender clan
					if (siege.checkIsDefender(((L2PcInstance)attacker).getClan()) &&
							siege.checkIsDefender(getClan()))
						return false;

					// Check if a siege is in progress and if attacker and the L2PcInstance aren't in the Attacker clan
					if (siege.checkIsAttacker(((L2PcInstance)attacker).getClan()) &&
							siege.checkIsAttacker(getClan()))
						return false;
				}

				// Check if clan is at war
				if (getClan() != null && ((L2PcInstance)attacker).getClan() != null
				                           && (getClan().isAtWarWith(((L2PcInstance)attacker).getClanId())
				                           && getWantsPeace() == 0
				                           && ((L2PcInstance)attacker).getWantsPeace() == 0
				                           && !isAcademyMember()))
				return true;
			}
		}
		else if (attacker instanceof L2SiegeGuardInstance)
		{
			if (getClan() != null)
			{
				Siege siege = SiegeManager.getInstance().getSiege(this);
				return (siege != null && siege.checkIsAttacker(getClan()));
			}
		}

		return false;
	}


	/**
	 * Check if the active L2Skill can be casted.<BR><BR>
	 *
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Check if the skill isn't toggle and is offensive </li>
	 * <li>Check if the target is in the skill cast range </li>
	 * <li>Check if the skill is Spoil type and if the target isn't already spoiled </li>
	 * <li>Check if the caster owns enought consummed Item, enough HP and MP to cast the skill </li>
	 * <li>Check if the caster isn't sitting </li>
	 * <li>Check if all skills are enabled and this skill is enabled </li><BR><BR>
	 * <li>Check if the caster own the weapon needed </li><BR><BR>
	 * <li>Check if the skill is active </li><BR><BR>
	 * <li>Check if all casting conditions are completed</li><BR><BR>
	 * <li>Notify the AI with AI_INTENTION_CAST and target</li><BR><BR>
	 *
	 * @param skill The L2Skill to use
	 * @param forceUse used to force ATTACK on players
	 * @param dontMove used to prevent movement, if not in range
	 *
	 */
	public void useMagic(L2Skill skill, boolean forceUse, boolean dontMove)
	{
        // Check if the skill is active
        if (skill.isPassive())
        {
            // just ignore the passive skill request. why does the client send it anyway ??
            // Send a Server->Client packet ActionFailed to the L2PcInstance
            sendPacket(ActionFailed.STATIC_PACKET);
            return;
        }
        
		if (isDead())
        {
            abortCast();
            sendPacket(ActionFailed.STATIC_PACKET);
            return;
        }

        if (inObserverMode() && !skill.isPotion())
        {
        	sendPacket(new SystemMessage(SystemMessageId.OBSERVERS_CANNOT_PARTICIPATE));
            abortCast();
            sendPacket(ActionFailed.STATIC_PACKET);
            return;
        }
        
        // Check if the caster is sitting
        if (isSitting() && !skill.isPotion())
        {
            // Send a System Message to the caster
            sendPacket(new SystemMessage(SystemMessageId.CANT_MOVE_SITTING));

            // Send a Server->Client packet ActionFailed to the L2PcInstance
            sendPacket(ActionFailed.STATIC_PACKET);
            return;
        }

		// Check if the skill type is TOGGLE
		if (skill.isToggle())
		{
			// Get effects of the skill
            L2Effect effect = getFirstEffect(skill);

			if (effect != null)
			{
				effect.exit();

				// Send a Server->Client packet ActionFailed to the L2PcInstance
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
		}

		if(_disabledSkills != null && _disabledSkills.contains(skill.getId()))
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.S1_PREPARED_FOR_REUSE);
			sm.addSkillName(skill);
			sendPacket(sm);
			return;
		}

		// Check if it's ok to summon
        // siege golem (13), Wild Hog Cannon (299), Swoop Cannon (448)
        if ((skill.getId() == 13 || skill.getId() == 299 || skill.getId() == 448)
        		&& !SiegeManager.getInstance().checkIfOkToSummon(this, false) 
                && !FortSiegeManager.getInstance().checkIfOkToSummon(this, false))
			return;


        //************************************* Check Casting in Progress *******************************************

        // If a skill is currently being used, queue this one if this is not the same
		// Note that this check is currently imperfect: getCurrentSkill() isn't always null when a skill has
		// failed to cast, or the casting is not yet in progress when this is rechecked
        if (getCurrentSkill() != null && isCastingNow())
        {
            // Check if new skill different from current skill in progress
            if (skill.getId() == getCurrentSkill().getSkillId())
            {
            	sendPacket(ActionFailed.STATIC_PACKET);
            	return;
            }

            if (Config.DEBUG && getQueuedSkill() != null)
                _log.info(getQueuedSkill().getSkill().getName() + " is already queued for " + getName() + ".");

            // Create a new SkillDat object and queue it in the player _queuedSkill
            setQueuedSkill(skill, forceUse, dontMove);
            sendPacket(ActionFailed.STATIC_PACKET);
            return;
        }
        if (getQueuedSkill() != null) // wiping out previous values, after casting has been aborted
        	setQueuedSkill(null, false, false);

        //************************************* Check Target *******************************************
        // Create and set a L2Object containing the target of the skill
        L2Object target = null;
        SkillTargetType sklTargetType = skill.getTargetType();
        SkillType sklType = skill.getSkillType();

        Point3D worldPosition = getCurrentSkillWorldPosition();
        
        if (sklTargetType == SkillTargetType.TARGET_GROUND && worldPosition == null)
        {
        	_log.info("WorldPosition is null for skill: "+skill.getName() + ", player: " + getName() + ".");
        	sendPacket(ActionFailed.STATIC_PACKET);
        	return;
        }
        
        switch (sklTargetType)
        {
        	// Target the player if skill type is AURA, PARTY, CLAN or SELF
        	case TARGET_AURA:
        	case TARGET_FRONT_AURA:
        	case TARGET_BEHIND_AURA:
        	case TARGET_PARTY:
        	case TARGET_ALLY:
        	case TARGET_CLAN:
        	case TARGET_GROUND:
        	case TARGET_SELF:
        		target = this;
        		break;
        	case TARGET_PET:
        		target = getPet();
        		break;
        	default:
        		target = getTarget();
        	break;
        }

        // Check the validity of the target
        if (target == null)
        {
            sendPacket(new SystemMessage(SystemMessageId.TARGET_CANT_FOUND));
            sendPacket(ActionFailed.STATIC_PACKET);
            return;
        }

        // skills can be used on Walls and Doors only durring siege
        if(target instanceof L2DoorInstance )
        {
        	boolean isCastle = (((L2DoorInstance) target).getCastle() != null
                    && ((L2DoorInstance) target).getCastle().getCastleId() > 0
                    && ((L2DoorInstance) target).getCastle().getSiege().getIsInProgress());
        	boolean isFort = (((L2DoorInstance) target).getFort() != null
                    && ((L2DoorInstance) target).getFort().getFortId() > 0
                    && ((L2DoorInstance) target).getFort().getSiege().getIsInProgress());
        	if (!isCastle && !isFort)
        		return;
        }

        // Are the target and the player in the same duel?
        if (isInDuel())
        {
        	if (!(target instanceof L2PcInstance && ((L2PcInstance)target).getDuelId() == getDuelId()))
        	{
        		sendMessage("You cannot do this while duelling.");
        		sendPacket(ActionFailed.STATIC_PACKET);
        		return;
        	}
        }

        //************************************* Check skill availability *******************************************

        // Check if this skill is enabled (ex : reuse time)
        if (isSkillDisabled(skill.getId()) && !getAccessLevel().allowPeaceAttack())
        {
            SystemMessage sm = new SystemMessage(SystemMessageId.S1_PREPARED_FOR_REUSE);
            sm.addSkillName(skill);
            sendPacket(sm);

            // Send a Server->Client packet ActionFailed to the L2PcInstance
            sendPacket(ActionFailed.STATIC_PACKET);
            return;
        }

        // Check if all skills are disabled
        if (isAllSkillsDisabled() && !getAccessLevel().allowPeaceAttack() && !skill.isPotion())
        {
            // Send a Server->Client packet ActionFailed to the L2PcInstance
            sendPacket(ActionFailed.STATIC_PACKET);
            return;
        }

        //************************************* Check Consumables *******************************************

        // Check if spell consumes a Soul
        if (skill.getSoulConsumeCount() > 0)
        {
            if (getSouls() < skill.getSoulConsumeCount())
            {
                sendPacket(new SystemMessage(SystemMessageId.THERE_IS_NOT_ENOUGH_SOUL));
                return;
            }
        }
        //************************************* Check Casting Conditions *******************************************

        // Check if the caster own the weapon needed
        if (!skill.getWeaponDependancy(this))
        {
            // Send a Server->Client packet ActionFailed to the L2PcInstance
            sendPacket(ActionFailed.STATIC_PACKET);
            return;
        }

        // Check if all casting conditions are completed
        if (!skill.checkCondition(this, target, false))
        {
            // Send a Server->Client packet ActionFailed to the L2PcInstance
            sendPacket(ActionFailed.STATIC_PACKET);
            return;
        }

        //************************************* Check Player State *******************************************

        // Abnormal effects(ex : Stun, Sleep...) are checked in L2Character useMagic()

        // Check if the player use "Fake Death" skill
        if (isAlikeDead())
        {
            // Send a Server->Client packet ActionFailed to the L2PcInstance
            sendPacket(ActionFailed.STATIC_PACKET);
            return;
        }

        if (isFishing() && (sklType != SkillType.PUMPING &&
        		sklType != SkillType.REELING && sklType != SkillType.FISHING))
        {
            //Only fishing skills are available
            sendPacket(new SystemMessage(SystemMessageId.ONLY_FISHING_SKILLS_NOW));
            return;
        }

        //************************************* Check Skill Type *******************************************

        // Check if this is offensive magic skill
        if (skill.isOffensive())
		{
			if ((isInsidePeaceZone(this, target)) && !getAccessLevel().allowPeaceAttack())
			{
				// If L2Character or target is in a peace zone, send a system message TARGET_IN_PEACEZONE a Server->Client packet ActionFailed
				sendPacket(new SystemMessage(SystemMessageId.TARGET_IN_PEACEZONE));
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}

			if (isInOlympiadMode() && !isOlympiadStart()){
				// if L2PcInstance is in Olympia and the match isn't already start, send a Server->Client packet ActionFailed
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}

            // Check if the target is attackable
            if (!target.isAttackable() && !getAccessLevel().allowPeaceAttack())
			{
				// If target is not attackable, send a Server->Client packet ActionFailed
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}

			// Check if a Forced ATTACK is in progress on non-attackable target
			if (!target.isAutoAttackable(this) && !forceUse)
			{
				switch (sklTargetType)
				{
					case TARGET_AURA:
					case TARGET_FRONT_AURA:
					case TARGET_BEHIND_AURA:
					case TARGET_CLAN:
					case TARGET_ALLY:
					case TARGET_PARTY:
					case TARGET_SELF:
					case TARGET_GROUND:
						break;
					default: // Send a Server->Client packet ActionFailed to the L2PcInstance
						sendPacket(ActionFailed.STATIC_PACKET);
						return;
				}
			}

			// Check if the target is in the skill cast range
			if (dontMove)
			{
				// Calculate the distance between the L2PcInstance and the target
				if (sklTargetType == SkillTargetType.TARGET_GROUND)
				{
					if (!isInsideRadius(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), skill.getCastRange()+getTemplate().collisionRadius, false, false))
					{
						// Send a System Message to the caster
						sendPacket(new SystemMessage(SystemMessageId.TARGET_TOO_FAR));

						// Send a Server->Client packet ActionFailed to the L2PcInstance
						sendPacket(ActionFailed.STATIC_PACKET);
						return;
					}
				}
				else if (skill.getCastRange() > 0 && !isInsideRadius(target, skill.getCastRange()+getTemplate().collisionRadius, false, false))
				{
					// Send a System Message to the caster
					sendPacket(new SystemMessage(SystemMessageId.TARGET_TOO_FAR));

					// Send a Server->Client packet ActionFailed to the L2PcInstance
					sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
			}
		}

		// Check if the skill is defensive
        if (!skill.isOffensive() && target instanceof L2MonsterInstance && !forceUse)
		{
			// check if the target is a monster and if force attack is set.. if not then we don't want to cast.
        	switch (sklTargetType)
			{
				case TARGET_PET:
				case TARGET_AURA:
				case TARGET_FRONT_AURA:
				case TARGET_BEHIND_AURA:
				case TARGET_CLAN:
				case TARGET_SELF:
				case TARGET_PARTY:
				case TARGET_ALLY:
				case TARGET_CORPSE_MOB:
				case TARGET_AREA_CORPSE_MOB:
				case TARGET_GROUND:
					break;
				default:
				{
					switch (sklType)
					{
						case BEAST_FEED:
						case DELUXE_KEY_UNLOCK:
						case UNLOCK:
							break;
						default:
							sendPacket(ActionFailed.STATIC_PACKET);
						return;
					}
					break;
				}
			}
		}

		// Check if the skill is Spoil type and if the target isn't already spoiled
		if (sklType == SkillType.SPOIL)
		{
			if (!(target instanceof L2MonsterInstance))
			{
				// Send a System Message to the L2PcInstance
				sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));

				// Send a Server->Client packet ActionFailed to the L2PcInstance
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
		}

        // Check if the skill is Sweep type and if conditions not apply
        if (sklType == SkillType.SWEEP && target instanceof L2Attackable)
		{
			int spoilerId = ((L2Attackable) target).getIsSpoiledBy();
			
			if (((L2Attackable) target).isDead())
			{
				if (!((L2Attackable) target).isSpoil())
				{
					// Send a System Message to the L2PcInstance
					sendPacket(new SystemMessage(SystemMessageId.SWEEPER_FAILED_TARGET_NOT_SPOILED));
					
					// Send a Server->Client packet ActionFailed to the L2PcInstance
					sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
				
				if (getObjectId() != spoilerId && !isInLooterParty(spoilerId))
				{
					// Send a System Message to the L2PcInstance
					sendPacket(new SystemMessage(SystemMessageId.SWEEP_NOT_ALLOWED));
					
					// Send a Server->Client packet ActionFailed to the L2PcInstance
					sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
			}
		}

		// Check if the skill is Drain Soul (Soul Crystals) and if the target is a MOB
		if (sklType == SkillType.DRAIN_SOUL)
		{
			if (!(target instanceof L2MonsterInstance))
			{
				// Send a System Message to the L2PcInstance
				sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));

				// Send a Server->Client packet ActionFailed to the L2PcInstance
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
		}

		// Check if this is a Pvp skill and target isn't a non-flagged/non-karma player
		switch (sklTargetType)
        {
			case TARGET_PARTY:
			case TARGET_ALLY:   // For such skills, checkPvpSkill() is called from L2Skill.getTargetList()
			case TARGET_CLAN:   // For such skills, checkPvpSkill() is called from L2Skill.getTargetList()
			case TARGET_AURA:
			case TARGET_FRONT_AURA:
			case TARGET_BEHIND_AURA:
			case TARGET_GROUND:
			case TARGET_SELF:
				break;
			default:
				if (!checkPvpSkill(target, skill) && !getAccessLevel().allowPeaceAttack())
                {
					// Send a System Message to the L2PcInstance
					sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));

					// Send a Server->Client packet ActionFailed to the L2PcInstance
					sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
		}

        if (sklTargetType == SkillTargetType.TARGET_HOLY && !TakeCastle.checkIfOkToCastSealOfRule(this, false))
        {
            sendPacket(ActionFailed.STATIC_PACKET);
            abortCast();
            return;
        }
        
        if (sklTargetType == SkillTargetType.TARGET_FLAGPOLE && !TakeFort.checkIfOkToCastFlagDisplay(this, false))
        {
            sendPacket(ActionFailed.STATIC_PACKET);
            abortCast();
            return;
        }

        if (sklType == SkillType.SIEGEFLAG && !SiegeFlag.checkIfOkToPlaceFlag(this, false))
        {
            sendPacket(ActionFailed.STATIC_PACKET);
            abortCast();
            return;
        }
        else if (sklType == SkillType.STRSIEGEASSAULT && !StrSiegeAssault.checkIfOkToUseStriderSiegeAssault(this, false))
        {
        	sendPacket(ActionFailed.STATIC_PACKET);
        	abortCast();
        	return;
        }

        // GeoData Los Check here
        if (skill.getCastRange() > 0)
        {
        	if (sklTargetType == SkillTargetType.TARGET_GROUND)
        	{
        		if (!GeoData.getInstance().canSeeTarget(this, worldPosition))
        		{
        			sendPacket(new SystemMessage(SystemMessageId.CANT_SEE_TARGET));
        			sendPacket(ActionFailed.STATIC_PACKET);
	        			return;
        		}
        	}
        	else if (!GeoData.getInstance().canSeeTarget(this, target))
        	{
        		sendPacket(new SystemMessage(SystemMessageId.CANT_SEE_TARGET));
        		sendPacket(ActionFailed.STATIC_PACKET);
        		return;
        	}
        }

        // If all conditions are checked, create a new SkillDat object and set the player _currentSkill
        setCurrentSkill(skill, forceUse, dontMove);

		// Check if the active L2Skill can be casted (ex : not sleeping...), Check if the target is correct and Notify the AI with AI_INTENTION_CAST and target
        super.useMagic(skill);

    }

    public boolean isInLooterParty(int LooterId)
    {
    	L2PcInstance looter = (L2PcInstance)L2World.getInstance().findObject(LooterId);

    	// if L2PcInstance is in a CommandChannel
    	if (isInParty() && getParty().isInCommandChannel() && looter != null)
    		return getParty().getCommandChannel().getMembers().contains(looter);

    	if (isInParty() && looter != null)
            return getParty().getPartyMembers().contains(looter);

		return false;
    }

	/**
	 * Check if the requested casting is a Pc->Pc skill cast and if it's a valid pvp condition
	 * @param target L2Object instance containing the target
	 * @param skill L2Skill instance with the skill being casted
	 * @return False if the skill is a pvpSkill and target is not a valid pvp target
	 */
	public boolean checkPvpSkill(L2Object target, L2Skill skill)
	{
		// check for PC->PC Pvp status
		if (
				target != null &&                                           			// target not null and
				target != this &&                                           			// target is not self and
				target instanceof L2PcInstance &&                           			// target is L2PcInstance and
				!(isInDuel() && ((L2PcInstance)target).getDuelId() == getDuelId()) &&	// self is not in a duel and attacking opponent
				!isInsideZone(ZONE_PVP) &&        						// Pc is not in PvP zone
				!((L2PcInstance)target).isInsideZone(ZONE_PVP)         	// target is not in PvP zone
		)
		{
			if(skill.isPvpSkill()) // pvp skill
			{
				if(getClan() != null && ((L2PcInstance)target).getClan() != null)
				{
					if(getClan().isAtWarWith(((L2PcInstance)target).getClan().getClanId()) && ((L2PcInstance)target).getClan().isAtWarWith(getClan().getClanId()))
						return true; // in clan war player can attack whites even with sleep etc.
				}
				if (
						((L2PcInstance)target).getPvpFlag() == 0 &&             //   target's pvp flag is not set and
						((L2PcInstance)target).getKarma() == 0                  //   target has no karma
					)
					return false;
			}
			else if (getCurrentSkill() != null && !getCurrentSkill().isCtrlPressed() && skill.isOffensive())
			{
				if (
						((L2PcInstance)target).getPvpFlag() == 0 &&             //   target's pvp flag is not set and
						((L2PcInstance)target).getKarma() == 0                  //   target has no karma
					)
					return false;
			}
		}

		return true;
	}

	/**
	 * Return True if the L2PcInstance is a Mage.<BR><BR>
	 */
	public boolean isMageClass()
	{
		return getClassId().isMage();
	}


	public boolean isMounted()
	{
		return _mountType > 0;
	}

	/**
	 * Set the type of Pet mounted (0 : none, 1 : Stridder, 2 : Wyvern) and send a Server->Client packet InventoryUpdate to the L2PcInstance.<BR><BR>
	 */
	public boolean checkLandingState()
	{
		// Check if char is in a no landing zone
		if (isInsideZone(ZONE_NOLANDING))
			return true;
		else
			// if this is a castle that is currently being sieged, and the rider is NOT a castle owner
			// he cannot land.
			// castle owner is the leader of the clan that owns the castle where the pc is
			if (isInsideZone(ZONE_SIEGE) && !(getClan()!= null &&
							CastleManager.getInstance().getCastle(this) == CastleManager.getInstance().getCastleByOwner(getClan()) &&
							this == getClan().getLeader().getPlayerInstance()))
				return true;

		return false;
	}

	// returns false if the change of mount type fails.
	public boolean setMount(int npcId, int mountType)
	{
		if (checkLandingState() && mountType == 2)
			return false;

		switch(mountType)
		{
			case 0:
                setIsFlying(false);
				setIsRidingFenrirWolf(false);
				setIsRidingWFenrirWolf(false);
				setIsRidingGreatSnowWolf(false);
				setIsRidingStrider(false);
                break; //Dismounted
			case 1:
                setIsRidingStrider(true);
                if(isNoble())
                {
                	L2Skill striderAssaultSkill = SkillTable.getInstance().getInfo(325, 1);
                	addSkill(striderAssaultSkill, false); // not saved to DB
                }
                break;
			case 2:
                setIsFlying(true);
                break; //Flying Wyvern
			case 3:
				setIsRidingFenrirWolf(true);
				setIsRidingWFenrirWolf(true);
				setIsRidingGreatSnowWolf(true);
				break;	

		}

		_mountType = mountType;
        _mountNpcId = npcId;

		return true;
	}

	/**
	 * Return the type of Pet mounted (0 : none, 1 : Stridder, 2 : Wyvern).<BR><BR>
	 */
	public int getMountType()
	{
		return _mountType;
	}

	/**
	 * Send a Server->Client packet UserInfo to this L2PcInstance and CharInfo to all L2PcInstance in its _KnownPlayers.<BR><BR>
	 *
	 * <B><U> Concept</U> :</B><BR><BR>
	 * Others L2PcInstance in the detection area of the L2PcInstance are identified in <B>_knownPlayers</B>.
	 * In order to inform other players of this L2PcInstance state modifications, server just need to go through _knownPlayers to send Server->Client Packet<BR><BR>
	 *
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Send a Server->Client packet UserInfo to this L2PcInstance (Public and Private Data)</li>
	 * <li>Send a Server->Client packet CharInfo to all L2PcInstance in _KnownPlayers of the L2PcInstance (Public data only)</li><BR><BR>
	 *
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : DON'T SEND UserInfo packet to other players instead of CharInfo packet.
	 * Indeed, UserInfo packet contains PRIVATE DATA as MaxHP, STR, DEX...</B></FONT><BR><BR>
	 *
	 */
	@Override
	public void updateAbnormalEffect()
	{
		broadcastUserInfo();
	}

	/**
	 * Disable the Inventory and create a new task to enable it after 1.5s.<BR><BR>
	 */
	public void tempInvetoryDisable()
	{
		_inventoryDisable  = true;

		ThreadPoolManager.getInstance().scheduleGeneral(new InventoryEnable(), 1500);
	}

	/**
	 * Return True if the Inventory is disabled.<BR><BR>
	 */
	public boolean isInvetoryDisabled()
	{
		return _inventoryDisable;
	}

	class InventoryEnable implements Runnable
	{
		public void run()
		{
			_inventoryDisable = false;
		}
	}

	public Map<Integer, L2CubicInstance> getCubics()
	{
		return _cubics;
	}

	/**
	 * Add a L2CubicInstance to the L2PcInstance _cubics.<BR><BR>
	 */
	public void addCubic(int id, int level, double matk, int activationtime, int activationchance)
	{
		if (Config.DEBUG)
			_log.info("L2PcInstance(" + getName() + "): addCubic(" + id + "|" + level + "|" + matk + ")");
		L2CubicInstance cubic = new L2CubicInstance(this, id, level, (int) matk, activationtime, activationchance);

		_cubics.put(id, cubic);
	}

	/**
	 * Remove a L2CubicInstance from the L2PcInstance _cubics.<BR><BR>
	 */
	public void delCubic(int id)
	{
		_cubics.remove(id);
	}

	/**
	 * Return the L2CubicInstance corresponding to the Identifier of the L2PcInstance _cubics.<BR><BR>
	 */
	public L2CubicInstance getCubic(int id)
	{
		return _cubics.get(id);
	}

	@Override
	public String toString()
	{
		return "player "+getName();
	}

	/**
	 * Return the modifier corresponding to the Enchant Effect of the Active Weapon (Min : 127).<BR><BR>
	 */
	public int getEnchantEffect()
	{
		L2ItemInstance wpn = getActiveWeaponInstance();

		if (wpn == null)
			return 0;

		return Math.min(127, wpn.getEnchantLevel());
	}

	/**
	 * Set the _lastFolkNpc of the L2PcInstance corresponding to the last Folk wich one the player talked.<BR><BR>
	 */
	public void setLastFolkNPC(L2FolkInstance folkNpc)
	{
		_lastFolkNpc = folkNpc;
	}

	/**
	 * Return the _lastFolkNpc of the L2PcInstance corresponding to the last Folk wich one the player talked.<BR><BR>
	 */
	public L2FolkInstance getLastFolkNPC()
	{
		return _lastFolkNpc;
	}

	/**
	 * Return True if L2PcInstance is a participant in the Festival of Darkness.<BR><BR>
	 */
	public boolean isFestivalParticipant()
	{
		return SevenSignsFestival.getInstance().isParticipant(this);
	}

	public void addAutoSoulShot(int itemId)
	{
		_activeSoulShots.put(itemId, itemId);
	}

	public void removeAutoSoulShot(int itemId)
	{
		_activeSoulShots.remove(itemId);
	}

	public Map<Integer, Integer> getAutoSoulShot()
	{
		return _activeSoulShots;
	}

	public void rechargeAutoSoulShot(boolean physical, boolean magic, boolean summon)
	{
		L2ItemInstance item;
		IItemHandler handler;
		
		if (_activeSoulShots == null || _activeSoulShots.size() == 0)
			return;
		
		try
		{
			Collection<Integer> vals = _activeSoulShots.values();
			
			for (int itemId : vals)
			{
				item = getInventory().getItemByItemId(itemId);
				
				if (item != null)
				{
					if (magic)
					{
						if (!summon)
						{
							if (itemId == 2509 || itemId == 2510
							        || itemId == 2511 || itemId == 2512
							        || itemId == 2513 || itemId == 2514
							        || itemId == 3947 || itemId == 3948
							        || itemId == 3949 || itemId == 3950
							        || itemId == 3951 || itemId == 3952
							        || itemId == 5790)
							{
								handler = ItemHandler.getInstance().getItemHandler(itemId);
								
								if (handler != null)
									handler.useItem(this, item);
							}
						}
						else
						{
							if (itemId == 6646 || itemId == 6647)
							{
								handler = ItemHandler.getInstance().getItemHandler(itemId);
								
								if (handler != null)
									handler.useItem(this, item);
							}
						}
					}
					
					if (physical)
					{
						if (!summon)
						{
							if (itemId == 1463 || itemId == 1464
							        || itemId == 1465 || itemId == 1466
							        || itemId == 1467 || itemId == 1835
							        || itemId == 5789)
							{
								handler = ItemHandler.getInstance().getItemHandler(itemId);
								
								if (handler != null)
									handler.useItem(this, item);
							}
						}
						else
						{
							if (itemId == 6645)
							{
								handler = ItemHandler.getInstance().getItemHandler(itemId);
								
								if (handler != null)
									handler.useItem(this, item);
							}
						}
					}
				}
				else
				{
					removeAutoSoulShot(itemId);
				}
			}
		}
		catch (NullPointerException npe)
		{
			
		}
	}

	private ScheduledFuture<?> _taskWarnUserTakeBreak;

	class WarnUserTakeBreak implements Runnable
	{
		public void run()
		{
			if (L2PcInstance.this.isOnline() == 1)
			{
				SystemMessage msg = new SystemMessage(SystemMessageId.PLAYING_FOR_LONG_TIME);
				L2PcInstance.this.sendPacket(msg);
			}
			else
				stopWarnUserTakeBreak();
		}
	}

	class RentPetTask implements Runnable
	{
		public void run()
		{
			stopRentPet();
		}
	}

    public ScheduledFuture<?> _taskforfish;

	class WaterTask implements Runnable
	{
		public void run()
		{
			double reduceHp = getMaxHp()/100.0;

			if (reduceHp < 1)
				reduceHp = 1;

			reduceCurrentHp(reduceHp,L2PcInstance.this,false);
			//reduced hp, becouse not rest
			SystemMessage sm = new SystemMessage(SystemMessageId.DROWN_DAMAGE_S1);
			sm.addNumber((int)reduceHp);
			sendPacket(sm);

		}
	}

    class LookingForFishTask implements Runnable
    {
    	boolean _isNoob, _isUpperGrade;
    	int _fishType, _fishGutsCheck, _gutsCheckTime;
    	long _endTaskTime;

    	protected LookingForFishTask(int fishWaitTime, int fishGutsCheck, int fishType, boolean isNoob, boolean isUpperGrade)
    	{
    		_fishGutsCheck = fishGutsCheck;
    		_endTaskTime = System.currentTimeMillis() + fishWaitTime + 10000;
    		_fishType = fishType;
    		_isNoob = isNoob;
    		_isUpperGrade = isUpperGrade;
    	}

        public void run()
        {
            if (System.currentTimeMillis() >= _endTaskTime) {
            	endFishing(false);
            	return;
            }
        	if (_fishType == -1)
        		return;
            int check = Rnd.get(1000);
            if(_fishGutsCheck > check)
            {
                stopLookingForFishTask();
                startFishCombat(_isNoob, _isUpperGrade);
            }
        }

    }

	public int getClanPrivileges()
    {
		return _clanPrivileges;
	}

	public void setClanPrivileges(int n)
    {
		_clanPrivileges = n;
	}

    // baron etc
	public void setPledgeClass(int classId)
    {
        _pledgeClass = classId;
    }

    public int getPledgeClass()
    {
        return _pledgeClass;
    }

    public void setPledgeType(int typeId)
    {
        _pledgeType = typeId;
    }

    public int getPledgeType()
    {
        return _pledgeType;
    }

    public int getApprentice()
    {
    	return _apprentice;
    }

    public void setApprentice(int apprentice_id)
    {
    	_apprentice = apprentice_id;
    }

    public int getSponsor()
    {
    	return _sponsor;
    }

    public void setSponsor(int sponsor_id)
    {
    	_sponsor = sponsor_id;
    }

	@Override
	public void sendMessage(String message)
	{
		sendPacket(SystemMessage.sendString(message));
	}

	public void enterObserverMode(int x, int y, int z)
	{
		_obsX = getX();
		_obsY = getY();
		_obsZ = getZ();

        setTarget(null);
		stopMove(null);
		setIsParalyzed(true);
        setIsInvul(true);
		getAppearance().setInvisible();
		sendPacket(new ObservationMode(x, y, z));
		setXYZ(x, y, z);

		_observerMode = true;
		broadcastUserInfo();
	}

	public void enterOlympiadObserverMode(int x, int y, int z, int id)
    {
        if (getPet() != null)
            getPet().unSummon(this);

        if (getCubics().size() > 0)
        {
            for (L2CubicInstance cubic : getCubics().values())
            {
                cubic.stopAction();
                cubic.cancelDisappear();
            }

            getCubics().clear();
        }

    	_olympiadGameId = id;
        _obsX = getX();
        if (isSitting())
            standUp();
        _obsY = getY();
        _obsZ = getZ();
        setTarget(null);
        setIsInvul(true);
        getAppearance().setInvisible();
        teleToLocation(x, y, z, false);
        sendPacket(new ExOlympiadMode(3));
        _observerMode = true;
		broadcastUserInfo();
    }

	public void leaveObserverMode()
	{
		setTarget(null);
		setXYZ(_obsX, _obsY, _obsZ);
		setIsParalyzed(false);
		getAppearance().setVisible();
        setIsInvul(false);

		if (getAI() != null)
			getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);

        _observerMode = false;
		sendPacket(new ObservationReturn(this));
		broadcastUserInfo();
	}

	public void leaveOlympiadObserverMode()
    {
		setTarget(null);
		sendPacket(new ExOlympiadMode(0));
        teleToLocation(_obsX, _obsY, _obsZ, true);
        getAppearance().setVisible();
        setIsInvul(false);
        if (getAI() != null)
		{
            getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
        }
        Olympiad.getInstance().removeSpectator(_olympiadGameId, this);
        _olympiadGameId = -1;
        _observerMode = false;
		broadcastUserInfo();
    }

    public void setOlympiadSide(int i)
    {
        _olympiadSide = i;
    }

    public int getOlympiadSide()
    {
        return _olympiadSide;
    }

    public void setOlympiadGameId(int id)
    {
        _olympiadGameId = id;
    }

    public int getOlympiadGameId()
    {
        return _olympiadGameId;
    }

	public int getObsX()
	{
		return _obsX;
	}

	public int getObsY()
	{
		return _obsY;
	}

	public int getObsZ()
	{
		return _obsZ;
	}

	public boolean inObserverMode()
	{
		return _observerMode;
	}

	public int getTeleMode()
	{
		return _telemode;
	}

	public void setTeleMode(int mode)
	{
		_telemode = mode;
	}

	public void setLoto(int i, int val)
	{
		_loto[i] = val;
	}

	public int getLoto(int i)
	{
		return _loto[i];
	}

	public void setRace(int i, int val)
	{
		_race[i] = val;
	}

	public int getRace(int i)
	{
		return _race[i];
	}

	public void setChatBanned(boolean isBanned)
	{
		_chatBanned = isBanned;

		if (isChatBanned())
			sendMessage("You have been chat banned by a server admin.");
		else
		{
			sendMessage("Your chat ban has been lifted.");
			if (_chatUnbanTask != null)
				_chatUnbanTask.cancel(false);
			_chatUnbanTask = null;
		}
		sendPacket(new EtcStatusUpdate(this));
	}

	public boolean isChatBanned()
	{
		return _chatBanned;
	}

    public void setChatUnbanTask(ScheduledFuture<?> task)
    {
         _chatUnbanTask = task;
    }
    public ScheduledFuture<?> getChatUnbanTask()
    {
        return _chatUnbanTask;
    }

	public boolean getMessageRefusal()
	{
		return _messageRefusal;
	}

	public void setMessageRefusal(boolean mode)
	{
		_messageRefusal = mode;
		sendPacket(new EtcStatusUpdate(this));
	}

	public void setDietMode(boolean mode)
	{
		_dietMode = mode;
	}

	public boolean getDietMode()
	{
		return _dietMode;
	}

	public void setTradeRefusal(boolean mode)
	{
		_tradeRefusal = mode;
	}

	public boolean getTradeRefusal()
	{
		return _tradeRefusal;
	}

	public void setExchangeRefusal(boolean mode)
	{
		_exchangeRefusal = mode;
	}

	public boolean getExchangeRefusal()
	{
		return _exchangeRefusal;
	}

	public BlockList getBlockList()
	{
		return _blockList;
	}

	public void setHero(boolean hero)
	{
		if (hero && _baseClass == _activeClass)
		{
			for (L2Skill s : HeroSkillTable.getHeroSkills())
				addSkill(s, false); //Dont Save Hero skills to database
		}
		else
		{
			for (L2Skill s : HeroSkillTable.getHeroSkills())
				super.removeSkill(s); //Just Remove skills from nonHero characters
		}
		_hero = hero;
		
		sendSkillList();
	}

	public void setIsInOlympiadMode(boolean b)
    {
    	_inOlympiadMode = b;
    }

	public void setIsOlympiadStart(boolean b)
    {
    	_OlympiadStart = b;
    }

	public boolean isOlympiadStart(){
		return _OlympiadStart;
	}

    public boolean isHero()
    {
        return _hero;
    }

    public boolean isInOlympiadMode()
    {
    	return _inOlympiadMode;
    }

	public boolean isInDuel()
	{
		return _isInDuel;
	}

	public int getDuelId()
	{
		return _duelId;
	}

	public void setDuelState(int mode)
	{
		_duelState = mode;
	}

	public int getDuelState()
	{
		return _duelState;
	}

	/**
	 * Sets up the duel state using a non 0 duelId.
	 * @param duelId 0=not in a duel
	 */
	public void setIsInDuel(int duelId)
	{
		if (duelId > 0)
		{
			_isInDuel = true;
			_duelState = Duel.DUELSTATE_DUELLING;
			_duelId = duelId;
		}
		else
		{
			if (_duelState == Duel.DUELSTATE_DEAD) { enableAllSkills(); getStatus().startHpMpRegeneration(); }
			_isInDuel = false;
			_duelState = Duel.DUELSTATE_NODUEL;
			_duelId = 0;
		}
	}

	/**
	 * This returns a SystemMessage stating why
	 * the player is not available for duelling.
	 * @return S1_CANNOT_DUEL... message
	 */
	public SystemMessage getNoDuelReason()
	{
		SystemMessage sm = new SystemMessage(_noDuelReason);
		sm.addPcName(this);
		_noDuelReason = SystemMessageId.THERE_IS_NO_OPPONENT_TO_RECEIVE_YOUR_CHALLENGE_FOR_A_DUEL;
		return sm;
	}

	/**
	 * Checks if this player might join / start a duel.
	 * To get the reason use getNoDuelReason() after calling this function.
	 * @return true if the player might join/start a duel.
	 */
	public boolean canDuel()
	{
		if (isInCombat() || isInJail()) { _noDuelReason = SystemMessageId.S1_CANNOT_DUEL_BECAUSE_S1_IS_CURRENTLY_ENGAGED_IN_BATTLE; return false; }
		if (isDead() || isAlikeDead() || (getCurrentHp() < getMaxHp()/2 || getCurrentMp() < getMaxMp()/2)) { _noDuelReason = SystemMessageId.S1_CANNOT_DUEL_BECAUSE_S1S_HP_OR_MP_IS_BELOW_50_PERCENT; return false; }
		if (isInDuel()) { _noDuelReason = SystemMessageId.S1_CANNOT_DUEL_BECAUSE_S1_IS_ALREADY_ENGAGED_IN_A_DUEL; return false;}
		if (isInOlympiadMode()) { _noDuelReason = SystemMessageId.S1_CANNOT_DUEL_BECAUSE_S1_IS_PARTICIPATING_IN_THE_OLYMPIAD; return false; }
		if (isCursedWeaponEquipped()) { _noDuelReason = SystemMessageId.S1_CANNOT_DUEL_BECAUSE_S1_IS_IN_A_CHAOTIC_STATE; return false; }
		if (getPrivateStoreType() != STORE_PRIVATE_NONE) { _noDuelReason = SystemMessageId.S1_CANNOT_DUEL_BECAUSE_S1_IS_CURRENTLY_ENGAGED_IN_A_PRIVATE_STORE_OR_MANUFACTURE; return false; }
		if (isMounted() || isInBoat()) { _noDuelReason = SystemMessageId.S1_CANNOT_DUEL_BECAUSE_S1_IS_CURRENTLY_RIDING_A_BOAT_WYVERN_OR_STRIDER; return false; }
		if (isFishing()) { _noDuelReason = SystemMessageId.S1_CANNOT_DUEL_BECAUSE_S1_IS_CURRENTLY_FISHING; return false; }
		if (isInsideZone(ZONE_PVP) || isInsideZone(ZONE_PEACE) || isInsideZone(ZONE_SIEGE))
		{
			_noDuelReason = SystemMessageId.S1_CANNOT_MAKE_A_CHALLANGE_TO_A_DUEL_BECAUSE_S1_IS_CURRENTLY_IN_A_DUEL_PROHIBITED_AREA;
			return false;
		}
		return true;
	}

    public boolean isNoble()
    {
    	return _noble;
    }

    public void setNoble(boolean val)
    {
    	if (val)
    		for (L2Skill s : NobleSkillTable.getInstance().getNobleSkills())
    			addSkill(s, false); //Dont Save Noble skills to Sql
    	else
    		for (L2Skill s : NobleSkillTable.getInstance().getNobleSkills())
    			super.removeSkill(s); //Just Remove skills without deleting from Sql
    	_noble = val;
    	
    	sendSkillList();
    }

    public void setLvlJoinedAcademy(int lvl)
    {
    	_lvlJoinedAcademy = lvl;
    }

    public int getLvlJoinedAcademy()
    {
    	return _lvlJoinedAcademy;
    }

    public boolean isAcademyMember()
    {
      return _lvlJoinedAcademy > 0;
    }

	public void setTeam(int team)
	{
		_team = team;
	}

	public int getTeam()
	{
		return _team;
	}

	public void setWantsPeace(int wantsPeace)
	{
		_wantsPeace = wantsPeace;
	}

	public int getWantsPeace()
	{
		return _wantsPeace;
	}

    public boolean isFishing()
    {
        return _fishing;
    }

    public void setFishing(boolean fishing)
    {
        _fishing = fishing;
    }

    public void setAllianceWithVarkaKetra(int sideAndLvlOfAlliance)
    {
    	// [-5,-1] varka, 0 neutral, [1,5] ketra
    	_alliedVarkaKetra = sideAndLvlOfAlliance;
    }

    public int getAllianceWithVarkaKetra()
    {
    	return _alliedVarkaKetra;
    }

    public boolean isAlliedWithVarka()
    {
        return (_alliedVarkaKetra < 0);
    }

    public boolean isAlliedWithKetra()
    {
        return (_alliedVarkaKetra > 0);
    }
    public void sendSkillList()  
    {  
    	sendSkillList(this);  
    }  
    
    public void sendSkillList(L2PcInstance player)  
    {  
    	SkillList sl = new SkillList();  
    	if(player != null)  
    	{  
    		for (L2Skill s : player.getAllSkills())  
    		{  
    			if (s == null)   
    				continue;  
    			if (s.getId() > 9000)  
    				continue; // Fake skills to change base stats  
    			sl.addSkill(s.getId(), s.getLevel(), s.isPassive());  
    		}  
    	}  
    	sendPacket(sl);  
    }
    
    /**
     * 1. Add the specified class ID as a subclass (up to the maximum number of <b>three</b>)
     * for this character.<BR>
     * 2. This method no longer changes the active _classIndex of the player. This is only
     * done by the calling of setActiveClass() method as that should be the only way to do so.
	 *
     * @param int classId
     * @param int classIndex
     * @return boolean subclassAdded
     */
    public boolean addSubClass(int classId, int classIndex)
    {
    	if (getTotalSubClasses() == Config.MAX_SUBCLASS || classIndex == 0)
    		return false;

    	if (getSubClasses().containsKey(classIndex))
    		return false;

    	// Note: Never change _classIndex in any method other than setActiveClass().

        SubClass newClass = new SubClass();
        newClass.setClassId(classId);
        newClass.setClassIndex(classIndex);

        Connection con = null;

        try
        {
            // Store the basic info about this new sub-class.
            con = L2DatabaseFactory.getInstance().getConnection();
            PreparedStatement statement = con.prepareStatement(ADD_CHAR_SUBCLASS);
            
            statement.setInt(1, getObjectId());
            statement.setInt(2, newClass.getClassId());
            statement.setLong(3, newClass.getExp());
            statement.setInt(4, newClass.getSp());
            statement.setInt(5, newClass.getLevel());
            statement.setInt(6, newClass.getClassIndex()); // <-- Added
            
            statement.execute();
            statement.close();
        }
        catch (Exception e) {
            _log.warning("WARNING: Could not add character sub class for " + getName() + ": " + e);
            return false;
        }
        finally {
            try { con.close(); } catch (Exception e) {}
        }

        // Commit after database INSERT incase exception is thrown.
        getSubClasses().put(newClass.getClassIndex(), newClass);

        if (Config.DEBUG)
            _log.info(getName() + " added class ID " + classId + " as a sub class at index " + classIndex + ".");

		ClassId subTemplate = ClassId.values()[classId];
		Collection<L2SkillLearn> skillTree = SkillTreeTable.getInstance().getAllowedSkills(subTemplate);

		if (skillTree == null)
			return true;

		Map<Integer, L2Skill> prevSkillList = new FastMap<Integer, L2Skill>();

		for (L2SkillLearn skillInfo : skillTree)
		{
			if (skillInfo.getMinLevel() <= 40)
			{
				L2Skill prevSkill = prevSkillList.get(skillInfo.getId());
				L2Skill newSkill = SkillTable.getInstance().getInfo(skillInfo.getId(), skillInfo.getLevel());

				if (prevSkill != null && (prevSkill.getLevel() > newSkill.getLevel()))
					continue;

				prevSkillList.put(newSkill.getId(), newSkill);
				storeSkill(newSkill, prevSkill, classIndex);
			}
		}

        if (Config.DEBUG)
            _log.info(getName() + " was given " + getAllSkills().length + " skills for their new sub class.");

        return true;
    }

    /**
     * 1. Completely erase all existance of the subClass linked to the classIndex.<BR>
     * 2. Send over the newClassId to addSubClass()to create a new instance on this classIndex.<BR>
     * 3. Upon Exception, revert the player to their BaseClass to avoid further problems.<BR>
     *
     * @param int classIndex
     * @param int newClassId
     * @return boolean subclassAdded
     */
    public boolean modifySubClass(int classIndex, int newClassId)
    {
    	int oldClassId = getSubClasses().get(classIndex).getClassId();

        if (Config.DEBUG)
	    	_log.info(getName() + " has requested to modify sub class index " + classIndex + " from class ID " + oldClassId + " to " + newClassId + ".");

    	Connection con = null;

        try
        {
            con = L2DatabaseFactory.getInstance().getConnection();
            PreparedStatement statement;

            // Remove all henna info stored for this sub-class.
            statement = con.prepareStatement(DELETE_CHAR_HENNAS);
            statement.setInt(1, getObjectId());
            statement.setInt(2, classIndex);
            statement.execute();
            statement.close();

            // Remove all shortcuts info stored for this sub-class.
            statement = con.prepareStatement(DELETE_CHAR_SHORTCUTS);
            statement.setInt(1, getObjectId());
            statement.setInt(2, classIndex);
            statement.execute();
            statement.close();

            // Remove all effects info stored for this sub-class.
            statement = con.prepareStatement(DELETE_SKILL_SAVE);
            statement.setInt(1, getObjectId());
            statement.setInt(2, classIndex);
            statement.execute();
            statement.close();

            // Remove all skill info stored for this sub-class.
            statement = con.prepareStatement(DELETE_CHAR_SKILLS);
            statement.setInt(1, getObjectId());
            statement.setInt(2, classIndex);
            statement.execute();
            statement.close();

            // Remove all basic info stored about this sub-class.
            statement = con.prepareStatement(DELETE_CHAR_SUBCLASS);
            statement.setInt(1, getObjectId());
            statement.setInt(2, classIndex);
            statement.execute();
            statement.close();
        }
        catch (Exception e)
        {
        	_log.warning("Could not modify sub class for " + getName() + " to class index " + classIndex + ": " + e);

        	// This must be done in order to maintain data consistency.
            getSubClasses().remove(classIndex);
        	return false;
        }
        finally {
            try { con.close(); } catch (Exception e) {}
        }

        getSubClasses().remove(classIndex);
        return addSubClass(newClassId, classIndex);
    }

    public boolean isSubClassActive()
    {
        return _classIndex > 0;
    }

    public Map<Integer, SubClass> getSubClasses()
    {
        if (_subClasses == null)
            _subClasses = new FastMap<Integer, SubClass>();

        return _subClasses;
    }

    public int getTotalSubClasses()
    {
        return getSubClasses().size();
    }

	public int getBaseClass()
	{
		return _baseClass;
	}

	public int getActiveClass()
	{
		return _activeClass;
	}

	public int getClassIndex()
	{
		return _classIndex;
	}

	private void setClassTemplate(int classId)
	{
		_activeClass = classId;

		L2PcTemplate t = CharTemplateTable.getInstance().getTemplate(classId);

		if (t == null)
		{
			_log.severe("Missing template for classId: "+classId);
			throw new Error();
		}

		// Set the template of the L2PcInstance
		setTemplate(t);
	}

    /**
     * Changes the character's class based on the given class index.
     * <BR><BR>
     * An index of zero specifies the character's original (base) class,
     * while indexes 1-3 specifies the character's sub-classes respectively.
     *
     * @param classIndex
     */
    public boolean setActiveClass(int classIndex)
    {
        //  Cannot switch or change subclasses while transformed
        if (isTransformed())
            return false;
        
        // Remove active item skills before saving char to database
        // because next time when choosing this class, weared items can
        // be different
        for (L2ItemInstance temp : getInventory().getAugmentedItems())
            if (temp != null && temp.isEquipped()) temp.getAugmentation().removeBonus(this);
        
        // Delete a force buff upon class change.
        if(_forceBuff != null)
            abortCast();

        // Stop casting for any player that may be casting a force buff on this l2pcinstance.
		for(L2Character character : getKnownList().getKnownCharacters())
		{
			if(character.getForceBuff() != null && character.getForceBuff().getTarget() == this)
				character.abortCast();
		}

        /*
         * 1. Call store() before modifying _classIndex to avoid skill effects rollover.
         * 2. Register the correct _classId against applied 'classIndex'.
         */
        store();

        if (classIndex == 0)
        {
        	setClassTemplate(getBaseClass());
        }
        else
        {
            try {
            	setClassTemplate(getSubClasses().get(classIndex).getClassId());
            }
            catch (Exception e) {
                _log.info("Could not switch " + getName() + "'s sub class to class index " + classIndex + ": " + e);
                return false;
            }
        }
        _classIndex = classIndex;

        if(isInParty())
        	getParty().recalculatePartyLevel();


        /*
		 * Update the character's change in class status.
         *
		 * 1. Remove any active cubics from the player.
         * 2. Renovate the characters table in the database with the new class info, storing also buff/effect data.
         * 3. Remove all existing skills.
         * 4. Restore all the learned skills for the current class from the database.
         * 5. Restore effect/buff data for the new class.
         * 6. Restore henna data for the class, applying the new stat modifiers while removing existing ones.
         * 7. Reset HP/MP/CP stats and send Server->Client character status packet to reflect changes.
         * 8. Restore shortcut data related to this class.
         * 9. Resend a class change animation effect to broadcast to all nearby players.
         * 10.Unsummon any active servitor from the player.
         */

        if (getPet() instanceof L2SummonInstance)
        	getPet().unSummon(this);

        if (getCubics().size() > 0)
        {
            for (L2CubicInstance cubic : getCubics().values())
            {
                cubic.stopAction();
                cubic.cancelDisappear();
            }

            getCubics().clear();
        }
        
        for (L2Skill oldSkill : getAllSkills())
            super.removeSkill(oldSkill);

        stopAllEffects();

        if (isSubClassActive())
        {
            _dwarvenRecipeBook.clear();
            _commonRecipeBook.clear();
        }
        else
        {
            restoreRecipeBook();
		}

        // Restore any Death Penalty Buff
        restoreDeathPenaltyBuffLevel();

        restoreSkills();
        regiveTemporarySkills();
        rewardSkills();
        // Prevents some issues when changing between subclases that shares skills
        if(_disabledSkills != null && !_disabledSkills.isEmpty()) 
        	_disabledSkills.clear();
        restoreEffects();
        updateEffectIcons();
        sendPacket(new EtcStatusUpdate(this));
        
        //if player has quest 422: Repent Your Sins, remove it
        QuestState st = getQuestState("422_RepentYourSins");
        
        if (st != null)
        {
        	st.exitQuest(true);
        }

        for (int i = 0; i < 3; i++)
            _henna[i] = null;

        restoreHenna();
        sendPacket(new HennaInfo(this));

        if (getCurrentHp() > getMaxHp())
        	setCurrentHp(getMaxHp());
        if (getCurrentMp() > getMaxMp())
        	setCurrentMp(getMaxMp());
        if (getCurrentCp() > getMaxCp())
        	setCurrentCp(getMaxCp());
        broadcastUserInfo();
        refreshOverloaded();
		refreshExpertisePenalty();

        // Clear resurrect xp calculation
        setExpBeforeDeath(0);

        //_macroses.restore();
        //_macroses.sendUpdate();
        _shortCuts.restore();
        sendPacket(new ShortCutInit(this));            

        broadcastPacket(new SocialAction(getObjectId(), 15));
        sendPacket(new SkillCoolTime(this));

        //decayMe();
        //spawnMe(getX(), getY(), getZ());

        return true;
    }

	public void stopWarnUserTakeBreak()
	{
		if (_taskWarnUserTakeBreak != null)
		{
			_taskWarnUserTakeBreak.cancel(true);
			_taskWarnUserTakeBreak = null;
		}
	}

	public void startWarnUserTakeBreak()
	{
		if (_taskWarnUserTakeBreak == null)
			_taskWarnUserTakeBreak = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new WarnUserTakeBreak(), 7200000, 7200000);
	}

	public void stopRentPet()
	{
		if (_taskRentPet != null)
		{
			// if the rent of a wyvern expires while over a flying zone, tp to down before unmounting
			if (checkLandingState() && getMountType()==2)
				teleToLocation(MapRegionTable.TeleportWhereType.Town);
			
			if (this.dismount())  // this should always be true now, since we teleported already
			{
				_taskRentPet.cancel(true);
				_taskRentPet = null;
			}
		}
	}

	public void startRentPet(int seconds)
	{
		if (_taskRentPet == null)
			_taskRentPet = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new RentPetTask(), seconds * 1000L, seconds * 1000L);
	}

	public boolean isRentedPet()
	{
		if (_taskRentPet != null)
			return true;

		return false;
	}

	public void stopWaterTask()
	{
		if (_taskWater != null)
		{
			_taskWater.cancel(false);

			_taskWater = null;
			sendPacket(new SetupGauge(2, 0));
		}
	}

	public void startWaterTask()
	{
		if (!isDead() && _taskWater == null)
		{
			int timeinwater = (int)calcStat(Stats.BREATH, 60000, this, null);

			sendPacket(new SetupGauge(2, timeinwater));
			_taskWater = ThreadPoolManager.getInstance().scheduleEffectAtFixedRate(new WaterTask(), timeinwater, 1000);
		}
	}

	public boolean isInWater()
	{
		if (_taskWater != null)
			return true;

		return false;
	}

	public void checkWaterState()
	{
		if (isInsideZone(ZONE_WATER))
			startWaterTask();
		else 
			stopWaterTask();
	}

	public void onPlayerEnter()
	{
		startWarnUserTakeBreak();

        if (SevenSigns.getInstance().isSealValidationPeriod() || SevenSigns.getInstance().isCompResultsPeriod())
        {
            if (!isGM() && isIn7sDungeon() && SevenSigns.getInstance().getPlayerCabal(this) != SevenSigns.getInstance().getCabalHighestScore())
            {
                teleToLocation(MapRegionTable.TeleportWhereType.Town);
                setIsIn7sDungeon(false);
                sendMessage("You have been teleported to the nearest town due to the beginning of the Seal Validation period.");
            }
        }
        else
        {
            if (!isGM() && isIn7sDungeon() && SevenSigns.getInstance().getPlayerCabal(this) == SevenSigns.CABAL_NULL)
            {
                teleToLocation(MapRegionTable.TeleportWhereType.Town);
                setIsIn7sDungeon(false);
                sendMessage("You have been teleported to the nearest town because you have not signed for any cabal.");
            }
        }

        // jail task
        updateJailState();

        if (_isInvul)
        	sendMessage("Entering world in Invulnerable mode.");
        if (getAppearance().getInvisible())
            sendMessage("Entering world in Invisible mode.");
        if (getMessageRefusal())
            sendMessage("Entering world in Message Refusal mode.");

		revalidateZone(true);
	}

	public long getLastAccess()
	{
		return _lastAccess;
	}

	private void checkRecom(int recsHave, int recsLeft)
	{
		Calendar check = Calendar.getInstance();
		check.setTimeInMillis(_lastRecomUpdate);
		check.add(Calendar.DAY_OF_MONTH,1);

		Calendar min = Calendar.getInstance();

		_recomHave = recsHave;
		_recomLeft = recsLeft;

		if (getStat().getLevel() < 10 || check.after(min) )
			return;

		restartRecom();
	}

	public void restartRecom()
	{
		if (Config.ALT_RECOMMEND)
		{
			Connection con = null;
			try
			{
				con = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement statement = con.prepareStatement(DELETE_CHAR_RECOMS);
				statement.setInt(1, getObjectId());
				
				statement.execute();
				statement.close();

				_recomChars.clear();
			}
			catch (Exception e)
			{
				_log.warning("could not clear char recommendations: "+e);
			}
			finally
			{
				try { con.close(); } catch (Exception e) {}
			}
		}

		if (getStat().getLevel() < 20)
		{
			_recomLeft = 3;
			_recomHave--;
		}
		else if (getStat().getLevel() < 40)
		{
			_recomLeft = 6;
			_recomHave -= 2;
		}
		else
		{
			_recomLeft = 9;
			_recomHave -= 3;
		}
		if (_recomHave < 0) _recomHave = 0;

		// If we have to update last update time, but it's now before 13, we should set it to yesterday
		Calendar update = Calendar.getInstance();
		if(update.get(Calendar.HOUR_OF_DAY) < 13) update.add(Calendar.DAY_OF_MONTH,-1);
		update.set(Calendar.HOUR_OF_DAY,13);
		_lastRecomUpdate = update.getTimeInMillis();
	}

	
	@Override
	public void doRevive()
	{
		super.doRevive();
		updateEffectIcons();
		sendPacket(new EtcStatusUpdate(this));
		_reviveRequested = 0;
		_revivePower = 0;

		if (isInParty() && getParty().isInDimensionalRift())
		{
			if (!DimensionalRiftManager.getInstance().checkIfInPeaceZone(getX(), getY(), getZ()))
				getParty().getDimensionalRift().memberRessurected(this);
		}
	}

	@Override
	public void doRevive(double revivePower)
	{
		// Restore the player's lost experience,
		// depending on the % return of the skill used (based on its power).
		restoreExp(revivePower);
		doRevive();
	}

	public void reviveRequest(L2PcInstance Reviver, L2Skill skill, boolean Pet)
	{
		if (_reviveRequested == 1)
		{
			if (_revivePet == Pet)
			{
				Reviver.sendPacket(new SystemMessage(SystemMessageId.RES_HAS_ALREADY_BEEN_PROPOSED)); // Resurrection is already been proposed.
			}
			else
			{
				if (Pet)
					Reviver.sendPacket(new SystemMessage(SystemMessageId.CANNOT_RES_PET2)); // A pet cannot be resurrected while it's owner is in the process of resurrecting.
				else
					Reviver.sendPacket(new SystemMessage(SystemMessageId.MASTER_CANNOT_RES)); // While a pet is attempting to resurrect, it cannot help in resurrecting its master.
			}
			return;
		}
		if((Pet && getPet() != null && getPet().isDead()) || (!Pet && isDead()))
		{
			_reviveRequested = 1;
			int restoreExp = 0;
			if (isPhoenixBlessed())
				_revivePower=100;
			else
				_revivePower = Formulas.getInstance().calculateSkillResurrectRestorePercent(skill.getPower(), Reviver.getWIT());
			
			restoreExp = (int)Math.round((getExpBeforeDeath() - getExp()) * _revivePower / 100);
			    
			_revivePet = Pet;
			
			ConfirmDlg dlg = new ConfirmDlg(SystemMessageId.RESSURECTION_REQUEST.getId());
			sendPacket(dlg.addPcName(Reviver).addString(""+restoreExp));
		}
	}

	public void reviveAnswer(int answer)
	{
		if (_reviveRequested != 1 || (!isDead() && !_revivePet) || (_revivePet && getPet() != null && !getPet().isDead()))
			return;
		//If character refuses a PhoenixBless autoress, cancell all buffs he had
		if (answer == 0 && isPhoenixBlessed())
		{
		    stopPhoenixBlessing(null);
		    stopAllEffects();
		}
		if (answer == 1)
		{
			if (!_revivePet)
			{
				if (_revivePower != 0)
					doRevive(_revivePower);
				else
					doRevive();
			}
			else if (getPet() != null)
			{
				if (_revivePower != 0)
					getPet().doRevive(_revivePower);
				else
					getPet().doRevive();
			}
		}
		_reviveRequested = 0;
		_revivePower = 0;
	}

	public boolean isReviveRequested()
	{
		return (_reviveRequested == 1);
	}

	public boolean isRevivingPet()
	{
		return _revivePet;
	}

	public void removeReviving()
	{
		_reviveRequested = 0;
		_revivePower = 0;
	}

	public void onActionRequest()
	{
		setProtection(false);
	}

	/**
	 * @param expertiseIndex The expertiseIndex to set.
	 */
	public void setExpertiseIndex(int expertiseIndex)
	{
		_expertiseIndex = expertiseIndex;
	}

	/**
	 * @return Returns the expertiseIndex.
	 */
	public int getExpertiseIndex()
	{
		return _expertiseIndex;
	}

	@Override
	public final void onTeleported()
	{
		super.onTeleported();
		
		// Force a revalidation
		revalidateZone(true);
		
		if ((Config.PLAYER_SPAWN_PROTECTION > 0) && !isInOlympiadMode())
			setProtection(true);
		
		// Modify the position of the tamed beast if necessary
		if (getTrainedBeast() != null)
		{
			getTrainedBeast().getAI().stopFollow();
			getTrainedBeast().teleToLocation(getPosition().getX() + Rnd.get(-100, 100), getPosition().getY() + Rnd.get(-100, 100), getPosition().getZ(), false);
			getTrainedBeast().getAI().startFollow(this);
		}
		
		// Modify the position of the pet if necessary
		if (getPet() != null)
		{
			getPet().setFollowStatus(false);
			getPet().teleToLocation(getPosition().getX() + Rnd.get(-100, 100), getPosition().getY() + Rnd.get(-100, 100), getPosition().getZ(), false);
			((L2SummonAI)getPet().getAI()).setStartFollowController(true);
			getPet().setFollowStatus(true);
			sendPacket(new PetInfo(getPet()));
			getPet().updateEffectIcons(true);
		}
		
	}

	@Override
	public final boolean updatePosition(int gameTicks)
	{
		// Disables custom movement for L2PCInstance when Old Synchronization is selected
		if (Config.COORD_SYNCHRONIZE == -1)
			return super.updatePosition(gameTicks);

		// Get movement data
		MoveData m = _move;

		if (_move == null)
			return true;

		if (!isVisible())
		{
			_move = null;
			return true;
		}

		// Check if the position has alreday be calculated
		if (m._moveTimestamp == 0)
			m._moveTimestamp = m._moveStartTime;

		// Check if the position has alreday be calculated
		if (m._moveTimestamp == gameTicks)
			return false;

		double dx = m._xDestination - getX();
		double dy = m._yDestination - getY();
		double dz = m._zDestination - getZ();
		int distPassed = (int)getStat().getMoveSpeed() * (gameTicks - m._moveTimestamp) / GameTimeController.TICKS_PER_SECOND;
		double distFraction = (distPassed) / Math.sqrt(dx*dx + dy*dy + dz*dz);
//		if (Config.DEVELOPER) _log.warning("Move Ticks:" + (gameTicks - m._moveTimestamp) + ", distPassed:" + distPassed + ", distFraction:" + distFraction);
		
		if (distFraction > 1)
		{
			// Set the position of the L2Character to the destination
			super.setXYZ(m._xDestination, m._yDestination, m._zDestination);
		}
		else
		{
			// Set the position of the L2Character to estimated after parcial move
			super.setXYZ(getX() + (int)(dx * distFraction + 0.5), getY() + (int)(dy * distFraction + 0.5), getZ() + (int)(dz * distFraction));
		}

		// Set the timer of last position update to now
		m._moveTimestamp = gameTicks;
		
		revalidateZone(false);

		return (distFraction > 1);
	}

	public void setLastClientPosition(int x, int y, int z)
	{
		_lastClientPosition.setXYZ(x,y,z);
	}

	public boolean checkLastClientPosition(int x, int y, int z)
	{
		return _lastClientPosition.equals(x,y,z);
	}

	public int getLastClientDistance(int x, int y, int z)
	{
		double dx = (x - _lastClientPosition.getX());
		double dy = (y - _lastClientPosition.getY());
		double dz = (z - _lastClientPosition.getZ());

		return (int)Math.sqrt(dx*dx + dy*dy + dz*dz);
	}

	public void setLastServerPosition(int x, int y, int z)
	{
		_lastServerPosition.setXYZ(x,y,z);
	}

	public boolean checkLastServerPosition(int x, int y, int z)
	{
		return _lastServerPosition.equals(x,y,z);
	}

	public int getLastServerDistance(int x, int y, int z)
	{
		double dx = (x - _lastServerPosition.getX());
		double dy = (y - _lastServerPosition.getY());
		double dz = (z - _lastServerPosition.getZ());

		return (int)Math.sqrt(dx*dx + dy*dy + dz*dz);
	}

	@Override
	public void addExpAndSp(long addToExp, int addToSp) { getStat().addExpAndSp(addToExp, addToSp); }
    public void removeExpAndSp(long removeExp, int removeSp) { getStat().removeExpAndSp(removeExp, removeSp); }
    @Override
	public void reduceCurrentHp(double i, L2Character attacker)
    {
    	getStatus().reduceHp(i, attacker);

    	// notify the tamed beast of attacks
    	if (getTrainedBeast() != null )
    		getTrainedBeast().onOwnerGotAttacked(attacker);
    }
	@Override
	public void reduceCurrentHp(double value, L2Character attacker, boolean awake)
	{
		getStatus().reduceHp(value, attacker, awake);

    	// notify the tamed beast of attacks
    	if (getTrainedBeast() != null )
    		getTrainedBeast().onOwnerGotAttacked(attacker);
	}

	public void broadcastSnoop(int type, String name, String _text)
	{
		if(_snoopListener.size() > 0)
		{
			Snoop sn = new Snoop(getObjectId(),getName(),type,name,_text);

            for (L2PcInstance pci : _snoopListener)
				if (pci != null)
					pci.sendPacket(sn);
		}
	}

	public void addSnooper(L2PcInstance pci )
	{
		if(!_snoopListener.contains(pci))
			_snoopListener.add(pci);
	}

	public void removeSnooper(L2PcInstance pci )
	{
		_snoopListener.remove(pci);
	}

	public void addSnooped(L2PcInstance pci )
	{
		if(!_snoopedPlayer.contains(pci))
			_snoopedPlayer.add(pci);
	}

	public void removeSnooped(L2PcInstance pci )
	{
		_snoopedPlayer.remove(pci);
	}

	public synchronized void addBypass(String bypass)
	{
		if (bypass == null) return;
		_validBypass.add(bypass);
		//_log.warning("[BypassAdd]"+getName()+" '"+bypass+"'");
	}

	public synchronized void addBypass2(String bypass)
	{
		if (bypass == null) return;
		_validBypass2.add(bypass);
		//_log.warning("[BypassAdd]"+getName()+" '"+bypass+"'");
	}

	public synchronized boolean validateBypass(String cmd)
	{
		if (!Config.BYPASS_VALIDATION)
			return true;

		for (String bp : _validBypass)
		{
		    if (bp == null) continue;

			//_log.warning("[BypassValidation]"+getName()+" '"+bp+"'");
			if (bp.equals(cmd))
				return true;
		}

		for (String bp : _validBypass2)
		{
		    if (bp == null) continue;

			//_log.warning("[BypassValidation]"+getName()+" '"+bp+"'");
			if (cmd.startsWith(bp))
				return true;
		}

		_log.warning("[L2PcInstance] player ["+getName()+"] sent invalid bypass '"+cmd+"', ban this player!");
		return false;
	}

	public boolean validateItemManipulation(int objectId, String action)
	{
		L2ItemInstance item = getInventory().getItemByObjectId(objectId);

		if (item == null || item.getOwnerId() != getObjectId())
		{
			_log.finest(getObjectId()+": player tried to " + action + " item he is not owner of");
			return false;
		}

		// Pet is summoned and not the item that summoned the pet AND not the buggle from strider you're mounting
		if (getPet() != null && getPet().getControlItemId() == objectId || getMountObjectID() == objectId)
		{
			if (Config.DEBUG)
				_log.finest(getObjectId()+": player tried to " + action + " item controling pet");

			return false;
		}

		if(getActiveEnchantItem() != null && getActiveEnchantItem().getObjectId() == objectId)
		{
			if (Config.DEBUG)
				_log.finest(getObjectId()+":player tried to " + action + " an enchant scroll he was using");

			return false;
		}

		if (CursedWeaponsManager.getInstance().isCursed(item.getItemId()))
		{
			// can not trade a cursed weapon
			return false;
		}

		if (item.isWear())
		{
			// cannot drop/trade wear-items
			return false;
		}

		return true;
	}

	public synchronized void clearBypass()
	{
        _validBypass.clear();
        _validBypass2.clear();
	}

	/**
	 * @return Returns the inBoat.
	 */
	public boolean isInBoat()
	{
		return _inBoat;
	}

	/**
	 * @param inBoat The inBoat to set.
	 */
	public void setInBoat(boolean inBoat)
	{
		_inBoat = inBoat;
	}

	/**
	 * @return
	 */
	public L2BoatInstance getBoat()
	{
		return _boat;
	}

	/**
	 * @param boat
	 */
	public void setBoat(L2BoatInstance boat)
	{
		_boat = boat;
	}

	public void setInCrystallize(boolean inCrystallize)
	{
		_inCrystallize = inCrystallize;
	}

	public boolean isInCrystallize()
	{
		return _inCrystallize;
	}

	/**
	 * @return
	 */
	public Point3D getInBoatPosition()
	{
		return _inBoatPosition;
	}

	public void setInBoatPosition(Point3D pt)
	{
		_inBoatPosition = pt;
	}

	/**
	 * Manage the delete task of a L2PcInstance (Leave Party, Unsummon pet, Save its inventory in the database, Remove it from the world...).<BR><BR>
	 *
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>If the L2PcInstance is in observer mode, set its position to its position before entering in observer mode </li>
	 * <li>Set the online Flag to True or False and update the characters table of the database with online status and lastAccess </li>
	 * <li>Stop the HP/MP/CP Regeneration task </li>
	 * <li>Cancel Crafting, Attak or Cast </li>
	 * <li>Remove the L2PcInstance from the world </li>
	 * <li>Stop Party and Unsummon Pet </li>
	 * <li>Update database with items in its inventory and remove them from the world </li>
	 * <li>Remove all L2Object from _knownObjects and _knownPlayer of the L2Character then cancel Attak or Cast and notify AI </li>
	 * <li>Close the connection with the client </li><BR><BR>
	 *
	 */
	public void deleteMe()
	{
		// Check if the L2PcInstance is in observer mode to set its position to its position before entering in observer mode
		if (inObserverMode())
			setXYZ(_obsX, _obsY, _obsZ);

		// Set the online Flag to True or False and update the characters table of the database with online status and lastAccess (called when login and logout)
		try { setOnlineStatus(false); } catch (Throwable t) {_log.log(Level.SEVERE, "deleteMe()", t); }

		// Stop the HP/MP/CP Regeneration task (scheduled tasks)
		try { stopAllTimers(); } catch (Throwable t) {_log.log(Level.SEVERE, "deleteMe()", t); }

		// Stop crafting, if in progress
		try { RecipeController.getInstance().requestMakeItemAbort(this); } catch (Throwable t) {_log.log(Level.SEVERE, "deleteMe()", t); }

		// Cancel Attak or Cast
		try { setTarget(null); } catch (Throwable t) {_log.log(Level.SEVERE, "deleteMe()", t); }
		
		try
		{
			if(_forceBuff != null)
				abortCast();

			for(L2Character character : getKnownList().getKnownCharacters())
				if(character.getForceBuff() != null && character.getForceBuff().getTarget() == this)
					character.abortCast();
		}
		catch(Throwable t) {_log.log(Level.SEVERE, "deleteMe()", t); }
		
		try
		{
			for (L2Effect effect : getAllEffects())
			{
				switch (effect.getEffectType())
				{
					case SIGNET_GROUND:
					case SIGNET_EFFECT:
						effect.exit();
						break;
				}
			}
		}
		catch (Throwable t)
		{
			_log.log(Level.SEVERE, "deleteMe()", t);
		}

		// Remove from world regions zones
        L2WorldRegion oldRegion = getWorldRegion();
		
		// Remove the L2PcInstance from the world
		if (isVisible())
			try { decayMe(); } catch (Throwable t) {_log.log(Level.SEVERE, "deleteMe()", t); }

	    if (oldRegion != null) oldRegion.removeFromZones(this);
			
		// If a Party is in progress, leave it (and festival party)
		if (isInParty()) try { leaveParty(); } catch (Throwable t) {_log.log(Level.SEVERE, "deleteMe()", t); }

		if (getOlympiadGameId() != -1) // handle removal from olympiad game
		    Olympiad.getInstance().removeDisconnectedCompetitor(this);
		
		// If the L2PcInstance has Pet, unsummon it
		if (getPet() != null)
		{
			try { getPet().unSummon(this); } catch (Throwable t) {_log.log(Level.SEVERE, "deleteMe()", t); }// returns pet to control item
		}

		if (getClanId() != 0 && getClan() != null)
		{
			// set the status for pledge member list to OFFLINE
			try
			{
				L2ClanMember clanMember = getClan().getClanMember(getName());
				if (clanMember != null) clanMember.setPlayerInstance(null);
			} catch (Throwable t) {_log.log(Level.SEVERE, "deleteMe()", t); }
		}

		if (getActiveRequester() != null)
		{
			// deals with sudden exit in the middle of transaction
			setActiveRequester(null);
		}

		// If the L2PcInstance is a GM, remove it from the GM List
		if (isGM())
		{
			try { GmListTable.getInstance().deleteGm(this); } catch (Throwable t) {_log.log(Level.SEVERE, "deleteMe()", t); }
		}

		// Update database with items in its inventory and remove them from the world
		try { getInventory().deleteMe(); } catch (Throwable t) {_log.log(Level.SEVERE, "deleteMe()", t); }

		// Update database with items in its warehouse and remove them from the world
		try { clearWarehouse(); } catch (Throwable t) {_log.log(Level.SEVERE, "deleteMe()", t); }
		if(Config.WAREHOUSE_CACHE)
			WarehouseCacheManager.getInstance().remCacheTask(this);

		// Update database with items in its freight and remove them from the world
		try { getFreight().deleteMe(); } catch (Throwable t) {_log.log(Level.SEVERE, "deleteMe()", t); }

		// Remove all L2Object from _knownObjects and _knownPlayer of the L2Character then cancel Attak or Cast and notify AI
		try { getKnownList().removeAllKnownObjects(); } catch (Throwable t) {_log.log(Level.SEVERE, "deleteMe()", t); }

		// Close the connection with the client
		closeNetConnection();

		// remove from flood protector
		FloodProtector.getInstance().removePlayer(getObjectId());

		if (getClanId() > 0)
			getClan().broadcastToOtherOnlineMembers(new PledgeShowMemberListUpdate(this), this);
			//ClanTable.getInstance().getClan(getClanId()).broadcastToOnlineMembers(new PledgeShowMemberListAdd(this));

		for(L2PcInstance player : _snoopedPlayer)
			player.removeSnooper(this);

		for(L2PcInstance player : _snoopListener)
			player.removeSnooped(this);

		if (_chanceSkills != null)
		{
			_chanceSkills.setOwner(null);
			_chanceSkills = null;
		}

		// Remove L2Object object from _allObjects of L2World
		L2World.getInstance().removeObject(this);
		L2World.getInstance().removeFromAllPlayers(this); // force remove in case of crash during teleport
	}

	private FishData _fish;

   /*  startFishing() was stripped of any pre-fishing related checks, namely the fishing zone check.
    * Also worthy of note is the fact the code to find the hook landing position was also striped. The
    * stripped code was moved into fishing.java. In my opinion it makes more sense for it to be there
    * since all other skill related checks were also there. Last but not least, moving the zone check
    * there, fixed a bug where baits would always be consumed no matter if fishing actualy took place.
    * startFishing() now takes up 3 arguments, wich are acurately described as being the hook landing 
    * coordinates.
    */
	public void startFishing(int _x, int _y, int _z)
    {
        stopMove(null);
        setIsImmobilized(true);
        _fishing = true;
        _fishx = _x;
        _fishy = _y;
        _fishz = _z;
        broadcastUserInfo();
        //Starts fishing
    	int lvl = GetRandomFishLvl();
		int group = GetRandomGroup();
		int type = GetRandomFishType(group);
		List<FishData> fishs = FishTable.getInstance().getfish(lvl, type, group);
		if (fishs == null || fishs.size() == 0)
		{
			sendMessage("Error - Fishes are not definied");
			endFishing(false);
			return;
		}
		int check = Rnd.get(fishs.size());
		// Use a copy constructor else the fish data may be over-written below
		_fish = new FishData(fishs.get(check));
		fishs.clear();
		fishs = null;
        sendPacket(new SystemMessage(SystemMessageId.CAST_LINE_AND_START_FISHING));
        ExFishingStart efs = null;
        if (!GameTimeController.getInstance().isNowNight() && _lure.isNightLure())
        	_fish.setType(-1);
		//sendMessage("Hook x,y: " + _x + "," + _y + " - Water Z, Player Z:" + _z + ", " + getZ()); //debug line, uncoment to show coordinates used in fishing.
    	efs = new ExFishingStart(this,_fish.getType(),_x,_y,_z,_lure.isNightLure());
        broadcastPacket(efs);
        startLookingForFishTask();
    }
    public void stopLookingForFishTask()
    {
        if (_taskforfish != null)
        {
            _taskforfish.cancel(false);
            _taskforfish = null;
        }
    }
    public void startLookingForFishTask()
    {
        if (!isDead() && _taskforfish == null)
        {
            int checkDelay = 0;
            boolean isNoob = false;
            boolean isUpperGrade = false;

            if (_lure != null)
            {
        		int lureid = _lure.getItemId();
        		isNoob = _fish.getGroup() == 0;
        		isUpperGrade = _fish.getGroup() == 2;
                if (lureid == 6519 || lureid == 6522 || lureid == 6525 || lureid == 8505 || lureid == 8508 || lureid == 8511) //low grade
                	checkDelay = Math.round((float)(_fish.getGutsCheckTime() * (1.33)));
                else if (lureid == 6520 || lureid == 6523 || lureid == 6526 || (lureid >= 8505 && lureid <= 8513) || (lureid >= 7610 && lureid <= 7613) || (lureid >= 7807 && lureid <= 7809) || (lureid >= 8484 && lureid <= 8486)) //medium grade, beginner, prize-winning & quest special bait
                	checkDelay = Math.round((float)(_fish.getGutsCheckTime() * (1.00)));
                else if (lureid == 6521 || lureid == 6524 || lureid == 6527 || lureid == 8507 || lureid == 8510 || lureid == 8513) //high grade
                	checkDelay = Math.round((float)(_fish.getGutsCheckTime() * (0.66)));
            }
            _taskforfish = ThreadPoolManager.getInstance().scheduleEffectAtFixedRate(new LookingForFishTask(_fish.getWaitTime(), _fish.getFishGuts(), _fish.getType(), isNoob, isUpperGrade), 10000, checkDelay);
        }
    }

	private int GetRandomGroup()
	{
		switch (_lure.getItemId()) {
			case 7807: //green for beginners
			case 7808: //purple for beginners
			case 7809: //yellow for beginners
			case 8486: //prize-winning for beginners
				return 0;
			case 8485: //prize-winning luminous
			case 8506: //green luminous
			case 8509: //purple luminous
			case 8512: //yellow luminous
				return 2;
			default:
				return 1;
		}
	}
	private int GetRandomFishType(int group)
	{
		int check = Rnd.get(100);
		int type = 1;
		switch (group) {
			case 0:	//fish for novices
				switch (_lure.getItemId()) {
					case 7807: //green lure, preferred by fast-moving (nimble) fish (type 5)
						if (check <= 54)
							type = 5;
						else if (check <= 77)
							type = 4;
						else
							type = 6;
						break;
					case 7808: //purple lure, preferred by fat fish (type 4)
						if (check <= 54)
							type = 4;
						else if (check <= 77)
							type = 6;
						else
							type = 5;
						break;
					case 7809: //yellow lure, preferred by ugly fish (type 6)
						if (check <= 54)
							type = 6;
						else if (check <= 77)
							type = 5;
						else
							type = 4;
						break;
					case 8486:	//prize-winning fishing lure for beginners
						if (check <= 33)
							type = 4;
						else if (check <= 66)
							type = 5;
						else
							type = 6;
						break;
				}
				break;
			case 1:	//normal fish
				switch (_lure.getItemId()) {
					case 7610:
					case 7611:
					case 7612:
					case 7613:
						type = 3;
						break;
					case 6519:  //all theese lures (green) are prefered by fast-moving (nimble) fish (type 1)
					case 8505:
					case 6520:
					case 6521:
					case 8507:
						if (check <= 54)
							type = 1;
						else if (check <= 74)
							type = 0;
						else if (check <= 94)
							type = 2;
						else
							type = 3;
						break;
					case 6522:	 //all theese lures (purple) are prefered by fat fish (type 0)
					case 8508:
					case 6523:
					case 6524:
					case 8510:
						if (check <= 54)
							type = 0;
						else if (check <= 74)
							type = 1;
						else if (check <= 94)
							type = 2;
						else
							type = 3;
						break;
					case 6525:	//all theese lures (yellow) are prefered by ugly fish (type 2)
					case 8511:
					case 6526:
					case 6527:
					case 8513:
						if (check <= 55)
							type = 2;
						else if (check <= 74)
							type = 1;
						else if (check <= 94)
							type = 0;
						else
							type = 3;
						break;
					case 8484:	//prize-winning fishing lure
						if (check <= 33)
							type = 0;
						else if (check <= 66)
							type = 1;
						else
							type = 2;
						break;
				}
				break;
			case 2:	//upper grade fish, luminous lure
				switch (_lure.getItemId()) {
					case 8506: //green lure, preferred by fast-moving (nimble) fish (type 8)
						if (check <= 54)
							type = 8;
						else if (check <= 77)
							type = 7;
						else
							type = 9;
						break;
					case 8509: //purple lure, preferred by fat fish (type 7)
						if (check <= 54)
							type = 7;
						else if (check <= 77)
							type = 9;
						else
							type = 8;
						break;
					case 8512: //yellow lure, preferred by ugly fish (type 9)
						if (check <= 54)
							type = 9;
						else if (check <= 77)
							type = 8;
						else
							type = 7;
						break;
					case 8485: //prize-winning fishing lure
						if (check <= 33)
							type = 7;
						else if (check <= 66)
							type = 8;
						else
							type = 9;
						break;
				}
		}
		return type;
	}
	private int GetRandomFishLvl()
	{
		L2Effect[] effects = getAllEffects();
		int skilllvl = getSkillLevel(1315);
		for (L2Effect e : effects) {
			if (e.getSkill().getId() == 2274)
				skilllvl = (int)e.getSkill().getPower(this);
		}
		if (skilllvl <= 0) return 1;
		int randomlvl;
		int check = Rnd.get(100);

		if (check <= 50)
		{
			randomlvl = skilllvl;
		}
		else if (check <= 85)
		{
			randomlvl = skilllvl - 1;
			if (randomlvl <= 0)
			{
				randomlvl = 1;
			}
		}
		else
		{
			randomlvl = skilllvl + 1;
			if (randomlvl > 27) randomlvl = 27;
		}

		return randomlvl;
	}
	public void startFishCombat(boolean isNoob, boolean isUpperGrade)
    {
        _fishCombat = new L2Fishing (this, _fish, isNoob, isUpperGrade);
    }
    
	public void endFishing(boolean win)
    {
        ExFishingEnd efe = new ExFishingEnd(win, this);
        broadcastPacket(efe);
        _fishing = false;
        _fishx = 0;
        _fishy = 0;
        _fishz = 0;
        broadcastUserInfo();
        if (_fishCombat == null)
            sendPacket(new SystemMessage(SystemMessageId.BAIT_LOST_FISH_GOT_AWAY));
        _fishCombat = null;
        _lure = null;
        //Ends fishing
        sendPacket(new SystemMessage(SystemMessageId.REEL_LINE_AND_STOP_FISHING));
        setIsImmobilized(false);
        stopLookingForFishTask();
    }

    public L2Fishing getFishCombat()
    {
        return _fishCombat;
    }

    public int getFishx()
    {
        return _fishx;
    }

    public int getFishy()
    {
        return _fishy;
    }

    public int getFishz()
    {
        return _fishz;
    }

    public void setLure (L2ItemInstance lure)
    {
        _lure = lure;
    }

 	public L2ItemInstance getLure()
	{
 		return _lure;
	}

	public int getInventoryLimit()
	{
		int ivlim;
		if (isGM())
			ivlim = Config.INVENTORY_MAXIMUM_GM;
		else if (getRace() == Race.Dwarf)
			ivlim = Config.INVENTORY_MAXIMUM_DWARF;
		else
        	ivlim = Config.INVENTORY_MAXIMUM_NO_DWARF;
		ivlim += (int)getStat().calcStat(Stats.INV_LIM, 0, null, null);

		return ivlim;
	}

	public int getWareHouseLimit()
	{
		int whlim;
		if (getRace() == Race.Dwarf)
               whlim = Config.WAREHOUSE_SLOTS_DWARF;
		else
			whlim = Config.WAREHOUSE_SLOTS_NO_DWARF;

		whlim += (int)getStat().calcStat(Stats.WH_LIM, 0, null, null);

		return whlim;
	}
	
	public int getPrivateSellStoreLimit()
	{
		int pslim;
	
		if (getRace() == Race.Dwarf)
			pslim = Config.MAX_PVTSTORESELL_SLOTS_DWARF;
        else
        	pslim = Config.MAX_PVTSTORESELL_SLOTS_OTHER;

		pslim += (int)getStat().calcStat(Stats.P_SELL_LIM, 0, null, null);

		return pslim;
	}

	public int getPrivateBuyStoreLimit()
	{
		int pblim;
		
		if (getRace() == Race.Dwarf)
			pblim = Config.MAX_PVTSTOREBUY_SLOTS_DWARF;
		else
			pblim = Config.MAX_PVTSTOREBUY_SLOTS_OTHER;
		pblim += (int)getStat().calcStat(Stats.P_BUY_LIM, 0, null, null);

		return pblim;
	}
	
	public int getFreightLimit()
	{
		return Config.FREIGHT_SLOTS + (int)getStat().calcStat(Stats.FREIGHT_LIM, 0, null, null);
	}

    public int getDwarfRecipeLimit()
    {
        int recdlim = Config.DWARF_RECIPE_LIMIT;
        recdlim += (int)getStat().calcStat(Stats.REC_D_LIM, 0, null, null);
        return recdlim;
    }

    public int getCommonRecipeLimit()
    {
        int recclim = Config.COMMON_RECIPE_LIMIT;
        recclim += (int)getStat().calcStat(Stats.REC_C_LIM, 0, null, null);
        return recclim;
    }

    /**
     * @return Returns the mountNpcId.
     */
    public int getMountNpcId()
    {
        return _mountNpcId;
    }

    public void setMountObjectID(int newID)
	{
	    _mountObjectID = newID;
	}

	public int getMountObjectID()
	{
	    return _mountObjectID;
	}

    private L2ItemInstance _lure = null;

    /**
     * Get the current skill in use or return null.<BR><BR>
     *
     */
    public SkillDat getCurrentSkill()
    {
        return _currentSkill;
    }


    /**
     * Create a new SkillDat object and set the player _currentSkill.<BR><BR>
     *
     */
    public void setCurrentSkill(L2Skill currentSkill, boolean ctrlPressed,
                                boolean shiftPressed)
    {
        if (currentSkill == null)
        {
            if (Config.DEBUG)
                _log.info("Setting current skill: NULL for " + getName() + ".");

            _currentSkill = null;
            return;
        }

        if (Config.DEBUG)
            _log.info("Setting current skill: " + currentSkill.getName() + " (ID: " + currentSkill.getId() + ") for " + getName() + ".");

        _currentSkill = new SkillDat(currentSkill, ctrlPressed, shiftPressed);
    }

    public SkillDat getQueuedSkill()
    {
        return _queuedSkill;
    }


    /**
     * Create a new SkillDat object and queue it in the player _queuedSkill.<BR><BR>
     *
     */
    public void setQueuedSkill(L2Skill queuedSkill, boolean ctrlPressed, boolean shiftPressed)
    {
        if (queuedSkill == null)
        {
            if (Config.DEBUG)
                _log.info("Setting queued skill: NULL for " + getName() + ".");

            _queuedSkill = null;
            return;
        }

        if (Config.DEBUG)
            _log.info("Setting queued skill: " + queuedSkill.getName() + " (ID: " + queuedSkill.getId() + ") for " + getName() + ".");

        _queuedSkill = new SkillDat(queuedSkill, ctrlPressed, shiftPressed);
    }

    public boolean isInJail()
    {
        return _inJail;
    }

    public void setInJail(boolean state)
    {
        _inJail = state;
    }

    public void setInJail(boolean state, int delayInMinutes)
    {
        _inJail = state;
        _jailTimer = 0;
        // Remove the task if any
        stopJailTask(false);

        if (_inJail)
        {
            if (delayInMinutes > 0)
            {
                _jailTimer = delayInMinutes * 60000L; // in millisec

                // start the countdown
                _jailTask = ThreadPoolManager.getInstance().scheduleGeneral(new JailTask(this), _jailTimer);
                sendMessage("You are in jail for "+delayInMinutes+" minutes.");
            }

            // Open a Html message to inform the player
            NpcHtmlMessage htmlMsg = new NpcHtmlMessage(0);
            String jailInfos = HtmCache.getInstance().getHtm("data/html/jail_in.htm");
            if (jailInfos != null)
                htmlMsg.setHtml(jailInfos);
            else
                htmlMsg.setHtml("<html><body>You have been put in jail by an admin.</body></html>");
            sendPacket(htmlMsg);

            teleToLocation(-114356, -249645, -2984, false);  // Jail
        } else
        {
            // Open a Html message to inform the player
            NpcHtmlMessage htmlMsg = new NpcHtmlMessage(0);
            String jailInfos = HtmCache.getInstance().getHtm("data/html/jail_out.htm");
            if (jailInfos != null)
                htmlMsg.setHtml(jailInfos);
            else
                htmlMsg.setHtml("<html><body>You are free for now, respect server rules!</body></html>");
            sendPacket(htmlMsg);

            teleToLocation(17836, 170178, -3507, true);  // Floran
        }

        // store in database
        storeCharBase();
    }

    public long getJailTimer()
    {
        return _jailTimer;
    }

    public void setJailTimer(long time)
    {
        _jailTimer = time;
    }

    private void updateJailState()
    {
        if (isInJail())
        {
            // If jail time is elapsed, free the player
            if (_jailTimer > 0)
            {
                // restart the countdown
                _jailTask = ThreadPoolManager.getInstance().scheduleGeneral(new JailTask(this), _jailTimer);
                sendMessage("You are still in jail for "+Math.round(_jailTimer/60000)+" minutes.");
            }

            // If player escaped, put him back in jail
            if (!isInsideZone(ZONE_JAIL))
                teleToLocation(-114356,-249645,-2984, true);
        }
    }

    public void stopJailTask(boolean save)
    {
        if (_jailTask != null)
        {
            if (save)
            {
            	long delay = _jailTask.getDelay(TimeUnit.MILLISECONDS);
            	if (delay < 0)
            		delay = 0;
            	setJailTimer(delay);
            }
            _jailTask.cancel(false);
            _jailTask = null;
        }
    }

    private class JailTask implements Runnable
    {
        L2PcInstance _player;
        protected long _startedAt;

        protected JailTask(L2PcInstance player)
        {
            _player = player;
            _startedAt = System.currentTimeMillis();
        }

        public void run()
        {
            _player.setInJail(false, 0);
        }
    }

	/**
	 * @return
	 */
	public int getPowerGrade()
	{
		return _powerGrade;
	}

	/**
	 * @return
	 */
	public void setPowerGrade(int power)
	{
		_powerGrade = power;
	}

	public boolean isCursedWeaponEquipped()
	{
		return _cursedWeaponEquippedId != 0;
	}

	public void setCursedWeaponEquippedId(int value)
	{
		_cursedWeaponEquippedId = value;
	}

	public int getCursedWeaponEquippedId()
	{
		return _cursedWeaponEquippedId;
	}
	
	public boolean isCombatFlagEquipped()
    {
        return _combatFlagEquippedId ;
    }

    public void setCombatFlagEquipped(boolean value)
    {
        _combatFlagEquippedId = value;
    }

	private boolean _charmOfCourage = false;

	public boolean getCharmOfCourage()
	{
		return _charmOfCourage;
	}

	public void setCharmOfCourage(boolean val) 
	{
		_charmOfCourage = val;
		sendPacket(new EtcStatusUpdate(this));
	}

    /**
     * Returns the Number of Souls this L2PcInstance got.
     * @return
     */
    public int getSouls()
    {
        return _souls;
    }

    /**
     * Absorbs a Soul from a Npc.
     * @param skill
     * @param target
     */
    public void absorbSoul(L2Skill skill, L2NpcInstance npc)
    {
        if (_souls >= skill.getNumSouls())
        {
            SystemMessage sm = new SystemMessage(SystemMessageId.SOUL_CANNOT_BE_INCREASED_ANYMORE);
            sendPacket(sm);
            return;
        }

        increaseSouls(1);

        if (npc != null)
        	broadcastPacket(new ExSpawnEmitter(this, npc), 500);
    }
    
    /**
     * Increase Souls
     * @param count
     */
    public void increaseSouls(int count)
    {
        if (count < 0 || count > 45)
            return;

        _souls += count;

        if (getSouls() > 45)
            _souls = 45;

        SystemMessage sm = new SystemMessage(SystemMessageId.YOUR_SOUL_HAS_INCREASED_BY_S1_SO_IT_IS_NOW_AT_S2);
        sm.addNumber(count);
        sm.addNumber(_souls);
        sendPacket(sm);

        restartSoulTask();

        sendPacket(new EtcStatusUpdate(this));
    }

    /**
     * Decreases existing Souls.
     * @param count
     */
    public void decreaseSouls(int count)
    {
        if (getSouls() <= 0)
            return;

        _souls -= count;

        if (getSouls() < 0)
            _souls = 0;

        if (getSouls() == 0)
            stopSoulTask();
        else
            restartSoulTask();

        sendPacket(new EtcStatusUpdate(this));
    }

    /**
     * Clear out all Souls from this L2PcInstance
     */
    public void clearSouls()
    {
        _souls = 0;
        stopSoulTask();
        sendPacket(new EtcStatusUpdate(this));
    }

    /**
     * Starts/Restarts the SoulTask to Clear Souls after 10 Mins.
     */
    private void restartSoulTask()
    {
        if (_soulTask != null)
        {
            _soulTask.cancel(false);
            _soulTask = null;
        }
        _soulTask = ThreadPoolManager.getInstance().scheduleGeneral(new SoulTask(this), 600000);
    }

    /**
     * Stops the Clearing Task.
     */
    public void stopSoulTask()
    {
        if (_soulTask != null)
        {
            _soulTask.cancel(false);
            _soulTask = null;
        }
    }

    private class SoulTask implements Runnable
    {
        L2PcInstance _player;
        
        protected SoulTask(L2PcInstance player)
        {
            _player = player;
        }
        
        public void run()
        {
            _player.clearSouls();
        }
    }
    
    /**
     *
     * @param magicId
     * @param level
     * @param time
     */
    public void shortBuffStatusUpdate(int magicId, int level, int time)
	{
		if (_shortBuffTask != null)
		{
			_shortBuffTask.cancel(false);
			_shortBuffTask = null;
		}
		_shortBuffTask = ThreadPoolManager.getInstance().scheduleGeneral(new ShortBuffTask(this), 15000);
		
		sendPacket(new ShortBuffStatusUpdate(magicId, level, time));
	}

    public int getDeathPenaltyBuffLevel()
    {
    	  return _deathPenaltyBuffLevel;
    }

    public void setDeathPenaltyBuffLevel(int level)
    {
    	 _deathPenaltyBuffLevel = level;
    }

    public void calculateDeathPenaltyBuffLevel(L2Character killer)
    {
    	if(Rnd.get(100) <= Config.DEATH_PENALTY_CHANCE 
    			&& !(killer instanceof L2PcInstance) && !(this.isGM())
    			&& !(this.getCharmOfLuck() && (killer instanceof L2GrandBossInstance || killer instanceof L2RaidBossInstance))) 
    		
    		increaseDeathPenaltyBuffLevel();
	}

    public void increaseDeathPenaltyBuffLevel()
    {
    	if(getDeathPenaltyBuffLevel() >= 15) //maximum level reached
    		return;
    	
    	if(getDeathPenaltyBuffLevel() != 0)
    	{
    		L2Skill skill = SkillTable.getInstance().getInfo(5076, getDeathPenaltyBuffLevel());

    		if(skill != null)
    			removeSkill(skill, true);
    	}

    	_deathPenaltyBuffLevel++;

    	addSkill(SkillTable.getInstance().getInfo(5076, getDeathPenaltyBuffLevel()), false);
    	sendPacket(new EtcStatusUpdate(this));
    	SystemMessage sm = new SystemMessage(SystemMessageId.DEATH_PENALTY_LEVEL_S1_ADDED);
    	sm.addNumber(getDeathPenaltyBuffLevel());
    	sendPacket(sm);
    }

    public void reduceDeathPenaltyBuffLevel()
    {
    	if(getDeathPenaltyBuffLevel() <= 0)
    		return;

    	L2Skill skill = SkillTable.getInstance().getInfo(5076, getDeathPenaltyBuffLevel());

    	if(skill != null)
    		removeSkill(skill, true);

    	_deathPenaltyBuffLevel--;

    	if(getDeathPenaltyBuffLevel() > 0)
    	{
    		addSkill(SkillTable.getInstance().getInfo(5076, getDeathPenaltyBuffLevel()), false);
    		sendPacket(new EtcStatusUpdate(this));
    		SystemMessage sm = new SystemMessage(SystemMessageId.DEATH_PENALTY_LEVEL_S1_ADDED);
    		sm.addNumber(getDeathPenaltyBuffLevel());
    		sendPacket(sm);
    	}
    	else
    	{
    		sendPacket(new EtcStatusUpdate(this));
    		sendPacket(new SystemMessage(SystemMessageId.DEATH_PENALTY_LIFTED));
    	}
    }

    public void restoreDeathPenaltyBuffLevel()
    {
    	L2Skill skill = SkillTable.getInstance().getInfo(5076, getDeathPenaltyBuffLevel());

    	if(skill != null)
    		removeSkill(skill, true);

    	if(getDeathPenaltyBuffLevel() > 0)
    	{
    		addSkill(SkillTable.getInstance().getInfo(5076, getDeathPenaltyBuffLevel()), false);
    	}
    }

	private FastMap<Integer, TimeStamp> _reuseTimeStamps = new FastMap<Integer, TimeStamp>().setShared(true);

    public Collection<TimeStamp> getReuseTimeStamps()
    {
        return _reuseTimeStamps.values();
    }
    
    public FastMap<Integer, TimeStamp> getReuseTimeStamp()
    {
    	return _reuseTimeStamps;
    }
    
	/**
	 * Simple class containing all neccessary information to maintain
	 * valid timestamps and reuse for skills upon relog. Filter this
	 * carefully as it becomes redundant to store reuse for small delays.
	 * @author  Yesod
	 */
	public static class TimeStamp
	{
		private int skill;
		private long reuse;
		private long stamp;

		public TimeStamp(int _skill, long _reuse)
		{
			skill = _skill;
			reuse = _reuse;
			stamp = System.currentTimeMillis()+ reuse;
		}

		public int getSkill()
        {
            return skill;
        }
        
        public long getReuse()
        {
            return reuse;
        }
        
        public long getRemaining()
        {
            return Math.max(stamp - System.currentTimeMillis(), 0);
        }

		/* Check if the reuse delay has passed and
		 * if it has not then update the stored reuse time
		 * according to what is currently remaining on
		 * the delay. */
		public boolean hasNotPassed()
		{
            if (System.currentTimeMillis() < stamp)
            {
                return true;
            }
			return false;
		}
	}

	/**
	 * Index according to skill id the current
	 * timestamp of use.
	 * @param skillid
	 * @param reuse delay
	 */
	@Override
	public void addTimeStamp(int s, int r)
	{
		_reuseTimeStamps.put(s, new TimeStamp(s, r));
	}

	/**
	 * Index according to skill this TimeStamp
	 * instance for restoration purposes only.
	 * @param TimeStamp
	 */
	public void addTimeStamp(TimeStamp ts)
	{
		_reuseTimeStamps.put(ts.getSkill(), ts);
	}

	/**
	 * Index according to skill id the current
	 * timestamp of use.
	 * @param skillid
	 */
	@Override
	public void removeTimeStamp(int s)
	{
		_reuseTimeStamps.remove(s);
	}

	@Override
	public final void sendDamageMessage(L2Character target, int damage, boolean mcrit, boolean pcrit, boolean miss)
    {
		// Check if hit is missed
		if (miss)
		{
			sendPacket(new SystemMessage(SystemMessageId.S1_ATTACK_WENT_ASTRAY).addString(getName()));
			return;
		}

		// Check if hit is critical
		if (pcrit)
        {
			sendPacket(new SystemMessage(SystemMessageId.S1_HAD_CRITICAL_HIT).addPcName(this));
            if (getSkillLevel(467) > 0 && target instanceof L2NpcInstance)
            {
                L2Skill skill = SkillTable.getInstance().getInfo(467,getSkillLevel(467));
                if (Rnd.get(100) < skill.getCritChance())
                {
                    absorbSoul(skill,((L2NpcInstance)target));
                }
            }
        }
        if (mcrit)
            sendPacket(new SystemMessage(SystemMessageId.CRITICAL_HIT_MAGIC));

        if (isInOlympiadMode() &&
        		target instanceof L2PcInstance &&
        		((L2PcInstance)target).isInOlympiadMode() &&
        		((L2PcInstance)target).getOlympiadGameId() == getOlympiadGameId())
        {
        	Olympiad.getInstance().notifyCompetitorDamage(getObjectId(), damage, getOlympiadGameId());
        }

		SystemMessage sm = new SystemMessage(SystemMessageId.S1_GAVE_S2_DAMAGE_OF_S3);
		sm.addPcName(this);
		sm.addCharName(target);
		sm.addNumber(damage);
		sendPacket(sm);
	}

    @Override
    public boolean mustFallDownOnDeath()
    {
        return	super.mustFallDownOnDeath() &&
        		( !TvTEvent.isStarted() || !TvTEvent.isPlayerParticipant( getObjectId() ) );
    }

    public void setAgathionId(int npcId)
    {
    	_agathionId = npcId;
    }
    public int getAgathionId()
    {
	    return _agathionId;
    }
}
