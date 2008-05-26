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

import java.util.logging.Logger;

import net.sf.l2j.gameserver.handler.ISkillHandler;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2Skill.SkillType;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.util.Rnd;

/**
 * @author evill33t
 *
 */
public class SummonTreasureKey implements ISkillHandler
{
    static Logger _log = Logger.getLogger(SummonTreasureKey.class.getName());
    private static final SkillType[] SKILL_IDS = {SkillType.SUMMON_TREASURE_KEY};

    public void useSkill(L2Character activeChar, @SuppressWarnings("unused")
    		L2Skill skill, @SuppressWarnings("unused")L2Object[] targets)
    {
        if (!(activeChar instanceof L2PcInstance)) return;

        L2PcInstance player = (L2PcInstance) activeChar;

        try
        {

            int item_id = 0;

            switch (skill.getLevel())
            {
                case 1:
                {
                  item_id = Rnd.get(6667, 6669);
                  break;
                }
                case 2:
                {
                  item_id = Rnd.get(6668, 6670);
                  break;
                }
                case 3:
                {
                  item_id = Rnd.get(6669, 6671);
                  break;
                }
                case 4:
                {
                  item_id = Rnd.get(6670, 6672);
                  break;
                }
            }
            player.addItem("Skill", item_id, Rnd.get(2,3), player, false);
        }
        catch (Exception e)
        {
            _log.warning("Error using skill summon Treasure Key:" + e);
        }
    }

    public SkillType[] getSkillIds()
    {
        return SKILL_IDS;
    }

}
