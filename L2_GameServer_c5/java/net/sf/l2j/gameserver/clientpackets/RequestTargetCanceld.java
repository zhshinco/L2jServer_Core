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
import net.sf.l2j.gameserver.model.L2Character;

/**
 * This class ...
 * 
 * @version $Revision: 1.3.4.2 $ $Date: 2005/03/27 15:29:30 $
 */
public class RequestTargetCanceld extends ClientBasePacket
{
	private static final String _C__37_REQUESTTARGETCANCELD = "[C] 37 RequestTargetCanceld";
	//private static Logger _log = Logger.getLogger(RequestTargetCanceld.class.getName());
    
    private final int _unselect; 

	/**
	 * packet type id 0x37
	 * packet format rev656  ch
	 * @param rawPacket
	 */
	public RequestTargetCanceld(ByteBuffer buf, ClientThread client)
	{
		super(buf, client);
        _unselect = readH();
	}

	void runImpl()
	{
		L2Character activeChar = getClient().getActiveChar();
        if (activeChar != null)
        {
            if (_unselect == 0)
            {
            	if (activeChar.isCastingNow())
            		activeChar.abortCast();
            	else if (activeChar.getTarget() != null)
            		activeChar.setTarget(null);
            }
            else if (activeChar.getTarget() != null)
            	activeChar.setTarget(null);
        }
	}

	/* (non-Javadoc)
	 * @see net.sf.l2j.gameserver.clientpackets.ClientBasePacket#getType()
	 */
	public String getType()
	{
		return _C__37_REQUESTTARGETCANCELD;
	}
}
