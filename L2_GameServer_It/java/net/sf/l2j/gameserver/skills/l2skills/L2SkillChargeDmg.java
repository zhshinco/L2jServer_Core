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
package net.sf.l2j.gameserver.skills.l2skills;

import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.skills.effects.EffectCharge;
import net.sf.l2j.gameserver.templates.L2WeaponType;
import net.sf.l2j.gameserver.templates.StatsSet;

public class L2SkillChargeDmg extends L2Skill 
{

	final int num_charges;
	final int charge_skill_id;
	
	public L2SkillChargeDmg(StatsSet set)
    {
		super(set);
		
		num_charges = set.getInteger("num_charges", getLevel());
		charge_skill_id = set.getInteger("charge_skill_id");
	}

	public boolean checkCondition(L2Character activeChar, boolean itemOrWeapon)
	{
		if (activeChar instanceof L2PcInstance)
		{
			L2PcInstance player = (L2PcInstance)activeChar;
			EffectCharge e = (EffectCharge)player.getEffect(charge_skill_id);
			if(e == null || e.num_charges < this.num_charges)
			{
				SystemMessage sm = new SystemMessage(113);
				sm.addSkillName(this.getId());
				activeChar.sendPacket(sm);
				return false;
			}
		}
		return super.checkCondition(activeChar, itemOrWeapon);
	}
	
	public void useSkill(L2Character caster, L2Object[] targets)
    {
		if (caster.isAlikeDead())
        {
			return;
        }
		
		// get the effect
		EffectCharge effect = (EffectCharge) caster.getEffect(charge_skill_id);
		if (effect == null || effect.num_charges < this.num_charges)
		{
			SystemMessage sm = new SystemMessage(113);
			sm.addSkillName(this.getId());
			caster.sendPacket(sm);
			return;
		}
        double modifier = 0;
        modifier = effect.num_charges*0.33;		
		if (this.getTargetType() != SkillTargetType.TARGET_AREA && this.getTargetType() != SkillTargetType.TARGET_MULTIFACE)
			effect.num_charges -= this.num_charges;
        //effect.num_charges = 0;
		caster.updateEffectIcons();
        if (effect.num_charges == 0)
        	{effect.exit();}
        for(int index = 0;index < targets.length;index++)
        {
        	L2ItemInstance weapon = caster.getActiveWeaponInstance();
					L2Character target = (L2Character)targets[index];
					if (target.isAlikeDead())
          {
						continue;
          }
			// TODO: should we use dual or not?
			// because if so, damage are lowered but we dont do anything special with dual then
			// like in doAttackHitByDual which in fact does the calcPhysDam call twice
			
			//boolean dual  = caster.isUsingDualWeapon();
			boolean shld = Formulas.getInstance().calcShldUse(caster, target);
			boolean crit = Formulas.getInstance().calcCrit(caster.getCriticalHit(target, this));
			boolean soul = (weapon!= null && weapon.getChargedSoulshot() == L2ItemInstance.CHARGED_SOULSHOT && weapon.getItemType() != L2WeaponType.DAGGER );
			
			int damage = (int)Formulas.getInstance().calcPhysDam(
					caster, target, this, shld, crit, false, soul);
			
			if (damage > 0)
            {
                double finalDamage = damage;
                finalDamage = finalDamage+(modifier*finalDamage);
				target.reduceCurrentHp(finalDamage, caster);
				
				if (crit) caster.sendPacket(new SystemMessage(SystemMessage.CRITICAL_HIT));
				
				SystemMessage sm = new SystemMessage(SystemMessage.YOU_DID_S1_DMG);
				sm.addNumber((int)finalDamage);
				caster.sendPacket(sm);
				
				if (soul && weapon!= null)
					weapon.setChargedSoulshot(L2ItemInstance.CHARGED_NONE);
			}
            else
            {
				SystemMessage sm = new SystemMessage(SystemMessage.MISSED_TARGET);
				caster.sendPacket(sm);
			}
		}
        // effect self :]
        L2Effect seffect = caster.getEffect(getId());
        if (seffect != null && seffect.isSelfEffect())
        {             
            //Replace old effect with new one.
            seffect.exit();
        }
        // cast self effect if any
        getEffectsSelf(caster);
	}
	
}
