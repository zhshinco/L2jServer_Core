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
package net.sf.l2j.gameserver.handler.itemhandlers;

import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2NpcInstance;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PlayableInstance;
import net.sf.l2j.gameserver.serverpackets.SystemMessage;

public class BeastSpice implements IItemHandler
{
	// Golden Spice, Crystal Spice
    private static int[] _itemIds = { 6643, 6644 };

	public void useItem(L2PlayableInstance playable, L2ItemInstance item)
	{
		if (!(playable instanceof L2PcInstance))
			return;
		
		L2PcInstance activeChar = (L2PcInstance)playable;

		if (!(activeChar.getTarget() instanceof L2NpcInstance))
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.TARGET_IS_INCORRECT));
			return;				
		}

		L2Object[] targets = new L2Object[1];
		targets[0] = activeChar.getTarget();
		
	    int itemId = item.getItemId();
		if (itemId == 6643) { // Golden Spice
			activeChar.useMagic(SkillTable.getInstance().getInfo(2188,1),false,false);
		}
		else if (itemId == 6644) { // Crystal Spice
			activeChar.useMagic(SkillTable.getInstance().getInfo(2189,1),false,false);
		}
	}

    public int[] getItemIds() 
	{ 
		return _itemIds; 
	} 
}