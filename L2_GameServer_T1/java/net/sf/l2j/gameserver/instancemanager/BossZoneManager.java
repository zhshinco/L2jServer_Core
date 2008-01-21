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
package net.sf.l2j.gameserver.instancemanager;

import javolution.util.FastList;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.zone.type.L2BossZone;

/**
 * 
 * @author DaRkRaGe
 */
public class BossZoneManager
{
    // =========================================================
    private static BossZoneManager _instance;
    
    public static final BossZoneManager getInstance()
    {
        if (_instance == null)
        {
            System.out.println("Initializing BossZoneManager");
            _instance = new BossZoneManager();
        }
        return _instance;
    }
    
    private FastList<L2BossZone> _zones;
    
    public BossZoneManager()
    {
    }
    
    public void addZone(L2BossZone zone)
    {
        if (_zones == null)
        {
            _zones = new FastList<L2BossZone>();
        }
        _zones.add(zone);
    }
    
    public final L2BossZone getZone(L2Character character)
    {
        if (_zones != null)
            for (L2BossZone temp : _zones)
            {
                if (temp.isCharacterInZone(character))
                {
                    return temp;
                }
            }
        return null;
    }
    
    public final L2BossZone getZone(int x, int y, int z)
    {
        if (_zones != null)
            for (L2BossZone temp : _zones)
            {
                if (temp.isInsideZone(x, y, z))
                {
                    return temp;
                }
            }
        return null;
    }
    
    public boolean checkIfInZone(String zoneType, L2Object obj)
    {
        L2BossZone temp = getZone(obj.getX(), obj.getY(), obj.getZ());
        if (temp == null)
        {
            return false;
        }
        return temp.getZoneName().equalsIgnoreCase(zoneType);
    }
}