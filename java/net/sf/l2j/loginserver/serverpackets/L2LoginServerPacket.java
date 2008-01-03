/* This program is free software; you can redistribute it and/or modify
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
package net.sf.l2j.loginserver.serverpackets;

import net.sf.l2j.loginserver.L2LoginClient;

import org.mmocore.network.SendablePacket;

/**
 *
 * @author  KenM
 */
public abstract class L2LoginServerPacket extends SendablePacket<L2LoginClient>
{

    /**
     * @see org.mmocore.network.SendablePacket#getHeaderSize()
     */
    @Override
    protected int getHeaderSize()
    {
        return 2;
    }

    /**
     * @see org.mmocore.network.SendablePacket#writeHeader(int)
     */
    @Override
    protected void writeHeader(int dataSize)
    {
        writeH(dataSize + this.getHeaderSize());
    }
    
}
