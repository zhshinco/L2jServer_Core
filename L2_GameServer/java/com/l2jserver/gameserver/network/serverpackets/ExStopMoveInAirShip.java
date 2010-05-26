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
package com.l2jserver.gameserver.network.serverpackets;

import com.l2jserver.gameserver.model.actor.L2Character;
/**
 * @authos kerberos
 *
 */
public class ExStopMoveInAirShip extends L2GameServerPacket
{
	private L2Character _activeChar;
    private int _shipObjId;

    public ExStopMoveInAirShip(L2Character player, int shipObjId)
    {
    	_activeChar = player;
    	_shipObjId = shipObjId;
    }

    @Override
	protected final void writeImpl()
    {
        writeC(0xfe);
        writeH(0x6e);
        writeD(_activeChar.getObjectId());
        writeD(_shipObjId);
        writeD(_activeChar.getX());
        writeD(_activeChar.getY());
        writeD(_activeChar.getZ());
        writeD(_activeChar.getHeading());
    }

    /* (non-Javadoc)
     * @see com.l2jserver.gameserver.serverpackets.ServerBasePacket#getType()
     */
    @Override
	public String getType()
    {
        return "[S] FE:6e ExStopMoveAirShip";
    }
}
