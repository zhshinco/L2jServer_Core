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
package net.sf.l2j.gameserver.ai;

import static net.sf.l2j.gameserver.ai.CtrlIntention.AI_INTENTION_ATTACK;
import static net.sf.l2j.gameserver.ai.CtrlIntention.AI_INTENTION_CAST;
import static net.sf.l2j.gameserver.ai.CtrlIntention.AI_INTENTION_FOLLOW;
import static net.sf.l2j.gameserver.ai.CtrlIntention.AI_INTENTION_IDLE;
import static net.sf.l2j.gameserver.ai.CtrlIntention.AI_INTENTION_INTERACT;
import static net.sf.l2j.gameserver.ai.CtrlIntention.AI_INTENTION_PICK_UP;
import net.sf.l2j.gameserver.model.L2Summon;
import net.sf.l2j.gameserver.model.L2Character.AIAccessor;

public class L2SummonAI extends L2CharacterAI
{

    private boolean _thinking; // to prevent recursive thinking

    public L2SummonAI(AIAccessor accessor)
    {
        super(accessor);
    }

    @Override
	protected void onIntentionIdle()
    {
        stopFollow();
    	onIntentionActive();
    }

    @Override
	protected void onIntentionActive()
    {
        L2Summon summon = (L2Summon) _actor;
        if (summon.getFollowStatus()) setIntention(AI_INTENTION_FOLLOW, summon.getOwner());
        else super.onIntentionActive();
    }

    private void thinkAttack()
    {
        if (checkTargetLostOrDead(getAttackTarget()))
        {
            setAttackTarget(null);
            return;
        }
        if (maybeMoveToPawn(getAttackTarget(), _actor.getPhysicalAttackRange())) return;
        clientStopMoving(null);
        _accessor.doAttack(getAttackTarget());
        return;
    }

    private void thinkCast()
    {
        L2Summon summon = (L2Summon) _actor;
        if (checkTargetLost(getCastTarget()))
        {
            setCastTarget(null);
            return;
        }
        if (maybeMoveToPawn(getCastTarget(), _actor.getMagicalAttackRange(_skill))) return;
        clientStopMoving(null);
        summon.setFollowStatus(false);
        setIntention(AI_INTENTION_IDLE);
        _accessor.doCast(_skill);
        return;
    }

    private void thinkPickUp()
    {
        if (_actor.isAllSkillsDisabled()) return;
        if (checkTargetLost(getTarget())) return;
        if (maybeMoveToPawn(getTarget(), 36)) return;
        setIntention(AI_INTENTION_IDLE);
        ((L2Summon.AIAccessor) _accessor).doPickupItem(getTarget());
        return;
    }

    private void thinkInteract()
    {
        if (_actor.isAllSkillsDisabled()) return;
        if (checkTargetLost(getTarget())) return;
        if (maybeMoveToPawn(getTarget(), 36)) return;
        setIntention(AI_INTENTION_IDLE);
        return;
    }

    @Override
	protected void onEvtThink()
    {
        if (_thinking || _actor.isAllSkillsDisabled()) return;
        _thinking = true;
        try
        {
            if (getIntention() == AI_INTENTION_ATTACK) thinkAttack();
            else if (getIntention() == AI_INTENTION_CAST) thinkCast();
            else if (getIntention() == AI_INTENTION_PICK_UP) thinkPickUp();
            else if (getIntention() == AI_INTENTION_INTERACT) thinkInteract();
        }
        finally
        {
            _thinking = false;
        }
    }

}
