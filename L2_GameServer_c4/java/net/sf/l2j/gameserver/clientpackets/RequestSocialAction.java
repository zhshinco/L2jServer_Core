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
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.serverpackets.SocialAction;
import net.sf.l2j.gameserver.serverpackets.SystemMessage;

/**
 * This class ...
 * 
 * @version $Revision: 1.6.4.4 $ $Date: 2005/03/27 15:29:30 $
 */
public class RequestSocialAction extends ClientBasePacket
{
	private static final String _C__1B_REQUESTSOCIALACTION = "[C] 1B RequestSocialAction";
	private static Logger _log = Logger.getLogger(RequestSocialAction.class.getName());
	
	// format  cd
	private final int _actionId;
	
	/**
	 * packet type id 0x1b
	 * format:		cd
	 * @param decrypt
	 */
	public RequestSocialAction(ByteBuffer buf, ClientThread client)
	{
		super(buf, client);
		_actionId  = readD();
	}

	void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
		    return;

        // You cannot do anything else while fishing
        if (activeChar.isFishing())
        {
            SystemMessage sm = new SystemMessage(1471);
            activeChar.sendPacket(sm);
            return;
        }
        
		if (	activeChar.getPrivateStoreType()==0 &&
				activeChar.getActiveRequester()==null &&
				!activeChar.isAlikeDead() &&
				!activeChar.isAllSkillsDisabled() &&
				activeChar.getAI().getIntention()==CtrlIntention.AI_INTENTION_IDLE)
		{
			if (Config.DEBUG) _log.fine("Social Action:" + _actionId);
			
			SocialAction atk = new SocialAction(activeChar.getObjectId(), _actionId);
			
			activeChar.broadcastPacket(atk);
		}
	}
	
	/* (non-Javadoc)
	 * @see net.sf.l2j.gameserver.clientpackets.ClientBasePacket#getType()
	 */
	public String getType()
	{
		return _C__1B_REQUESTSOCIALACTION;
	}
}
