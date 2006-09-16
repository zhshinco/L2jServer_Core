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

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.ClientThread;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.serverpackets.SystemMessage;

/**
 * This class ...
 * 
 * @version $Revision: 1.3.4.3 $ $Date: 2005/03/27 15:29:30 $
 */
public class RequestFriendList extends ClientBasePacket
{
	private static Logger _log = Logger.getLogger(RequestFriendList.class.getName());
	private static final String _C__60_REQUESTFRIENDLIST = "[C] 60 RequestFriendList";
	
	/**
	 * packet type id 0x60
	 * format:		c
	 * @param rawPacket
	 */
	
	public RequestFriendList(ByteBuffer buf, ClientThread client)
	{
		super(buf, client);
	}
	
	void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();
		
		if (activeChar == null)  
			return;
		
		SystemMessage sm;
		java.sql.Connection con = null;
		
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT friend_id, friend_name FROM character_friends WHERE char_id=?");
			statement.setInt(1, activeChar.getObjectId());
            
			ResultSet rset = statement.executeQuery();
			
			//======<Friend List>======
			activeChar.sendPacket(new SystemMessage(SystemMessage.FRIEND_LIST_HEAD));
			
            L2PcInstance friend = null;
			while (rset.next())
			{
				int friendId = rset.getInt("friend_id");
				String friendName = rset.getString("friend_name");
				friend = (L2PcInstance)L2World.getInstance().findObject(friendId);
					
				if (friend == null)
				{
				    //	(Currently: Offline)
				    sm = new SystemMessage(SystemMessage.S1_OFFLINE);
				    sm.addString(friendName);
				}
				else
				{
				    //(Currently: Online)
				    sm = new SystemMessage(SystemMessage.S1_ONLINE);
				    sm.addString(friendName);
				}
				
				activeChar.sendPacket(sm);
			}
			
			//=========================
			activeChar.sendPacket(new SystemMessage(SystemMessage.FRIEND_LIST_FOOT));
			
			rset.close();
			statement.close();
		}
		catch (Exception e) {
			_log.warning("Error in /friendlist for " + activeChar + ": " + e);
		}
		finally	{
			try {con.close();} catch (Exception e) {}
		}
	}
	
	public String getType()
	{
		return _C__60_REQUESTFRIENDLIST;
	}
}
