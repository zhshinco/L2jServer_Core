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
package net.sf.l2j.gameserver.handler.itemhandlers;

import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PlayableInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.serverpackets.ChooseInventoryItem;
import net.sf.l2j.gameserver.serverpackets.SystemMessage;

public class EnchantScrolls implements IItemHandler
{

	private static final int[] ITEM_IDS = {
        729, 730, 731, 732, 6569, 6570, // a grade
        947, 948, 949, 950, 6571, 6572, // b grade
        951, 952, 953, 954, 6573, 6574, // c grade
        955, 956, 957, 958, 6575, 6576, // d grade
        959, 960, 961, 962, 6577, 6578  // s grade
	};


	public void useItem(L2PlayableInstance playable, L2ItemInstance item)
	{
		if (!(playable instanceof L2PcInstance)) return;
		L2PcInstance activeChar = (L2PcInstance)playable;
        if(activeChar.isCastingNow()) return;

		activeChar.setActiveEnchantItem(item);
		activeChar.sendPacket(new SystemMessage(SystemMessageId.SELECT_ITEM_TO_ENCHANT));
		activeChar.sendPacket(new ChooseInventoryItem(item.getItemId()));
		return;
	}

	public int[] getItemIds()
	{
		return ITEM_IDS;
	}
}
