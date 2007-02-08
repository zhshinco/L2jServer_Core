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
package net.sf.l2j.gameserver.model.actor.instance;

/**
 * @author NightMarez
 * @version $Revision: 1.3.2.2.2.5 $ $Date: 2005/03/27 15:29:32 $
 *
 */

import java.util.StringTokenizer;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.datatables.TeleportLocationTable;
import net.sf.l2j.gameserver.model.L2CharPosition;
import net.sf.l2j.gameserver.model.L2TeleportLocation;
import net.sf.l2j.gameserver.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.serverpackets.MyTargetSelected;
import net.sf.l2j.gameserver.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.templates.L2NpcTemplate;

public final class L2CastleTeleporterInstance extends L2FolkInstance
{
	//private static Logger _log = Logger.getLogger(L2TeleporterInstance.class.getName());

	private static int Cond_All_False = 0;
	private static int Cond_Busy_Because_Of_Siege = 1;
	private static int Cond_Owner = 2;
	private static int Cond_Regular = 3;
	
	/**
	 * @param template
	 */
	public L2CastleTeleporterInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}
	
	public void onAction(L2PcInstance player)
	{
		player.sendPacket(new ActionFailed());
		player.setTarget(this);
		player.sendPacket(new MyTargetSelected(getObjectId(), -15));
	
		if (isInsideRadius(player, INTERACTION_DISTANCE, false, false))
			showChatWindow(player);
	}
	
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		player.sendPacket( new ActionFailed() );

		int condition = validateCondition(player);
		if (condition <= Cond_Busy_Because_Of_Siege)
            return;

        StringTokenizer st = new StringTokenizer(command, " ");
        String actualCommand = st.nextToken(); // Get actual command

		if (actualCommand.equalsIgnoreCase("goto"))
		{
			if (st.countTokens() <= 0) {return;}
			int whereTo = Integer.parseInt(st.nextToken());
		    if (condition == Cond_Regular)
            {
                doTeleport(player, whereTo);
                return;
            }
		    else if (condition == Cond_Owner)
		    {
				int minPrivilegeLevel = 0; // NOTE: Replace 0 with highest level when privilege level is implemented
				if (st.countTokens() >= 1) {minPrivilegeLevel = Integer.parseInt(st.nextToken());}
				if (10 >= minPrivilegeLevel) // NOTE: Replace 10 with privilege level of player
					doTeleport(player, whereTo);
				else
				    player.sendMessage("You do not sufficient access level to teleport there.");
                return;
		    }
		}

        super.onBypassFeedback(player, command);
	}
	public String getHtmlPath(int npcId, int val)
	{
		String pom = "";
		if (val == 0)
		{
			pom = "" + npcId;
		} 
		else 
		{
			pom = npcId + "-" + val;
		}
		
		return "data/html/teleporter/" + pom + ".htm";
	}

	
	public void showChatWindow(L2PcInstance player)
	{
		String filename = "data/html/teleporter/castleteleporter-no.htm";
		
		int condition = validateCondition(player);
		if (condition == Cond_Regular)
		{
		    super.showChatWindow(player);
		    return;
		}
		else if (condition > Cond_All_False)
		{
	        if (condition == Cond_Busy_Because_Of_Siege)
	            filename = "data/html/teleporter/castleteleporter-busy.htm";		// Busy because of siege
	        else if (condition == Cond_Owner)										// Clan owns castle
	            filename = "data/html/teleporter/" + getNpcId() + ".htm";		// Owner message window
		}

		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(filename);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		html.replace("%npcname%", getName());
		player.sendPacket(html);
	}

	private void doTeleport(L2PcInstance player, int val)
	{
		L2TeleportLocation list = TeleportLocationTable.getInstance().getTemplate(val);
		if (list != null)
		{
			if(player.reduceAdena("Teleport", list.getPrice(), player.getLastFolkNPC(), true))
			{
				if (Config.DEBUG)
					_log.fine("Teleporting player "+player.getName()+" to new location: "+list.getLocX()+":"+list.getLocY()+":"+list.getLocZ());
                
				// teleport
				player.teleToLocation(list.getLocX(), list.getLocY(), list.getLocZ(), true);
				player.stopMove(new L2CharPosition(list.getLocX(), list.getLocY(), list.getLocZ(), player.getHeading()));
			}
		}
		else
		{
			_log.warning("No teleport destination with id:" +val);
		}
		player.sendPacket( new ActionFailed() );
	}

	private int validateCondition(L2PcInstance player)
	{
		if (player.getClan() != null && getCastle() != null)
		{
	        if (getCastle().getSiege().getIsInProgress())
	            return Cond_Busy_Because_Of_Siege;										// Busy because of siege
	        else if (getCastle().getOwnerId() == player.getClanId())					// Clan owns castle
	            return Cond_Owner;	// Owner
		}
		
		return Cond_All_False;
	}
}
