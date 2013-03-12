package net.uvnode.uvvillagers;

import java.util.HashMap;
import java.util.Map;


import net.minecraft.server.v1_4_R1.Village;

import org.bukkit.Location;
import org.bukkit.World;

public class UVVillageData {
	
	World _world;
	int _centerX;
	int _centerY;
	int _centerZ;
	int _size;
	int _doors;
	int _population;
	Location _location;
	Map<String, Integer> _playerReputations = new HashMap<String, Integer>();
	Village _village;

	public UVVillageData(World world, int centerX, int centerY, int centerZ) {
		_world = world;
		_centerX = centerX;
		_centerY = centerY;
		_centerZ = centerZ;
		_location = new Location(world, centerX, centerY, centerZ);
		_village = null;
		_size = 32;
		_doors = 0;
		_population = 0;
	}
	
	public int getDoors() {
		return _doors;
	}

	public int getPopulation() {
		return _population;
	}

	public int modifyPlayerReputation(String name, Integer amount) {
		int currentRep = 0;
		if (_playerReputations.containsKey(name))
			currentRep = _playerReputations.get(name);
		currentRep += amount;
		_playerReputations.put(name, currentRep);
		return currentRep;
	}
	
	public int getPlayerReputation(String name) {
		if (_playerReputations.containsKey(name))
			return _playerReputations.get(name);
		else
			return 0;
	}
	
	public Village getVillage() { 
		return _village;
	}
	
	/**
	 * @param village
	 * @return 0: No changes detected. 1: geometry changed. 2: data changed. 3: both changed.
	 */
	public int setVillage(Village village) {
		_village = village;
		boolean geometryChanged = false, dataChanged = false;
		if (_centerX != village.getCenter().x || _centerY != village.getCenter().y || _centerZ != village.getCenter().z) {			
			_centerX = village.getCenter().x;
			_centerY = village.getCenter().y;
			_centerZ = village.getCenter().z;
			_location.setX(_centerX);
			_location.setY(_centerY);
			_location.setZ(_centerZ);
			geometryChanged = true;
		}
			
		if (village.getSize() != _size) {
			_size = village.getSize();
			geometryChanged = true;			
		}
		if (village.getSize() != _size) {
			_doors = village.getDoorCount();
			dataChanged = true;
		}
		if (village.getSize() != _size) {
			_population = village.getPopulationCount();
			dataChanged = true;
		}		
		
		return 0 + (geometryChanged?1:0) + (dataChanged?2:0);
	}

	public Location getLocation() {
		return _location;
	}

	public int getSize() {
		return _size;
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

	public void checkData() {
		if (_village != null) {
			
		}
		
	}
}
