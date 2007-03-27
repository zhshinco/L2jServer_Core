/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 *
 * http://www.gnu.org/copyleft/gpl.html
 */
package net.sf.l2j.gameserver.model.quest;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javolution.util.FastList;
import javolution.util.FastMap;
import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.datatables.NpcTable;
import net.sf.l2j.gameserver.instancemanager.QuestManager;
import net.sf.l2j.gameserver.lib.Rnd;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2Party;
import net.sf.l2j.gameserver.model.actor.instance.L2NpcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.templates.L2NpcTemplate;


/**
 * @author Luis Arias
 *
 */
public abstract class Quest
{
	protected static Logger _log = Logger.getLogger(Quest.class.getName());

	/** HashMap containing events from String value of the event */
	private static Map<String, Quest> allEventsS = new FastMap<String, Quest>();

	private final int _questId;
	private final String _name;
	private final String _descr;
    private State initialState;
    private Map<String, State> states;
	
	/**
	 * Return collection view of the values contains in the allEventS
	 * @return Collection<Quest>
	 */
	public static Collection<Quest> findAllEvents() {
		return allEventsS.values();
	}
	
    /**
     * (Constructor)Add values to class variables and put the quest in HashMaps. 
     * @param questId : int pointing out the ID of the quest
     * @param name : String corresponding to the name of the quest
     * @param descr : String for the description of the quest
     */
	public Quest(int questId, String name, String descr)
	{
		_questId = questId;
		_name = name;
		_descr = descr;
        states = new FastMap<String, State>();
		if (questId != 0) {
            QuestManager.getInstance().getQuests().add(Quest.this);
		} else {
			allEventsS.put(name, this);
		}
	}
	
    public static enum QuestEventType 
    {
        QUEST_START,
        QUEST_TALK,
        MOBGOTATTACKED,
        MOBKILLED
    }	
	/**
	 * Return ID of the quest
	 * @return int
	 */
	public int getQuestIntId() {
		return _questId;
	}
	
	/**
	 * Set the initial state of the quest with parameter "state"
	 * @param state
	 */
	public void setInitialState(State state) {
		this.initialState = state;
	}
	
	/**
	 * Add a new QuestState to the database and return it.
	 * @param player
	 * @return QuestState : QuestState created
	 */
	public QuestState newQuestState(L2PcInstance player) {
		QuestState qs = new QuestState(this, player, getInitialState(), false);
		Quest.createQuestInDb(qs);
		return qs;
	}
	
	/**
	 * Return initial state of the quest
	 * @return State
	 */
	public State getInitialState() {
		return initialState;
	}
    
	/**
	 * Return name of the quest
	 * @return String
	 */
	public String getName() {
		return _name;
	}
	
	/**
	 * Return description of the quest
	 * @return String
	 */
	public String getDescr() {
		return _descr;
	}
	
	/**
	 * Add a state to the quest
	 * @param state
	 * @return state added
	 */
    public State addState(State state)
    {
        states.put(state.getName(), state);
		return state;
    }
    
    /**
     * Add the quest to the NPC's startQuest
     * @param npcId
     * @return L2NpcTemplate : Start NPC
     */
    public L2NpcTemplate addStartNpc(int npcId)
    {
    	try
    	{
		L2NpcTemplate t = NpcTable.getInstance().getTemplate(npcId);
		if (t != null) {
			t.addQuestEvent(Quest.QuestEventType.QUEST_START, this);
		}
		return t;
    	}
    	catch(Exception e)
    	{
    		e.printStackTrace();
    		return null;
    	}
    }
    
	// these are methods to call from java
    public final boolean notifyAttack(L2NpcInstance npc, L2PcInstance attacker) {
        String res = null;
        try { res = onAttack(npc, attacker); } catch (Exception e) { return showError(attacker, e); }
        return showResult(attacker, res);
    } 
    public final boolean notifyDeath(L2NpcInstance npc, L2Character character, QuestState qs) {
        String res = null;
        try { res = onDeath(npc, character, qs); } catch (Exception e) { return showError(qs.getPlayer(), e); }
        return showResult(qs.getPlayer(), res);
    } 
    public final boolean notifyEvent(String event, QuestState qs) {
        String res = null;
        try { res = onEvent(event, qs); } catch (Exception e) { return showError(qs.getPlayer(), e); }
        return showResult(qs.getPlayer(), res);
    } 
	public final boolean notifyKill (L2NpcInstance npc, L2PcInstance killer) {
		String res = null;
		try { res = onKill(npc, killer); } catch (Exception e) { return showError(killer, e); }
		return showResult(killer, res);
	}
	public final boolean notifyTalk (L2NpcInstance npc, QuestState qs) {
		String res = null;
		try { res = onTalk(npc, qs.getPlayer()); } catch (Exception e) { return showError(qs.getPlayer(), e); }
		qs.getPlayer().setLastQuestNpcObject(npc.getObjectId());
		return showResult(qs.getPlayer(), res);
	}

	// these are methods that java calls to invoke scripts
    @SuppressWarnings("unused") public String onAttack(L2NpcInstance npc, L2PcInstance attacker) { return null; } 
    @SuppressWarnings("unused") public String onDeath (L2NpcInstance npc, L2Character character, QuestState qs) { return onEvent("", qs); }
    @SuppressWarnings("unused") public String onEvent(String event, QuestState qs) { return null; } 
    @SuppressWarnings("unused") public String onKill (L2NpcInstance npc, L2PcInstance killer) { return null; }
    @SuppressWarnings("unused") public String onTalk (L2NpcInstance npc, L2PcInstance talker) { return null; }
	
	/**
	 * Show message error to player who has an access level greater than 0
	 * @param qs : QuestState
	 * @param t : Throwable
	 * @return boolean
	 */
	private boolean showError(L2PcInstance player, Throwable t) {
		_log.log(Level.WARNING, "", t);
		if (player.getAccessLevel() > 0) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			t.printStackTrace(pw);
			pw.close();
			String res = "<html><body><title>Script error</title>"+sw.toString()+"</body></html>";
			return showResult(player, res);
		}
		return false;
	}
	
	/**
	 * Show a message to player.<BR><BR>
	 * <U><I>Concept : </I></U><BR>
	 * 3 cases are managed according to the value of the parameter "res" :<BR>
	 * <LI><U>"res" ends with string ".html" :</U> an HTML is opened in order to be shown in a dialog box</LI>
	 * <LI><U>"res" starts with "<html>" :</U> the message hold in "res" is shown in a dialog box</LI>
	 * <LI><U>otherwise :</U> the message hold in "res" is shown in chat box</LI>
	 * @param qs : QuestState 
	 * @param res : String pointing out the message to show at the player
	 * @return boolean
	 */
	private boolean showResult(L2PcInstance player, String res) {
		if (res == null)
			return true;
		if (res.endsWith(".htm")) {
			showHtmlFile(player, res);
		}
		else if (res.startsWith("<html>")) {
			NpcHtmlMessage npcReply = new NpcHtmlMessage(5);
			npcReply.setHtml(res);
			player.sendPacket(npcReply);
		}
		else {
			SystemMessage sm = new SystemMessage(SystemMessage.S1_S2);
			sm.addString(res);
			player.sendPacket(sm);
		}
		return false;
	}
	
	/**
	 * Add quests to the L2PCInstance of the player.<BR><BR>
	 * <U><I>Action : </U></I><BR>
	 * Add state of quests, drops and variables for quests in the HashMap _quest of L2PcInstance
	 * @param player : Player who is entering the world
	 */
	public static void playerEnter(L2PcInstance player) {

        java.sql.Connection con = null;
        try
        {
	    // Get list of quests owned by the player from database
            con = L2DatabaseFactory.getInstance().getConnection();
            PreparedStatement statement;
            
            PreparedStatement invalidQuestData      = con.prepareStatement("DELETE FROM character_quests WHERE char_id=? and name=?");
            PreparedStatement invalidQuestDataVar   = con.prepareStatement("delete FROM character_quests WHERE char_id=? and name=? and var=?");
            
            statement = con.prepareStatement("SELECT name,value FROM character_quests WHERE char_id=? AND var=?");
            statement.setInt(1, player.getObjectId());
            statement.setString(2, "<state>");
			ResultSet rs = statement.executeQuery();
			while (rs.next()) {
				
				// Get ID of the quest and ID of its state
				String questId = rs.getString("name");
				String stateId = rs.getString("value");
				
				// Search quest associated with the ID
				Quest q = QuestManager.getInstance().getQuest(questId);
				if (q == null) {
					_log.finer("Unknown quest "+questId+" for player "+player.getName());
					if (Config.AUTODELETE_INVALID_QUEST_DATA){
                        invalidQuestData.setInt(1, player.getObjectId());
                        invalidQuestData.setString(2, questId);
                        invalidQuestData.executeUpdate();
					}
					continue;
				}
				
				// Identify the state of the quest for the player
				boolean completed = false;
				if (stateId.length() > 0 && stateId.charAt(0) == '*') { // probably obsolete check 
					completed = true;
					stateId = stateId.substring(1);
				}
				if(stateId.equals("Completed")) completed = true;
				
				// Create an object State containing the state of the quest
				State state = q.states.get(stateId);
				if (state == null) {
					_log.finer("Unknown state "+state+" in quest "+questId+" for player "+player.getName());
					if (Config.AUTODELETE_INVALID_QUEST_DATA){
					    invalidQuestData.setInt(1, player.getObjectId());
                        invalidQuestData.setString(2, questId);
                        invalidQuestData.executeUpdate();
					}
					continue;
				}
				// Create a new QuestState for the player that will be added to the player's list of quests
				new QuestState(q, player, state, completed);
			}
			rs.close();
            invalidQuestData.close();
            statement.close();

            // Get list of quests owned by the player from the DB in order to add variables used in the quest.
            statement = con.prepareStatement("SELECT name,var,value FROM character_quests WHERE char_id=?");
            statement.setInt(1,player.getObjectId());
			rs = statement.executeQuery();
			while (rs.next()) {
				String questId = rs.getString("name");
				String var     = rs.getString("var");
				String value   = rs.getString("value");
				// Get the QuestState saved in the loop before
				QuestState qs = player.getQuestState(questId);
				if (qs == null) {
					_log.finer("Lost variable "+var+" in quest "+questId+" for player "+player.getName());
					if (Config.AUTODELETE_INVALID_QUEST_DATA){
					    invalidQuestDataVar.setInt   (1,player.getObjectId());
                        invalidQuestDataVar.setString(2,questId);
                        invalidQuestDataVar.setString(3,var);
                        invalidQuestDataVar.executeUpdate();
					}
					continue;
				}
				// Add parameter to the quest
				qs.setInternal(var, value);
			}
			rs.close();
            invalidQuestDataVar.close();
            statement.close();
			
		} catch (Exception e) {
			_log.log(Level.WARNING, "could not insert char quest:", e);
        } finally {
            try { con.close(); } catch (Exception e) {}
        }
		
		// events
		for (String name : allEventsS.keySet()) {
			player.processQuestEvent(name, "enter");
		}
	}


	/**
	 * Insert in the database the quest for the player.
	 * @param qs : QuestState pointing out the state of the quest
	 * @param var : String designating the name of the variable for the quest
	 * @param value : String designating the value of the variable for the quest
	 */
	public static void createQuestVarInDb(QuestState qs, String var, String value) {
        java.sql.Connection con = null;
        try
        {
            con = L2DatabaseFactory.getInstance().getConnection();
            PreparedStatement statement;
            statement = con.prepareStatement("INSERT INTO character_quests (char_id,name,var,value) VALUES (?,?,?,?)");
            statement.setInt   (1, qs.getPlayer().getObjectId());
            statement.setString(2, qs.getQuest().getName());
            statement.setString(3, var);
            statement.setString(4, value);
	    statement.executeUpdate();
            statement.close();
        } catch (Exception e) {
			_log.log(Level.WARNING, "could not insert char quest:", e);
        } finally {
            try { con.close(); } catch (Exception e) {}
        }
	}
	
	/**
	 * Update the value of the variable "var" for the quest.<BR><BR>
	 * <U><I>Actions :</I></U><BR>
	 * The selection of the right record is made with :
	 * <LI>char_id = qs.getPlayer().getObjectID()</LI>
	 * <LI>name = qs.getQuest().getName()</LI>
	 * <LI>var = var</LI>
	 * <BR><BR>
	 * The modification made is :
	 * <LI>value = parameter value</LI>
	 * @param qs : Quest State
	 * @param var : String designating the name of the variable for quest
	 * @param value : String designating the value of the variable for quest
	 */
    public static void updateQuestVarInDb(QuestState qs, String var, String value) {
        java.sql.Connection con = null;
        try
        {
            con = L2DatabaseFactory.getInstance().getConnection();
            PreparedStatement statement;
            statement = con.prepareStatement("UPDATE character_quests SET value=? WHERE char_id=? AND name=? AND var = ?");
            statement.setString(1, value);
            statement.setInt   (2, qs.getPlayer().getObjectId());
            statement.setString(3, qs.getQuest().getName());
            statement.setString(4, var);
			statement.executeUpdate();
            statement.close();
        } catch (Exception e) {
			_log.log(Level.WARNING, "could not update char quest:", e);
        } finally {
            try { con.close(); } catch (Exception e) {}
        }
	}
	
    /**
     * Delete a variable of player's quest from the database.
     * @param qs : object QuestState pointing out the player's quest
     * @param var : String designating the variable characterizing the quest
     */
	public static void deleteQuestVarInDb(QuestState qs, String var) {
        java.sql.Connection con = null;
        try
        {
            con = L2DatabaseFactory.getInstance().getConnection();
            PreparedStatement statement;
            statement = con.prepareStatement("DELETE FROM character_quests WHERE char_id=? AND name=? AND var=?");
            statement.setInt   (1, qs.getPlayer().getObjectId());
            statement.setString(2, qs.getQuest().getName());
            statement.setString(3, var);
	    statement.executeUpdate();
            statement.close();
        } catch (Exception e) {
			_log.log(Level.WARNING, "could not delete char quest:", e);
        } finally {
            try { con.close(); } catch (Exception e) {}
        }
	}
	
	/**
	 * Delete the player's quest from database.
	 * @param qs : QuestState pointing out the player's quest
	 */
	public static void deleteQuestInDb(QuestState qs) {
        java.sql.Connection con = null;
        try
        {
            con = L2DatabaseFactory.getInstance().getConnection();
            PreparedStatement statement;
            statement = con.prepareStatement("DELETE FROM character_quests WHERE char_id=? AND name=?");
            statement.setInt   (1, qs.getPlayer().getObjectId());
            statement.setString(2, qs.getQuest().getName());
			statement.executeUpdate();
            statement.close();
        } catch (Exception e) {
			_log.log(Level.WARNING, "could not delete char quest:", e);
        } finally {
            try { con.close(); } catch (Exception e) {}
        }
	}
	
	/**
	 * Create a record in database for quest.<BR><BR>
	 * <U><I>Actions :</I></U><BR>
	 * Use fucntion createQuestVarInDb() with following parameters :<BR>
	 * <LI>QuestState : parameter sq that puts in fields of database :
	 * 	 <UL type="square">
	 *     <LI>char_id : ID of the player</LI>
	 *     <LI>name : name of the quest</LI>
	 *   </UL>
	 * </LI>
	 * <LI>var : string "&lt;state&gt;" as the name of the variable for the quest</LI>
	 * <LI>val : string corresponding at the ID of the state (in fact, initial state)</LI>
	 * @param qs : QuestState
	 */
	public static void createQuestInDb(QuestState qs) {
		createQuestVarInDb(qs, "<state>", qs.getStateId());
	}
	
	/**
	 * Update informations regarding quest in database.<BR>
	 * <U><I>Actions :</I></U><BR>
	 * <LI>Get ID state of the quest recorded in object qs</LI>
	 * <LI>Test if quest is completed. If true, add a star (*) before the ID state</LI>
	 * <LI>Save in database the ID state (with or without the star) for the variable called "&lt;state&gt;" of the quest</LI>
	 * @param qs : QuestState
	 */
	public static void updateQuestInDb(QuestState qs) {
		String val = qs.getStateId();
		//if (qs.isCompleted())
		//	val = "*" + val;
		updateQuestVarInDb(qs, "<state>", val);
	}
	
    /**
     * Add this quest to the list of quests that the passed mob will respond to for Attack Events.<BR><BR>
     * @param attackId
     * @return int : attackId
     */
    public int addAttackId(int attackId) {
    	NpcTable.getInstance().getTemplate(attackId).addQuestEvent(Quest.QuestEventType.MOBGOTATTACKED, this);
        return attackId;
    }
    
	/**
     * Add this quest to the list of quests that the passed mob will respond to for Kill Events.<BR><BR>
	 * @param killId
	 * @return int : killId
	 */
	public int addKillId(int killId) {
    	NpcTable.getInstance().getTemplate(killId).addQuestEvent(Quest.QuestEventType.MOBKILLED, this);
		return killId;
	}
	
    /**
     * Add this quest to the list of quests that the passed npc will respond to for Talk Events.<BR><BR>
     * @param talkId : ID of the NPC
     * @return int : ID of the NPC
     */
    public int addTalkId(int talkId) {
    	NpcTable.getInstance().getTemplate(talkId).addQuestEvent(Quest.QuestEventType.QUEST_TALK, this);
        return talkId;
    }
    
    /**
     * Return a QuestPcSpawn for the given player instance
     */
    public final QuestPcSpawn getPcSpawn(L2PcInstance player)
    {
        return QuestPcSpawnManager.getInstance().getPcSpawn(player);
    }
    
    // returns a random party member's L2PcInstance for the passed player's party
    // returns the passed player if he has no party.
    public L2PcInstance getRandomPartyMember(L2PcInstance player)
    {
    	// NPE prevention.  If the player is null, there is nothing to return
    	if (player == null )
    		return null;
    	if ((player.getParty() == null) || (player.getParty().getPartyMembers().size()==0))
    		return player;
    	L2Party party = player.getParty();
    	return party.getPartyMembers().get(Rnd.get(party.getPartyMembers().size()));
    }

    /**
     * Auxilary function for party quests. 
     * Note: This function is only here because of how commonly it may be used by quest developers.
     * For any variations on this function, the quest script can always handle things on its own
     * @param player: the instance of a player whose party is to be searched
     * @param value: the value of the "cond" variable that must be matched
     * @return L2PcInstance: L2PcInstance for a random party member that matches the specified 
     * 			condition, or null if no match.
     */
    public L2PcInstance getRandomPartyMember(L2PcInstance player, String value)
    {
    	return getRandomPartyMember(player, "cond", value);
    }

    /**
     * Auxilary function for party quests. 
     * Note: This function is only here because of how commonly it may be used by quest developers.
     * For any variations on this function, the quest script can always handle things on its own
     * @param player: the instance of a player whose party is to be searched
     * @param var/value: a tuple specifying a quest condition that must be satisfied for
     *     a party member to be considered.
     * @return L2PcInstance: L2PcInstance for a random party member that matches the specified 
     * 				condition, or null if no match.  If the var is null, any random party 
     * 				member is returned (i.e. no condition is applied).
     */
    public L2PcInstance getRandomPartyMember(L2PcInstance player, String var, String value)
    {
    	// if no valid player instance is passed, there is nothing to check...
    	if (player == null)
    		return null;
    	
    	// for null var condition, return any random party member.
    	if (var == null) 
    		return getRandomPartyMember(player);
    	
    	// normal cases...if the player is not in a partym check the player's state
    	QuestState temp = null;
    	L2Party party = player.getParty();
    	// if this player is not in a party, just check if this player instance matches the conditions itself
    	if ( (party == null) || (party.getPartyMembers().size()==0) )
    	{
    		temp = player.getQuestState(getName());
    		if( (temp != null) && (temp.get(var)!=null) && ((String) temp.get(var)).equalsIgnoreCase(value) )
				return player;  // match
    		
    		return null;	// no match
    	}
    	
    	// if the player is in a party, gather a list of all matching party members (possibly 
    	// including this player) 
    	FastList<L2PcInstance> candidates = new FastList<L2PcInstance>();
    	for(L2PcInstance partyMember: party.getPartyMembers())
    	{
    		temp = partyMember.getQuestState(getName());
    		if( (temp != null) && (temp.get(var)!=null) && ((String) temp.get(var)).equalsIgnoreCase(value) )
    			candidates.add(partyMember);
    	}
    	// if there was no match, return null...
    	if (candidates.size()==0)
    		return null;
    	
    	// if a match was found from the party, return one of them at random.
    	return candidates.get( Rnd.get(candidates.size()) );
    }

    /**
     * Auxilary function for party quests. 
     * Note: This function is only here because of how commonly it may be used by quest developers.
     * For any variations on this function, the quest script can always handle things on its own
     * @param player: the instance of a player whose party is to be searched
     * @param state: the state in which the party member's queststate must be in order to be considered.
     * @return L2PcInstance: L2PcInstance for a random party member that matches the specified 
     * 				condition, or null if no match.  If the var is null, any random party 
     * 				member is returned (i.e. no condition is applied).
     */
    public L2PcInstance getRandomPartyMemberState(L2PcInstance player, State state)
    {
    	// if no valid player instance is passed, there is nothing to check...
    	if (player == null)
    		return null;
    	
    	// for null var condition, return any random party member.
    	if (state == null) 
    		return getRandomPartyMember(player);
    	
    	// normal cases...if the player is not in a partym check the player's state
    	QuestState temp = null;
    	L2Party party = player.getParty();
    	// if this player is not in a party, just check if this player instance matches the conditions itself
    	if ( (party == null) || (party.getPartyMembers().size()==0) )
    	{
    		temp = player.getQuestState(getName());
    		if( (temp != null) && (temp.getState() == state) )
				return player;  // match
    		
    		return null;	// no match
    	}
    	
    	// if the player is in a party, gather a list of all matching party members (possibly 
    	// including this player) 
    	FastList<L2PcInstance> candidates = new FastList<L2PcInstance>();
    	for(L2PcInstance partyMember: party.getPartyMembers())
    	{
    		temp = partyMember.getQuestState(getName());
    		if( (temp != null) && (temp.getState() == state) )
    			candidates.add(partyMember);
    	}
    	// if there was no match, return null...
    	if (candidates.size()==0)
    		return null;
    	
    	// if a match was found from the party, return one of them at random.
    	return candidates.get( Rnd.get(candidates.size()) );
    }

	/**
	 * Show HTML file to client
	 * @param fileName
	 * @return String : message sent to client 
	 */
	public String showHtmlFile(L2PcInstance player, String fileName) 
    {
        String questId = getName();

        //Create handler to file linked to the quest
        String directory    = getDescr().toLowerCase();
        String content = HtmCache.getInstance().getHtm("data/jscript/" + directory + "/" + questId + "/"+fileName);
        
        if (content == null)
            content = HtmCache.getInstance().getHtmForce("data/jscript/quests/"+questId+"/"+fileName);

        if (player != null && player.getTarget() != null)
            content = content.replaceAll("%objectId%", String.valueOf(player.getTarget().getObjectId()));

        //Send message to client if message not empty     
         if (content != null) 
         {
             NpcHtmlMessage npcReply = new NpcHtmlMessage(5);
             npcReply.setHtml(content);
             player.sendPacket(npcReply);
         }
         
         return content;
	}
}
