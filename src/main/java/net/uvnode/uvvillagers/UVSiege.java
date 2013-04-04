package net.uvnode.uvvillagers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.bukkit.entity.LivingEntity;

/**
 * A siege.
 *
 * @author James Cornwell-Shiel
 *
 */
public class UVSiege {

    private Map<Integer, LivingEntity> _spawns = new HashMap<Integer, LivingEntity>();
    //ArrayList<Entity> _spawns = new ArrayList<Entity>();
    //private ArrayList<Integer> _spawnIds = new ArrayList<Integer>();
    private Map<String, Integer> _players = new HashMap<String, Integer>();
    private Map<String, Integer> _playerPoints = new HashMap<String, Integer>();
    private UVVillage _village = null;
    private int _mobsLeft;

    UVSiege(UVVillage village) {
        _village = village;
        _mobsLeft = 0;
    }

    /**
     * Add a spawn to the records
     *
     * @param entity
     */
    public void addSpawn(LivingEntity entity) {
        _spawns.put(entity.getEntityId(), entity);
        _mobsLeft++;
    }

    /**
     * Check to see if an entity is part of this siege
     *
     * @param entityId the entity's ID
     * @return true if part, false if not
     */
    public boolean checkEntityId(int entityId) {
        return _spawns.containsKey(entityId);
    }

    /**
     * Record a player kill
     *
     * @param name player name
     * @param value point value of kill
     */
    public void addPlayerKill(String name, Integer value) {
        if (_players.containsKey(name)) {
            _players.put(name, _players.get(name) + 1);
            _playerPoints.put(name, _playerPoints.get(name) + value);
        } else {
            _players.put(name, 1);
            _playerPoints.put(name, value);
        }
    }

    /**
     * Get a player's kill count
     *
     * @param name player name
     * @return kill count
     */
    public int getPlayerKills(String name) {
        if (_players.containsKey(name)) {
            return _players.get(name);
        } else {
            return 0;
        }
    }

    /**
     * Get a player's kill points
     *
     * @param name player name
     * @return kill points
     */
    public int getPlayerPoints(String name) {
        if (_playerPoints.containsKey(name)) {
            return _playerPoints.get(name);
        } else {
            return 0;
        }
    }

    /**
     * Get the village object for this siege
     *
     * @return village object
     */
    public UVVillage getVillage() {
        return _village;
    }

    /**
     * Get the siege report
     *
     * @return arraylist of strings
     */
    public ArrayList<String> overviewMessage() {
        ArrayList<String> msgs = new ArrayList<String>();

        msgs.add("Zombie Siege Stats:");
        msgs.add(" - Location: " + _village.getName());
        msgs.add(" - Enemies: " + _spawns.size() + " (" + getMobsLeft().size() + " still alive)");
        msgs.add(" - Player Kills: ");
        Iterator<String> playersIterator = _players.keySet().iterator();
        while (playersIterator.hasNext()) {
            String playerName = playersIterator.next();
            msgs.add("   - " + playerName + ": " + _players.get(playerName));
        }
        return msgs;
    }

    /**
     * Register than a mob has been killed
     */
    public void killMob() {
        _mobsLeft--;
    }

    /**
     * Get the number of mobs remaining
     *
     * @return number of siege mobs alive
     */
    public int getNumMobsLeft() {
        return getMobsLeft().size();
    }

    /**
     * Get a list of remaining mobs
     *
     * @return hashmap of Ids and entities of remaining living mobs
     */
    public Map<Integer, LivingEntity> getMobsLeft() {
        Map<Integer, LivingEntity> living = new HashMap<Integer, LivingEntity>();
        for (Map.Entry<Integer, LivingEntity> mobEntry : _spawns.entrySet()) {
            if (mobEntry.getValue() != null && !mobEntry.getValue().isDead()) {
                living.put(mobEntry.getKey(), mobEntry.getValue());
            }
        }
        return living;
    }
}
