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
package net.sf.l2j.gameserver.skills.effects;

import java.util.logging.Logger;

import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.skills.Env;

/**
 * @author Ahmed
 * 
 */
public class EffectImmobileUntilAttacked extends L2Effect
{
    static final Logger _log = Logger.getLogger(EffectImmobileUntilAttacked.class.getName());
    
    public EffectImmobileUntilAttacked(Env env, EffectTemplate template)
    {
        super(env, template);
    }
    
    @Override
    public EffectType getEffectType()
    {
        return EffectType.IMMOBILEUNTILATTACKED;
    }
    
    /** Notify started */
    @Override
    public void onStart()
    {
    	getEffected().startImmobileUntilAttacked();
    }
    
    /** Notify exited */
    @Override
    public void onExit()
    {
    	getEffected().stopImmobileUntilAttacked(this);
    }
    
    @Override
    public boolean onActionTime()
    {
    	getEffected().stopImmobileUntilAttacked(this);
        // just stop this effect
        return false;
    }
}
