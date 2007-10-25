/*
+ * This program is free software; you can redistribute it and/or modify
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
package net.sf.l2j.gameserver.skills.conditions;

import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.skills.Env;
import net.sf.l2j.gameserver.skills.effects.EffectBattleForce;
import net.sf.l2j.gameserver.skills.effects.EffectSpellForce;


/**
 * @author kombat
 *
 */
public class ConditionForceBuff extends Condition
{
	private static int BATTLE_FORCE = 426;
	private static int SPELL_FORCE = 427;

	private int _battleForces;
	private int _spellForces;

	public ConditionForceBuff(int[] forces)
	{
		_battleForces = forces[0];
		_spellForces = forces[1];
	}

	public ConditionForceBuff(int battle, int spell)
	{
		_battleForces = battle;
		_spellForces = spell;
	}

	@Override
	public boolean testImpl(Env env)
	{
		int neededBattle = _battleForces;
		if (neededBattle > 0)
		{
			L2Effect battleForce = env.player.getEffect(BATTLE_FORCE);
			if (!(battleForce instanceof EffectBattleForce) || 
			  ((EffectBattleForce)battleForce).forces < neededBattle)
				return false;
		}
		int neededSpell = _spellForces;
		if (neededSpell > 0)
		{
			L2Effect spellForce = env.player.getEffect(SPELL_FORCE);
			if (!(spellForce instanceof EffectSpellForce) || 
			  ((EffectSpellForce)spellForce).forces < neededSpell)
				return false;
		}
		return true;
	}
}
