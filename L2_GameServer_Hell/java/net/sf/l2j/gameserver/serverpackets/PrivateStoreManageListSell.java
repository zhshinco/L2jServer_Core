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
package net.sf.l2j.gameserver.serverpackets;

import net.sf.l2j.gameserver.model.TradeList;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;


/**
 * 3 section to this packet
 * 1)playerinfo which is always sent
 * dd
 *
 * 2)list of items which can be added to sell
 * d(hhddddhhhd)
 *
 * 3)list of items which have already been setup
 * for sell in previous sell private store sell manageent
 * d(hhddddhhhdd) *
 * @version $Revision: 1.3.2.1.2.3 $ $Date: 2005/03/27 15:29:39 $
 */
public class PrivateStoreManageListSell extends L2GameServerPacket
{
	private static final String _S__B3_PRIVATESELLLISTSELL = "[S] a0 PrivateSellListSell";
	private L2PcInstance _activeChar;
	private int _playerAdena;
	private boolean _packageSale;
	private TradeList.TradeItem[] _itemList;
	private TradeList.TradeItem[] _sellList;

	public PrivateStoreManageListSell(L2PcInstance player)
	{
		_activeChar = player;
		_playerAdena = _activeChar.getAdena();
		_activeChar.getSellList().updateItems();
		_packageSale = _activeChar.getSellList().isPackaged();
		_itemList = _activeChar.getInventory().getAvailableItems(_activeChar.getSellList());
		_sellList = _activeChar.getSellList().getItems();
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0xa0);
		//section 1
		writeD(_activeChar.getObjectId());
		writeD(_packageSale ? 1 : 0); // Package sell
		writeD(_playerAdena);

		//section2
		writeD(_itemList.length); //for potential sells
		for (TradeList.TradeItem item : _itemList)
		{
			writeD(item.getItem().getType2());
			writeD(item.getObjectId());
			writeD(item.getItem().getItemId());
			writeD(item.getCount());
			writeH(0);
			writeH(item.getEnchant());//enchant lvl
			writeH(0);
			writeD(item.getItem().getBodyPart());
			writeD(item.getPrice()); //store price
			
			// T1
            writeD(item.getAttackAttrElement());
            writeD(item.getAttackAttrElementVal());
            writeD(item.getDefAttrFire());
            writeD(item.getDefAttrWater());
            writeD(item.getDefAttrWind());
            writeD(item.getDefAttrEarth());
            writeD(item.getDefAttrHoly());
            writeD(item.getDefAttrUnholy());
		}
		//section 3
		writeD(_sellList.length); //count for any items already added for sell
		for (TradeList.TradeItem item : _sellList)
		{
			writeD(item.getItem().getType2());
			writeD(item.getObjectId());
			writeD(item.getItem().getItemId());
			writeD(item.getCount());
			writeH(0);
			writeH(item.getEnchant());//enchant lvl
			writeH(0x00);
			writeD(item.getItem().getBodyPart());
			writeD(item.getPrice());//your price
			writeD(item.getItem().getReferencePrice()); //store price
			
			// T1
            writeD(item.getAttackAttrElement());
            writeD(item.getAttackAttrElementVal());
            writeD(item.getDefAttrFire());
            writeD(item.getDefAttrWater());
            writeD(item.getDefAttrWind());
            writeD(item.getDefAttrEarth());
            writeD(item.getDefAttrHoly());
            writeD(item.getDefAttrUnholy());
		}
	}

	/* (non-Javadoc)
	 * @see net.sf.l2j.gameserver.serverpackets.ServerBasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _S__B3_PRIVATESELLLISTSELL;
	}
}
