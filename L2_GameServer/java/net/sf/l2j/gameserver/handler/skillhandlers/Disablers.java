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
package net.sf.l2j.gameserver.handler.skillhandlers;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javolution.util.FastList;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.ai.CtrlEvent;
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.ai.L2AttackableAI;
import net.sf.l2j.gameserver.handler.ISkillHandler;
import net.sf.l2j.gameserver.handler.SkillHandler;
import net.sf.l2j.gameserver.instancemanager.DuelManager;
import net.sf.l2j.gameserver.model.L2Attackable;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2Summon;
import net.sf.l2j.gameserver.model.actor.instance.L2CubicInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2NpcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PlayableInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2SiegeSummonInstance;
import net.sf.l2j.gameserver.model.base.Experience;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.Env;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.skills.Stats;
import net.sf.l2j.gameserver.skills.effects.EffectBuff;
import net.sf.l2j.gameserver.templates.L2SkillType;
import net.sf.l2j.util.Rnd;

/**
 * This Handles Disabler skills
 * @author _drunk_
 */
public class Disablers implements ISkillHandler
{
	private static final L2SkillType[] SKILL_IDS =
	{
		L2SkillType.STUN,
		L2SkillType.ROOT,
		L2SkillType.SLEEP,
		L2SkillType.CONFUSION,
		L2SkillType.AGGDAMAGE,
		L2SkillType.AGGREDUCE,
		L2SkillType.AGGREDUCE_CHAR,
		L2SkillType.AGGREMOVE,
		L2SkillType.UNBLEED,
		L2SkillType.UNPOISON,
		L2SkillType.MUTE,
		L2SkillType.FAKE_DEATH,
		L2SkillType.CONFUSE_MOB_ONLY,
		L2SkillType.NEGATE,
		L2SkillType.CANCEL,
		L2SkillType.CANCEL_DEBUFF,
		L2SkillType.PARALYZE,
		L2SkillType.ERASE,
		L2SkillType.MAGE_BANE,
		L2SkillType.WARRIOR_BANE,
		L2SkillType.BETRAY,
		L2SkillType.DISARM,
		L2SkillType.STEAL_BUFF
	};
	
	protected static final Logger _log = Logger.getLogger(L2Skill.class.getName());
	private String[] _negateStats = null;
	private float _negatePower = 0.f;
	private int _negateId = 0;
	
	/**
	 * 
	 * @see net.sf.l2j.gameserver.handler.ISkillHandler#useSkill(net.sf.l2j.gameserver.model.L2Character, net.sf.l2j.gameserver.model.L2Skill, net.sf.l2j.gameserver.model.L2Object[])
	 */
	public void useSkill(L2Character activeChar, L2Skill skill, L2Object[] targets)
	{
		L2SkillType type = skill.getSkillType();
		
		boolean ss = false;
		boolean sps = false;
		boolean bss = false;
		
		L2ItemInstance weaponInst = activeChar.getActiveWeaponInstance();
		
		if (activeChar instanceof L2PcInstance)
		{
			if (weaponInst == null && skill.isOffensive())
			{
				activeChar.sendMessage("You must equip a weapon before casting a spell.");
				return;
			}
		}
		
		if (weaponInst != null)
		{
			if (skill.isMagic())
			{
				if (weaponInst.getChargedSpiritshot() == L2ItemInstance.CHARGED_BLESSED_SPIRITSHOT)
				{
					bss = true;
					if (skill.getId() != 1020) // vitalize
						weaponInst.setChargedSpiritshot(L2ItemInstance.CHARGED_NONE);
				}
				else if (weaponInst.getChargedSpiritshot() == L2ItemInstance.CHARGED_SPIRITSHOT)
				{
					sps = true;
					if (skill.getId() != 1020) // vitalize
						weaponInst.setChargedSpiritshot(L2ItemInstance.CHARGED_NONE);
				}
			}
			else
			{
				if (weaponInst.getChargedSoulshot() == L2ItemInstance.CHARGED_SOULSHOT)
				{
					ss = true;
					if (skill.getId() != 1020) // vitalize
						weaponInst.setChargedSoulshot(L2ItemInstance.CHARGED_NONE);
				}
			}
		}
		// If there is no weapon equipped, check for an active summon.
		else if (activeChar instanceof L2Summon)
		{
			L2Summon activeSummon = (L2Summon) activeChar;
			
			if (skill.isMagic())
			{
				if (activeSummon.getChargedSpiritShot() == L2ItemInstance.CHARGED_BLESSED_SPIRITSHOT)
				{
					bss = true;
					activeSummon.setChargedSpiritShot(L2ItemInstance.CHARGED_NONE);
				}
				else if (activeSummon.getChargedSpiritShot() == L2ItemInstance.CHARGED_SPIRITSHOT)
				{
					sps = true;
					activeSummon.setChargedSpiritShot(L2ItemInstance.CHARGED_NONE);
				}
			}
			else
			{
				if (activeSummon.getChargedSoulShot() == L2ItemInstance.CHARGED_SOULSHOT)
				{
					ss = true;
					activeSummon.setChargedSoulShot(L2ItemInstance.CHARGED_NONE);
				}
			}
		}
		else if (activeChar instanceof L2NpcInstance)
		{
			bss = ((L2NpcInstance) activeChar).isUsingShot(false);
			ss = ((L2NpcInstance) activeChar).isUsingShot(true);
		}
		
		for (L2Character target: (L2Character[]) targets)
		{
			if (target == null || target.isDead() || target.isInvul()) //bypass if target is null, invul or dead
				continue;
			
			switch (type)
			{
				case BETRAY:
				{
					if (Formulas.getInstance().calcSkillSuccess(activeChar, target, skill, ss, sps, bss))
						skill.getEffects(activeChar, target);
					else
					{
						SystemMessage sm = new SystemMessage(SystemMessageId.S1_WAS_UNAFFECTED_BY_S2);
						sm.addCharName(target);
						sm.addSkillName(skill);
						activeChar.sendPacket(sm);
					}
					break;
				}
				case FAKE_DEATH:
				{
					// stun/fakedeath is not mdef dependant, it depends on lvl difference, target CON and power of stun
					skill.getEffects(activeChar, target);
					break;
				}
				case ROOT:
				case DISARM:
				case STUN:
				{
					if (target.reflectSkill(skill))
						target = activeChar;
					
					if (Formulas.getInstance().calcSkillSuccess(activeChar, target, skill, ss, sps, bss))
						skill.getEffects(activeChar, target);
					else
					{
						if (activeChar instanceof L2PcInstance)
						{
							SystemMessage sm = new SystemMessage(SystemMessageId.S1_WAS_UNAFFECTED_BY_S2);
							sm.addCharName(target);
							sm.addSkillName(skill);
							activeChar.sendPacket(sm);
						}
					}
					break;
				}
				case SLEEP:
				case PARALYZE: //use same as root for now
				{
					if (target.reflectSkill(skill))
						target = activeChar;
					
					if (Formulas.getInstance().calcSkillSuccess(activeChar, target, skill, ss, sps, bss))
						skill.getEffects(activeChar, target);
					else
					{
						if (activeChar instanceof L2PcInstance)
						{
							SystemMessage sm = new SystemMessage(SystemMessageId.S1_WAS_UNAFFECTED_BY_S2);
							sm.addCharName(target);
							sm.addSkillName(skill);
							activeChar.sendPacket(sm);
						}
					}
					break;
				}
				case CONFUSION:
				case MUTE:
				{
					if (target.reflectSkill(skill))
						target = activeChar;
					
					if (Formulas.getInstance().calcSkillSuccess(activeChar, target, skill, ss, sps, bss))
					{
						// stop same type effect if avaiable
						L2Effect[] effects = target.getAllEffects();
						for (L2Effect e : effects)
							if (e.getSkill().getSkillType() == type)
								e.exit();
						// then restart
						// Make above skills mdef dependant
						if (Formulas.getInstance().calcSkillSuccess(activeChar, target, skill, ss, sps, bss))
							//if(Formulas.getInstance().calcMagicAffected(activeChar, target, skill))
							skill.getEffects(activeChar, target);
						else
						{
							if (activeChar instanceof L2PcInstance)
							{
								SystemMessage sm = new SystemMessage(SystemMessageId.S1_WAS_UNAFFECTED_BY_S2);
								sm.addCharName(target);
								sm.addSkillName(skill);
								activeChar.sendPacket(sm);
							}
						}
					}
					else
					{
						if (activeChar instanceof L2PcInstance)
						{
							SystemMessage sm = new SystemMessage(SystemMessageId.S1_WAS_UNAFFECTED_BY_S2);
							sm.addCharName(target);
							sm.addSkillName(skill);
							activeChar.sendPacket(sm);
						}
					}
					break;
				}
				case CONFUSE_MOB_ONLY:
				{
					// do nothing if not on mob
					if (target instanceof L2Attackable)
					{
						if (Formulas.getInstance().calcSkillSuccess(activeChar, target, skill, ss, sps, bss))
						{
							L2Effect[] effects = target.getAllEffects();
							for (L2Effect e : effects)
							{
								if (e.getSkill().getSkillType() == type)
									e.exit();
							}
							skill.getEffects(activeChar, target);
						}
						else
						{
							if (activeChar instanceof L2PcInstance)
							{
								SystemMessage sm = new SystemMessage(SystemMessageId.S1_WAS_UNAFFECTED_BY_S2);
								sm.addCharName(target);
								sm.addSkillName(skill);
								activeChar.sendPacket(sm);
							}
						}
					}
					else
						activeChar.sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
					break;
				}
				case AGGDAMAGE:
				{
					if (target instanceof L2Attackable)
						target.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, activeChar, (int) ((150 * skill.getPower()) / (target.getLevel() + 7)));
					// TODO [Nemesiss] should this have 100% chance?
					skill.getEffects(activeChar, target);
					break;
				}
				case AGGREDUCE:
				{
					// these skills needs to be rechecked
					if (target instanceof L2Attackable)
					{
						skill.getEffects(activeChar, target);
						
						double aggdiff = ((L2Attackable) target).getHating(activeChar) - target.calcStat(Stats.AGGRESSION, ((L2Attackable) target).getHating(activeChar), target, skill);
						
						if (skill.getPower() > 0)
							((L2Attackable) target).reduceHate(null, (int) skill.getPower());
						else if (aggdiff > 0)
							((L2Attackable) target).reduceHate(null, (int) aggdiff);
					}
					// when fail, target.getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, activeChar);
					break;
				}
				case AGGREDUCE_CHAR:
				{
					// these skills needs to be rechecked
					if (Formulas.getInstance().calcSkillSuccess(activeChar, target, skill, ss, sps, bss))
					{
						if (target instanceof L2Attackable)
						{
							L2Attackable targ = (L2Attackable) target;
							targ.stopHating(activeChar);
							if (targ.getMostHated() == null)
							{
								((L2AttackableAI) targ.getAI()).setGlobalAggro(-25);
								targ.clearAggroList();
								targ.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
								targ.setWalking();
							}
						}
						skill.getEffects(activeChar, target);
					}
					else
					{
						if (activeChar instanceof L2PcInstance)
						{
							SystemMessage sm = new SystemMessage(SystemMessageId.S1_WAS_UNAFFECTED_BY_S2);
							sm.addCharName(target);
							sm.addSkillName(skill);
							activeChar.sendPacket(sm);
						}
						target.getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, activeChar);
					}
					break;
				}
				case AGGREMOVE:
				{
					// these skills needs to be rechecked
					if (target instanceof L2Attackable && !target.isRaid())
					{
						if (Formulas.getInstance().calcSkillSuccess(activeChar, target, skill, ss, sps, bss))
						{
							if (skill.getTargetType() == L2Skill.SkillTargetType.TARGET_UNDEAD)
							{
								if (target.isUndead())
									((L2Attackable) target).reduceHate(null, ((L2Attackable) target).getHating(((L2Attackable) target).getMostHated()));
							}
							else
								((L2Attackable) target).reduceHate(null, ((L2Attackable) target).getHating(((L2Attackable) target).getMostHated()));
						}
						else
						{
							if (activeChar instanceof L2PcInstance)
							{
								SystemMessage sm = new SystemMessage(SystemMessageId.S1_WAS_UNAFFECTED_BY_S2);
								sm.addCharName(target);
								sm.addSkillName(skill);
								activeChar.sendPacket(sm);
							}
							target.getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, activeChar);
						}
					}
					else
						target.getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, activeChar);
					break;
				}
				case UNBLEED:
				{
					negateEffect(target, L2SkillType.BLEED, skill.getPower(), skill.getMaxNegatedEffects());
					break;
				}
				case UNPOISON:
				{
					negateEffect(target, L2SkillType.POISON, skill.getPower(), skill.getMaxNegatedEffects());
					break;
				}
				case ERASE:
				{
					if (Formulas.getInstance().calcSkillSuccess(activeChar, target, skill, ss, sps, bss)
					// doesn't affect siege golem or wild hog cannon
							&& !(target instanceof L2SiegeSummonInstance))
					{
						L2PcInstance summonOwner = null;
						L2Summon summonPet = null;
						summonOwner = ((L2Summon) target).getOwner();
						summonPet = summonOwner.getPet();
						if (summonPet != null)
						{
							summonPet.unSummon(summonOwner);
							SystemMessage sm = new SystemMessage(SystemMessageId.YOUR_SERVITOR_HAS_VANISHED);
							summonOwner.sendPacket(sm);
						}
					}
					else
					{
						if (activeChar instanceof L2PcInstance)
						{
							SystemMessage sm = new SystemMessage(SystemMessageId.S1_WAS_UNAFFECTED_BY_S2);
							sm.addCharName(target);
							sm.addSkillName(skill);
							activeChar.sendPacket(sm);
						}
					}
					break;
				}
				case MAGE_BANE:
				{
					if (target.reflectSkill(skill))
						target = activeChar;
					
					if (!Formulas.getInstance().calcSkillSuccess(activeChar, target, skill, ss, sps, bss))
					{
						if (activeChar instanceof L2PcInstance)
						{
							SystemMessage sm = new SystemMessage(SystemMessageId.S1_WAS_UNAFFECTED_BY_S2);
							sm.addCharName(target);
							sm.addSkillName(skill);
							activeChar.sendPacket(sm);
						}
						continue;
					}
					
					L2Effect[] effects = target.getAllEffects();
					for (L2Effect e : effects)
					{
						// TODO: Unhardcode this SkillType, maybe on its own child class
						// only Acumen and Greater Empower
						if (e.getSkill().getId() == 1085 || e.getSkill().getId() == 1059)
							e.exit();
					}
					
					break;
				}
				case WARRIOR_BANE:
				{
					if (target.reflectSkill(skill))
						target = activeChar;
					
					if (!Formulas.getInstance().calcSkillSuccess(activeChar, target, skill, ss, sps, bss))
					{
						if (activeChar instanceof L2PcInstance)
						{
							SystemMessage sm = new SystemMessage(SystemMessageId.S1_WAS_UNAFFECTED_BY_S2);
							sm.addCharName(target);
							sm.addSkillName(skill);
							activeChar.sendPacket(sm);
						}
						continue;
					}
					
					L2Effect[] effects = target.getAllEffects();
					for (L2Effect e : effects)
					{
						// TODO: Unhardcode this SkillType, maybe on its own child class
						// only Wind Walk and Haste
						if (e.getSkill().getId() == 1204 || e.getSkill().getId() == 1086)
							e.exit();
					}
					break;
				}
				case CANCEL_DEBUFF:
				{
					L2Effect[] effects = target.getAllEffects();
					
					if (effects.length == 0 || effects == null)
						break;
					
					int count = (skill.getMaxNegatedEffects() > 0) ? skill.getMaxNegatedEffects() : -2;
					for (L2Effect e : effects)
					{
						if (e.getSkill().isDebuff() && count < skill.getMaxNegatedEffects())
						{
							e.exit();
							if (count > -1)
								count++;
						}
					}
					
					break;
				}
				case STEAL_BUFF:
				{
					if (!(target instanceof L2PlayableInstance))
						return;
							
					L2Effect[] effects = target.getAllEffects();
						
					if (effects == null || effects.length < 1)
						return;
						
						// Reversing array
						List<L2Effect> list = Arrays.asList(effects);
						Collections.reverse(list);
						list.toArray(effects);
						
						FastList<L2Effect> toSteal = new FastList<L2Effect>();
						int count = 0;
						int lastSkill = 0;
						
						for (L2Effect e : effects)
						{
							if (e == null || (!(e instanceof EffectBuff) && e.getEffectType() != L2Effect.EffectType.TRANSFORMATION)
									|| e.getSkill().getSkillType() == L2SkillType.HEAL
									|| e.getSkill().isToggle()
									|| e.getSkill().isDebuff()
									|| e.getSkill().isHeroSkill()
									|| e.getSkill().isPotion()
									|| e.isHerbEffect())
								continue;
							
							if (e.getSkill().getId() == lastSkill)
							{
								if (count == 0) count = 1;
									toSteal.add(e);
							}
							else if (count < skill.getPower())
							{
								toSteal.add(e);
								count++;
							}
							else
								break;
						}
						if (!toSteal.isEmpty())
							stealEffects(activeChar, target, toSteal);
						
					break;
				}
				case CANCEL:
				case NEGATE:
				{
					if (target.reflectSkill(skill))
						target = activeChar;
					
					if (skill.cancelEffect() > 0)
					{
						L2Effect[] effects = target.getAllEffects();
						for (L2Effect e : effects)
						{
							if (e.getSkill().getId() == skill.cancelEffect())
								e.exit();
						}
					}
					//TODO@   Rewrite it to properly use Formulas class.
					// cancel
					else if (skill.getId() == 1056)
					{
						int lvlmodifier = 52 + skill.getLevel() * 2;
						if (skill.getLevel() == 12)
							lvlmodifier = (Experience.MAX_LEVEL - 1);
						int landrate = 90;
						if ((target.getLevel() - lvlmodifier) > 0)
							landrate = 90 - 4 * (target.getLevel() - lvlmodifier);
						
						landrate = (int) activeChar.calcStat(Stats.CANCEL_VULN, landrate, target, null);
						
						if (Rnd.get(100) < landrate)
						{
							L2Effect[] effects = target.getAllEffects();
							int maxfive = skill.getMaxNegatedEffects();
							for (L2Effect e : effects)
							{
								// do not delete signet effects!
								switch (e.getEffectType())
								{
									case SIGNET_GROUND:
									case SIGNET_EFFECT:
										continue;
								}
								
								if (e.getSkill().getId() != 4082 && e.getSkill().getId() != 4215 && e.getSkill().getId() != 4515  && e.getSkill().getId() != 5182 && e.getSkill().getId() != 110 && e.getSkill().getId() != 111 && e.getSkill().getId() != 1323
										&& e.getSkill().getId() != 1325) // Cannot cancel skills 4082, 4215, 4515, 110, 111, 1323, 1325
								{
									if (e.getSkill().getSkillType() != L2SkillType.BUFF) //sleep, slow, surrenders etc
										e.exit();
									else
									{
										int rate = 100;
										int level = e.getLevel();
										if (level > 0)
											rate = Integer.valueOf(150 / (1 + level));
										if (rate > 95)
											rate = 95;
										else if (rate < 5)
											rate = 5;
										if (Rnd.get(100) < rate)
										{
											e.exit();
											maxfive--;
											if (maxfive == 0)
												break;
										}
									}
								}
							}
						}
						else
						{
							if (activeChar instanceof L2PcInstance)
							{
								SystemMessage sm = new SystemMessage(SystemMessageId.S1_WAS_UNAFFECTED_BY_S2);
								sm.addCharName(target);
								sm.addSkillName(skill);
								activeChar.sendPacket(sm);
							}
						}
						break;
					}
					// fishing potion
					else if (skill.getId() == 2275)
					{
						_negatePower = skill.getNegatePower();
						_negateId = skill.getNegateId();
						
						negateEffect(target, L2SkillType.BUFF, _negatePower, _negateId, -1);
					}
					// all others negate type skills
					else
					{
						_negateStats = skill.getNegateStats();
						_negatePower = skill.getNegatePower();
						
						int removedBuffs = (skill.getMaxNegatedEffects() > 0) ? 0 : -2;
						
						for (String stat : _negateStats)
						{
							if (removedBuffs > skill.getMaxNegatedEffects())
								break;
							
							stat = stat.toLowerCase().intern();
							
							if (stat == "buff")
							{
								int lvlmodifier = 52 + skill.getMagicLevel() * 2;
								if (skill.getMagicLevel() == 12)
									lvlmodifier = (Experience.MAX_LEVEL - 1);
								int landrate = 90;
								if ((target.getLevel() - lvlmodifier) > 0)
									landrate = 90 - 4 * (target.getLevel() - lvlmodifier);
								
								landrate = (int) activeChar.calcStat(Stats.CANCEL_VULN, landrate, target, null);
								
								if (Rnd.get(100) < landrate)
									removedBuffs += negateEffect(target, L2SkillType.BUFF, -1, skill.getMaxNegatedEffects());
							}
							else if (stat == "debuff" && removedBuffs < skill.getMaxNegatedEffects())
								removedBuffs += negateEffect(target, L2SkillType.DEBUFF, -1, skill.getMaxNegatedEffects());
							else if (stat == "weakness" && removedBuffs < skill.getMaxNegatedEffects())
								removedBuffs += negateEffect(target, L2SkillType.WEAKNESS, -1, skill.getMaxNegatedEffects());
							else if (stat == "stun" && removedBuffs < skill.getMaxNegatedEffects())
								removedBuffs += negateEffect(target, L2SkillType.STUN, -1, skill.getMaxNegatedEffects());
							else if (stat == "sleep" && removedBuffs < skill.getMaxNegatedEffects())
								removedBuffs += negateEffect(target, L2SkillType.SLEEP, -1, skill.getMaxNegatedEffects());
							else if (stat == "confusion" && removedBuffs < skill.getMaxNegatedEffects())
								removedBuffs += negateEffect(target, L2SkillType.CONFUSION, -1, skill.getMaxNegatedEffects());
							else if (stat == "mute" && removedBuffs < skill.getMaxNegatedEffects())
								removedBuffs += negateEffect(target, L2SkillType.MUTE, -1, skill.getMaxNegatedEffects());
							else if (stat == "fear" && removedBuffs < skill.getMaxNegatedEffects())
								removedBuffs += negateEffect(target, L2SkillType.FEAR, -1, skill.getMaxNegatedEffects());
							else if (stat == "poison" && removedBuffs < skill.getMaxNegatedEffects())
								removedBuffs += negateEffect(target, L2SkillType.POISON, _negatePower, skill.getMaxNegatedEffects());
							else if (stat == "bleed" && removedBuffs < skill.getMaxNegatedEffects())
								removedBuffs += negateEffect(target, L2SkillType.BLEED, _negatePower, skill.getMaxNegatedEffects());
							else if (stat == "paralyze" && removedBuffs < skill.getMaxNegatedEffects())
								removedBuffs += negateEffect(target, L2SkillType.PARALYZE, -1, skill.getMaxNegatedEffects());
							else if (stat == "root" && removedBuffs < skill.getMaxNegatedEffects())
								removedBuffs += negateEffect(target, L2SkillType.ROOT, -1, skill.getMaxNegatedEffects());
							else if (stat == "heal" && removedBuffs < skill.getMaxNegatedEffects())
							{
								ISkillHandler Healhandler = SkillHandler.getInstance().getSkillHandler(L2SkillType.HEAL);
								if (Healhandler == null)
								{
									_log.severe("Couldn't find skill handler for HEAL.");
									continue;
								}
								L2Character tgts[] = new L2Character[]{target};
								try
								{
									Healhandler.useSkill(activeChar, skill, tgts);
								}
								catch (IOException e)
								{
									_log.log(Level.WARNING, "", e);
								}
							}
						}//end for
					}//end else
					
					if (Formulas.getInstance().calcSkillSuccess(activeChar, target, skill, ss, sps, bss))
					{
						skill.getEffects(activeChar, target);
					}
				}// end case
			}//end switch
			
			//Possibility of a lethal strike
			Formulas.getInstance().calcLethalHit(activeChar, target, skill);
			
		}//end for
		
		// self Effect :]
		L2Effect effect = activeChar.getFirstEffect(skill.getId());
		if (effect != null && effect.isSelfEffect())
		{
			//Replace old effect with new one.
			effect.exit();
		}
		skill.getEffectsSelf(activeChar);
		
	} //end void
	
	public void useCubicSkill(L2CubicInstance activeCubic, L2Skill skill, L2Object[] targets)
	{
		if (Config.DEBUG)
			_log.info("Disablers: useCubicSkill()");
		
		L2SkillType type = skill.getSkillType();
		
		for (L2Character target: (L2Character[]) targets)
		{
			if (target == null || target.isDead()) //bypass if target is null or dead
				continue;
			
			switch (type)
			{
				case STUN:
				{
					if (Formulas.getInstance().calcCubicSkillSuccess(activeCubic, target, skill))
					{
						// if this is a debuff let the duel manager know about it
						// so the debuff can be removed after the duel
						// (player & target must be in the same duel)
						if (target instanceof L2PcInstance && ((L2PcInstance) target).isInDuel() && skill.getSkillType() == L2SkillType.DEBUFF && activeCubic.getOwner().getDuelId() == ((L2PcInstance) target).getDuelId())
						{
							DuelManager dm = DuelManager.getInstance();
							for (L2Effect debuff : skill.getEffects(activeCubic.getOwner(), target))
								if (debuff != null)
									dm.onBuff(((L2PcInstance) target), debuff);
						}
						else
							skill.getEffects(activeCubic, target);
						SystemMessage sm = new SystemMessage(SystemMessageId.S1_SUCCEEDED);
						sm.addSkillName(skill);
						activeCubic.getOwner().sendPacket(sm);
						if (Config.DEBUG)
							_log.info("Disablers: useCubicSkill() -> success");
					}
					else
					{
						SystemMessage sm = new SystemMessage(SystemMessageId.S1_WAS_UNAFFECTED_BY_S2);
						sm.addCharName(target);
						sm.addSkillName(skill);
						activeCubic.getOwner().sendPacket(sm);
						if (Config.DEBUG)
							_log.info("Disablers: useCubicSkill() -> failed");
					}
					break;
				}
				case PARALYZE: //use same as root for now
				{
					if (Formulas.getInstance().calcCubicSkillSuccess(activeCubic, target, skill))
					{
						// if this is a debuff let the duel manager know about it
						// so the debuff can be removed after the duel
						// (player & target must be in the same duel)
						if (target instanceof L2PcInstance && ((L2PcInstance) target).isInDuel() && skill.getSkillType() == L2SkillType.DEBUFF && activeCubic.getOwner().getDuelId() == ((L2PcInstance) target).getDuelId())
						{
							DuelManager dm = DuelManager.getInstance();
							for (L2Effect debuff : skill.getEffects(activeCubic.getOwner(), target))
								if (debuff != null)
									dm.onBuff(((L2PcInstance) target), debuff);
						}
						else
							skill.getEffects(activeCubic, target);
						
						SystemMessage sm = new SystemMessage(SystemMessageId.S1_SUCCEEDED);
						sm.addSkillName(skill);
						activeCubic.getOwner().sendPacket(sm);
						if (Config.DEBUG)
							_log.info("Disablers: useCubicSkill() -> success");
					}
					else
					{
						SystemMessage sm = new SystemMessage(SystemMessageId.S1_WAS_UNAFFECTED_BY_S2);
						sm.addCharName(target);
						sm.addSkillName(skill);
						activeCubic.getOwner().sendPacket(sm);
						if (Config.DEBUG)
							_log.info("Disablers: useCubicSkill() -> failed");
					}
					break;
				}
				case CANCEL_DEBUFF:
				{
					L2Effect[] effects = target.getAllEffects();
					
					if (effects == null || effects.length == 0)
						break;
					
					int count = (skill.getMaxNegatedEffects() > 0) ? 0 : -2;
					for (L2Effect e : effects)
					{
						if (e.getSkill().isDebuff() && count < skill.getMaxNegatedEffects())
						{
							e.exit();
							if (count > -1)
								count++;
						}
					}
					
					break;
				}
			}//end switch
		}//end for
	}
	
	/**
	 * 
	 * @param target
	 * @param type
	 * @param power
	 * @param maxRemoved
	 * @return
	 */
	private int negateEffect(L2Character target, L2SkillType type, double power, int maxRemoved)
	{
		return negateEffect(target, type, power, 0, maxRemoved);
	}
	
	/**
	 * 
	 * @param target
	 * @param type
	 * @param power
	 * @param skillId
	 * @param maxRemoved
	 * @return
	 */
	private int negateEffect(L2Character target, L2SkillType type, double power, int skillId, int maxRemoved)
	{
		L2Effect[] effects = target.getAllEffects();
		int count = (maxRemoved <= 0) ? -2 : 0;
		for (L2Effect e : effects)
		{
			if (power == -1) // if power is -1 the effect is always removed without power/lvl check ^^
			{
				if (e.getSkill().getSkillType() == type || (e.getSkill().getEffectType() != null && e.getSkill().getEffectType() == type))
				{
					if (skillId != 0)
					{
						if (skillId == e.getSkill().getId() && count < maxRemoved)
						{
							e.exit();
							if (count > -1)
								count++;
						}
					}
					else if (count < maxRemoved)
					{
						e.exit();
						if (count > -1)
							count++;
					}
				}
			}
			else if ((e.getSkill().getSkillType() == type && e.getSkill().getPower() <= power) || (e.getSkill().getEffectType() != null && e.getSkill().getEffectType() == type && e.getSkill().getEffectLvl() <= power))
			{
				if (skillId != 0)
				{
					if (skillId == e.getSkill().getId() && count < maxRemoved)
					{
						e.exit();
						if (count > -1)
							count++;
					}
				}
				else if (count < maxRemoved)
				{
					e.exit();
					if (count > -1)
						count++;
				}
			}
		}
		
		return (maxRemoved <= 0) ? count + 2 : count;
	}
	
	private void stealEffects(L2Character stealer, L2Character stolen, FastList<L2Effect> stolenEffects)
	{
		if (stolen == null || stolenEffects == null || stolenEffects.isEmpty()) return;
		
		for (L2Effect eff : stolenEffects)
		{
			// if eff time is smaller than 1 sec, will not be stolen, just to save CPU,
			// avoid synchronization(?) problems and NPEs
			if (eff.getPeriod() - eff.getTime() < 1)
				continue;
			
			Env env = new Env();
			env.player = stolen;
			env.target = stealer;
			env.skill = eff.getSkill();
			L2Effect e = eff.getEffectTemplate().getStolenEffect(env, eff);
			
			// Since there is a previous check that limits allowed effects to those which come from SkillType.BUFF,
			// it is not needed another check for SkillType
			if (stealer instanceof L2PcInstance && e != null)
			{
				SystemMessage smsg = new SystemMessage(SystemMessageId.YOU_FEEL_S1_EFFECT);
				smsg.addSkillName(eff);
				stealer.sendPacket(smsg);
			}
			// Finishing stolen effect
				eff.exit();
		}
	}
	
	/**
	 * 
	 * @see net.sf.l2j.gameserver.handler.ISkillHandler#getSkillIds()
	 */
	public L2SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}
