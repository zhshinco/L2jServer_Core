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

import java.util.List;

import javolution.util.FastList;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.quest.Quest;
import net.sf.l2j.gameserver.model.quest.QuestDropInfo;
import net.sf.l2j.gameserver.model.quest.QuestState;

/**
 * 
 * sample for rev 377:
 * 
 * 98 
 * 05 00 		number of quests
 * ff 00 00 00 
 * 0a 01 00 00 
 * 39 01 00 00 
 * 04 01 00 00 
 * a2 00 00 00 
 * 
 * 04 00 		number of quest items
 * 
 * 85 45 13 40 	item obj id
 * 36 05 00 00 	item id
 * 02 00 00 00 	count
 * 00 00 		?? bodyslot
 * 
 * 23 bd 12 40 
 * 86 04 00 00 
 * 0a 00 00 00 
 * 00 00 
 * 
 * 1f bd 12 40 
 * 5a 04 00 00 
 * 09 00 00 00 
 * 00 00 
 * 
 * 1b bd 12 40 
 * 5b 04 00 00 
 * 39 00 00 00 
 * 00 00                                                 .
 * 
 * format h (d) h (dddh)   rev 377
 * format h (dd) h (dddd)  rev 417
 * 
 * @version $Revision: 1.4.2.2.2.2 $ $Date: 2005/02/10 16:44:28 $
 */
public class QuestList extends ServerBasePacket
{
	private static final String _S__98_QUESTLIST = "[S] 80 QuestList";

	private Quest[] _quests;
    private L2PcInstance _activeChar;
	
	final void runImpl()
	{
        if(getClient() != null && getClient().getActiveChar() != null)
        {
            _activeChar = getClient().getActiveChar();
            _quests = _activeChar.getAllActiveQuests();
        }
	}
	
	final void writeImpl()
	{
	    /**
	     * This text was wrote by XaKa
	     * QuestList packet structure:
	     * {
	     * 		1 byte - 0x80
	     * 		2 byte - Number of Quests
	     * 		for Quest in AvailibleQuests
	     * 		{
	     * 			4 byte - Quest.ID
	     * 			4 byte - 1
	     * 		}
	     * 		2 byte - Number of All Quests Item
	     * 		for Item in AllQuestsItem
	     * 		{
	     * 			4 byte - Item.ObjID
	     * 			4 byte - Item.ID
	     * 			4 byte - Item.Count
	     * 			4 byte - 5
	     * 		}
	     */		
	    if((_quests == null) || (_quests.length == 0))
	    {
	        writeC(0x80);
			writeH(0);
			writeH(0);
			return;
		}
		
		writeC(0x80);
		writeH(_quests.length);
		for (Quest q : _quests)
		{
			writeD(q.getQuestIntId());
            QuestState qs = _activeChar.getQuestState(q.getName());
			if(qs == null)
			{
				writeD(0);
                continue;
			}
            
            writeD(qs.getInt("cond"));   // stage of quest progress
		}

		//Prepare info about all quests
		List<QuestDropInfo> questDrops = new FastList<QuestDropInfo>();
		int FullCountDropItems = 0;
		for(Quest q : _quests)
		{
			QuestDropInfo newQDrop = new QuestDropInfo(_activeChar, q.getName());
			//Calculate full count drop items
			FullCountDropItems += newQDrop.dropList.size();
			
			questDrops.add(newQDrop);
		}
		
		writeH(FullCountDropItems);
		for(QuestDropInfo currQDropInfo : questDrops)
		{
			for(QuestDropInfo.DropInfo itemDropInfo : currQDropInfo.dropList)
			{
				writeD(itemDropInfo.dropItemObjID);
				writeD(itemDropInfo.dropItemID);
				writeD(itemDropInfo.dropItemCount);
				writeD(5);							//<<<<---- what is that?
			}
		}
	}

	/* (non-Javadoc)
	 * @see net.sf.l2j.gameserver.serverpackets.ServerBasePacket#getType()
	 */
	public String getType()
	{
		return _S__98_QUESTLIST;
	}
}
