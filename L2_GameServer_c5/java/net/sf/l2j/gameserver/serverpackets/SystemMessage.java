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
package net.sf.l2j.gameserver.serverpackets;

import java.util.Vector;

/**
 * This class ...
 * 
 * @version $Revision: 1.18.2.5.2.8 $ $Date: 2005/04/05 19:41:08 $
 */
public class SystemMessage extends ServerBasePacket
{
	// d d (d S/d d/d dd)
	//      |--------------> 0 - String  1-number 2-textref npcname (1000000-1002655)  3-textref itemname 4-textref skills 5-??	
	private static final int TYPE_SKILL_NAME = 4;
	private static final int TYPE_ITEM_NAME = 3;
	private static final int TYPE_NPC_NAME = 2;
	private static final int TYPE_NUMBER = 1;
	private static final int TYPE_TEXT = 0;
	private static final String _S__7A_SYSTEMMESSAGE = "[S] 64 SystemMessage";
	private int _messageId;
	private Vector<Integer> _types = new Vector<Integer>();
	private Vector<Object> _values = new Vector<Object>();
	
	//PeaceZones
	public static final int TARGET_IN_PEACEZONE = 85;
	public static final int CANT_ATK_PEACEZONE = 84;
	
	// magic related
	public static final int NOT_ENOUGH_HP = 23;
    public static final int NOT_ENOUGH_MP = 24;
    public static final int NOT_ENOUGH_ITEMS = 351;
    
	public static final int USE_S1 = 46;
	public static final int S1_PREPARED_FOR_REUSE = 48;
	public static final int YOU_FEEL_S1_EFFECT = 110;
	public static final int REJUVENATING_HP = 25;
    public static final int REJUVENATING_MP = 26;
	public static final int CANNOT_USE_ITEM_WHILE_USING_MAGIC = 104;
	public static final int CASTING_INTERRUPTED = 27;
    public static final int S1_WAS_UNAFFECTED_BY_S2 = 139;
    public static final int DRAIN_HALF_SUCCESFUL = 156;
    public static final int RESISTED_S1_DRAIN = 157;
    public static final int ATTACK_FAILED = 158;
    public static final int RESISTED_S1_MAGIC = 159;
    public static final int S1_HP_RESTORED = 1066;
    public static final int S2_HP_RESTORED_BY_S1 = 1067;
    public static final int S1_MP_RESTORED = 1068;
    public static final int S2_MP_RESTORED_BY_S1 = 1069;

    public static final int EARNED_S2_S1_s = 53;
    public static final int EARNED_ITEM = 54;
	public static final int EARNED_ADENA = 52;
	public static final int DISSAPEARED_ITEM = 301;
	public static final int DISSAPEARED_ADENA = 672;

	
	public static final int YOU_DID_S1_DMG = 0x23;
	public static final int S1_GAVE_YOU_S2_DMG = 0x24;
	public static final int EFFECT_S1_DISAPPEARED = 0x5c;
	public static final int YOU_EARNED_S1_EXP_AND_S2_SP = 0x5f;
	public static final int YOU_INCREASED_YOUR_LEVEL = 0x60;
	
	public static final int NOTHING_HAPPENED = 61;
	public static final int ITEM_MISSING_TO_LEARN_SKILL = 0x114;
	public static final int LEARNED_SKILL_S1 = 0x115;
	public static final int NOT_ENOUGH_SP_TO_LEARN_SKILL = 0x116;
    public static final int DO_NOT_HAVE_FURTHER_SKILLS_TO_LEARN = 607;
	
	public static final int FALL_DAMAGE_S1 = 0x128;
	public static final int DROWN_DAMAGE_S1 = 0x129;

	public static final int YOU_DROPPED_S1 = 0x12a;
	public static final int TARGET_IS_NOT_FOUND_IN_THE_GAME = 0x91;
	public static final int TARGET_CANT_FOUND = 50;
	public static final int TARGET_MUST_BE_IN_CLAN = 234;

	public static final int CANNOT_BE_RESURRECTED_DURING_SIEGE = 1053;
	
	public static final int SOULSHOTS_GRADE_MISMATCH = 0x151;
	public static final int NOT_ENOUGH_SOULSHOTS = 0x152;
    public static final int ENABLED_SOULSHOT = 342;
	public static final int CANNOT_USE_SOULSHOTS = 0x153;
	public static final int SPIRITSHOTS_GRADE_MISMATCH = 530;
	public static final int NOT_ENOUGH_SPIRITSHOTS = 531;
	public static final int CANNOT_USE_SPIRITSHOTS = 532;
    public static final int ENABLED_SPIRITSHOT = 533;
    public static final int USE_OF_S1_WILL_BE_AUTO = 1433;
	public static final int AUTO_USE_OF_S1_CANCELLED = 1434;
	
	public static final int S1_IS_NOT_ONLINE = 3;
	
	public static final int GETTING_READY_TO_SHOOT_AN_ARROW    =   41;
	public static final int AVOIDED_S1s_ATTACK                 =   42;
	public static final int MISSED_TARGET                      =   43;
	public static final int CRITICAL_HIT                       =   44;
	public static final int CRITICAL_HIT_BY_PET                = 1017;
	public static final int CRITICAL_HIT_BY_SUMMONED_MOB       = 1028;
	public static final int SHIELD_DEFENCE_SUCCESSFULL         =  111;
	public static final int OVER_HIT                           =  361;
	public static final int SPOIL_SUCCESS                      =  612;
	public static final int SPOIL_CANNOT_USE                   =  661;
    public static final int SWEEPER_FAILED_TARGET_NOT_SPOILED  =  343;
    public static final int S1_SWEEPED_UP_S2_S3 = 608;
    public static final int S1_SWEEPED_UP_S2 = 609;
    public static final int ALREDAY_SPOILED = 357;
    public static final int CANT_MOVE_SITTING = 31;
    public static final int SYMBOL_ADDED = 877;
    public static final int CANT_DRAW_SYMBOL = 899;
    
    public static final int SKILL_NOT_AVAILABLE             = 0x30;
    
    public static final int CANT_SEE_TARGET                 = 0xb5;
	
	// party related 
	public static final int WAITING_FOR_REPLY = 164;
	
	public static final int S1_INVITED_YOU_TO_PARTY_FINDER_KEEPER = 572;
	public static final int S1_INVITED_YOU_TO_PARTY_RANDOM = 573;
	
	public static final int S1_INVITED_YOU_TO_PARTY = 66;
	public static final int YOU_INVITED_S1_TO_PARTY = 105;
	
	public static final int OTHER_PARTY_IS_DROZEN = 692;
	
	public static final int YOU_JOINED_S1_PARTY = 106;
	public static final int S1_JOINED_PARTY = 107;
	public static final int S1_LEFT_PARTY = 108;
	public static final int YOU_LEFT_PARTY = 200;
	public static final int ONLY_LEADER_CAN_INVITE = 154;

	public static final int S1_DID_NOT_REPLY = 135;
	public static final int YOU_DID_NOT_REPLY = 136;
	
	public static final int PLAYER_DECLINED = 305;
	
	public static final int PARTY_FULL = 155;
	public static final int S1_IS_ALREADY_IN_PARTY = 160;
	public static final int INVITED_USER_NOT_ONLINE = 161;

	public static final int PARTY_DISPERSED = 203;
	
	public static final int S1_HAS_BECOME_A_PARTY_LEADER = 1384;
	public static final int ONLY_A_PARTY_LEADER_CAN_TRANSFER_ONES_RIGHTS_TO_ANOTHER_PLAYER = 0x577;
	public static final int PLEASE_SELECT_THE_PERSON_TO_WHOM_YOU_WOULD_LIKE_TO_TRANSFER_THE_RIGHTS_OF_A_PARTY_LEADER = 0x0578;
	public static final int YOU_CANNOT_TRANSFER_RIGHTS_TO_YOURSELF = 0x0579;
	public static final int YOU_CAN_TRANSFER_RIGHTS_ONLY_TO_ANOTHER_PARTY_MEMBER = 0x057A;
	public static final int YOU_HAVE_FAILED_TO_TRANSFER_THE_PARTY_LEADER_RIGHTS = 0x057B;

	//weight & inventory
	public static final int WEIGHT_LIMIT_EXCEEDED = 422;
	public static final int NOT_ENOUGH_ARROWS = 112;
    public static final int SLOTS_FULL = 129;
    public static final int CANNOT_DISCARD_EXCHANGE_ITEM = 603;
    public static final int CANNOT_DISCARD_DISTANCE_TOO_FAR = 151;
    public static final int CANNOT_DISCARD_THIS_ITEM = 98;

	// trade 
	public static final int S1_IS_BUSY_TRY_LATER = 153;
	public static final int TARGET_IS_INCORRECT = 144;
	public static final int ALREADY_TRADING = 142;
	public static final int REQUEST_S1_FOR_TRADE = 118;
	public static final int S1_DENIED_TRADE_REQUEST = 119;
	public static final int BEGIN_TRADE_WITH_S1 = 120;
	public static final int S1_CONFIRMED_TRADE = 121;
	public static final int TRADE_SUCCESSFUL = 123;
	public static final int S1_CANCELED_TRADE = 124;
    public static final int CANNOT_USE_ITEM_WHILE_TRADING = 149;
    
	//private store & store
	public static final int S1_PURCHASED_S2 = 378;
	public static final int S1_PURCHASED_S2_S3 = 379;
	public static final int S1_PURCHASED_S3_S2_s = 380;
	public static final int PURCHASED_S2_FROM_S1 = 559;
	public static final int PURCHASED_S2_S3_FROM_S1 =560;
	public static final int	PURCHASED_S3_S2_s_FROM_S1 =561;
	public static final int	THE_PURCHASE_IS_COMPLETE =700;
	public static final int	THE_PURCHASE_PRICE_IS_HIGHER_THAN_MONEY = 720;
	public static final int YOU_NOT_ENOUGH_ADENA = 279;
    public static final int CANNOT_TRADE_DISCARD_DROP_ITEM_WHILE_IN_SHOPMODE = 1065;
    public static final int YOU_HAVE_EXCEEDED_QUANTITY_THAT_CAN_BE_INPUTTED = 1036;
    
    //Community Board
    public static final int NO_CB_IN_MY_CLAN = 1050;
    public static final int CB_OFFLINE = 938;
    
	//Clan stuff
	public static final int FAILED_TO_CREATE_CLAN = 190;
	public static final int CLAN_NAME_INCORRECT = 261;
	public static final int CLAN_NAME_TOO_LONG = 262;
	
	public static final int CLAN_CREATED = 189;
	public static final int CLAN_LVL_3_NEEDED_TO_ENDOWE_TITLE = 271;
	public static final int CLAN_LVL_3_NEEDED_TO_SET_CREST = 272;
	
	public static final int CANNOT_INVITE_YOURSELF = 4;
	public static final int S1_WORKING_WITH_ANOTHER_CLAN = 10;
	public static final int ENTERED_THE_CLAN = 195;
	public static final int S1_REFUSED_TO_JOIN_CLAN = 196;
	public static final int S1_HAS_JOINED_CLAN = 222;
	public static final int CLAN_MEMBER_S1_LOGGED_IN = 304;
	
	public static final int CLAN_LEVEL_INCREASED = 274;
	public static final int FAILED_TO_INCREASE_CLAN_LEVEL = 275;
	
	public static final int CLAN_MEMBER_S1_EXPELLED = 191;
	public static final int CLAN_MEMBERSHIP_TERMINATED = 199;
    
	public static final int CLAN_EMBLEM_WAS_SUCCESSFULLY_REGISTERED = 1663;
	
    public static final int YOU_DO_NOT_HAVE_THE_RIGHT_TO_USE_CLAN_WAREHOUSE = 709;
    public static final int ONLY_LEVEL_1_CLAN_OR_HIGHER_CAN_USE_WAREHOUSE = 710;
    public static final int ONLY_CLAN_LEADER_CAN_RETRIEVE_ITEMS_FROM_CLAN_WAREHOUSE = 1039;
    public static final int TITLE_CHANGED = 214;
    
    public static final int CLAN_INFO_HEAD = 496;
    public static final int CLAN_INFO_NAME = 497;
    public static final int CLAN_INFO_LEADER = 498;
    public static final int CLAN_INFO_LEVEL = 499;
    public static final int CLAN_INFO_SEPARATOR = 500;
    public static final int CLAN_INFO_FOOT = 501;
	
	//pickup items
	public static final int YOU_PICKED_UP_S1_ADENA = 28;
	public static final int YOU_PICKED_UP_S1_S2 = 29;
	public static final int YOU_PICKED_UP_S1 = 30;	
	public static final int S1_PICKED_UP_S2_S3 = 299;
	public static final int S1_PICKED_UP_S2 = 300;
	
	//GM LIST
	public static final int GM_LIST = 703;
	public static final int GM_S1 = 704;
	public static final int THE_PERSON_IS_IN_MESSAGE_REFUSAL_MODE = 176;
	public static final int MESSAGE_REFUSAL_MODE = 177;
	public static final int MESSAGE_ACCEPTANCE_MODE = 178;
    
    // loc messages
    public static final int LOC_TI_S1_S2_S3 = 910;
    public static final int LOC_GLUDIN_S1_S2_S3 = 911;
    public static final int LOC_GLUDIO_S1_S2_S3 = 912;
    public static final int LOC_NETRAL_ZONE_S1_S2_S3 = 913;
    public static final int LOC_ELVEN_S1_S2_S3 = 914;
    public static final int LOC_DARK_ELVEN_S1_S2_S3 = 915;
    public static final int LOC_DION_S1_S2_S3 = 916;
    public static final int LOC_FLORAN_S1_S2_S3 = 917;
    public static final int LOC_GIRAN_S1_S2_S3 = 918;
    public static final int LOC_GIRAN_HARBOR_S1_S2_S3 = 919;
    public static final int LOC_ORC_S1_S2_S3 = 920;
    public static final int LOC_DWARVEN_S1_S2_S3 = 921;
    public static final int LOC_OREN_S1_S2_S3 = 922;
    public static final int LOC_HUNTER_S1_S2_S3 = 923;
    public static final int LOC_ADEN_S1_S2_S3 = 924;
    public static final int LOC_COLISEUM_S1_S2_S3 = 925;
    public static final int LOC_HEINE_S1_S2_S3 = 926;
    public static final int LOC_RUNE_S1_S2_S3 = 1537;
    public static final int LOC_GODDARD_S1_S2_S3 = 1538;    
	
	//Crystallize
	public static final int CRYSTALLIZE_LEVEL_TOO_LOW = 562;
	public static final int S1_CANNOT_BE_USED = 113;
    
    //Monster Derby
    public static final int ACQUIRED = 371;
    public static final int MONSRACE_NO_PAYOUT_INFO = 1044;
    public static final int MONSRACE_TICKETS_NOT_AVAILABLE = 1046;
    public static final int MONSRACE_TICKETS_AVAILABLE_FOR_S1_RACE = 816;
    public static final int MONSRACE_TICKETS_NOW_AVAILABLE_FOR_S1_RACE = 817;
    public static final int MONSRACE_TICKETS_STOP_IN_S1_MINUTES = 818;
    public static final int MONSRACE_TICKET_SALES_CLOSED = 819;
    public static final int MONSRACE_BEGINS_IN_S1_MINUTES = 820;
    public static final int MONSRACE_BEGINS_IN_30_SECONDS = 821;
    public static final int MONSRACE_COUNTDOWN_IN_FIVE_SECONDS = 822;
    public static final int MONSRACE_BEGINS_IN_S1_SECONDS = 823;
    public static final int MONSRACE_RACE_START = 824;
    public static final int MONSRACE_RACE_END = 825;
    public static final int MONSRACE_FIRST_PLACE_S1_SECOND_S2 = 826;

    // Naming conventions
	public static final int NAMING_THERE_IS_A_SPACE = 581;		
	public static final int NAMING_INAPPROPRIATE_CHARACTER_NAME = 582;		
	public static final int NAMING_INCLUDES_FORBIDDEN_WORDS = 583;		
	public static final int NAMING_ALREADY_IN_USE_BY_ANOTHER_PET = 584;
	public static final int NAMING_PETNAME_UP_TO_8CHARS = 548; 
	public static final int NAMING_NAME_PET = 535;
	public static final int NAMING_YOU_CANNOT_SET_NAME_OF_THE_PET = 695;
	public static final int NAMING_NAME_ALREADY_EXISTS = 79;	
	public static final int NAMING_CHARNAME_UP_TO_16CHARS = 80;	
	public static final int NAMING_PETNAME_CONTAINS_INVALID_CHARS = 591;		
    
	//Pets
	public static final int ITEM_NOT_FOR_PETS = 544;
    public static final int DEAD_PET_CANNOT_BE_RETURNED = 589;
	public static final int CANNOT_GIVE_ITEMS_TO_DEAD_PET = 590;
	public static final int CANNOT_EQUIP_PET_ITEM = 600;
    public static final int PET_EARNED_S1_EXP = 1014;
    public static final int PET_RECEIVED_DAMAGE_OF_S2_BY_S1 = 1016;
    public static final int PET_CRITICAL_HIT = 1017;
	public static final int PET_DID_S1_DMG = 1015;
    public static final int S1_GAME_PET_S2_DMG = 1016;
    public static final int PET_CANNOT_USE_ITEM = 972;
    public static final int PET_TOOK_S1_BECAUSE_HE_WAS_HUNGRY = 1527;
    public static final int YOU_CANNOT_RESTORE_HUNGRY_PETS = 594;
    public static final int PET_CANNOT_SENT_BACK_DURING_BATTLE = 579;
    public static final int YOU_ALREADY_HAVE_A_PET = 543;
    public static final int YOU_CANNOT_SUMMON_IN_COMBAT = 578;
    
    public static final int STRIDER_CANT_BE_RIDDEN_WHILE_DEAD = 1009;
    public static final int DEAD_STRIDER_CANT_BE_RIDDEN = 1010;
    public static final int STRIDER_IN_BATLLE_CANT_BE_RIDDEN = 1011;
    public static final int STRIDER_CANT_BE_RIDDEN_WHILE_IN_BATTLE = 1012;
    public static final int STRIDER_CAN_BE_RIDDEN_ONLY_WHILE_STANDING = 1013;
	
    //Summons
    public static final int SUMMON_GAVE_DAMAGE_OF_S1 = 1026;
    public static final int SUMMON_RECEIVED_DAMAGE_OF_S2_BY_S1 = 1027;
    public static final int SUMMON_CRITICAL_HIT = 1028;
    public static final int SUMMON_A_PET = 547;
    public static final int CUBIC_SUMMONING_FAILED = 568;
    public static final int SUMMONING_SERVITOR_COSTS_S2_S1 = 1197;
    
	// enchants
	public static final int S1_SUCCESSFULLY_ENCHANTED = 62;
	public static final int S1_S2_SUCCESSFULLY_ENCHANTED = 63;
	public static final int ENCHANTMENT_FAILED_S1_EVAPORATED = 64;
	public static final int ENCHANTMENT_FAILED_S1_S2_EVAPORATED = 65;
	public static final int SELECT_ITEM_TO_ENCHANT = 303;
	public static final int INAPPROPRIATE_ENCHANT_CONDITION = 355;
	public static final int BLESSED_ENCHANT_FAILED = 1517;
	public static final int ENCHANT_SCROLL_CANCELLED = 423;
	
	// Equip related
	public static final int EQUIPMENT_S1_S2_REMOVED = 1064;
	public static final int S1_DISARMED = 417;
	public static final int S1_S2_EQUIPPED = 368;
	public static final int S1_EQUIPPED = 49;


    // SevenSign
	public static final int QUEST_EVENT_PERIOD = 1176;
	public static final int VALIDATION_PERIOD = 1177;
	// public static final int AVARICE_DESCRIPTION = 1178;
	// public static final int GNOSIS_DESCRIPTION = 1179;
	// public static final int STRIFE_DESCRIPTION = 1180;
	public static final int INITIAL_PERIOD = 1183;
	public static final int RESULTS_PERIOD = 1184;
    public static final int QUEST_EVENT_PERIOD_BEGUN = 1210;
    public static final int QUEST_EVENT_PERIOD_ENDED = 1211;
    public static final int DAWN_OBTAINED_AVARICE = 1212;
    public static final int DAWN_OBTAINED_GNOSIS = 1213;
    public static final int DAWN_OBTAINED_STRIFE = 1214;
    public static final int DUSK_OBTAINED_AVARICE = 1215;
    public static final int DUSK_OBTAINED_GNOSIS = 1216;
    public static final int DUSK_OBTAINED_STRIFE = 1217;
    public static final int SEAL_VALIDATION_PERIOD_BEGUN = 1218;
    public static final int SEAL_VALIDATION_PERIOD_ENDED = 1219;
    public static final int DAWN_WON = 1241;
    public static final int DUSK_WON = 1240;
    public static final int PREPARATIONS_PERIOD_BEGUN = 1260;
    public static final int COMPETITION_PERIOD_BEGUN = 1261;
    public static final int RESULTS_PERIOD_BEGUN = 1262;
    public static final int VALIDATION_PERIOD_BEGUN = 1263;
    public static final int CONTRIB_SCORE_INCREASED = 1267;
    public static final int SEVENSIGNS_PARTECIPATION_DAWN = 1273;
    public static final int SEVENSIGNS_PARTECIPATION_DUSK = 1274;
    public static final int FIGHT_FOR_AVARICE = 1275;
    public static final int FIGHT_FOR_GNOSIS = 1276;
    public static final int FIGHT_FOR_STRIFE = 1277;
    public static final int CONTRIB_SCORE_EXCEEDED = 1279;
    public static final int UNTIL_MONDAY_6PM = 1286;
    public static final int UNTIL_TODAY_6PM = 1287;
    // public static final int S1_WILL_WIN_COMPETITION = 1288;
    public static final int SEAL_OWNED_10_MORE_VOTED = 1289;
    public static final int SEAL_NOT_OWNED_35_MORE_VOTED = 1290;
    public static final int SEAL_OWNED_10_LESS_VOTED = 1291;
    public static final int SEAL_NOT_OWNED_35_LESS_VOTED = 1292;
    // public static final int COMPETITION_WILL_TIE = 1293;
    public static final int COMPETITION_TIE_SEAL_NOT_AWARDED = 1294;
    public static final int CAN_BE_USED_BY_DAWN = 1301;
    public static final int CAN_BE_USED_BY_DUSK = 1302;
    
    
    //wars
    public static final int WAR_WITH_THE_S1_CLAN_HAS_BEGUN = 215;
    public static final int WAR_WITH_THE_S1_CLAN_HAS_ENDED = 216;
    public static final int YOU_HAVE_WON_THE_WAR_OVER_THE_S1_CLAN = 217;
    public static final int YOU_HAVE_SURRENDERED_TO_THE_S1_CLAN = 218;
    public static final int REQUEST_TO_END_WAR_HAS_BEEN_DENIED = 228;
    public static final int YOU_HAVE_PERSONALLY_SURRENDERED_TO_THE_S1_CLAN = 250;
    public static final int WAR_PROCLAMATION_HAS_BEEN_REFUSED = 626;
    
    //alliance
    public static final int FEATURE_ONLY_FOR_ALLIANCE_LEADER = 464;
    public static final int NO_CURRENT_ALLIANCES = 465;
    public static final int CANT_ENTER_ALLIANCE_WITHIN_1_DAY = 468;
    public static final int MAY_NOT_ALLY_CLAN_BATTLE = 469;
    public static final int ONLY_CLAN_LEADER_WITHDRAW_ALLY = 470;
    public static final int ALLIANCE_LEADER_CANT_WITHDRW= 471;
    public static final int DIFFERANT_ALLIANCE = 473;
    public static final int CLAN_DOESNT_EXISTS = 474;
    public static final int NO_RESPONSE_INVIT_CANCEL = 477;
    public static final int NO_RESPONSE_ENTRANCE_CANCEL = 478;
    public static final int ALREADY_JOINED_ALLIANCE = 502;
    public static final int ONLY_CLAN_LEADER_CREATE_ALLIANCE = 504;
    public static final int CANT_CREATE_ALLIANCE_10_DAYS_DISOLUTION = 505;
    public static final int INCORRECT_ALLIANCE_NAME = 506;
    public static final int INCORRECT_ALLIANCE_NAME_LENGTH = 507;
    public static final int ALLIANCE_ALREADY_EXISTS = 508;
    public static final int CANT_ACCEPT_ALLY_ENEMY_FOR_SIEGE = 509;
    public static final int YOU_INVITED_FOR_ALLIANCE = 510;
    public static final int SELECT_USER_TO_INVITE = 511;
    public static final int DO_YOU_WISH_TO_WITHDRW = 512;
	public static final int ENTER_NAME_CLAN_TO_EXPEL = 513;
	public static final int DO_YOU_WISH_TO_DISOLVE = 514;
    public static final int YOU_ACCEPTED_ALLIANCE = 517;
    public static final int FAILED_TO_INVITE_CLAN_IN_ALLIANCE = 518;
    public static final int YOU_HAVE_WITHDRAWN_FROM_ALLIANCE = 519;
    public static final int YOU_HAVE_FAILED_TO_WITHDRAWN_FROM_ALLIANCE = 520;
    public static final int YOU_HAVE_EXPELED_A_CLAN = 521;
    public static final int FAILED_TO_EXPELED_A_CLAN = 522;
    public static final int ALLIANCE_DISOLVED = 523;
    public static final int FAILED_TO_DISOLVE_ALLIANCE = 524;
    public static final int S2_ALLIANCE_LEADER_OF_S1_REQUESTED_ALLIANCE = 527;
    public static final int S1_CLAN_ALREADY_MEMBER_OF_S2_ALLIANCE = 691;
    
    public static final int ALLIANCE_INFO_HEAD = 491;
    public static final int ALLIANCE_NAME_S1 = 492;
    public static final int CONNECTION_S1_TOTAL_S2 = 493;
    public static final int ALLIANCE_LEADER_S2_OF_S1 = 494;
    public static final int ALLIANCE_CLAN_TOTAL_S1 = 495;
    
    //Friend
    public static final int SI_INVITED_YOU_AS_FRIEND = 516;
    public static final int INVITED_A_FRIEND = 525;
    public static final int FAILED_TO_INVITE_A_FRIEND = 526;
    public static final int S1_ADDED_TO_FRIENDS = 132;
    public static final int S1_JOINED_AS_FRIEND = 479;
    public static final int FRIEND_S1_HAS_LOGGED_IN = 503;
    
    public static final int FAILED_TO_REGISTER_TO_IGNORE_LIST = 615;
    public static final int S1_WAS_ADDED_TO_YOUR_IGNORE_LIST = 617;
    public static final int S1_WAS_REMOVED_FROM_YOUR_IGNORE_LIST = 618;
    public static final int S1_HAS_ADDED_YOU_TO_IGNORE_LIST = 619;
    
    public static final int FRIEND_LIST_HEAD = 487;
    public static final int S1_ONLINE = 488;
    public static final int S1_OFFLINE = 489;
    public static final int FRIEND_LIST_FOOT = 490;
    
    //
    public static final int S1_S2 = 614;
    public static final int CANT_LOGOUT_WHILE_FIGHTING = 0x65;
    public static final int CANT_RESTART_WHILE_FIGHTING = 0x66;
	public static final int ENTER_FILE_NAME_CREST = 515;
    public static final int FILE_NOT_FOUND = 528;
    public static final int S1_ROLLED_S2 = 814;
    public static final int NO_GM_PROVIDING_SERVICE_NOW = 702;
    public static final int TARGET_TOO_FAR = 22;
    public static final int S1_DISAPPEARED = 302;
    public static final int INCORRECT_TARGET = 109;
    
    public static final int S1_DIED_DROPPED_S3_S2 = 1208;//$s1 died and dropped $s3 $s2.
    
    //manor
    public static final int SEED_SUCCESSFULLY_SOWN = 889;
    public static final int SEED_NOT_SOWN = 890;
    public static final int SEED_CANNOT_BE_SOWN_HERE = 882;
    
    //Recommedations
    public static final int YOU_CANNOT_RECOMMEND_YOURSELF = 829;
    public static final int YOU_HAVE_BEEN_RECOMMENDED = 831;
    public static final int YOU_HAVE_RECOMMENDED = 830;
    public static final int THAT_CHARACTER_IS_RECOMMENDED = 832;
    public static final int NO_MORE_RECOMMENDATIONS_TO_HAVE = 833;
    public static final int ONLY_LEVEL_SUP_10_CAN_RECOMMEND = 898;
    public static final int YOU_NO_LONGER_RECIVE_A_RECOMMENDATION = 1188;

    public static final int NPC_SERVER_NOT_OPERATING = 1278;
    
    //Craft and Recipes Related 
 	public static final int RECIPE_ALREADY_REGISTERED = 840; 
 	public static final int NO_FUTHER_RECIPES_CAN_BE_ADDED = 841; 
 	public static final int NOT_AUTHORIZED_TO_REGISTER_RECIPE = 642; 
 	public static final int CANT_ALTER_RECIPEBOOK_WHILE_CRAFTING = 853; 
 	public static final int CANT_REGISTER_NO_ABILITY_TO_CRAFT = 1061; 
    
    // enter/exit mother tree 
 	public static final int ENTER_SHADOW_MOTHER_TREE = 0x72; 
 	public static final int EXIT_SHADOW_MOTHER_TREE = 0x73; 

    // Formal Wear
    public static final int CANNOT_USE_ITEMS_SKILLS_WITH_FORMALWEAR = 1604;
    
    // Olympiad
    public static final int YOU_WILL_ENTER_THE_OLYMPIAD_STADIUM_IN_S1_SECOND_S = 1492;
    public static final int THE_GAME_HAS_BEEN_CANCELLED_BECAUSE_THE_OTHER_PARTY_ENDS_THE_GAME = 1493;
    public static final int THE_GAME_HAS_BEEN_CANCELLED_BECAUSE_THE_OTHER_PARTY_DOES_NOT_MEET_THE_REQUIREMENTS_FOR_JOINING_THE_GAME = 1494;
    public static final int THE_GAME_WILL_START_IN_S1_SECOND_S = 1495;
    public static final int STARTS_THE_GAME = 1496;
    public static final int S1_HAS_WON_THE_GAME = 1497;
    public static final int THE_GAME_ENDED_IN_A_TIE = 1498;
    public static final int YOU_WILL_GO_BACK_TO_THE_VILLAGE_IN_S1_SECOND_S = 1499;
    public static final int YOU_CANT_JOIN_THE_OLYMPIAD_WITH_A_SUB_JOB_CHARACTER = 1500;
    public static final int ONLY_NOBLESS_CAN_PARTICIPATE_IN_THE_OLYMPIAD = 1501;
    public static final int YOU_HAVE_ALREADY_BEEN_REGISTERED_IN_A_WAITING_LIST_OF_AN_EVENT = 1502;
    public static final int YOU_HAVE_BEEN_REGISTERED_IN_A_WAITING_LIST_OF_CLASSIFIED_GAMES = 1503;
    public static final int YOU_HAVE_BEEN_REGISTERED_IN_A_WAITING_LIST_OF_NO_CLASS_GAMES = 1504;
    public static final int YOU_HAVE_BEEN_DELETED_FROM_THE_WAITING_LIST_OF_A_GAME = 1505;
    public static final int YOU_HAVE_NOT_BEEN_REGISTERED_IN_A_WAITING_LIST_OF_A_GAME = 1506;
    public static final int THIS_ITEM_CANT_BE_EQUIPPED_FOR_THE_OLYMPIAD_EVENT = 1507;
    public static final int THIS_ITEM_IS_NOT_AVAILABLE_FOR_THE_OLYMPIAD_EVENT = 1508;
    public static final int THIS_SKILL_IS_NOT_AVAILABLE_FOR_THE_OLYMPIAD_EVENT = 1509;
    public static final int OLYMPIAD_PERIOD_S1_HAS_STARTED = 1639;
    public static final int OLYMPIAD_PERIOD_S1_HAS_ENDED = 1640;
    public static final int THE_OLYMPIAD_GAME_HAS_STARTED = 1641;
    public static final int THE_OLYMPIAD_GAME_HAS_ENDED = 1642;
    public static final int THE_OLYMPIAD_GAME_IS_NOT_CURRENTLY_IN_PROGRESS = 1651;
    public static final int S1_HAS_GAINED_S2_OLYMPIAD_POINTS = 1657;
    public static final int S1_HAS_LOST_S2_OLYMPIAD_POINTS = 1658;
    public static final int THE_PRESENT_RECORD_DURING_THE_CURRENT_OLYMPIAD_SESSION_IS_S1_WINS_S2_DEFEATS_YOU_HAVE_EARNED_S3_OLYMPIAD_POINTS = 1673;
    public static final int YOU_ARE_ALREADY_ON_THE_WAITING_LIST_TO_PARTICIPATE_IN_THE_GAME_FOR_YOUR_CLASS = 1689;
    public static final int YOU_ARE_ALREADY_ON_THE_WAITING_LIST_FOR_ALL_CLASSES_WAITING_TO_PARTICIPATE_IN_THE_GAME = 1690;
    public static final int SINCE_80_PERCENT_OR_MORE_OF_YOUR_INVENTORY_SLOTS_ARE_FULL_YOU_CANNOT_PARTICIPATE_IN_THE_OLYMPIAD = 1691;
    public static final int SINCE_YOU_HAVE_CHANGED_YOUR_CLASS_INTO_A_SUB_JOB_YOU_CANNOT_PARTICIPATE_IN_THE_OLYMPIAD = 1692;
    public static final int WHILE_YOU_ARE_ON_THE_WAITING_LIST_YOU_ARE_NOT_ALLOWED_TO_WATCH_THE_GAME = 1693;

	/**
	 * @param _characters
	 */
	public SystemMessage(int messageId)
	{
		_messageId = messageId;
	}
    
    public static SystemMessage sendString(String msg)
    {
        SystemMessage sm = new SystemMessage(S1_S2);
        sm.addString(msg);
        
        return sm;
    }
	
	public SystemMessage addString(String text)
	{
		_types.add(new Integer(TYPE_TEXT));
		_values.add(text);
        
        return this;
	}

	public SystemMessage addNumber(int number)
	{
		_types.add(new Integer(TYPE_NUMBER));
		_values.add(new Integer(number));
        return this;
	}
	
	public SystemMessage addNpcName(int id)
	{
		_types.add(new Integer(TYPE_NPC_NAME));
		_values.add(new Integer(1000000 + id));
        
        return this;
	}

	public SystemMessage addItemName(int id)
	{
		_types.add(new Integer(TYPE_ITEM_NAME));
		_values.add(new Integer(id));
        
        return this;
	}

	public SystemMessage addSkillName(int id)
	{
		_types.add(new Integer(TYPE_SKILL_NAME));
		_values.add(new Integer(id));
        
        return this;
	}

	final void runImpl()
	{
		// no long-running tasks
	}
	
	final void writeImpl()
	{
		writeC(0x64);

		writeD(_messageId);
		writeD(_types.size());

		for (int i = 0; i < _types.size(); i++)
		{
			int t = _types.get(i).intValue();

			writeD(t);

			switch (t)
			{
				case TYPE_TEXT:
				{
					writeS( (String)_values.get(i));
					break;
				}
				case TYPE_NUMBER:
				case TYPE_NPC_NAME:
				case TYPE_ITEM_NAME:
				{
					int t1 = ((Integer)_values.get(i)).intValue();
					writeD(t1);	
					break;
				}
				case TYPE_SKILL_NAME:
				{
					int t1 = ((Integer)_values.get(i)).intValue();
					writeD(t1);	
					writeD(1);  //??
					break;
				}
			}
		}
	}

	/* (non-Javadoc)
	 * @see net.sf.l2j.gameserver.serverpackets.ServerBasePacket#getType()
	 */
	public String getType()
	{
		return _S__7A_SYSTEMMESSAGE;
	}
}
	
