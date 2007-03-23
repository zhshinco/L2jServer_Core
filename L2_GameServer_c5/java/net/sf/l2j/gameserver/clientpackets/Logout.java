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
package net.sf.l2j.gameserver.clientpackets;

import java.nio.ByteBuffer;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.ClientThread;
import net.sf.l2j.gameserver.SevenSignsFestival;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.model.L2Party;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.serverpackets.FriendList;
import net.sf.l2j.gameserver.serverpackets.LeaveWorld;
import net.sf.l2j.gameserver.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.taskmanager.AttackStanceTaskManager;

/**
 * This class ...
 * 
 * @version $Revision: 1.9.4.3 $ $Date: 2005/03/27 15:29:30 $
 */
public class Logout extends ClientBasePacket
{
    private static final String _C__09_LOGOUT = "[C] 09 Logout";
    private static Logger _log = Logger.getLogger(Logout.class.getName());
    
    // c

    /**
     * @param decrypt
     */
    public Logout(ByteBuffer buf, ClientThread client)
    {
        super(buf, client);
        // this is just a trigger packet. it has no content
    }

    void runImpl()
    {
        // Dont allow leaving if player is fighting
        L2PcInstance player = getClient().getActiveChar();
        
        if (player == null)
            return;
	
	player.getInventory().updateDatabase();

        if(AttackStanceTaskManager.getInstance().getAttackStanceTask(player))
        {
            if (Config.DEBUG) _log.fine("Player " + player.getName() + " tried to logout while fighting");
            
            player.sendPacket(new SystemMessage(SystemMessage.CANT_LOGOUT_WHILE_FIGHTING));
            player.sendPacket(new ActionFailed());
            return;
        }
        
        if(player.atEvent) {
            player.sendPacket(SystemMessage.sendString("A superior power doesn't allow you to leave the event"));
            return;
        }
   
     
        // Prevent player from logging out if they are a festival participant
        // and it is in progress, otherwise notify party members that the player
        // is not longer a participant.
        if (player.isFestivalParticipant()) {
            if (SevenSignsFestival.getInstance().isFestivalInitialized()) 
            {
                player.sendMessage("You cannot log out while you are a participant in a festival.");
                return;
            }
            L2Party playerParty = player.getParty();
            
            if (playerParty != null)
                player.getParty().broadcastToPartyMembers(SystemMessage.sendString(player.getName() + " has been removed from the upcoming festival."));
        }
        if (player.isFlying()) 
        { 
        	player.removeSkill(SkillTable.getInstance().getInfo(4289, 1));
        }
        player.deleteMe();
        notifyFriends(player);
        //save character
        ClientThread.saveCharToDisk(player);
        
        // normally the server would send serveral "delete object" before "leaveWorld"
        // we skip that for now
        
        LeaveWorld ql = new LeaveWorld();
        sendPacket(ql);
    }

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
            
            while (rset.next())
            {
            	friendName = rset.getString("friend_name");

            	friend = L2World.getInstance().getPlayer(friendName);

                if (friend != null) //friend logged in.
                {
                	friend.sendPacket(new FriendList(friend));
                }
		    }
        } 
		catch (Exception e) {
            _log.warning("could not restore friend data:"+e);
        } 
		finally {
            try {con.close();} catch (Exception e){}
        }
	}

    /* (non-Javadoc)
     * @see net.sf.l2j.gameserver.clientpackets.ClientBasePacket#getType()
     */
    public String getType()
    {
        return _C__09_LOGOUT;
    }
}