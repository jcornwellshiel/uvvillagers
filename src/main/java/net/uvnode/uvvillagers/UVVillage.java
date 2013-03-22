package net.uvnode.uvvillagers;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.server.v1_4_R1.Village;
//import net.minecraft.server.v1_5_R2.Village;

import org.bukkit.Location;

/**
 * Custom village stuff
 *
 * @author James Cornwell-Shiel
 */
public class UVVillage {

    private int _doors;
    private Location _location;
    private Map<String, Integer> _playerReputations = new HashMap<String, Integer>();
    private Map<String, Date> _playerArrived = new HashMap<String, Date>();
    private Map<String, Integer> _playerTicksHere = new HashMap<String, Integer>();
    private Map<String, Integer> _playerTicksGone = new HashMap<String, Integer>();
    private int _population;
    private int _size;
    private Village _villageCore;
    private String _name;

    /**
     * Default Constructor
     *
     * @param location The location of the village
     * @param village The core village associated with this village
     */
    public UVVillage(Location location, Village village) {
        _location = location;
        _villageCore = village;
        if (_villageCore != null) {
            updateVillageDataFromCore();
        }
    }

    /**
     * In depth constructor (for loads)
     *
     * @param location
     * @param doors
     * @param population
     * @param size
     * @param playerReputations
     */
    public UVVillage(Location location, int doors, int population, int size, Map<String, Integer> playerReputations) {
        _location = location;
        _doors = doors;
        _population = population;
        _size = size;
        _playerReputations = playerReputations;
        _villageCore = null;
    }

    /**
     * Get the number of doors in the village.
     *
     * @return number of doors
     */
    public int getDoors() {
        return _doors;
    }

    /**
     * Get the name of the village
     *
     * @return name
     */
    public String getName() {
        return _name;
    }

    /**
     * Get the location of the village
     *
     * @return location
     */
    public Location getLocation() {
        return _location;
    }

    /**
     * Get the village population
     *
     * @return population count
     */
    public int getPopulation() {
        return _population;
    }

    /**
     * Get the physical size of the village
     *
     * @return the block diameter of the village.
     */
    public int getSize() {
        return _size;
    }

    /**
     * Get the core MC village object associated with this village
     *
     * @return core village
     */
    public Village getVillageCore() {
        return _villageCore;
    }

    /**
     * Set the village name
     *
     * @param name
     */
    public void setName(String name) {
        _name = name;
    }

    /**
     * Set the number of doors
     *
     * @param doors
     */
    public void setDoors(int doors) {
        _doors = doors;
    }

    /**
     * Set the village location
     *
     * @param location
     */
    public void setLocation(Location location) {
        _location = location;
    }

    /**
     * Set the village population
     *
     * @param population
     */
    public void setPopulation(int population) {
        _population = population;
    }

    /**
     * Set the village size
     *
     * @param size
     */
    public void setSize(int size) {
        _size = size;
    }

    /**
     * Attach a core village
     *
     * @param villageCore core MC village
     */
    public void setVillageCore(Village villageCore) {
        _villageCore = villageCore;
    }

    /**
     * Updates the village data from the associated core village, and return a
     * value indicating what has changed.
     *
     * @return 0 if nothing changed, 1 if geometry changed, 2 if data changed, 3
     * if both changed.
     */
    public int updateVillageDataFromCore() {
        boolean geometryChanged = false, dataChanged = false;
        if (_location.getBlockX() != _villageCore.getCenter().x || _location.getBlockY() != _villageCore.getCenter().y || _location.getBlockZ() != _villageCore.getCenter().z) {
            _location.setX(_villageCore.getCenter().x);
            _location.setY(_villageCore.getCenter().y);
            _location.setZ(_villageCore.getCenter().z);
            geometryChanged = true;
        }

        if (_villageCore.getSize() != _size) {
            _size = _villageCore.getSize();
            geometryChanged = true;
        }
        if (_villageCore.getDoorCount() != _doors) {
            _doors = _villageCore.getDoorCount();
            dataChanged = true;
        }
        if (_villageCore.getPopulationCount() != _population) {
            _population = _villageCore.getPopulationCount();
            dataChanged = true;
        }

        return 0 + (geometryChanged ? 1 : 0) + (dataChanged ? 2 : 0);
    }

    /**
     * Increment a player's reputation with this village.
     *
     * @param name Player name
     * @param amount Increment amount
     * @return new reputation
     */
    public int modifyPlayerReputation(String name, Integer amount) {
        int currentRep = 0;
        if (_playerReputations.containsKey(name)) {
            currentRep = _playerReputations.get(name);
        }
        currentRep += amount;
        _playerReputations.put(name, currentRep);
        return currentRep;
    }

    /**
     * Get a player's reputation with this village.
     *
     * @param name Player name
     * @return reputation
     */
    public int getPlayerReputation(String name) {
        if (_playerReputations.containsKey(name)) {
            return _playerReputations.get(name);
        } else {
            return 0;
        }
    }

    /**
     * Get all player reputations
     *
     * @return Map of player names and reputations
     */
    public Map<String, Integer> getPlayerReputations() {
        return _playerReputations;
    }

    /**
     * Get the name of the player with the top reputation
     *
     * @return player name
     */
    public String getTopReputation() {
        String topPlayer = "Nobody";
        int topRep = Integer.MIN_VALUE;
        for (Map.Entry<String, Integer> entry : _playerReputations.entrySet()) {
            if (entry.getValue() > topRep) {
                topPlayer = entry.getKey();
                topRep = entry.getValue();
            }
        }
        return topPlayer;
    }
    
    /**
     * Marks the player as here in this village.
     * @param playerName
     */
    public void setPlayerHere(String playerName) {
        // If not listed as here already, mark here.
        if (!_playerTicksHere.containsKey(playerName)) {
            _playerTicksHere.put(playerName, 0);
        }
        // Player is no longer gone.
        if (_playerTicksGone.containsKey(playerName)) {
            _playerTicksGone.remove(playerName);
        }
    }

    /**
     * Marks the player as not in this village.
     * @param playerName
     */
    public void setPlayerGone(String playerName) {
        // If not listed as gone already, mark gone.
        if (!_playerTicksGone.containsKey(playerName)) {
            _playerTicksGone.put(playerName, 0);
        }
        // Player is no longer here.
        if (_playerTicksHere.containsKey(playerName)) {
            _playerTicksHere.remove(playerName);
        }
    }
    
    public void tickPlayerPresence(String playerName) {
        if (_playerTicksHere.containsKey(playerName)) {
            // If the player is here, increment here counter.
            _playerTicksHere.put(playerName, _playerTicksHere.get(playerName) + 1);
        } else if (_playerTicksGone.containsKey(playerName)) {
            // If the player is gone, increment gone counter.
            _playerTicksGone.put(playerName, _playerTicksGone.get(playerName) + 1);
        }
    }
    
    public boolean isPlayerHere(String playerName) {
        return _playerTicksHere.containsKey(playerName);
    }
    
    public int getPlayerTicksHere(String playerName) {
        if (_playerTicksHere.containsKey(playerName)) {
            return _playerTicksHere.get(playerName);
        } else {
            return 0;
        }
    }
    
    public int getPlayerTicksGone(String playerName) {
        if (_playerTicksGone.containsKey(playerName)) {
            return _playerTicksGone.get(playerName);
        } else {
            return 0;
        }
    }
}
