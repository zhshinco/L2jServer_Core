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
package net.sf.l2j.gameserver.clientpackets;

import java.io.UnsupportedEncodingException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Logger;

import net.sf.l2j.Base64;
import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.Announcements;
import net.sf.l2j.gameserver.GmListTable;
import net.sf.l2j.gameserver.Olympiad;
import net.sf.l2j.gameserver.SevenSigns;
import net.sf.l2j.gameserver.TaskPriority;
import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.communitybbs.Manager.RegionBBSManager;
import net.sf.l2j.gameserver.datatables.MapRegionTable;
import net.sf.l2j.gameserver.handler.AdminCommandHandler;
import net.sf.l2j.gameserver.instancemanager.ClanHallManager;
import net.sf.l2j.gameserver.instancemanager.CoupleManager;
import net.sf.l2j.gameserver.instancemanager.CursedWeaponsManager;
import net.sf.l2j.gameserver.instancemanager.DimensionalRiftManager;
import net.sf.l2j.gameserver.instancemanager.PetitionManager;
import net.sf.l2j.gameserver.instancemanager.SiegeManager;
import net.sf.l2j.gameserver.instancemanager.TransformationManager;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.entity.ClanHall;
import net.sf.l2j.gameserver.model.entity.Couple;
import net.sf.l2j.gameserver.model.entity.Hero;
import net.sf.l2j.gameserver.model.entity.L2Event;
import net.sf.l2j.gameserver.model.entity.Siege;
import net.sf.l2j.gameserver.model.entity.TvTEvent;
import net.sf.l2j.gameserver.model.quest.Quest;
import net.sf.l2j.gameserver.model.quest.QuestState;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.serverpackets.Die;
import net.sf.l2j.gameserver.serverpackets.EtcStatusUpdate;
import net.sf.l2j.gameserver.serverpackets.ExBasicActionList;
import net.sf.l2j.gameserver.serverpackets.ExStorageMaxCount;
import net.sf.l2j.gameserver.serverpackets.FriendList;
import net.sf.l2j.gameserver.serverpackets.HennaInfo;
import net.sf.l2j.gameserver.serverpackets.ItemList;
import net.sf.l2j.gameserver.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.serverpackets.PledgeShowMemberListAll;
import net.sf.l2j.gameserver.serverpackets.PledgeShowMemberListUpdate;
import net.sf.l2j.gameserver.serverpackets.PledgeSkillList;
import net.sf.l2j.gameserver.serverpackets.PledgeStatusChanged;
import net.sf.l2j.gameserver.serverpackets.QuestList;
import net.sf.l2j.gameserver.serverpackets.ShortCutInit;
import net.sf.l2j.gameserver.serverpackets.SkillCoolTime;
import net.sf.l2j.gameserver.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.serverpackets.UserInfo;
import net.sf.l2j.gameserver.util.FloodProtector;
/**
 * Enter World Packet Handler<p>
 * <p>
 * 0000: 03 <p>
 * packet format rev656 cbdddd
 * <p>
 *
 * @version $Revision: 1.16.2.1.2.7 $ $Date: 2005/03/29 23:15:33 $
 */
public class EnterWorld extends L2GameClientPacket
{
    private static final String _C__03_ENTERWORLD = "[C] 03 EnterWorld";
    private static Logger _log = Logger.getLogger(EnterWorld.class.getName());

    public TaskPriority getPriority() { return TaskPriority.PR_URGENT; }

    @Override
    protected void readImpl()
    {
        // this is just a trigger packet. it has no content
    }

    @Override
    protected void runImpl()
    {
        L2PcInstance activeChar = getClient().getActiveChar();

        if (activeChar == null)
        {
            _log.warning("EnterWorld failed! activeChar is null...");
            getClient().closeNow();
            return;
        }
        
        // Register in flood protector
        FloodProtector.getInstance().registerNewPlayer(activeChar.getObjectId());

        if (L2World.getInstance().findObject(activeChar.getObjectId()) != null)
        {
            if(Config.DEBUG)
                _log.warning("User already exist in OID map! User "+activeChar.getName()+" is character clone");
            //activeChar.closeNetConnection();
        }
        
        if (activeChar.isGM())
        {
            if (Config.GM_STARTUP_INVULNERABLE
                    && (!Config.ALT_PRIVILEGES_ADMIN && activeChar.getAccessLevel() >= Config.GM_GODMODE
                      || Config.ALT_PRIVILEGES_ADMIN && AdminCommandHandler.getInstance().checkPrivileges(activeChar, "admin_invul")))
                activeChar.setIsInvul(true);

            if (Config.GM_STARTUP_INVISIBLE
                    && (!Config.ALT_PRIVILEGES_ADMIN && activeChar.getAccessLevel() >= Config.GM_GODMODE
                      || Config.ALT_PRIVILEGES_ADMIN && AdminCommandHandler.getInstance().checkPrivileges(activeChar, "admin_invisible")))
                activeChar.getAppearance().setInvisible();

            if (Config.GM_STARTUP_SILENCE
                    && (!Config.ALT_PRIVILEGES_ADMIN && activeChar.getAccessLevel() >= Config.GM_MENU
                      || Config.ALT_PRIVILEGES_ADMIN && AdminCommandHandler.getInstance().checkPrivileges(activeChar, "admin_silence")))
                activeChar.setMessageRefusal(true);

            if (Config.GM_STARTUP_AUTO_LIST
                    && (!Config.ALT_PRIVILEGES_ADMIN && activeChar.getAccessLevel() >= Config.GM_MENU
                      || Config.ALT_PRIVILEGES_ADMIN && AdminCommandHandler.getInstance().checkPrivileges(activeChar, "admin_gmliston")))
                GmListTable.getInstance().addGm(activeChar, false);
            else
                GmListTable.getInstance().addGm(activeChar, true);

            if (Config.GM_NAME_COLOR_ENABLED)
            {
                if (activeChar.getAccessLevel() >= 100)
                    activeChar.getAppearance().setNameColor(Config.ADMIN_NAME_COLOR);
                else if (activeChar.getAccessLevel() >= 75)
                    activeChar.getAppearance().setNameColor(Config.GM_NAME_COLOR);
            }
            
            if (Config.GM_TITLE_COLOR_ENABLED)
            {
                if (activeChar.getAccessLevel() >= 100)
                    activeChar.getAppearance().setTitleColor(Config.ADMIN_TITLE_COLOR);
                else if (activeChar.getAccessLevel() >= 75)
                    activeChar.getAppearance().setTitleColor(Config.GM_TITLE_COLOR);
            }
        }
        
        if (activeChar.getCurrentHp() < 0.5) // is dead
        	activeChar.setIsDead(true);

        if (activeChar.getClan() != null)
        {
            for (Siege siege : SiegeManager.getInstance().getSieges())
            {
                if (!siege.getIsInProgress()) continue;
                if (siege.checkIsAttacker(activeChar.getClan()))
                    activeChar.setSiegeState((byte)1);
                else if (siege.checkIsDefender(activeChar.getClan()))
                    activeChar.setSiegeState((byte)2);
            }
        }
        
        if (Hero.getInstance().getHeroes() != null &&
                Hero.getInstance().getHeroes().containsKey(activeChar.getObjectId()))
            activeChar.setHero(true);

        setPledgeClass(activeChar);

        activeChar.sendPacket(new UserInfo(activeChar));
        
        // Send Macro List
        activeChar.getMacroses().sendUpdate();
        
        // Send Item List
        sendPacket(new ItemList(activeChar, false));
        
        // Send gg check (even if we are not going to check for reply)
        activeChar.queryGameGuard();
        
        // Send Shortcuts
        sendPacket(new ShortCutInit(activeChar));
        
        // Send Action list
        activeChar.sendPacket(ExBasicActionList.DEFAULT_ACTION_LIST);

        activeChar.sendSkillList();
        
        activeChar.sendPacket(new HennaInfo(activeChar));
        
        Quest.playerEnter(activeChar);
        activeChar.sendPacket(new QuestList());
        loadTutorial(activeChar);
        
        if (Config.PLAYER_SPAWN_PROTECTION > 0)
            activeChar.setProtection(true);

        activeChar.spawnMe(activeChar.getX(), activeChar.getY(), activeChar.getZ());

        if (L2Event.active && L2Event.connectionLossData.containsKey(activeChar.getName()) && L2Event.isOnEvent(activeChar))
            L2Event.restoreChar(activeChar);
        else if (L2Event.connectionLossData.containsKey(activeChar.getName()))
            L2Event.restoreAndTeleChar(activeChar);

         // engage and notify Partner
        if(Config.L2JMOD_ALLOW_WEDDING)
        {
            engage(activeChar);
            notifyPartner(activeChar,activeChar.getPartnerId());
        }
        
        if(activeChar.isCursedWeaponEquipped()) 
        {
            CursedWeaponsManager.getInstance().getCursedWeapon(activeChar.getCursedWeaponEquippedId()).cursedOnLogin();
        }
        else if (activeChar.transformSelectInfo() > 0)
        {
            TransformationManager.getInstance().transformPlayer(activeChar.transformId(), activeChar, Long.MAX_VALUE);
        }

        if (activeChar.getAllEffects() != null)
        {
            for (L2Effect e : activeChar.getAllEffects())
            {
                if (e.getEffectType() == L2Effect.EffectType.HEAL_OVER_TIME)
                {
                    activeChar.stopEffects(L2Effect.EffectType.HEAL_OVER_TIME);
                    activeChar.removeEffect(e);
                }

                if (e.getEffectType() == L2Effect.EffectType.COMBAT_POINT_HEAL_OVER_TIME)
                {
                    activeChar.stopEffects(L2Effect.EffectType.COMBAT_POINT_HEAL_OVER_TIME);
                    activeChar.removeEffect(e);
                }
                
                //  Charges are gone after relog.
                if (e.getEffectType() == L2Effect.EffectType.CHARGE)
                {
                    e.exit();
                }
            }
        }
        
        activeChar.updateEffectIcons();

        activeChar.sendPacket(new EtcStatusUpdate(activeChar));

        //Expand Skill
        ExStorageMaxCount esmc = new ExStorageMaxCount(activeChar);
        activeChar.sendPacket(esmc);
        
        sendPacket(new FriendList(activeChar));
        
        SystemMessage sm = new SystemMessage(SystemMessageId.WELCOME_TO_LINEAGE);
        sendPacket(sm);

        sm = new SystemMessage(SystemMessageId.S1_S2);
        sm.addString(getText("VGhpcyBzZXJ2ZXIgdXNlcyBMMkosIGEgcHJvamVjdCBmb3VuZGVkIGJ5IEwyQ2hlZg==\n")); 

        sendPacket(sm);
        sm = new SystemMessage(SystemMessageId.S1_S2);
        sm.addString(getText("YW5kIGRldmVsb3BlZCBieSB0aGUgTDJKIERldiBUZWFtIGF0IGwyanNlcnZlci5jb20=\n")); 

        sendPacket(sm);

        if (Config.SERVER_VERSION != null)
        {
            sm = new SystemMessage(SystemMessageId.S1_S2);
            sm.addString(getText("TDJKIFNlcnZlciBWZXJzaW9uOg==")+"      "+Config.SERVER_VERSION);
            sendPacket(sm);
        }

        if (Config.DATAPACK_VERSION != null)
        {
            sm = new SystemMessage(SystemMessageId.S1_S2);
            sm.addString(getText("TDJKIERhdGFwYWNrIFZlcnNpb246")+"  "+Config.DATAPACK_VERSION);
            sendPacket(sm);
        }
        sm = null;

        sm = new SystemMessage(SystemMessageId.S1_S2);
        sm.addString(getText("Q29weXJpZ2h0IDIwMDQtMjAwOA==\n"));
        sendPacket(sm);

        SevenSigns.getInstance().sendCurrentPeriodMsg(activeChar);
        Announcements.getInstance().showAnnouncements(activeChar);

        if (Config.SERVER_NEWS)
        {
            String serverNews = HtmCache.getInstance().getHtm("data/html/servnews.htm");
            if (serverNews != null)
                sendPacket(new NpcHtmlMessage(1, serverNews));
        }

        PetitionManager.getInstance().checkPetitionMessages(activeChar);

        // send user info again .. just like the real client
        //sendPacket(ui);

        if (activeChar.getClanId() != 0 && activeChar.getClan() != null)
        {
            sendPacket(new PledgeShowMemberListAll(activeChar.getClan(), activeChar));
            sendPacket(new PledgeStatusChanged(activeChar.getClan()));
        }

      
        if (activeChar.isAlikeDead()) // dead or fake dead
        {
            // no broadcast needed since the player will already spawn dead to others
            sendPacket(new Die(activeChar));
        }

        notifyFriends(activeChar);
        notifyClanMembers(activeChar);
        notifySponsorOrApprentice(activeChar);

        activeChar.onPlayerEnter();
        
        sendPacket(new SkillCoolTime(activeChar));

        if (Olympiad.getInstance().playerInStadia(activeChar))
        {
            activeChar.teleToLocation(MapRegionTable.TeleportWhereType.Town);
            activeChar.sendMessage("You have been teleported to the nearest town due to you being in an Olympiad Stadium");
        }

        if (DimensionalRiftManager.getInstance().checkIfInRiftZone(activeChar.getX(), activeChar.getY(), activeChar.getZ(), false))
        {
            DimensionalRiftManager.getInstance().teleportToWaitingRoom(activeChar);
        }

        if (activeChar.getClanJoinExpiryTime() > System.currentTimeMillis())
        {
            activeChar.sendPacket(new SystemMessage(SystemMessageId.CLAN_MEMBERSHIP_TERMINATED));
        }

        if (activeChar.getClan() != null)
        {
            activeChar.sendPacket(new PledgeSkillList(activeChar.getClan()));

            // Add message if clanHall not paid. Possibly this is custom...
            ClanHall clanHall = ClanHallManager.getInstance().getClanHallByOwner(activeChar.getClan());
            if(clanHall != null){
                if(!clanHall.getPaid()){
                    activeChar.sendPacket(new SystemMessage(SystemMessageId.PAYMENT_FOR_YOUR_CLAN_HALL_HAS_NOT_BEEN_MADE_PLEASE_MAKE_PAYMENT_TO_YOUR_CLAN_WAREHOUSE_BY_S1_TOMORROW));
                }
            }
        }

        if (!activeChar.isGM() && activeChar.getSiegeState() < 2 && activeChar.isInsideZone(L2Character.ZONE_SIEGE))
        {
            // Attacker or spectator logging in to a siege zone. Actually should be checked for inside castle only?
            activeChar.teleToLocation(MapRegionTable.TeleportWhereType.Town);
            activeChar.sendMessage("You have been teleported to the nearest town due to you being in siege zone");
        }
        
        RegionBBSManager.getInstance().changeCommunityBoard();
        
        TvTEvent.onLogin(activeChar);
    }

    /**
     * @param activeChar
     */
    private void engage(L2PcInstance cha)
    {
        int _chaid = cha.getObjectId();

        for(Couple cl: CoupleManager.getInstance().getCouples())
        {
           if(cl.getPlayer1Id()==_chaid || cl.getPlayer2Id()==_chaid)
            {
                if(cl.getMaried())
                    cha.setMarried(true);

                cha.setCoupleId(cl.getId());

                if(cl.getPlayer1Id()==_chaid)
                {
                    cha.setPartnerId(cl.getPlayer2Id());
                }
                else
                {
                    cha.setPartnerId(cl.getPlayer1Id());
                }
            }
        }
    }

    /**
     * @param activeChar partnerid
     */
    private void notifyPartner(L2PcInstance cha,int partnerId)
    {
        if(cha.getPartnerId()!=0)
        {
            L2PcInstance partner;
            partner = (L2PcInstance)L2World.getInstance().findObject(cha.getPartnerId());

            if (partner != null)
            {
                partner.sendMessage("Your Partner has logged in");
            }

            partner = null;
        }
    }

    /**
     * @param activeChar
     */
    private void notifyFriends(L2PcInstance cha)
    {
        java.sql.Connection con = null;

        try {
            con = L2DatabaseFactory.getInstance().getConnection();
            PreparedStatement statement;
            statement = con.prepareStatement("SELECT friend_name FROM character_friends WHERE char_id=?");
            statement.setInt(1, cha.getObjectId());
            ResultSet rset = statement.executeQuery();

            L2PcInstance friend;
            String friendName;

            SystemMessage sm = new SystemMessage(SystemMessageId.FRIEND_S1_HAS_LOGGED_IN);
            sm.addString(cha.getName());

            while (rset.next())
            {
                friendName = rset.getString("friend_name");

                friend = L2World.getInstance().getPlayer(friendName);

                if (friend != null) //friend logged in.
                {
                    friend.sendPacket(new FriendList(friend));
                    friend.sendPacket(sm);
                }
            }
            sm = null;

            rset.close();
            statement.close();
        }
        catch (Exception e) {
            _log.warning("could not restore friend data:"+e);
        }
        finally {
            try {con.close();} catch (Exception e){}
        }
    }

    /**
     * @param activeChar
     */
    private void notifyClanMembers(L2PcInstance activeChar)
    {
        L2Clan clan = activeChar.getClan();
        if (clan != null)
        {
            clan.getClanMember(activeChar.getName()).setPlayerInstance(activeChar);
            SystemMessage msg = new SystemMessage(SystemMessageId.CLAN_MEMBER_S1_LOGGED_IN);
            msg.addString(activeChar.getName());
            clan.broadcastToOtherOnlineMembers(msg, activeChar);
            msg = null;
            clan.broadcastToOtherOnlineMembers(new PledgeShowMemberListUpdate(activeChar), activeChar);
        }
    }

    /**
     * @param activeChar
     */
    private void notifySponsorOrApprentice(L2PcInstance activeChar)
    {
        if (activeChar.getSponsor() != 0)
        {
            L2PcInstance sponsor = (L2PcInstance)L2World.getInstance().findObject(activeChar.getSponsor());

            if (sponsor != null)
            {
                SystemMessage msg = new SystemMessage(SystemMessageId.YOUR_APPRENTICE_S1_HAS_LOGGED_IN);
                msg.addString(activeChar.getName());
                sponsor.sendPacket(msg);
            }
        }
        else if (activeChar.getApprentice() != 0)
        {
            L2PcInstance apprentice = (L2PcInstance)L2World.getInstance().findObject(activeChar.getApprentice());

            if (apprentice != null)
            {
                SystemMessage msg = new SystemMessage(SystemMessageId.YOUR_SPONSOR_S1_HAS_LOGGED_IN);
                msg.addString(activeChar.getName());
                apprentice.sendPacket(msg);
            }
        }
    }

    /**
     * @param string
     * @return
     * @throws UnsupportedEncodingException
     */
    private String getText(String string)
    {
        try {
            String result = new String(Base64.decode(string), "UTF-8");
            return result;
        } catch (UnsupportedEncodingException e) {
            // huh, UTF-8 is not supported? :)
            return null;
        }
    }

    private void loadTutorial(L2PcInstance player)
    {
    	QuestState qs = player.getQuestState("255_Tutorial");
    	if(qs != null)
    		qs.getQuest().notifyEvent("UC", null, player);
    }
     
    /* (non-Javadoc)
     * @see net.sf.l2j.gameserver.clientpackets.ClientBasePacket#getType()
     */
    @Override
    public String getType()
    {
        return _C__03_ENTERWORLD;
    }

    private void setPledgeClass(L2PcInstance activeChar)
    {
        int pledgeClass = 0;
        if ( activeChar.getClan() != null)
            pledgeClass = activeChar.getClan().getClanMember(activeChar.getObjectId()).calculatePledgeClass(activeChar);

        if (activeChar.isNoble() && pledgeClass < 5)
               pledgeClass = 5;

        if (activeChar.isHero())
               pledgeClass = 8;

        activeChar.setPledgeClass(pledgeClass);
    }
}
