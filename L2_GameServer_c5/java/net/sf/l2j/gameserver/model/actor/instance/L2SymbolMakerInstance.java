/*
 * $Header$
 *
 * 
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

import javolution.lang.TextBuilder;
import net.sf.l2j.Config;
import net.sf.l2j.gameserver.HennaTreeTable;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2HennaInstance;
import net.sf.l2j.gameserver.serverpackets.HennaEquipList;
import net.sf.l2j.gameserver.templates.L2NpcTemplate;

/**
 * This class ...
 * 
 * @version $Revision$ $Date$
 */
public class L2SymbolMakerInstance extends L2FolkInstance
{
	//private static Logger _log = Logger.getLogger(L2SymbolMakerInstance.class.getName());
	
	private double _collisionRadius;   
	private double _collisionHeight; // this is  positioning the model relative to the ground
	
	/**
	 * @return Returns the zOffset.
	 */
	public double getCollisionHeight()
	{
		return _collisionHeight;
	}
	
	/**
	 * @param offset The zOffset to set.
	 */
	public void setCollisionHeight(double offset)
	{
		_collisionHeight = offset;
	}
	
	
	
	/**
	 * @return Returns the collisionRadius.
	 */
	public double getCollisionRadius()
	{
		return _collisionRadius;
	}
	
	/**
	 * @param collisionRadius The collisionRadius to set.
	 */
	public void setCollisionRadius(double collisionRadius)
	{
		_collisionRadius = collisionRadius;
	}
	
	/**
	 * @return Returns the unknown1.
	 */
    
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		if (command.equals("Draw"))
		{
			L2HennaInstance[] henna = HennaTreeTable.getInstance().getAvailableHenna(player.getClassId());
			HennaEquipList hel = new HennaEquipList(player, henna);
			player.sendPacket(hel);
		}
		else if (command.equals("RemoveList"))
        {
			showRemoveChat(player);
		}
		else if (command.startsWith("Remove "))
		{
			int slot = Integer.parseInt(command.substring(7));
			player.removeHenna(slot);
		}
		else 
        {
			super.onBypassFeedback(player, command);
		}
	}
	
	private void showRemoveChat(L2PcInstance player){
        TextBuilder html1 = new TextBuilder("<html><body>");
        html1.append("Select symbol you would like to remove:<br><br>");
        boolean hasHennas = false;
        
        for (int i=1;i<=3;i++)
        {
        	L2HennaInstance henna = player.getHenna(i);
            
        	if (henna != null)
            {
        		hasHennas = true;
        		html1.append("<a action=\"bypass -h npc_%objectId%_Remove "+i+"\">"+henna.getName()+"</a><br>");
        	}
        }
        
        if (!hasHennas)
        	html1.append("You don't have any symbol to remove!");

        html1.append("</body></html>");
        
		insertObjectIdAndShowChatWindow(player, html1.toString());
	}
	
	public L2SymbolMakerInstance(int objectID, L2NpcTemplate template)
	{
		super(objectID, template);
	}
	
	/**
	 * this is called when a player interacts with this NPC
	 * @param player
	 */
	public void onAction(L2PcInstance player)
	{
		if (Config.DEBUG) _log.fine("Symbol Maker activated");
		player.setLastFolkNPC(this);
		super.onAction(player);
	}
	
	public String getHtmlPath(int npcId, int val)
	{
		String pom = "";
        
		if (val == 0)
			pom = "" + npcId;
		else 
			pom = npcId + "-" + val;
		
		return "data/html/symbolmaker/" + pom + ".htm";
	}
	
	
    /* (non-Javadoc)
     * @see net.sf.l2j.gameserver.model.L2Object#isAttackable()
     */
    public boolean isAutoAttackable(@SuppressWarnings("unused") L2Character attacker)
    {
        return false;
    }
}
