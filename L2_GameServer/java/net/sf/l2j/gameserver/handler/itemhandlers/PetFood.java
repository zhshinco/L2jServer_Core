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
package net.sf.l2j.gameserver.handler.itemhandlers;

import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2PetDataTable;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PetInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PlayableInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

/**
 * @author  Kerberos
 */
public class PetFood implements IItemHandler
{
	private static final int[] ITEM_IDS =
	{
		2515, 4038, 5168, 5169, 6316, 7582, 9668, 10425
	};
	
	/**
	 * 
	 * @see net.sf.l2j.gameserver.handler.IItemHandler#useItem(net.sf.l2j.gameserver.model.actor.instance.L2PlayableInstance, net.sf.l2j.gameserver.model.L2ItemInstance)
	 */
	public void useItem(L2PlayableInstance playable, L2ItemInstance item)
	{
		int itemId = item.getItemId();
		switch (itemId)
		{
			case 2515: // Wolf's food
				useFood(playable, 2048, item);
				break;
			case 4038: // Hatchling's food
				useFood(playable, 2063, item);
				break;
			case 5168: // Strider's food
				useFood(playable, 2101, item);
				break;
			case 5169: // ClanHall / Castle Strider's food
				useFood(playable, 2102, item);
				break;
			case 6316: // Wyvern's food
				useFood(playable, 2180, item);
				break;
			case 7582: // Baby Pet's food
				useFood(playable, 2048, item);
				break;
			case 9668: // Great Wolf's food
				useFood(playable, 2361, item);
				break;
			case 10425: // Improved Baby Pet's food
				useFood(playable, 2361, item);
				break;
		}
	}
	
	public boolean useFood(L2PlayableInstance activeChar, int magicId, L2ItemInstance item)
	{
		L2Skill skill = SkillTable.getInstance().getInfo(magicId, 1);
		
		if (skill != null)
		{
			if (activeChar instanceof L2PetInstance)
			{
				if (((L2PetInstance)activeChar).destroyItem("Consume", item.getObjectId(), 1, null, false))
				{
					activeChar.broadcastPacket(new MagicSkillUse(activeChar, activeChar, magicId, 1, 0, 0));
					((L2PetInstance)activeChar).setCurrentFed(((L2PetInstance)activeChar).getCurrentFed() + skill.getFeed());
					((L2PetInstance)activeChar).broadcastStatusUpdate();
					if (((L2PetInstance)activeChar).getCurrentFed() < (0.55 * ((L2PetInstance)activeChar).getPetData().getPetMaxFeed()))
					{
						SystemMessage sm = new SystemMessage(SystemMessageId.YOUR_PET_ATE_A_LITTLE_BUT_IS_STILL_HUNGRY);
						((L2PetInstance)activeChar).getOwner().sendPacket(sm);
					}
					return true;
				}
			}
			else if (activeChar instanceof L2PcInstance)
			{
				L2PcInstance player = ((L2PcInstance)activeChar);
				int itemId = item.getItemId();
				boolean canUse = false;
				if (player.isMounted())
				{
					int petId = player.getMountNpcId();
					if (L2PetDataTable.isWolf(petId) && L2PetDataTable.isWolfFood(itemId))
					{
						canUse = true;
					}
					else if (L2PetDataTable.isEvolvedWolf(petId) && L2PetDataTable.isEvolvedWolfFood(itemId))
					{
						canUse = true;
					}
					else if (L2PetDataTable.isSinEater(petId) && L2PetDataTable.isSinEaterFood(itemId))
					{
						canUse = true;
					}
					else if (L2PetDataTable.isHatchling(petId) && L2PetDataTable.isHatchlingFood(itemId))
					{
						canUse = true;
					}
					else if (L2PetDataTable.isStrider(petId) && L2PetDataTable.isStriderFood(itemId))
					{
						canUse = true;
					}
					else if (L2PetDataTable.isWyvern(petId) && L2PetDataTable.isWyvernFood(itemId))
					{
						canUse = true;
					}
					else if (L2PetDataTable.isBaby(petId) && L2PetDataTable.isBabyFood(itemId))
					{
						canUse = true;
					}
					else if (L2PetDataTable.isImprovedBaby(petId) && L2PetDataTable.isImprovedBabyFood(itemId))
					{
						canUse = true;
					}
					if (canUse)
					{
						if (player.destroyItem("Consume", item.getObjectId(), 1, null, false))
						{
							player.broadcastPacket(new MagicSkillUse(activeChar, activeChar, magicId, 1, 0, 0));
							player.setCurrentFeed(player.getCurrentFeed() + skill.getFeed());
						}
						return true;
					}
					else
					{
						activeChar.sendPacket(new SystemMessage(SystemMessageId.S1_CANNOT_BE_USED));
						return false;
					}
				}
				else
					player.sendPacket(new SystemMessage(SystemMessageId.S1_CANNOT_BE_USED));
				return false;
			}
		}
		return false;
	}
	
	/**
	 * 
	 * @see net.sf.l2j.gameserver.handler.IItemHandler#getItemIds()
	 */
	public int[] getItemIds()
	{
		return ITEM_IDS;
	}
}
