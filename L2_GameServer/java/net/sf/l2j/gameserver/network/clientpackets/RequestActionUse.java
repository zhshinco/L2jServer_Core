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
package net.sf.l2j.gameserver.network.clientpackets;

import java.util.Arrays;
import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.ai.L2SummonAI;
import net.sf.l2j.gameserver.datatables.PetSkillsTable;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.instancemanager.CastleManager;
import net.sf.l2j.gameserver.model.L2CharPosition;
import net.sf.l2j.gameserver.model.L2ManufactureList;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.L2Summon;
import net.sf.l2j.gameserver.model.actor.instance.L2DoorInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PetInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2SiegeSummonInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2StaticObjectInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2SummonInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.ChairSit;
import net.sf.l2j.gameserver.network.serverpackets.RecipeShopManageList;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

/**
 * This class ...
 *
 * @version $Revision: 1.11.2.7.2.9 $ $Date: 2005/04/06 16:13:48 $
 */
public final class RequestActionUse extends L2GameClientPacket
{
	private static final String _C__45_REQUESTACTIONUSE = "[C] 45 RequestActionUse";
	private static Logger _log = Logger.getLogger(RequestActionUse.class.getName());
	
	private int _actionId;
	private boolean _ctrlPressed;
	private boolean _shiftPressed;
	
	@Override
	protected void readImpl()
	{
		_actionId = readD();
		_ctrlPressed = (readD() == 1);
		_shiftPressed = (readC() == 1);
	}
	
	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();
		
		if (activeChar == null)
			return;
		
		if (Config.DEBUG)
			_log.finest(activeChar.getName() + " request Action use: id " + _actionId + " 2:" + _ctrlPressed + " 3:" + _shiftPressed);
		
		// dont do anything if player is dead
		if (activeChar.isAlikeDead() || activeChar.isDead())
		{
			getClient().sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		// don't do anything if player is confused
		if (activeChar.isOutOfControl())
		{
			getClient().sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		// don't allow to do some action if player is transformed
		if (activeChar.isTransformed())
		{
			int[] notAllowedActions = {0, 10, 28, 37, 51, 61};
			if (Arrays.binarySearch(notAllowedActions,_actionId) >= 0)
			{
				getClient().sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
		}
		
		L2Summon pet = activeChar.getPet();
		L2Object target = activeChar.getTarget();
		
		if (Config.DEBUG)
			_log.info("Requested Action ID: " + String.valueOf(_actionId));
		
		switch (_actionId)
		{
			case 0:
				if (activeChar.getMountType() != 0)
					break;
				
				if (target != null && !activeChar.isSitting() && target instanceof L2StaticObjectInstance && ((L2StaticObjectInstance) target).getType() == 1 && CastleManager.getInstance().getCastle(target) != null
						&& activeChar.isInsideRadius(target, L2StaticObjectInstance.INTERACTION_DISTANCE, false, false))
				{
					ChairSit cs = new ChairSit(activeChar, ((L2StaticObjectInstance) target).getStaticObjectId());
					activeChar.sendPacket(cs);
					activeChar.sitDown();
					activeChar.broadcastPacket(cs);
					break;
				}
				
				if (activeChar.isSitting())
					activeChar.standUp();
				else
					activeChar.sitDown();
				
				if (Config.DEBUG)
					_log.fine("new wait type: " + (activeChar.isSitting() ? "SITTING" : "STANDING"));
				
				break;
			case 1:
				if (activeChar.isRunning())
					activeChar.setWalking();
				else
					activeChar.setRunning();
				
				if (Config.DEBUG)
					_log.fine("new move type: " + (activeChar.isRunning() ? "RUNNING" : "WALKIN"));
				break;
			case 10:
				// Private Store Sell
				activeChar.tryOpenPrivateSellStore(false);
				break;
			case 28:
				activeChar.tryOpenPrivateBuyStore();
				break;
			case 15:
			case 21: // pet follow/stop
				if (pet != null && !activeChar.isBetrayed())
					((L2SummonAI) pet.getAI()).notifyFollowStatusChange();
				break;
			case 16:
			case 22: // pet attack
				if (target != null && pet != null && pet != target && !pet.isAttackingDisabled() && !pet.isBetrayed())
				{
					if (pet instanceof L2PetInstance && (pet.getLevel() - activeChar.getLevel() > 20))
					{
						activeChar.sendPacket(new SystemMessage(SystemMessageId.PET_TOO_HIGH_TO_CONTROL));
						return;
					}
					
					if (activeChar.isInOlympiadMode() && !activeChar.isOlympiadStart())
					{
						// if L2PcInstance is in Olympia and the match isn't already start, send a Server->Client packet ActionFailed
						activeChar.sendPacket(ActionFailed.STATIC_PACKET);
						return;
					}
					
					if (!activeChar.getAccessLevel().allowPeaceAttack() && activeChar.isInsidePeaceZone(pet, target))
					{
						activeChar.sendPacket(new SystemMessage(SystemMessageId.TARGET_IN_PEACEZONE));
						return;
					}
					if (pet.getNpcId() == 12564 || pet.getNpcId() == 12621)
					{
						// sin eater and wyvern can't attack with attack button
						activeChar.sendPacket(ActionFailed.STATIC_PACKET);
						return;
					}
					if (target.isAutoAttackable(activeChar) || _ctrlPressed)
					{
						if (target instanceof L2DoorInstance)
						{
							if (((L2DoorInstance) target).isAttackable(activeChar) && pet.getNpcId() != L2SiegeSummonInstance.SWOOP_CANNON_ID)
								pet.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, target);
						}
						// siege golem AI doesn't support attacking other than doors at the moment
						else if (pet.getNpcId() != L2SiegeSummonInstance.SIEGE_GOLEM_ID)
							pet.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, target);
					}
					else
					{
						pet.setFollowStatus(false);
						pet.getAI().setIntention(CtrlIntention.AI_INTENTION_FOLLOW, target);
					}
				}
				break;
			case 17:
			case 23: // pet - cancel action
				if (pet != null && !pet.isMovementDisabled() && !activeChar.isBetrayed())
					pet.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE, null);
				break;
			case 19: // pet unsummon
				if (pet != null)
				{
					//returns pet to control item
					if (pet.isDead())
					{
						activeChar.sendPacket(new SystemMessage(SystemMessageId.DEAD_PET_CANNOT_BE_RETURNED));
					}
					else if (pet.isAttackingNow() || pet.isInCombat() || pet.isRooted() || pet.isBetrayed())
					{
						activeChar.sendPacket(new SystemMessage(SystemMessageId.PET_CANNOT_SENT_BACK_DURING_BATTLE));
					}
					else
					{
						// if it is a pet and not a summon
						if (pet instanceof L2PetInstance)
						{
							if (!pet.isHungry())
							{
								if (pet.isInCombat())
									activeChar.sendPacket(new SystemMessage(SystemMessageId.PET_CANNOT_SENT_BACK_DURING_BATTLE));
								else
									pet.unSummon(activeChar);
							}
							else
								activeChar.sendPacket(new SystemMessage(SystemMessageId.YOU_CANNOT_RESTORE_HUNGRY_PETS));
						}
					}
				}
				break;
			case 38: // pet mount
				// mount
				activeChar.mountPlayer(pet);
				break;
			case 32: // Wild Hog Cannon - Mode Change
				useSkill(4230);
				break;
			case 36: // Soulless - Toxic Smoke
				useSkill(4259);
				break;
			case 37:
				if (activeChar.isAlikeDead())
				{
					getClient().sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
				if (activeChar.getPrivateStoreType() != 0)
				{
					activeChar.setPrivateStoreType(L2PcInstance.STORE_PRIVATE_NONE);
					activeChar.broadcastUserInfo();
				}
				if (activeChar.isSitting())
					activeChar.standUp();
				
				if (activeChar.getCreateList() == null)
				{
					activeChar.setCreateList(new L2ManufactureList());
				}
				
				activeChar.sendPacket(new RecipeShopManageList(activeChar, true));
				break;
			case 39: // Soulless - Parasite Burst
				useSkill(4138);
				break;
			case 41: // Wild Hog Cannon - Attack
				useSkill(4230);
				break;
			case 42: // Kai the Cat - Self Damage Shield
				useSkill(4378, activeChar);
				break;
			case 43: // Unicorn Merrow - Hydro Screw
				useSkill(4137);
				break;
			case 44: // Big Boom - Boom Attack
				useSkill(4139);
				break;
			case 45: // Unicorn Boxer - Master Recharge
				useSkill(4025, activeChar);
				break;
			case 46: // Mew the Cat - Mega Storm Strike
				useSkill(4261);
				break;
			case 47: // Silhouette - Steal Blood
				useSkill(4260);
				break;
			case 48: // Mechanic Golem - Mech. Cannon
				useSkill(4068);
				break;
			case 51:
				// Player shouldn't be able to set stores if he/she is alike dead (dead or fake death)
				if (activeChar.isAlikeDead())
				{
					getClient().sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
				if (activeChar.getPrivateStoreType() != 0)
				{
					activeChar.setPrivateStoreType(L2PcInstance.STORE_PRIVATE_NONE);
					activeChar.broadcastUserInfo();
				}
				if (activeChar.isSitting())
					activeChar.standUp();
				
				if (activeChar.getCreateList() == null)
					activeChar.setCreateList(new L2ManufactureList());
				
				activeChar.sendPacket(new RecipeShopManageList(activeChar, false));
				break;
			case 52: // unsummon
				if (pet != null && pet instanceof L2SummonInstance)
				{
					if (pet.isBetrayed())
					{
						activeChar.sendPacket(new SystemMessage(SystemMessageId.PET_REFUSING_ORDER));
					}
					else if (pet.isAttackingNow() || pet.isInCombat())
						activeChar.sendPacket(new SystemMessage(SystemMessageId.PET_CANNOT_SENT_BACK_DURING_BATTLE));
					else
						pet.unSummon(activeChar);
				}
				break;
			case 53: // move to target
				if (target != null && pet != null && pet != target && !pet.isMovementDisabled() && !pet.isBetrayed())
				{
					pet.setFollowStatus(false);
					pet.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new L2CharPosition(target.getX(), target.getY(), target.getZ(), 0));
				}
				break;
			case 54: // move to target hatch/strider
				if (target != null && pet != null && pet != target && !pet.isMovementDisabled() && !pet.isBetrayed())
				{
					pet.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new L2CharPosition(target.getX(), target.getY(), target.getZ(), 0));
				}
				break;
			case 61:
				// Private Store Package Sell
				activeChar.tryOpenPrivateSellStore(true);
				break;
			case 65:
				// Bot report Button.
				activeChar.sendMessage("Action not handled yet.");
				break;
			case 96: // Quit Party Command Channel
				_log.info("98 Accessed");
				break;
			case 97: // Request Party Command Channel Info
				//if (!PartyCommandManager.getInstance().isPlayerInChannel(activeChar))
				//return;
				_log.info("97 Accessed");
				//PartyCommandManager.getInstance().getActiveChannelInfo(activeChar);
				break;
			case 1000: // Siege Golem - Siege Hammer
				if (target instanceof L2DoorInstance)
					useSkill(4079);
				break;
			case 1001:
				break;
			case 1003: // Wind Hatchling/Strider - Wild Stun
				useSkill(4710);
				break;
			case 1004: // Wind Hatchling/Strider - Wild Defense
				useSkill(4711, activeChar);
				break;
			case 1005: // Star Hatchling/Strider - Bright Burst
				useSkill(4712); 
				break;
			case 1006: // Star Hatchling/Strider - Bright Heal
				useSkill(4713, activeChar);
				break;
			case 1007: // Cat Queen - Blessing of Queen
				useSkill(4699, activeChar);
				break;
			case 1008: // Cat Queen - Gift of Queen
				useSkill(4700, activeChar);
				break;
			case 1009: // Cat Queen - Cure of Queen
				useSkill(4701);
				break;
			case 1010: // Unicorn Seraphim - Blessing of Seraphim
				useSkill(4702, activeChar);
				break;
			case 1011: // Unicorn Seraphim - Gift of Seraphim
				useSkill(4703, activeChar);
				break;
			case 1012: // Unicorn Seraphim - Cure of Seraphim
				useSkill(4704);
				break;
			case 1013: // Nightshade - Curse of Shade
				useSkill(4705);
				break;
			case 1014: // Nightshade - Mass Curse of Shade
				useSkill(4706, activeChar);
				break;
			case 1015: // Nightshade - Shade Sacrifice
				useSkill(4707);
				break;
			case 1016: // Cursed Man - Cursed Blow
				useSkill(4709);
				break;
			case 1017: // Cursed Man - Cursed Strike/Stun
				useSkill(4708);
				break;
			case 1031: // Feline King - Slash
				useSkill(5135);
				break;
			case 1032: // Feline King - Spinning Slash
				useSkill(5136);
				break;
			case 1033: // Feline King - Grip of the Cat
				useSkill(5137);
				break;
			case 1034: // Magnus the Unicorn - Whiplash
				useSkill(5138);
				break;
			case 1035: // Magnus the Unicorn - Tridal Wave
				useSkill(5139);
				break;
			case 1036: // Spectral Lord - Corpse Kaboom
				useSkill(5142);
				break;
			case 1037: // Spectral Lord - Dicing Death
				useSkill(5141);
				break;
			case 1038: // Spectral Lord - Force Curse
				useSkill(5140);
				break;
			case 1039: // Swoop Cannon - Cannon Fodder
				if (!(target instanceof L2DoorInstance))
					useSkill(5110);
				break;
			case 1040: // Swoop Cannon - Big Bang
				if (!(target instanceof L2DoorInstance))
					useSkill(5111);
				break;
			case 1041: // Great Wolf - Bite Attack
				useSkill(5442);
				break;
			case 1042: // Great Wolf - Maul
				useSkill(5444);
				break;
			case 1043: // Great Wolf - Cry of the Wolf
				useSkill(5443);
				break;
			case 1044: // Great Wolf - Awakening
				useSkill(5445);
				break;
			case 1045: // Great Wolf - Howl
				useSkill(5584);
				break;
			case 1047: // Divine Beast - Bite
				useSkill(5580);
				break;
			case 1048: // Divine Beast - Stun Attack
				useSkill(5581);
				break;
			case 1049: // Divine Beast - Fire Breath
				useSkill(5582);
				break;
			case 1050: // Divine Beast - Roar
				useSkill(5583);
				break;
			case 1051: //Feline Queen - Bless The Body
				useSkill(5638);
				break;
			case 1052: //Feline Queen - Bless The Soul
				useSkill(5639);
				break;
			case 1053: //Feline Queen - Haste
				useSkill(5640);
				break;
			case 1054: //Unicorn Seraphim - Acumen
				useSkill(5643);
				break;
			case 1055: //Unicorn Seraphim - Clarity
				useSkill(5647);
				break;
			case 1056: //Unicorn Seraphim - Empower
				useSkill(5648);
				break;
			case 1057: //Unicorn Seraphim - Wild Magic
				useSkill(5646);
				break;
			case 1058: //Nightshade - Death Whisper
				useSkill(5652);
				break;
			case 1059: //Nightshade - Focus
				useSkill(5653);
				break;
			case 1060: //Nightshade - Guidance
				useSkill(5654);
				break;
			default:
				_log.warning(activeChar.getName() + ": unhandled action type " + _actionId);
		}
	}
	
	/*
	 * Cast a skill for active pet/servitor.
	 * Target is specified as a parameter but can be
	 * overwrited or ignored depending on skill type.
	 */
	private void useSkill(int skillId, L2Object target)
	{
		L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
			return;
		
		L2Summon activeSummon = activeChar.getPet();
		
		if (activeChar.getPrivateStoreType() != 0)
		{
			activeChar.sendMessage("Cannot use skills while trading");
			return;
		}
		
		if (activeSummon != null && !activeSummon.isBetrayed())
		{
			if (activeSummon instanceof L2PetInstance && (activeSummon.getLevel() - activeChar.getLevel() > 20))
			{
				activeChar.sendPacket(new SystemMessage(SystemMessageId.PET_TOO_HIGH_TO_CONTROL));
				return;
			}
			int lvl = PetSkillsTable.getInstance().getAvailableLevel(activeSummon, skillId);
			if (lvl == 0)
				return;
			
			L2Skill skill = SkillTable.getInstance().getInfo(skillId, lvl);
			if (skill == null)
				return;
			
			activeSummon.setTarget(target);
			activeSummon.useMagic(skill, _ctrlPressed, _shiftPressed);
		}
	}
	
	/*
	 * Cast a skill for active pet/servitor.
	 * Target is retrieved from owner' target,
	 * then validated by overloaded method useSkill(int, L2Character).
	 */
	private void useSkill(int skillId)
	{
		L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
			return;
		
		useSkill(skillId, activeChar.getTarget());
	}
	
	@Override
	public String getType()
	{
		return _C__45_REQUESTACTIONUSE;
	}
}
