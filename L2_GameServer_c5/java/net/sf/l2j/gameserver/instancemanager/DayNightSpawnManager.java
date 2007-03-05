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
package net.sf.l2j.gameserver.instancemanager;

import java.util.Map;
import java.util.logging.Logger;

import javolution.util.FastMap;
import net.sf.l2j.gameserver.GameTimeController;
import net.sf.l2j.gameserver.model.L2Spawn;
import net.sf.l2j.gameserver.model.actor.instance.L2NpcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2RaidBossInstance;

/**
 * This class ...
 * 
 * @version $Revision: $ $Date: $
 * @author  godson
 */

public class DayNightSpawnManager {
    
    private static Logger _log = Logger.getLogger(DayNightSpawnManager.class.getName());
    
    private static DayNightSpawnManager _instance;
    private static Map<L2Spawn, L2NpcInstance> _dayCreatures;
    private static Map<L2Spawn, L2NpcInstance> _nightCreatures;
    private static Map<L2Spawn, L2RaidBossInstance> _bosses;
    
    //private static int _currentState;  // 0 = Day, 1 = Night
    
    public static DayNightSpawnManager getInstance()
    {
        if (_instance == null)
            _instance = new DayNightSpawnManager(); 
        return _instance;
    }
    
    private DayNightSpawnManager()
    {
        _dayCreatures = new FastMap<L2Spawn, L2NpcInstance>();
        _nightCreatures = new FastMap<L2Spawn, L2NpcInstance>();
        _bosses = new FastMap<L2Spawn, L2RaidBossInstance>();
        
        _log.info("DayNightSpawnManager: Day/Night handler initialised");
    }
    
    public void addDayCreature(L2Spawn spawnDat)
    { 
        if (_dayCreatures.containsKey(spawnDat))
        {
            _log.warning("DayNightSpawnManager: Spawn already added into day map");
            return;
        }
        else
            _dayCreatures.put(spawnDat, null);
    }
    
    public void addNightCreature(L2Spawn spawnDat)
    {
        if (_nightCreatures.containsKey(spawnDat))
        {
            _log.warning("DayNightSpawnManager: Spawn already added into night map");
            return;
        }
        else
            _nightCreatures.put(spawnDat, null);
    }
    
    public void spawnDayCreatures()
    {
        try
        {
            if (_nightCreatures.size() != 0)
            {
                int i = 0;
                for (L2NpcInstance nightCreature : _nightCreatures.values())
                {
                    if (nightCreature == null) continue;
                    nightCreature.getSpawn().stopRespawn();
                    nightCreature.deleteMe();
                    i++;
                }
                
                _log.info("DayNightSpawnManager: Deleted " + i + " night creatures");
            }
            
            int i = 0;
            L2NpcInstance creature = null;
            for (L2Spawn spawnDat : _dayCreatures.keySet())
            {
                if (_dayCreatures.get(spawnDat) == null)
                {
                    creature = spawnDat.doSpawn();
                    if (creature == null) continue;
                    
                    creature.setCurrentHp(creature.getMaxHp());
                    creature.setCurrentMp(creature.getMaxMp());
                    _dayCreatures.remove(spawnDat);
                    _dayCreatures.put(spawnDat, creature);

			creature = _dayCreatures.get(spawnDat);
			creature.getSpawn().startRespawn();

                }
                else
                {
                    creature = _dayCreatures.get(spawnDat);
                    if (creature == null) continue;
                    
                    creature.getSpawn().startRespawn();
                    creature.setCurrentHp(creature.getMaxHp());
                    creature.setCurrentMp(creature.getMaxMp());
                    creature.spawnMe();
                }
                i++;
            }
            
            _log.info("DayNightSpawnManager: Spawning " + i + " day creatures");
        }catch(Exception e){e.printStackTrace();}
    }
    
    public void spawnNightCreatures()
    {
        try
        {
            if (_dayCreatures.size() != 0)
            {
                int i = 0;
                for (L2NpcInstance dayCreature : _dayCreatures.values())
                {
                    if (dayCreature == null) continue;
                    
                    dayCreature.getSpawn().stopRespawn();
                    dayCreature.deleteMe();
                    i++;
                }
                _log.info("DayNightSpawnManager: Deleted " + i + " day creatures");
            }
            
            int i = 0;
            L2NpcInstance creature = null;
            for (L2Spawn spawnDat : _nightCreatures.keySet())
            {
                if (_nightCreatures.get(spawnDat) == null)
                {
                    creature = spawnDat.doSpawn();
                    if (creature == null) continue;
                    
                    _nightCreatures.remove(spawnDat);
                    _nightCreatures.put(spawnDat, creature);
                    creature.setCurrentHp(creature.getMaxHp());
                    creature.setCurrentMp(creature.getMaxMp());
					
			creature = _nightCreatures.get(spawnDat);
			creature.getSpawn().startRespawn();
                }
                else
                {
                    creature = _nightCreatures.get(spawnDat);
                    if (creature == null) continue;
                    
                    creature.getSpawn().startRespawn();
                    creature.setCurrentHp(creature.getMaxHp());
                    creature.setCurrentMp(creature.getMaxMp());
                    creature.spawnMe();
                }
                
                i++;
            }
            
            _log.info("DayNightSpawnManager: Spawning " + i + " night creatures");
        }catch(Exception e){e.printStackTrace();}
    }
    
    private void changeMode(int mode)
    {
        if (_nightCreatures.size() == 0 && _dayCreatures.size() == 0)
            return;
        
        switch(mode) {
            case 0:
                spawnDayCreatures();
                specialNightBoss(0);
                break;
            case 1:
                spawnNightCreatures();
                specialNightBoss(1);
                break;
                default:
                    _log.warning("DayNightSpawnManager: Wrong mode sent");
                break;
        }
    }
    
    public void notifyChangeMode()
    {
        try{
            if (GameTimeController.getInstance().isNowNight())
                changeMode(1);
            else
                changeMode(0);
        }catch(Exception e){e.printStackTrace();}
    }
    
    public void cleanUp()
    {
        _nightCreatures.clear();
        _dayCreatures.clear();
        _bosses.clear();
    }
    
    private void specialNightBoss(int mode)
    {
        try{
        for (L2Spawn spawn : _bosses.keySet())
        {
            L2RaidBossInstance boss = _bosses.get(spawn);
            
            if (boss == null && mode == 1)
            {
                boss = (L2RaidBossInstance)spawn.doSpawn();
                RaidBossSpawnManager.getInstance().notifySpawnNightBoss(boss);
                _bosses.remove(spawn);
                _bosses.put(spawn, boss);
                continue;
            }
            
            if (boss == null && mode == 0)
                continue;
            
            if(boss.getNpcId() == 25328 && 
                    boss.getRaidStatus().equals(RaidBossSpawnManager.StatusEnum.ALIVE))
                handleHellmans(boss, mode);
            return;
        }
        }catch(Exception e){e.printStackTrace();}
    }
    
    private void handleHellmans(L2RaidBossInstance boss, int mode)
    {
        switch(mode)
        {
            case 0:
                boss.deleteMe();
                _log.info("DayNightSpawnManager: Deleting Hellman raidboss");
                break;
            case 1:
                boss.spawnMe();
                _log.info("DayNightSpawnManager: Spawning Hellman raidboss");
                break;
        }
    }
    
    public L2RaidBossInstance handleBoss(L2Spawn spawnDat)
    {
        if(_bosses.containsKey(spawnDat)) return _bosses.get(spawnDat);
        
        if (GameTimeController.getInstance().isNowNight())
        {    
            L2RaidBossInstance raidboss = (L2RaidBossInstance)spawnDat.doSpawn();
            _bosses.put(spawnDat, raidboss);
            
            return raidboss;
        }
        else
            _bosses.put(spawnDat, null);
        
       return null;
    }
}
