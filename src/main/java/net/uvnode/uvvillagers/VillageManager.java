package net.uvnode.uvvillagers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import net.minecraft.server.v1_7_R2.Village;
import net.minecraft.server.v1_7_R2.WorldServer;
import org.bukkit.craftbukkit.v1_7_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_7_R2.entity.CraftVillager;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;

/**
 * Manages UVVillage objects and events.
 *
 * @author James Cornwell-Shiel
 */
public class VillageManager {

    private UVVillagers _plugin;
    private Map<String, Map<String, UVVillage>> _villages;
    private boolean _matchRunning = false;

    /**
     * Basic Constructor
     *
     * @param plugin The UVVillagers plugin reference.
     */
    public VillageManager(UVVillagers plugin) {
        _plugin = plugin;
        _villages = new HashMap<>();
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
        WorldServer worldServer = ((CraftWorld) location.getWorld()).getHandle();
        Village closest = null;
        double closestDistance = maxDistance * maxDistance;

        // TODO: Loop through worldServer.villages.getVillages()
        List<Village> coreVillages = worldServer.villages.getVillages();
        for (Village coreVillage : coreVillages) {
            double centerToEdgeDistanceSquared = coreVillage.getSize() * coreVillage.getSize();
            double distance = location.distanceSquared(new Location(location.getWorld(), coreVillage.getCenter().x, coreVillage.getCenter().y, coreVillage.getCenter().z)) - centerToEdgeDistanceSquared;
            if (distance < closestDistance) {
                closestDistance = distance;
                closest = coreVillage;
            }
        }
        return closest;
        //return worldServer.villages.getClosestVillage(location.getBlockX(), location.getBlockY(), location.getBlockZ(), maxDistance);
    }

    /**
     *
     * @param location
     * @param maxDistance
     * @return
     */
    public List<Village> getCoreVillagesNearLocation(Location location, int maxDistance) {
        WorldServer worldServer = ((CraftWorld) location.getWorld()).getHandle();
        List<Village> nearby = new ArrayList<>();
        double closestDistance = maxDistance * maxDistance;

        // TODO: Loop through worldServer.villages.getVillages()
        List<Village> coreVillages = worldServer.villages.getVillages();
        for (Village coreVillage : coreVillages) {
            double centerToEdgeDistanceSquared = coreVillage.getSize() * coreVillage.getSize();
            double distance = location.distanceSquared(new Location(location.getWorld(), coreVillage.getCenter().x, coreVillage.getCenter().y, coreVillage.getCenter().z)) - centerToEdgeDistanceSquared;
            if (distance < closestDistance) {
                closestDistance = distance;
                nearby.add(coreVillage);
            }
        }
        return nearby;
        //return worldServer.villages.getClosestVillage(location.getBlockX(), location.getBlockY(), location.getBlockZ(), maxDistance);
    }

    /**
     *
     * @param world
     * @return
     */
    public List<Village> getLoadedCoreVillages(World world) {
        WorldServer worldServer = ((CraftWorld) world).getHandle();
        return worldServer.villages.getVillages();
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
        Map<String, UVVillage> villages = new HashMap<>();
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
     * @param world 
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
        Map<String, UVVillage> villages = new HashMap<>();
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
        Map<String, UVVillage> villages = new HashMap<>();
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

    /**
     *
     * @param world
     * @return
     */
    public Map<String, UVVillage> getLoadedVillages(World world) {
        Map<String, UVVillage> villages = new HashMap<>();
        if (_villages.containsKey(world.getName())) {
            for (Map.Entry<String, UVVillage> villageEntry : _villages.get(world.getName()).entrySet()) {
                if (world.getName().equalsIgnoreCase(villageEntry.getValue().getLocation().getWorld().getName())) {
                    if (villageEntry.getValue().getLocation().getChunk().isLoaded()) {
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
     * @param world 
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
        UVVillage newVillage = new UVVillage(location, village, _plugin);
        newVillage.setCreated();
        // Name it! "[player]ville @ X,Y,Z"
        Integer x = location.getBlockX(), y = location.getBlockY(), z = location.getBlockZ();
        String name = _plugin.getLanguageManager().getString("village_default_name")
                .replace("@player", player.getName())
                .replace("@x", x.toString())
                .replace("@y", y.toString())
                .replace("@z", z.toString());
        newVillage.setName(name);

        // Grant discoverer bonus rep!
        if (player.hasPermission("uvv.reputation")) {
            newVillage.modifyPlayerReputation(player.getName(), _plugin.getRandomNumber(_plugin._minStartingReputation, _plugin._maxStartingReputation) + _plugin._discoverBonus);
        }
        // Do we have villages in this world yet?
        if (!_villages.containsKey(location.getWorld().getName())) {
            _villages.put(location.getWorld().getName(), new HashMap<String, UVVillage>());
        }

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
     * @param world 
     * @param key The unique key for the village
     */
    public void abandonVillage(World world, String key) {
        // Byebye village! Delete it from the list.
        if (_villages.containsKey(world.getName())) {
            _villages.get(world.getName()).remove(key);
        }

        // Throw an ABANDONED event!
        UVVillageEvent event = new UVVillageEvent((UVVillage) null, key, UVVillageEventType.ABANDONED, world.getName());
        _plugin.getServer().getPluginManager().callEvent(event);
    }

    /**
     * Step through our UVVillages and update their data from the nearest core
     * village (if there is one).
     * @param world 
     */
    public void matchVillagesToCore(World world) {
        List<UVVillage> markedForAbandon = new ArrayList<>();
        if (_villages.containsKey(world.getName())) {
            Map<String, UVVillage> villages = _villages.get(world.getName());
            for (Map.Entry<String, UVVillage> villageEntry : villages.entrySet()) {
                // Is this village's chunk loaded? (cheaper than the player check, so we do it first)
                if (villageEntry.getValue().getLocation().getChunk().isLoaded()) {

                    // Just because the chunk is loaded doesn't mean the village is, apparently.
                    // Check if any players are near enough for the core village object to be loaded.
                    if (_plugin.areAnyPlayersInRange(villageEntry.getValue().getLocation(), 64)) {
                        // If it's loaded, find the nearest core village.
                        Village closest = getClosestCoreVillageToLocation(villageEntry.getValue().getLocation(), 64);
                        if (closest == null) {
                            // No nearby village where there should be. Mark for abandon.
                            villageEntry.getValue().addAbandonStrike();
                            _plugin.debug(String.format("%s has %d abandon strikes.", villageEntry.getKey(), villageEntry.getValue().getAbandonStrikes()));
                            if (villageEntry.getValue().getAbandonStrikes() > 3) {
                                _plugin.debug(String.format("%s is marked for abandon.", villageEntry.getKey()));
                                markedForAbandon.add(villageEntry.getValue());
                            }
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
                            villageEntry.getValue().clearAbandonStrikes();
                            //_plugin.debug(String.format("%s has a nearby core village, clearing strikes.", villageEntry.getKey()));
                        }
                    } else {
                        villageEntry.getValue().clearAbandonStrikes();
                        //_plugin.debug(String.format("%s is not near a player, clearing strikes.", villageEntry.getKey()));
                    }
                } else {
                    villageEntry.getValue().clearAbandonStrikes();
                    //_plugin.debug(String.format("%s is not in a loaded chunk, clearing strikes.", villageEntry.getKey()));
                }
            }
            // Abandon the villages marked for abandoning
            for (UVVillage village : markedForAbandon) {
                abandonVillage(village.getLocation().getWorld(), village.getName());
            }
        }
    }

    /**
     * Loads the UVVillages from a configuration section.
     *
     * @param villageConfig The ConfigurationSection that holds the village data
     * @param world  
     */
    public void loadVillages(ConfigurationSection villageConfig, World world) {
        // Load Village Data
        //_villages.clear();
        if (villageConfig == null || villageConfig.getValues(false) == null) {
            return;
        }
        _villages.put(world.getName(), new HashMap<String, UVVillage>());
        if (villageConfig.getConfigurationSection(world.getName()) != null) {
            Map<String, Object> villageList = villageConfig.getConfigurationSection(world.getName()).getValues(false);
            _villages.put(world.getName(), new HashMap<String, UVVillage>());
            for (Map.Entry<String, Object> villageEntry : villageList.entrySet()) {
                // Grab the next village's configuration section
                ConfigurationSection villageConfigSection = (ConfigurationSection) villageEntry.getValue();

                // Read location data
                World w = _plugin.getServer().getWorld(villageConfigSection.getString("world"));
                int x = villageConfigSection.getInt("x");
                int y = villageConfigSection.getInt("y");
                int z = villageConfigSection.getInt("z");

                Location location = new Location(w, x, y, z);

                // Read informational data
                int size = villageConfigSection.getInt("size");
                int doors = villageConfigSection.getInt("doors");
                int population = villageConfigSection.getInt("population");
                
                boolean isServerVillage = villageConfigSection.getBoolean("isServerVillage");

                int maxX = villageConfigSection.getInt("maxx", x + size/2);
                int maxY = villageConfigSection.getInt("maxy", y + size/2);
                int maxZ = villageConfigSection.getInt("maxz", z + size/2);
                int minX = villageConfigSection.getInt("minx", x - size/2);
                int minY = villageConfigSection.getInt("miny", y - size/2);
                int minZ = villageConfigSection.getInt("minz", z - size/2);
                
                boolean has_sign = villageConfigSection.getBoolean("has_sign");
                int sign_x = villageConfigSection.getInt("sign_x");
                int sign_y = villageConfigSection.getInt("sign_y");
                int sign_z = villageConfigSection.getInt("sign_z");
                
                boolean has_chest = villageConfigSection.getBoolean("has_chest");
                int chest_x = villageConfigSection.getInt("chest_x");
                int chest_y = villageConfigSection.getInt("chest_y");
                int chest_z = villageConfigSection.getInt("chest_z");


                // Read player reputation map
                Map<String, Object> playersMap = villageConfigSection.getConfigurationSection("pr").getValues(false);
                Map<String, Integer> playerReputations = new HashMap<>();
                for (Map.Entry<String, Object> playerEntry : playersMap.entrySet()) {
                    String playerName = playerEntry.getKey();
                    int playerRep = (Integer) playerEntry.getValue();
                    playerReputations.put(playerName, playerRep);
                }

                // Create village and set name by key
                UVVillage village = new UVVillage(location, doors, population, size, playerReputations, isServerVillage, _plugin);
                village.setName(villageEntry.getKey());
                village.setCreated(villageConfigSection.getString("created"));
                village.setBounds(minX, maxX, minY, maxY, minZ, maxZ);
                if (has_sign)
                    village.setMayorSign(new Location(w, sign_x, sign_y, sign_z));
                if (has_chest)
                    village.setTributeChest(new Location(w, chest_x, chest_y, chest_z));
                
                // Add to the collection!
                _villages.get(world.getName()).put(villageEntry.getKey(), village);
            }
        }
        _plugin.getLogger().info(String.format("Loaded %d villages in %s.", _villages.get(world.getName()).size(), world.getName()));
    }

    public UVVillage toggleServerVillage(UVVillage v) {
        v.setServerVillage(!v.isServerVillage());
        _villages.get(v.getLocation().getWorld().getName()).put(v.getName(), v);
        return v;
        
    }
    
    
    /**
     * Renames a village.
     *
     * @param world 
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
        Map<String, Object> map = new HashMap<>();
        // Step through each UVVillage
        for (Map.Entry<String, Map<String, UVVillage>> worldEntry : _villages.entrySet()) {
            Map<String, Object> w = new HashMap<>();
            for (Map.Entry<String, UVVillage> villageEntry : worldEntry.getValue().entrySet()) {
                // Make a nice readable hashmap for this UVVillage
                Map<String, Object> v = new HashMap<>();
                // Put in all its nice data. Someday maybe I'll learn to serialize instead...
                v.put("world", villageEntry.getValue().getLocation().getWorld().getName());
                v.put("x", villageEntry.getValue().getLocation().getBlockX());
                v.put("y", villageEntry.getValue().getLocation().getBlockY());
                v.put("z", villageEntry.getValue().getLocation().getBlockZ());
                
                v.put("minx", villageEntry.getValue().getMinX());
                v.put("maxx", villageEntry.getValue().getMaxX());
                v.put("miny", villageEntry.getValue().getMinY());
                v.put("maxy", villageEntry.getValue().getMaxY());
                v.put("minz", villageEntry.getValue().getMinZ());
                v.put("maxz", villageEntry.getValue().getMaxZ());
                
                v.put("y", villageEntry.getValue().getLocation().getBlockY());
                v.put("z", villageEntry.getValue().getLocation().getBlockZ());
                v.put("x", villageEntry.getValue().getLocation().getBlockX());
                v.put("y", villageEntry.getValue().getLocation().getBlockY());
                v.put("z", villageEntry.getValue().getLocation().getBlockZ());
                
                if (villageEntry.getValue().getMayorSign() != null) {
                    v.put("has_sign", true);
                    v.put("sign_x", villageEntry.getValue().getMayorSignLocation().getBlockX());
                    v.put("sign_y", villageEntry.getValue().getMayorSignLocation().getBlockY());
                    v.put("sign_z", villageEntry.getValue().getMayorSignLocation().getBlockZ());                    
                } else {
                    v.put("has_sign", false);
                    v.put("sign_x", -1);
                    v.put("sign_y", -1);
                    v.put("sign_z", -1);
                }
                if (villageEntry.getValue().getChest()!= null) {
                    v.put("has_chest", true);
                    v.put("chest_x", villageEntry.getValue().getChest().getBlockX());
                    v.put("chest_y", villageEntry.getValue().getChest().getBlockY());
                    v.put("chest_z", villageEntry.getValue().getChest().getBlockZ());                    
                } else {
                    v.put("has_chest", false);
                    v.put("chest_x", -1);
                    v.put("chest_y", -1);
                    v.put("chest_z", -1);
                }
                v.put("size", villageEntry.getValue().getSize());
                v.put("doors", villageEntry.getValue().getDoorCount());
                v.put("population", villageEntry.getValue().getPopulation());
                v.put("pr", villageEntry.getValue().getPlayerReputations());
                v.put("isServerVillage", villageEntry.getValue().isServerVillage());
                v.put("created", villageEntry.getValue().getCreatedString());
                // Add it to the main map.
                w.put(villageEntry.getKey(), v);
            }
            map.put(worldEntry.getKey(), w);
            _plugin.getLogger().info(String.format("Saving %d villages in %s.", worldEntry.getValue().size(), worldEntry.getKey()));
        }
        // Return our new hashmap!
        return map;
    }

    public void checkPlayerProximities(World world, int distance) {
        for (Player player : world.getPlayers()) {
            updatePlayerProximity(player.getLocation(), player, distance);
        }
    }
    
    /**
     *
     * @param location
     * @param player
     * @param maxDistance
     */
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
            } else if (coreVillage.getPopulationCount() >= _plugin._villageMinPopulation) {
                // No, no UVVillage, and the core village's population is over the minimum. Discover!
                // Discover one, and get it.
                village = discoverVillage(coreVillageLocation, coreVillage, player);
                // Announce that the player has discovered a lovely new UVVillage.
                player.sendMessage(_plugin.getLanguageManager().getString("village_discovered").replace("@village", village.getName()));
            }

            if (village == null) {
                for (Map.Entry<String, Map<String, UVVillage>> worldEntry : _villages.entrySet()) {
                    for (Map.Entry<String, UVVillage> villageEntry : worldEntry.getValue().entrySet()) {
                        // If the player was in this village previously, let them know they left.
                        if (villageEntry.getValue().isPlayerHere(player.getName())) {
                            player.sendMessage(_plugin.getLanguageManager().getString("village_leave").replace("@village", villageEntry.getValue().getName()));
                        }
                        villageEntry.getValue().setPlayerGone(player.getName());
                    }
                }
                return;
            }
            // Find missing mayors
            if (village.getMayor() == null) {
                if (village.getMayorSign() != null) {
                    for (Entity entity : village.getMayorSign().getNearbyEntities(village.getSize(), village.getSize(), village.getSize())) {
                        if (entity.getType() == EntityType.VILLAGER) {
                            if (((CraftVillager) entity).isCustomNameVisible() && ((CraftVillager) entity).getCustomName().contains("Mayor of")) {
                                village.setMayor((Villager) entity);
                            }
                        }
                    }
                }
            }
                
            
            // Was this player listed as at this UVVillage already?
            if (village.isPlayerHere(player.getName())) {
                // Yes.
                // Do nothing.
            } else {
                if (!village.isPlayerKnown(player.getName())) {
                    _plugin.debug(String.format("%s is new in %s. Initial reputation set to %d.", player.getName(), village.getName(), village.modifyPlayerReputation(player.getName(), _plugin.getRandomNumber(_plugin._minStartingReputation, _plugin._maxStartingReputation) + _plugin._discoverBonus)));
                }
                // No, the village didn't know he was here yet.
                // Announce to the player that he's in a new UVVillage.
                player.sendMessage(_plugin.getLanguageManager().getString("village_enter").replace("@village", village.getName()).replace("@reputation", _plugin.getRank(village.getPlayerReputation(player.getName())).getName()));
                // Tell the village that the player is present.
                village.setPlayerHere(player.getName());
                // Loop through all the villages
                for (Map.Entry<String, Map<String, UVVillage>> worldEntry : _villages.entrySet()) {
                    for (Map.Entry<String, UVVillage> villageEntry : worldEntry.getValue().entrySet()) {
                        // Tell the villages we're not in that the player is gone.
                        if (!villageEntry.getKey().equalsIgnoreCase(village.getName())) {
                            // If the player was in this village previously, let them know they left.
                            if (villageEntry.getValue().isPlayerHere(player.getName())) {
                                player.sendMessage(_plugin.getLanguageManager().getString("village_leave").replace("@village", villageEntry.getValue().getName()));
                            }
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
                    if (villageEntry.getValue().isPlayerHere(player.getName())) {
                        player.sendMessage(_plugin.getLanguageManager().getString("village_leave").replace("@village", villageEntry.getValue().getName()));
                    }
                    villageEntry.getValue().setPlayerGone(player.getName());
                }
            }
        }
    }

    /**
     *
     * @param world
     */
    public void tickProximityReputations(World world) {
        // Step through each village
        if (_villages.containsKey(world.getName())) {
            for (Map.Entry<String, UVVillage> villageEntry : _villages.get(world.getName()).entrySet()) {
                UVVillage village = villageEntry.getValue();
                // Only process this village if we're processing its world this tick.
                if (world.getName().equalsIgnoreCase(village.getLocation().getWorld().getName())) {
                    // Get the players online in the village's world
                    List<Player> players = village.getLocation().getWorld().getPlayers();
                    // Step through them.
                    for (Player player : players) {
                        if (player.hasPermission("uvv.reputation")) {
                            // Tell the village to tick for the player.
                            village.tickPlayerPresence(player.getName());

                            // If the player is in the village...
                            if (village.isPlayerHere(player.getName())) {
                                // And the player has been here a multiple of 12 ticks
                                int ticksHere = village.getPlayerTicksHere(player.getName());
                                if (ticksHere > 0 && ticksHere % 60 == 0) {
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
                                if (ticksGone > 0 && ticksGone % 240 == 0) {
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

    /**
     *
     * @param world
     */
    protected void checkForMerge(World world) {
        List<String> markedForAbandon = new ArrayList<>();
        if (!_villages.containsKey(world.getName())) {
            return;
        }
        for (Map.Entry<String, UVVillage> currentVillageEntry : _villages.get(world.getName()).entrySet()) {
            UVVillage currentVillage = currentVillageEntry.getValue();
            for (Map.Entry<String, UVVillage> villageEntry : _villages.get(world.getName()).entrySet()) {
                UVVillage village = villageEntry.getValue();
                if (!markedForAbandon.contains(village.getName())) {
                    if (!village.getName().equalsIgnoreCase(currentVillage.getName())
                            && village.getVillageCore() != null
                            && currentVillage.getVillageCore() != null
                            && village.getVillageCore().equals(currentVillage.getVillageCore())) {
                        UVVillage abandon = mergeVillages(currentVillage, village);

                        UVVillageEvent event = new UVVillageEvent(abandon,
                                abandon.getName(),
                                UVVillageEventType.MERGED,
                                abandon.getName().equalsIgnoreCase(village.getName()) ? currentVillage.getName() : village.getName());
                        _plugin.getServer().getPluginManager().callEvent(event);
                        markedForAbandon.add(abandon.getName());
                    }
                }
            }
        }
        for (String abandonName : markedForAbandon) {
            abandonVillage(world, abandonName);
        }
    }

    private UVVillage mergeVillages(UVVillage village1, UVVillage village2) {
        UVVillage source, destination;
        if (village1.getCreated().before(village2.getCreated())) {
            source = village2;
            destination = village1;
        } else {
            source = village1;
            destination = village2;
        }
        for (Map.Entry<String, Integer> repEntry : source.getPlayerReputations().entrySet()) {
            // Merge the reputation values
            destination.modifyPlayerReputation(repEntry.getKey(), repEntry.getValue());
        }
        if (destination.getMayor() == null && source.getMayor() != null) {
            destination.setMayor(source.getMayor());
        }
        if (destination.getMayorSign() == null && source.getMayorSign() != null) {
            destination.setMayorSign(source.getMayorSign());
        }
        return source;
    }

    /**
     *
     * @param world
     */
    protected void tickMayorMovement(World world) {
        if (_villages.containsKey(world.getName())) {
            for (Map.Entry<String, UVVillage> villageEntry : _villages.get(world.getName()).entrySet()) {
                // Is this village's chunk loaded? (cheaper than the player check, so we do it first)
                if (villageEntry.getValue().getLocation().getChunk().isLoaded()) {

                    // Just because the chunk is loaded doesn't mean the village is, apparently.
                    // Check if any players are near enough for the core village object to be loaded.
                    if (_plugin.areAnyPlayersInRange(villageEntry.getValue().getLocation(), 64)) {
                        if (villageEntry.getValue().getMayor() == null) {
                            if (villageEntry.getValue().getMayorSign() != null) {
                                List<Entity> entities = villageEntry.getValue().getMayorSign().getNearbyEntities(16, 16, 16);
                                for (Entity entity : entities) {
                                    if (entity.getType() == EntityType.VILLAGER)  {
                                        CraftVillager craftVillager = (CraftVillager) entity;
                                        if (craftVillager.isCustomNameVisible() && craftVillager.getCustomName().contains("Mayor of ")) {
                                            villageEntry.getValue().setMayor((Villager) entity);
                                        }
                                    }
                                }
                            }
                        } else {
                            villageEntry.getValue().moveMayor();
                        }
                    }
                }
            }
        }
        
    }

    void clearTributes(World world) {
        if(_villages.containsKey(world.getName())) {
            for(Map.Entry<String, UVVillage> villageEntry : _villages.get(world.getName()).entrySet()) {
                villageEntry.getValue().clearEmeraldTributes();
            }
        }
    }

    void tickWorld(World world, int _tributeRange) {
        
    }
}
