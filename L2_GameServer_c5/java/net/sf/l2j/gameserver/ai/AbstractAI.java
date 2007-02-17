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
import static net.sf.l2j.gameserver.ai.CtrlIntention.AI_INTENTION_FOLLOW;
import static net.sf.l2j.gameserver.ai.CtrlIntention.AI_INTENTION_IDLE;

import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.l2j.gameserver.GameTimeController;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.model.L2CharPosition;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2Effect.EffectType;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.serverpackets.AutoAttackStart;
import net.sf.l2j.gameserver.serverpackets.AutoAttackStop;
import net.sf.l2j.gameserver.serverpackets.CharMoveToLocation;
import net.sf.l2j.gameserver.serverpackets.Die;
import net.sf.l2j.gameserver.serverpackets.MoveToLocationInVehicle;
import net.sf.l2j.gameserver.serverpackets.MoveToPawn;
import net.sf.l2j.gameserver.serverpackets.ServerBasePacket;
import net.sf.l2j.gameserver.serverpackets.StopMove;
import net.sf.l2j.gameserver.serverpackets.StopRotation;
import net.sf.l2j.gameserver.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.taskmanager.AttackStanceTaskManager;

/**
 * Mother class of all objects AI in the world.<BR><BR>
 *
 * AbastractAI :<BR><BR>
 * <li>L2CharacterAI</li><BR><BR>
 *
 */
abstract class AbstractAI implements Ctrl
{

    protected static final Logger _log = Logger.getLogger(AbstractAI.class.getName());

    class FollowTask implements Runnable
    {
        int _range = 60;

        public FollowTask()
        {
        }

        public FollowTask(int range)
        {
            _range = range;
        }

        public void run()
        {
            try
            {
                if (_follow_task == null) return;

                if (_follow_target == null)
                {
                    stopFollow();
                    return;
                }

                moveToPawn(_follow_target, _range);
            }
            catch (Throwable t)
            {
                _log.log(Level.WARNING, "", t);
            }
        }
    }

    /** The character that this AI manages */
    final L2Character _actor;

    /** An accessor for private methods of the actor */
    final L2Character.AIAccessor _accessor;

    /** Current long-term intention */
    protected CtrlIntention _intention = AI_INTENTION_IDLE;
    /** Current long-term intention parameter */
    protected Object _intention_arg0 = null;
    /** Current long-term intention parameter */
    protected Object _intention_arg1 = null;

    /** Flags about client's state, in order to know which messages to send */
    protected boolean _client_moving;
    /** Flags about client's state, in order to know which messages to send */
    protected boolean _client_auto_attacking;
    /** Flags about client's state, in order to know which messages to send */
    protected int _client_moving_to_pawn_offset;

    /** Different targets this AI maintains */
    private L2Object _target;
    private L2Character _cast_target;
    protected L2Character _attack_target;
    protected L2Character _follow_target;

    /** The skill we are curently casting by INTENTION_CAST */
    L2Skill _skill;

    /** Diferent internal state flags */
    private int _move_to_pawn_timeout;

    protected Future _follow_task = null;
    private static final int FOLLOW_INTERVAL = 1000;
    private static final int ATTACK_FOLLOW_INTERVAL = 500;

    /**
     * Constructor of AbstractAI.<BR><BR>
     *
     * @param accessor The AI accessor of the L2Character
     *
     */
    protected AbstractAI(L2Character.AIAccessor accessor)
    {
        _accessor = accessor;

        // Get the L2Character managed by this Accessor AI
        _actor = accessor.getActor();
    }

    /**
     * Return the L2Character managed by this Accessor AI.<BR><BR>
     */
    public L2Character getActor()
    {
        return _actor;
    }

    /**
     * Return the current Intention.<BR><BR>
     */
    public CtrlIntention getIntention()
    {
        return _intention;
    }

    protected synchronized void setCastTarget(L2Character target)
    {
        _cast_target = target;
    }

    /**
     * Return the current cast target.<BR><BR>
     */
    public L2Character getCastTarget()
    {
        return _cast_target;
    }

    protected synchronized void setAttackTarget(L2Character target)
    {
        _attack_target = target;
    }

    /**
     * Return current attack target.<BR><BR>
     */
    public L2Character getAttackTarget()
    {
        return _attack_target;
    }

    /**
     * Set the Intention of this AbstractAI.<BR><BR>
     *
     * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method is USED by AI classes</B></FONT><BR><BR>
     *
     * <B><U> Overriden in </U> : </B><BR>
     * <B>L2AttackableAI</B> : Create an AI Task executed every 1s (if necessary)<BR>
     * <B>L2PlayerAI</B> : Stores the current AI intention parameters to later restore it if necessary<BR><BR>
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
         _log.warning("AbstractAI: changeIntention -> " + intention + " " + arg0 + " " + arg1);
         */

        _intention = intention;
        _intention_arg0 = arg0;
        _intention_arg1 = arg1;
    }

    /**
     * Launch the L2CharacterAI onIntention method corresponding to the new Intention.<BR><BR>
     *
     * <FONT COLOR=#FF0000><B> <U>Caution</U> : Stop the FOLLOW mode if necessary</B></FONT><BR><BR>
     *
     * @param intention The new Intention to set to the AI
     *
     */
    public final void setIntention(CtrlIntention intention)
    {
        setIntention(intention, null, null);
    }

    /**
     * Launch the L2CharacterAI onIntention method corresponding to the new Intention.<BR><BR>
     *
     * <FONT COLOR=#FF0000><B> <U>Caution</U> : Stop the FOLLOW mode if necessary</B></FONT><BR><BR>
     *
     * @param intention The new Intention to set to the AI
     * @param arg0 The first parameter of the Intention (optional target)
     *
     */
    public final void setIntention(CtrlIntention intention, Object arg0)
    {
        setIntention(intention, arg0, null);
    }

    /**
     * Launch the L2CharacterAI onIntention method corresponding to the new Intention.<BR><BR>
     *
     * <FONT COLOR=#FF0000><B> <U>Caution</U> : Stop the FOLLOW mode if necessary</B></FONT><BR><BR>
     *
     * @param intention The new Intention to set to the AI
     * @param arg0 The first parameter of the Intention (optional target)
     * @param arg1 The second parameter of the Intention (optional target)
     *
     */
    public final void setIntention(CtrlIntention intention, Object arg0, Object arg1)
    {
        if (!_actor.isVisible() || !_actor.hasAI()) return;

        /*
         if (Config.DEBUG)
         _log.warning("AbstractAI: setIntention -> " + intention + " " + arg0 + " " + arg1);
         */
        
        // Silent move stop if player attack or cast
        if (intention == CtrlIntention.AI_INTENTION_ATTACK 
            || (intention == CtrlIntention.AI_INTENTION_CAST 
                && arg0 instanceof L2Skill 
                && ((L2Skill)arg0).getId() != 221
               )
           )
        {
        	L2Effect silentMove = _actor.getEffect(EffectType.SILENT_MOVE);
            if (silentMove != null && silentMove.getSkill() != null)
            {
                SystemMessage sm = new SystemMessage(SystemMessage.EFFECT_S1_DISAPPEARED);
                sm.addSkillName(silentMove.getSkill().getId());
                silentMove.exit();
                _actor.sendPacket(sm);
                sm = null;
            }
            silentMove = null;
        }

        // Stop the follow mode if necessary
        if (intention != AI_INTENTION_FOLLOW && intention != AI_INTENTION_ATTACK) stopFollow();

        // Launch the onIntention method of the L2CharacterAI corresponding to the new Intention
        switch (intention)
        {
            case AI_INTENTION_IDLE:
                onIntentionIdle();
                break;
            case AI_INTENTION_ACTIVE:
                onIntentionActive();
                break;
            case AI_INTENTION_REST:
                onIntentionRest();
                break;
            case AI_INTENTION_ATTACK:
                onIntentionAttack((L2Character) arg0);
                break;
            case AI_INTENTION_CAST:
                onIntentionCast((L2Skill) arg0, (L2Object) arg1);
                break;
            case AI_INTENTION_MOVE_TO:
                onIntentionMoveTo((L2CharPosition) arg0);
                break;
            case AI_INTENTION_MOVE_TO_IN_A_BOAT:
                onIntentionMoveToInABoat((L2CharPosition) arg0, (L2CharPosition) arg1);
                break;
            case AI_INTENTION_FOLLOW:
                onIntentionFollow((L2Character) arg0);
                break;
            case AI_INTENTION_PICK_UP:
                onIntentionPickUp((L2Object) arg0);
                break;
            case AI_INTENTION_INTERACT:
                onIntentionInteract((L2Object) arg0);
                break;
        }
    }

    /**
     * Launch the L2CharacterAI onEvt method corresponding to the Event.<BR><BR>
     *
     * <FONT COLOR=#FF0000><B> <U>Caution</U> : The current general intention won't be change
     * (ex : If the character attack and is stunned, he will attack again after the stunned periode)</B></FONT><BR><BR>
     *
     * @param evt The event whose the AI must be notified
     *
     */
    public final void notifyEvent(CtrlEvent evt)
    {
        notifyEvent(evt, null, null);
    }

    /**
     * Launch the L2CharacterAI onEvt method corresponding to the Event.<BR><BR>
     *
     * <FONT COLOR=#FF0000><B> <U>Caution</U> : The current general intention won't be change
     * (ex : If the character attack and is stunned, he will attack again after the stunned periode)</B></FONT><BR><BR>
     *
     * @param evt The event whose the AI must be notified
     * @param arg0 The first parameter of the Event (optional target)
     *
     */
    public final void notifyEvent(CtrlEvent evt, Object arg0)
    {
        notifyEvent(evt, arg0, null);
    }

    /**
     * Launch the L2CharacterAI onEvt method corresponding to the Event.<BR><BR>
     *
     * <FONT COLOR=#FF0000><B> <U>Caution</U> : The current general intention won't be change
     * (ex : If the character attack and is stunned, he will attack again after the stunned periode)</B></FONT><BR><BR>
     *
     * @param evt The event whose the AI must be notified
     * @param arg0 The first parameter of the Event (optional target)
     * @param arg1 The second parameter of the Event (optional target)
     *
     */
    public final void notifyEvent(CtrlEvent evt, Object arg0, Object arg1)
    {
        if (!_actor.isVisible() || !_actor.hasAI()) return;

        /*
         if (Config.DEBUG)
         _log.warning("AbstractAI: notifyEvent -> " + evt + " " + arg0 + " " + arg1);
         */

        switch (evt)
        {
            case EVT_THINK:
                onEvtThink();
                break;
            case EVT_ATTACKED:
                onEvtAttacked((L2Character) arg0);
                break;
            case EVT_AGGRESSION:
                onEvtAggression((L2Character) arg0, ((Number) arg1).intValue());
                break;
            case EVT_STUNNED:
                onEvtStunned((L2Character) arg0);
                break;
            case EVT_SLEEPING:
                onEvtSleeping((L2Character) arg0);
                break;
            case EVT_ROOTED:
                onEvtRooted((L2Character) arg0);
                break;
            case EVT_CONFUSED:
                onEvtConfused((L2Character) arg0);
                break;
            case EVT_MUTED:
                onEvtMuted((L2Character) arg0);
                break;
            case EVT_READY_TO_ACT:
                onEvtReadyToAct();
                break;
            case EVT_USER_CMD:
                onEvtUserCmd(arg0, arg1);
                break;
            case EVT_ARRIVED:
                onEvtArrived();
                break;
            case EVT_ARRIVED_REVALIDATE:
                onEvtArrivedRevalidate();
                break;
            case EVT_ARRIVED_BLOCKED:
                onEvtArrivedBlocked((L2CharPosition) arg0);
                break;
            case EVT_FORGET_OBJECT:
                onEvtForgetObject((L2Object) arg0);
                break;
            case EVT_CANCEL:
                onEvtCancel();
                break;
            case EVT_DEAD:
                onEvtDead();
                break;
            case EVT_FAKE_DEATH:
                onEvtFakeDeath();
                break;
            case EVT_FINISH_CASTING:
                onEvtFinishCasting();
                break;
        }
    }

    protected abstract void onIntentionIdle();

    protected abstract void onIntentionActive();

    protected abstract void onIntentionRest();

    protected abstract void onIntentionAttack(L2Character target);

    protected abstract void onIntentionCast(L2Skill skill, L2Object target);

    protected abstract void onIntentionMoveTo(L2CharPosition destination);

    protected abstract void onIntentionMoveToInABoat(L2CharPosition destination, L2CharPosition origin);

    protected abstract void onIntentionFollow(L2Character target);

    protected abstract void onIntentionPickUp(L2Object item);

    protected abstract void onIntentionInteract(L2Object object);

    protected abstract void onEvtThink();

    protected abstract void onEvtAttacked(L2Character attacker);

    protected abstract void onEvtAggression(L2Character target, int aggro);

    protected abstract void onEvtStunned(L2Character attacker);

    protected abstract void onEvtSleeping(L2Character attacker);

    protected abstract void onEvtRooted(L2Character attacker);

    protected abstract void onEvtConfused(L2Character attacker);

    protected abstract void onEvtMuted(L2Character attacker);

    protected abstract void onEvtReadyToAct();

    protected abstract void onEvtUserCmd(Object arg0, Object arg1);

    protected abstract void onEvtArrived();

    protected abstract void onEvtArrivedRevalidate();

    protected abstract void onEvtArrivedBlocked(L2CharPosition blocked_at_pos);

    protected abstract void onEvtForgetObject(L2Object object);

    protected abstract void onEvtCancel();

    protected abstract void onEvtDead();

    protected abstract void onEvtFakeDeath();

    protected abstract void onEvtFinishCasting();

    /**
     * Cancel action client side by sending Server->Client packet ActionFailed to the L2PcInstance actor.<BR><BR>
     *
     * <FONT COLOR=#FF0000><B> <U>Caution</U> : Low level function, used by AI subclasses</B></FONT><BR><BR>
     *
     */
    protected void clientActionFailed()
    {
        if (_actor instanceof L2PcInstance) _actor.sendPacket(new ActionFailed());
    }

    /**
     * Sit Down the actor server side AND client side by sending Server->Client packet ChangeWaitType <I>(broadcast)</I>.<BR><BR>
     *
     * <FONT COLOR=#FF0000><B> <U>Caution</U> : Low level function, used by AI subclasses</B></FONT><BR><BR>
     *
     */
    protected void clientSitDown()
    {
        if (_actor instanceof L2PcInstance)
        {
            ((L2PcInstance)_actor).sitDown();
        }
    }

    /**
     * Stand up the actor server side AND client side by sending Server->Client packet ChangeWaitType <I>(broadcast)</I>.<BR><BR>
     *
     * <FONT COLOR=#FF0000><B> <U>Caution</U> : Low level function, used by AI subclasses</B></FONT><BR><BR>
     *
     */
    protected void clientStandUp()
    {
        if (_actor instanceof L2PcInstance)
        {
            ((L2PcInstance)_actor).standUp();
        }
    }

    /**
     * Move the actor to Pawn server side AND client side by sending Server->Client packet MoveToPawn <I>(broadcast)</I>.<BR><BR>
     *
     * <FONT COLOR=#FF0000><B> <U>Caution</U> : Low level function, used by AI subclasses</B></FONT><BR><BR>
     *
     */
    protected void moveToPawn(L2Object pawn, int offset)
    {
        // Chek if actor can move
        if (!_actor.isMovementDisabled())
        {
            if (offset < 10) offset = 10;

            // don't send packets too often, check we already moving to this pawn
            if (_client_moving && _target == pawn && _client_moving_to_pawn_offset == offset)
            {
                if (GameTimeController.getGameTicks() < _move_to_pawn_timeout) return;
            }

            // Set AI movement data
            _client_moving = true;
            _client_moving_to_pawn_offset = offset;
            _target = pawn;
            _move_to_pawn_timeout = GameTimeController.getGameTicks();
            _move_to_pawn_timeout += 1000 / GameTimeController.MILLIS_IN_TICK;

            if (pawn == null || _accessor == null) return;

            // Calculate movement data for a move to location action and add the actor to movingObjects of GameTimeController
            _accessor.moveTo(pawn.getX(), pawn.getY(), pawn.getZ(), offset);

            // Send a Server->Client packet MoveToPawn/CharMoveToLocation to the actor and all L2PcInstance in its _knownPlayers
            ServerBasePacket msg;

            if (pawn instanceof L2Character) msg = new MoveToPawn(_actor, (L2Character) pawn, offset);
            else msg = new CharMoveToLocation(_actor);

            _actor.broadcastPacket(msg);

        }
        else
        {
            _actor.sendPacket(new ActionFailed());
        }
    }

    /**
     * Move the actor to Location (x,y,z) server side AND client side by sending Server->Client packet CharMoveToLocation <I>(broadcast)</I>.<BR><BR>
     *
     * <FONT COLOR=#FF0000><B> <U>Caution</U> : Low level function, used by AI subclasses</B></FONT><BR><BR>
     *
     */
    protected void moveTo(int x, int y, int z)
    {
        // Chek if actor can move
        if (!_actor.isMovementDisabled())
        {
            // Set AI movement data
            _client_moving = true;
            _client_moving_to_pawn_offset = 0;

            // Calculate movement data for a move to location action and add the actor to movingObjects of GameTimeController
            _accessor.moveTo(x, y, z);

            // Send a Server->Client packet CharMoveToLocation to the actor and all L2PcInstance in its _knownPlayers
            CharMoveToLocation msg = new CharMoveToLocation(_actor);
            _actor.broadcastPacket(msg);

        }
        else
        {
            _actor.sendPacket(new ActionFailed());
        }
    }

    protected void moveToInABoat(L2CharPosition destination, L2CharPosition origin)
    {
        // Chek if actor can move
        if (!_actor.isMovementDisabled())
        {
            /*	// Set AI movement data
             _client_moving = true;
             _client_moving_to_pawn_offset = 0;

             // Calculate movement data for a move to location action and add the actor to movingObjects of GameTimeController
             _accessor.moveTo(((L2PcInstance)_actor).getBoat().getX() - destination.x,((L2PcInstance)_actor).getBoat().getY()- destination.y,((L2PcInstance)_actor).getBoat().getZ() - destination.z);
             */
            // Send a Server->Client packet CharMoveToLocation to the actor and all L2PcInstance in its _knownPlayers
            //CharMoveToLocation msg = new CharMoveToLocation(_actor);
            if (((L2PcInstance) _actor).getBoat() != null)
            {
                MoveToLocationInVehicle msg = new MoveToLocationInVehicle(_actor, destination, origin);
                _actor.broadcastPacket(msg);
            }

        }
        else
        {
            _actor.sendPacket(new ActionFailed());
        }
    }

    /**
     * Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation <I>(broadcast)</I>.<BR><BR>
     *
     * <FONT COLOR=#FF0000><B> <U>Caution</U> : Low level function, used by AI subclasses</B></FONT><BR><BR>
     *
     */
    protected void clientStopMoving(L2CharPosition pos)
    {
        /*
         if (Config.DEBUG)
         _log.warning("clientStopMoving();");
         */

        // Stop movement of the L2Character
        if (_actor.isMoving()) _accessor.stopMove(pos);

        _client_moving_to_pawn_offset = 0;

        if (_client_moving || pos != null)
        {
            _client_moving = false;

            // Send a Server->Client packet StopMove to the actor and all L2PcInstance in its _knownPlayers
            StopMove msg = new StopMove(_actor);
            _actor.broadcastPacket(msg);

            if (pos != null)
            {
                // Send a Server->Client packet StopRotation to the actor and all L2PcInstance in its _knownPlayers
                StopRotation sr = new StopRotation(_actor, pos.heading);
                _actor.sendPacket(sr);
                _actor.broadcastPacket(sr);
            }
        }

    }
    
    public boolean isAutoAttacking()
    {
        return _client_auto_attacking;
    }
    
    public void setAutoAttacking(boolean isAutoAttacking)
    {
        _client_auto_attacking = isAutoAttacking;
    }

    /**
     * Start the actor Auto Attack client side by sending Server->Client packet AutoAttackStart <I>(broadcast)</I>.<BR><BR>
     *
     * <FONT COLOR=#FF0000><B> <U>Caution</U> : Low level function, used by AI subclasses</B></FONT><BR><BR>
     *
     */
    public void clientStartAutoAttack()
    {
        if (!isAutoAttacking())
        {
            // Send a Server->Client packet AutoAttackStart to the actor and all L2PcInstance in its _knownPlayers
            _actor.broadcastPacket(new AutoAttackStart(_actor.getObjectId()));
            setAutoAttacking(true);
        }
        AttackStanceTaskManager.getInstance().addAttackStanceTask(_actor);
    }

    /**
     * Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop <I>(broadcast)</I>.<BR><BR>
     *
     * <FONT COLOR=#FF0000><B> <U>Caution</U> : Low level function, used by AI subclasses</B></FONT><BR><BR>
     *
     */
    public void clientStopAutoAttack()
    {
        if (_actor instanceof L2PcInstance)
        {
            if (!AttackStanceTaskManager.getInstance().getAttackStanceTask(_actor) && isAutoAttacking()) 
                AttackStanceTaskManager.getInstance().addAttackStanceTask(_actor);
        }
        else if (isAutoAttacking())
        {
            _actor.broadcastPacket(new AutoAttackStop(_actor.getObjectId()));
        }
        setAutoAttacking(false);
    }

    /**
     * Kill the actor client side by sending Server->Client packet AutoAttackStop, StopMove/StopRotation, Die <I>(broadcast)</I>.<BR><BR>
     *
     * <FONT COLOR=#FF0000><B> <U>Caution</U> : Low level function, used by AI subclasses</B></FONT><BR><BR>
     *
     */
    protected void clientNotifyDead()
    {
        // Send a Server->Client packet Die to the actor and all L2PcInstance in its _knownPlayers
        Die msg = new Die(_actor);
        _actor.broadcastPacket(msg);

        // Init AI
        _intention = AI_INTENTION_IDLE;
        _target = null;
        _cast_target = null;
        _attack_target = null;

        // Cancel the follow task if necessary
        stopFollow();
    }

    /**
     * Update the state of this actor client side by sending Server->Client packet MoveToPawn/CharMoveToLocation and AutoAttackStart to the L2PcInstance player.<BR><BR>
     *
     * <FONT COLOR=#FF0000><B> <U>Caution</U> : Low level function, used by AI subclasses</B></FONT><BR><BR>
     *
     * @param player The L2PcIstance to notify with state of this L2Character
     *
     */
    public void describeStateToPlayer(L2PcInstance player)
    {
        if (_client_moving)
        {
            if (_client_moving_to_pawn_offset != 0 && _follow_target != null)
            {
                // Send a Server->Client packet MoveToPawn to the actor and all L2PcInstance in its _knownPlayers
                MoveToPawn msg = new MoveToPawn(_actor, _follow_target, _client_moving_to_pawn_offset);
                player.sendPacket(msg);
            }
            else
            {
                // Send a Server->Client packet CharMoveToLocation to the actor and all L2PcInstance in its _knownPlayers
                CharMoveToLocation msg = new CharMoveToLocation(_actor);
                player.sendPacket(msg);
            }
        }
    }

    /**
     * Create and Launch an AI Follow Task to execute every 1s.<BR><BR>
     *
     * @param target The L2Character to follow
     *
     */
    public synchronized void startFollow(L2Character target)
    {
        if (_follow_task != null)
        {
            _follow_task.cancel(false);
            _follow_task = null;
        }

        // Create and Launch an AI Follow Task to execute every 1s
        _follow_target = target;
        _follow_task = ThreadPoolManager.getInstance().scheduleAiAtFixedRate(new FollowTask(), 5,
                                                                             FOLLOW_INTERVAL);
    }

    /**
     * Create and Launch an AI Follow Task to execute every 0.5s, following at specified range.<BR><BR>
     *
     * @param target The L2Character to follow
     *
     */
    public synchronized void startFollow(L2Character target, int range)
    {
        if (_follow_task != null)
        {
            _follow_task.cancel(false);
            _follow_task = null;
        }

        _follow_target = target;
        _follow_task = ThreadPoolManager.getInstance().scheduleAiAtFixedRate(new FollowTask(range), 5,
                                                                             ATTACK_FOLLOW_INTERVAL);
    }

    /**
     * Stop an AI Follow Task.<BR><BR>
     */
    public synchronized void stopFollow()
    {
        if (_follow_task != null)
        {
            // Stop the Follow Task
            _follow_task.cancel(false);
            _follow_task = null;
        }
        _follow_target = null;
    }

    protected L2Character getFollowTarget()
    {
        return _follow_target;
    }

    protected L2Object getTarget()
    {
        return _target;
    }

    protected synchronized void setTarget(L2Object target)
    {
        _target = target;
    }

}
