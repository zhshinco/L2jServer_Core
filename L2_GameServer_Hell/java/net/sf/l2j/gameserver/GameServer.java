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

package net.sf.l2j.gameserver;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.Server;
import net.sf.l2j.gameserver.cache.CrestCache;
import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.communitybbs.Manager.ForumsBBSManager;
import net.sf.l2j.gameserver.datatables.ArmorSetsTable;
import net.sf.l2j.gameserver.datatables.AugmentationData;
import net.sf.l2j.gameserver.datatables.CharNameTable;
import net.sf.l2j.gameserver.datatables.CharTemplateTable;
import net.sf.l2j.gameserver.datatables.ClanTable;
import net.sf.l2j.gameserver.datatables.DoorTable;
import net.sf.l2j.gameserver.datatables.EventDroplist;
import net.sf.l2j.gameserver.datatables.ExtractableItemsData;
import net.sf.l2j.gameserver.datatables.FishTable;
import net.sf.l2j.gameserver.datatables.HelperBuffTable;
import net.sf.l2j.gameserver.datatables.HennaTable;
import net.sf.l2j.gameserver.datatables.HennaTreeTable;
import net.sf.l2j.gameserver.datatables.HeroSkillTable;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.datatables.LevelUpData;
import net.sf.l2j.gameserver.datatables.MapRegionTable;
import net.sf.l2j.gameserver.datatables.MerchantPriceConfigTable;
import net.sf.l2j.gameserver.datatables.NobleSkillTable;
import net.sf.l2j.gameserver.datatables.NpcBufferTable;
import net.sf.l2j.gameserver.datatables.NpcTable;
import net.sf.l2j.gameserver.datatables.NpcWalkerRoutesTable;
import net.sf.l2j.gameserver.datatables.SkillSpellbookTable;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.datatables.SkillTreeTable;
import net.sf.l2j.gameserver.datatables.SpawnTable;
import net.sf.l2j.gameserver.datatables.StaticObjects;
import net.sf.l2j.gameserver.datatables.SummonItemsData;
import net.sf.l2j.gameserver.datatables.TeleportLocationTable;
import net.sf.l2j.gameserver.datatables.ZoneData;
import net.sf.l2j.gameserver.geoeditorcon.GeoEditorListener;
import net.sf.l2j.gameserver.handler.AdminCommandHandler;
import net.sf.l2j.gameserver.handler.ItemHandler;
import net.sf.l2j.gameserver.handler.SkillHandler;
import net.sf.l2j.gameserver.handler.UserCommandHandler;
import net.sf.l2j.gameserver.handler.VoicedCommandHandler;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminAdmin;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminAnnouncements;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminBBS;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminBan;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminBanChat;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminCache;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminChangeAccessLevel;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminCreateItem;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminCursedWeapons;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminDelete;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminDoorControl;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminEditChar;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminEditNpc;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminEffects;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminEnchant;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminEventEngine;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminExpSp;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminFightCalculator;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminFortSiege;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminGeoEditor;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminGeodata;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminGm;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminGmChat;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminHeal;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminHelpPage;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminInvul;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminKick;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminKill;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminLevel;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminLogin;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminMammon;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminManor;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminMenu;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminMobGroup;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminMonsterRace;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminPForge;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminPathNode;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminPetition;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminPledge;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminPolymorph;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminQuest;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminRepairChar;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminRes;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminRide;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminShop;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminShutdown;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminSiege;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminSkill;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminSpawn;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminTarget;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminTeleport;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminTest;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminTvTEvent;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminUnblockIp;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminZone;
import net.sf.l2j.gameserver.handler.itemhandlers.BeastSoulShot;
import net.sf.l2j.gameserver.handler.itemhandlers.BeastSpice;
import net.sf.l2j.gameserver.handler.itemhandlers.BeastSpiritShot;
import net.sf.l2j.gameserver.handler.itemhandlers.BlessedSpiritShot;
import net.sf.l2j.gameserver.handler.itemhandlers.Book;
import net.sf.l2j.gameserver.handler.itemhandlers.CharChangePotions;
import net.sf.l2j.gameserver.handler.itemhandlers.CrystalCarol;
import net.sf.l2j.gameserver.handler.itemhandlers.EnchantScrolls;
import net.sf.l2j.gameserver.handler.itemhandlers.EnergyStone;
import net.sf.l2j.gameserver.handler.itemhandlers.ExtractableItems;
import net.sf.l2j.gameserver.handler.itemhandlers.Firework;
import net.sf.l2j.gameserver.handler.itemhandlers.FishShots;
import net.sf.l2j.gameserver.handler.itemhandlers.Harvester;
import net.sf.l2j.gameserver.handler.itemhandlers.Key;
import net.sf.l2j.gameserver.handler.itemhandlers.Maps;
import net.sf.l2j.gameserver.handler.itemhandlers.MercTicket;
import net.sf.l2j.gameserver.handler.itemhandlers.MysteryPotion;
import net.sf.l2j.gameserver.handler.itemhandlers.PaganKeys;
import net.sf.l2j.gameserver.handler.itemhandlers.Potions;
import net.sf.l2j.gameserver.handler.itemhandlers.Recipes;
import net.sf.l2j.gameserver.handler.itemhandlers.Remedy;
import net.sf.l2j.gameserver.handler.itemhandlers.RollingDice;
import net.sf.l2j.gameserver.handler.itemhandlers.ScrollOfEscape;
import net.sf.l2j.gameserver.handler.itemhandlers.ScrollOfResurrection;
import net.sf.l2j.gameserver.handler.itemhandlers.Scrolls;
import net.sf.l2j.gameserver.handler.itemhandlers.Seed;
import net.sf.l2j.gameserver.handler.itemhandlers.SevenSignsRecord;
import net.sf.l2j.gameserver.handler.itemhandlers.SoulCrystals;
import net.sf.l2j.gameserver.handler.itemhandlers.SoulShots;
import net.sf.l2j.gameserver.handler.itemhandlers.SpecialXMas;
import net.sf.l2j.gameserver.handler.itemhandlers.SpiritShot;
import net.sf.l2j.gameserver.handler.itemhandlers.SummonItems;
import net.sf.l2j.gameserver.handler.skillhandlers.BalanceLife;
import net.sf.l2j.gameserver.handler.skillhandlers.BeastFeed;
import net.sf.l2j.gameserver.handler.skillhandlers.Blow;
import net.sf.l2j.gameserver.handler.skillhandlers.Charge;
import net.sf.l2j.gameserver.handler.skillhandlers.CombatPointHeal;
import net.sf.l2j.gameserver.handler.skillhandlers.Continuous;
import net.sf.l2j.gameserver.handler.skillhandlers.CpDam;
import net.sf.l2j.gameserver.handler.skillhandlers.Craft;
import net.sf.l2j.gameserver.handler.skillhandlers.DeluxeKey;
import net.sf.l2j.gameserver.handler.skillhandlers.Disablers;
import net.sf.l2j.gameserver.handler.skillhandlers.DrainSoul;
import net.sf.l2j.gameserver.handler.skillhandlers.Fishing;
import net.sf.l2j.gameserver.handler.skillhandlers.FishingSkill;
import net.sf.l2j.gameserver.handler.skillhandlers.GetPlayer;
import net.sf.l2j.gameserver.handler.skillhandlers.Harvest;
import net.sf.l2j.gameserver.handler.skillhandlers.Heal;
import net.sf.l2j.gameserver.handler.skillhandlers.ManaHeal;
import net.sf.l2j.gameserver.handler.skillhandlers.Manadam;
import net.sf.l2j.gameserver.handler.skillhandlers.Mdam;
import net.sf.l2j.gameserver.handler.skillhandlers.Pdam;
import net.sf.l2j.gameserver.handler.skillhandlers.Recall;
import net.sf.l2j.gameserver.handler.skillhandlers.Resurrect;
import net.sf.l2j.gameserver.handler.skillhandlers.SiegeFlag;
import net.sf.l2j.gameserver.handler.skillhandlers.Soul;
import net.sf.l2j.gameserver.handler.skillhandlers.Sow;
import net.sf.l2j.gameserver.handler.skillhandlers.Spoil;
import net.sf.l2j.gameserver.handler.skillhandlers.StrSiegeAssault;
import net.sf.l2j.gameserver.handler.skillhandlers.SummonFriend;
import net.sf.l2j.gameserver.handler.skillhandlers.SummonTreasureKey;
import net.sf.l2j.gameserver.handler.skillhandlers.Sweep;
import net.sf.l2j.gameserver.handler.skillhandlers.TakeCastle;
import net.sf.l2j.gameserver.handler.skillhandlers.TakeFort;
import net.sf.l2j.gameserver.handler.skillhandlers.TransformDispel;
import net.sf.l2j.gameserver.handler.skillhandlers.Trap;
import net.sf.l2j.gameserver.handler.skillhandlers.Unlock;
import net.sf.l2j.gameserver.handler.usercommandhandlers.ChannelDelete;
import net.sf.l2j.gameserver.handler.usercommandhandlers.ChannelLeave;
import net.sf.l2j.gameserver.handler.usercommandhandlers.ChannelListUpdate;
import net.sf.l2j.gameserver.handler.usercommandhandlers.ClanPenalty;
import net.sf.l2j.gameserver.handler.usercommandhandlers.ClanWarsList;
import net.sf.l2j.gameserver.handler.usercommandhandlers.DisMount;
import net.sf.l2j.gameserver.handler.usercommandhandlers.Escape;
import net.sf.l2j.gameserver.handler.usercommandhandlers.InstanceZone;
import net.sf.l2j.gameserver.handler.usercommandhandlers.Loc;
import net.sf.l2j.gameserver.handler.usercommandhandlers.Mount;
import net.sf.l2j.gameserver.handler.usercommandhandlers.OlympiadStat;
import net.sf.l2j.gameserver.handler.usercommandhandlers.PartyInfo;
import net.sf.l2j.gameserver.handler.usercommandhandlers.Time;
import net.sf.l2j.gameserver.handler.voicedcommandhandlers.Banking;
import net.sf.l2j.gameserver.handler.voicedcommandhandlers.Wedding;
import net.sf.l2j.gameserver.handler.voicedcommandhandlers.stats;
import net.sf.l2j.gameserver.idfactory.IdFactory;
import net.sf.l2j.gameserver.instancemanager.AuctionManager;
import net.sf.l2j.gameserver.instancemanager.BoatManager;
import net.sf.l2j.gameserver.instancemanager.CastleManager;
import net.sf.l2j.gameserver.instancemanager.CastleManorManager;
import net.sf.l2j.gameserver.instancemanager.ClanHallManager;
import net.sf.l2j.gameserver.instancemanager.CoupleManager;
import net.sf.l2j.gameserver.instancemanager.CursedWeaponsManager;
import net.sf.l2j.gameserver.instancemanager.DayNightSpawnManager;
import net.sf.l2j.gameserver.instancemanager.DimensionalRiftManager;
import net.sf.l2j.gameserver.instancemanager.FortManager;
import net.sf.l2j.gameserver.instancemanager.FortSiegeManager;
import net.sf.l2j.gameserver.instancemanager.GrandBossManager;
import net.sf.l2j.gameserver.instancemanager.ItemsOnGroundManager;
import net.sf.l2j.gameserver.instancemanager.MercTicketManager;
import net.sf.l2j.gameserver.instancemanager.PetitionManager;
import net.sf.l2j.gameserver.instancemanager.QuestManager;
import net.sf.l2j.gameserver.instancemanager.RaidBossSpawnManager;
import net.sf.l2j.gameserver.instancemanager.SiegeManager;
import net.sf.l2j.gameserver.instancemanager.TransformationManager;
import net.sf.l2j.gameserver.model.AutoChatHandler;
import net.sf.l2j.gameserver.model.AutoSpawnHandler;
import net.sf.l2j.gameserver.model.L2Manor;
import net.sf.l2j.gameserver.model.L2PetDataTable;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.entity.Hero;
import net.sf.l2j.gameserver.model.entity.TvTManager;
import net.sf.l2j.gameserver.network.L2GameClient;
import net.sf.l2j.gameserver.network.L2GamePacketHandler;
import net.sf.l2j.gameserver.pathfinding.geonodes.GeoPathFinding;
import net.sf.l2j.gameserver.script.faenor.FaenorScriptEngine;
import net.sf.l2j.gameserver.scripting.L2ScriptEngineManager;
import net.sf.l2j.gameserver.taskmanager.KnownListUpdateTaskManager;
import net.sf.l2j.gameserver.taskmanager.TaskManager;
import net.sf.l2j.gameserver.util.DynamicExtension;
import net.sf.l2j.gameserver.util.FloodProtector;
import net.sf.l2j.status.Status;

import org.mmocore.network.SelectorConfig;
import org.mmocore.network.SelectorThread;

/**
 * This class ...
 *
 * @version $Revision: 1.29.2.15.2.19 $ $Date: 2005/04/05 19:41:23 $
 */
public class GameServer
{
	private static final Logger _log = Logger.getLogger(GameServer.class.getName());
	private final SelectorThread<L2GameClient> _selectorThread;
	private final SkillTable _skillTable;
	private final ItemTable _itemTable;
	private final NpcTable _npcTable;
	private final HennaTable _hennaTable;
	private final IdFactory _idFactory;
	public static GameServer gameServer;
	private static ClanHallManager _cHManager;
	private final ItemHandler _itemHandler;
	private final SkillHandler _skillHandler;
	private final AdminCommandHandler _adminCommandHandler;
	private final Shutdown _shutdownHandler;
	private final UserCommandHandler _userCommandHandler;
    private final VoicedCommandHandler _voicedCommandHandler;
    private final DoorTable _doorTable;
    private final SevenSigns _sevenSignsEngine;
    private final AutoChatHandler _autoChatHandler;
	private final AutoSpawnHandler _autoSpawnHandler;
	private LoginServerThread _loginThread;
    private final HelperBuffTable _helperBuffTable;

	private static Status _statusServer;
	@SuppressWarnings("unused")
	private final ThreadPoolManager _threadpools;

    public static final Calendar dateTimeServerStarted = Calendar.getInstance();

    public long getUsedMemoryMB()
	{
		return (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory())/1048576; // 1024 * 1024 = 1048576;
	}

    public SelectorThread<L2GameClient> getSelectorThread()
    {
    	return _selectorThread;
    }

	public ClanHallManager getCHManager(){
		return _cHManager;
	}
	public GameServer() throws Exception
	{
        gameServer = this;
		_log.finest("used mem:" + getUsedMemoryMB()+"MB" );

        if (Config.SERVER_VERSION != null)
        {
            _log.info("L2J Server Version:    "+Config.SERVER_VERSION);
        }
        if (Config.DATAPACK_VERSION != null)
        {
            _log.info("L2J Datapack Version:  "+Config.DATAPACK_VERSION);
        }
		_idFactory = IdFactory.getInstance();
        if (!_idFactory.isInitialized())
        {
            _log.severe("Could not read object IDs from DB. Please Check Your Data.");
            throw new Exception("Could not initialize the ID factory");
        }

        _threadpools = ThreadPoolManager.getInstance();

		new File(Config.DATAPACK_ROOT, "data/clans").mkdirs();
		new File(Config.DATAPACK_ROOT, "data/crests").mkdirs();

        // load script engines
        L2ScriptEngineManager.getInstance();
        
		// start game time control early
		GameTimeController.getInstance();

		// keep the references of Singletons to prevent garbage collection
		CharNameTable.getInstance();

		_itemTable = ItemTable.getInstance();
		if (!_itemTable.isInitialized())
		{
		    _log.severe("Could not find the extraced files. Please Check Your Data.");
		    throw new Exception("Could not initialize the item table");
		}

		ExtractableItemsData.getInstance();
		SummonItemsData.getInstance();

		MerchantPriceConfigTable.getInstance();
		TradeController.getInstance();
		_skillTable = SkillTable.getInstance();
		if (!_skillTable.isInitialized())
		{
		    _log.severe("Could not find the extraced files. Please Check Your Data.");
		    throw new Exception("Could not initialize the skill table");
		}
		
		// L2EMU_ADD by Rayan. L2J - BigBro
		if(Config.ALLOW_NPC_WALKERS)
		    NpcWalkerRoutesTable.getInstance().load();
		
		NpcBufferTable.getInstance();
		
		RecipeController.getInstance();

		SkillTreeTable.getInstance();
		ArmorSetsTable.getInstance();
		FishTable.getInstance();
		SkillSpellbookTable.getInstance();
		CharTemplateTable.getInstance();
		NobleSkillTable.getInstance();
		HeroSkillTable.getInstance();

        //Call to load caches
        HtmCache.getInstance();
        CrestCache.getInstance();
        ClanTable.getInstance();
		_npcTable = NpcTable.getInstance();

		if (!_npcTable.isInitialized())
		{
		    _log.severe("Could not find the extraced files. Please Check Your Data.");
		    throw new Exception("Could not initialize the npc table");
		}

		_hennaTable = HennaTable.getInstance();

		if (!_hennaTable.isInitialized())
		{
		   throw new Exception("Could not initialize the Henna Table");
		}

		HennaTreeTable.getInstance();

		if (!_hennaTable.isInitialized())
		{
		   throw new Exception("Could not initialize the Henna Tree Table");
		}

        _helperBuffTable = HelperBuffTable.getInstance();

        if (!_helperBuffTable.isInitialized())
        {
           throw new Exception("Could not initialize the Helper Buff Table");
        }

        GeoData.getInstance();
        if (Config.GEODATA == 2)
        	GeoPathFinding.getInstance();

        // Load clan hall data before zone data
        _cHManager = ClanHallManager.getInstance();
		CastleManager.getInstance();
		SiegeManager.getInstance();
		FortManager.getInstance();
        FortSiegeManager.getInstance();

		TeleportLocationTable.getInstance();
		LevelUpData.getInstance();
		L2World.getInstance();
		ZoneData.getInstance();
        SpawnTable.getInstance();
        RaidBossSpawnManager.getInstance();
        DayNightSpawnManager.getInstance().notifyChangeMode();
        GrandBossManager.getInstance();
        DimensionalRiftManager.getInstance();
		Announcements.getInstance();
		MapRegionTable.getInstance();
		EventDroplist.getInstance();

		/** Load Manor data */
		L2Manor.getInstance();

		/** Load Manager */
		AuctionManager.getInstance();
		BoatManager.getInstance();
		CastleManorManager.getInstance();
		MercTicketManager.getInstance();
		//PartyCommandManager.getInstance();
		PetitionManager.getInstance();
		QuestManager.getInstance();
        TransformationManager.getInstance();
        
        try
        {
            _log.info("Loading Server Scripts");
            File scripts = new File("data/scripts.cfg");
            L2ScriptEngineManager.getInstance().executeScriptList(scripts);
        }
        catch (IOException ioe)
        {
            _log.severe("Failed loading scripts.cfg, no script going to be loaded");
        }
        
        QuestManager.getInstance().report();
        TransformationManager.getInstance().report();
        
		AugmentationData.getInstance();
		if (Config.SAVE_DROPPED_ITEM)
			ItemsOnGroundManager.getInstance();

		if (Config.AUTODESTROY_ITEM_AFTER > 0 || Config.HERB_AUTO_DESTROY_TIME > 0)
    	    ItemsAutoDestroy.getInstance();

        MonsterRace.getInstance();

		_doorTable = DoorTable.getInstance();
		_doorTable.parseData();
        StaticObjects.getInstance();

		_sevenSignsEngine = SevenSigns.getInstance();
        SevenSignsFestival.getInstance();
		_autoSpawnHandler = AutoSpawnHandler.getInstance();
		_autoChatHandler = AutoChatHandler.getInstance();

        // Spawn the Orators/Preachers if in the Seal Validation period.
        _sevenSignsEngine.spawnSevenSignsNPC();

        Olympiad.getInstance();
        Hero.getInstance();
        FaenorScriptEngine.getInstance();
        // Init of a cursed weapon manager
        CursedWeaponsManager.getInstance();

		_log.config("AutoChatHandler: Loaded " + _autoChatHandler.size() + " handlers in total.");
		_log.config("AutoSpawnHandler: Loaded " + _autoSpawnHandler.size() + " handlers in total.");

		_itemHandler = ItemHandler.getInstance();
		_itemHandler.registerItemHandler(new ScrollOfEscape());
		_itemHandler.registerItemHandler(new ScrollOfResurrection());
		_itemHandler.registerItemHandler(new SoulShots());
		_itemHandler.registerItemHandler(new SpiritShot());
		_itemHandler.registerItemHandler(new BlessedSpiritShot());
        _itemHandler.registerItemHandler(new BeastSoulShot());
        _itemHandler.registerItemHandler(new BeastSpiritShot());
        _itemHandler.registerItemHandler(new Key());
        _itemHandler.registerItemHandler(new PaganKeys());
		_itemHandler.registerItemHandler(new Maps());
		_itemHandler.registerItemHandler(new Potions());
		_itemHandler.registerItemHandler(new Recipes());
        _itemHandler.registerItemHandler(new RollingDice());
        _itemHandler.registerItemHandler(new MysteryPotion());
		_itemHandler.registerItemHandler(new EnchantScrolls());
        _itemHandler.registerItemHandler(new EnergyStone());
		_itemHandler.registerItemHandler(new Book());
		_itemHandler.registerItemHandler(new Remedy());
		_itemHandler.registerItemHandler(new Scrolls());
		_itemHandler.registerItemHandler(new CrystalCarol());
		_itemHandler.registerItemHandler(new SoulCrystals());
		_itemHandler.registerItemHandler(new SevenSignsRecord());
        _itemHandler.registerItemHandler(new CharChangePotions());
        _itemHandler.registerItemHandler(new Firework());
        _itemHandler.registerItemHandler(new Seed());
        _itemHandler.registerItemHandler(new Harvester());
        _itemHandler.registerItemHandler(new MercTicket());
		_itemHandler.registerItemHandler(new FishShots());
		_itemHandler.registerItemHandler(new ExtractableItems());
		_itemHandler.registerItemHandler(new SpecialXMas());
		_itemHandler.registerItemHandler(new SummonItems());
		_itemHandler.registerItemHandler(new BeastSpice());
        _log.config("ItemHandler: Loaded " + _itemHandler.size() + " handlers.");

		_skillHandler = SkillHandler.getInstance();
		_skillHandler.registerSkillHandler(new Blow());
		_skillHandler.registerSkillHandler(new Pdam());
		_skillHandler.registerSkillHandler(new Mdam());
		_skillHandler.registerSkillHandler(new CpDam());
		_skillHandler.registerSkillHandler(new Manadam());
		_skillHandler.registerSkillHandler(new Heal());
        _skillHandler.registerSkillHandler(new CombatPointHeal());
		_skillHandler.registerSkillHandler(new ManaHeal());
		_skillHandler.registerSkillHandler(new BalanceLife());
		_skillHandler.registerSkillHandler(new Charge());
		_skillHandler.registerSkillHandler(new Continuous());
		_skillHandler.registerSkillHandler(new Resurrect());
        _skillHandler.registerSkillHandler(new Spoil());
        _skillHandler.registerSkillHandler(new Sweep());
        _skillHandler.registerSkillHandler(new StrSiegeAssault());
        _skillHandler.registerSkillHandler(new SummonFriend());
        _skillHandler.registerSkillHandler(new SummonTreasureKey());
        _skillHandler.registerSkillHandler(new Disablers());
		_skillHandler.registerSkillHandler(new Recall());
        _skillHandler.registerSkillHandler(new SiegeFlag());
        _skillHandler.registerSkillHandler(new TakeCastle());
        _skillHandler.registerSkillHandler(new TakeFort());
        _skillHandler.registerSkillHandler(new Unlock());
        _skillHandler.registerSkillHandler(new DrainSoul());
        _skillHandler.registerSkillHandler(new Craft());
		_skillHandler.registerSkillHandler(new Fishing());
		_skillHandler.registerSkillHandler(new FishingSkill());
        _skillHandler.registerSkillHandler(new BeastFeed());
        _skillHandler.registerSkillHandler(new DeluxeKey());
        _skillHandler.registerSkillHandler(new Sow());
        _skillHandler.registerSkillHandler(new Soul());
        _skillHandler.registerSkillHandler(new Harvest());
        _skillHandler.registerSkillHandler(new GetPlayer());
        _skillHandler.registerSkillHandler(new TransformDispel());
        _skillHandler.registerSkillHandler(new Trap());
        _log.config("SkillHandler: Loaded " + _skillHandler.size() + " handlers.");

		_adminCommandHandler = AdminCommandHandler.getInstance();
		_adminCommandHandler.registerAdminCommandHandler(new AdminAdmin());
		_adminCommandHandler.registerAdminCommandHandler(new AdminInvul());
		_adminCommandHandler.registerAdminCommandHandler(new AdminDelete());
		_adminCommandHandler.registerAdminCommandHandler(new AdminKill());
		_adminCommandHandler.registerAdminCommandHandler(new AdminTarget());
		_adminCommandHandler.registerAdminCommandHandler(new AdminShop());
		_adminCommandHandler.registerAdminCommandHandler(new AdminAnnouncements());
		_adminCommandHandler.registerAdminCommandHandler(new AdminCreateItem());
        _adminCommandHandler.registerAdminCommandHandler(new AdminHeal());
		_adminCommandHandler.registerAdminCommandHandler(new AdminHelpPage());
		_adminCommandHandler.registerAdminCommandHandler(new AdminShutdown());
		_adminCommandHandler.registerAdminCommandHandler(new AdminSpawn());
		_adminCommandHandler.registerAdminCommandHandler(new AdminSkill());
		_adminCommandHandler.registerAdminCommandHandler(new AdminExpSp());
        _adminCommandHandler.registerAdminCommandHandler(new AdminEventEngine());
		_adminCommandHandler.registerAdminCommandHandler(new AdminGmChat());
		_adminCommandHandler.registerAdminCommandHandler(new AdminEditChar());
		_adminCommandHandler.registerAdminCommandHandler(new AdminGm());
		_adminCommandHandler.registerAdminCommandHandler(new AdminTeleport());
		_adminCommandHandler.registerAdminCommandHandler(new AdminRepairChar());
        _adminCommandHandler.registerAdminCommandHandler(new AdminChangeAccessLevel());
        _adminCommandHandler.registerAdminCommandHandler(new AdminBan());
        _adminCommandHandler.registerAdminCommandHandler(new AdminPolymorph());
		_adminCommandHandler.registerAdminCommandHandler(new AdminBanChat());
        _adminCommandHandler.registerAdminCommandHandler(new AdminKick());
        _adminCommandHandler.registerAdminCommandHandler(new AdminMonsterRace());
        _adminCommandHandler.registerAdminCommandHandler(new AdminEditNpc());
        _adminCommandHandler.registerAdminCommandHandler(new AdminFightCalculator());
        _adminCommandHandler.registerAdminCommandHandler(new AdminMenu());
        _adminCommandHandler.registerAdminCommandHandler(new AdminSiege());
        _adminCommandHandler.registerAdminCommandHandler(new AdminFortSiege());
        _adminCommandHandler.registerAdminCommandHandler(new AdminPathNode());
        _adminCommandHandler.registerAdminCommandHandler(new AdminPetition());
        _adminCommandHandler.registerAdminCommandHandler(new AdminPForge());
        _adminCommandHandler.registerAdminCommandHandler(new AdminBBS());
        _adminCommandHandler.registerAdminCommandHandler(new AdminEffects());
        _adminCommandHandler.registerAdminCommandHandler(new AdminDoorControl());
        _adminCommandHandler.registerAdminCommandHandler(new AdminTest());
        _adminCommandHandler.registerAdminCommandHandler(new AdminEnchant());
        _adminCommandHandler.registerAdminCommandHandler(new AdminMobGroup());
        _adminCommandHandler.registerAdminCommandHandler(new AdminRes());
        _adminCommandHandler.registerAdminCommandHandler(new AdminMammon());
        _adminCommandHandler.registerAdminCommandHandler(new AdminUnblockIp());
        _adminCommandHandler.registerAdminCommandHandler(new AdminPledge());
        _adminCommandHandler.registerAdminCommandHandler(new AdminRide());
        _adminCommandHandler.registerAdminCommandHandler(new AdminLogin());
        _adminCommandHandler.registerAdminCommandHandler(new AdminCache());
        _adminCommandHandler.registerAdminCommandHandler(new AdminLevel());
        _adminCommandHandler.registerAdminCommandHandler(new AdminQuest());
        _adminCommandHandler.registerAdminCommandHandler(new AdminZone());
        _adminCommandHandler.registerAdminCommandHandler(new AdminCursedWeapons());
        _adminCommandHandler.registerAdminCommandHandler(new AdminGeodata());
        _adminCommandHandler.registerAdminCommandHandler(new AdminGeoEditor());
        _adminCommandHandler.registerAdminCommandHandler(new AdminManor());
        _adminCommandHandler.registerAdminCommandHandler(new AdminTvTEvent());
        //_adminCommandHandler.registerAdminCommandHandler(new AdminRadar());
        _log.config("AdminCommandHandler: Loaded " + _adminCommandHandler.size() + " handlers.");

        _userCommandHandler = UserCommandHandler.getInstance();
        _userCommandHandler.registerUserCommandHandler(new ClanPenalty());
        _userCommandHandler.registerUserCommandHandler(new ClanWarsList());
        _userCommandHandler.registerUserCommandHandler(new DisMount());
        _userCommandHandler.registerUserCommandHandler(new Escape());
        _userCommandHandler.registerUserCommandHandler(new InstanceZone());
        _userCommandHandler.registerUserCommandHandler(new Loc());
        _userCommandHandler.registerUserCommandHandler(new Mount());
        _userCommandHandler.registerUserCommandHandler(new PartyInfo());
		_userCommandHandler.registerUserCommandHandler(new Time());
		_userCommandHandler.registerUserCommandHandler(new OlympiadStat());
		_userCommandHandler.registerUserCommandHandler(new ChannelLeave());
		_userCommandHandler.registerUserCommandHandler(new ChannelDelete());
		_userCommandHandler.registerUserCommandHandler(new ChannelListUpdate());

        _log.config("UserCommandHandler: Loaded " + _userCommandHandler.size() + " handlers.");

		_voicedCommandHandler = VoicedCommandHandler.getInstance();
		_voicedCommandHandler.registerVoicedCommandHandler(new stats());

		if(Config.L2JMOD_ALLOW_WEDDING)
			_voicedCommandHandler.registerVoicedCommandHandler(new Wedding());

		if(Config.BANKING_SYSTEM_ENABLED)
			_voicedCommandHandler.registerVoicedCommandHandler(new Banking());
		
		_log.config("VoicedCommandHandler: Loaded " + _voicedCommandHandler.size() + " handlers.");
						
		if(Config.L2JMOD_ALLOW_WEDDING)
			CoupleManager.getInstance();

        TaskManager.getInstance();

		GmListTable.getInstance();

        // read pet stats from db
        L2PetDataTable.getInstance().loadPetsData();

        Universe.getInstance();

		if (Config.ACCEPT_GEOEDITOR_CONN)
			GeoEditorListener.getInstance();

		_shutdownHandler = Shutdown.getInstance();
		Runtime.getRuntime().addShutdownHook(_shutdownHandler);

		try
        {
            _doorTable.getDoor(24190001).openMe();
            _doorTable.getDoor(24190002).openMe();
            _doorTable.getDoor(24190003).openMe();
            _doorTable.getDoor(24190004).openMe();
            _doorTable.getDoor(23180001).openMe();
            _doorTable.getDoor(23180002).openMe();
            _doorTable.getDoor(23180003).openMe();
            _doorTable.getDoor(23180004).openMe();
            _doorTable.getDoor(23180005).openMe();
            _doorTable.getDoor(23180006).openMe();

            _doorTable.checkAutoOpen();
        }
        catch (NullPointerException e)
        {
            _log.warning("There is errors in your Door.csv file. Update door.csv");
            if (Config.DEBUG)
            	e.printStackTrace();
        }
        ForumsBBSManager.getInstance();
        _log.config("IdFactory: Free ObjectID's remaining: " + IdFactory.getInstance().size());

        // initialize the dynamic extension loader
        try {
            DynamicExtension.getInstance();
        } catch (Exception ex) {
            _log.log(Level.WARNING, "DynamicExtension could not be loaded and initialized", ex);
        }

        FloodProtector.getInstance();
        TvTManager.getInstance();
        KnownListUpdateTaskManager.getInstance();
		System.gc();
		// maxMemory is the upper limit the jvm can use, totalMemory the size of the current allocation pool, freeMemory the unused memory in the allocation pool
		long freeMem = (Runtime.getRuntime().maxMemory()-Runtime.getRuntime().totalMemory()+Runtime.getRuntime().freeMemory()) / 1048576; // 1024 * 1024 = 1048576;
		long totalMem = Runtime.getRuntime().maxMemory() / 1048576;
		_log.info("GameServer Started, free memory "+freeMem+" Mb of "+totalMem+" Mb");

		_loginThread = LoginServerThread.getInstance();
		_loginThread.start();
        
        L2GamePacketHandler gph = new L2GamePacketHandler();
		SelectorConfig<L2GameClient> sc = new SelectorConfig<L2GameClient>(null, null, gph, gph);
        sc.setMaxSendPerPass(12);
        sc.setSelectorSleepTime(20);
        
		_selectorThread = new SelectorThread<L2GameClient>(sc, gph, gph, null);
		
		InetAddress bindAddress = null;
		if (!Config.GAMESERVER_HOSTNAME.equals("*"))
		{
			try
			{
				bindAddress = InetAddress.getByName(Config.GAMESERVER_HOSTNAME);
			}
			catch (UnknownHostException e1)
			{
				_log.severe("WARNING: The GameServer bind address is invalid, using all avaliable IPs. Reason: "+e1.getMessage());
				if (Config.DEVELOPER)
				{
					e1.printStackTrace();
				}
			}
		}
		
		try
		{
			_selectorThread.openServerSocket(bindAddress, Config.PORT_GAME);
		}
		catch (IOException e)
		{
			_log.severe("FATAL: Failed to open server socket. Reason: "+e.getMessage());
			if (Config.DEVELOPER)
			{
				e.printStackTrace();
			}
			System.exit(1);
		}
		_selectorThread.start();
		_log.config("Maximum Numbers of Connected Players: " + Config.MAXIMUM_ONLINE_USERS);
	}

	public static void main(String[] args) throws Exception
    {
		Server.serverMode = Server.MODE_GAMESERVER;
//      Local Constants
		final String LOG_FOLDER = "log"; // Name of folder for log file
		final String LOG_NAME   = "./log.cfg"; // Name of log file

		/*** Main ***/
		// Create log folder
		File logFolder = new File(Config.DATAPACK_ROOT, LOG_FOLDER);
		logFolder.mkdir();

		// Create input stream for log file -- or store file data into memory
		InputStream is =  new FileInputStream(new File(LOG_NAME));
		LogManager.getLogManager().readConfiguration(is);
		is.close();

		// Initialize config
		Config.load();
		L2DatabaseFactory.getInstance();
		gameServer = new GameServer();

		if ( Config.IS_TELNET_ENABLED ) {
		    _statusServer = new Status(Server.serverMode);
		    _statusServer.start();
		}
		else {
		    _log.info("Telnet server is currently disabled.");
		}
    }
}
