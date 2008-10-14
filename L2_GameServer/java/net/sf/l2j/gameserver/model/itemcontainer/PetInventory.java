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
package net.sf.l2j.gameserver.model.itemcontainer;

import net.sf.l2j.gameserver.model.L2ItemInstance.ItemLocation;
import net.sf.l2j.gameserver.model.actor.instance.L2PetInstance;

public class PetInventory extends Inventory
{
	private final L2PetInstance _owner;

	public PetInventory(L2PetInstance owner)
    {
		_owner = owner;
	}

	@Override
	public L2PetInstance getOwner()
    {
        return _owner;
    }
	
	@Override
	public int getOwnerId()
	{
		// gets the L2PcInstance-owner's ID
		int id;
		try
		{
			id = _owner.getOwner().getObjectId();
		}
		catch (NullPointerException e) 
		{
			return 0;
		}
		return id;
	}

	@Override
	protected ItemLocation getBaseLocation()
    {
        return ItemLocation.PET;
    }

	@Override
	protected ItemLocation getEquipLocation()
    {
        return ItemLocation.PET_EQUIP;
    }
}
