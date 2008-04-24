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
package net.sf.l2j.gameserver.datatables;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javolution.util.FastList;
import javolution.util.FastMap;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.model.L2EnchantSkillLearn;
import net.sf.l2j.gameserver.model.L2PledgeSkillLearn;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2SkillLearn;
import net.sf.l2j.gameserver.model.L2TransformSkillLearn;
import net.sf.l2j.gameserver.model.L2EnchantSkillLearn.EnchantSkillDetail;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.base.ClassId;

/**
 * This class ...
 *
 * @version $Revision: 1.13.2.2.2.8 $ $Date: 2005/04/06 16:13:25 $
 */
public class SkillTreeTable
{
    public static final int NORMAL_ENCHANT_COST_MULTIPLIER = 1;
    public static final int SAFE_ENCHANT_COST_MULTIPLIER = 3;
    
    public static final int NORMAL_ENCHANT_BOOK = 6622;
    public static final int SAFE_ENCHANT_BOOK = 9627;
    public static final int CHANGE_ENCHANT_BOOK = 9626;
    public static final int UNTRAIN_ENCHANT_BOOK = 9625;
    
	private static Logger _log = Logger.getLogger(SkillTreeTable.class.getName());
	private static SkillTreeTable _instance;

	private Map<ClassId, Map<Integer,L2SkillLearn>> _skillTrees;
    private List<L2SkillLearn> _fishingSkillTrees; //all common skills (teached by Fisherman)
    private List<L2SkillLearn> _expandDwarfCraftSkillTrees; //list of special skill for dwarf (expand dwarf craft) learned by class teacher
    private List<L2PledgeSkillLearn> _pledgeSkillTrees; //pledge skill list
    private Map<Integer, L2EnchantSkillLearn> _enchantSkillTrees; //enchant skill list
	private List<L2TransformSkillLearn> _TransformSkillTrees; // Transform Skills (Test)

    public static SkillTreeTable getInstance()
	{
        if (_instance == null)
            _instance = new SkillTreeTable();

		return _instance;
	}

	/**
	 * Return the minimum level needed to have this Expertise.<BR><BR>
	 *
	 * @param grade The grade level searched
	 */
	public int getExpertiseLevel(int grade)
	{
		if (grade <= 0)
			return 0;

		// since expertise comes at same level for all classes we use paladin for now
		Map<Integer,L2SkillLearn> learnMap = getSkillTrees().get(ClassId.paladin);

		int skillHashCode = SkillTable.getSkillHashCode(239,grade);
        if (learnMap.containsKey(skillHashCode))
        {
            return learnMap.get(skillHashCode).getMinLevel();
		}

		_log.severe("Expertise not found for grade "+grade);
		return 0;
	}

	/**
     * Each class receives new skill on certain levels, this methods allow the retrieval of the minimun character level
     * of given class required to learn a given skill
     * @param skillId The iD of the skill
     * @param classID The classId of the character
     * @param skillLvl The SkillLvl
     * @return The min level
     */
    public int getMinSkillLevel(int skillId, ClassId classId, int skillLvl)
    {
        Map<Integer,L2SkillLearn> map = getSkillTrees().get(classId);

        int skillHashCode = SkillTable.getSkillHashCode(skillId,skillLvl);

        if (map.containsKey(skillHashCode))
        {
            return map.get(skillHashCode).getMinLevel();
        }

        return 0;
    }

    public int getMinSkillLevel(int skillId, int skillLvl)
    {
    	int skillHashCode = SkillTable.getSkillHashCode(skillId, skillLvl);

    	// Look on all classes for this skill (takes the first one found)
    	for (Map<Integer,L2SkillLearn> map : getSkillTrees().values())
    	{
    		// checks if the current class has this skill
            if (map.containsKey(skillHashCode))
            {
                return map.get(skillHashCode).getMinLevel();
            }
    	}
        return 0;
    }

	private SkillTreeTable()
	{
	    int classId = 0;
        int count   = 0;

		java.sql.Connection con = null;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT * FROM class_list ORDER BY id");
			ResultSet classlist = statement.executeQuery();

			Map<Integer, L2SkillLearn> map;
			int parentClassId;
			L2SkillLearn skillLearn;

			while (classlist.next())
			{
				map = new FastMap<Integer, L2SkillLearn>();
				parentClassId = classlist.getInt("parent_id");
				classId = classlist.getInt("id");
				PreparedStatement statement2 = con.prepareStatement("SELECT class_id, skill_id, level, name, sp, min_level FROM skill_trees where class_id=? ORDER BY skill_id, level");
				statement2.setInt(1, classId);
				ResultSet skilltree = statement2.executeQuery();

				if (parentClassId != -1)
				{
					Map<Integer, L2SkillLearn> parentMap = getSkillTrees().get(ClassId.values()[parentClassId]);
					map.putAll(parentMap);
				}

				int prevSkillId = -1;

				while (skilltree.next())
				{
					int id = skilltree.getInt("skill_id");
					int lvl = skilltree.getInt("level");
					String name = skilltree.getString("name");
					int minLvl = skilltree.getInt("min_level");
					int cost = skilltree.getInt("sp");

					if (prevSkillId != id)
						prevSkillId = id;

                    skillLearn = new L2SkillLearn(id, lvl, minLvl, name, cost, 0, 0);
					map.put(SkillTable.getSkillHashCode(id,lvl), skillLearn);
				}

				getSkillTrees().put(ClassId.values()[classId], map);
				skilltree.close();
				statement2.close();

                count += map.size();
				_log.fine("SkillTreeTable: skill tree for class " + classId + " has " + map.size() + " skills");
			}

			classlist.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.severe("Error while creating skill tree (Class ID " + classId + "):" + e);
		}

        _log.config("SkillTreeTable: Loaded " + count + " skills.");

        //Skill tree for fishing skill (from Fisherman)
        int count2   = 0;
        int count3   = 0;

        try
        {
            _fishingSkillTrees = new FastList<L2SkillLearn>();
            _expandDwarfCraftSkillTrees = new FastList<L2SkillLearn>();

            PreparedStatement statement = con.prepareStatement("SELECT skill_id, level, name, sp, min_level, costid, cost, isfordwarf FROM fishing_skill_trees ORDER BY skill_id, level");
            ResultSet skilltree2 = statement.executeQuery();

            int prevSkillId = -1;

            while (skilltree2.next())
            {
                int id = skilltree2.getInt("skill_id");
                int lvl = skilltree2.getInt("level");
                String name = skilltree2.getString("name");
                int minLvl = skilltree2.getInt("min_level");
                int cost = skilltree2.getInt("sp");
                int costId = skilltree2.getInt("costid");
                int costCount = skilltree2.getInt("cost");
                int isDwarven = skilltree2.getInt("isfordwarf");

                if (prevSkillId != id)
                    prevSkillId = id;

                L2SkillLearn skill = new L2SkillLearn(id, lvl, minLvl, name, cost, costId, costCount);

                if (isDwarven == 0)
                    _fishingSkillTrees.add(skill);
                else
                    _expandDwarfCraftSkillTrees.add(skill);
            }

            skilltree2.close();
            statement.close();

            count2 = _fishingSkillTrees.size();
            count3 = _expandDwarfCraftSkillTrees.size();
		}
        catch (Exception e)
        {
            _log.severe("Error while creating fishing skill table: " + e);
        }

        int count4   = 0;
        try
        {
            _enchantSkillTrees = new FastMap<Integer, L2EnchantSkillLearn>();

            PreparedStatement statement = con.prepareStatement("SELECT skill_id, level, name, base_lvl, sp, min_skill_lvl, exp, success_rate76, success_rate77, success_rate78 FROM enchant_skill_trees ORDER BY skill_id, level");
            ResultSet skilltree3 = statement.executeQuery();

            int prevSkillId = -1;

            while (skilltree3.next())
            {
                int id = skilltree3.getInt("skill_id");
                int lvl = skilltree3.getInt("level");
                String name = skilltree3.getString("name");
                int baseLvl = skilltree3.getInt("base_lvl");
                int minSkillLvl = skilltree3.getInt("min_skill_lvl");
                int sp = skilltree3.getInt("sp");
                int exp = skilltree3.getInt("exp");
                byte rate76 = skilltree3.getByte("success_rate76");
                byte rate77 = skilltree3.getByte("success_rate77");
                byte rate78 = skilltree3.getByte("success_rate78");

                if (prevSkillId != id)
                    prevSkillId = id;

                L2EnchantSkillLearn skill = _enchantSkillTrees.get(id);
                if (skill == null)
                {
                    skill = new L2EnchantSkillLearn(id, baseLvl);
                    _enchantSkillTrees.put(id, skill);
                }
                EnchantSkillDetail esd = new EnchantSkillDetail(lvl, minSkillLvl,  name, sp, exp, rate76 , rate77, rate78);
                skill.addEnchantDetail(esd);
            }

            skilltree3.close();
            statement.close();

            count4 = _enchantSkillTrees.size();
        }
        catch (Exception e)
        {
            _log.log(Level.SEVERE, "Error while creating enchant skill table ", e);
        }

        int count5   = 0;
        try
        {
            _pledgeSkillTrees = new FastList<L2PledgeSkillLearn>();

            PreparedStatement statement = con.prepareStatement("SELECT skill_id, level, name, clan_lvl, repCost, itemId FROM pledge_skill_trees ORDER BY skill_id, level");
            ResultSet skilltree4 = statement.executeQuery();

            int prevSkillId = -1;

            while (skilltree4.next())
            {
                int id = skilltree4.getInt("skill_id");
                int lvl = skilltree4.getInt("level");
                String name = skilltree4.getString("name");
                int baseLvl = skilltree4.getInt("clan_lvl");
                int sp = skilltree4.getInt("repCost");
                int itemId = skilltree4.getInt("itemId");

                if (prevSkillId != id)
                    prevSkillId = id;

                L2PledgeSkillLearn skill = new L2PledgeSkillLearn(id, lvl, baseLvl, name, sp, itemId);

                _pledgeSkillTrees.add(skill);
            }

            skilltree4.close();
            statement.close();

            count5 = _pledgeSkillTrees.size();
        }
        catch (Exception e)
        {
            _log.severe("Error while creating fishing skill table: " + e);
        }
        int count6   = 0;
        try
        {
            _TransformSkillTrees = new FastList<L2TransformSkillLearn>();

            PreparedStatement statement = con.prepareStatement("SELECT race_id, skill_id, item_id, level, name, sp, min_level FROM transform_skill_trees ORDER BY race_id, skill_id, level");
            ResultSet skilltree5 = statement.executeQuery();

            int prevSkillId = -1;

            while (skilltree5.next())
            {
                int race_id = skilltree5.getInt("race_id");
                int skill_id = skilltree5.getInt("skill_id");
                int item_id = skilltree5.getInt("item_id");
                int level = skilltree5.getInt("level");
                String name = skilltree5.getString("name");
                int sp = skilltree5.getInt("sp");
                int min_level = skilltree5.getInt("min_level");

                if (prevSkillId != skill_id)
                    prevSkillId = skill_id;

                L2TransformSkillLearn skill = new L2TransformSkillLearn(race_id, skill_id, item_id, level, name, sp, min_level);

                _TransformSkillTrees.add(skill);
            }

            skilltree5.close();
            statement.close();

            count6 = _TransformSkillTrees.size();
        }
        catch (Exception e)
        {
            _log.log(Level.SEVERE, "Error while creating Transformation skill table ", e);
        }

        finally
        {
            try { con.close(); } catch (Exception e) {}
        }

        _log.config("FishingSkillTreeTable: Loaded " + count2 + " general skills.");
        _log.config("FishingSkillTreeTable: Loaded " + count3 + " dwarven skills.");
        _log.config("EnchantSkillTreeTable: Loaded " + count4 + " enchant skills.");
        _log.config("PledgeSkillTreeTable: Loaded " + count5 + " pledge skills");
        _log.config("TransformSkillTreeTable: Loaded " + count6 + " transform skills");
    }

    private Map<ClassId, Map<Integer, L2SkillLearn>> getSkillTrees()
    {
        if (_skillTrees == null)
            _skillTrees = new FastMap<ClassId, Map<Integer, L2SkillLearn>>();

        return _skillTrees;
    }

	public L2SkillLearn[] getAvailableSkills(L2PcInstance cha, ClassId classId)
	{
		List<L2SkillLearn> result = new FastList<L2SkillLearn>();
		Collection<L2SkillLearn> skills = getSkillTrees().get(classId).values();

		if (skills == null)
		{
			// the skilltree for this class is undefined, so we give an empty list
			_log.warning("Skilltree for class " + classId + " is not defined !");
			return new L2SkillLearn[0];
		}

		L2Skill[] oldSkills = cha.getAllSkills();

        for (L2SkillLearn temp : skills)
        {
            if (temp.getMinLevel() <= cha.getLevel())
            {
                boolean knownSkill = false;

                for (int j = 0; j < oldSkills.length && !knownSkill; j++)
                {
                    if (oldSkills[j].getId() == temp.getId() )
                    {
                        knownSkill = true;

                        if ( oldSkills[j].getLevel() == temp.getLevel()-1)
                        {
                            // this is the next level of a skill that we know
                            result.add(temp);
                        }
                    }
                }

                if (!knownSkill && temp.getLevel() == 1)
                {
                    // this is a new skill
                    result.add(temp);
                }
            }
        }

        return result.toArray(new L2SkillLearn[result.size()]);
	}

	public L2SkillLearn[] getAvailableSkills(L2PcInstance cha)
	{
	    List<L2SkillLearn> result = new FastList<L2SkillLearn>();
	    List<L2SkillLearn> skills = new FastList<L2SkillLearn>();

        skills.addAll(_fishingSkillTrees);

	    if (skills == null)
	    {
	        // the skilltree for this class is undefined, so we give an empty list
	        _log.warning("Skilltree for fishing is not defined !");
	        return new L2SkillLearn[0];
	    }

        if (cha.hasDwarvenCraft() && _expandDwarfCraftSkillTrees != null)
        {
            skills.addAll(_expandDwarfCraftSkillTrees);
        }

        L2Skill[] oldSkills = cha.getAllSkills();

        for (L2SkillLearn temp : skills)
        {
            if (temp.getMinLevel() <= cha.getLevel())
            {
                boolean knownSkill = false;

                for (int j = 0; j < oldSkills.length && !knownSkill; j++)
                {
                    if (oldSkills[j].getId() == temp.getId() )
                    {
                        knownSkill = true;

                        if ( oldSkills[j].getLevel() == temp.getLevel()-1)
                        {
                            // this is the next level of a skill that we know
                            result.add(temp);
                        }
                    }
                }

                if (!knownSkill && temp.getLevel() == 1)
                {
                    // this is a new skill
                    result.add(temp);
                }
            }
        }

        return result.toArray(new L2SkillLearn[result.size()]);
	}
    
    public L2EnchantSkillLearn getSkillEnchantmentForSkill(L2Skill skill)
    {
        L2EnchantSkillLearn esl = this.getSkillEnchantmentBySkillId(skill.getId());
        // there is enchantment for this skill and we have the required level of it
        if (esl != null && skill.getLevel() >= esl.getBaseLevel())
        {
            return esl;
        }
        return null;
    }
    
    public L2EnchantSkillLearn getSkillEnchantmentBySkillId(int skillId)
    {
        return _enchantSkillTrees.get(skillId);
    }

    public L2TransformSkillLearn[] getAvailableTransformSkills(L2PcInstance cha)
    {
        List<L2TransformSkillLearn> result = new FastList<L2TransformSkillLearn>();
        List<L2TransformSkillLearn> skills = _TransformSkillTrees;

        if (skills == null)
        {
            // the skilltree for this class is undefined, so we give an empty list

            _log.warning("No Transform skills defined!");
            return new L2TransformSkillLearn[0];
        }

        L2Skill[] oldSkills = cha.getAllSkills();

        for (L2TransformSkillLearn temp : skills)
        {
            if (temp.getMinLevel() <= cha.getLevel() && temp.getRace() == cha.getRace().ordinal())
            {
                boolean knownSkill = false;

                for (int j = 0; j < oldSkills.length && !knownSkill; j++)
                {
                    if (oldSkills[j].getId() == temp.getId() )
                    {
                        knownSkill = true;

                        if ( oldSkills[j].getLevel() == temp.getLevel()-1)
                        {
                            // this is the next level of a skill that we know
                            result.add(temp);
                        }
                    }
                }

                if (!knownSkill && temp.getLevel() == 1)
                {
                    // this is a new skill
                    result.add(temp);
                }
            }
        }

        return result.toArray(new L2TransformSkillLearn[result.size()]);
    }

    public L2PledgeSkillLearn[] getAvailablePledgeSkills(L2PcInstance cha)
    {
        List<L2PledgeSkillLearn> result = new FastList<L2PledgeSkillLearn>();
        List<L2PledgeSkillLearn> skills = _pledgeSkillTrees;

        if (skills == null)
        {
            // the skilltree for this class is undefined, so we give an empty list

            _log.warning("No clan skills defined!");
            return new L2PledgeSkillLearn[0];
        }

        L2Skill[] oldSkills = cha.getClan().getAllSkills();

        for (L2PledgeSkillLearn temp : skills)
        {
            if (temp.getBaseLevel() <= cha.getClan().getLevel())
            {
                boolean knownSkill = false;

                for (int j = 0; j < oldSkills.length && !knownSkill; j++)
                {
                    if (oldSkills[j].getId() == temp.getId() )
                    {
                        knownSkill = true;

                        if ( oldSkills[j].getLevel() == temp.getLevel()-1)
                        {
                            // this is the next level of a skill that we know
                            result.add(temp);
                        }
                    }
                }

                if (!knownSkill && temp.getLevel() == 1)
                {
                    // this is a new skill
                    result.add(temp);
                }
            }
        }

        return result.toArray(new L2PledgeSkillLearn[result.size()]);
    }


	/**
	 * Returns all allowed skills for a given class.
	 * @param classId
	 * @return all allowed skills for a given class.
	 */
	public Collection<L2SkillLearn> getAllowedSkills(ClassId classId)
	{
		return getSkillTrees().get(classId).values();
	}

	public int getMinLevelForNewSkill(L2PcInstance cha, ClassId classId)
	{
        int minLevel = 0;
		Collection<L2SkillLearn> skills = getSkillTrees().get(classId).values();

		if (skills == null)
		{
			// the skilltree for this class is undefined, so we give an empty list
			_log.warning("Skilltree for class " + classId + " is not defined !");
			return minLevel;
        }

        for (L2SkillLearn temp: skills)
        {
            if (temp.getMinLevel() > cha.getLevel() && temp.getSpCost() != 0)
                if (minLevel==0 || temp.getMinLevel()<minLevel)
                    minLevel = temp.getMinLevel();
		}

		return minLevel;
	}

    public int getMinLevelForNewSkill(L2PcInstance cha)
    {
        int minLevel = 0;
        List<L2SkillLearn> skills = new FastList<L2SkillLearn>();

        skills.addAll(_fishingSkillTrees);

        if (skills == null)
        {
            // the skilltree for this class is undefined, so we give an empty list
            _log.warning("SkillTree for fishing is not defined !");
            return minLevel;
        }

        if (cha.hasDwarvenCraft() && _expandDwarfCraftSkillTrees != null)
        {
            skills.addAll(_expandDwarfCraftSkillTrees);
        }

        for (L2SkillLearn s : skills)
        {
            if (s.getMinLevel() > cha.getLevel())
                if (minLevel == 0 || s.getMinLevel() < minLevel)
                    minLevel = s.getMinLevel();
        }

        return minLevel;
    }

    public int getMinLevelForNewTransformSkill(L2PcInstance cha)
    {
        int minLevel = 0;
        List<L2TransformSkillLearn> skills = new FastList<L2TransformSkillLearn>();

        skills.addAll(_TransformSkillTrees);

        if (skills == null)
        {
            // the skilltree for this class is undefined, so we give an empty list
            _log.warning("SkillTree for fishing is not defined !");
            return minLevel;
        }

        for (L2TransformSkillLearn s : skills)
        {
            if ((s.getMinLevel() > cha.getLevel()) && (s.getRace() == cha.getRace().ordinal()))
                if (minLevel == 0 || s.getMinLevel() < minLevel)
                    minLevel = s.getMinLevel();
        }

        return minLevel;
    }
    
    public int getSkillCost(L2PcInstance player, L2Skill skill)
    {
        int skillCost = 100000000;
        ClassId classId = player.getSkillLearningClassId();
        int skillHashCode = SkillTable.getSkillHashCode(skill);

        if (getSkillTrees().get(classId).containsKey(skillHashCode))
        {
        	L2SkillLearn skillLearn = getSkillTrees().get(classId).get(skillHashCode);
        	if (skillLearn.getMinLevel() <= player.getLevel())
            {
                skillCost = skillLearn.getSpCost();
                if (!player.getClassId().equalsOrChildOf(classId))
                {
                    if (skill.getCrossLearnAdd() < 0)
                        return skillCost;

                    skillCost += skill.getCrossLearnAdd();
                    skillCost *= skill.getCrossLearnMul();
                }

                if ((classId.getRace() != player.getRace()) && !player.isSubClassActive())
                    skillCost *= skill.getCrossLearnRace();

                if (classId.isMage() != player.getClassId().isMage())
                    skillCost *= skill.getCrossLearnProf();
            }
        }

        return skillCost;
    }

    public int getEnchantSkillSpCost(L2Skill skill)
    {
        L2EnchantSkillLearn enchantSkillLearn = _enchantSkillTrees.get(skill.getId());
        if (enchantSkillLearn != null)
        {
            EnchantSkillDetail esd = enchantSkillLearn.getEnchantSkillDetail(skill.getLevel());
            if (esd != null)
            {
                return esd.getSpCost();
            }
        }
        
        return Integer.MAX_VALUE;
    }

    public int getEnchantSkillExpCost(L2Skill skill)
    {
        L2EnchantSkillLearn enchantSkillLearn = _enchantSkillTrees.get(skill.getId());
        if (enchantSkillLearn != null)
        {
            EnchantSkillDetail esd = enchantSkillLearn.getEnchantSkillDetail(skill.getLevel());
            if (esd != null)
            {
                return esd.getExp();
            }
        }
        
        return Integer.MAX_VALUE;
    }

    public byte getEnchantSkillRate(L2PcInstance player, L2Skill skill)
    {
        L2EnchantSkillLearn enchantSkillLearn = _enchantSkillTrees.get(skill.getId());
        if (enchantSkillLearn != null)
        {
            EnchantSkillDetail esd = enchantSkillLearn.getEnchantSkillDetail(skill.getLevel());
            if (esd != null)
            {
                return esd.getRate(player);
            }
        }
        
        return 0;
    }
}
