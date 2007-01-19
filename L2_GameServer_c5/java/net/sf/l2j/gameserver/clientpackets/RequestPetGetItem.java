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

import net.sf.l2j.gameserver.ClientThread;
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PetInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2SummonInstance;
import net.sf.l2j.gameserver.serverpackets.ActionFailed;

/**
 * This class ...
 * 
 * @version $Revision: 1.2.4.4 $ $Date: 2005/03/29 23:15:33 $
 */
public class RequestPetGetItem extends ClientBasePacket
{

	//private static Logger _log = Logger.getLogger(RequestPetGetItem.class.getName());
	private static final String _C__8f_REQUESTPETGETITEM= "[C] 8F RequestPetGetItem";
	
	private final int _objectId;
	
	public RequestPetGetItem(ByteBuffer buf, ClientThread client)
	{
		super(buf, client);
		_objectId = readD();
	}

	void runImpl()
	{
		L2World world = L2World.getInstance();
		L2ItemInstance item = (L2ItemInstance)world.findObject(_objectId);
		if (item == null || getClient().getActiveChar() == null)
		    return;
		if(getClient().getActiveChar().getPet() instanceof L2SummonInstance)
		{
			sendPacket(new ActionFailed());
			return;
		}
		L2PetInstance pet = (L2PetInstance)getClient().getActiveChar().getPet();
		if (pet == null || pet.isDead() || pet.isOutOfControl())
		{
			sendPacket(new ActionFailed());
			return;
		}
		pet.getAI().setIntention(CtrlIntention.AI_INTENTION_PICK_UP, item);
	}

	public String getType()
	{
		return _C__8f_REQUESTPETGETITEM;
	}

}
