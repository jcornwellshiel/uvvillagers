package net.uvnode.uvvillagers;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Skeleton.SkeletonType;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;

/**
 * Manages Zombie Siege (Village Siege) enhancements and events.
 * @author James Cornwell-Shiel
 */
public class SiegeManager {
	private UVVillagers _plugin;
	private UVSiege _currentSiege;
	
	private Map<EntityType,Integer> _populationThresholds = new HashMap<EntityType, Integer>(); 
	private Map<EntityType,Integer> _killValues = new HashMap<EntityType, Integer>();
	private Map<EntityType,Integer> _chanceOfExtraMobs = new HashMap<EntityType, Integer>();
	private Map<EntityType,Integer> _maxExtraMobs = new HashMap<EntityType, Integer>();
	@SuppressWarnings("unused")
	private int _spawnSpread;
	@SuppressWarnings("unused")
	private int _spawnVSpread;
	private int _chanceOfExtraWitherSkeleton;
	private int _witherSkeletonPopulationThreshold;
	private int _maxExtraWitherSkeletons;
	private int _killValueWitherSkeletons;
	/**
	 * Basic Constructor
	 * @param plugin The UVVillagers plugin
	 */
	public SiegeManager(UVVillagers plugin) {
		_plugin = plugin;
		_currentSiege = null;
	}
	
	/**
	 * Loads the config settings from a configuration section.
	 * @param siegeSection The configuration section to load from.
	 */
	public void loadConfig(ConfigurationSection siegeSection) {
		_spawnSpread = siegeSection.getInt("randomSpread", 1);
		_spawnVSpread = siegeSection.getInt("randomVerticalSpread", 2);
		
		_chanceOfExtraMobs.put(EntityType.ZOMBIE, siegeSection.getInt("mobs.zombie.chance", 100));
		_populationThresholds.put(EntityType.ZOMBIE, siegeSection.getInt("mobs.zombie.threshold", 1));
		_maxExtraMobs.put(EntityType.ZOMBIE, siegeSection.getInt("mobs.zombie.max", 100));
		_killValues.put(EntityType.ZOMBIE, siegeSection.getInt("mobs.zombie.value", 1));

		_chanceOfExtraMobs.put(EntityType.SKELETON, siegeSection.getInt("mobs.skeleton.chance", 0));
		_populationThresholds.put(EntityType.SKELETON, siegeSection.getInt("mobs.skeleton.threshold", 20));
		_maxExtraMobs.put(EntityType.SKELETON, siegeSection.getInt("mobs.skeleton.max", 50));
		_killValues.put(EntityType.SKELETON, siegeSection.getInt("mobs.skeleton.value", 1));

		_chanceOfExtraMobs.put(EntityType.SPIDER, siegeSection.getInt("mobs.spider.chance", 0));
		_populationThresholds.put(EntityType.SPIDER, siegeSection.getInt("mobs.spider.threshold", 20));
		_maxExtraMobs.put(EntityType.SPIDER, siegeSection.getInt("mobs.spider.max", 50));
		_killValues.put(EntityType.SPIDER, siegeSection.getInt("mobs.spider.value", 1));

		_chanceOfExtraMobs.put(EntityType.CAVE_SPIDER, siegeSection.getInt("mobs.cave_spider.chance", 0));
		_populationThresholds.put(EntityType.CAVE_SPIDER, siegeSection.getInt("mobs.cave_spider.threshold", 20));
		_maxExtraMobs.put(EntityType.CAVE_SPIDER, siegeSection.getInt("mobs.cave_spider.max", 50));
		_killValues.put(EntityType.CAVE_SPIDER, siegeSection.getInt("mobs.cave_spider.value", 5));

		_chanceOfExtraMobs.put(EntityType.PIG_ZOMBIE, siegeSection.getInt("mobs.pig_zombie.chance", 0));
		_populationThresholds.put(EntityType.PIG_ZOMBIE, siegeSection.getInt("mobs.pig_zombie.threshold", 20));
		_maxExtraMobs.put(EntityType.PIG_ZOMBIE, siegeSection.getInt("mobs.pig_zombie.max", 50));
		_killValues.put(EntityType.PIG_ZOMBIE, siegeSection.getInt("mobs.pig_zombie.value", 5));

		_chanceOfExtraMobs.put(EntityType.BLAZE, siegeSection.getInt("mobs.blaze.chance", 0));
		_populationThresholds.put(EntityType.BLAZE, siegeSection.getInt("mobs.blaze.threshold", 20));
		_maxExtraMobs.put(EntityType.BLAZE, siegeSection.getInt("mobs.blaze.max", 50));
		_killValues.put(EntityType.BLAZE, siegeSection.getInt("mobs.blaze.value", 10));

		_chanceOfExtraMobs.put(EntityType.WITCH, siegeSection.getInt("mobs.witch.chance", 0));
		_populationThresholds.put(EntityType.WITCH, siegeSection.getInt("mobs.witch.threshold", 20));
		_maxExtraMobs.put(EntityType.WITCH, siegeSection.getInt("mobs.witch.max", 50));
		_killValues.put(EntityType.WITCH, siegeSection.getInt("mobs.witch.value", 10));

		_chanceOfExtraMobs.put(EntityType.MAGMA_CUBE, siegeSection.getInt("mobs.magma_cube.chance", 0));
		_populationThresholds.put(EntityType.MAGMA_CUBE, siegeSection.getInt("mobs.magma_cube.threshold", 20));
		_maxExtraMobs.put(EntityType.MAGMA_CUBE, siegeSection.getInt("mobs.magma_cube.max", 50));
		_killValues.put(EntityType.MAGMA_CUBE, siegeSection.getInt("mobs.magma_cube.value", 10));

		_chanceOfExtraWitherSkeleton = siegeSection.getInt("mobs.wither_skeleton.chance", 0);
		_witherSkeletonPopulationThreshold = siegeSection.getInt("mobs.wither_skeleton.threshold", 20);
		_maxExtraWitherSkeletons = siegeSection.getInt("wither_skeleton.max", 50);
		_killValueWitherSkeletons = siegeSection.getInt("mobs.wither_skeleton.value", 10);

		_chanceOfExtraMobs.put(EntityType.ENDERMAN, siegeSection.getInt("mobs.enderman.chance", 0));
		_populationThresholds.put(EntityType.ENDERMAN, siegeSection.getInt("mobs.enderman.threshold", 20));
		_maxExtraMobs.put(EntityType.ENDERMAN, siegeSection.getInt("mobs.enderman.max", 50));
		_killValues.put(EntityType.ENDERMAN, siegeSection.getInt("mobs.enderman.value", 10));

		_chanceOfExtraMobs.put(EntityType.GHAST, siegeSection.getInt("mobs.ghast.chance", 0));
		_populationThresholds.put(EntityType.GHAST, siegeSection.getInt("mobs.ghast.threshold", 20));
		_maxExtraMobs.put(EntityType.GHAST, siegeSection.getInt("mobs.ghast.max", 50));
		_killValues.put(EntityType.GHAST, siegeSection.getInt("mobs.ghast.value", 100));
		
		_chanceOfExtraMobs.put(EntityType.WITHER, siegeSection.getInt("mobs.wither.chance", 0));
		_populationThresholds.put(EntityType.WITHER, siegeSection.getInt("mobs.wither.threshold", 20));
		_maxExtraMobs.put(EntityType.WITHER, siegeSection.getInt("mobs.wither.max", 50));
		_killValues.put(EntityType.WITHER, siegeSection.getInt("mobs.wither.value", 500));
		
		_chanceOfExtraMobs.put(EntityType.ENDER_DRAGON, siegeSection.getInt("mobs.ender_dragon.chance", 0));
		_populationThresholds.put(EntityType.ENDER_DRAGON, siegeSection.getInt("mobs.ender_dragon.threshold", 20));
		_maxExtraMobs.put(EntityType.ENDER_DRAGON, siegeSection.getInt("mobs.ender_dragon.max", 50));
		_killValues.put(EntityType.ENDER_DRAGON, siegeSection.getInt("mobs.ender_dragon.value", 500));
	}

	
	/**
	 * Check to see if a siege is happening.
	 * @return True if a siege is active, false if not.
	 */
	public boolean isSiegeActive() {
		return (_currentSiege != null);
	}
	
	public void endSiege() {
		// If there was a siege, do stuff. Otherwise do nothing.
		if (isSiegeActive()) {
			// Throw a SIEGE_ENDED event!
			UVVillageEvent event = new UVVillageEvent(_currentSiege.getVillage(), _currentSiege.getVillage().getName(), UVVillageEventType.SIEGE_ENDED, _currentSiege.overviewMessage());
			_plugin.getServer().getPluginManager().callEvent(event);
		}
	}
	
	public void clearSiege() {
		// Null out the siege object
		_currentSiege = null;

	}
	
	private void startSiege(Location location, UVVillage village) {
		// Create the siege and associate it with the village we found
		_currentSiege = new UVSiege(village);
		
		// Throw a SIEGE_BEGAN event!
		UVVillageEvent event = new UVVillageEvent(village, village.getName(), UVVillageEventType.SIEGE_BEGAN);
		_plugin.getServer().getPluginManager().callEvent(event);

		// Spawn bonus mobs!
		spawnMoreMobs(location);

	}
	
	/**
	 * Checks to see if a spawn event is related to this siege
	 * @param event CreatureSpawnEvent
	 */
	public void trackSpawn(CreatureSpawnEvent event) {
		// Is there an active siege?
		if (!isSiegeActive()) {
			// No! Find the village closest to the siege...
			UVVillage village = _plugin.villageManager.getClosestVillageToLocation(event.getLocation(), 32);
			
			// TODO ***Maybe replace this with a "check if coords are in a village's boundaries" event***
			
			// Check to see if that village exists
			if (village == null) {
				// If not, throw a note to the log and drop out. No point in tracking/boosting a siege if we can't associate it with a village.
				_plugin.getLogger().info("Siege at " + event.getLocation().toString() + " doesn't have a nearby village... O_o ...dropping out without adding a siege.");
				return;
			}
			// start a siege there.
			startSiege(event.getLocation(), village);
		}
		
		// Add this spawn to the siege's mob list for kill tracking
		addSpawn(event.getEntity());
	}
	
	private void addSpawn(LivingEntity entity) {
		// TODO Randomly buff the entity
		_currentSiege.addSpawn(entity);
	}

	/**
	 * @param location
	 * @param village
	 */
	private void spawnMoreMobs(Location location) {
		// Make sure we didn't call this by mistake... 
		if (_currentSiege != null && _currentSiege.getVillage() != null) {
			// Grab the population
			int population = _currentSiege.getVillage().getPopulation();
			// Try to spawn various types
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
			trySpawn(location, population, EntityType.ENDER_DRAGON, null);
		}
	}

	/**
	 * @param location
	 * @param population
	 * @param type
	 * @param skeletonType
	 */
	private void trySpawn(Location location, int population, EntityType type, SkeletonType skeletonType) {
		int threshold, chance, max;
		if (skeletonType != null) {
			threshold = _witherSkeletonPopulationThreshold;
			chance = _chanceOfExtraWitherSkeleton;
			max = _maxExtraWitherSkeletons;
		} else {
			threshold = getPopulationThreshold(type);
			chance = getExtraMobChance(type);
			max = this.getMaxToSpawn(type);
		}
		if (population >= threshold) {
			if (getExtraMobChance(type) > 1) {
				// Generate a random number of extra mobs to possibly spawn
				int count = _plugin.rng.nextInt((int)(population / threshold)) + 1;
				int numSpawned = 0; 
				for (int i = 0; i < count; i++) {
					// Are we under our max allowed of this type?
					if (numSpawned < max) {
						// Randomly decide whether this mob will spawn.
						if (chance > _plugin.rng.nextInt(100)) {
							// Yay! It's spawning!
							numSpawned++;
							if(skeletonType == SkeletonType.WITHER) {
								Skeleton spawn = (Skeleton) location.getWorld().spawnEntity(location, type);
								spawn.setSkeletonType(SkeletonType.WITHER);
								addSpawn(spawn);
							}
							else
								addSpawn((LivingEntity) location.getWorld().spawnEntity(location, type));
						}
					}
				}
			}
		}
		
	}

	/**
	 * @param type
	 * @return
	 */
	private int getExtraMobChance(EntityType type) {
		if (_chanceOfExtraMobs.containsKey(type))
			return _chanceOfExtraMobs.get(type);
		else 
			return 0;
	}

	private int getMaxToSpawn(EntityType type) {
		if (_maxExtraMobs.containsKey(type))
			return _maxExtraMobs.get(type);
		else 
			return 0;
	}
	
	/**
	 * @param type
	 * @return
	 */
	private int getPopulationThreshold(EntityType type) {
		if (_populationThresholds.containsKey(type))
			return _populationThresholds.get(type);
		else 
			return 999;
	}

	/**
	 * @param entityType
	 * @return
	 */
	private Integer getKillValue(LivingEntity entity) {
		// Is it a skeleton?
		if (entity.getType() == EntityType.SKELETON) {
			// It's a skeleton... is it a wither skeleton?
			if (((Skeleton) entity).getSkeletonType() == SkeletonType.WITHER) {
				return _killValueWitherSkeletons;
			}
		}
		// If it's not a wither skeleton, check to see if we have a proper kill value for this mob
		if (_killValues.containsKey(entity.getType())) {
			// Yes! Return it!
			return _killValues.get(entity.getType());
		} else {
			// Return 0! No points for unknowns!
			return 0;
		}
	}

	public void checkDeath(EntityDeathEvent event) {
		// If there's an active siege being tracked, check to see if the mob killed is part of the siege 
		if (_currentSiege != null) {
			// There's a siege. Is this entity part of the siege?
			if (_currentSiege.checkEntityId(event.getEntity().getEntityId())) {
				_currentSiege.killMob();
				// Was it killed by a player?
				if(event.getEntity().getKiller() != null) {
					// Yep, add it to the siege's kill list 
					_currentSiege.addPlayerKill(event.getEntity().getKiller().getName(), getKillValue(event.getEntity()));
					// And bump up the player's reputation with the village for the kill
					_currentSiege.getVillage().modifyPlayerReputation(event.getEntity().getKiller().getName(), getKillValue(event.getEntity()));
				}
			}
		}
		
	}

	
	public int getPlayerKills(String name) {
		if(isSiegeActive())
			return _currentSiege.getPlayerKills(name);
		else
			return 0;
	}

	public UVVillage getVillage() {
		if (isSiegeActive())
			return _currentSiege.getVillage();
		else
			return null;
	}

	
	
}
