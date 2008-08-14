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
package net.sf.l2j.gameserver.skills.l2skills;

import net.sf.l2j.gameserver.model.Inventory;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.InventoryUpdate;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.templates.L2Weapon;
import net.sf.l2j.gameserver.templates.StatsSet;

/**
 *
 * @author nBd
 */
public class L2SkillChangeWeapon extends L2Skill
{
    
    /**
     * @param set
     */
    public L2SkillChangeWeapon(StatsSet set)
    {
        super(set);
    }
    
    /**
     * @see net.sf.l2j.gameserver.model.L2Skill#useSkill(net.sf.l2j.gameserver.model.L2Character, net.sf.l2j.gameserver.model.L2Object[])
     */
    @Override
    public void useSkill(L2Character caster, L2Object[] targets)
    {
        if(caster.isAlikeDead())
            return;
        
        if (!(caster instanceof L2PcInstance))
            return;
        
        L2PcInstance player = (L2PcInstance)caster;
        
        L2Weapon weaponItem = player.getActiveWeaponItem();
        
        if (weaponItem == null)
            return;
        
        L2ItemInstance wpn = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);
        if (wpn == null)
            wpn = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LRHAND);
        
        if (wpn != null)
        {
            if (wpn.isWear())
                return;
            
            if (wpn.isAugmented())
                return;
            
            int newItemId = 0;
            int enchantLevel = 0;
            
            if (weaponItem.getChangeWeaponId() != 0)
            {
                newItemId = weaponItem.getChangeWeaponId();
                enchantLevel = wpn.getEnchantLevel();
                

                if (newItemId == -1)
                    return;
                
                L2ItemInstance[] unequiped = player.getInventory().unEquipItemInBodySlotAndRecord(wpn.getItem().getBodyPart());
                InventoryUpdate iu = new InventoryUpdate();
                for (int i = 0; i < unequiped.length; i++)
                    iu.addModifiedItem(unequiped[i]);
                
                player.sendPacket(iu);
                
                if (unequiped.length > 0)
                {
                    byte count = 0;
                    
                    for (int i = 0; i < unequiped.length; i++)
                    {
                        if (!(unequiped[i].getItem() instanceof L2Weapon))
                        {
                            count++;
                            continue;
                        }
                        
                        SystemMessage sm = null;
                        if (unequiped[i].getEnchantLevel() > 0)
                        {
                            sm = new SystemMessage(SystemMessageId.EQUIPMENT_S1_S2_REMOVED);
                            sm.addNumber(unequiped[i].getEnchantLevel());
                            sm.addItemName(unequiped[i]);
                        }
                        else
                        {
                            sm = new SystemMessage(SystemMessageId.S1_DISARMED);
                            sm.addItemName(unequiped[i]);
                        }
                        player.sendPacket(sm);
                    }
                    
                    if (count == unequiped.length)
                        return;
                }
                else
                {
                    return;
                }
                
                L2ItemInstance destroyItem = player.getInventory().destroyItem("ChangeWeapon", wpn, player, null);
                
                if (destroyItem == null)
                    return;
                
                L2ItemInstance newItem = player.getInventory().addItem("ChangeWeapon", newItemId, 1, player, destroyItem);
                
                if (newItem == null)
                    return;
                
                newItem.setEnchantLevel(enchantLevel);
                player.getInventory().equipItem(newItem);
                
                SystemMessage msg = null;
                
                if (newItem.getEnchantLevel() > 0)
                {
                    msg = new SystemMessage(SystemMessageId.S1_S2_EQUIPPED);
                    msg.addNumber(newItem.getEnchantLevel());
                    msg.addItemName(newItem);
                }
                else
                {
                    msg = new SystemMessage(SystemMessageId.S1_EQUIPPED);
                    msg.addItemName(newItem);
                }
                player.sendPacket(msg);
                
                InventoryUpdate u = new InventoryUpdate();
                u.addRemovedItem(destroyItem);
                player.sendPacket(u);
                
                player.broadcastUserInfo();
            }
            
        }
    }
}
