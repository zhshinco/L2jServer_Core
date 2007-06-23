
package net.sf.l2j.gameserver.serverpackets;

import net.sf.l2j.gameserver.model.L2Character;
/**
 * This class ...
 * 
 * @version $Revision: 1.3.2.1.2.3 $ $Date: 2005/03/27 15:29:39 $
 */
public class ValidateLocationInVehicle extends L2GameServerPacket
{
    private static final String _S__73_ValidateLocationInVehicle = "[S] 73 ValidateLocationInVehicle";
    private L2Character _activeChar;


    /**
     * 0x73 ValidateLocationInVehicle         hdd 
     * @param _characters
     */
    public ValidateLocationInVehicle(L2Character player)
    {
    	_activeChar = player;
    }
    
    protected final void writeImpl()
    {
        writeC(0x73);
        writeD(_activeChar.getObjectId());
        writeD(1343225858); //TODO verify vehicle object id ??
        writeD(_activeChar.getX());
        writeD(_activeChar.getY());
        writeD(_activeChar.getZ());
        writeD(_activeChar.getHeading());
    }

    /* (non-Javadoc)
     * @see net.sf.l2j.gameserver.serverpackets.ServerBasePacket#getType()
     */
    public String getType()
    {
        return _S__73_ValidateLocationInVehicle;
    }
}
