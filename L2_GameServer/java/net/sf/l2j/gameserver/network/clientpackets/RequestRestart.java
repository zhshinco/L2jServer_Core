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
package net.sf.l2j.gameserver.network.clientpackets;

import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.Olympiad;
import net.sf.l2j.gameserver.SevenSignsFestival;
import net.sf.l2j.gameserver.communitybbs.Manager.RegionBBSManager;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.model.L2Party;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.entity.TvTEvent;
import net.sf.l2j.gameserver.network.L2GameClient;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.L2GameClient.GameClientState;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.CharSelectionInfo;
import net.sf.l2j.gameserver.network.serverpackets.RestartResponse;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.taskmanager.AttackStanceTaskManager;

/**
 * This class ...
 *
 * @version $Revision: 1.11.2.1.2.4 $ $Date: 2005/03/27 15:29:30 $
 */
public final class RequestRestart extends L2GameClientPacket
{
    private static final String _C__46_REQUESTRESTART = "[C] 46 RequestRestart";
    private static Logger _log = Logger.getLogger(RequestRestart.class.getName());


    @Override
	protected void readImpl()
    {
    	// trigger
    }

    @Override
	protected void runImpl()
    {
        L2PcInstance player = getClient().getActiveChar();
        if (player == null)
        {
            _log.warning("[RequestRestart] activeChar null!?");
            return;
        }

        if (player.isInOlympiadMode() || Olympiad.getInstance().isRegistered(player))
        {
            player.sendMessage("You cant logout in olympiad mode");
            return;
        }

        if (player.isTeleporting()) {
        	player.abortCast();
        	player.setIsTeleporting(false);
        }
        
        player.getInventory().updateDatabase();

        if (player.getPrivateStoreType() != 0)
        {
            player.sendMessage("Cannot restart while trading");
            return;
        }

        if (player.getActiveRequester() != null)
        {
            player.getActiveRequester().onTradeCancel(player);
            player.onTradeCancel(player.getActiveRequester());
        }

        if (AttackStanceTaskManager.getInstance().getAttackStanceTask(player))
        {
            if (Config.DEBUG)
                _log.fine("Player " + player.getName() + " tried to logout while fighting.");

            player.sendPacket(new SystemMessage(SystemMessageId.CANT_RESTART_WHILE_FIGHTING));
            player.sendPacket(ActionFailed.STATIC_PACKET);
            return;
        }

        // Prevent player from restarting if they are a festival participant
        // and it is in progress, otherwise notify party members that the player
        // is not longer a participant.
        if (player.isFestivalParticipant())
        {
            if (SevenSignsFestival.getInstance().isFestivalInitialized())
            {
                player.sendMessage("You cannot restart while you are a participant in a festival.");
                player.sendPacket(ActionFailed.STATIC_PACKET);
                return;
            }
            L2Party playerParty = player.getParty();

            if (playerParty != null)
                player.getParty().broadcastToPartyMembers(
                                                          SystemMessage.sendString(player.getName()
                                                              + " has been removed from the upcoming festival."));
        }
        if (player.isFlying())
        {
        	player.removeSkill(SkillTable.getInstance().getInfo(4289, 1));
        }

        L2GameClient client = getClient();

        // detach the client from the char so that the connection isnt closed in the deleteMe
        player.setClient(null);

        TvTEvent.onLogout(player);
        RegionBBSManager.getInstance().changeCommunityBoard();

        // removing player from the world
        player.deleteMe();
        L2GameClient.saveCharToDisk(client.getActiveChar());


        getClient().setActiveChar(null);

        // return the client to the authed status
        client.setState(GameClientState.AUTHED);

        RestartResponse response = new RestartResponse();
        sendPacket(response);

        // send char list
        CharSelectionInfo cl = new CharSelectionInfo(client.getAccountName(),
        									   client.getSessionId().playOkID1);
        sendPacket(cl);
        client.setCharSelection(cl.getCharInfo());
    }

    /* (non-Javadoc)
     * @see net.sf.l2j.gameserver.clientpackets.ClientBasePacket#getType()
     */
    @Override
	public String getType()
    {
        return _C__46_REQUESTRESTART;
    }
}