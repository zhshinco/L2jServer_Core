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

import java.util.StringTokenizer;
import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.GMAudit;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2ControllableMobInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.serverpackets.SystemMessage;

/**
 * This class handles following admin commands:
 * - kill = kills target L2Character
 * - kill_monster = kills target non-player
 * 
 * - kill <radius> = If radius is specified, then ALL players only in that radius will be killed.
 * - kill_monster <radius> = If radius is specified, then ALL non-players only in that radius will be killed.
 * 
 * @version $Revision: 1.2.4.5 $ $Date: 2005/04/11 10:06:06 $
 */
public class AdminKill implements IAdminCommandHandler 
{
	private static Logger _log = Logger.getLogger(AdminKill.class.getName());
	private static String[] _adminCommands = {"admin_kill", "admin_kill_monster"};
	private static final int REQUIRED_LEVEL = Config.GM_NPC_EDIT;
	
	private boolean checkLevel(int level) 
	{
		return (level >= REQUIRED_LEVEL);
	}

	public boolean useAdminCommand(String command, L2PcInstance activeChar) 
	{
        if (!Config.ALT_PRIVILEGES_ADMIN)
            if (!(checkLevel(activeChar.getAccessLevel()) && activeChar.isGM())) 
            	return false;
        
		String target = (activeChar.getTarget() != null) ? activeChar.getTarget().getName() : "no-target";
        GMAudit.auditGMAction(activeChar.getName(), command, target, "");

        if (command.startsWith("admin_kill")) 
        {
        	StringTokenizer st = new StringTokenizer(command, " ");
        	st.nextToken(); // skip command

        	if (st.hasMoreTokens())
        	{
	        	String firstParam = st.nextToken();
	            L2PcInstance plyr = L2World.getInstance().getPlayer(firstParam);
	            if (plyr != null)
	            {
	            	if (st.hasMoreTokens())
	            	{
	                	try 
	                	{
	                		int radius  = Integer.parseInt(st.nextToken());
	
	                		for (L2Character knownChar : plyr.getKnownList().getKnownCharactersInRadius(radius))
	                		{
	                			if (knownChar == null || knownChar instanceof L2ControllableMobInstance || knownChar.equals(activeChar)) continue;
	                			
	                			kill(activeChar, knownChar);
	                		}

		            		activeChar.sendMessage("Killed all characters within a " + radius + " unit radius.");
		            		return true;
	                	}
	    				catch (NumberFormatException e) {
	    					activeChar.sendMessage("Invalid radius.");
	    					return false;
	    				}
	            	} else
	            	{
            			kill(activeChar, plyr);
	            	}
	            }
	            else
	            {
	            	try 
	            	{
	            		int radius  = Integer.parseInt(firstParam);

                		for (L2Character knownChar : activeChar.getKnownList().getKnownCharactersInRadius(radius))
                		{
                			if (knownChar == null || knownChar instanceof L2ControllableMobInstance || knownChar.equals(activeChar)) continue;
                			
                			kill(activeChar, knownChar);
                		}
	            		
	            		activeChar.sendMessage("Killed all characters within a " + radius + " unit radius.");
	            		return true;
	            	}
					catch (NumberFormatException e) {
						activeChar.sendMessage("Enter a valid player name or radius.");
						return false;
					}
	            }
        	} else
        	{
        		L2Object obj = activeChar.getTarget();

                if (obj == null || obj instanceof L2ControllableMobInstance || !(obj instanceof L2Character))
                {
                    activeChar.sendPacket(new SystemMessage(SystemMessage.INCORRECT_TARGET));
                } else
                {
                	kill(activeChar, (L2Character)obj);
                }
        	}
        }
        
		return true;
	}
    
    private void kill(L2PcInstance activeChar, L2Character target)
    {
    	if (target instanceof L2PcInstance)
    		target.reduceCurrentHp(target.getMaxHp() + target.getMaxCp() + 1, activeChar);
    	else
    		target.reduceCurrentHp(target.getMaxHp() + 1, activeChar);

		if (Config.DEBUG) 
		    _log.fine("GM: "+activeChar.getName()+"("+activeChar.getObjectId()+")"+
		              " killed character "+target.getObjectId());
    }
	
	public String[] getAdminCommandList() 
	{
		return _adminCommands;
	}
}
