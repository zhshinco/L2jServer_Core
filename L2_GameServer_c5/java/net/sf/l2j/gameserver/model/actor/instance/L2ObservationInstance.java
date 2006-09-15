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

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.instancemanager.SiegeManager;
import net.sf.l2j.gameserver.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.serverpackets.ItemList;
import net.sf.l2j.gameserver.templates.L2NpcTemplate;

/**
 * @author NightMarez
 * @version $Revision: 1.3.2.2.2.5 $ $Date: 2005/03/27 15:29:32 $
 *
 */
public final class L2ObservationInstance extends L2FolkInstance
{
    //private static Logger _log = Logger.getLogger(L2TeleporterInstance.class.getName());

    /**
     * @param template
     */
    public L2ObservationInstance(int objectId, L2NpcTemplate template)
    {
        super(objectId, template);
    }

    public void onBypassFeedback(L2PcInstance player, String command)
    {
        // first do the common stuff
        // and handle the commands that all NPC classes know
        super.onBypassFeedback(player, command);

        if (command.startsWith("observeSiege"))
        {
            String val = command.substring(13);
            StringTokenizer st = new StringTokenizer(val);
            st.nextToken(); // Bypass cost

            if (SiegeManager.getInstance().checkIfInZone(Integer.parseInt(st.nextToken()),
                                                         Integer.parseInt(st.nextToken()))) doObserve(
                                                                                                      player,
                                                                                                      val);
            else player.sendMessage("You may only view castles during a siege");
        }
        else if (command.startsWith("observe"))
        {
            doObserve(player, command.substring(8));
        }
    }

    public String getHtmlPath(int npcId, int val)
    {
        String pom = "";
        if (val == 0)
        {
            pom = "" + npcId;
        }
        else
        {
            pom = npcId + "-" + val;
        }

        return "data/html/observation/" + pom + ".htm";
    }

    public void onAction(L2PcInstance player)
    {
        if (Config.DEBUG) _log.fine("Teleporter activated");
        super.onAction(player);
    }

    private void doObserve(L2PcInstance player, String val)
    {
        StringTokenizer st = new StringTokenizer(val);
        int cost = Integer.parseInt(st.nextToken());
        int x = Integer.parseInt(st.nextToken());
        int y = Integer.parseInt(st.nextToken());
        int z = Integer.parseInt(st.nextToken());
        if (player.reduceAdena("Broadcast", cost, this, true))
        {
            // enter mode
            player.enterObserverMode(x, y, z);
            ItemList il = new ItemList(player, false);
            player.sendPacket(il);
        }
        player.sendPacket(new ActionFailed());
    }
}
