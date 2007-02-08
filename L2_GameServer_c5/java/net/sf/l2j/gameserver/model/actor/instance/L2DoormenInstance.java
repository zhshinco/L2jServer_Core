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
package net.sf.l2j.gameserver.model.actor.instance;

import java.util.StringTokenizer;

import net.sf.l2j.gameserver.datatables.ClanTable;
import net.sf.l2j.gameserver.instancemanager.ClanHallManager;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.entity.ClanHall;
import net.sf.l2j.gameserver.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.serverpackets.MyTargetSelected;
import net.sf.l2j.gameserver.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.templates.L2NpcTemplate;

/**
 * This class ...
 * 
 * @version $Revision$ $Date$
 */
public class L2DoormenInstance extends L2FolkInstance
{
    //private static Logger _log = Logger.getLogger(L2DoormenInstance.class.getName());

    private ClanHall _ClanHall;
    private static int Cond_All_False = 0;
    private static int Cond_Busy_Because_Of_Siege = 1;
    private static int Cond_Castle_Owner = 2;
    private static int Cond_Hall_Owner = 3;

    /**
     * @param template
     */
    public L2DoormenInstance(int objectID, L2NpcTemplate template)
    {
        super(objectID, template);
    }

    public final ClanHall getClanHall()
    {
        //_log.warning(this.getName()+" searching ch");
        if (_ClanHall == null)
            _ClanHall = ClanHallManager.getInstance().getClanHall(getX(), getY(), 1500);
        //if (_ClanHall != null)
        //    _log.warning(this.getName()+" found ch "+_ClanHall.getName());
        return _ClanHall;
    }

    public void onBypassFeedback(L2PcInstance player, String command)
    {
        player.sendPacket(new ActionFailed());

        int condition = validateCondition(player);
        if (condition <= Cond_All_False) return;
        if (condition == Cond_Busy_Because_Of_Siege) return;
        else if (condition == Cond_Castle_Owner || condition == Cond_Hall_Owner)
        {
            if (command.startsWith("Chat"))
            {
                showMessageWindow(player);
                return;
            }
            else if (command.startsWith("open_doors"))
            {
                if (condition == Cond_Hall_Owner)
                {
                    getClanHall().openCloseDoors(true);
                    player.sendPacket(new NpcHtmlMessage(
                    		                             getObjectId(),
                                                         "<html><head><body>You have <font color=\"LEVEL\">opened</font> the clan hall door.<br>Outsiders may enter the clan hall while the door is open. Please close it when you've finished your business.<br><center><button value=\"Close\" action=\"bypass -h npc_"
                                                             + getObjectId()
                                                             + "_close_doors\" width=70 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></center></body></html>"));
                }
                else
                {
                    //DoorTable doorTable = DoorTable.getInstance();
                    StringTokenizer st = new StringTokenizer(command.substring(10), ", ");
                    st.nextToken(); // Bypass first value since its castleid/hallid

                    if (condition == 2)
                    {
                        while (st.hasMoreTokens())
                        {
                            getCastle().openDoor(player, Integer.parseInt(st.nextToken()));
                        }
                        return;
                    }

                }
            }
            else if (command.startsWith("close_doors"))
            {
                if (condition == Cond_Hall_Owner)
                {
                    getClanHall().openCloseDoors(false);
                    player.sendPacket(new NpcHtmlMessage(
                    		                             getObjectId(),
                                                         "<html><head><body>You have <font color=\"LEVEL\">closed</font> the clan hall door.<br>Good day!<br><center><button value=\"To Begining\" action=\"bypass -h npc_"
                                                             + getObjectId()
                                                             + "_Chat\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></center></body></html>"));
                }
                else
                {
                    //DoorTable doorTable = DoorTable.getInstance();
                    StringTokenizer st = new StringTokenizer(command.substring(11), ", ");
                    st.nextToken(); // Bypass first value since its castleid/hallid

                    //L2Clan playersClan = player.getClan();

                    if (condition == 2)
                    {
                        while (st.hasMoreTokens())
                        {
                            getCastle().closeDoor(player, Integer.parseInt(st.nextToken()));
                        }
                        return;
                    }
                }
            }
        }

        super.onBypassFeedback(player, command);
    }

    /**
     * this is called when a player interacts with this NPC
     * @param player
     */
    public void onAction(L2PcInstance player)
    {
        player.sendPacket(new ActionFailed());
        player.setTarget(this);
        player.sendPacket(new MyTargetSelected(getObjectId(), -15));

        if (isInsideRadius(player, INTERACTION_DISTANCE, false, false))
            showMessageWindow(player);
    }

    public void showMessageWindow(L2PcInstance player)
    {
        player.sendPacket(new ActionFailed());
        String filename = "data/html/doormen/" + getTemplate().npcId + "-no.htm";

        int condition = validateCondition(player);
        if (condition == Cond_Busy_Because_Of_Siege) filename = "data/html/doormen/"
            + getTemplate().npcId + "-busy.htm"; // Busy because of siege
        else if (condition == Cond_Castle_Owner) // Clan owns castle
            filename = "data/html/doormen/" + getTemplate().npcId + ".htm"; // Owner message window

        // Prepare doormen for clan hall
        NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
        String str;
        if (getClanHall() != null)
        {
            if (condition == Cond_Hall_Owner)
            {
                str = "<html><body>Hello!<br><font color=\"55FFFF\">" + getName()
                    + "</font> I am honored to serve your clan.<br>How may i serve you?<br>";
                str += "<center><table><tr><td><button value=\"Open Door\" action=\"bypass -h npc_%objectId%_open_doors\" width=70 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"><br1></td></tr></table><br>";
                str += "<table><tr><td><button value=\"Close Door\" action=\"bypass -h npc_%objectId%_close_doors\" width=70 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td></tr></table></center></body></html>";
            }
            else
            {
                L2Clan owner = ClanTable.getInstance().getClan(getClanHall().getOwnerId());
                if (owner != null && owner.getLeader() != null)
                {
                    str = "<html><body>Hello there!<br>This clan hall is owned by <font color=\"55FFFF\">"
                        + owner.getLeader().getName() + " who is the Lord of the ";
                    str += owner.getName() + "</font> clan.<br>";
                    str += "I am sorry, but only the clan members who belong to the <font color=\"55FFFF\">"
                        + owner.getName() + "</font> clan can enter the clan hall.</body></html>";
                }
                else str = "<html><body>" + getName() + ":<br1>Clan hall <font color=\"LEVEL\">"
                    + getClanHall().getName()
                    + "</font> have no owner clan.<br>You can rent it at auctioneers..</body></html>";
            }
            html.setHtml(str);
        }
        else html.setFile(filename);

        html.replace("%objectId%", String.valueOf(getObjectId()));
        player.sendPacket(html);
    }

    private int validateCondition(L2PcInstance player)
    {
        if (player.getClan() != null)
        {
            // Prepare doormen for clan hall
            if (getClanHall() != null)
            {
                if (player.getClanId() == getClanHall().getOwnerId()) return Cond_Hall_Owner;
                else return Cond_All_False;
            }
            if (getCastle() != null && getCastle().getCastleId() > 0)
            {
                //		        if (getCastle().getSiege().getIsInProgress())
                //		            return Cond_Busy_Because_Of_Siege;									// Busy because of siege
                //		        else 
                if (getCastle().getOwnerId() == player.getClanId()) // Clan owns castle
                    return Cond_Castle_Owner; // Owner
            }
        }

        return Cond_All_False;
    }
}
