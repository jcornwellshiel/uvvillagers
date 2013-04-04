package net.uvnode.uvvillagers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Skeleton.SkeletonType;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Manages Zombie Siege (Village Siege) enhancements and events.
 *
 * @author James Cornwell-Shiel
 */
public class SiegeManager {

    private UVVillagers _plugin;
    protected Map<String, UVSiege> _currentSieges = new HashMap<String, UVSiege>();;
    protected Map<String, Integer> _populationThresholds = new HashMap<String, Integer>();
    protected Map<String, Integer> _killValues = new HashMap<String, Integer>();
    protected Map<String, Integer> _chanceOfExtraMobs = new HashMap<String, Integer>();
    protected Map<String, Integer> _maxExtraMobs = new HashMap<String, Integer>();
    protected int _spawnSpread;
    protected int _spawnVSpread;
    protected boolean _useCoreSieges;
    protected int _nonCoreSiegeChance;
    
    /**
     * Basic Constructor
     *
     * @param plugin The UVVillagers plugin
     */
    public SiegeManager(UVVillagers plugin) {
        _plugin = plugin;;
    }

    /**
     * Loads the config settings from a configuration section.
     *
     * @param siegeSection The configuration section to load from.
     */
    public void loadConfig(ConfigurationSection siegeSection) {
        _useCoreSieges = siegeSection.getBoolean("useCoreSiegeEvent");
        
        _nonCoreSiegeChance = siegeSection.getInt("nonCoreSiegeChance", 1);
        
        _spawnSpread = siegeSection.getInt("randomSpread", 1);
        _spawnVSpread = siegeSection.getInt("randomVerticalSpread", 2);

        _chanceOfExtraMobs.put("ZOMBIE", siegeSection.getInt("mobs.zombie.chance"));
        _populationThresholds.put("ZOMBIE", siegeSection.getInt("mobs.zombie.threshold"));
        _maxExtraMobs.put("ZOMBIE", siegeSection.getInt("mobs.zombie.max"));
        _killValues.put("ZOMBIE", siegeSection.getInt("mobs.zombie.points"));

        _chanceOfExtraMobs.put("SKELETON_NORMAL", siegeSection.getInt("mobs.skeleton.chance"));
        _populationThresholds.put("SKELETON_NORMAL", siegeSection.getInt("mobs.skeleton.threshold"));
        _maxExtraMobs.put("SKELETON_NORMAL", siegeSection.getInt("mobs.skeleton.max"));
        _killValues.put("SKELETON_NORMAL", siegeSection.getInt("mobs.skeleton.points"));

        _chanceOfExtraMobs.put("SPIDER", siegeSection.getInt("mobs.spider.chance"));
        _populationThresholds.put("SPIDER", siegeSection.getInt("mobs.spider.threshold"));
        _maxExtraMobs.put("SPIDER", siegeSection.getInt("mobs.spider.max"));
        _killValues.put("SPIDER", siegeSection.getInt("mobs.spider.points"));

        _chanceOfExtraMobs.put("CAVE_SPIDER", siegeSection.getInt("mobs.cave_spider.chance"));
        _populationThresholds.put("CAVE_SPIDER", siegeSection.getInt("mobs.cave_spider.threshold"));
        _maxExtraMobs.put("CAVE_SPIDER", siegeSection.getInt("mobs.cave_spider.max"));
        _killValues.put("CAVE_SPIDER", siegeSection.getInt("mobs.cave_spider.points"));

        _chanceOfExtraMobs.put("PIG_ZOMBIE", siegeSection.getInt("mobs.pig_zombie.chance"));
        _populationThresholds.put("PIG_ZOMBIE", siegeSection.getInt("mobs.pig_zombie.threshold"));
        _maxExtraMobs.put("PIG_ZOMBIE", siegeSection.getInt("mobs.pig_zombie.max"));
        _killValues.put("PIG_ZOMBIE", siegeSection.getInt("mobs.pig_zombie.points"));

        _chanceOfExtraMobs.put("BLAZE", siegeSection.getInt("mobs.blaze.chance"));
        _populationThresholds.put("BLAZE", siegeSection.getInt("mobs.blaze.threshold"));
        _maxExtraMobs.put("BLAZE", siegeSection.getInt("mobs.blaze.max"));
        _killValues.put("BLAZE", siegeSection.getInt("mobs.blaze.points"));

        _chanceOfExtraMobs.put("WITCH", siegeSection.getInt("mobs.witch.chance"));
        _populationThresholds.put("WITCH", siegeSection.getInt("mobs.witch.threshold"));
        _maxExtraMobs.put("WITCH", siegeSection.getInt("mobs.witch.max"));
        _killValues.put("WITCH", siegeSection.getInt("mobs.witch.points"));

        _chanceOfExtraMobs.put("MAGMA_CUBE", siegeSection.getInt("mobs.magma_cube.chance"));
        _populationThresholds.put("MAGMA_CUBE", siegeSection.getInt("mobs.magma_cube.threshold"));
        _maxExtraMobs.put("MAGMA_CUBE", siegeSection.getInt("mobs.magma_cube.max"));
        _killValues.put("MAGMA_CUBE", siegeSection.getInt("mobs.magma_cube.points"));
        
        _chanceOfExtraMobs.put("SKELETON_WITHER", siegeSection.getInt("mobs.wither_skeleton.chance"));
        _populationThresholds.put("SKELETON_WITHER", siegeSection.getInt("mobs.wither_skeleton.threshold"));
        _maxExtraMobs.put("SKELETON_WITHER", siegeSection.getInt("mobs.wither_skeleton.max"));
        _killValues.put("SKELETON_WITHER", siegeSection.getInt("mobs.wither_skeleton.points"));

        _chanceOfExtraMobs.put("ENDERMAN", siegeSection.getInt("mobs.enderman.chance"));
        _populationThresholds.put("ENDERMAN", siegeSection.getInt("mobs.enderman.threshold"));
        _maxExtraMobs.put("ENDERMAN", siegeSection.getInt("mobs.enderman.max"));
        _killValues.put("ENDERMAN", siegeSection.getInt("mobs.enderman.points"));

        _chanceOfExtraMobs.put("GHAST", siegeSection.getInt("mobs.ghast.chance"));
        _populationThresholds.put("GHAST", siegeSection.getInt("mobs.ghast.threshold"));
        _maxExtraMobs.put("GHAST", siegeSection.getInt("mobs.ghast.max"));
        _killValues.put("GHAST", siegeSection.getInt("mobs.ghast.points"));

        _chanceOfExtraMobs.put("WITHER", siegeSection.getInt("mobs.wither.chance"));
        _populationThresholds.put("WITHER", siegeSection.getInt("mobs.wither.threshold"));
        _maxExtraMobs.put("WITHER", siegeSection.getInt("mobs.wither.max"));
        _killValues.put("WITHER", siegeSection.getInt("mobs.wither.points"));

        _chanceOfExtraMobs.put("ENDER_DRAGON", siegeSection.getInt("mobs.ender_dragon.chance"));
        _populationThresholds.put("ENDER_DRAGON", siegeSection.getInt("mobs.ender_dragon.threshold"));
        _maxExtraMobs.put("ENDER_DRAGON", siegeSection.getInt("mobs.ender_dragon.max"));
        _killValues.put("ENDER_DRAGON", siegeSection.getInt("mobs.ender_dragon.points"));
        _plugin.getLogger().info(String.format("Loaded %d siege mob definitions.", _chanceOfExtraMobs.size(), _populationThresholds.size(), _maxExtraMobs.size(), _killValues.size(), _chanceOfExtraMobs.size()));
    }

    /**
     * Get whether we're listening for core siege events or not
     * @return whether we're listening for core siege events
     */
    public boolean usingCoreSieges() {
        return _useCoreSieges;
    }
    /**
     * Get the chance of a non-core siege
     * @return chance of non-core siege (1-100)
     */
    public int getChanceOfSiege() {
        return _nonCoreSiegeChance;
    }
    
    /**
     * Check to see if a siege is happening.
     *
     * @return True if a siege is active, false if not.
     */
    public boolean isSiegeActive(World world) {
        return (_currentSieges.containsKey(world.getName()));
    }

    /**
     * Trigger end-of-siege processing
     */
    public void endSiege(World world) {
        // If there was a siege, do stuff. Otherwise do nothing.
        if (isSiegeActive(world)) {
            // Throw a SIEGE_ENDED event!
            UVVillageEvent event = new UVVillageEvent(_currentSieges.get(world.getName()).getVillage(), _currentSieges.get(world.getName()).getVillage().getName(), UVVillageEventType.SIEGE_ENDED, _currentSieges.get(world.getName()).overviewMessage());
            _plugin.getServer().getPluginManager().callEvent(event);
        }
    }

    /**
     * Null out the current siege.
     */
    public void clearSiege(World world) {
        // Null out the siege object
        if (_currentSieges.containsKey(world.getName()))
            _currentSieges.remove(world.getName());

    }

    /**
     * Record that a siege began at this location, for this village!
     *
     * @param location Location
     * @param village Village
     */
    public void startSiege(Location location, UVVillage village) {
        // Create the siege and associate it with the village we found
        _currentSieges.put(location.getWorld().getName(), new UVSiege(village));

        // Throw a SIEGE_BEGAN event!
        UVVillageEvent event = new UVVillageEvent(village, village.getName(), UVVillageEventType.SIEGE_BEGAN);
        _plugin.getServer().getPluginManager().callEvent(event);
        
        // Spawn bonus mobs!
        spawnMoreMobs(location);

    }

    /**
     * Checks to see if a spawn event is related to this siege
     *
     * @param event CreatureSpawnEvent
     */
    public void trackSpawn(CreatureSpawnEvent event) {
        // Is there an active siege in this world?
        if (!isSiegeActive(event.getLocation().getWorld())) {
            // No! Find the village closest to the siege...
            UVVillage village = _plugin.getVillageManager().getClosestVillageToLocation(event.getLocation(), 32);

            // TODO ***Maybe replace this with a "check if coords are in a village's boundaries" event***

            // Check to see if that village exists
            if (village == null) {
                // If not, throw a note to the log and drop out. No point in tracking/boosting a siege if we can't associate it with a village.
                _plugin.getLogger().info(String.format("Siege at %s doesn't have a nearby village... O_o ...dropping out without adding a siege.", event.getLocation().toString()));
                return;
            }
            // start a siege there.
            startSiege(event.getLocation(), village);
        }

        // Add this spawn to the siege's mob list for kill tracking
        addSpawn(event.getEntity());
    }

    /**
     * Add this entity to the current siege's tracker
     *
     * @param entity the spawn
     */
    private void addSpawn(LivingEntity entity) {
        // TODO Randomly buff the entity
        if(_currentSieges.containsKey(entity.getLocation().getWorld().getName())) {
            _currentSieges.get(entity.getLocation().getWorld().getName()).addSpawn(entity);
        }
    }

    /**
     * Spawn more mobs!
     *
     * @param location Location to spawn them!
     */
    private void spawnMoreMobs(Location location) {
        // Make sure we didn't call this by mistake... 
        if (_currentSieges.get(location.getWorld().getName()) != null && _currentSieges.get(location.getWorld().getName()).getVillage() != null) {
            // Grab the population
            int population = _currentSieges.get(location.getWorld().getName()).getVillage().getPopulation();
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
        } else {
            _plugin.debug("We can't spawn! :(");
        }
    }

    /**
     * Try to spawn additional mobs of a type
     *
     * @param location Village location
     * @param population Village population
     * @param type Mob type
     * @param skeletonType Skeleton type
     */
    private void trySpawn(Location location, int population, EntityType type, SkeletonType skeletonType) {
        int threshold, chance, max;
        String typeString = type.toString();
        if (skeletonType != null) {
            typeString += "_" + skeletonType.toString();
        }
        threshold = getPopulationThreshold(typeString);
        chance = getExtraMobChance(typeString);
        max = getMaxToSpawn(typeString);

        _plugin.debug(String.format("Trying to spawn %s: %d of %d villagers needed, %d percent chance, max %d", type.toString(), population, threshold, chance, max));

        if (threshold <= 0) threshold = 1;
        
        if (population >= threshold) {
            if (chance > 0) {
                // Generate a random number of extra mobs to possibly spawn
                int count = _plugin.getRandomNumber(1, (int) (population / threshold) + 1);
                int numSpawned = 0;
                for (int i = 0; i < count; i++) {
                    // Are we under our max allowed of this type?
                    if (numSpawned < max) {
                        // Randomly decide whether this mob will spawn.
                        if (chance > _plugin.getRandomNumber(0, 100)) {
                            // Yay! It's spawning!

                            numSpawned++;

                            // Randomize the location for this spawn
                            Location spawnLocation = location.clone();
                            int xOffset = _plugin.getRandomNumber(_spawnSpread * -1, _spawnSpread);
                            int yOffset = _plugin.getRandomNumber(0, _spawnVSpread);
                            int zOffset = _plugin.getRandomNumber(_spawnSpread * -1, _spawnSpread);
                            spawnLocation.setX(spawnLocation.getX() + xOffset);
                            spawnLocation.setY(spawnLocation.getY() + yOffset);
                            spawnLocation.setZ(spawnLocation.getZ() + zOffset);
                            
                            // Make sure we're not in the ground
                            if (!spawnLocation.getBlock().isEmpty())
                                spawnLocation.setY(spawnLocation.getWorld().getHighestBlockAt(location).getY());
                            
                            // If it's a wither skeleton, spawn it and equip it
                            if (skeletonType == SkeletonType.WITHER) {
                                Skeleton spawn = (Skeleton) location.getWorld().spawnEntity(spawnLocation, type);
                                spawn.setSkeletonType(SkeletonType.WITHER);
                                switch(_plugin.getRandomNumber(0, 2)) {
                                    case 0: 
                                        spawn.getEquipment().setItemInHand(new ItemStack(Material.STONE_SWORD));
                                        break;
                                    case 1: 
                                        spawn.getEquipment().setItemInHand(new ItemStack(Material.IRON_SWORD));
                                        break;
                                    case 2: 
                                        spawn.getEquipment().setItemInHand(new ItemStack(Material.DIAMOND_SWORD));
                                        break;
                                }
                                addSpawn(spawn);
                            // Spawn non-wither skeletons and if it's a skeleton, equip it.
                            } else {
                                LivingEntity spawn = (LivingEntity) location.getWorld().spawnEntity(spawnLocation, type);
                                if (type == EntityType.SKELETON)
                                    spawn.getEquipment().setItemInHand(new ItemStack(Material.BOW));
                                addSpawn(spawn);
                                
                            }
                        }
                    }
                }
                _plugin.debug(String.format("Spawned %d %ss.", numSpawned, type.toString()));

            }
        }

    }

    /**
     * Get the chance of spawning extras of this mob type
     *
     * @param type mob type
     * @return chance out of 100
     */
    protected int getExtraMobChance(String type) {
        if (_chanceOfExtraMobs.containsKey(type)) {
            return _chanceOfExtraMobs.get(type);
        } else {
            return 0;
        }
    }

    /**
     * Get the max number of this mob type to allow to spawn
     *
     * @param type mob type
     * @return max
     */
    protected int getMaxToSpawn(String type) {
        if (_maxExtraMobs.containsKey(type)) {
            return _maxExtraMobs.get(type);
        } else {
            return 0;
        }
    }

    /**
     * Get the minumum population this mob type is allowed to spawn at
     *
     * @param type
     * @return minimum population
     */
    protected int getPopulationThreshold(String type) {
        if (_populationThresholds.containsKey(type)) {
            return _populationThresholds.get(type);
        } else {
            return 999;
        }
    }

    /**
     * Get the point value for killing this mob type
     *
     * @param entity mob type
     * @return point value
     */
    protected Integer getKillValue(LivingEntity entity) {
        
        _plugin.debug(_killValues.toString());
        _plugin.debug(_killValues.keySet().toString());
        _plugin.debug(_killValues.keySet().toArray().toString());
        String typeString = entity.getType().toString();
        _plugin.debug(typeString);
        // Is it a skeleton? If so add skeleton type
        if (entity.getType() == EntityType.SKELETON) {
            typeString += "_" + ((Skeleton) entity).getSkeletonType().toString();
        }
        _plugin.debug(typeString);
        
        if (_killValues.containsKey(typeString)) {
            // Yes! Return it!
            return _killValues.get(typeString);
        } else {
            // Return 0! No points for unknowns!
            _plugin.debug("Kill value not found");

            return 0;
        }
    }

    /**
     * Try to process a mob death.
     *
     * @param event mob death event
     */
    public void checkDeath(EntityDeathEvent event) {
        String worldName = event.getEntity().getLocation().getWorld().getName();
        // If there's an active siege being tracked, check to see if the mob killed is part of the siege 
        if (_currentSieges.get(worldName) != null) {
            // There's a siege. Is this entity part of the siege?
            if (_currentSieges.get(worldName).checkEntityId(event.getEntity().getEntityId())) {
                _currentSieges.get(worldName).killMob();
                // Was it killed by a player?
                if (event.getEntity().getKiller() != null) {
                    // Yep, add it to the siege's kill list 
                    _currentSieges.get(worldName).addPlayerKill(event.getEntity().getKiller().getName(), getKillValue(event.getEntity()));
                    // And bump up the player's reputation with the village for the kill
                    
                    if (event.getEntity().getKiller().hasPermission("uvv.reputation")) {
                        _currentSieges.get(worldName).getVillage().modifyPlayerReputation(event.getEntity().getKiller().getName(), getKillValue(event.getEntity()));
                    }
                    _plugin.debug(String.format("%s gained %d rep with %s for killing a %s.", event.getEntity().getKiller().getName(), getKillValue(event.getEntity()), _currentSieges.get(worldName).getVillage().getName(), event.getEntity().getType().getName()));
                }
            }
        }

    }

    /**
     * Get the number of kills a player got during the current siege
     *
     * @param name player name
     * @return kill count
     */
    public int getPlayerKills(String name, World world) {
        if (isSiegeActive(world)) {
            return _currentSieges.get(world.getName()).getPlayerKills(name);
        } else {
            return 0;
        }
    }

    /**
     * Get the village associated with the active siege
     *
     * @return village object
     */
    public UVVillage getVillage(World world) {
        if (isSiegeActive(world)) {
            return _currentSieges.get(world.getName()).getVillage();
        } else {
            return null;
        }
    }
    
    public ArrayList<String> getSiegeInfo(World world) {
        if (_currentSieges.get(world.getName()) != null) {
            return _currentSieges.get(world.getName()).overviewMessage();
        } else {
            ArrayList<String> messages = new ArrayList<String>();
            messages.add("No sieges so far today!");
            return messages;
        }
    }
}
