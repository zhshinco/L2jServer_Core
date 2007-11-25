/* This program is free software; you can redistribute it and/or modify
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
package net.sf.l2j.gameserver.model.quest.jython;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.quest.Quest;

import org.apache.bsf.BSFException;
import org.apache.bsf.BSFManager;

public abstract class QuestJython extends Quest
{
	private static BSFManager _bsf;

	/**
	 * Initialize the engine for scripts of quests, luxury shops and blacksmith
	 */
	public static void init()
	{
		try
		{
			// Initialize the engine for loading Jython scripts
			_bsf = new BSFManager();
			// Execution of all the scripts placed in data/jscript
			// inside the DataPack directory

			String dataPackDirForwardSlashes = Config.DATAPACK_ROOT.getPath().replaceAll("\\\\","/");
			String loadingScript =
			    "import sys;"
			  + "sys.path.insert(0,'" + dataPackDirForwardSlashes + "');"
			  + "import data";

			_bsf.exec("jython", "quest", 0, 0, loadingScript);
		}
		catch (BSFException e)
		{
			e.printStackTrace();
		}
	}

	public static boolean reloadQuest(String questFolder)
	{
		try
		{
			_bsf.exec("jython", "quest", 0, 0, "reload(data.jscript.quests."+questFolder+");");
			return true;
		}
		catch (Exception e)
		{
			//System.out.println("Reload Failed");
			//e.printStackTrace();
		}
		return false;
	}

	/**
	 * Constructor used in jython files.
	 * @param questId : int designating the ID of the quest
	 * @param name : String designating the name of the quest
	 * @param descr : String designating the description of the quest
	 */
	public QuestJython(int questId, String name, String descr)
	{
		super(questId, name, descr);
	}
}