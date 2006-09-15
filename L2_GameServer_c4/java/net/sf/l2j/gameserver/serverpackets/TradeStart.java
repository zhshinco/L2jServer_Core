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
package net.sf.l2j.gameserver.serverpackets;

import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

/**
 * This class ...
 * 
 * @version $Revision: 1.4.2.1.2.3 $ $Date: 2005/03/27 15:29:39 $
 */
public class TradeStart extends ServerBasePacket{
	private static final String _S__2E_TRADESTART = "[S] 1E TradeStart";
	private L2PcInstance _player;
	private L2ItemInstance[] _itemList;
	public TradeStart (L2PcInstance player)
	{
        _player = player;
	}
	
	final void runImpl()
	{
		_itemList = _player.getInventory().getAvailableItems(true);
	}
	
	final void writeImpl()
	{//0x2e TradeStart   d h (h dddhh dhhh)
		if (_player.getActiveTradeList() == null || _player.getActiveTradeList().getPartner() == null)
			return;
		
		writeC(0x1E);
		writeD(_player.getActiveTradeList().getPartner().getObjectId());
		//writeD((_char != null || _char.getTransactionRequester() != null)? _char.getTransactionRequester().getObjectId() : 0);
		
		writeH(_itemList.length);
		for (L2ItemInstance item : _itemList)//int i = 0; i < count; i++)
		{
			writeH(item.getItem().getType1()); // item type1
			writeD(item.getObjectId());
			writeD(item.getItemId());
			writeD(item.getCount());
			writeH(item.getItem().getType2());	// item type2
			writeH(0x00);	// ?

			writeD(item.getItem().getBodyPart());	// rev 415  slot    0006-lr.ear  0008-neck  0030-lr.finger  0040-head  0080-??  0100-l.hand  0200-gloves  0400-chest  0800-pants  1000-feet  2000-??  4000-r.hand  8000-r.hand
			writeH(item.getEnchantLevel());	// enchant level
			writeH(0x00);	// ?
			writeH(0x00);
		}
	}

	/* (non-Javadoc)
	 * @see net.sf.l2j.gameserver.serverpackets.ServerBasePacket#getType()
	 */
	public String getType()
	{
		return _S__2E_TRADESTART;
	}
}
