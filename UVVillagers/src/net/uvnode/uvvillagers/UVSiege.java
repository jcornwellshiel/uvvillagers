package net.uvnode.uvvillagers;

import java.util.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

public class UVSiege {
	Date _start;
	long _gamestart;
	int _minX;
	int _maxX;
	int _minY;
	int _maxY;
	int _minZ;
	int _maxZ;
	ArrayList<Entity> _spawns = new ArrayList<Entity>();
	ArrayList<Integer> _spawnIds = new ArrayList<Integer>();
	Map<String, Integer> _players = new HashMap<String, Integer>();
	
	UVSiege(long gamestart, Date start) {
		_start = start;
		_gamestart = gamestart;
	}
	
	UVSiege(long gamestart) {
		this(gamestart, new Date());
	}

	public void addSpawn(LivingEntity entity) {
		_spawns.add(entity);
		_spawnIds.add(entity.getEntityId());
	}
	
	public void sendOverview(CommandSender sender) {
		sender.sendMessage("Siege started at " + _gamestart);
		sender.sendMessage(" - Location: " + _minX + ", " + _minY + ", " + _minZ);
		sender.sendMessage(" - Zombies: " + _spawns.size());
		sender.sendMessage(" - Players: " + _players.size() + _players.toString());
	}

	public boolean checkEntityId(int entityId) {
		return _spawnIds.contains(entityId);
	}

	public void addPlayerKill(String name) {
		if (_players.containsKey(name))
			_players.put(name, _players.get(name) + 1);
		else 
			_players.put(name, 1);
	}

	public int getPlayerKills(String name) {
		if (_players.containsKey(name))
			return _players.get(name);
		else 
			return 0;
	}
}
