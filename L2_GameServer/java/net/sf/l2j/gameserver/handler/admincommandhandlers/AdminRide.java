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
package net.sf.l2j.gameserver.handler.admincommandhandlers;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.serverpackets.SystemMessage;

/**
 * @author 
 */
public class AdminRide implements IAdminCommandHandler
{
    private static final String[] ADMIN_COMMANDS = {
        "admin_ride_wyvern",
        "admin_ride_strider",
        "admin_unride_wyvern",
        "admin_unride_strider",
        "admin_unride", 
        "admin_ride_wolf",
        "admin_unride_wolf",
    };
    private static final int REQUIRED_LEVEL = Config.GM_RIDER;
    private int _petRideId;

    public boolean useAdminCommand(String command, L2PcInstance activeChar) {

        if (!Config.ALT_PRIVILEGES_ADMIN)
            if (!(checkLevel(activeChar.getAccessLevel()) && activeChar.isGM())) return false;

        if(command.startsWith("admin_ride"))
        {
            if(activeChar.isMounted() || activeChar.getPet() != null){
                SystemMessage sm = new SystemMessage(SystemMessageId.S1_S2);
                sm.addString("Already Have a Pet or Mounted.");
                activeChar.sendPacket(sm);
                return false;
            }
            if (command.startsWith("admin_ride_wyvern")) {
            	_petRideId = 12621;
            }
            else if (command.startsWith("admin_ride_strider")) {
            	_petRideId = 12526;
            }
            else if (command.startsWith("admin_ride_wolf")) { 
            	_petRideId = 16030; 
            }
            else
            {
                SystemMessage sm = new SystemMessage(SystemMessageId.S1_S2);
                sm.addString("Command '"+command+"' not recognized");
                activeChar.sendPacket(sm);
                return false;
            }
            
            activeChar.mount(_petRideId, 0);
            
            return false;
        }
        else if(command.startsWith("admin_unride"))
        {
            activeChar.dismount();
        }
        return true;
    }

    public String[] getAdminCommandList() {
        return ADMIN_COMMANDS;
    }

    private boolean checkLevel(int level) {
        return (level >= REQUIRED_LEVEL);
    }
}
