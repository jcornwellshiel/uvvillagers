package net.uvnode.uvvillagers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.PigZombie;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Skeleton.SkeletonType;
import org.bukkit.entity.Wolf;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Manages Zombie Siege (Village Siege) enhancements and events.
 *
 * @author James Cornwell-Shiel
 */
public class SiegeManager {

    private UVVillagers _plugin;
    /**
     *
     */
    protected Map<String, UVSiege> _currentSieges = new HashMap<String, UVSiege>();
    /**
     *
     */
    protected boolean _useCoreSieges;
    /**
     *
     */
    protected int _nonCoreSiegeChance;
    /**
     *
     */
    protected ConfigurationSection _siegeConfig;
    
    /**
     * Basic Constructor
     *
     * @param plugin The UVVillagers plugin
     */
    public SiegeManager(UVVillagers plugin) {
        _plugin = plugin;
    }

    /**
     * Loads the config settings from a configuration section.
     *
     * @param siegeSection The configuration section to load from.
     */
    public void loadConfig(ConfigurationSection siegeSection) {
        _siegeConfig = siegeSection;
    }

    /**
     * Get whether we're listening for core siege events or not
     * @return whether we're listening for core siege events
     */
    public boolean usingCoreSieges() {
        return _siegeConfig.getBoolean("useCoreSiegeEvent");
    }
    /**
     * Get the chance of a non-core siege
     * @return chance of non-core siege (1-100)
     */
    public int getChanceOfSiege() {
        return _siegeConfig.getInt("nonCoreSiegeChance");
    }
    
    /**
     * Check to see if a siege is happening.
     *
     * @param world 
     * @return True if a siege is active, false if not.
     */
    public boolean isSiegeActive(World world) {
        return (_currentSieges.containsKey(world.getName()));
    }

    /**
     * Trigger end-of-siege processing
     * @param world 
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
     * @param world 
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
        int threshold, chance, max, min;
        String typeString = type.toString();
        if (skeletonType != null) {
            typeString += "_" + skeletonType.toString();
        }
        threshold = getPopulationThreshold(typeString);
        chance = getExtraMobChance(typeString);
        max = getMaxToSpawn(typeString);
        min = getMinToSpawn(typeString);

        _plugin.debug(String.format("Trying to spawn %s: %d of %d villagers needed, %d percent chance, max %d", type.toString(), population, threshold, chance, max));

        if (threshold <= 0) threshold = 1;
        
        if (population >= threshold) {
            if (chance > 0) {
                // Generate a random number of extra mobs to possibly spawn
                int count = _plugin.getRandomNumber(1, (int) (population / threshold) + 1);
                if (count < min)
                    count = min;
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
                            int xOffset = _plugin.getRandomNumber(_siegeConfig.getInt("randomSpread") * -1, _siegeConfig.getInt("randomSpread"));
                            int yOffset = _plugin.getRandomNumber(0, _siegeConfig.getInt("randomVerticalSpread"));
                            int zOffset = _plugin.getRandomNumber(_siegeConfig.getInt("randomSpread") * -1, _siegeConfig.getInt("randomSpread"));
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
                                
                                switch(_plugin.getRandomNumber(0, 3)) {
                                    case 0: 
                                        spawn.getEquipment().setItemInHand(new ItemStack(Material.STONE_SWORD));
                                        break;
                                    case 1: 
                                        spawn.getEquipment().setItemInHand(new ItemStack(Material.IRON_SWORD));
                                        break;
                                    case 2: 
                                        spawn.getEquipment().setItemInHand(new ItemStack(Material.GOLD_SWORD));
                                        break;
                                    case 3: 
                                        spawn.getEquipment().setItemInHand(new ItemStack(Material.DIAMOND_SWORD));
                                        break;
                                }
                                addSpawn(spawn);
                            // Spawn non-wither skeletons and if it's a skeleton, equip it.
                            } else {
                                LivingEntity spawn = (LivingEntity) location.getWorld().spawnEntity(spawnLocation, type);
                                // Give it potion effects
                                buffMob(spawn, 30);
                                // If it's a wolf, make it angry
                                if (type == EntityType.WOLF) {
                                    ((Wolf) spawn).setAngry(true);
                                }
                                // If it's a zombie pigman, make it angry
                                if (type == EntityType.PIG_ZOMBIE) {
                                    switch(_plugin.getRandomNumber(0, 3)) {
                                        case 0: 
                                            spawn.getEquipment().setItemInHand(new ItemStack(Material.STONE_SWORD));
                                            break;
                                        case 1: 
                                            spawn.getEquipment().setItemInHand(new ItemStack(Material.IRON_SWORD));
                                            break;
                                        case 2: 
                                            spawn.getEquipment().setItemInHand(new ItemStack(Material.GOLD_SWORD));
                                            break;
                                        case 3: 
                                            spawn.getEquipment().setItemInHand(new ItemStack(Material.DIAMOND_SWORD));
                                            break;
                                    }
                                    ((PigZombie) spawn).setAngry(true);
                                    ((PigZombie) spawn).setAnger(24000);
                                }
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
        return _siegeConfig.getInt(String.format("mobs.%s.chance", type.toLowerCase()));
    }

    /**
     * Get the max number of this mob type to allow to spawn
     *
     * @param type mob type
     * @return max
     */
    protected int getMaxToSpawn(String type) {
        return _siegeConfig.getInt(String.format("mobs.%s.max", type.toLowerCase()));
    }
    
    /**
     * Get the min number of this mob type to allow to spawn
     *
     * @param type mob type
     * @return max
     */
    protected int getMinToSpawn(String type) {
        return _siegeConfig.getInt(String.format("mobs.%s.min", type.toLowerCase()));
    }

    /**
     * Get the minumum population this mob type is allowed to spawn at
     *
     * @param type
     * @return minimum population
     */
    protected int getPopulationThreshold(String type) {
        return _siegeConfig.getInt(String.format("mobs.%s.threshold", type.toLowerCase()));
    }

    /**
     * Get the point value for killing this mob type
     *
     * @param entity mob type
     * @return point value
     */
    protected Integer getKillValue(LivingEntity entity) {
        String type = entity.getType().toString();
        _plugin.debug(type);
        // Is it a skeleton? If so add skeleton type
        if (entity.getType() == EntityType.SKELETON) {
            type += "_" + ((Skeleton) entity).getSkeletonType().toString();
        }
        
        return _siegeConfig.getInt(String.format("mobs.%s.points", type.toLowerCase()));
    }
    
    /**
     * Get the chance of this mob type receiving this potion buff
     *
     * @param type 
     * @param potionType 
     * @return point value
     */
    protected Integer getPotionChance(String type, String potionType) {
        return _siegeConfig.getInt(String.format("mobs.%s.potions.%s", type.toLowerCase(), potionType.toLowerCase()), 0);
    }
    /**
     * Get the chance of this mob type receiving this potion buff
     *
     * @param type 
     * @return point value
     */
    protected Integer getMaxPotions(String type) {
        return _siegeConfig.getInt(String.format("mobs.%s.max_potions", type.toLowerCase()), 0);
    }

    /**
     * Try to process a mob death.
     *
     * @param event mob death event
     */
    public boolean checkDeath(EntityDeathEvent event) {
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
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Get the number of kills a player got during the current siege
     *
     * @param name player name
     * @param world 
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
     * @param world 
     * @return village object
     */
    public UVVillage getVillage(World world) {
        if (isSiegeActive(world)) {
            return _currentSieges.get(world.getName()).getVillage();
        } else {
            return null;
        }
    }
    
    /**
     *
     * @param world
     * @return
     */
    public ArrayList<String> getSiegeInfo(World world) {
        if (_currentSieges.get(world.getName()) != null) {
            return _currentSieges.get(world.getName()).overviewMessage();
        } else {
            ArrayList<String> messages = new ArrayList<String>();
            messages.add("No sieges so far today!");
            return messages;
        }
    }
    
    private void buffMob(LivingEntity entity, int chance) {
        Collection<PotionEffect> effects = new ArrayList<PotionEffect>();
        String type = entity.getType().getName();
        try {
            for (PotionEffectType potionType : PotionEffectType.values()) {
                if(potionType != null) {
                    if (getPotionChance(type, potionType.getName()) > 0) {
                        if(_plugin.getRandomNumber(0, 99) < getPotionChance(type, potionType.getName()) && effects.size() < getMaxPotions(type)) {
                            effects.add(
                                    new PotionEffect(
                                        potionType, 
                                        _plugin.getRandomNumber(_siegeConfig.getInt("potionMinDuration"), _siegeConfig.getInt("potionMaxDuration")), 
                                        _plugin.getRandomNumber(_siegeConfig.getInt("potionMinPower"), _siegeConfig.getInt("potionMaxPower")), 
                                        true));
                        }
                    }
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        entity.addPotionEffects(effects);
    }
}
