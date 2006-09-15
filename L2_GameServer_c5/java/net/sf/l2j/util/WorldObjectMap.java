/*
 * $Header: WorldObjectMap.java, 22/07/2005 14:15:11 luisantonioa Exp $
 *
 * $Author: luisantonioa $
 * $Date: 22/07/2005 14:15:11 $
 * $Revision: 1 $
 * $Log: WorldObjectMap.java,v $
 * Revision 1  22/07/2005 14:15:11  luisantonioa
 * Added copyright notice
 *
 * 
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
package net.sf.l2j.util;

import java.util.Iterator;
import java.util.Map;

import javolution.util.FastMap;
import net.sf.l2j.gameserver.model.L2Object;

/**
 * This class ...
 * 
 * @version $Revision: 1.2 $ $Date: 2004/06/27 08:12:59 $
 */

public class WorldObjectMap<T extends L2Object> extends L2ObjectMap<T>
{
    Map<Integer, T> objectMap    = new FastMap<Integer, T>().setShared(true);
    
    /* (non-Javadoc)
     * @see net.sf.l2j.util.L2ObjectMap#size()
     */
    public int size()
    {
        return objectMap.size();
    }

    /* (non-Javadoc)
     * @see net.sf.l2j.util.L2ObjectMap#isEmpty()
     */
    public boolean isEmpty()
    {
        return objectMap.isEmpty();
    }

    /* (non-Javadoc)
     * @see net.sf.l2j.util.L2ObjectMap#clear()
     */
    public void clear()
    {
        objectMap.clear();
    }

    /* (non-Javadoc)
     * @see net.sf.l2j.util.L2ObjectMap#put(T)
     */
    public void put(T obj)
    {
        if (obj != null)
            objectMap.put(obj.getObjectId(), obj);
    }

    /* (non-Javadoc)
     * @see net.sf.l2j.util.L2ObjectMap#remove(T)
     */
    public void remove(T obj)
    {
        if (obj != null)
            objectMap.remove(obj.getObjectId());
    }

    /* (non-Javadoc)
     * @see net.sf.l2j.util.L2ObjectMap#get(int)
     */
    public T get(int id)
    {
        return objectMap.get(id);
    }

    /* (non-Javadoc)
     * @see net.sf.l2j.util.L2ObjectMap#contains(T)
     */
    public boolean contains(T obj)
    {
        if (obj == null)
            return false;
        return objectMap.get(obj.getObjectId()) != null;
    }

    /* (non-Javadoc)
     * @see net.sf.l2j.util.L2ObjectMap#iterator()
     */
    public Iterator<T> iterator()
    {
        return objectMap.values().iterator();
    }

}
