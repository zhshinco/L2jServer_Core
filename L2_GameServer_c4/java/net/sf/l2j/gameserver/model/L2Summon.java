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
package net.sf.l2j.gameserver.model;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.ai.L2CharacterAI;
import net.sf.l2j.gameserver.ai.L2SummonAI;
import net.sf.l2j.gameserver.model.L2Skill.SkillTargetType;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PlayableInstance;
import net.sf.l2j.gameserver.model.actor.knownlist.SummonKnownList;
import net.sf.l2j.gameserver.model.actor.stat.SummonStat;
import net.sf.l2j.gameserver.model.actor.status.SummonStatus;
import net.sf.l2j.gameserver.model.base.Experience;
import net.sf.l2j.gameserver.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.serverpackets.MyTargetSelected;
import net.sf.l2j.gameserver.serverpackets.NpcInfo;
import net.sf.l2j.gameserver.serverpackets.PetDelete;
import net.sf.l2j.gameserver.serverpackets.PetStatusShow;
import net.sf.l2j.gameserver.serverpackets.PetStatusUpdate;
import net.sf.l2j.gameserver.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.taskmanager.DecayTaskManager;
import net.sf.l2j.gameserver.templates.L2NpcTemplate;
import net.sf.l2j.gameserver.templates.L2Weapon;

public abstract class L2Summon extends L2PlayableInstance
{
    //private static Logger _log = Logger.getLogger(L2Summon.class.getName());
    
	protected int _pkKills;
    private byte _pvpFlag;
    private L2PcInstance _owner;
    private int _karma = 0;
    private int _attackRange = 36; //Melee range
    private boolean _follow = true;
    private boolean _previousFollowStatus = true;
    private int _maxLoad;

    private boolean _showSummonAnimation;
    
    private int _chargedSoulShot;
    private int _chargedSpiritShot;
    private int _usedSoulShots = 0;
    private int _usedSpiritShots = 0;
    
	public class AIAccessor extends L2Character.AIAccessor
	{
		protected AIAccessor() {}
		public L2Summon getSummon() { return L2Summon.this; }
		public boolean isAutoFollow() {
			return L2Summon.this.getFollowStatus();
		}
		public void doPickupItem(L2Object object) {
			L2Summon.this.doPickupItem(object);
		}
	}

	public L2Summon(int objectId, L2NpcTemplate template, L2PcInstance owner)
	{
		super(objectId, template);
        super.setKnownList(new SummonKnownList(new L2Summon[] {this}));
        super.setStat(new SummonStat(new L2Summon[] {this}));
        super.setStatus(new SummonStatus(new L2Summon[] {this}));
        
        _showSummonAnimation = true;
		_owner = owner;
		_ai = new L2SummonAI(new L2Summon.AIAccessor());
        
		setXYZInvisible(owner.getX()+50, owner.getY()+100, owner.getZ()+100);
	}

    public final SummonKnownList getKnownList() { return (SummonKnownList)super.getKnownList(); }
    public SummonStat getStat() { return (SummonStat)super.getStat(); }
    public SummonStatus getStatus() { return (SummonStatus)super.getStatus(); }
	
	public L2CharacterAI getAI() 
    {
		if (_ai == null)
		{
			synchronized(this)
			{
				if (_ai == null)
					_ai = new L2SummonAI(new L2Summon.AIAccessor());
			}
		}
        
		return _ai;
	}
	
	public L2NpcTemplate getTemplate()
	{
		return (L2NpcTemplate)super.getTemplate();
	}

	// this defines the action buttons, 1 for Summon, 2 for Pets
    public abstract int getSummonType();

	public void updateAbnormalEffect()
    {
		for (L2PcInstance player : getKnownList().getKnownPlayers())
			player.sendPacket(new NpcInfo(this, player));
    }
    
    /**
     * @return Returns the mountable.
     */
    public boolean isMountable()
    {
        return false;
    }

	public void onAction(L2PcInstance player)
    {
        if (player == _owner && player.getTarget() == this)
        {
            player.sendPacket(new PetStatusShow(this));
            player.sendPacket(new ActionFailed());
        }
        else
        {
            if (Config.DEBUG) _log.fine("new target selected:"+getObjectId());
            player.setTarget(this);
            MyTargetSelected my = new MyTargetSelected(getObjectId(), player.getLevel() - getLevel());
            player.sendPacket(my);
        }
    }
    
	public int getExpForThisLevel()
    {
        if(getLevel() >= Experience.LEVEL.length)
        {
            return 0;
        }
		return Experience.LEVEL[getLevel()];
    }
	
    public int getExpForNextLevel()
    {
        if(getLevel() >= Experience.LEVEL.length - 1)
        {
            return 0;
        }
        return Experience.LEVEL[getLevel()+1];
    }
    
	public final int getKarma()
    {
        return _karma;
    }
    
    public void setKarma(int karma)
    {
        _karma = karma;
    }
    
    public final L2PcInstance getOwner()
    {
        return _owner;
    }
    
    public final int getNpcId()
    {
        return getTemplate().npcId;
    }
    
    public void setPvpFlag(byte pvpFlag)
    {
        _pvpFlag = pvpFlag;
    }
    
    public byte getPvpFlag()
    {
        return _pvpFlag;
    }
    
    public void setPkKills(int pkKills)
    {
        _pkKills = pkKills;
    }
    
    public final int getPkKills()
    {
        return _pkKills;
    }
    
    public final int getMaxLoad()
    {
        return _maxLoad;
    }
    
    public final int getUsedSoulShots()
    {
        return _usedSoulShots;
    }
    
    public final int getUsedSpiritShots()
    {
        return _usedSpiritShots;
    }
    
    public final void increaseUsedSoulShots(int numShots)
    {
        _usedSoulShots += numShots;
    }
    
    public final void increaseUsedSpiritShots(int numShots)
    {
        _usedSpiritShots += numShots;
    }
    
    public void setMaxLoad(int maxLoad)
    {
        _maxLoad = maxLoad;
    }
    
    public void setChargedSoulShot(int shotType)
    {
        _chargedSoulShot = shotType;
    }
    
    public void setChargedSpiritShot(int shotType)
    {
        _chargedSpiritShot = shotType;
    }
    
    public void followOwner()
    {
		setFollowStatus(true);
    }
	
	public synchronized void doDie(L2Character killer)
    {
        DecayTaskManager.getInstance().addDecayTask(this);
		super.doDie(killer);
	}

	public synchronized void doDie(L2Character killer, boolean decayed)
    {
		if(!decayed)
		{
			DecayTaskManager.getInstance().addDecayTask(this);
		}

		super.doDie(killer);
	}
	
    public void stopDecay()
    {
        DecayTaskManager.getInstance().cancelDecayTask(this);
    }
    
    public void onDecay()
    {
        deleteMe(_owner);
    }    
    
    public void broadcastStatusUpdate()
    {
        super.broadcastStatusUpdate();

        if (getOwner() != null && isVisible())
            getOwner().sendPacket(new PetStatusUpdate(this));
    }
    
    public void deleteMe(L2PcInstance owner)
    {   
        getAI().stopFollow();
        owner.sendPacket(new PetDelete(getObjectId(), 2));
        
        //FIXME: I think it should really drop items to ground and only owner can take for a while
        giveAllToOwner(); 
        decayMe();
        getKnownList().removeAllKnownObjects();
        owner.setPet(null); 
    }

    public synchronized void unSummon(L2PcInstance owner)
    {
		if (isVisible() && !isDead())
	    {
			getAI().stopFollow();
	        owner.sendPacket(new PetDelete(getObjectId(), 2));
	        store();
            
	        giveAllToOwner();
		    decayMe();
            getKnownList().removeAllKnownObjects();
	        owner.setPet(null);
	        setTarget(null);
	    }
    }
    
    public int getAttackRange()
    {
        return _attackRange; 
    }
    
    public void setAttackRange(int range)
    {
        if (range < 36)
            range = 36;
        _attackRange = range;
    }
    
    public void setFollowStatus(boolean state)
    {
        _follow = state;
		if (_follow)
			getAI().setIntention(CtrlIntention.AI_INTENTION_FOLLOW, getOwner());
		else
			getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE, null);
    }
    
    public boolean getFollowStatus()
    {
        return _follow;
    }
    

    public boolean isAutoAttackable(L2Character attacker)
    {
        return _owner.isAutoAttackable(attacker);
    }
    /**
     * @return Returns the showSummonAnimation.
     */
    public boolean isShowSummonAnimation()
    {
        return _showSummonAnimation;
    }
    
    /**
     * @param showSummonAnimation The showSummonAnimation to set.
     */
    public void setShowSummonAnimation(boolean showSummonAnimation)
    {
        _showSummonAnimation = showSummonAnimation;
    }
    
    public int getChargedSoulShot()
    {
        return _chargedSoulShot;
    }
    
    public int getChargedSpiritShot()
    {
        return _chargedSpiritShot;
    }
    
    public int getControlItemId()
    {
        return 0;
    }
    
    public L2Weapon getActiveWeapon()
    {
        return null;
    }
    
    public PetInventory getInventory()
    {
        return null;
    }
    
	protected void doPickupItem(L2Object object)
    {
        return;
    }
    
    public void giveAllToOwner()
    {
        return;
    }
    
    public void store()
    {
        return;
    }
	
	public L2ItemInstance getActiveWeaponInstance() 
	{
		return null;
	}
	
	public L2Weapon getActiveWeaponItem() 
    {
		return null;
	}
    
	public L2ItemInstance getSecondaryWeaponInstance() 
    {
		return null;
	}
    
	public L2Weapon getSecondaryWeaponItem() 
    {
		return null;
	}
	
	/**
	 * Return the L2Party object of its L2PcInstance owner or null.<BR><BR>
	 */
	public L2Party getParty()
	{
		if (_owner == null) 
			return null;
		else
			return _owner.getParty();
	}
    
	/**
	 * Return True if the L2Character has a Party in progress.<BR><BR>
	 */
    public boolean isInParty()
	{
    	if (_owner == null)
    		return false;
    	else
    		return _owner.getParty() != null;
	}

	/**
	 * Check if the active L2Skill can be casted.<BR><BR>
	 *
	 * <B><U> Actions</U> :</B><BR><BR>
     * <li>Check if the target is correct </li>
	 * <li>Check if the target is in the skill cast range </li>
	 * <li>Check if the summon owns enough HP and MP to cast the skill </li>
	 * <li>Check if all skills are enabled and this skill is enabled </li><BR><BR>
	 * <li>Check if the skill is active </li><BR><BR>
	 * <li>Notify the AI with AI_INTENTION_CAST and target</li><BR><BR>
	 *
	 * @param skill The L2Skill to use
	 * @param forceUse used to force ATTACK on players
	 * @param dontMove used to prevent movement, if not in range
	 * 
	 */
	public void useMagic(L2Skill skill, boolean forceUse, boolean dontMove)
	{
		if (skill == null || isDead())
			return;
		
        // Check if the skill is active
		if (skill.isPassive())
		{
	        // just ignore the passive skill request. why does the client send it anyway ??
			return;
		}
		
		//************************************* Check Casting in Progress *******************************************
        
        // If a skill is currently being used
        if (isCastingNow())
		{
            return;
		}
            
        //************************************* Check Target *******************************************
        
		// Get the target for the skill
		L2Object target = null;
		
		switch (skill.getTargetType())
		{
			// OWNER_PET should be cast even if no target has been found
			case TARGET_OWNER_PET:
				target = this.getOwner();
				break;
			// PARTY, AURA, SELF should be cast even if no target has been found
			case TARGET_PARTY:
			case TARGET_AURA:
			case TARGET_SELF:
				target = this;
				break;
			default:
                // Get the first target of the list
			    target = skill.getFirstOfTargetList(this);
			    break;
		}

        // Check the validity of the target
        if (target == null)
        {
        	if (getOwner() != null)
        		getOwner().sendPacket(new SystemMessage(SystemMessage.TARGET_CANT_FOUND));
            return;
        }
        
        //************************************* Check skill availability *******************************************
        
        // Check if this skill is enabled (ex : reuse time)
        if (isSkillDisabled(skill.getId()) 
        		&& getOwner() != null
        		&& (getOwner().getAccessLevel() < Config.GM_PEACEATTACK))
        {
            SystemMessage sm = new SystemMessage(SystemMessage.SKILL_NOT_AVAILABLE);
            sm.addString(skill.getName());
            getOwner().sendPacket(sm);
            return;
        }
        
        // Check if all skills are disabled
        if (isAllSkillsDisabled()
        		&& getOwner() != null
        		&& (getOwner().getAccessLevel() < Config.GM_PEACEATTACK))
        {
            return;
        }

        //************************************* Check Consumables *******************************************
        
        // Check if the summon has enough MP
        if (getCurrentMp() < skill.getMpConsume())
        {
            // Send a System Message to the caster
        	if (getOwner() != null)
        		getOwner().sendPacket(new SystemMessage(SystemMessage.NOT_ENOUGH_MP));
            return;
        }
        
        // Check if the summon has enough HP
        if (getCurrentHp() <= skill.getHpConsume())
        {
            // Send a System Message to the caster
        	if (getOwner() != null)
        		getOwner().sendPacket(new SystemMessage(SystemMessage.NOT_ENOUGH_HP));
            return;
        }

        //************************************* Check Summon State *******************************************
        
        // Check if this is offensive magic skill
        if (skill.isOffensive())  
		{
			if (isInsidePeaceZone(this, target)
					&& getOwner() != null
					&& (getOwner().getAccessLevel() < Config.GM_PEACEATTACK))
			{
				// If summon or target is in a peace zone, send a system message TARGET_IN_PEACEZONE
	        	sendPacket(new SystemMessage(SystemMessage.TARGET_IN_PEACEZONE));
				return;
			}

            // Check if the target is attackable
            if (!target.isAttackable() 
            		&& getOwner() != null 
            		&& (getOwner().getAccessLevel() < Config.GM_PEACEATTACK))
			{
				return;
			}

			// Check if a Forced ATTACK is in progress on non-attackable target
			if (!target.isAutoAttackable(this) && !forceUse &&
					skill.getTargetType() != SkillTargetType.TARGET_AURA &&
					skill.getTargetType() != SkillTargetType.TARGET_CLAN &&
					skill.getTargetType() != SkillTargetType.TARGET_ALLY &&
					skill.getTargetType() != SkillTargetType.TARGET_PARTY &&
					skill.getTargetType() != SkillTargetType.TARGET_SELF)
			{
				return;
			}
		}

		// Notify the AI with AI_INTENTION_CAST and target
		getAI().setIntention(CtrlIntention.AI_INTENTION_CAST, skill, target);
	}
	
	public void setIsImobilised(boolean value)
	{
		super.setIsImobilised(value);

		if (value)
		{
			_previousFollowStatus = getFollowStatus();
			// if imobilized temporarly disable follow mode
			if (_previousFollowStatus)
				setFollowStatus(false);
		}
		else
		{
			// if not more imobilized restore previous follow mode
			setFollowStatus(_previousFollowStatus);
		}
	}
	
	public void setOwner(L2PcInstance newOwner)
	{
		_owner = newOwner;
	}
}
