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

import java.util.logging.Logger;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

/**
 * Format (ch) dd
 * c: (id) 0xD0
 * h: (subid) 0x33
 * d: skill id
 * d: skill lvl
 * @author -Wooden-
 *
 */
public final class RequestExEnchantSkillUntrain extends L2GameClientPacket
{
    protected static final Logger _log = Logger.getLogger(RequestExEnchantSkillUntrain.class.getName());
	private int _skillId;
	private int _skillLvl;
	
	
	protected void readImpl()
	{
		_skillId = readD();
		_skillLvl = readD(); 
	}

	/* (non-Javadoc)
	 * @see net.sf.l2j.gameserver.clientpackets.ClientBasePacket#runImpl()
	 */
	@Override
	protected
	void runImpl()
	{

		L2PcInstance player = getClient().getActiveChar();
        if (player == null)
        	return;

        _log.info("[T1:RequestExEnchantSkillUntrain] skill id?:"+_skillId);
        _log.info("[T1:RequestExEnchantSkillUntrain] skill lvl?:"+_skillLvl);
	}

	/* (non-Javadoc)
	 * @see net.sf.l2j.gameserver.BasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return "[C] D0:33 RequestExEnchantSkillUntrain";
	}
	
}
