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
package net.sf.l2j.gameserver.ai;

import static net.sf.l2j.gameserver.ai.CtrlIntention.AI_INTENTION_ATTACK;
import static net.sf.l2j.gameserver.ai.CtrlIntention.AI_INTENTION_CAST;
import static net.sf.l2j.gameserver.ai.CtrlIntention.AI_INTENTION_IDLE;
import static net.sf.l2j.gameserver.ai.CtrlIntention.AI_INTENTION_INTERACT;
import static net.sf.l2j.gameserver.ai.CtrlIntention.AI_INTENTION_PICK_UP;
import static net.sf.l2j.gameserver.ai.CtrlIntention.AI_INTENTION_REST;

import java.util.EmptyStackException;
import java.util.Stack;

import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Character.AIAccessor;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2StaticObjectInstance;
import net.sf.l2j.gameserver.model.actor.knownlist.ObjectKnownList.KnownListAsynchronousUpdateTask;

public class L2PlayerAI extends L2CharacterAI
{

    private boolean thinking; // to prevent recursive thinking

    class IntentionCommand
    {
        protected CtrlIntention intention;
        protected Object arg0, arg1;

        protected IntentionCommand(CtrlIntention pIntention, Object pArg0, Object pArg1)
        {
            intention = pIntention;
            arg0 = pArg0;
            arg1 = pArg1;
        }
    }

    private Stack<IntentionCommand> _interuptedIntentions = new Stack<IntentionCommand>();

    public L2PlayerAI(AIAccessor accessor)
    {
        super(accessor);
    }

    /**
     * Saves the current Intention for this L2PlayerAI if necessary and calls changeIntention in AbstractAI.<BR><BR>
     *
     * @param intention The new Intention to set to the AI
     * @param arg0 The first parameter of the Intention
     * @param arg1 The second parameter of the Intention
     *
     */
    synchronized void changeIntention(CtrlIntention intention, Object arg0, Object arg1)
    {
        /*
         if (Config.DEBUG)
         _log.warning("L2PlayerAI: changeIntention -> " + intention + " " + arg0 + " " + arg1);
         */

        // nothing to do if it does not CAST intention
        if (intention != AI_INTENTION_CAST)
        {
            super.changeIntention(intention, arg0, arg1);
            return;
        }

        // do nothing if next intention is same as current one.
        if (intention == _intention && arg0 == _intention_arg0 && arg1 == _intention_arg1)
        {
            super.changeIntention(intention, arg0, arg1);
            return;
        }

        /*
         if (Config.DEBUG)
         _log.warning("L2PlayerAI: changeIntention -> Saving current intention: " + _intention + " " + _intention_arg0 + " " + _intention_arg1);
         */

        // push current intention to stack
        _interuptedIntentions.push(new IntentionCommand(_intention, _intention_arg0, _intention_arg1));
        super.changeIntention(intention, arg0, arg1);
    }

    /**
     * Finalize the casting of a skill. This method overrides L2CharacterAI method.<BR><BR>
     *
     * <B>What it does:</B>
     * Check if actual intention is set to CAST and, if so, retrieves latest intention
     * before the actual CAST and set it as the current intention for the player
     */
    protected void onEvtFinishCasting()
    {
        // forget interupted actions after offensive skill
        if (_skill != null && _skill.isOffensive()) _interuptedIntentions.clear();

        if (getIntention() == AI_INTENTION_CAST)
        {
            // run interupted intention if it remain.
            if (!_interuptedIntentions.isEmpty())
            {
                IntentionCommand cmd = null;
                try
                {
                    cmd = _interuptedIntentions.pop();
                }
                catch (EmptyStackException ese)
                {
                }

                /*
                 if (Config.DEBUG)
                 _log.warning("L2PlayerAI: onEvtFinishCasting -> " + cmd._intention + " " + cmd._arg0 + " " + cmd._arg1);
                 */

                if (cmd != null && cmd.intention != AI_INTENTION_CAST) // previous state shouldn't be casting 
                {
                	setIntention(cmd.intention, cmd.arg0, cmd.arg1);
                }
                else setIntention(AI_INTENTION_IDLE);
            }
            else
            {
                /*
                 if (Config.DEBUG)
                 _log.warning("L2PlayerAI: no previous intention set... Setting it to IDLE");
                 */
                // set intention to idle if skill doesn't change intention.
                setIntention(AI_INTENTION_IDLE);
            }
        }
    }

    protected void onIntentionRest()
    {
        if (getIntention() != AI_INTENTION_REST)
        {
            changeIntention(AI_INTENTION_REST, null, null);
            setTarget(null);
            if (getAttackTarget() != null)
            {
                setAttackTarget(null);
            }
            clientStopMoving(null);
            clientSitDown();
        }
    }

    protected void onIntentionActive()
    {
        setIntention(AI_INTENTION_IDLE);
    }

    protected void clientNotifyDead()
    {
        _client_moving_to_pawn_offset = 0;
        _client_moving = false;

        super.clientNotifyDead();
    }

    private void thinkAttack()
    {
    	L2Character target = getAttackTarget();
    	if (target == null) return;
        if (checkTargetLostOrDead(target))
        {
            if (target != null)
            {
                // Notify the target
                setAttackTarget(null);
            }
            return;
        }
        if (maybeMoveToPawn(target, _actor.getPhysicalAttackRange())) return;

        _accessor.doAttack(target);
        return;
    }

    private void thinkCast()
    {

    	L2Character target = getCastTarget();
        //if (Config.DEBUG) _log.warning("L2PlayerAI: thinkCast -> Start");

        if (checkTargetLost(target))
        {
            if (_skill.isOffensive() && getAttackTarget() != null)
            {
                //Notify the target
                setCastTarget(null);
            }
            return;
        }

        if (target != null) 
        	if (maybeMoveToPawn(target, _actor.getMagicalAttackRange(_skill))) return;
        
        if (_skill.getSkillTime() > 50) clientStopMoving(null);

        L2Object oldTarget = _actor.getTarget();
        if (oldTarget != null)
        {
            // Replace the current target by the cast target
            if (target != null && oldTarget != target) _actor.setTarget(getCastTarget());

            // Launch the Cast of the skill
            _accessor.doCast(_skill);

            // Restore the initial target
            if (target != null && oldTarget != target) _actor.setTarget(oldTarget);
        }
        else _accessor.doCast(_skill);

        return;
    }

    private void thinkPickUp()
    {
        if (_actor.isAllSkillsDisabled()) return;
    	L2Object target = getTarget();
        if (checkTargetLost(target)) return;
        if (maybeMoveToPawn(target, 36)) return;
        setIntention(AI_INTENTION_IDLE);
        ((L2PcInstance.AIAccessor) _accessor).doPickupItem(target);
        return;
    }

    private void thinkInteract()
    {
        if (_actor.isAllSkillsDisabled()) return;
        L2Object target = getTarget();
        if (checkTargetLost(target)) return;
        if (maybeMoveToPawn(target, 36)) return;
        if (!(target instanceof L2StaticObjectInstance)) ((L2PcInstance.AIAccessor) _accessor).doInteract((L2Character) target);
        setIntention(AI_INTENTION_IDLE);
        return;
    }

    protected void onEvtThink()
    {
        if (thinking || _actor.isAllSkillsDisabled()) return;

        /*
         if (Config.DEBUG)
         _log.warning("L2PlayerAI: onEvtThink -> Check intention");
         */

        thinking = true;
        try
        {
            if (getIntention() == AI_INTENTION_ATTACK) thinkAttack();
            else if (getIntention() == AI_INTENTION_CAST) thinkCast();
            else if (getIntention() == AI_INTENTION_PICK_UP) thinkPickUp();
            else if (getIntention() == AI_INTENTION_INTERACT) thinkInteract();
        }
        finally
        {
            thinking = false;
        }
    }

    protected void onEvtArrivedRevalidate()
    {
        ThreadPoolManager.getInstance().executeTask(new KnownListAsynchronousUpdateTask(_actor));
        super.onEvtArrivedRevalidate();
    }

}
