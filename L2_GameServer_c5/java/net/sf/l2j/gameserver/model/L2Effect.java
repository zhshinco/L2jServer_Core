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

import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javolution.util.FastList;
import net.sf.l2j.gameserver.GameTimeController;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.serverpackets.MagicEffectIcons;
import net.sf.l2j.gameserver.serverpackets.PartySpelled;
import net.sf.l2j.gameserver.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.serverpackets.ExOlympiadSpelledInfo;
import net.sf.l2j.gameserver.skills.EffectTemplate;
import net.sf.l2j.gameserver.skills.Env;
import net.sf.l2j.gameserver.skills.Func;
import net.sf.l2j.gameserver.skills.FuncTemplate;
import net.sf.l2j.gameserver.skills.Lambda;

/**
 * This class ...
 * 
 * @version $Revision: 1.1.2.1.2.12 $ $Date: 2005/04/11 10:06:07 $
 */
public abstract class L2Effect
{
    static final Logger _log = Logger.getLogger(L2Effect.class.getName());

    public static enum EffectState {
        CREATED, ACTING, FINISHING
    }

    public static enum EffectType {
        BUFF, CHARGE, DMG_OVER_TIME, HEAL_OVER_TIME, COMBAT_POINT_HEAL_OVER_TIME, MANA_DMG_OVER_TIME, MANA_HEAL_OVER_TIME, RELAXING, STUN, ROOT, SLEEP, HATE, FAKE_DEATH, CONFUSION, CONFUSE_MOB_ONLY, MUTE, FEAR, SILENT_MOVE, SEED, PARALYZE, STUN_SELF
    }

    private static final Func[] _emptyFunctionSet = new Func[0];

    //member _effector is the instance of L2Character that cast/used the spell/skill that is
    //causing this effect.  Do not confuse with the instance of L2Character that
    //is being affected by this effect.
    private final L2Character _effector;

    //member _effected is the instance of L2Character that was affected
    //by this effect.  Do not confuse with the instance of L2Character that
    //catsed/used this effect.
    private final L2Character _effected;

    //the skill that was used.
    private final L2Skill _skill;

    //or the items that was used.
    //private final L2Item _item;

    // the value of an update
    private final Lambda _lambda;

    // the current state
    private EffectState _state;

    // period, seconds
    private final int _period;
    private int _periodStartTicks;
    private int _periodfirsttime;

    // function templates
    private final FuncTemplate[] _funcTemplates;

    // counter
    private int _count;

    // abnormal effect mask
    private short _abnormalEffect;

    public final class EffectTask implements Runnable
    {
        protected final int delay;
        protected final int rate;

        EffectTask(int pDelay, int pRate)
        {
            this.delay = pDelay;
            this.rate = pRate;
        }

        public void run()
        {
            try
            {
                if (getPeriodfirsttime() == 0) setPeriodStartTicks(GameTimeController.getGameTicks());
                else setPeriodfirsttime(0);
                L2Effect.this.scheduleEffect();
            }
            catch (Throwable e)
            {
                _log.log(Level.SEVERE, "", e);
            }
        }
    }

    private ScheduledFuture _currentFuture;
    private EffectTask _currentTask;

    /** The Identifier of the stack group */
    private final String _stackType;

    /** The position of the effect in the stack group */
    private final int _stackOrder;

    private boolean _inUse = false;

    protected L2Effect(Env env, EffectTemplate template)
    {
        _state = EffectState.CREATED;
        _skill = env._skill;
        //_item = env._item == null ? null : env._item.getItem();
        _effected = env._target;
        _effector = env._player;
        _lambda = template._lambda;
        _funcTemplates = template._funcTemplates;
        _count = template._counter;
        _period = template._period;
        _abnormalEffect = template._abnormalEffect;
        _stackType = template._stackType;
        _stackOrder = template._stackOrder;
        _periodStartTicks = GameTimeController.getGameTicks();
        _periodfirsttime = 0;
        scheduleEffect();
    }

    public int getCount()
    {
        return _count;
    }

    public void setCount(int newcount)
    {
        _count = newcount;
    }

    public void setFirstTime(int newfirsttime)
    {
        if (_currentFuture != null)
        {
            _periodStartTicks = GameTimeController.getGameTicks() - newfirsttime
                * GameTimeController.TICKS_PER_SECOND;
            _currentFuture.cancel(false);
            _currentFuture = null;
            _currentTask = null;
            _periodfirsttime = newfirsttime;
            int duration = _period - _periodfirsttime;
            //_log.warning("Period: "+_period+"-"+_periodfirsttime+"="+duration);
            _currentTask = new EffectTask(duration * 1000, -1);
            _currentFuture = ThreadPoolManager.getInstance().scheduleEffect(_currentTask,
                                                                            duration * 1000);
        }
    }

    public int getPeriod()
    {
        return _period;
    }

    public int getTime()
    {
        return (GameTimeController.getGameTicks() - _periodStartTicks)
            / GameTimeController.TICKS_PER_SECOND;
    }

    public boolean getInUse()
    {
        return _inUse;
    }

    public void setInUse(boolean inUse)
    {
        _inUse = inUse;
    }

    public String getStackType()
    {
        return _stackType;
    }

    public int getStackOrder()
    {
        return _stackOrder;
    }

    public final L2Skill getSkill()
    {
        return _skill;
    }

    public final L2Character getEffector()
    {
        return _effector;
    }

    public final L2Character getEffected()
    {
        return _effected;
    }

    public final double calc()
    {
        Env env = new Env();
        env._player = _effector;
        env._target = _effected;
        env._skill = _skill;
        return _lambda.calc(env);
    }

    private synchronized void startEffectTask(int duration)
    {
        stopEffectTask();
        _currentTask = new EffectTask(duration, -1);
        _currentFuture = ThreadPoolManager.getInstance().scheduleEffect(_currentTask, duration);
        if (_state == EffectState.ACTING) _effected.addEffect(this);
        updateEffects(_effected);
    }

    private synchronized void startEffectTaskAtFixedRate(int delay, int rate)
    {
        stopEffectTask();
        _currentTask = new EffectTask(delay, rate);
        _currentFuture = ThreadPoolManager.getInstance().scheduleEffectAtFixedRate(_currentTask, delay,
                                                                                   rate);
        if (_state == EffectState.ACTING) _effected.addEffect(this);
        updateEffects(_effected);
    }

    /**
     * Stop the L2Effect task and send Server->Client update packet.<BR><BR>
     * 
     * <B><U> Actions</U> :</B><BR><BR>
     * <li>Cancel the effect in the the abnormal effect map of the L2Character </li>
     * <li>Stop the task of the L2Effect, remove it and update client magic icone </li><BR><BR>
     * 
     */
    public final void exit()
    {
        _state = EffectState.FINISHING;
        scheduleEffect();
    }

    /**
     * Stop the task of the L2Effect, remove it and update client magic icone.<BR><BR>
     * 
     * <B><U> Actions</U> :</B><BR><BR>
     * <li>Cancel the task </li>
     * <li>Stop and remove L2Effect from L2Character and update client magic icone </li><BR><BR>
     * 
     */
    private synchronized void stopEffectTask()
    {
        if (_currentFuture != null)
        {
            // Cancel the task
            _currentFuture.cancel(false);
            _currentFuture = null;
            _currentTask = null;

            _effected.removeEffect(this);

            // Not implemented
            _effected.updateStats();
        }
    }

    /** returns effect type */
    public abstract EffectType getEffectType();

    /** Notify started */
    public void onStart()
    {
        if (_abnormalEffect != 0) getEffected().startAbnormalEffect(_abnormalEffect);
    }

    /**
     * Cancel the effect in the the abnormal effect map of the effected L2Character.<BR><BR>
     */
    public void onExit()
    {
        if (_abnormalEffect != 0) getEffected().stopAbnormalEffect(_abnormalEffect);
    }

    /** Return true for continueation of this effect */
    public abstract boolean onActionTime();

    public final void rescheduleEffect()
    {
        if (_state != EffectState.ACTING)
        {
            scheduleEffect();
        }
        else
        {
            if (_count > 1)
            {
                startEffectTaskAtFixedRate(5, _period * 1000);
                return;
            }
            if (_period > 0)
            {
                startEffectTask(_period * 1000);
                return;
            }
        }
    }

    public final void scheduleEffect()
    {
        if (_state == EffectState.CREATED)
        {
            _state = EffectState.ACTING;
            onStart();

            if (_skill.isPvpSkill())
            {
                SystemMessage smsg = new SystemMessage(110);
                smsg.addString(_skill.getName());
                getEffected().sendPacket(smsg);
            }

            if (_count > 1)
            {
                startEffectTaskAtFixedRate(5, _period * 1000);
                return;
            }
            if (_period > 0)
            {
                startEffectTask(_period * 1000);
                return;
            }
        }

        if (_state == EffectState.ACTING)
        {
            if (_count-- > 0)
            {
                if (onActionTime()) return;
            }
            _state = EffectState.FINISHING;
        }

        if (_state == EffectState.FINISHING)
        {
            // Cancel the effect in the the abnormal effect map of the L2Character
            onExit();

            //If the time left is equal to zero, send the message
            if (_count == 0)
            {
                SystemMessage smsg3 = new SystemMessage(92);
                smsg3.addString(_skill.getName());
                getEffected().sendPacket(smsg3);
            }
            // Stop the task of the L2Effect, remove it and update client magic icone
            stopEffectTask();

        }
    }

    public void updateEffects(L2Character effectedChar)
    {
        effectedChar.updateStats();
    }

    public Func[] getStatFuncs()
    {
        if (_funcTemplates == null) return _emptyFunctionSet;
        List<Func> funcs = new FastList<Func>();
        for (FuncTemplate t : _funcTemplates)
        {
            Env env = new Env();
            env._player = getEffector();
            env._target = getEffected();
            env._skill = getSkill();
            Func f = t.getFunc(env, this); // effect is owner
            if (f != null) funcs.add(f);
        }
        if (funcs.size() == 0) return _emptyFunctionSet;
        return funcs.toArray(new Func[funcs.size()]);
    }

    public final void addIcon(MagicEffectIcons mi)
    {
        EffectTask task = _currentTask;
        ScheduledFuture future = _currentFuture;
        if (task == null || future == null) return;
        if (_state == EffectState.FINISHING || _state == EffectState.CREATED) return;
        L2Skill sk = getSkill();
        if (task.rate > 0) mi.addEffect(sk.getId(), getLevel(), -1);
        else mi.addEffect(sk.getId(), getLevel(), (int) future.getDelay(TimeUnit.MILLISECONDS));
    }

    public final void addPartySpelledIcon(PartySpelled ps)
    {
        EffectTask task = _currentTask;
        ScheduledFuture future = _currentFuture;
        if (task == null || future == null) return;
        if (_state == EffectState.FINISHING || _state == EffectState.CREATED) return;
        L2Skill sk = getSkill();
        ps.addPartySpelledEffect(sk.getId(), getLevel(), (int) future.getDelay(TimeUnit.MILLISECONDS));
    }

    public final void addOlympiadSpelledIcon(ExOlympiadSpelledInfo os)
    {
        EffectTask task = _currentTask;
        ScheduledFuture future = _currentFuture;
        if (task == null || future == null) return;
        if (_state == EffectState.FINISHING || _state == EffectState.CREATED) return;
        L2Skill sk = getSkill();
        os.addEffect(sk.getId(), getLevel(), (int) future.getDelay(TimeUnit.MILLISECONDS));
    }

    protected int getLevel()
    {
        return getSkill().getLevel();
    }

    public int getPeriodfirsttime()
    {
        return _periodfirsttime;
    }

    public void setPeriodfirsttime(int periodfirsttime)
    {
        _periodfirsttime = periodfirsttime;
    }

    public int getPeriodStartTicks()
    {
        return _periodStartTicks;
    }

    public void setPeriodStartTicks(int periodStartTicks)
    {
        _periodStartTicks = periodStartTicks;
    }
}
