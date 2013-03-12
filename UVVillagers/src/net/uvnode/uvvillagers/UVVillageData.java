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
	
	public void setVillage(Village village) {
		_village = village;
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
}
