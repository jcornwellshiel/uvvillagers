package net.uvnode.uvvillagers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import net.minecraft.server.v1_4_R1.Village;
import net.minecraft.server.v1_4_R1.WorldServer;
import org.bukkit.craftbukkit.v1_4_R1.CraftWorld;

//import net.minecraft.server.v1_5_R2.Village;
//import net.minecraft.server.v1_5_R2.WorldServer;
//import org.bukkit.craftbukkit.v1_5_R2.CraftWorld;

/**
 * Manages UVVillage objects and events.
 *
 * @author James Cornwell-Shiel
 */
public class VillageManager {
    private UVVillagers _plugin;
    private Map<String, Map<String, UVVillage>> _villages;
    private boolean _matchRunning = false;
    private List<UVVillage> _markedForAbandonOnce = new ArrayList<UVVillage>();
    /**
     * Basic Constructor
     *
     * @param plugin The UVVillagers plugin reference.
     * @param worldServer The WorldServer object for the world being managed.
     */
    public VillageManager(UVVillagers plugin) {
        _plugin = plugin;
        _villages = new HashMap<String, Map<String, UVVillage>>();
    }

    /**
     * Get the closest core minecraft village object to the provided
     *
     * @param location Location from which to search
     * @param maxDistance Maximum distance around the provided location to
     * search
     * @return Core Minecraft Village object. Null if none found.
     */
    public Village getClosestCoreVillageToLocation(Location location, int maxDistance) {
        WorldServer worldServer = ((CraftWorld)location.getWorld()).getHandle();
        return worldServer.villages.getClosestVillage(location.getBlockX(), location.getBlockY(), location.getBlockZ(), maxDistance);
    }

    /**
     * Get UVVillage nearest to a location
     *
     * @param location The location to search from
     * @param maxDistance Maximum distance around the provided location to
     * search
     * @return UVVillage object closest to the provided location. Null if no
     * UVVillages in range.
     */
    public UVVillage getClosestVillageToLocation(Location location, int maxDistance) {
        UVVillage closest = null;
        double closestDistance = maxDistance * maxDistance;
        if (_villages.containsKey(location.getWorld().getName())) {
            for (Map.Entry<String, UVVillage> villageEntry : _villages.get(location.getWorld().getName()).entrySet()) {
                if (villageEntry.getValue().getLocation().getWorld().getName().equalsIgnoreCase(location.getWorld().getName())) {
                    double centerToEdgeDistanceSquared = villageEntry.getValue().getSize() * villageEntry.getValue().getSize();
                    double distance = location.distanceSquared(villageEntry.getValue().getLocation()) - centerToEdgeDistanceSquared;
                    if (distance < closestDistance) {
                        closestDistance = distance;
                        closest = villageEntry.getValue();
                    }
                }
            }
        }
        return closest;
    }

    /**
     * Get all UVVillages near a location.
     *
     * @param location The location to search from
     * @param maxDistance Maximum distance around the provided location to
     * search
     * @return hashmap of villages found within maxDistance of location
     */
    public Map<String, UVVillage> getVillagesNearLocation(Location location, int maxDistance) {
        Map<String, UVVillage> villages = new HashMap<String, UVVillage>();
        if (_villages.containsKey(location.getWorld().getName())) {
            for (Map.Entry<String, UVVillage> villageEntry : _villages.get(location.getWorld().getName()).entrySet()) {
                if (villageEntry.getValue().getLocation().getWorld().getName().equalsIgnoreCase(location.getWorld().getName())) {
                    double centerToEdgeDistanceSquared = villageEntry.getValue().getSize() * villageEntry.getValue().getSize();
                    double distance = location.distanceSquared(villageEntry.getValue().getLocation()) - centerToEdgeDistanceSquared;
                    if (distance < maxDistance * maxDistance) {
                        villages.put(villageEntry.getKey(), villageEntry.getValue());
                    }
                }
            }
        }
        return villages;
    }

    /**
     * Get a hashmap of all UVVillages
     *
     * @return hashmap of all known UVVillages
     */
    public Map<String, UVVillage> getAllVillages(World world) {
        return _villages.get(world.getName());
    }
    
    /**
     * Get a hashmap of all UVVillages
     *
     * @return hashmap of all known UVVillages
     */
    public Map<String, UVVillage> getAllVillages() {
        Map<String, UVVillage> villages = new HashMap<String, UVVillage>();
        for (Map.Entry<String, Map<String, UVVillage>> worldEntry : _villages.entrySet()) {
            for (Map.Entry<String, UVVillage> villageEntry : worldEntry.getValue().entrySet()) {
                villages.put("world" + villageEntry.getKey(), villageEntry.getValue());
            }
        }
        return villages;
    }

    /**
     * Get a hashmap of all UVVillages that are in currently loaded chunks.
     *
     * @return hashmap of UVVillages in loaded chunks.
     */
    public Map<String, UVVillage> getLoadedVillages() {
        Map<String, UVVillage> villages = new HashMap<String, UVVillage>();
        for (Map.Entry<String, Map<String, UVVillage>> worldEntry : _villages.entrySet()) {
            for (Map.Entry<String, UVVillage> villageEntry : worldEntry.getValue().entrySet()) {
                if (villageEntry.getValue().getLocation().getChunk().isLoaded()) {
                    if (_plugin.areAnyPlayersInRange(villageEntry.getValue().getLocation(), 128)) {
                        villages.put(villageEntry.getKey(), villageEntry.getValue());
                    }
                }
            }
        }
        return villages;
    }
    
    public Map<String, UVVillage> getLoadedVillages(World world) {
        Map<String, UVVillage> villages = new HashMap<String, UVVillage>();
        if (_villages.containsKey(world.getName())) {
            for (Map.Entry<String, UVVillage> villageEntry : _villages.get(world.getName()).entrySet()) {
                if (world.getName().equalsIgnoreCase(villageEntry.getValue().getLocation().getWorld().getName())) {
                    if(villageEntry.getValue().getLocation().getChunk().isLoaded()) {
                        if (_plugin.areAnyPlayersInRange(villageEntry.getValue().getLocation(), 128)) {
                            villages.put(villageEntry.getKey(), villageEntry.getValue());
                        }
                    }
                }
            }
        }
        return villages;
    }

    /**
     * Get a UVVillage object by its key.
     *
     * @param key The unique key for the village
     * @return A UVVillage object. Null if none is found.
     */
    public UVVillage getVillageByKey(World world, String key) {
        if (_villages.containsKey(world.getName())) {
            return _villages.get(world.getName()).get(key);
        } else {
            return null;
        }
    }

    /**
     * Discover a new UVVillage!
     *
     * @param location The location.
     * @param village The core village.
     * @param player The player who discovered it.
     * @return The newly created UVVillage object.
     */
    public UVVillage discoverVillage(Location location, Village village, Player player) {
        // And there it was... a new village. 
        UVVillage newVillage = new UVVillage(location, village);

        // Name it! "[player]ville @ X,Y,Z"
        String name = player.getName() + "ville @ " + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ();
        newVillage.setName(name);

        // Grant discoverer bonus rep!
        newVillage.modifyPlayerReputation(player.getName(), 10);

        // Do we have villages in this world yet?
        if (!_villages.containsKey(location.getWorld().getName()))
            _villages.put(location.getWorld().getName(), new HashMap<String, UVVillage>());
        
        // Add it to the list
        _villages.get(location.getWorld().getName()).put(name, newVillage);

        // Throw a DISCOVERED event!
        UVVillageEvent event = new UVVillageEvent(newVillage, name, UVVillageEventType.DISCOVERED);
        _plugin.getServer().getPluginManager().callEvent(event);

        // Return the village
        return newVillage;
    }

    /**
     * Delete a UVVillage that no longer has a core village nearby
     *
     * @param key The unique key for the village
     */
    public void abandonVillage(World world, String key) {
        // Byebye village! Delete it from the list.
        if (_villages.containsKey(world.getName()))
            _villages.get(world.getName()).remove(key);

        // Throw an ABANDONED event!
        UVVillageEvent event = new UVVillageEvent((UVVillage) null, key, UVVillageEventType.ABANDONED);
        _plugin.getServer().getPluginManager().callEvent(event);
    }

    /**
     * Step through our UVVillages and update their data from the nearest core
     * village (if there is one).
     */
    public void matchVillagesToCore() {
        if (!_matchRunning) {
            _matchRunning = true;
            List<UVVillage> markedForAbandon = new ArrayList<UVVillage>();
            for (Map.Entry<String, Map<String, UVVillage>> worldEntry : _villages.entrySet()) {
                for (Map.Entry<String, UVVillage> villageEntry : worldEntry.getValue().entrySet()) {
                    // Is this village's chunk loaded? (cheaper than the player check, so we do it first)
                    if (villageEntry.getValue().getLocation().getChunk().isLoaded()) {

                        // Just because the chunk is loaded doesn't mean the village is, apparently.
                        // Check if any players are near enough for the core village object to be loaded.
                        if (_plugin.areAnyPlayersInRange(villageEntry.getValue().getLocation(), 64)) {

                            // If it's loaded, find the nearest core village.
                            Village closest = getClosestCoreVillageToLocation(villageEntry.getValue().getLocation(), 64);
                            if (closest == null) {
                                // No nearby village where there should be. Mark for abandon.
                                markedForAbandon.add(villageEntry.getValue());
                            } else {
                                // Found one! Run an update!
                                villageEntry.getValue().setVillageCore(closest);
                                int result = villageEntry.getValue().updateVillageDataFromCore();
                                if (result > 0) {
                                    // Update returned that something has changed. Throw an UPDATED event!
                                    UVVillageEvent event = new UVVillageEvent(villageEntry.getValue(), villageEntry.getKey(), UVVillageEventType.UPDATED);
                                    _plugin.getServer().getPluginManager().callEvent(event);
                                }
                                // And remove this from the potential abandons list
                                _markedForAbandonOnce.remove(villageEntry.getValue());
                            }
                        } else {
                            if (_markedForAbandonOnce.contains(villageEntry.getValue()))
                                _markedForAbandonOnce.remove(villageEntry.getValue());
                        }
                    } else {
                        if (_markedForAbandonOnce.contains(villageEntry.getValue()))
                            _markedForAbandonOnce.remove(villageEntry.getValue());
                    }
                }
            }
            // Abandon the villages marked for abandoning
            for (UVVillage village : markedForAbandon) {
                if (_markedForAbandonOnce.contains(village)) {
                    _markedForAbandonOnce.remove(village);
                    abandonVillage(village.getLocation().getWorld(), village.getName());
                } else {
                    _markedForAbandonOnce.add(village);
                }
            }
            _matchRunning = false;
        }

    }

    /**
     * Loads the UVVillages from a configuration section.
     *
     * @param villageConfig The ConfigurationSection that holds the village data
     */
    public void loadVillages(ConfigurationSection villageConfig, World world) {
        // Load Village Data
        //_villages.clear();
        if (villageConfig == null || villageConfig.getValues(false) == null) {
            return;
        }
/*
        // Pull the configuration section into a hashmap
        Map<String, Object> worldList = villageConfig.getValues(false);
        // Loop through the villages in the configuration section
        for (Map.Entry<String, Object> worldEntry : worldList.entrySet()) {
            _plugin.getLogger().info(worldEntry.toString());
            ConfigurationSection worldConfigSection = (ConfigurationSection) worldEntry.getValue();
            * */
            _villages.put(world.getName(), new HashMap<String, UVVillage>());
            if (villageConfig.getConfigurationSection(world.getName()) != null) {
                Map<String, Object> villageList = villageConfig.getConfigurationSection(world.getName()).getValues(false);
                _villages.put(world.getName(), new HashMap<String, UVVillage>());
                //Map<String, Object> villageList = worldConfigSection.getValues(false);
                for (Map.Entry<String, Object> villageEntry : villageList.entrySet()) {
                    // Grab the next village's configuration section
                    ConfigurationSection villageConfigSection = (ConfigurationSection) villageEntry.getValue();

                    // Read location data
                    World w = _plugin.getServer().getWorld(villageConfigSection.getString("world"));
                    _plugin.getLogger().info(villageConfigSection.getString("world"));
                    int x = villageConfigSection.getInt("x");
                    int y = villageConfigSection.getInt("y");
                    int z = villageConfigSection.getInt("z");
                    Location location = new Location(w, x, y, z);

                    // Read informational data
                    int size = villageConfigSection.getInt("size");
                    int doors = villageConfigSection.getInt("doors");
                    int population = villageConfigSection.getInt("population");

                    // Read player reputation map
                    Map<String, Object> playersMap = villageConfigSection.getConfigurationSection("pr").getValues(false);
                    Map<String, Integer> playerReputations = new HashMap<String, Integer>();
                    for (Map.Entry<String, Object> playerEntry : playersMap.entrySet()) {
                        String playerName = playerEntry.getKey();
                        int playerRep = (Integer) playerEntry.getValue();
                        playerReputations.put(playerName, playerRep);
                    }

                    // Create village and set name by key
                    UVVillage village = new UVVillage(location, doors, population, size, playerReputations);
                    village.setName(villageEntry.getKey());

                    // Add to the collection!
                    _villages.get(world.getName()).put(villageEntry.getKey(), village);
    //                _villages.get(worldEntry.getKey()).put(villageEntry.getKey(), village);
                }
            }
            _plugin.getLogger().info(String.format("Loaded %d villages in %s.", _villages.get(world.getName()).size(), world.getName()));
            //_plugin.getLogger().info(String.format("Loaded %d villages in %s.", _villages.get(worldEntry.getKey()).size(), worldEntry.getKey()));
        //}
    }

    /**
     * Renames a village.
     *
     * @param oldName The old name of the village.
     * @param newName The new name of the village.
     * @return True if successful, false if renaming failed.
     */
    public boolean renameVillage(World world, String oldName, String newName) {
        if (_villages.containsKey(world.getName()) && _villages.get(world.getName()).containsKey(oldName) && !_villages.get(world.getName()).containsKey(newName)) {
            UVVillage village = _villages.get(world.getName()).get(oldName);
            village.setName(newName);
            _villages.get(world.getName()).put(newName, village);
            _villages.get(world.getName()).remove(oldName);

            // Throw an RENAMED event!
            UVVillageEvent event = new UVVillageEvent(village, newName, UVVillageEventType.RENAMED, oldName);
            _plugin.getServer().getPluginManager().callEvent(event);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Dumps the UVVillages to a format safe for saving.
     *
     * @return A Configuration-friendly string-object map of villages for
     * saving.
     */
    public Map<String, Object> saveVillages() {
        // Make a nice clean map to save this in.
        Map<String, Object> map = new HashMap<String, Object>();
        // Step through each UVVillage
        for (Map.Entry<String, Map<String,UVVillage>> worldEntry : _villages.entrySet()) {
            Map<String, Object> w = new HashMap<String, Object>();
            for (Map.Entry<String, UVVillage> villageEntry : worldEntry.getValue().entrySet()) {
                // Make a nice readable hashmap for this UVVillage
                Map<String, Object> v = new HashMap<String, Object>();
                // Put in all its nice data. Someday maybe I'll learn to serialize instead...
                v.put("world", villageEntry.getValue().getLocation().getWorld().getName());
                v.put("x", villageEntry.getValue().getLocation().getBlockX());
                v.put("y", villageEntry.getValue().getLocation().getBlockY());
                v.put("z", villageEntry.getValue().getLocation().getBlockZ());
                v.put("size", villageEntry.getValue().getSize());
                v.put("doors", villageEntry.getValue().getDoors());
                v.put("population", villageEntry.getValue().getPopulation());
                v.put("pr", villageEntry.getValue().getPlayerReputations());
                // Add it to the main map.
                w.put(villageEntry.getKey(), v);
            }
            map.put(worldEntry.getKey(), w);
            _plugin.getLogger().info(String.format("Saving %d villages in %s.", worldEntry.getValue().size(), worldEntry.getKey()));
        }
        // Return our new hashmap!
        return map;
    }

    public void updatePlayerProximity(Location location, Player player, Integer maxDistance) {
        
        // Get closest core village
        Village coreVillage = getClosestCoreVillageToLocation(location, maxDistance);
        
        // Is this player's new location near a core village?
        if (coreVillage != null) {
            // Yes, it's near a core village. 
            // Get the location.
            Location coreVillageLocation = new Location(location.getWorld(), coreVillage.getCenter().x, coreVillage.getCenter().y, coreVillage.getCenter().z);
            
            // Get closest UVVillage.
            UVVillage village = this.getClosestVillageToLocation(coreVillageLocation, maxDistance);
            // Did we find a UVVillage for our core village?
            if (village != null) {
                // Yes, we found a UVVillage.
                // Do nothing.
            } else {
                // No, no UVVillage. 
                // Discover one, and get it.
                village = discoverVillage(coreVillageLocation, coreVillage, player);
                // Announce that the player has discovered a lovely new UVVillage.
                player.sendMessage(String.format("You discovered %s!", village.getName()));
            }
            
            // Was this player listed as at this UVVillage already?
            if (village.isPlayerHere(player.getName())) {
                // Yes.
                // Do nothing.
            } else {
                // No, the village didn't know he was here yet.
                // Announce to the player that he's in a new UVVillage.
                player.sendMessage(String.format("You're near %s! Your reputation with the %d villagers here is %s.", village.getName(), village.getPopulation(), _plugin.getRank(village.getPlayerReputation(player.getName())).getName()));
                // Tell the village that the player is present.
                village.setPlayerHere(player.getName());
                // Loop through all the villages
                for (Map.Entry<String, Map<String, UVVillage>> worldEntry : _villages.entrySet()) {
                    for (Map.Entry<String, UVVillage> villageEntry : worldEntry.getValue().entrySet()) {
                        // Tell the villages we're not in that the player is gone.
                        if (!villageEntry.getKey().equalsIgnoreCase(village.getName())) {
                            // If the player was in this village previously, let them know they left.
                            if (villageEntry.getValue().isPlayerHere(player.getName()))
                                player.sendMessage(String.format("You are no longer near %s.", villageEntry.getValue().getName()));
                            villageEntry.getValue().setPlayerGone(player.getName());
                        }
                    }
                }
            }
        } else {
            // No, it's not near a core village.
            // Tell all villages that this player isn't near them.
            for (Map.Entry<String, Map<String, UVVillage>> worldEntry : _villages.entrySet()) {
                 for (Map.Entry<String, UVVillage> villageEntry : worldEntry.getValue().entrySet()) {
                    // If the player was in this village previously, let them know they left.
                    if (villageEntry.getValue().isPlayerHere(player.getName()))
                        player.sendMessage(String.format("You are no longer near %s.", villageEntry.getValue().getName()));
                    villageEntry.getValue().setPlayerGone(player.getName());
                }
            }

        }
    }
    
    public void tickProximityReputations(World world) {
        // Step through each village
        if(_villages.containsKey(world.getName())) {
            for (Map.Entry<String, UVVillage> villageEntry : _villages.get(world.getName()).entrySet()) {
                UVVillage village = villageEntry.getValue();
                // Only process this village if we're processing its world this tick.
                if (world.getName().equalsIgnoreCase(village.getLocation().getWorld().getName())) {
                    // Get the players online in the village's world
                    List<Player> players = village.getLocation().getWorld().getPlayers();
                    // Step through them.
                    for (Player player : players) {
                        // Tell the village to tick for the player.
                        village.tickPlayerPresence(player.getName());

                        // If the player is in the village...
                        if (village.isPlayerHere(player.getName())) {
                            // And the player has been here a multiple of 12 ticks
                            int ticksHere = village.getPlayerTicksHere(player.getName());
                            if (ticksHere > 0 && ticksHere%60 == 0) {
                                // Bump his reputation up.
                                village.modifyPlayerReputation(player.getName(), 1);
                                _plugin.debug(String.format("Increased %s's reputation with %s by 1 for being present for %d ticks.", 
                                                            player.getName(), 
                                                            village.getName(),
                                                            village.getPlayerTicksHere(player.getName())));
                            }

                        } else { // If the player is NOT in the village...
                            // And the player has been gone a multiple of 240 ticks
                            int ticksGone = village.getPlayerTicksGone(player.getName());
                            if (ticksGone > 0 && ticksGone%240 == 0) {
                                // And his reputation is positive.
                                if (village.getPlayerReputation(player.getName()) > 0) {
                                    // Bump his reputation down.
                                    village.modifyPlayerReputation(player.getName(), -1);
                                    _plugin.debug(String.format("Decreased %s's reputation with %s by 1 for being away for %d ticks.", 
                                                                player.getName(), 
                                                                village.getName(), 
                                                                village.getPlayerTicksGone(player.getName())));
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
