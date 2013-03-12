/**
 * 
 */
package net.uvnode.uvvillagers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import net.minecraft.server.v1_4_R1.Village;
import net.minecraft.server.v1_4_R1.WorldServer;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
//import org.bukkit.potion.PotionEffect;
//import org.bukkit.potion.PotionEffectType;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Skeleton.SkeletonType;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;

import org.bukkit.craftbukkit.v1_4_R1.CraftWorld;

//import lib.PatPeter.SQLibrary.*;

/**
 * @author James Cornwell-Shiel
 *
 * Adds village tributes and additional zombie siege functionality.
 * 
 */
public final class UVVillagers extends JavaPlugin implements Listener {
	
	WorldServer _worldserver;
	// Initialize null active siege
	UVSiege _activeSiege = null;
	Random rng = new Random();

	Map<String, UVVillageData> _villageData = new HashMap<String, UVVillageData>();
	
	Map<String,Integer> _playerVillagesProximity = new HashMap<String, Integer>(); 
	Map<EntityType,Integer> _populationThresholds = new HashMap<EntityType, Integer>(); 
	Map<EntityType,Integer> _killValues = new HashMap<EntityType, Integer>();
	Map<EntityType,Integer> _chanceOfExtraMobs = new HashMap<EntityType, Integer>();
	Map<EntityType,Integer> _maxExtraMobs = new HashMap<EntityType, Integer>();
	
	// Configuration Settings
	UVTributeMode tributeMode;

	int tributeRange, 
		villagerCount, 
		minPerVillagerCount, 
		maxPerVillagerCount, 
		baseSiegeBonus, 
		minPerSiegeKill,
		maxPerSiegeKill,
		spawnSpread,
		spawnVSpread,
		_chanceOfExtraWitherSkeleton,
		_witherSkeletonPopulationThreshold,
		_maxExtraWitherSkeletons,
		_killValueWitherSkeletons; // Wither skeletons are a subtype of skeleton at the moment, so can't use the _chanceOfExtraMobs hashmap. 
	
	boolean emeraldsAtDawnRunning = false;
	
	@Override
	public void onEnable() {

		_worldserver = ((CraftWorld) getServer().getWorlds().get(0)).getHandle();
		
		getServer().getPluginManager().registerEvents(this, this);

		saveDefaultConfig();
		loadConfig();
		startDayTimer();
	}

	@Override
	public void onDisable() {
		//updateConfig();
	}
	
	/**
	 * Starts a timer that throws dawn/dusk events.
	 */
	public void startDayTimer() {
		// Step through worlds every 20 ticks and throw UVTimeEvents for various times of day.
		getServer().getScheduler().runTaskTimerAsynchronously(this, new Runnable() {
			@Override
			public void run() {
				List<World> worlds = getServer().getWorlds();
				for (int i = 0; i < worlds.size(); i++) {
					if (worlds.get(i).getTime() >= 0 && worlds.get(i).getTime() < 20) {
						UVTimeEvent event = new UVTimeEvent(worlds.get(i), UVTimeEventType.DAWN);
						getServer().getPluginManager().callEvent(event);
					}
					if (worlds.get(i).getTime() >= 12500 && worlds.get(i).getTime() < 12520) {
						UVTimeEvent event = new UVTimeEvent(worlds.get(i), UVTimeEventType.DUSK);
						getServer().getPluginManager().callEvent(event);
					}
				}
			}
		}, 0, 20);
	}
	
	/**
	 * Loads the plugin configuration.
	 */
	private void loadConfig() {
		tributeRange = getConfig().getInt("tributeRange", 64);
		villagerCount = getConfig().getInt("villagerCount", 20);
		minPerVillagerCount = getConfig().getInt("minPerVillagerCount", 0);
		maxPerVillagerCount = getConfig().getInt("maxPerVillagerCount", 3);
		baseSiegeBonus = getConfig().getInt("baseSiegeBonus", 1);
		minPerSiegeKill = getConfig().getInt("minPerSiegeKill", 1);
		maxPerSiegeKill = getConfig().getInt("maxPerSiegeKill", 2);
		spawnSpread = getConfig().getInt("randomSpread", 1);
		spawnVSpread = getConfig().getInt("randomVerticalSpread", 2);
		
		loadMobData();
		loadVillageData();
		getLogger().info("Configuration loaded.");
	}

	

	/**
	 * Loads the configuration data for mob spawn rate, etc.
	 */
	private void loadMobData() {
		// Load Mob Spawn Chances
		_chanceOfExtraMobs.put(EntityType.ZOMBIE, getConfig().getInt("mobs.zombie.chance", 100));
		_populationThresholds.put(EntityType.ZOMBIE, getConfig().getInt("mobs.zombie.threshold", 1));
		_maxExtraMobs.put(EntityType.ZOMBIE, getConfig().getInt("mobs.zombie.max", 100));
		_killValues.put(EntityType.ZOMBIE, getConfig().getInt("mobs.zombie.value", 1));

		_chanceOfExtraMobs.put(EntityType.SKELETON, getConfig().getInt("mobs.skeleton.chance", 0));
		_populationThresholds.put(EntityType.SKELETON, getConfig().getInt("mobs.skeleton.threshold", 20));
		_maxExtraMobs.put(EntityType.SKELETON, getConfig().getInt("mobs.skeleton.max", 50));
		_killValues.put(EntityType.SKELETON, getConfig().getInt("mobs.skeleton.value", 1));

		_chanceOfExtraMobs.put(EntityType.SPIDER, getConfig().getInt("mobs.spider.chance", 0));
		_populationThresholds.put(EntityType.SPIDER, getConfig().getInt("mobs.spider.threshold", 20));
		_maxExtraMobs.put(EntityType.SPIDER, getConfig().getInt("mobs.spider.max", 50));
		_killValues.put(EntityType.SPIDER, getConfig().getInt("mobs.spider.value", 1));

		_chanceOfExtraMobs.put(EntityType.CAVE_SPIDER, getConfig().getInt("mobs.cave_spider.chance", 0));
		_populationThresholds.put(EntityType.CAVE_SPIDER, getConfig().getInt("mobs.cave_spider.threshold", 20));
		_maxExtraMobs.put(EntityType.CAVE_SPIDER, getConfig().getInt("mobs.cave_spider.max", 50));
		_killValues.put(EntityType.CAVE_SPIDER, getConfig().getInt("mobs.cave_spider.value", 5));

		_chanceOfExtraMobs.put(EntityType.PIG_ZOMBIE, getConfig().getInt("mobs.pig_zombie.chance", 0));
		_populationThresholds.put(EntityType.PIG_ZOMBIE, getConfig().getInt("mobs.pig_zombie.threshold", 20));
		_maxExtraMobs.put(EntityType.PIG_ZOMBIE, getConfig().getInt("mobs.pig_zombie.max", 50));
		_killValues.put(EntityType.PIG_ZOMBIE, getConfig().getInt("mobs.pig_zombie.value", 5));

		_chanceOfExtraMobs.put(EntityType.BLAZE, getConfig().getInt("mobs.blaze.chance", 0));
		_populationThresholds.put(EntityType.BLAZE, getConfig().getInt("mobs.blaze.threshold", 20));
		_maxExtraMobs.put(EntityType.BLAZE, getConfig().getInt("mobs.blaze.max", 50));
		_killValues.put(EntityType.BLAZE, getConfig().getInt("mobs.blaze.value", 10));

		_chanceOfExtraMobs.put(EntityType.WITCH, getConfig().getInt("mobs.witch.chance", 0));
		_populationThresholds.put(EntityType.WITCH, getConfig().getInt("mobs.witch.threshold", 20));
		_maxExtraMobs.put(EntityType.WITCH, getConfig().getInt("mobs.witch.max", 50));
		_killValues.put(EntityType.WITCH, getConfig().getInt("mobs.witch.value", 10));

		_chanceOfExtraMobs.put(EntityType.MAGMA_CUBE, getConfig().getInt("mobs.magma_cube.chance", 0));
		_populationThresholds.put(EntityType.MAGMA_CUBE, getConfig().getInt("mobs.magma_cube.threshold", 20));
		_maxExtraMobs.put(EntityType.MAGMA_CUBE, getConfig().getInt("mobs.magma_cube.max", 50));
		_killValues.put(EntityType.MAGMA_CUBE, getConfig().getInt("mobs.magma_cube.value", 10));

		_chanceOfExtraWitherSkeleton = getConfig().getInt("mobs.wither_skeleton.chance", 0);
		_witherSkeletonPopulationThreshold = getConfig().getInt("mobs.wither_skeleton.threshold", 20);
		_maxExtraWitherSkeletons = getConfig().getInt("wither_skeleton.max", 50);
		_killValueWitherSkeletons = getConfig().getInt("mobs.wither_skeleton.value", 10);

		_chanceOfExtraMobs.put(EntityType.ENDERMAN, getConfig().getInt("mobs.enderman.chance", 0));
		_populationThresholds.put(EntityType.ENDERMAN, getConfig().getInt("mobs.enderman.threshold", 20));
		_maxExtraMobs.put(EntityType.ENDERMAN, getConfig().getInt("mobs.enderman.max", 50));
		_killValues.put(EntityType.ENDERMAN, getConfig().getInt("mobs.enderman.value", 10));

		_chanceOfExtraMobs.put(EntityType.GHAST, getConfig().getInt("mobs.ghast.chance", 0));
		_populationThresholds.put(EntityType.GHAST, getConfig().getInt("mobs.ghast.threshold", 20));
		_maxExtraMobs.put(EntityType.GHAST, getConfig().getInt("mobs.ghast.max", 50));
		_killValues.put(EntityType.GHAST, getConfig().getInt("mobs.ghast.value", 100));
		
		_chanceOfExtraMobs.put(EntityType.WITHER, getConfig().getInt("mobs.wither.chance", 0));
		_populationThresholds.put(EntityType.WITHER, getConfig().getInt("mobs.wither.threshold", 20));
		_maxExtraMobs.put(EntityType.WITHER, getConfig().getInt("mobs.wither.max", 50));
		_killValues.put(EntityType.WITHER, getConfig().getInt("mobs.wither.value", 500));
		
		_chanceOfExtraMobs.put(EntityType.ENDER_DRAGON, getConfig().getInt("mobs.ender_dragon.chance", 0));
		_populationThresholds.put(EntityType.ENDER_DRAGON, getConfig().getInt("mobs.ender_dragon.threshold", 20));
		_maxExtraMobs.put(EntityType.ENDER_DRAGON, getConfig().getInt("mobs.ender_dragon.max", 50));
		_killValues.put(EntityType.ENDER_DRAGON, getConfig().getInt("mobs.ender_dragon.value", 500));
	}


	/**
	 * Loads the configuration data for villages and player-village reputations.
	 */
	private void loadVillageData() {
		// Load Village Data
		_villageData.clear();
		try {
			Map<String, Object> villageList = getConfig().getConfigurationSection("villages").getValues(false);
			for(Map.Entry<String, Object> villageEntry : villageList.entrySet()) {
				ConfigurationSection villageConfigSection = (ConfigurationSection) villageEntry.getValue();
				Map<String, Object> playersMap = villageConfigSection.getConfigurationSection("pr").getValues(false);

				World w = getServer().getWorld(villageConfigSection.getString("world"));
				int x = villageConfigSection.getInt("x");
				int y = villageConfigSection.getInt("y");
				int z = villageConfigSection.getInt("z");

				UVVillageData v = new UVVillageData(w, x, y, z);
				for(Map.Entry<String, Object> playerEntry : playersMap.entrySet()) {
					String playerName = playerEntry.getKey();
					int playerRep = (Integer) playerEntry.getValue();
					v._playerReputations.put(playerName, playerRep);
				}
				_villageData.put(villageEntry.getKey(), v);
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Updates the configuration file and saves it.
	 */
	private void updateConfig() {
		this.getConfig().createSection("villages", saveVillageData());

		saveConfig();
	}

	/**
	 * Dumps the village data into a config-friendly hashmap.
	 * @return village data hashmap
	 */
	private  Map<String, Object> saveVillageData() {
		// Save Village Data
		Map<String, Object> map = new HashMap<String, Object>();
		for(Map.Entry<String, UVVillageData> vdata : _villageData.entrySet())
		{
			Map<String, Object> v = new HashMap<String, Object>();
			v.put("world", vdata.getValue()._world.getName());
			v.put("x", vdata.getValue()._centerX);
			v.put("y", vdata.getValue()._centerY);
			v.put("z", vdata.getValue()._centerZ);
			v.put("pr", vdata.getValue()._playerReputations);
			map.put(vdata.getKey(), v);
		}
		getLogger().info(map.toString());
		return map;
	}

	/**
	 * Command listener
	 */
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
		if(cmd.getName().equalsIgnoreCase("uvv")){
			if(args.length > 0) {
				if (args[0].equalsIgnoreCase("reload")) {
					if (sender instanceof Player) {
						Player p = (Player) sender;
						if (p.hasPermission("uvv.reload")) {
							sender.sendMessage("Reloading...");
							loadConfig();
						} else 
							sender.sendMessage("You don't have permission to do that.");
					} else {
						sender.sendMessage("Reloading...");
						loadConfig();
					}
				} else if (args[0].equalsIgnoreCase("save")) {
					if (sender instanceof Player) {
						Player p = (Player) sender;
						if (p.hasPermission("uvv.save")) {
							sender.sendMessage("Saving...");
							updateConfig();
						} else 
							sender.sendMessage("You don't have permission to do that.");
					} else {
						sender.sendMessage("Saving...");
						updateConfig();
					}
				} else if (args[0].equalsIgnoreCase("list")) {
					if (sender instanceof Player) {
						Player p = (Player) sender;
						if (p.hasPermission("uvv.list")) {
							sender.sendMessage("Saving...");
							loadConfig();
						} else 
							sender.sendMessage("You don't have permission to do that.");
					} else {
						sender.sendMessage("Saving...");
						loadConfig();
					}
				} else if (args[0].equalsIgnoreCase("nearest")) {
					if (sender instanceof Player) {
						Player p = (Player) sender;
						if (p.hasPermission("uvv.nearest")) {
							sender.sendMessage("Saving...");
							loadConfig();
						} else 
							sender.sendMessage("You don't have permission to do that.");
					} else {
						sender.sendMessage("Saving...");
						loadConfig();
					}
				} else if (args[0].equalsIgnoreCase("checkrange")) {
					if (sender instanceof Player) {
						Player p = (Player) sender;
						if (p.hasPermission("uvv.checkrange")) {
							sender.sendMessage("Saving...");
							loadConfig();
						} else 
							sender.sendMessage("You don't have permission to do that.");
					} else {
						sender.sendMessage("Saving...");
						loadConfig();
					}
				}
			} else {
				sender.sendMessage("- UVVillagers -");
				@SuppressWarnings("unchecked")
				List<Village> villages = _worldserver.villages.getVillages();
				sender.sendMessage(villages.size() + " villages are loaded.");
				if (sender instanceof Player) {
					Location pLoc = ((Player)sender).getLocation();
					
					Village closest = _worldserver.villages.getClosestVillage(pLoc.getBlockX(), pLoc.getBlockY(), pLoc.getBlockZ(), tributeRange);
					if (closest != null) {
						sender.sendMessage("Closest village at " + closest.getCenter().x + ", " + closest.getCenter().y + ", " + closest.getCenter().z);
						sender.sendMessage(" - Population: " + closest.getPopulationCount() + ", Size: " + closest.getSize() + ", Doors: " + closest.getDoorCount());
					} else
						sender.sendMessage("No villages nearby.");
					UVVillageData uv = getUVVillageData(pLoc, tributeRange);
					if (uv != null)
						sender.sendMessage("Your reputation is " + uv.getPlayerReputation(sender.getName()));
				} else {
					printAllVillageData(sender);
				}
			}
			return true;
		}
		return false;
	}
	/**
	 * Sends village data to sender
	 * @param sender CommandSender
	 */
	private void printAllVillageData(CommandSender sender) {
		Iterator<String> vIterator = _villageData.keySet().iterator();
		while (vIterator.hasNext()) {
			String key = vIterator.next();
			UVVillageData uv = _villageData.get(key);
			sender.sendMessage(key);
			sender.sendMessage("Stored village at " + uv.getLocation().getBlockX() + ", " + uv.getLocation().getBlockY() + ", " + uv.getLocation().getBlockZ() + " has " + uv._playerReputations.toString());
			Village closest = _worldserver.villages.getClosestVillage(uv.getLocation().getBlockX(), uv.getLocation().getBlockY(), uv.getLocation().getBlockZ(), 16);
			if (closest != null) {
				sender.sendMessage("Closest village at " + closest.getCenter().x + ", " + closest.getCenter().y + ", " + closest.getCenter().z);
				sender.sendMessage(" - Population: " + closest.getPopulationCount() + ", Size: " + closest.getSize() + ", Doors: " + closest.getDoorCount());
			}
			sender.sendMessage(uv.toString());
		}
	}

	/**
	 * Player move listener
	 * @param event PlayerMoveEvent
	 */
	@EventHandler
	public void playerMove(PlayerMoveEvent event) {
		// Kick out if not on primary world
		if (event.getTo().getWorld() != getServer().getWorlds().get(0))
			return;
		// Get closest village
		Village v = _worldserver.villages.getClosestVillage(
				event.getTo().getBlockX(),
				event.getTo().getBlockY(),
				event.getTo().getBlockZ(),
				tributeRange);
		// Get player name
		String name = event.getPlayer().getName();
		// If a village is nearby
		if (v != null) {
			// Do we have a UVVillageData object for this village?
			UVVillageData villageData = getUVVillageData(event.getTo().getWorld(), v.getCenter().x, v.getCenter().y, v.getCenter().z, v.getSize());
			if (villageData == null) {
				villageData = addUVVillageData(event.getTo().getWorld(), v.getCenter().x, v.getCenter().y, v.getCenter().z, v, name);
				event.getPlayer().sendMessage("You discovered a new village!");
			}
			// Do we have a record for this player?
			if (_playerVillagesProximity.containsKey(name)) {
				// If so, check to see if we've already alerted the player that he's near this village
				int current = _playerVillagesProximity.get(name);
				if (v.hashCode() != current) {
					// No? Alert him!
					event.getPlayer().sendMessage("You're near a village! Your popularity with the " + v.getPopulationCount() + " villagers here is " + villageData.getPlayerReputation(name));
					// Set the player as near this village
					_playerVillagesProximity.put(name, v.hashCode());
				}
			} else {
				// if no record, alert him!
				event.getPlayer().sendMessage("You're near a village! Your popularity with the " + v.getPopulationCount() + " villagers here is " + villageData.getPlayerReputation(name));				
				// Set the player as near this village
				_playerVillagesProximity.put(name, v.hashCode());
			}

		} else {
			// The player has left the village!
			_playerVillagesProximity.put(name, -1);
		}
	}
		
	private UVVillageData addUVVillageData(World world, int x, int y, int z, Village v, String name) {
		UVVillageData uv = new UVVillageData(world, x, y, z);
		uv.setVillage(v);
		uv.modifyPlayerReputation(name, 10);
		String key = "village_" + x  + "_" + y + "_" + z;
		_villageData.put(key, uv);
		UVVillageEvent event = new UVVillageEvent(uv, key, UVVillageEventType.DISCOVERED);
		getServer().getPluginManager().callEvent(event);
		return uv;
	}

	private UVVillageData getUVVillageData(World world, int x, int y, int z, int distance) {
		Location location = new Location(world, x, y, z);
		return getUVVillageData(location, distance);
	}
	
	private UVVillageData getUVVillageData(Location location, int distance) {
		Iterator<String> vname = _villageData.keySet().iterator();
		while (vname.hasNext()) {
			UVVillageData uv = _villageData.get(vname.next());
			if (uv.getLocation().distanceSquared(location) < (distance * distance)) {
				return uv;
			}
		}
		return null;
	}

	@EventHandler
	public void creatureSpawn(CreatureSpawnEvent event) {
		switch(event.getSpawnReason()) {
			case VILLAGE_INVASION: 
				// VILLAGE_INVASION is only triggered in a zombie siege.
				// TO DO - ADD METHOD THAT MAKES SURE THAT ZOMBIES SPAWN AT VILLAGE HEIGHT - thebloodline has his village floating 3 blocks over land, and all the zombies spawn underneath.
				// If we haven't already registered this siege, create a new siege tracking object
				if (_activeSiege == null) {
					getServer().broadcastMessage("A zombie siege has begun!!!");
					Village v = _worldserver.villages.getClosestVillage(
							event.getEntity().getLocation().getBlockX(),
							event.getEntity().getLocation().getBlockY(),
							event.getEntity().getLocation().getBlockZ(),
							32);
					_activeSiege = new UVSiege(event.getEntity().getWorld().getTime(), v);
//					PotionEffect p = new PotionEffect(PotionEffectType.INCREASE_DAMAGE, rng.nextInt(180), rng.nextInt(5));
//					event.getEntity().addPotionEffect(p);
					
					// Spawn bonus mobs!
					spawnMoreMobs(event.getLocation(), v);
				}
				
				// Add this spawn to the siege's mob list for kill tracking
				_activeSiege.addSpawn(event.getEntity());
				break;
			default:
				break;
		}
	}
		
	private void spawnMoreMobs(Location location, Village village) {
//		UVVillageData villageData = getUVVillageData(location.getWorld(), village.getCenter().x, village.getCenter().y, village.getCenter().z);
		// If configured to, randomly spawn extra zombies! 
		// Eventually this will be replaced by a method that handles spawning a variety of mob types based on village size
		int population = village.getPopulationCount();
		trySpawn(location, population, EntityType.ZOMBIE, null);
		trySpawn(location, population, EntityType.SKELETON, SkeletonType.NORMAL);
		trySpawn(location, population, EntityType.SKELETON, SkeletonType.WITHER);
		trySpawn(location, population, EntityType.SPIDER, null);
		trySpawn(location, population, EntityType.CAVE_SPIDER, null);
		trySpawn(location, population, EntityType.MAGMA_CUBE, null);
		trySpawn(location, population, EntityType.WITCH, null);
		trySpawn(location, population, EntityType.PIG_ZOMBIE, null);
		trySpawn(location, population, EntityType.BLAZE, null);
		trySpawn(location, population, EntityType.GHAST, null);
		trySpawn(location, population, EntityType.WITHER, null);
	}

	private void trySpawn(Location location, int population, EntityType type, SkeletonType skeletonType) {
		int threshold, chance;
		if (skeletonType != null) {
			threshold = _witherSkeletonPopulationThreshold;
			chance = _chanceOfExtraWitherSkeleton;
		} else {
			threshold = getPopulationThreshold(type);
			chance = getExtraMobChance(type);
		}
		if (population >= threshold) {
			if (getExtraMobChance(type) > 1) {
				// Generate a random number of extra zombies to possibly spawn
				int count = rng.nextInt((int)(population / threshold)) + 1;
				for (int i = 0; i < count; i++) {
					// Randomly decide whether this zombie will spawn.
					if (chance > rng.nextInt(100)) {
						if(skeletonType == SkeletonType.WITHER) {
							Skeleton spawn = (Skeleton) location.getWorld().spawnEntity(location, type);
							spawn.setSkeletonType(SkeletonType.WITHER);
							_activeSiege.addSpawn(spawn);
						}
						else
							_activeSiege.addSpawn((LivingEntity) location.getWorld().spawnEntity(location, type));
					}
				}
			}
		}
		
	}

	private int getExtraMobChance(EntityType type) {
		if (_chanceOfExtraMobs.containsKey(type))
			return _chanceOfExtraMobs.get(type);
		else 
			return 0;
	}
	
	private int getPopulationThreshold(EntityType type) {
		if (_populationThresholds.containsKey(type))
			return _populationThresholds.get(type);
		else 
			return 999;
	}

	@EventHandler
	public void entityDeath(EntityDeathEvent event) {
		// If there's an active siege being tracked, check to see if the mob killed is part of the siege 
		if (_activeSiege != null) {
			if (_activeSiege.checkEntityId(event.getEntity().getEntityId())) {
				// If so and it was killed by a player, log the kill for rewards at dawn.
				if(event.getEntity().getKiller() != null)
					_activeSiege.addPlayerKill(event.getEntity().getKiller().getName(), getKillValue(event.getEntityType()));
			}
		}
	}
		
	private Integer getKillValue(EntityType entityType) {
		if (_killValues.containsKey(entityType))
			return _killValues.get(entityType);
		else 
			return 0;
	}

	@EventHandler
	public void emeraldsAtDawn(UVTimeEvent event) {
		if (!emeraldsAtDawnRunning) {
			emeraldsAtDawnRunning = true;
		}
		switch(event.getType()) {
			case DAWN:
				// TO-DO: ADD MULTIWORLD SUPPORT!
				// Kick us out of this if we're not dealing with the main world.
				if (event.getWorld().getName() != _worldserver.getWorld().getName()) { return; }
					
				//getServer().broadcastMessage("Dawn has arrived in world " + event.getWorld().getName() + "!");

				//Map<String, Integer> playerVillagerCounts = new HashMap<String, Integer>();
				List<Player> players = event.getWorld().getPlayers();
				
				@SuppressWarnings("unchecked")
				List<Village> villages = _worldserver.villages.getVillages();
				Iterator<Player> playerIterator = players.iterator();
				
				// Step through the players to calculate tribute 
				while (playerIterator.hasNext()) {
					Player p = playerIterator.next();
					Iterator<Village> villageIterator = villages.iterator();
					
					int tributeAmount = 0, killBonus = 0, numVillagesNearby = 0;
					Random rng = new Random();

					// Calculate bonus from kills
					if (_activeSiege != null) {
						int kills = _activeSiege.getPlayerKills(p.getName());
						for (int i = 0; i < kills; i++) {
							killBonus += rng.nextInt(maxPerSiegeKill + 1 - minPerSiegeKill) + minPerSiegeKill;
						}
					}

					// Step through the villages. Add their tributes if they're close enough.
					while (villageIterator.hasNext()) {
						int villageTributeAmount = 0;
						Village v = villageIterator.next();
						Location loc = new Location(event.getWorld(), v.getCenter().x, v.getCenter().y, v.getCenter().z);
						// Get UV village data object
						UVVillageData uv = getUVVillageData(loc, tributeRange);
						p.getName();
						// Check village distance from player
						if (p.getLocation().distanceSquared(loc) < (tributeRange+v.getSize()) * (tributeRange+v.getSize())) {
							numVillagesNearby++;
							// Check population size
							int population = v.getPopulationCount();
							if (population > 0 && _activeSiege != null) {
								// Update reputation if villagers survived
								uv.modifyPlayerReputation(p.getName(), _activeSiege.getPlayerPoints(p.getName()));
							}
							if (population > 20) {
								// Give a random bonus per 20 villagers
								for (int i = 0; i < (int)(population / villagerCount); i++) {
									villageTributeAmount += rng.nextInt(maxPerVillagerCount + 1 - minPerVillagerCount) + minPerVillagerCount + baseSiegeBonus;
								}
								// If this village was the one sieged, give the kill bonus and an extra survival thankfulness bonus
								if (_activeSiege != null && _activeSiege.getVillage() != null && _activeSiege.getVillage().hashCode() == v.hashCode()) {
									villageTributeAmount += 1 * (int)(population / villagerCount) + killBonus;
								}
							}
						}
						// Add the tribute from this village to the total owed
						tributeAmount += villageTributeAmount;
					}
					// TO-DO: Save the tribute amount for each player/village so that receive it next time the player talks to a villager in that village.
					// This will force players to interact with every village that they are to get credit for.
					// But for now... just award the tribute directly.
					if (numVillagesNearby > 0) {
						if (tributeAmount > 0) {
							ItemStack items = new ItemStack(Material.EMERALD, tributeAmount);
							p.getInventory().addItem(items);
							p.sendMessage("Grateful villagers gave you " + tributeAmount + " emeralds!");
							getLogger().info(p.getName() + " received " + tributeAmount + " emeralds.");						
						} else
							p.sendMessage("The villagers didn't have any emeralds for you today.");
					} else
						p.sendMessage("You weren't near any villages large enough to pay you tribute.");
				}
				if (_activeSiege != null) {
					ArrayList<String> messages = _activeSiege.overviewMessage();
					Iterator<String> messageIterator = messages.iterator();
					while (messageIterator.hasNext())
						getServer().broadcastMessage(messageIterator.next());
				}
				break;
			case DUSK:
				// TO-DO: ADD MULTIWORLD SUPPORT!
				// Kick us out of this if we're not dealing with the main world.
				if (event.getWorld().getName() != _worldserver.getWorld().getName()) { return; }

				//getServer().broadcastMessage("Dusk has arrived in world " + event.getWorld().getName() + "!");

				// TO-DO: clear pending tributes list
				// clear active siege
				_activeSiege = null;
				break;
			default:
				break;
		}
		emeraldsAtDawnRunning = false;
	}


	public Map<String, UVVillageData> getVillages() {
		return _villageData;
	}
}
