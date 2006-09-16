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
package net.sf.l2j.gameserver.model.actor.instance;

import java.util.Random;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.ai.L2CharacterAI;
import net.sf.l2j.gameserver.ai.L2SiegeGuardAI;
import net.sf.l2j.gameserver.model.L2Attackable;
import net.sf.l2j.gameserver.model.L2CharPosition;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.actor.knownlist.SiegeGuardKnownList;
import net.sf.l2j.gameserver.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.serverpackets.MyTargetSelected;
import net.sf.l2j.gameserver.serverpackets.NpcInfo;
import net.sf.l2j.gameserver.serverpackets.SocialAction;
import net.sf.l2j.gameserver.serverpackets.StatusUpdate;
import net.sf.l2j.gameserver.serverpackets.ValidateLocation;
import net.sf.l2j.gameserver.templates.L2NpcTemplate;

/**
 * This class represents all guards in the world. It inherits all methods from
 * L2Attackable and adds some more such as tracking PK's or custom interactions.
 * 
 * @version $Revision: 1.11.2.1.2.7 $ $Date: 2005/04/06 16:13:40 $
 */
public final class L2SiegeGuardInstance extends L2Attackable
{
    //private static Logger _log = Logger.getLogger(L2GuardInstance.class.getName());
	private final Random _rnd = new Random();

    private int _homeX;
    private int _homeY;
    private int _homeZ;
    
    public L2SiegeGuardInstance(int objectId, L2NpcTemplate template)
    {
        super(objectId, template);
        super.setKnownList(new SiegeGuardKnownList(new L2SiegeGuardInstance[] {this}));
    }

    public final SiegeGuardKnownList getKnownList() { return (SiegeGuardKnownList)super.getKnownList(); }

	public L2CharacterAI getAI() 
	{
		synchronized(this)
		{
			if (_ai == null)
				_ai = new L2SiegeGuardAI(new AIAccessor());
		}
		return _ai;
	}    
    
	/**
	 * Return True if a siege is in progress and the L2Character attacker isn't a Defender.<BR><BR>
	 * 
	 * @param attacker The L2Character that the L2SiegeGuardInstance try to attack
	 * 
	 */
    public boolean isAutoAttackable(L2Character attacker) 
	{
 // Attackable during siege by all except defenders
		return (attacker != null 
		        && attacker instanceof L2PcInstance 
		        && getCastle() != null
		        && getCastle().getCastleId() > 0
		        && getCastle().getSiege().getIsInProgress()
         && !getCastle().getSiege().checkIsDefender(((L2PcInstance)attacker).getClan()));
    }
    
    /**
     * Sets home location of guard. Guard will always try to return to this location after
     * it has killed all PK's in range.
     *
     */
    public void getHomeLocation()
    {
        _homeX = getX();
        _homeY = getY();
        _homeZ = getZ();
        
        if (Config.DEBUG)
            _log.finer(getObjectId()+": Home location set to"+
                    " X:" + _homeX + " Y:" + _homeY + " Z:" + _homeZ);
    }
    
    public int getHomeX()
    {
    	return _homeX;
    }
    
    public int getHomeY()
    {
    	return _homeY;
    }
    
    /**
     * This method forces guard to return to home location previously set
     *
     */
    public void returnHome()
    {
        if (!isInsideRadius(_homeX, _homeY, 40, false))
        {
            if (Config.DEBUG) _log.fine(getObjectId()+": moving home");
            
            clearAggroList();
            
            if (hasAI())
                getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new L2CharPosition(_homeX, _homeY, _homeZ, 0));
        }
    }
    
    /**
     * Custom onAction behaviour. Note that super() is not called because guards need
     * extra check to see if a player should interract ot ATTACK them when clicked.
     * 
     */
    public void onAction(L2PcInstance player)
    {
      //  if (player == null)
      //      return;
        
		if (this != player.getTarget())
		{
			if (Config.DEBUG) _log.fine("new target selected:"+getObjectId());
			//player.setNewTarget(this);
			
			NpcInfo npcInfo = new NpcInfo(this, player);
			player.sendPacket(npcInfo);
			
			player.setTarget(this);
			MyTargetSelected my = new MyTargetSelected(getObjectId(), player.getLevel() - getLevel());
			player.sendPacket(my);
			
			//if ((isAutoAttackable(player))||(this instanceof L2SiegeGuardInstance))
			//{	
				StatusUpdate su = new StatusUpdate(getObjectId());
				su.addAttribute(StatusUpdate.CUR_HP, (int)getCurrentHp() );
				su.addAttribute(StatusUpdate.MAX_HP, getMaxHp() );
				player.sendPacket(su);
			//}
			
			// correct location
			player.sendPacket(new ValidateLocation(this));
		}
		else
		{
			MyTargetSelected my = new MyTargetSelected(getObjectId(), player.getLevel() - getLevel());
			player.sendPacket(my);
			
			if (isAutoAttackable(player) && !isAlikeDead())
			{
				if (Math.abs(player.getZ() - getZ()) < 600) // this max heigth difference might need some tweaking
				{
					player.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, this);
//					player.startAttack(this);
				}
				else
				{
					player.sendPacket(new ActionFailed());
				}
			}
			if(!isAutoAttackable(player))
			{
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);
                if (!isInsideRadius(player, INTERACTION_DISTANCE, false, false))
                {
//					player.setCurrentState(L2Character.STATE_INTERACT);
//					player.setInteractTarget(this);
//					player.moveTo(this.getX(), this.getY(), this.getZ(), INTERACTION_DISTANCE);
				} else 
                {
					SocialAction sa = new SocialAction(getObjectId(), _rnd.nextInt(8));
					broadcastPacket(sa);
					sendPacket(sa);
					showChatWindow(player, 0);
					player.sendPacket(new ActionFailed());					
//					player.setCurrentState(L2Character.STATE_IDLE);
				}
			}
		}
		//super.onAction(player);
    }
    
    public void addDamageHate(L2Character attacker, int damage, int aggro)
    {
        if (attacker == null)
            return;
        
        if (!(attacker instanceof L2SiegeGuardInstance))
        {
            super.addDamageHate(attacker, damage, aggro);
        }
    }
}
