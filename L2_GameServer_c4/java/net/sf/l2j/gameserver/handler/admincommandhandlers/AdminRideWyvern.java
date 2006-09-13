/*
 * This program is free software; you can redistribute it and/or modify
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
package net.sf.l2j.gameserver.handler.admincommandhandlers;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.serverpackets.Ride;
import net.sf.l2j.gameserver.serverpackets.SystemMessage;

/**
 * @author 
 *
 * TODO nothing.
 */
public class AdminRideWyvern implements IAdminCommandHandler
{
    private static String[] _adminCommands = {
        "admin_ride_wyvern",
        "admin_ride_strider",
        "admin_unride_wyvern",
        "admin_unride_strider",
        "admin_unride",
    };
    private static final int REQUIRED_LEVEL = Config.GM_RIDER;
    private int PetRideId;
    
    public boolean useAdminCommand(String command, L2PcInstance activeChar) {
        
        if (!Config.ALT_PRIVILEGES_ADMIN)
            if (!(checkLevel(activeChar.getAccessLevel()) && activeChar.isGM())) return false;
    
        if(command.startsWith("admin_ride"))
        {
            if(activeChar.isMounted() || activeChar.getPet() != null){
                SystemMessage sm = new SystemMessage(614);
                sm.addString("Already Have a Pet or Mounted.");
                activeChar.sendPacket(sm);
                return false;
            }
            if (command.startsWith("admin_ride_wyvern")) {
            PetRideId = 12621;
            }
            else if (command.startsWith("admin_ride_strider")) {
                PetRideId = 12526;
            }         
            else
            {
                SystemMessage sm = new SystemMessage(614);
                sm.addString("Command '"+command+"' not recognized");
                activeChar.sendPacket(sm);
                return false;
            }
            Ride mount = new Ride(activeChar.getObjectId(), Ride.ACTION_MOUNT, PetRideId);
            activeChar.sendPacket(mount);
            activeChar.broadcastPacket(mount);
            activeChar.setMountType(mount.getMountType());
        }
        else if(command.startsWith("admin_unride"))
        {
            Ride dismount = new Ride(activeChar.getObjectId(), Ride.ACTION_DISMOUNT,0);
            activeChar.broadcastPacket(dismount);
            activeChar.setMountType(0);
        }
        return true;
    }
    
    public String[] getAdminCommandList() {
        return _adminCommands;
    }
    
    private boolean checkLevel(int level) {
        return (level >= REQUIRED_LEVEL);
    }    
}