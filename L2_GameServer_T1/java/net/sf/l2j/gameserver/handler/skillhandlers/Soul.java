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
package net.sf.l2j.gameserver.handler.skillhandlers; 

import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.handler.ISkillHandler;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2Skill.SkillType;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.serverpackets.SystemMessage;

/**
 * 
 * @author nBd
 */

public class Soul implements ISkillHandler
{
    private static final SkillType[] SKILL_IDS = { SkillType.CHARGESOUL };
    
    public void useSkill(L2Character activeChar, L2Skill skill, @SuppressWarnings("unused") L2Object[] targets)
    {
        if (activeChar == null || activeChar.isAlikeDead() || !(activeChar instanceof L2PcInstance))
            return;
        
        L2PcInstance player = (L2PcInstance)activeChar;
        
        L2Skill soulmastery = SkillTable.getInstance().getInfo(467, player.getSkillLevel(467));
        
        if (soulmastery != null)
        {
            if (player.getSouls() < soulmastery.getNumSouls())
            {
                int count = 0;
                
                if (player.getSouls() + skill.getNumSouls() <= soulmastery.getNumSouls())
                    count = skill.getNumSouls();
                else
                    count = soulmastery.getNumSouls() - player.getSouls();
                
                player.increaseSouls(count);
            }
            else
            {
                SystemMessage sm = new SystemMessage(SystemMessageId.SOUL_CANNOT_BE_INCREASED_ANYMORE);
                player.sendPacket(sm);
                return;
            }
        }
    }
    
    public SkillType[] getSkillIds() 
    { 
        return SKILL_IDS; 
    }
}
