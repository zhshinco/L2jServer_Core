package net.sf.l2j.gameserver.model.actor.instance;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.ai.L2CharacterAI;
import net.sf.l2j.gameserver.ai.L2ControllableMobAI;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.templates.L2NpcTemplate;

/**
 * @author littlecrow
 *
 */
public class L2ControllableMobInstance extends L2MonsterInstance
{
	private boolean _isInvul;
	private L2ControllableMobAI _aiBackup;	// to save ai, avoiding beeing detached
	private boolean _killedAlready = false;
	
	protected class ControllableAIAcessor extends AIAccessor 
    {
		public void detachAI() 
        {
			// do nothing, AI of controllable mobs can't be detached automatically
		}
	}
	
	
	public boolean isAggressive()
    {
		return true;
	}

	public int getAggroRange() 
    {
		// force mobs to be aggro
		return 500;
	}

	public L2ControllableMobInstance(int objectId, L2NpcTemplate template) 
    {
		super(objectId, template);
	}

	public L2CharacterAI getAI() 
    {
		if (_ai == null)
		{
			synchronized(this)
			{
				if (_ai == null && _aiBackup == null)
                {
					_ai = new L2ControllableMobAI(new ControllableAIAcessor());
					_aiBackup = (L2ControllableMobAI)_ai;
				}
				else
                {
					_ai = _aiBackup;
                }
			}
		}
		return _ai;
	}

	public boolean isInvul() 
    {
		return _isInvul;
	}

	public void setInvul(boolean isInvul) 
    {
		_isInvul = isInvul;
	}

	public void reduceCurrentHp(double i, L2Character attacker, boolean awake) 
    {
		if (isInvul() || isDead())
			return;
		
		if (awake)
			stopSleeping(null);
		
		i = getCurrentHp() - i;
		
		if (i < 0)
			i = 0;
		
		setCurrentHp(i);
		
		if (isDead())
		{
			// killing is only possible one time
			synchronized (this)
			{
				if (_killedAlready)
					return;
				
				_killedAlready = true;
			}
			
			// first die (and calculate rewards), if currentHp < 0,
			// then overhit may be calculated
			if (Config.DEBUG) _log.fine("char is dead.");
			
			stopMove(null);
			
			// Start the doDie process
			doDie(attacker);
			
			// now reset currentHp to zero
			setCurrentHp(0);
		}
	}

	public void doDie(L2Character killer) 
    {
		removeAI();
		super.doDie(killer);
	}

	public void deleteMe() 
    {
		removeAI();
		super.deleteMe();
	}
	
	/**
	 * Definitively remove AI
	 */
	protected void removeAI() 
    {
		synchronized (this) 
        {
			if (_aiBackup != null) 
            {
				_aiBackup.setIntention(CtrlIntention.AI_INTENTION_IDLE);
				_aiBackup = null;
				_ai = null;
			}
		}
	}
}