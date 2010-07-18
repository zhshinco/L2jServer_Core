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
package com.l2jserver.gameserver.network.clientpackets;

import java.util.logging.Logger;

import com.l2jserver.Config;
import com.l2jserver.gameserver.datatables.ItemTable;
import com.l2jserver.gameserver.model.Elementals;
import com.l2jserver.gameserver.model.L2Augmentation;
import com.l2jserver.gameserver.model.L2ItemInstance;
import com.l2jserver.gameserver.model.L2Multisell;
import com.l2jserver.gameserver.model.L2Object;
import com.l2jserver.gameserver.model.L2Multisell.MultiSellEntry;
import com.l2jserver.gameserver.model.L2Multisell.MultiSellIngredient;
import com.l2jserver.gameserver.model.L2Multisell.MultiSellListContainer;
import com.l2jserver.gameserver.model.actor.L2Npc;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.itemcontainer.PcInventory;
import com.l2jserver.gameserver.network.SystemMessageId;
import com.l2jserver.gameserver.network.serverpackets.ExBrExtraUserInfo;
import com.l2jserver.gameserver.network.serverpackets.ItemList;
import com.l2jserver.gameserver.network.serverpackets.StatusUpdate;
import com.l2jserver.gameserver.network.serverpackets.SystemMessage;
import com.l2jserver.gameserver.network.serverpackets.UserInfo;
import com.l2jserver.gameserver.templates.item.L2Armor;
import com.l2jserver.gameserver.templates.item.L2Item;
import com.l2jserver.gameserver.templates.item.L2Weapon;

import javolution.util.FastList;

/**
 * The Class MultiSellChoose.
 */
public class MultiSellChoose extends L2GameClientPacket
{
	private static final String _C__A7_MULTISELLCHOOSE = "[C] A7 MultiSellChoose";
	private static Logger _log = Logger.getLogger(MultiSellChoose.class.getName());
	private int _listId;
	private int _entryId;
	private long _amount;
	private int _enchantment;
	private long _transactionTax; // local handling of taxation
	@SuppressWarnings("unused")
    private int _unk1;
	@SuppressWarnings("unused")
	private int _unk2;
	@SuppressWarnings("unused")
	private int _unk3;
	@SuppressWarnings("unused")
	private int _unk7;
	@SuppressWarnings("unused")
	private int _unk4;
	@SuppressWarnings("unused")
	private int _unk5;
	@SuppressWarnings("unused")
	private int _unk6;
	@SuppressWarnings("unused")
	private int _unk8;
	@SuppressWarnings("unused")
	private int _unk9;
	@SuppressWarnings("unused")
	private int _unk10;
	@SuppressWarnings("unused")
	private int _unk11;
	
	/* (non-Javadoc)
	 * @see com.l2jserver.gameserver.network.clientpackets.L2GameClientPacket#readImpl()
	 */
	@Override
	protected void readImpl()
	{
		_listId = readD();
		_entryId = readD();
		_amount = readQ();
		_unk1 = readH();
		_unk2 = readD();
		_unk3 = readD();
		_unk4 = readH(); // elemental attributes
		_unk5 = readH();// elemental attributes
		_unk6 = readH();// elemental attributes
		_unk7 = readH();// elemental attributes
		_unk8 = readH();// elemental attributes
		_unk9 = readH();// elemental attributes
		_unk10 = readH();// elemental attributes
		_unk11 = readH();// elemental attributes
		_enchantment = _entryId % 100000;
		_entryId = _entryId / 100000;
		_transactionTax = 0; // initialize tax amount to 0...
	}
	
	/* (non-Javadoc)
	 * @see com.l2jserver.gameserver.network.clientpackets.L2GameClientPacket#runImpl()
	 */
	@Override
	public void runImpl()
	{
		L2PcInstance player = getClient().getActiveChar();
		if (player == null)
			return;

		if (!getClient().getFloodProtectors().getMultiSell().tryPerformAction("multisell choose"))
			return;

		if (_amount < 1 || _amount > 5000)
			return;
		
		MultiSellListContainer list = L2Multisell.getInstance().getList(_listId);
		if (list == null)
			return;

		L2Object target = player.getTarget();
		if (!player.isGM() && (target == null
				|| !(target instanceof L2Npc)
				|| !list.checkNpcId(((L2Npc)target).getNpcId())
				|| !((L2Npc)target).canInteract(player)))
			return;

		for (MultiSellEntry entry : list.getEntries())
		{
			if (entry.getEntryId() == _entryId)
			{
				doExchange(player, entry, list.getApplyTaxes(), list.getMaintainEnchantment(), _enchantment);
				return;
			}
		}
	}
	
	/**
	 * Do exchange.
	 *
	 * @param player the player
	 * @param templateEntry the template entry
	 * @param applyTaxes the apply taxes
	 * @param maintainEnchantment the maintain enchantment
	 * @param enchantment the enchantment
	 */
	private void doExchange(L2PcInstance player, MultiSellEntry templateEntry, boolean applyTaxes, boolean maintainEnchantment, int enchantment)
	{
		PcInventory inv = player.getInventory();
		
		// given the template entry and information about maintaining enchantment and applying taxes
		// re-create the instance of the entry that will be used for this exchange
		// i.e. change the enchantment level of select ingredient/products and adena amount appropriately.
		L2Npc merchant = (player.getTarget() instanceof L2Npc) ? (L2Npc) player.getTarget() : null;
		if (merchant == null)
			return;
		
		MultiSellEntry entry = prepareEntry(merchant, templateEntry, applyTaxes, maintainEnchantment, enchantment);

		int slots = 0;
		int weight = 0;
		for (MultiSellIngredient e : entry.getProducts())
		{
			if (e.getItemId() < 0)
				continue;
			L2Item template = ItemTable.getInstance().getTemplate(e.getItemId());
			if (template == null)
				continue;
			if (!template.isStackable())
				slots += e.getItemCount() * _amount;
			else if (player.getInventory().getItemByItemId(e.getItemId()) == null)
				slots++;
			weight += e.getItemCount() * _amount * template.getWeight();
		}
		
		if (!inv.validateWeight(weight))
		{
			player.sendPacket(new SystemMessage(SystemMessageId.WEIGHT_LIMIT_EXCEEDED));
			return;
		}
		if (!inv.validateCapacity(slots))
		{
			player.sendPacket(new SystemMessage(SystemMessageId.SLOTS_FULL));
			return;
		}
		
		// Generate a list of distinct ingredients and counts in order to check if the correct item-counts
		// are possessed by the player
		FastList<MultiSellIngredient> _ingredientsList = FastList.newInstance();
		boolean newIng = true;
		for (MultiSellIngredient e : entry.getIngredients())
		{
			newIng = true;
			
			// at this point, the template has already been modified so that enchantments are properly included
			// whenever they need to be applied.  Uniqueness of items is thus judged by item id AND enchantment level
			for (MultiSellIngredient ex : _ingredientsList)
			{
				// if the item was already added in the list, merely increment the count
				// this happens if 1 list entry has the same ingredient twice (example 2 swords = 1 dual)
				if ((ex.getItemId() == e.getItemId()) && (ex.getEnchantmentLevel() == e.getEnchantmentLevel()))
				{
					if ((double) ex.getItemCount() + e.getItemCount() > Integer.MAX_VALUE)
					{
						player.sendPacket(new SystemMessage(SystemMessageId.YOU_HAVE_EXCEEDED_QUANTITY_THAT_CAN_BE_INPUTTED));
						_ingredientsList.clear();
						_ingredientsList = null;
						return;
					}
					ex.setItemCount(ex.getItemCount() + e.getItemCount());
					newIng = false;
				}
			}
			if (newIng)
			{
				// if it's a new ingredient, just store its info directly (item id, count, enchantment)
				_ingredientsList.add(L2Multisell.getInstance().new MultiSellIngredient(e));
			}
		}
		// now check if the player has sufficient items in the inventory to cover the ingredients' expences
		for (MultiSellIngredient e : _ingredientsList)
		{
			if ((double) e.getItemCount() * _amount > Integer.MAX_VALUE)
			{
				player.sendPacket(new SystemMessage(SystemMessageId.YOU_HAVE_EXCEEDED_QUANTITY_THAT_CAN_BE_INPUTTED));
				_ingredientsList.clear();
				_ingredientsList = null;
				return;
			}
			switch (e.getItemId())
			{
				case -200: // Clan Reputation Score
				{
					if (player.getClan() == null)
					{
						player.sendPacket(new SystemMessage(SystemMessageId.YOU_ARE_NOT_A_CLAN_MEMBER));
						return;
					}
					if (!player.isClanLeader())
					{
						player.sendPacket(new SystemMessage(SystemMessageId.ONLY_THE_CLAN_LEADER_IS_ENABLED));
						return;
					}
					if (player.getClan().getReputationScore() < e.getItemCount() * _amount)
					{
						player.sendPacket(new SystemMessage(SystemMessageId.THE_CLAN_REPUTATION_SCORE_IS_TOO_LOW));
						return;
					}
					break;
				}
				case -300: // Player Fame
				{
					if (player.getFame() < e.getItemCount() * _amount)
					{
						player.sendPacket(new SystemMessage(SystemMessageId.NOT_ENOUGH_FAME_POINTS));
						return;
					}
					break;
				}
				default:
				{
					// if this is not a list that maintains enchantment, check the count of all items that have the given id.
					// otherwise, check only the count of items with exactly the needed enchantment level
					if (inv.getInventoryItemCount(e.getItemId(), maintainEnchantment ? e.getEnchantmentLevel() : -1, false) < ((Config.ALT_BLACKSMITH_USE_RECIPES || !e.getMaintainIngredient()) ? (e.getItemCount() * _amount) : e.getItemCount()))
					{
						player.sendPacket(new SystemMessage(SystemMessageId.NOT_ENOUGH_REQUIRED_ITEMS));//Update by rocknow
						_ingredientsList.clear();
						_ingredientsList = null;
						return;
					}
					break;
				}
			}
		}
		
		FastList.recycle(_ingredientsList);
		FastList<L2Augmentation> augmentation = FastList.newInstance();
		Elementals elemental = null;
		/** All ok, remove items and add final product */
		
		for (MultiSellIngredient e : entry.getIngredients())
		{
			switch (e.getItemId())
			{
				case -200: // Clan Reputation Score
				{
					int repCost = (int) (e.getItemCount() * _amount);
					player.getClan().takeReputationScore(repCost, true);
					SystemMessage smsg = new SystemMessage(SystemMessageId.S1_DEDUCTED_FROM_CLAN_REP);
					smsg.addItemNumber(e.getItemCount() * _amount);
					player.sendPacket(smsg);
					break;
				}
				case -300: // Player Fame
				{
					int fameCost = (int) (player.getFame() - (e.getItemCount() * _amount));
					player.setFame(fameCost);
					player.sendPacket(new UserInfo(player));
					player.sendPacket(new ExBrExtraUserInfo(player));
					break;
				}
				default:
				{
					
					L2ItemInstance itemToTake = inv.getItemByItemId(e.getItemId()); // initialize and initial guess for the item to take.
					if (itemToTake == null)
					{ //this is a cheat, transaction will be aborted and if any items already taken will not be returned back to inventory!
						_log.severe("Character: " + player.getName() + " is trying to cheat in multisell, merchatnt id:" + merchant.getNpcId());
						return;
					}
					
					/*if (itemToTake.isEquipped())
					{ //this is a cheat, transaction will be aborted and if any items already taken will not be returned back to inventory!
						_log.severe("Character: " + player.getName() + " is trying to cheat in multisell, exchanging equipped item, merchatnt id:" + merchant.getNpcId());
						return;
					}*/

					if (itemToTake.isWear())
					{//Player trying to buy something from the Multisell store with an item that's just being used from the Wear option from merchants.
						_log.severe("Character: " + player.getName() + " is trying to cheat in multisell, merchatnt id:" + merchant.getNpcId());
						return;
					}
					
					if (Config.ALT_BLACKSMITH_USE_RECIPES || !e.getMaintainIngredient())
					{
						// if it's a stackable item, just reduce the amount from the first (only) instance that is found in the inventory
						if (itemToTake.isStackable())
						{
							if (!player.destroyItem("Multisell", itemToTake.getObjectId(), (e.getItemCount() * _amount), player.getTarget(), true))
								return;
						}
						else
						{
							// for non-stackable items, one of two scenaria are possible:
							// a) list maintains enchantment: get the instances that exactly match the requested enchantment level
							// b) list does not maintain enchantment: get the instances with the LOWEST enchantment level
							
							// a) if enchantment is maintained, then get a list of items that exactly match this enchantment
							if (maintainEnchantment)
							{
								// loop through this list and remove (one by one) each item until the required amount is taken.
								L2ItemInstance[] inventoryContents = inv.getAllItemsByItemId(e.getItemId(), e.getEnchantmentLevel(), false);
								for (int i = 0; i < (e.getItemCount() * _amount); i++)
								{
									if (inventoryContents[i].isAugmented())
										augmentation.add(inventoryContents[i].getAugmentation());
									if(inventoryContents[i].getElementals() != null)
										elemental = inventoryContents[i].getElementals();
									if (!player.destroyItem("Multisell", inventoryContents[i].getObjectId(), 1, player.getTarget(), true))
										return;
								}
							}
							else
							// b) enchantment is not maintained.  Get the instances with the LOWEST enchantment level
							{
								/* NOTE: There are 2 ways to achieve the above goal.
								 * 1) Get all items that have the correct itemId, loop through them until the lowest enchantment
								 * 		level is found.  Repeat all this for the next item until proper count of items is reached.
								 * 2) Get all items that have the correct itemId, sort them once based on enchantment level,
								 * 		and get the range of items that is necessary.
								 * Method 1 is faster for a small number of items to be exchanged.
								 * Method 2 is faster for large amounts.
								 *
								 * EXPLANATION:
								 *   Worst case scenario for algorithm 1 will make it run in a number of cycles given by:
								 * m*(2n-m+1)/2 where m is the number of items to be exchanged and n is the total
								 * number of inventory items that have a matching id.
								 *   With algorithm 2 (sort), sorting takes n*log(n) time and the choice is done in a single cycle
								 * for case b (just grab the m first items) or in linear time for case a (find the beginning of items
								 * with correct enchantment, index x, and take all items from x to x+m).
								 * Basically, whenever m > log(n) we have: m*(2n-m+1)/2 = (2nm-m*m+m)/2 >
								 * (2nlogn-logn*logn+logn)/2 = nlog(n) - log(n*n) + log(n) = nlog(n) + log(n/n*n) =
								 * nlog(n) + log(1/n) = nlog(n) - log(n) = (n-1)log(n)
								 * So for m < log(n) then m*(2n-m+1)/2 > (n-1)log(n) and m*(2n-m+1)/2 > nlog(n)
								 *
								 * IDEALLY:
								 * In order to best optimize the performance, choose which algorithm to run, based on whether 2^m > n
								 * if ( (2<<(e.getItemCount() * _amount)) < inventoryContents.length )
								 *   // do Algorithm 1, no sorting
								 * else
								 *   // do Algorithm 2, sorting
								 *
								 * CURRENT IMPLEMENTATION:
								 * In general, it is going to be very rare for a person to do a massive exchange of non-stackable items
								 * For this reason, we assume that algorithm 1 will always suffice and we keep things simple.
								 * If, in the future, it becomes necessary that we optimize, the above discussion should make it clear
								 * what optimization exactly is necessary (based on the comments under "IDEALLY").
								 */

								// choice 1.  Small number of items exchanged.  No sorting.
								for (int i = 1; i <= (e.getItemCount() * _amount); i++)
								{
									L2ItemInstance[] inventoryContents = inv.getAllItemsByItemId(e.getItemId(), false);
									
									itemToTake = inventoryContents[0];
									// get item with the LOWEST enchantment level  from the inventory...
									// +0 is lowest by default...
									if (itemToTake.getEnchantLevel() > 0)
									{
										for (L2ItemInstance item : inventoryContents)
										{
											if (item.getEnchantLevel() < itemToTake.getEnchantLevel())
											{
												itemToTake = item;
												// nothing will have enchantment less than 0. If a zero-enchanted
												// item is found, just take it
												if (itemToTake.getEnchantLevel() == 0)
													break;
											}
										}
									}
									if (!player.destroyItem("Multisell", itemToTake.getObjectId(), 1, player.getTarget(), true))
										return;
								}
							}
						}
					}
					break;
				}
			}
		}
		// Generate the appropriate items
		for (MultiSellIngredient e : entry.getProducts())
		{
			switch (e.getItemId())
			{
				case -200: // Clan Reputation Score - now not supported
				{
					//player.getClan().setReputationScore((int)(player.getClan().getReputationScore() + e.getItemCount() * _amount), true);
					break;
				}
				case -300: // Player Fame
				{
					player.setFame((int)(player.getFame() + e.getItemCount() * _amount));
					player.sendPacket(new UserInfo(player));
					player.sendPacket(new ExBrExtraUserInfo(player));
					break;
				}
				default:
				{
					L2ItemInstance tempItem = ItemTable.getInstance().createDummyItem(e.getItemId());
					if (tempItem == null)
					{
						_log.severe("Problem with multisell ID:"+_listId+" entry ID:"+_entryId+" - Product ID:"+e.getItemId()+" not exist.");
						return;
					}
					if (tempItem.isStackable())
					{
						inv.addItem("Multisell", e.getItemId(), (e.getItemCount() * _amount), player, player.getTarget());
					}
					else
					{
						L2ItemInstance product = null;
						for (int i = 0; i < (e.getItemCount() * _amount); i++)
						{
							product = inv.addItem("Multisell", e.getItemId(), 1, player, player.getTarget());
							if (maintainEnchantment)
							{
								if (i < augmentation.size())
									product.setAugmentation(new L2Augmentation(augmentation.get(i).getAugmentationId(), augmentation.get(i).getSkill()));
								if (elemental != null)
									product.setElementAttr(elemental.getElement(), elemental.getValue());
								product.setEnchantLevel(e.getEnchantmentLevel());
								product.updateDatabase();
							}
						}
					}
					// msg part
					SystemMessage sm;
					
					if (e.getItemCount() * _amount > 1)
					{
						sm = new SystemMessage(SystemMessageId.EARNED_S2_S1_S);
						sm.addItemName(e.getItemId());
						sm.addItemNumber(e.getItemCount() * _amount);
						player.sendPacket(sm);
						sm = null;
					}
					else
					{
						if (maintainEnchantment && e.getEnchantmentLevel() > 0)
						{
							sm = new SystemMessage(SystemMessageId.ACQUIRED_S1_S2);
							sm.addItemNumber(e.getEnchantmentLevel());
							sm.addItemName(e.getItemId());
						}
						else
						{
							sm = new SystemMessage(SystemMessageId.EARNED_ITEM);
							sm.addItemName(e.getItemId());
						}
						player.sendPacket(sm);
						sm = null;
					}
				}
			}
		}
		player.sendPacket(new ItemList(player, false));
		
		StatusUpdate su = new StatusUpdate(player);
		su.addAttribute(StatusUpdate.CUR_LOAD, player.getCurrentLoad());
		player.sendPacket(su);
		su = null;
		
		// finally, give the tax to the castle...
		if (merchant.getIsInTown() && merchant.getCastle().getOwnerId() > 0)
			merchant.getCastle().addToTreasury(_transactionTax * _amount);
		
		FastList.recycle(augmentation);
	}
	
	// Regarding taxation, the following appears to be the case:
	// a) The count of aa remains unchanged (taxes do not affect aa directly).
	// b) 5/6 of the amount of aa is taxed by the normal tax rate.
	// c) the resulting taxes are added as normal adena value.
	// d) normal adena are taxed fully.
	// e) Items other than adena and ancient adena are not taxed even when the list is taxable.
	// example: If the template has an item worth 120aa, and the tax is 10%,
	// then from 120aa, take 5/6 so that is 100aa, apply the 10% tax in adena (10a)
	// so the final price will be 120aa and 10a!
	/**
	 * Prepare entry.
	 *
	 * @param merchant the merchant
	 * @param templateEntry the template entry
	 * @param applyTaxes the apply taxes
	 * @param maintainEnchantment the maintain enchantment
	 * @param enchantLevel the enchant level
	 * @return the multi sell entry
	 */
	private MultiSellEntry prepareEntry(L2Npc merchant, MultiSellEntry templateEntry, boolean applyTaxes, boolean maintainEnchantment, int enchantLevel)
	{
		MultiSellEntry newEntry = L2Multisell.getInstance().new MultiSellEntry();
		newEntry.setEntryId(templateEntry.getEntryId());
		long totalAdenaCount = 0;
		boolean hasIngredient = false;
		
		for (MultiSellIngredient ing : templateEntry.getIngredients())
		{
			// load the ingredient from the template
			MultiSellIngredient newIngredient = L2Multisell.getInstance().new MultiSellIngredient(ing);
			
			if (newIngredient.getItemId() == 57 && newIngredient.isTaxIngredient())
			{
				double taxRate = 0.0;
				if (applyTaxes)
				{
					if (merchant != null && merchant.getIsInTown())
						taxRate = merchant.getCastle().getTaxRate();
				}
				
				_transactionTax = Math.round(newIngredient.getItemCount() * taxRate);
				totalAdenaCount += _transactionTax;
				continue; // do not yet add this adena amount to the list as non-taxIngredient adena might be entered later (order not guaranteed)
			}
			else if (ing.getItemId() == 57) // && !ing.isTaxIngredient()
			{
				totalAdenaCount += newIngredient.getItemCount();
				continue; // do not yet add this adena amount to the list as taxIngredient adena might be entered later (order not guaranteed)
			}
			// if it is an armor/weapon, modify the enchantment level appropriately, if necessary
			// not used for clan reputation and fame
			else if (maintainEnchantment && newIngredient.getItemId() > 0)
			{
				L2Item tempItem = ItemTable.getInstance().createDummyItem(newIngredient.getItemId()).getItem();
				if ((tempItem instanceof L2Armor) || (tempItem instanceof L2Weapon))
				{
					newIngredient.setEnchantmentLevel(enchantLevel);
					hasIngredient = true;
				}
			}
			
			// finally, add this ingredient to the entry
			newEntry.addIngredient(newIngredient);
		}
		// Next add the adena amount, if any
		if (totalAdenaCount > 0)
			newEntry.addIngredient(L2Multisell.getInstance().new MultiSellIngredient(57, totalAdenaCount, false, false));
		
		// Now modify the enchantment level of products, if necessary
		for (MultiSellIngredient ing : templateEntry.getProducts())
		{
			// load the ingredient from the template
			MultiSellIngredient newIngredient = L2Multisell.getInstance().new MultiSellIngredient(ing);
			
			if (maintainEnchantment && hasIngredient)
			{
				// if it is an armor/weapon, modify the enchantment level appropriately
				// (note, if maintain enchantment is "false" this modification will result to a +0)
				L2Item tempItem = ItemTable.getInstance().createDummyItem(newIngredient.getItemId()).getItem();
				if ((tempItem instanceof L2Armor) || (tempItem instanceof L2Weapon))
					newIngredient.setEnchantmentLevel(enchantLevel);
			}
			newEntry.addProduct(newIngredient);
		}
		return newEntry;
	}
	
	/* (non-Javadoc)
	 * @see com.l2jserver.gameserver.network.clientpackets.L2GameClientPacket#getType()
	 */
	@Override
	public String getType()
	{
		return _C__A7_MULTISELLCHOOSE;
	}
}
