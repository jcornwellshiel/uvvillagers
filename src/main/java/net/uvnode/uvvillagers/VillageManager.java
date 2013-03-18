package net.uvnode.uvvillagers;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import net.minecraft.server.v1_4_R1.Village;
import net.minecraft.server.v1_4_R1.WorldServer;

/**
 * Manages UVVillage objects and events.
 *
 * @author James Cornwell-Shiel
 */
public class VillageManager {

    private UVVillagers _plugin;
    private WorldServer _worldServer;
    private Map<String, UVVillage> _villages;

    /**
     * Basic Constructor
     *
     * @param plugin The UVVillagers plugin reference.
     * @param worldServer The WorldServer object for the world being managed.
     */
    public VillageManager(UVVillagers plugin, WorldServer worldServer) {
        _plugin = plugin;
        _worldServer = worldServer;
        _villages = new HashMap<String, UVVillage>();
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
        return _worldServer.villages.getClosestVillage(location.getBlockX(), location.getBlockY(), location.getBlockZ(), maxDistance);
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
        for (Map.Entry<String, UVVillage> villageEntry : _villages.entrySet()) {
            double centerToEdgeDistanceSquared = villageEntry.getValue().getSize() * villageEntry.getValue().getSize();
            double distance = location.distanceSquared(villageEntry.getValue().getLocation()) - centerToEdgeDistanceSquared;
            if (distance < closestDistance) {
                closestDistance = distance;
                closest = villageEntry.getValue();
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
        for (Map.Entry<String, UVVillage> villageEntry : _villages.entrySet()) {
            double centerToEdgeDistanceSquared = villageEntry.getValue().getSize() * villageEntry.getValue().getSize();
            double distance = location.distanceSquared(villageEntry.getValue().getLocation()) - centerToEdgeDistanceSquared;
            if (distance < maxDistance * maxDistance) {
                villages.put(villageEntry.getKey(), villageEntry.getValue());
            }
        }
        return villages;
    }

    /**
     * Get a hashmap of all UVVillages
     *
     * @return hashmap of all known UVVillages
     */
    public Map<String, UVVillage> getAllVillages() {
        return _villages;
    }

    /**
     * Get a hashmap of all UVVillages that are in currently loaded chunks.
     *
     * @return hashmap of UVVillages in loaded chunks.
     */
    public Map<String, UVVillage> getLoadedVillages() {
        Map<String, UVVillage> villages = new HashMap<String, UVVillage>();
        for (Map.Entry<String, UVVillage> villageEntry : _villages.entrySet()) {
            if (villageEntry.getValue().getLocation().getChunk().isLoaded()) {
                villages.put(villageEntry.getKey(), villageEntry.getValue());
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
    public UVVillage getVillageByKey(String key) {
        return _villages.get(key);
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

        // Add it to the list
        _villages.put(name, newVillage);

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
    public void abandonVillage(String key) {
        // Byebye village! Delete it from the list.
        _villages.remove(key);

        // Throw an ABANDONED event!
        UVVillageEvent event = new UVVillageEvent((UVVillage) null, key, UVVillageEventType.ABANDONED);
        _plugin.getServer().getPluginManager().callEvent(event);
    }

    /**
     * Step through our UVVillages and update their data from the nearest core
     * village (if there is one).
     */
    public void matchVillagesToCore() {
        for (Map.Entry<String, UVVillage> villageEntry : _villages.entrySet()) {
            // Is this village's chunk loaded? (cheaper than the player check, so we do it first)
            if (villageEntry.getValue().getLocation().getChunk().isLoaded()) {

                // Just because the chunk is loaded doesn't mean the village is, apparently.
                // Check if any players are near enough for the core village object to be loaded.
                if (_plugin.areAnyPlayersInRange(villageEntry.getValue().getLocation(), 128)) {

                    // If it's loaded, find the nearest core village.
                    Village closest = getClosestCoreVillageToLocation(villageEntry.getValue().getLocation(), 64);
                    if (closest == null) {
                        // No nearby 
                        abandonVillage(villageEntry.getKey());
                    } else {
                        // Found one! Run an update!
                        villageEntry.getValue().setVillageCore(closest);
                        int result = villageEntry.getValue().updateVillageDataFromCore();
                        if (result > 0) {
                            // Update returned that something has changed. Throw an UPDATED event!
                            UVVillageEvent event = new UVVillageEvent(villageEntry.getValue(), villageEntry.getKey(), UVVillageEventType.UPDATED);
                            _plugin.getServer().getPluginManager().callEvent(event);
                        }
                    }
                }
            }
        }

    }

    /**
     * Loads the UVVillages from a configuration section.
     *
     * @param villageConfig The ConfigurationSection that holds the village data
     */
    public void loadVillages(ConfigurationSection villageConfig) {
        // Load Village Data
        _villages.clear();
        if (villageConfig == null || villageConfig.getValues(false) == null) {
            return;
        }

        try {
            // Pull the configuration section into a hashmap
            Map<String, Object> villageList = villageConfig.getValues(false);
            // Loop through the villages in the configuration section
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
                _villages.put(villageEntry.getKey(), village);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Renames a village.
     *
     * @param oldName The old name of the village.
     * @param newName The new name of the village.
     * @return True if successful, false if renaming failed.
     */
    public boolean renameVillage(String oldName, String newName) {
        if (_villages.containsKey(oldName) && !_villages.containsKey(newName)) {
            UVVillage village = _villages.get(oldName);
            village.setName(newName);
            _villages.put(newName, village);
            _villages.remove(oldName);

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
        for (Map.Entry<String, UVVillage> vdata : _villages.entrySet()) {
            // Make a nice readable hashmap for this UVVillage
            Map<String, Object> v = new HashMap<String, Object>();
            // Put in all its nice data. Someday maybe I'll learn to serialize instead...
            v.put("world", vdata.getValue().getLocation().getWorld().getName());
            v.put("x", vdata.getValue().getLocation().getBlockX());
            v.put("y", vdata.getValue().getLocation().getBlockY());
            v.put("z", vdata.getValue().getLocation().getBlockZ());
            v.put("size", vdata.getValue().getSize());
            v.put("doors", vdata.getValue().getDoors());
            v.put("population", vdata.getValue().getPopulation());
            v.put("pr", vdata.getValue().getPlayerReputations());
            // Add it to the main map.
            map.put(vdata.getKey(), v);
        }
        // Return our new hashmap!
        return map;
    }

    /**
     * Get the world that this VillageManager operates in.
     *
     * @return The world the VillageManager operates in.
     */
    public World getWorld() {
        return _worldServer.getWorld();
    }
    /**
     * Get the world that this VillageManager operates in.
     *
     * @return The world the VillageManager operates in.
     */
}
