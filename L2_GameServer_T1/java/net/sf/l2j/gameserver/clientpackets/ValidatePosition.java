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
package net.sf.l2j.gameserver.clientpackets;

import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.GeoData;
import net.sf.l2j.gameserver.TaskPriority;
import net.sf.l2j.gameserver.Universe;
import net.sf.l2j.gameserver.geoeditorcon.GeoEditorListener;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.serverpackets.PartyMemberPosition;
import net.sf.l2j.gameserver.serverpackets.ValidateLocation;
import net.sf.l2j.gameserver.serverpackets.ValidateLocationInVehicle;

/**
 * This class ...
 *
 * @version $Revision: 1.13.4.7 $ $Date: 2005/03/27 15:29:30 $
 */
public class ValidatePosition extends L2GameClientPacket
{
    private static Logger _log = Logger.getLogger(ValidatePosition.class.getName());
    private static final String _C__48_VALIDATEPOSITION = "[C] 48 ValidatePosition";

    /** urgent messages, execute immediatly */
    public TaskPriority getPriority() { return TaskPriority.PR_HIGH; }

    private int _x;
    private int _y;
    private int _z;
    private int _heading;
    @SuppressWarnings("unused")
    private int _data;


    @Override
	protected void readImpl()
    {
        _x  = readD();
        _y  = readD();
        _z  = readD();
        _heading  = readD();
        _data  = readD();
    }

    @Override
	protected void runImpl()
    {
        L2PcInstance activeChar = getClient().getActiveChar();
        if (activeChar == null || activeChar.isTeleporting()) return;

        if (Config.GEODATA > 0 
        		&& (activeChar.isInOlympiadMode() || activeChar.isInsideZone(L2Character.ZONE_SIEGE))
        		&& !activeChar.isFlying()
        		&& GeoData.getInstance().hasGeo(_x, _y))
        {
        	// check Z coordinate sent by client
        	short geoHeight = GeoData.getInstance().getSpawnHeight(_x, _y, activeChar.getZ()-30, activeChar.getZ()+30, activeChar.getObjectId());
        	if (Math.abs(geoHeight - _z) > 15)
        	{
        		// causes mild flashing in the middle of a drop from a castle wall for example
        		_z = geoHeight;
        		// System.out.println("Spawnheight validation diff="+Math.abs(geoHeight - _z));
        	}
        }
        if (Config.COORD_SYNCHRONIZE > 0)
        {
            activeChar.setClientX(_x);
            activeChar.setClientY(_y);
            activeChar.setClientZ(_z);
            activeChar.setClientHeading(_heading);
            int realX = activeChar.getX();
            int realY = activeChar.getY();
            // int realZ = activeChar.getZ();

            double dx = _x - realX;
            double dy = _y - realY;
            double diffSq = (dx*dx + dy*dy);

            /*
            if (Config.DEVELOPER && false)
            {
            	int dxs = (_x - activeChar._lastClientPosition.x);
            	int dys = (_y - activeChar._lastClientPosition.y);
            	int dist = (int)Math.sqrt(dxs*dxs + dys*dys);
            	int heading = dist > 0 ? (int)(Math.atan2(-dys/dist, -dxs/dist) * 10430.378350470452724949566316381) + 32768 : 0;
                _log.info("Client X:" + _x + ", Y:" + _y + ", Z:" + _z + ", H:" + _heading + ", Dist:" + activeChar.getLastClientDistance(_x, _y, _z));
                _log.info("Server X:" + realX + ", Y:" + realY + ", Z:" + realZ + ", H:" + activeChar.getHeading() + ", Dist:" + activeChar.getLastServerDistance(realX, realY, realZ));
            }
        	*/

            if (diffSq > 0 && diffSq < 250000) // if too large, messes observation
            {
                if ((Config.COORD_SYNCHRONIZE & 1) == 1
                    && (!activeChar.isMoving() // character is not moving, take coordinates from client
                    || !activeChar.validateMovementHeading(_heading))) // Heading changed on client = possible obstacle
                {
                    if (Config.DEVELOPER)
                        _log.info(activeChar.getName() + ": Synchronizing position Client --> Server" + (activeChar.isMoving()?" (collision)":" (stay sync)"));
                    
                    if (diffSq < 2500) // 50*50 - attack won't work fluently if even small differences are corrected
                    	activeChar.setXYZ(realX, realY, _z);
                    else
                    	activeChar.setXYZ(_x, _y, _z);
                    activeChar.setHeading(_heading);
                }
                else if ((Config.COORD_SYNCHRONIZE & 2) == 2
                        && diffSq > 10000) // more than can be considered to be result of latency
                {
                    if (Config.DEVELOPER)
                        _log.info(activeChar.getName() + ": Synchronizing position Server --> Client");
                    if (activeChar.isInBoat())
                    {
                        sendPacket(new ValidateLocationInVehicle(activeChar));
                    }
                    else
                    {
                    	activeChar.sendPacket(new ValidateLocation(activeChar));
                    }
                }
            }
            activeChar.setLastClientPosition(_x, _y, _z);
            activeChar.setLastServerPosition(activeChar.getX(), activeChar.getY(), activeChar.getZ());
        }
        else if (Config.COORD_SYNCHRONIZE == -1)
        {
            activeChar.setClientX(_x);
            activeChar.setClientY(_y);
            activeChar.setClientZ(_z);
            activeChar.setClientHeading(_heading); // No real need to validate heading.
            int realX = activeChar.getX();
            int realY = activeChar.getY();
            int realZ = activeChar.getZ();

            double dx = _x - realX;
            double dy = _y - realY;
            double diffSq = (dx*dx + dy*dy);
            if (diffSq < 250000)
                activeChar.setXYZ(realX,realY,_z);

            //TODO: do we need to validate?
            /*double dx = (_x - realX);
             double dy = (_y - realY);
             double dist = Math.sqrt(dx*dx + dy*dy);
             if ((dist < 500)&&(dist > 2)) //check it wasnt teleportation, and char isn't there yet
             activeChar.sendPacket(new CharMoveToLocation(activeChar));*/

            if (Config.DEBUG) {
                _log.fine("client pos: "+ _x + " "+ _y + " "+ _z +" head "+ _heading);
                _log.fine("server pos: "+ realX + " "+realY+ " "+realZ +" head "+activeChar.getHeading());
            }

            if (Config.ACTIVATE_POSITION_RECORDER && !activeChar.isFlying() && Universe.getInstance().shouldLog(activeChar.getObjectId()))
                Universe.getInstance().registerHeight(realX, realY, _z);

            if (Config.DEVELOPER)
            {
                if (diffSq > 1000000) {
                    if (Config.DEBUG) _log.fine("client/server dist diff "+ (int)Math.sqrt(diffSq));
                    if (activeChar.isInBoat())
                    {
                        sendPacket(new ValidateLocationInVehicle(activeChar));
                    }
                    else
                    {
                    	activeChar.sendPacket(new ValidateLocation(activeChar));
                    }
                }
            }
        }
		if(activeChar.getParty() != null)
			activeChar.getParty().broadcastToPartyMembers(activeChar,new PartyMemberPosition(activeChar));

		if (Config.ACCEPT_GEOEDITOR_CONN)
            if (GeoEditorListener.getInstance().getThread() != null  && GeoEditorListener.getInstance().getThread().isWorking()  && GeoEditorListener.getInstance().getThread().isSend(activeChar))
            	GeoEditorListener.getInstance().getThread().sendGmPosition(_x,_y,(short)_z);
    }

    /* (non-Javadoc)
     * @see net.sf.l2j.gameserver.clientpackets.ClientBasePacket#getType()
     */
    @Override
	public String getType()
    {
        return _C__48_VALIDATEPOSITION;
    }

    @Deprecated
    public boolean equal(ValidatePosition pos)
    {
        return _x == pos._x && _y == pos._y && _z == pos._z && _heading == pos._heading;
    }
}
