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
import net.sf.l2j.gameserver.datatables.ClanTable;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.serverpackets.PledgeInfo;

/**
 * This class ...
 * 
 * @version $Revision: 1.5.4.3 $ $Date: 2005/03/27 15:29:30 $
 */
public class RequestPledgeInfo extends ClientBasePacket
{
	private static final String _C__66_REQUESTPLEDGEINFO = "[C] 66 RequestPledgeInfo";

	private static Logger _log = Logger.getLogger(RequestPledgeInfo.class
			.getName());

	private final int _clanId;

	/**
	 * packet type id 0x66 format: cd
	 * 
	 * @param rawPacket
	 */
	public RequestPledgeInfo(ByteBuffer buf, ClientThread client)
	{
		super(buf, client);
		_clanId = readD();
	}

	void runImpl()
	{
		if (Config.DEBUG)
			_log.fine("infos for clan " + _clanId + " requested");

		L2PcInstance activeChar = getClient().getActiveChar();
		L2Clan clan = ClanTable.getInstance().getClan(_clanId);
		if (clan == null)
		{
			_log.warning("Clan data for clanId " + _clanId + " is missing");
			return; // we have no clan data ?!? should not happen
		}

		PledgeInfo pc = new PledgeInfo(clan);
		if (activeChar != null)
		{
			activeChar.sendPacket(pc);

			/*
			 * if (clan.getClanId() == activeChar.getClanId()) {
			 * activeChar.sendPacket(new PledgeShowMemberListDeleteAll());
			 * PledgeShowMemberListAll pm = new PledgeShowMemberListAll(clan,
			 * activeChar); activeChar.sendPacket(pm); }
			 */
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.sf.l2j.gameserver.clientpackets.ClientBasePacket#getType()
	 */
	public String getType()
	{
		return _C__66_REQUESTPLEDGEINFO;
	}
}
