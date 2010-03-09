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
package com.l2jserver.gameserver.model.actor.instance;

import com.l2jserver.Config;
import com.l2jserver.gameserver.cache.HtmCache;
import com.l2jserver.gameserver.model.actor.L2Npc;
import com.l2jserver.gameserver.network.serverpackets.ExShowDominionRegistry;
import com.l2jserver.gameserver.templates.chars.L2NpcTemplate;

/**
 * NPC that gives information about territory wars
 * @author  GodKratos
 */
public class L2MercenaryManagerInstance extends L2Npc
{
    /**
     * @param objectId
     * @param template
     */
    public L2MercenaryManagerInstance(int objectId, L2NpcTemplate template)
    {
           super(objectId, template);
           setInstanceType(InstanceType.L2MercenaryManagerInstance);
    }

	@Override
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		if (command.equalsIgnoreCase("Territory"))
		{
			int castleId = getNpcId() - 36480;
			player.sendPacket(new ExShowDominionRegistry(castleId));
		}
		else
			super.onBypassFeedback(player, command);
	}
    
	@Override
	public String getHtmlPath(int npcId, int val)
	{
		String temp = "";
		if (val == 0)
			temp = "data/html/mercmanager/MercenaryManager.htm";
		else
			temp = "data/html/mercmanager/MercenaryManager-" + val + ".htm";
		
		if (!Config.LAZY_CACHE)
		{
			// If not running lazy cache the file must be in the cache or it doesnt exist
			if (HtmCache.getInstance().contains(temp))
				return temp;
		}
		else
		{
			if (HtmCache.getInstance().isLoadable(temp))
				return temp;
		}
		
		// If the file is not found, the standard message "I have nothing to say to you" is returned
		return "data/html/npcdefault.htm";
	}
}
