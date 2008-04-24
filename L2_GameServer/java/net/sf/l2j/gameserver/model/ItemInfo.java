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
package net.sf.l2j.gameserver.model;

import net.sf.l2j.gameserver.templates.L2Item;

/**
 * Get all information from L2ItemInstance to generate ItemInfo.<BR><BR>
 *
 */
public class ItemInfo
{
    /** Identifier of the L2ItemInstance */
    private int _objectId;

    /** The L2Item template of the L2ItemInstance */
    private L2Item _item;

    /** The level of enchant on the L2ItemInstance */
    private int _enchant;

	/** The augmentation of the item */
    private int _augmentation;

    /** The quantity of L2ItemInstance */
    private int _count;

    /** The price of the L2ItemInstance */
    private int _price;

    /** The custom L2ItemInstance types (used loto, race tickets) */
    private int _type1;
    private int _type2;

    /** If True the L2ItemInstance is equipped */
    private int _equipped;

    /** The action to do clientside (1=ADD, 2=MODIFY, 3=REMOVE) */
    private int _change;

	/** The mana of this item */
    private int _mana;
    
    private int _location;

    private int _element;
    private int _val;
    private int _fire;
    private int _water;
    private int _earth;
    private int _wind;
    private int _holy;
    private int _unholy;

    /**
     * Get all information from L2ItemInstance to generate ItemInfo.<BR><BR>
     *
     */
	public ItemInfo(L2ItemInstance item)
	{
		if (item == null) return;

        // Get the Identifier of the L2ItemInstance
		_objectId = item.getObjectId();

        // Get the L2Item of the L2ItemInstance
		_item = item.getItem();

        // Get the enchant level of the L2ItemInstance
		_enchant = item.getEnchantLevel();

		// Get the augmentation boni
		if (item.isAugmented()) _augmentation = item.getAugmentation().getAugmentationId();
		else _augmentation = 0;

        // Get the quantity of the L2ItemInstance
		_count = item.getCount();

        // Get custom item types (used loto, race tickets)
		_type1 = item.getCustomType1();
		_type2 = item.getCustomType2();

        // Verify if the L2ItemInstance is equipped
		_equipped = item.isEquipped() ? 1 : 0;

        // Get the action to do clientside
        switch (item.getLastChange())
        {
            case (L2ItemInstance.ADDED): { _change = 1; break; }
            case (L2ItemInstance.MODIFIED): { _change = 2; break; }
            case (L2ItemInstance.REMOVED): { _change = 3; break;}
        }

		// Get shadow item mana
		_mana = item.getMana();
		
		_location = item.getLocationSlot();
	}

	public ItemInfo(L2ItemInstance item, int change)
	{
        if (item == null) return;
        
        // Get the Identifier of the L2ItemInstance
		_objectId = item.getObjectId();
        
        // Get the L2Item of the L2ItemInstance
		_item = item.getItem();
        
        // Get the enchant level of the L2ItemInstance
		_enchant = item.getEnchantLevel();
		
		// Get the augmentation boni
		if (item.isAugmented()) _augmentation = item.getAugmentation().getAugmentationId();
		else _augmentation = 0;
        
        // Get the quantity of the L2ItemInstance
		_count = item.getCount();
        
        // Get custom item types (used loto, race tickets)
		_type1 = item.getCustomType1();
		_type2 = item.getCustomType2();
        
        // Verify if the L2ItemInstance is equipped
		_equipped = item.isEquipped() ? 1 : 0;
        
        // Get the action to do clientside
		_change = change;
		
		// Get shadow item mana
		_mana = item.getMana();
		
		_location = item.getLocationSlot();
        
        _element = item.getAttackAttrElement();
        _val = item.getAttackAttrElementVal();
        _fire = item.getDefAttrFire();
        _water = item.getDefAttrWater();
        _wind = item.getDefAttrWind();
        _earth = item.getDefAttrEarth();
        _holy = item.getDefAttrHoly();
        _unholy = item.getDefAttrUnholy();
	}
	
    
	public int getObjectId(){return _objectId;}
	public L2Item getItem(){return _item;}
	public int getEnchant(){return _enchant;}
	public int getAugmentationBonus(){return _augmentation;}
	public int getCount(){return _count;}
	public int getPrice(){return _price;}
	public int getCustomType1(){return _type1;}
	public int getCustomType2(){return _type2;}
	public int getEquipped(){return _equipped;}
	public int getChange(){return _change;}
	public int getMana(){return _mana;}
	public int getLocation(){return _location;}
    public int getAttackAttrElement(){return _element;}
    public int getAttackAttrElementVal(){return _val;}
    public int getDefAttrFire(){return _fire;}
    public int getDefAttrWater(){return _water;}
    public int getDefAttrWind(){return _wind;}
    public int getDefAttrEarth(){return _earth;}
    public int getDefAttrHoly(){return _holy;}
    public int getDefAttrUnholy(){return _unholy;}
    
}
