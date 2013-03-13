package net.uvnode.uvvillagers;


import java.util.HashMap;
import java.util.Map;

import net.minecraft.server.v1_4_R1.Village;

import org.bukkit.Location;

public class UVVillage {
	private int _doors;
	private Location _location;
	private Map<String, Integer> _playerReputations = new HashMap<String, Integer>();
	private int _population;
	private int _size;
	private Village _villageCore;
	private String _name;
	
	/**
	 * @param location
	 * @param village
	 */
	public UVVillage(Location location, Village village) {
		_location = location;
		_villageCore = village;
		if(_villageCore != null) {
			updateVillageDataFromCore();
		}
	}
	
	/**
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
	 * @return
	 */
	public int getDoors() {
		return _doors;
	}
	
	/**
	 * @return
	 */
	public String getName() {
		return _name;
	}
	
	/**
	 * @return
	 */
	public Location getLocation() {
		return _location;
	}

	/**
	 * @return
	 */
	public int getPopulation() {
		return _population;
	}

	/**
	 * @return
	 */
	public int getSize() {
		return _size;
	}

	/**
	 * @return
	 */
	public Village getVillageCore() {
		return _villageCore;
	}

	/**
	 * @param name
	 */
	public void setName(String name) {
		_name = name;
	}

	/**
	 * @param doors
	 */
	public void setDoors(int doors) {
		_doors = doors;
	}

	/**
	 * @param location
	 */
	public void setLocation(Location location) {
		_location = location;
	}

	/**
	 * @param population
	 */
	public void setPopulation(int population) {
		_population = population;
	}

	/**
	 * @param size
	 */
	public void setSize(int size) {
		_size = size;
	}

	/**
	 * @param villageCore
	 */
	public void setVillageCore(Village villageCore) {
		_villageCore = villageCore;
	}
	
	/**
	 * @return
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
		
		return 0 + (geometryChanged?1:0) + (dataChanged?2:0);
	}

	/**
	 * @param name
	 * @param amount
	 * @return
	 */
	public int modifyPlayerReputation(String name, Integer amount) {
		int currentRep = 0;
		if (_playerReputations.containsKey(name))
			currentRep = _playerReputations.get(name);
		currentRep += amount;
		_playerReputations.put(name, currentRep);
		return currentRep;
	}
	
	/**
	 * @param name
	 * @return
	 */
	public int getPlayerReputation(String name) {
		if (_playerReputations.containsKey(name))
			return _playerReputations.get(name);
		else
			return 0;
	}

	/**
	 * @return
	 */
	public Map<String, Integer> getPlayerReputations() {
		return _playerReputations;
	}
	
	public String getTopReputation() {
		String topPlayer = "Nobody";
		int topRep = Integer.MIN_VALUE;
		for(Map.Entry<String, Integer> entry : _playerReputations.entrySet()) {
			if (entry.getValue() > topRep) {
				topPlayer = entry.getKey();
				topRep = entry.getValue();
			}
		}
		return topPlayer;
	}
}
