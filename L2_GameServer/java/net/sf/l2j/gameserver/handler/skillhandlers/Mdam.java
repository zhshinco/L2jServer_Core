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

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.handler.ISkillHandler;
import net.sf.l2j.gameserver.lib.Log;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2Summon;
import net.sf.l2j.gameserver.model.actor.instance.L2CubicInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2NpcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2RaidBossInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.templates.L2SkillType;

/**
 * This class ...
 *
 * @version $Revision: 1.1.2.8.2.9 $ $Date: 2005/04/05 19:41:23 $
 */

public class Mdam implements ISkillHandler
{
	protected static final Logger _log = Logger.getLogger(Mdam.class.getName());
	
	private static final L2SkillType[] SKILL_IDS =
	{
		L2SkillType.MDAM,
		L2SkillType.DEATHLINK
	};
	
	/**
	 * 
	 * @see net.sf.l2j.gameserver.handler.ISkillHandler#useSkill(net.sf.l2j.gameserver.model.L2Character, net.sf.l2j.gameserver.model.L2Skill, net.sf.l2j.gameserver.model.L2Object[])
	 */
	public void useSkill(L2Character activeChar, L2Skill skill, L2Object[] targets)
	{
		if (activeChar.isAlikeDead())
			return;
		
		boolean ss = false;
		boolean bss = false;
		
		L2ItemInstance weaponInst = activeChar.getActiveWeaponInstance();

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
		else if (activeChar instanceof L2NpcInstance)
		{
			bss = ((L2NpcInstance) activeChar).isUsingShot(false);
			ss = ((L2NpcInstance) activeChar).isUsingShot(true);
		}
		
		for (L2Character target: (L2Character[]) targets)
		{
			if (activeChar instanceof L2PcInstance && target instanceof L2PcInstance && target.isFakeDeath())
			{
				target.stopFakeDeath(null);
			}
			else if (target.isDead())
			{
				continue;
			}
			
			boolean mcrit = Formulas.getInstance().calcMCrit(activeChar.getMCriticalHit(target, skill));
			
			int damage = (int) Formulas.getInstance().calcMagicDam(activeChar, target, skill, ss, bss, mcrit);
			if (skill.getMaxSoulConsumeCount() > 0 && activeChar instanceof L2PcInstance && ((L2PcInstance) activeChar).getSouls() > 0)
			{
				switch (((L2PcInstance) activeChar).getSouls())
				{
					case 1:
						damage *= 1.10;
						break;
					case 2:
						damage *= 1.12;
						break;
					case 3:
						damage *= 1.15;
						break;
					case 4:
						damage *= 1.18;
						break;
					default:
						damage *= 1.20;
						break;
				}
			}			
			
			if (damage > 0)
			{
				// Manage attack or cast break of the target (calculating rate, sending message...)
				if (!target.isRaid() && Formulas.getInstance().calcAtkBreak(target, damage))
				{
					target.breakAttack();
					target.breakCast();
				}
				
				activeChar.sendDamageMessage(target, damage, mcrit, false, false);
				
				if (skill.hasEffects())
				{
					if (target.reflectSkill(skill))
					{
						activeChar.stopSkillEffects(skill.getId());
						skill.getEffects(target, activeChar);
						SystemMessage sm = new SystemMessage(SystemMessageId.YOU_FEEL_S1_EFFECT);
						sm.addSkillName(skill);
						activeChar.sendPacket(sm);
					}
					else
					{
						// activate attacked effects, if any
						target.stopSkillEffects(skill.getId());
						if (Formulas.getInstance().calcSkillSuccess(activeChar, target, skill, false, ss, bss))
							skill.getEffects(activeChar, target);
						else
						{
							SystemMessage sm = new SystemMessage(SystemMessageId.S1_WAS_UNAFFECTED_BY_S2);
							sm.addCharName(target);
							sm.addSkillName(skill);
							activeChar.sendPacket(sm);
						}
					}
				}
				
				target.reduceCurrentHp(damage, activeChar);
				
				// Logging damage
				if (Config.LOG_GAME_DAMAGE && damage > 5000 && activeChar instanceof L2PcInstance)
				{
					String name = "";
					if (target instanceof L2RaidBossInstance)
						name = "RaidBoss ";
					if (target instanceof L2NpcInstance)
						name += target.getName() + "(" + ((L2NpcInstance) target).getTemplate().npcId + ")";
					if (target instanceof L2PcInstance)
						name = target.getName() + "(" + target.getObjectId() + ") ";
					name += target.getLevel() + " lvl";
					Log.add(activeChar.getName() + "(" + activeChar.getObjectId() + ") " + activeChar.getLevel() + " lvl did damage " + damage + " with skill " + skill.getName() + "(" + skill.getId() + ") to " + name, "damage_mdam");
				}
			}
			// Possibility of a lethal strike
			Formulas.getInstance().calcLethalHit(activeChar, target, skill);
		}
		// self Effect :]
		L2Effect effect = activeChar.getFirstEffect(skill.getId());
		if (effect != null && effect.isSelfEffect())
		{
			//Replace old effect with new one.
			effect.exit();
		}
		skill.getEffectsSelf(activeChar);
		
		if (skill.isSuicideAttack())
			activeChar.doDie(activeChar);
		
	}
	
	/**
	 * 
	 * @param activeCubic
	 * @param skill
	 * @param targets
	 */
	public void useCubicSkill(L2CubicInstance activeCubic, L2Skill skill, L2Object[] targets)
	{
		for (L2Character target: (L2Character[]) targets)
		{
			if (target instanceof L2PcInstance && target.isAlikeDead() && target.isFakeDeath())
			{
				target.stopFakeDeath(null);
			}
			else if (target.isAlikeDead())
			{
				continue;
			}
			
			boolean mcrit = Formulas.getInstance().calcMCrit(activeCubic.getMCriticalHit(target, skill));
			int damage = (int) Formulas.getInstance().calcMagicDam(activeCubic, target, skill, mcrit);
			
			// if target is reflecting the skill then no damage is done
			if (target.reflectSkill(skill))
				damage = 0;
			
			if (Config.DEBUG)
				_log.info("L2SkillMdam: useCubicSkill() -> damage = " + damage);
			
			if (damage > 0)
			{
				// Manage attack or cast break of the target (calculating rate, sending message...)
				if (!target.isRaid() && Formulas.getInstance().calcAtkBreak(target, damage))
				{
					target.breakAttack();
					target.breakCast();
				}
				
				activeCubic.getOwner().sendDamageMessage(target, damage, mcrit, false, false);
				
				if (skill.hasEffects())
				{
					// activate attacked effects, if any
					target.stopSkillEffects(skill.getId());
					if (target.getFirstEffect(skill) != null)
						target.removeEffect(target.getFirstEffect(skill));
					if (Formulas.getInstance().calcCubicSkillSuccess(activeCubic, target, skill))
						skill.getEffects(activeCubic, target);
					else
					{
						SystemMessage sm = new SystemMessage(SystemMessageId.S1_WAS_UNAFFECTED_BY_S2);
						sm.addCharName(target);
						sm.addSkillName(skill.getDisplayId());
						activeCubic.getOwner().sendPacket(sm);
					}
				}
				
				target.reduceCurrentHp(damage, activeCubic.getOwner());
			}
		}
	}
	
	/**
	 * 
	 * @see net.sf.l2j.gameserver.handler.ISkillHandler#getSkillIds()
	 */
	public L2SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}
