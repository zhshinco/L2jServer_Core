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
package com.l2jserver.gameserver.skills.funcs;

import com.l2jserver.Config;
import com.l2jserver.gameserver.model.L2ItemInstance;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.skills.Env;
import com.l2jserver.gameserver.skills.Stats;
import com.l2jserver.gameserver.templates.item.L2Item;
import com.l2jserver.gameserver.templates.item.L2WeaponType;

public class FuncEnchant extends Func
{
	
	public FuncEnchant(Stats pStat, int pOrder, Object owner, Lambda lambda)
	{
		super(pStat, pOrder, owner);
	}
	
	@Override
	public void calc(Env env)
	{
		if (cond != null && !cond.test(env))
			return;
		L2ItemInstance item = (L2ItemInstance) funcOwner;
		
		int enchant = item.getEnchantLevel();
		
		if (enchant <= 0)
			return;
		
		int overenchant = 0;
		
		if (enchant > 3)
		{
			overenchant = enchant - 3;
			enchant = 3;
		}
		
		if (env.player != null && env.player instanceof L2PcInstance)
		{
			L2PcInstance player = (L2PcInstance)env.player;
			if (player.isInOlympiadMode() && Config.ALT_OLY_ENCHANT_LIMIT >= 0 &&
					(enchant + overenchant) > Config.ALT_OLY_ENCHANT_LIMIT)
			{
				if (Config.ALT_OLY_ENCHANT_LIMIT > 3)
				{
					overenchant = Config.ALT_OLY_ENCHANT_LIMIT - 3;
				}
				else
				{
					overenchant = 0;
					enchant = Config.ALT_OLY_ENCHANT_LIMIT;
				}
			}
		}
		
		if (stat == Stats.MAGIC_DEFENCE || stat == Stats.POWER_DEFENCE)
		{
			env.value += enchant + 3 * overenchant;
			return;
		}
		
		if (stat == Stats.MAGIC_ATTACK)
		{
			switch (item.getItem().getItemGradeSPlus())
			{
				case L2Item.CRYSTAL_S:
				case L2Item.CRYSTAL_S80:
				case L2Item.CRYSTAL_S84:
					// M. Atk. increases by 4 for all weapons.
					// Starting at +4, M. Atk. bonus double.
					env.value += 4 * enchant + 8 * overenchant;
					break;
				case L2Item.CRYSTAL_A:
				case L2Item.CRYSTAL_B:
				case L2Item.CRYSTAL_C:
					// M. Atk. increases by 3 for all weapons.
					// Starting at +4, M. Atk. bonus double.
					env.value += 3 * enchant + 6 * overenchant;
					break;
				case L2Item.CRYSTAL_D:
				case L2Item.CRYSTAL_NONE:
					// M. Atk. increases by 2 for all weapons. Starting at +4, M. Atk. bonus double.
					// Starting at +4, M. Atk. bonus double.
					env.value += 2 * enchant + 4 * overenchant;
					break;
			}
			return;
		}
		
		
		if (item.isWeapon())
		{
			L2WeaponType type = (L2WeaponType) item.getItemType();
			
			switch (item.getItem().getItemGradeSPlus())
			{
				case L2Item.CRYSTAL_S:
				case L2Item.CRYSTAL_S80:
				case L2Item.CRYSTAL_S84:
					switch(type)
					{
						case BOW:
						case CROSSBOW:
							// P. Atk. increases by 10 for bows.
							// Starting at +4, P. Atk. bonus double.
							env.value += 10 * enchant + 20 * overenchant;
							break;
						case BIGSWORD:
						case BIGBLUNT:
						case DUAL:
						case DUALFIST:
						case DUALDAGGER:
							// P. Atk. increases by 6 for two-handed swords, two-handed blunts, dualswords, and two-handed combat weapons.
							// Starting at +4, P. Atk. bonus double.
							env.value += 6 * enchant + 12 * overenchant;
							break;
						default:
							// P. Atk. increases by 5 for one-handed swords, one-handed blunts, daggers, spears, and other weapons.
							// Starting at +4, P. Atk. bonus double.
							env.value += 5 * enchant + 10 * overenchant;
							break;
					}
					break;
				case L2Item.CRYSTAL_A:
					switch(type)
					{
						case BOW:
						case CROSSBOW:
							// P. Atk. increases by 8 for bows.
							// Starting at +4, P. Atk. bonus double.
							env.value += 8 * enchant + 16 * overenchant;
							break;
						case BIGSWORD:
						case BIGBLUNT:
						case DUAL:
						case DUALFIST:
						case DUALDAGGER:
							// P. Atk. increases by 5 for two-handed swords, two-handed blunts, dualswords, and two-handed combat weapons.
							// Starting at +4, P. Atk. bonus double.
							env.value += 5 * enchant + 10 * overenchant;
							break;
						default:
							// P. Atk. increases by 4 for one-handed swords, one-handed blunts, daggers, spears, and other weapons.
							// Starting at +4, P. Atk. bonus double.
							env.value += 4 * enchant + 8 * overenchant;
							break;
					}
					break;
				case L2Item.CRYSTAL_B:
				case L2Item.CRYSTAL_C:
					switch(type)
					{
						case BOW:
						case CROSSBOW:
							// P. Atk. increases by 6 for bows.
							// Starting at +4, P. Atk. bonus double.
							env.value += 6 * enchant + 12 * overenchant;
							break;
						case BIGSWORD:
						case BIGBLUNT:
						case DUAL:
						case DUALFIST:
						case DUALDAGGER:
							// P. Atk. increases by 4 for two-handed swords, two-handed blunts, dualswords, and two-handed combat weapons.
							// Starting at +4, P. Atk. bonus double.
							env.value += 4 * enchant + 8 * overenchant;
							break;
						default:
							// P. Atk. increases by 3 for one-handed swords, one-handed blunts, daggers, spears, and other weapons.
							// Starting at +4, P. Atk. bonus double.
							env.value += 3 * enchant + 6 * overenchant;
							break;
					}
					break;
				case L2Item.CRYSTAL_D:
				case L2Item.CRYSTAL_NONE:
					switch(type)
					{
						case BOW:
						case CROSSBOW:
						{
							// Bows increase by 4.
							// Starting at +4, P. Atk. bonus double.
							env.value += 4 * enchant + 8 * overenchant;
							break;
						}
						default:
							// P. Atk. increases by 2 for all weapons with the exception of bows.
							// Starting at +4, P. Atk. bonus double.
							env.value += 2 * enchant + 4 * overenchant;
							break;
					}
					break;
			}
		}
	}
}
