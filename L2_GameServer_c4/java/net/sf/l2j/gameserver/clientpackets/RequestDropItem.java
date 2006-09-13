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
import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.ClientThread;
import net.sf.l2j.gameserver.GmListTable;
import net.sf.l2j.gameserver.model.GMAudit;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.serverpackets.InventoryUpdate;
import net.sf.l2j.gameserver.serverpackets.ItemList;
import net.sf.l2j.gameserver.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.templates.L2Item;
import net.sf.l2j.gameserver.util.IllegalPlayerAction;
import net.sf.l2j.gameserver.util.Util;

/**
 * This class ...
 * 
 * @version $Revision: 1.11.2.1.2.7 $ $Date: 2005/04/02 21:25:21 $
 */
public class RequestDropItem extends ClientBasePacket
{
	private static final String _C__12_REQUESTDROPITEM = "[C] 12 RequestDropItem";
	private static Logger _log = Logger.getLogger(RequestDropItem.class.getName());

	private final int _objectId;
	private final int _count;
	private final int _x;
	private final int _y;
	private final int _z;
	/**
	 * packet type id 0x12
	 * 
	 * sample
	 * 
	 * 12 
	 * 09 00 00 40 		// object id
	 * 01 00 00 00 		// count ??
	 * fd e7 fe ff 		// x
	 * e5 eb 03 00 		// y
	 * bb f3 ff ff 		// z 
	 * 
	 * format:		cdd ddd 
	 * @param decrypt
	 */
	public RequestDropItem(ByteBuffer buf, ClientThread client)
	{
		super(buf, client);
		_objectId = readD();
		_count    = readD();
		_x        = readD();
		_y        = readD();
		_z        = readD();
	}

	void runImpl()
	{
        L2PcInstance activeChar = getClient().getActiveChar();
    	if (activeChar == null) return;
        L2ItemInstance item = activeChar.getInventory().getItemByObjectId(_objectId);
        
        if (item == null || _count == 0 
        		|| !Config.ALLOW_DISCARDITEM || Config.LIST_NONDROPPABLE_ITEMS.contains(item.getItemId())
        		|| !activeChar.validateItemManipulation(_objectId, "drop"))
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.NOTHING_HAPPENED));
			return;
		}
        
        int itemId = item.getItemId();
        
        if ((itemId >= 6611 && itemId <= 6621) || itemId == 6842)
            return;
        
        if(_count < 0)
        {
        	Util.handleIllegalPlayerAction(activeChar,"[RequestDropItem] count <= 0! ban! oid: "+_objectId+" owner: "+activeChar.getName(),IllegalPlayerAction.PUNISH_KICK);
        	return;
        }
        
        if(!item.isStackable() && _count > 1)
        {
        	Util.handleIllegalPlayerAction(activeChar,"[RequestDropItem] count > 1 but item is not stackable! ban! oid: "+_objectId+" owner: "+activeChar.getName(),IllegalPlayerAction.PUNISH_KICK);
        	return;
        }

        
        if (Config.GM_DISABLE_TRANSACTION && activeChar.getAccessLevel() >= Config.GM_TRANSACTION_MIN && activeChar.getAccessLevel() <= Config.GM_TRANSACTION_MAX)
        {
            activeChar.sendMessage("Transactions are disable for your Access Level");
            activeChar.sendPacket(new SystemMessage(SystemMessage.NOTHING_HAPPENED));
            return;
        }
		
		if (activeChar.isProcessingTransaction() || activeChar.getPrivateStoreType() != 0)
        {
            activeChar.sendPacket(new SystemMessage(SystemMessage.CANNOT_TRADE_DISCARD_DROP_ITEM_WHILE_IN_SHOPMODE));
            return;
        }
		if (activeChar.isFishing())
        {
			//You can't mount, dismount, break and drop items while fishing
            activeChar.sendPacket(new SystemMessage(1470));
            return;
        }
		
		// Cannot discard item that the skill is consumming
		if (activeChar.isCastingNow())
		{
			if (activeChar.getCurrentSkill() != null && activeChar.getCurrentSkill().getSkill().getItemConsumeId() == item.getItemId())
			{
	            activeChar.sendPacket(new SystemMessage(SystemMessage.CANNOT_DISCARD_THIS_ITEM));
	            return;
			}
		}
        
		if (L2Item.TYPE2_QUEST == item.getItem().getType2())
		{
			if (Config.DEBUG) _log.finest(activeChar.getObjectId()+":player tried to drop quest item");
            activeChar.sendPacket(new SystemMessage(SystemMessage.CANNOT_DISCARD_EXCHANGE_ITEM));
            return;
		}
		
		if (!activeChar.isInsideRadius(_x, _y, 150, false) || Math.abs(_z - activeChar.getZ()) > 50)
		{ 
			if (Config.DEBUG) _log.finest(activeChar.getObjectId()+": trying to drop too far away");
            activeChar.sendPacket(new SystemMessage(SystemMessage.CANNOT_DISCARD_DISTANCE_TOO_FAR));
		    return;
		}

		if (Config.DEBUG) _log.fine("requested drop item " + _objectId + "("+ item.getCount()+") at "+_x+"/"+_y+"/"+_z);
        
		if (item.isEquipped())
		{
			L2ItemInstance[] unequiped = activeChar.getInventory().unEquipItemInBodySlotAndRecord(item.getItem().getBodyPart());
			InventoryUpdate iu = new InventoryUpdate();
			for (int i = 0; i < unequiped.length; i++)
			{
			 iu.addModifiedItem(unequiped[i]);
			}
			activeChar.sendPacket(iu);
			activeChar.broadcastUserInfo();
			
			ItemList il = new ItemList(activeChar, true);
			activeChar.sendPacket(il);
		}
		
		L2ItemInstance dropedItem = activeChar.dropItem("Drop", _objectId, _count, _x, _y, _z, null, false);
		
		if (Config.DEBUG) _log.fine("dropping " + _objectId + " item("+_count+") at: " + _x + " " + _y + " " + _z);

		activeChar.broadcastUserInfo();

		if (activeChar.isGM())
		{
			String target = (activeChar.getTarget() != null?activeChar.getTarget().getName():"no-target");
			GMAudit.auditGMAction(activeChar.getName(), "drop", target, dropedItem.getItemId() + " - " +dropedItem.getName());
		}

        if (dropedItem.getItemId() == 57 && dropedItem.getCount() >= 1000000)
        {
            String msg = "Character ("+activeChar.getName()+") has dropped ("+dropedItem.getCount()+")adena at ("+_x+","+_y+","+_z+")";
            _log.warning(msg);
            GmListTable.broadcastMessageToGMs(msg);
        }
	}
	/* (non-Javadoc)
	 * @see net.sf.l2j.gameserver.clientpackets.ClientBasePacket#getType()
	 */
	public String getType()
	{
		return _C__12_REQUESTDROPITEM;
	}
}
