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
package net.sf.l2j.gameserver.network.clientpackets;

import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.datatables.SkillTreeTable;
import net.sf.l2j.gameserver.model.L2EnchantSkillLearn;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2ShortCut;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2EnchantSkillLearn.EnchantSkillDetail;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2NpcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.base.Experience;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ExBrExtraUserInfo;
import net.sf.l2j.gameserver.network.serverpackets.ShortCutRegister;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.network.serverpackets.UserInfo;
import net.sf.l2j.util.Rnd;

/**
 * Format (ch) dd
 * c: (id) 0xD0
 * h: (subid) 0x32
 * d: skill id
 * d: skill lvl
 * @author -Wooden-
 *
 */
public final class RequestExEnchantSkillSafe extends L2GameClientPacket
{
    private static final Logger _log = Logger.getLogger(RequestExEnchantSkillSafe.class.getName());
	private static final Logger _logEnchant = Logger.getLogger("enchant");

	private int _skillId;
	private int _skillLvl;
	
	
	@Override
    protected void readImpl()
	{
		_skillId = readD();
		_skillLvl = readD();
	}

	/* (non-Javadoc)
	 * @see net.sf.l2j.gameserver.clientpackets.ClientBasePacket#runImpl()
	 */
	@Override
	protected void runImpl()
	{
        L2PcInstance player = getClient().getActiveChar();
        if (player == null)
            return;
        
        L2Npc trainer = player.getLastFolkNPC();
        if (!(trainer instanceof L2NpcInstance))
            return;
        
        if (!trainer.canInteract(player) && !player.isGM())
            return;
        
        if (player.getClassId().level() < 3) // requires to have 3rd class quest completed
            return;
        
        if (player.getLevel() < 76) 
            return;
        
        L2Skill skill = SkillTable.getInstance().getInfo(_skillId, _skillLvl);
        if (skill == null)
        {
            return;
        }
        
/*        if (!skill.canTeachBy(npcid) || !skill.getCanLearn(player.getClassId()))
        {
            if (!Config.ALT_GAME_SKILL_LEARN)
            {
                player.sendMessage("You are trying to learn skill that u can't..");
                Util.handleIllegalPlayerAction(player, "Client "+this.getClient()+" tried to learn skill that he can't!!!", IllegalPlayerAction.PUNISH_KICK);
                return;
            }
        }*/

        int costMultiplier = SkillTreeTable.SAFE_ENCHANT_COST_MULTIPLIER;
        int reqItemId = SkillTreeTable.SAFE_ENCHANT_BOOK;
        
        L2EnchantSkillLearn s = SkillTreeTable.getInstance().getSkillEnchantmentBySkillId(_skillId);
        if (s == null)
        {
            return;
        }
        EnchantSkillDetail esd = s.getEnchantSkillDetail(_skillLvl);
        if (player.getSkillLevel(_skillId) != esd.getMinSkillLevel())
        {
            return;
        }
        
        int requiredSp = esd.getSpCost() * costMultiplier;
        int requiredExp = esd.getExp() * costMultiplier;
        int rate = esd.getRate(player);
        
        if (player.getSp() >= requiredSp)
        {
            long expAfter = player.getExp() - requiredExp;
            if (player.getExp() >= requiredExp && expAfter >= Experience.LEVEL[player.getLevel()])
            {
                // No config option for safe enchant book consume
                L2ItemInstance spb = player.getInventory().getItemByItemId(reqItemId);
                if (spb == null)// Haven't spellbook
                {
                    player.sendPacket(new SystemMessage(SystemMessageId.YOU_DONT_HAVE_ALL_OF_THE_ITEMS_NEEDED_TO_ENCHANT_THAT_SKILL));
                    return;
                }
                
                boolean check = player.getStat().removeExpAndSp(requiredExp, requiredSp, false);
                check &= player.destroyItem("Consume", spb.getObjectId(), 1, trainer, true);
                
                if (!check)
                {
                    player.sendPacket(new SystemMessage(SystemMessageId.YOU_DONT_HAVE_ALL_OF_THE_ITEMS_NEEDED_TO_ENCHANT_THAT_SKILL));
                    return;
                }
                
                // ok.  Destroy ONE copy of the book
                if (Rnd.get(100) <= rate)
                {
                	if (Config.LOG_SKILL_ENCHANTS)
                	{
                        LogRecord record = new LogRecord(Level.INFO, "Safe Success");
        				record.setParameters(new Object[]{player, skill, spb, rate});
        				record.setLoggerName("skill");
        				_logEnchant.log(record);
                	}
                    
                    player.addSkill(skill, true);
                    
                    if (Config.DEBUG)
                    {
                        _log.fine("Learned skill ID: "+_skillId+" Level: "+_skillLvl+" for "+requiredSp+" SP, "+requiredExp+" EXP.");
                    }

                    player.sendPacket(new UserInfo(player));
                    player.sendPacket(new ExBrExtraUserInfo(player));
                    
                    SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_SUCCEEDED_IN_ENCHANTING_THE_SKILL_S1);
                    sm.addSkillName(_skillId);
                    player.sendPacket(sm);
                }
                else
                {
                	if (Config.LOG_SKILL_ENCHANTS)
                	{
                        LogRecord record = new LogRecord(Level.INFO, "Safe Fail");
        				record.setParameters(new Object[]{player, skill, spb, rate});
        				record.setLoggerName("skill");
        				_logEnchant.log(record);
                	}

                	SystemMessage sm = new SystemMessage(SystemMessageId.SKILL_ENCHANT_FAILED_S1_LEVEL_WILL_REMAIN);
                    sm.addSkillName(_skillId);
                    player.sendPacket(sm);
                }
                ((L2NpcInstance)trainer).showEnchantSkillList(player, true);
                
                this.updateSkillShortcuts(player);
            }
            else
            {
                SystemMessage sm = new SystemMessage(SystemMessageId.YOU_DONT_HAVE_ENOUGH_EXP_TO_ENCHANT_THAT_SKILL);
                player.sendPacket(sm);
            }
        }
        else
        {
            SystemMessage sm = new SystemMessage(SystemMessageId.YOU_DONT_HAVE_ENOUGH_SP_TO_ENCHANT_THAT_SKILL);
            player.sendPacket(sm);
        }
	}
    
    private void updateSkillShortcuts(L2PcInstance player)
    {
        // update all the shortcuts to this skill
        L2ShortCut[] allShortCuts = player.getAllShortCuts();
        
        for (L2ShortCut sc : allShortCuts)
        {
            if (sc.getId() == _skillId && sc.getType() == L2ShortCut.TYPE_SKILL)
            {
                L2ShortCut newsc = new L2ShortCut(sc.getSlot(), sc.getPage(), sc.getType(), sc.getId(), player.getSkillLevel(_skillId), 1);
                player.sendPacket(new ShortCutRegister(newsc));
                player.registerShortCut(newsc);
            }
        }
    }

	/* (non-Javadoc)
	 * @see net.sf.l2j.gameserver.BasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return "[C] D0:32 RequestExEnchantSkillSafe";
	}
	
}
