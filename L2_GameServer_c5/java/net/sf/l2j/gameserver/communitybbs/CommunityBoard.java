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
package net.sf.l2j.gameserver.communitybbs;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.ClientThread;
import net.sf.l2j.gameserver.communitybbs.Manager.ClanBBSManager;
import net.sf.l2j.gameserver.communitybbs.Manager.PostBBSManager;
import net.sf.l2j.gameserver.communitybbs.Manager.RegionBBSManager;
import net.sf.l2j.gameserver.communitybbs.Manager.TopBBSManager;
import net.sf.l2j.gameserver.communitybbs.Manager.TopicBBSManager;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.serverpackets.ShowBoard;
import net.sf.l2j.gameserver.serverpackets.SystemMessage;

public class CommunityBoard
{	
	private static CommunityBoard _instance;
	
	public static CommunityBoard getInstance()
	{
		if (_instance == null)
		{
			_instance = new CommunityBoard();
		}
		
		return _instance;
	}
	
	public void handleCommands(ClientThread client, String command)
	{
		
		L2PcInstance activeChar = client.getActiveChar();
        if(activeChar == null)
            return;        
        if(Config.COMMUNITY_TYPE.toLowerCase().equals("full"))
        {
        	if (command.startsWith("_bbsclan"))
        	{
        		ClanBBSManager.getInstance().parsecmd(command,activeChar);
        	}
        	else if(command.startsWith("_bbsmemo"))
        	{
        		TopicBBSManager.getInstance().parsecmd(command,activeChar);
        	}
        	else if(command.startsWith("_bbstopics"))
        	{
        		TopicBBSManager.getInstance().parsecmd(command,activeChar);
        	}
        	else if(command.startsWith("_bbsposts"))
        	{
        		PostBBSManager.getInstance().parsecmd(command,activeChar);
        	}
        	else if(command.startsWith("_bbstop"))
        	{
        		TopBBSManager.getInstance().parsecmd(command,activeChar);
        	}
        	else if(command.startsWith("_bbshome"))
        	{
        		TopBBSManager.getInstance().parsecmd(command,activeChar);
        	}
        	else if(command.startsWith("_bbsloc"))
        	{
        		RegionBBSManager.getInstance().parsecmd(command,activeChar);
        	}
        	else 
        	{
        		ShowBoard sb = new ShowBoard("<html><body><br><br><center>the command: "+command+" is not implemented yet</center><br><br></body></html>","101");
    			activeChar.sendPacket(sb);
    			activeChar.sendPacket(new ShowBoard(null,"102"));
    			activeChar.sendPacket(new ShowBoard(null,"103"));        	
        	}     
        }
        else  if(Config.COMMUNITY_TYPE.toLowerCase().equals("old"))
        {
        	RegionBBSManager.getInstance().parsecmd(command,activeChar);
        }
        else
        {        	
			activeChar.sendPacket(new SystemMessage(SystemMessage.CB_OFFLINE));			 
        }
	}	

	/**
	 * @param client
	 * @param url
	 * @param arg1
	 * @param arg2
	 * @param arg3
	 * @param arg4
	 * @param arg5
	 */
	public void handleWriteCommands(ClientThread client, String url, String arg1, String arg2, String arg3, String arg4, String arg5)
	{
		L2PcInstance ac;
		ac = client.getActiveChar();			
		if(Config.COMMUNITY_TYPE.toLowerCase().equals("full"))
        {
		if(url.equals("Topic"))
		{
			TopicBBSManager.getInstance().parsewrite(arg1,arg2,arg3,arg4,arg5,ac);
		}
		else if(url.equals("Post"))
		{
			PostBBSManager.getInstance().parsewrite(arg1,arg2,arg3,arg4,arg5,ac);
		}
		else if(url.equals("Region"))
		{
			RegionBBSManager.getInstance().parsewrite(arg1,arg2,arg3,arg4,arg5,ac);
		}
		else 
    	{
    		ShowBoard sb = new ShowBoard("<html><body><br><br><center>the command: "+url+" is not implemented yet</center><br><br></body></html>","101");
			ac.sendPacket(sb);
			ac.sendPacket(new ShowBoard(null,"102"));
			ac.sendPacket(new ShowBoard(null,"103"));        	
    	} 
	 }
		  else  if(Config.COMMUNITY_TYPE.toLowerCase().equals("old"))
	        {
			  RegionBBSManager.getInstance().parsewrite(arg1,arg2,arg3,arg4,arg5,ac);
	        }
	        else
	        {
	        	ShowBoard sb = new ShowBoard("<html><body><br><br><center>The Community board is currently disable</center><br><br></body></html>","101");
				ac.sendPacket(sb);
				ac.sendPacket(new ShowBoard(null,"102"));
				ac.sendPacket(new ShowBoard(null,"103"));     
	        }
	}
}
