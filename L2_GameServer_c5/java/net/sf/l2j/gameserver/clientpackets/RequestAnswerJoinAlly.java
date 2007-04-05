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
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.serverpackets.SystemMessage;

/**
 *  sample
 *  5F
 *  01 00 00 00
 *
 *  format  cdd
 *
 *
 * @version $Revision: 1.7.4.2 $ $Date: 2005/03/27 15:29:30 $
 */
public class RequestAnswerJoinAlly extends ClientBasePacket
{
	private static final String _C__83_REQUESTANSWERJOINALLY = "[C] 83 RequestAnswerJoinAlly";
	//private static Logger _log = Logger.getLogger(RequestAnswerJoinAlly.class.getName());

	private final int _response;

	public RequestAnswerJoinAlly(ByteBuffer buf, ClientThread client)
	{
		super(buf, client);
		_response = readD();
	}

	void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
		{
		    return;
		}

		L2PcInstance requestor = activeChar.getRequest().getPartner();
        if (requestor == null)
        {
        	return;
        }

		if (_response == 0)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.YOU_DID_NOT_RESPOND_TO_ALLY_INVITATION));
			requestor.sendPacket(new SystemMessage(SystemMessage.NO_RESPONSE_TO_ALLY_INVITATION));
		}
		else
		{
	        if (!(requestor.getRequest().getRequestPacket() instanceof RequestJoinAlly))
	        {
	        	return; // hax
	        }

	        L2Clan clan = requestor.getClan();
			// we must double check this cause of hack
			if (clan.CheckAllyJoinCondition(requestor, activeChar))
	        {
		        //TODO: Need correct message id
				requestor.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_SUCCEEDED_INVITING_FRIEND));

				activeChar.sendPacket(new SystemMessage(SystemMessage.YOU_ACCEPTED_ALLIANCE));

				activeChar.getClan().setAllyId(clan.getAllyId());
				activeChar.getClan().setAllyName(clan.getAllyName());
				activeChar.getClan().setAllyPenaltyExpiryTime(0, 0);
				activeChar.getClan().updateClanInDB();
	        }
		}

		activeChar.getRequest().onRequestResponse();
	}

	/* (non-Javadoc)
	 * @see net.sf.l2j.gameserver.clientpackets.ClientBasePacket#getType()
	 */
	public String getType()
	{
		return _C__83_REQUESTANSWERJOINALLY;
	}
}
