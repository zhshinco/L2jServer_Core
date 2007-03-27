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
package net.sf.l2j.gameserver.handler.admincommandhandlers;

import java.util.StringTokenizer;

import javolution.text.TextBuilder;
import net.sf.l2j.Config;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.GMAudit;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.serverpackets.ItemList;
import net.sf.l2j.gameserver.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.serverpackets.SystemMessage;

/**
 * This class handles following admin commands:
 * - itemcreate = show menu
 * - create_item id [num] = creates num items with respective id, if num is not specified num = 1
 * 
 * @version $Revision: 1.2.2.2.2.3 $ $Date: 2005/04/11 10:06:06 $
 */
public class AdminCreateItem implements IAdminCommandHandler {

	private static String[] _adminCommands = {
			"admin_itemcreate",
			"admin_create_item"
			};
	private static final int REQUIRED_LEVEL = Config.GM_CREATE_ITEM;

	public boolean useAdminCommand(String command, L2PcInstance activeChar) {
        if (!Config.ALT_PRIVILEGES_ADMIN)
            if (!(checkLevel(activeChar.getAccessLevel()) && activeChar.isGM())) return false;
		
		if (command.equals("admin_itemcreate"))
		{
			AdminHelpPage.showHelpPage(activeChar, "itemcreation.htm");
		}		
		else if (command.startsWith("admin_create_item"))
		{
			try
			{
				String val = command.substring(17);
				StringTokenizer st = new StringTokenizer(val);
				if (st.countTokens()== 2)
				{
					String id = st.nextToken();
					int idval = Integer.parseInt(id);
					String num = st.nextToken();
					int numval = Integer.parseInt(num);
					createItem(activeChar,idval,numval);
				}
				else if (st.countTokens()== 1)
				{
					String id = st.nextToken();
					int idval = Integer.parseInt(id);
					createItem(activeChar,idval,1);
				}
				else
				{
					AdminHelpPage.showHelpPage(activeChar, "itemcreation.htm");
				}
			}
			catch (StringIndexOutOfBoundsException e)
			{
				SystemMessage sm = new SystemMessage(614);
				sm.addString("Error while creating item.");
				activeChar.sendPacket(sm);
			} catch (NumberFormatException nfe)
			{
				SystemMessage sm = new SystemMessage(614);
				sm.addString("Wrong number entered.");
				activeChar.sendPacket(sm);
			}
		}
		String target = (activeChar.getTarget() != null?activeChar.getTarget().getName():"no-target");
        GMAudit.auditGMAction(activeChar.getName(), command, target, "");
		return true;
	}
	
	public String[] getAdminCommandList() {
		return _adminCommands;
	}
	
	private boolean checkLevel(int level) {
		return (level >= REQUIRED_LEVEL);
	}
	
	private void createItem(L2PcInstance activeChar, int id, int num)
	{
		activeChar.getInventory().addItem("Admin", id, num, activeChar, null);

		ItemList il = new ItemList(activeChar, true);
		activeChar.sendPacket(il);
		
		SystemMessage sm = new SystemMessage(614);
		sm.addString("You have spawned " + num + " item(s) number " + id + " in your inventory.");
		activeChar.sendPacket(sm);
				
		NpcHtmlMessage adminReply = new NpcHtmlMessage(5);		
		TextBuilder replyMSG = new TextBuilder("<html><body>");
		replyMSG.append("<table width=260><tr>");
		replyMSG.append("<td width=40><button value=\"Main\" action=\"bypass -h admin_admin\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("<td width=180><center>Item Creation Menu</center></td>");
		replyMSG.append("<td width=40><button value=\"Back\" action=\"bypass -h admin_help itemcreation.htm\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("</tr></table>");
		replyMSG.append("<br><br>");
		replyMSG.append("<table width=270><tr><td>Item Creation Complete.<br></td></tr></table>");
		replyMSG.append("<table width=270><tr><td>You have spawned " + num + " item(s) number " + id + " in your inventory.</td></tr></table>");
		replyMSG.append("</body></html>");
		
		adminReply.setHtml(replyMSG.toString());
		activeChar.sendPacket(adminReply);
	}
}
