package net.uvnode.uvvillagers;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.bukkit.entity.LivingEntity;

/**
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
	 * @param entity
	 */
	public void addSpawn(LivingEntity entity) {
		//_spawns.add(entity);
		//_spawnIds.add(entity.getEntityId());
		_spawns.put(entity.getEntityId(), entity);
		_mobsLeft++;
	}

	/**
	 * @param entityId
	 * @return
	 */
	public boolean checkEntityId(int entityId) {
//		return _spawnIds.contains(entityId);
		return _spawns.containsKey(entityId);
	}

	/**
	 * @param name
	 * @param value
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
	 * @param name
	 * @return
	 */
	public int getPlayerKills(String name) {
		if (_players.containsKey(name))
			return _players.get(name);
		else 
			return 0;
	}
	
	/**
	 * @param name
	 * @return
	 */
	public int getPlayerPoints(String name) {
		if (_playerPoints.containsKey(name))
			return _playerPoints.get(name);
		else 
			return 0;
	}
	
	/**
	 * @return
	 */
	public UVVillage getVillage() {
		return _village;
	}
	
	/**
	 * @return
	 */
	public ArrayList<String> overviewMessage() {
		ArrayList<String> msgs = new ArrayList<String>();
		
		msgs.add("Zombie Siege Stats:");
		msgs.add(" - Location: " + _village.getName());
		msgs.add(" - Enemies: " + _spawns.size() + " (" + _mobsLeft + " still alive)");
		msgs.add(" - Player Kills: ");
		Iterator<String> playersIterator = _players.keySet().iterator();
		while (playersIterator.hasNext()) {
			String playerName = playersIterator.next();
			msgs.add("   - " + playerName + ": " + _players.get(playerName));
		}
		return msgs;
	}

	/**
	 * 
	 */
	public void killMob() {
		_mobsLeft--;
	}
	
	/**
	 * @return
	 */
	public int getNumMobsLeft() {
		return _mobsLeft;
	}
	
	/**
	 * @return
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
