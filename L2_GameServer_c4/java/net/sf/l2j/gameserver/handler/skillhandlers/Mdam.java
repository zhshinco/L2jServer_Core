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
package net.sf.l2j.gameserver.handler.skillhandlers;

import net.sf.l2j.gameserver.handler.ISkillHandler;
import net.sf.l2j.gameserver.lib.Log;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2Summon;
import net.sf.l2j.gameserver.model.L2Skill.SkillType;
import net.sf.l2j.gameserver.model.actor.instance.L2NpcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2RaidBossInstance;
import net.sf.l2j.gameserver.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.Formulas;

/**
 * This class ...
 * 
 * @version $Revision: 1.1.2.8.2.9 $ $Date: 2005/04/05 19:41:23 $
 */

public class Mdam implements ISkillHandler
{
    //private static Logger _log = Logger.getLogger(Mdam.class.getName());

    /* (non-Javadoc)
     * @see net.sf.l2j.gameserver.handler.IItemHandler#useItem(net.sf.l2j.gameserver.model.L2PcInstance, net.sf.l2j.gameserver.model.L2ItemInstance)
     */
    private static SkillType[] _skillIds = {SkillType.MDAM, SkillType.DEATHLINK};

    /* (non-Javadoc)
     * @see net.sf.l2j.gameserver.handler.IItemHandler#useItem(net.sf.l2j.gameserver.model.L2PcInstance, net.sf.l2j.gameserver.model.L2ItemInstance)
     */
    public void useSkill(L2Character activeChar, L2Skill skill, L2Object[] targets)
    {
        if (activeChar.isAlikeDead()) return;

        boolean ss = false;
        boolean bss = false;

        L2ItemInstance weaponInst = activeChar.getActiveWeaponInstance();

        if (activeChar instanceof L2PcInstance)
        {
            if (weaponInst == null)
            {
                SystemMessage sm2 = new SystemMessage(614);
                sm2.addString("You must equip a weapon before casting a spell.");
                activeChar.sendPacket(sm2);
                return;
            }
        }

        if (weaponInst != null)
        {
            if (weaponInst.getChargedSpiritshot() == L2ItemInstance.CHARGED_BLESSED_SPIRITSHOT)
            {
                bss = true;
                weaponInst.setChargedSpiritshot(L2ItemInstance.CHARGED_NONE);
            }
            else if (weaponInst.getChargedSpiritshot() == L2ItemInstance.CHARGED_SPIRITSHOT)
            {
                ss = true;
                weaponInst.setChargedSpiritshot(L2ItemInstance.CHARGED_NONE);
            }
        }
        // If there is no weapon equipped, check for an active summon.
        else if (activeChar instanceof L2Summon)
        {
            L2Summon activeSummon = (L2Summon) activeChar;

            if (activeSummon.getChargedSpiritShot() == L2ItemInstance.CHARGED_BLESSED_SPIRITSHOT)
            {
                bss = true;
                activeSummon.setChargedSpiritShot(L2ItemInstance.CHARGED_NONE);
            }
            else if (activeSummon.getChargedSpiritShot() == L2ItemInstance.CHARGED_SPIRITSHOT)
            {
                ss = true;
                activeSummon.setChargedSpiritShot(L2ItemInstance.CHARGED_NONE);
            }
        }

        for (int index = 0; index < targets.length; index++)
        {
            L2Character target = (L2Character) targets[index];

            if (activeChar instanceof L2PcInstance && target instanceof L2PcInstance
                && target.isAlikeDead() && target.isFakeDeath())
            {
                target.stopFakeDeath(null);
            }
            else if (target.isAlikeDead())
            {
                continue;
            }
            //			if (skill != null)
            //			if (skill.isOffensive())
            //			{

            //				boolean acted;
            //				if (skill.getSkillType() == L2Skill.SkillType.DOT || skill.getSkillType() == L2Skill.SkillType.MDOT)
            //				    acted = Formulas.getInstance().calcSkillSuccess(
            //						activeChar, target, skill);
            //				else			
            //				    acted = Formulas.getInstance().calcMagicAffected(
            //						activeChar, target, skill);
            //				if (!acted) {
            //					activeChar.sendPacket(new SystemMessage(SystemMessage.MISSED_TARGET));
            //					continue;
            //				}
            //				
            //			}

            boolean mcrit = Formulas.getInstance().calcMCrit(activeChar.getMCriticalHit(target, skill));

            int damage = (int) Formulas.getInstance().calcMagicDam(activeChar, target, skill, ss, bss,
                                                                   mcrit);

            if (damage > 5000 && activeChar instanceof L2PcInstance)
            {
                String name = "";
                if (target instanceof L2RaidBossInstance) name = "RaidBoss ";
                if (target instanceof L2NpcInstance)
                    name += target.getName() + "(" + ((L2NpcInstance) target).getTemplate().npcId + ")";
                if (target instanceof L2PcInstance)
                    name = target.getName() + "(" + target.getObjectId() + ") ";
                name += target.getLevel() + " lvl";
                Log.add(activeChar.getName() + "(" + activeChar.getObjectId() + ") "
                    + activeChar.getLevel() + " lvl did damage " + damage + " with skill "
                    + skill.getName() + "(" + skill.getId() + ") to " + name, "damage_mdam");
            }
            boolean abort = Formulas.getInstance().calcAtkBreak(target, target.getStat().getAtkCancel());
            if (abort)
            {
                target.breakAttack();
                target.breakCast();
            }
            // Why are we trying to reduce the current target HP here?
            // Why not inside the below "if" condition, after the effects processing as it should be?
            // It doesn't seem to make sense for me. I'm moving this line inside the "if" condition, right after the effects processing...
            // [changed by nexus - 2006-08-15]
            //target.reduceCurrentHp(damage, activeChar);
    
            if (damage > 0)
            {
                if (activeChar instanceof L2PcInstance)
                {
                    if (mcrit) activeChar.sendPacket(new SystemMessage(1280));
    
                    SystemMessage sm = new SystemMessage(SystemMessage.YOU_DID_S1_DMG);
                    sm.addNumber(damage);
                    activeChar.sendPacket(sm);
                }
    
                if (skill.hasEffects())
                {
                    // activate attacked effects, if any
                    target.stopEffect(skill.getId());
                    if (target.getEffect(skill.getId()) != null)
                        target.removeEffect(target.getEffect(skill.getId()));
                    if (Formulas.getInstance().calcSkillSuccess(activeChar, target, skill, false, ss, bss)) 
                        skill.getEffects(activeChar, target);
                    else
                    {
                        SystemMessage sm = new SystemMessage(139);
                        sm.addString(target.getName());
                        sm.addString(skill.getName());
                        activeChar.sendPacket(sm);
                    }
                }
                
                target.reduceCurrentHp(damage, activeChar);
            }
        }
        
        if (skill.isSuicideAttack())
        {
        	activeChar.doDie(null);
        	activeChar.setCurrentHp(0);
        }
    }

    public SkillType[] getSkillIds()
    {
        return _skillIds;
    }
}
