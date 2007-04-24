package net.sf.l2j.gameserver.handler.skillhandlers;

import net.sf.l2j.gameserver.handler.ISkillHandler;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2Summon;
import net.sf.l2j.gameserver.model.L2Skill.SkillType;
import net.sf.l2j.gameserver.model.actor.instance.L2NpcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.serverpackets.StatusUpdate;
import net.sf.l2j.gameserver.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.Formulas;

/**
 * Class handling the Mana damage skill
 *
 * @author slyce
 */
public class Manadam implements ISkillHandler
{
	private static SkillType[] _skillIds =
		{ SkillType.MANADAM };

	public void useSkill(@SuppressWarnings("unused")
	L2Character activeChar, L2Skill skill, L2Object[] targets)
	{
		L2Character target = null;

		if (activeChar.isAlikeDead()) return;

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
		for (int index = 0; index < targets.length; index++)
		{
			target = (L2Character) targets[index];
			
            if(target.reflectSkill(skill))
            	target = activeChar;
            
			boolean acted = Formulas.getInstance().calcMagicAffected(activeChar, target, skill);
			if (!acted)
			{
				activeChar.sendPacket(new SystemMessage(SystemMessage.MISSED_TARGET));
			} else
			{
				double damage = Formulas.getInstance().calcManaDam(activeChar, target, skill, ss, bss);
				
				double mp = ( damage > target.getCurrentMp() ? target.getCurrentMp() : damage);
				target.setCurrentMp(target.getCurrentMp() - mp);
				StatusUpdate sump = new StatusUpdate(target.getObjectId());
				sump.addAttribute(StatusUpdate.CUR_MP, (int) target.getCurrentMp());
				// [L2J_JP EDIT START - TSL]
				target.sendPacket(sump);
				SystemMessage sm = new SystemMessage(SystemMessage.S2_MP_HAS_BEEN_DRAINED_BY_S1);
				if (activeChar instanceof L2NpcInstance)
				{
					int mobId = ((L2NpcInstance)activeChar).getNpcId();
					sm.addNpcName(mobId);
				}
				else if (activeChar instanceof L2Summon)
				{
					int mobId = ((L2Summon)activeChar).getNpcId();
					sm.addNpcName(mobId);
				}
				else
				{
					sm.addString(activeChar.getName());
				}
				sm.addNumber((int)mp);
				target.sendPacket(sm);
				if (activeChar instanceof L2PcInstance)
	            {
	                SystemMessage sm2 = new SystemMessage(SystemMessage.YOUR_OPPONENTS_MP_WAS_REDUCED_BY_S1);
					sm2.addNumber((int)mp);
	                activeChar.sendPacket(sm2);
	            }
				// [L2J_JP EDIT END - TSL]
			}
		}
	}

	public SkillType[] getSkillIds()
	{
		return _skillIds;
	}
}
