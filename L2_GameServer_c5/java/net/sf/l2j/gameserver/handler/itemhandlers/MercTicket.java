package net.sf.l2j.gameserver.handler.itemhandlers;

import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.instancemanager.CastleManager;
import net.sf.l2j.gameserver.instancemanager.MercTicketManager;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PlayableInstance;
import net.sf.l2j.gameserver.model.entity.Castle;

public class MercTicket implements IItemHandler
{
    private static String[] _Messages = {
                                         "To arms!.",
                                         "I am ready to serve you my lord when the time comes.",
                                         "You summon me."
                                        };
    
    /**
     * handler for using mercenary tickets.  Things to do:
     * 1) Check constraints:
     * 1.a) Tickets may only be used in a castle
     * 1.b) Only specific tickets may be used in each castle (different tickets for each castle)
     * 1.c) only the owner of that castle may use them
     * 1.d) tickets cannot be used during siege
     * 1.e) Check if max number of tickets has been reached
     * 1.f) Check if max number of tickets from this ticket's TYPE has been reached
     * 2) If allowed, call the MercTicketManager to add the item and spawn in the world
     * 3) Remove the item from the person's inventory  
     */
    public void useItem(L2PlayableInstance playable, L2ItemInstance item)
    {
    	int itemId = item.getItemId();
    	L2PcInstance activeChar = (L2PcInstance)playable;
    	Castle castle = CastleManager.getInstance().getCastle(activeChar);
    	int castleId = -1;
    	if (castle != null) castleId = castle.getCastleId();
    	
    	//add check that certain tickets can only be placed in certain castles
    	if (MercTicketManager.getInstance().getTicketCastleId(itemId) != castleId)
    	{
    		switch (castleId)
    		{
    		case 1:activeChar.sendMessage("This Mercenary Ticket can only be used in Gludio.");return;
    		case 2:activeChar.sendMessage("This Mercenary Ticket can only be used in Dion.");return;
    		case 3:activeChar.sendMessage("This Mercenary Ticket can only be used in Giran.");return;
    		case 4:activeChar.sendMessage("This Mercenary Ticket can only be used in Oren.");return;
    		case 5:activeChar.sendMessage("This Mercenary Ticket can only be used in Aden.");return;
    		case 6:activeChar.sendMessage("This Mercenary Ticket can only be used in Hienne.");return;
    		// NOTE: rune and goddard will be 7 & 8, but I may have gotten them backwards.
    		case 7:activeChar.sendMessage("This Mercenary Ticket can only be used in Rune.");return;	
    		case 8:activeChar.sendMessage("This Mercenary Ticket can only be used in Goddard.");return;
    		// player is not in a castle
    		default: activeChar.sendMessage("Mercenary Tickets can only be used in a castle.");return;
    		}
    	}
    	
        if (!activeChar.isCastleLord(castleId))
        {
        	activeChar.sendMessage("You are not the lord of this castle!");
            return;
        }
        
        if (castle.getSiege().getIsInProgress())
        {
            activeChar.sendMessage("You cannot hire mercenary while siege is in progress!");
            return;
        }
        
        if(MercTicketManager.getInstance().isAtCasleLimit(item.getItemId()))
        {
            activeChar.sendMessage("You cannot hire any more mercenaries");
            return;
        }
        if(MercTicketManager.getInstance().isAtTypeLimit(item.getItemId()))
        {
            activeChar.sendMessage("You cannot hire any more mercenaries of this type.  You may still hire other types of mercenaries");
            return;
        }
 
        int npcId = MercTicketManager.getInstance().addTicket(item.getItemId(), activeChar, _Messages);
        activeChar.destroyItem("Consume", item.getObjectId(), 1, null, false); // Remove item from char's inventory
        activeChar.sendMessage("Hired mercenary ("+itemId+","+npcId+") at coords:" + activeChar.getX() + "," + activeChar.getY() + "," + activeChar.getZ() + " heading:" + activeChar.getHeading());
    }

    // left in here for backward compatibility
    public int[] getItemIds()
    {
        return MercTicketManager.getInstance().getItemIds();
    }
}
