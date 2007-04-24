package net.sf.l2j.gameserver.model.actor.status;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2Summon;
import net.sf.l2j.gameserver.model.actor.instance.L2NpcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PlayableInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2SummonInstance;
import net.sf.l2j.gameserver.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.Stats;
import net.sf.l2j.gameserver.util.Util;

public class PcStatus extends PlayableStatus
{
    // =========================================================
    // Data Field
    
    // =========================================================
    // Constructor
    public PcStatus(L2PcInstance activeChar)
    {
        super(activeChar);
    }

    // =========================================================
    // Method - Public
    public final void reduceHp(double value, L2Character attacker) { reduceHp(value, attacker, true); }
    public final void reduceHp(double value, L2Character attacker, boolean awake)
    {
        if (getActiveChar().isInvul()) return;
        
		if ( attacker instanceof L2PcInstance)
		{
			if ( getActiveChar().isDead() && !getActiveChar().isFakeDeath()) return;
		} else {
			if ( getActiveChar().isDead()) return;
		}
		
		int fullValue = (int) value;
		
        if (attacker != null && attacker != getActiveChar())
        {
            // Check and calculate transfered damage
            L2Summon summon = getActiveChar().getPet();
            //TODO correct range 
            if (summon != null && summon instanceof L2SummonInstance && Util.checkIfInRange(900, getActiveChar(), summon, true))
            {
                int tDmg = (int)value * (int)getActiveChar().getStat().calcStat(Stats.TRANSFER_DAMAGE_PERCENT, 0, null, null) /100;
                
                // Only transfer dmg up to current HP, it should not be killed
                if (summon.getCurrentHp() < tDmg) tDmg = (int)summon.getCurrentHp() - 1;
                if (tDmg > 0)
                {
                    summon.reduceCurrentHp(tDmg, attacker);
                    value -= tDmg;
                    fullValue = (int) value; // reduce the annouced value here as player will get a message about summon dammage
                }
            }

            if (attacker instanceof L2PlayableInstance)
            {
                if (getCurrentCp() >= value)
                {
                    setCurrentCp(getCurrentCp() - value);   // Set Cp to diff of Cp vs value
                    value = 0;                              // No need to subtract anything from Hp
                }
                else
                {
                    value -= getCurrentCp();                // Get diff from value vs Cp; will apply diff to Hp
                    setCurrentCp(0);                        // Set Cp to 0
                }
            }
        }

        super.reduceHp(value, attacker, awake);

        if (!getActiveChar().isDead() && getActiveChar().isSitting()) 
            getActiveChar().standUp();
        
        if (getActiveChar().isFakeDeath()) 
            getActiveChar().stopFakeDeath(null);

        if (attacker != null && attacker != getActiveChar() && fullValue > 0)
        {
            // Send a System Message to the L2PcInstance
            SystemMessage smsg = new SystemMessage(SystemMessage.S1_GAVE_YOU_S2_DMG);

            if (Config.DEBUG)
                _log.fine("Attacker:" + attacker.getName());

            if (attacker instanceof L2NpcInstance)
            {
                int mobId = ((L2NpcInstance)attacker).getTemplate().idTemplate;
                
                if (Config.DEBUG) 
                    _log.fine("mob id:" + mobId);
                
                smsg.addNpcName(mobId);
            }
            else if (attacker instanceof L2Summon)
            {
                int mobId = ((L2Summon)attacker).getTemplate().idTemplate;
                
                smsg.addNpcName(mobId);
            }
            else
            {
                smsg.addString(attacker.getName());
            }
            
            smsg.addNumber(fullValue);
            getActiveChar().sendPacket(smsg);
        }
    }

    // =========================================================
    // Method - Private

    // =========================================================
    // Property - Public
    public L2PcInstance getActiveChar() { return (L2PcInstance)super.getActiveChar(); }
}
