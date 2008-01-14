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

package net.sf.l2j.gameserver.model;

import java.util.concurrent.Future;
import java.util.logging.Logger;

import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.skills.effects.EffectForce;

/**
 * @author kombat
 *
 */
public class ForceBuff
{
	static final Logger _log = Logger.getLogger(ForceBuff.class.getName());

	protected L2PcInstance _caster;
	private L2PcInstance _target;
	private L2Skill _skill;
	private L2Skill _force;
	private Future<?> _task;
	private boolean _applied;

	public L2PcInstance getCaster() { return _caster; }
	public L2PcInstance getTarget() { return _target; }
	public L2Skill getSkill() { return _skill; }
	public L2Skill getForce() { return _force; }
	protected void setTask(Future<?> task) { _task = task; }
	protected void setApplied(boolean applied) { _applied = applied; }

	public ForceBuff(L2PcInstance caster, L2PcInstance target, L2Skill skill)
	{
		_caster = caster;
		_target = target;
		_skill = skill;
		_force = SkillTable.getInstance().getInfo(skill.getForceId(), 1);
		_applied = false;

		Runnable r = new Runnable()
		{
			public void run()
			{
				setApplied(true);
				setTask(null);

				int forceId = getForce().getId();
				boolean create = true;
				L2Effect[] effects = getTarget().getAllEffects();
				if (effects != null)
				{
					for(L2Effect e : effects)
					{
						if (e.getSkill().getId() == forceId)
						{
							EffectForce ef = (EffectForce)e;
							if(ef.forces < 3)
								ef.increaseForce();
							create = false;
							break;
						}
					}
				}
				if(create)
				{
					getForce().getEffects(_caster, getTarget());
				}
			}
		};
		setTask(ThreadPoolManager.getInstance().scheduleGeneral(r, 2000));
	}

	public void delete()
	{
		if (_task != null)
		{
			_task.cancel(false);
			_task = null;
		}

		_caster.setForceBuff(null);

		if(!_applied) return;

		int toDeleteId = getForce().getId();

		L2Effect[] effects = _target.getAllEffects();
		if (effects != null)
		{
			for(L2Effect e : effects)
			{
				if (e.getSkill().getId() == toDeleteId && (e instanceof EffectForce))
				{
					((EffectForce)e).decreaseForce();
					break;
				}
			}
		}
	}
}
