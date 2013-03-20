package net.uvnode.uvvillagers;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;

import net.minecraft.server.v1_4_R1.Village;
import net.uvnode.uvvillagers.util.FileManager;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;

import org.bukkit.craftbukkit.v1_4_R1.CraftWorld;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;

/**
 * @author James Cornwell-Shiel
 *
 * Adds village tributes and additional zombie siege functionality.
 *
 */
public final class UVVillagers extends JavaPlugin implements Listener {

    private VillageManager _villageManager;
    private SiegeManager _siegeManager;
    private Random rng = new Random();
    private Map<String, String> _playerVillagesProximity = new HashMap<String, String>();
    //UVTributeMode tributeMode;
    private List<UVVillageRank> _reputationRanks = new ArrayList<UVVillageRank>();
    private int tributeRange,
            villagerCount,
            minPerVillagerCount,
            maxPerVillagerCount,
            baseSiegeBonus,
            minPerSiegeKill,
            maxPerSiegeKill,
            timerInterval = 100;
    private boolean tributeCalculating = false;
    private boolean _debug = false;
    
    private FileManager baseConfiguration, villageConfiguration, siegeConfiguration, ranksConfiguration;
    private Integer _villagerValue;
    private Integer _babyVillagerValue;
    private Integer _ironGolemValue;
    

    /**
     * Loads data and runs initialization tasks when enabling the plugin (e.g.
     * on server startup)
     */
    @Override
    public void onEnable() {
        // Initialize the village and siege manager objects
        _villageManager = new VillageManager(this, ((CraftWorld) getServer().getWorlds().get(0)).getHandle());
        _siegeManager = new SiegeManager(this);

        // Register us to handle events
        getServer().getPluginManager().registerEvents(this, this);

        baseConfiguration = new FileManager(this, "config.yml");
        readBaseConfig();

        ranksConfiguration = new FileManager(this, "ranks.yml");
        readRanksConfig();
        
        villageConfiguration = new FileManager(this, "villages.yml");
        siegeConfiguration = new FileManager(this, "siege.yml");

        
        startDayTimer();
    }

    /**
     * Saves data when disabling the plugin (e.g. on server shutdown)
     */
    @Override
    public void onDisable() {
        saveUpdatedVillages();
    }

    /**
     * Reads the plugin configuration.
     */
    private void readBaseConfig() {
        _ironGolemValue = baseConfiguration.getInt("ironGolemValue");
        _villagerValue = baseConfiguration.getInt("villagerValue");
        _babyVillagerValue = baseConfiguration.getInt("babyVillagerValue");
        tributeRange = baseConfiguration.getInt("tributeRange");
        villagerCount = baseConfiguration.getInt("villagerCount");
        minPerVillagerCount = baseConfiguration.getInt("minPerVillagerCount");
        maxPerVillagerCount = baseConfiguration.getInt("maxPerVillagerCount");
        baseSiegeBonus = baseConfiguration.getInt("baseSiegeBonus");
        minPerSiegeKill = baseConfiguration.getInt("minPerSiegeKill");
        maxPerSiegeKill = baseConfiguration.getInt("maxPerSiegeKill");
        _debug = baseConfiguration.getBoolean("debug");
        if (_debug) debug("Debug enabled.");


        getLogger().info("Base configuration loaded.");
    }
    
    /**
     * Reads the village configuration.
     */
    private void readRanksConfig() {
        _reputationRanks.clear();

        Map<String, Object> rankMap = ranksConfiguration.getConfigSection("ranks").getValues(false);

        for (Map.Entry<String, Object> rank : rankMap.entrySet()) {
            String name = rank.getKey();
            int threshold = ranksConfiguration.getInt("ranks." + name + ".threshold");
            double multiplier = ranksConfiguration.getDouble("ranks." + name + ".multiplier");
            _reputationRanks.add(new UVVillageRank(name, threshold, multiplier));
        }
        Collections.sort(_reputationRanks);
        getLogger().info(String.format("%d reputation ranks loaded.", _reputationRanks.size()));
    }
    
    /**
     * Reads the village configuration.
     */
    private void readVillageConfig() {
        _villageManager.loadVillages(villageConfiguration.getConfigSection("villages"));
        getLogger().info(String.format("%d villages loaded.", _villageManager.getAllVillages().size()));
    }
    
    /**
     * Reads the village configuration.
     */
    private void readSiegeConfig() {
        _siegeManager.loadConfig(siegeConfiguration.getConfigSection("siege"));
    }

    /**
     * Updates the configuration file and saves it.
     */
    private void saveUpdatedVillages() {
        villageConfiguration.createSection("villages", _villageManager.saveVillages());
        getLogger().info(String.format("Saving %d villages", _villageManager.getAllVillages().size()));
        if (villageConfiguration.saveFile())
            getLogger().info("Save successful.");
        else
            getLogger().warning("Save failed.");
    }

    /**
     * Starts a timer that throws dawn/dusk events.
     */
    private void startDayTimer() {
        // Step through worlds every 20 ticks and throw UVTimeEvents for various times of day.
        getServer().getScheduler().runTaskTimer(this, new Runnable() {
            @Override
            public void run() {
                List<World> worlds = getServer().getWorlds();
                for (int i = 0; i < worlds.size(); i++) {
                    if (worlds.get(i).getTime() >= 0 && worlds.get(i).getTime() < 0 + timerInterval) {
                        UVTimeEvent event = new UVTimeEvent(worlds.get(i), UVTimeEventType.DAWN);
                        getServer().getPluginManager().callEvent(event);
                    }
                    if (worlds.get(i).getTime() >= 12500 && worlds.get(i).getTime() < 12500 + timerInterval) {
                        UVTimeEvent event = new UVTimeEvent(worlds.get(i), UVTimeEventType.DUSK);
                        getServer().getPluginManager().callEvent(event);
                    }
                     if (worlds.get(i).getTime() >= 5000 && worlds.get(i).getTime() < 5000 + timerInterval) {
                     UVTimeEvent event = new UVTimeEvent(worlds.get(i), UVTimeEventType.NOON);
                     getServer().getPluginManager().callEvent(event);
                     }
                     if (worlds.get(i).getTime() >= 17000 && worlds.get(i).getTime() < 17000 + timerInterval) {
                     UVTimeEvent event = new UVTimeEvent(worlds.get(i), UVTimeEventType.MIDNIGHT);
                     getServer().getPluginManager().callEvent(event);
                     }
                    UVTimeEvent event = new UVTimeEvent(worlds.get(i), UVTimeEventType.CHECK);
                    getServer().getPluginManager().callEvent(event);

                }

            }
        }, 0, timerInterval);
    }

    /**
     * Command listener
     *
     * @param sender The command sender.
     * @param cmd The command sent.
     * @param label The command label.
     * @param args The command arguments.
     * @return Whether the command was processed.
     */
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("uvv")) {
            if (args.length > 0) {
                if (args[0].equalsIgnoreCase("reload")) {
                    if (sender instanceof Player) {
                        Player p = (Player) sender;
                        if (p.hasPermission("uvv.admin")) {
                            if (args.length > 1 && args[1].equalsIgnoreCase("villages")) {
                                sender.sendMessage("Reloading villages from disk...");
                                // Reload the config from disk.
                                villageConfiguration.loadFile();
                                // Process the new config.
                                readVillageConfig();
                            } else {
                                sender.sendMessage("Reloading config data...");
                                // Reload the config from disk.
                                baseConfiguration.loadFile();
                                ranksConfiguration.loadFile();
                                siegeConfiguration.loadFile();
                                // Process the new config.
                                readBaseConfig();
                                readRanksConfig();
                                readSiegeConfig();
                            }
                        } else {
                            sender.sendMessage("You don't have permission to do that.");
                        }
                    } else {
                        if (args.length > 1 && args[1].equalsIgnoreCase("villages")) {
                            sender.sendMessage("Reloading villages from disk...");
                            // Reload the config from disk.
                            villageConfiguration.loadFile();
                            // Process the new config.
                            readVillageConfig();
                        } else {
                            sender.sendMessage("Reloading config data...");
                            // Reload the config from disk.
                            baseConfiguration.loadFile();
                            ranksConfiguration.loadFile();
                            siegeConfiguration.loadFile();
                            // Process the new config.
                            readBaseConfig();
                            readRanksConfig();
                            readSiegeConfig();
                        }
                    }
                } else if (args[0].equalsIgnoreCase("debug")) {
                    if (!(sender instanceof Player)) {
                        _debug = !_debug;
                        sender.sendMessage("Debug is " + (_debug?"On":"Off"));
                    }
                } else if (args[0].equalsIgnoreCase("save")) {
                    if (sender instanceof Player) {
                        Player p = (Player) sender;
                        if (p.hasPermission("uvv.admin")) {
                            sender.sendMessage("Saving...");
                            saveUpdatedVillages();
                        } else {
                            sender.sendMessage("You don't have permission to do that.");
                        }
                    } else {
                        sender.sendMessage("Saving...");
                        saveUpdatedVillages();
                    }
                } else if (args[0].equalsIgnoreCase("startsiege")) {
                    if (sender instanceof Player) {
                        Player p = (Player) sender;
                        if (p.hasPermission("uvv.admin")) {
                            sender.sendMessage("Starting a siege...");
                            startSiege();
                        } else {
                            sender.sendMessage("You don't have permission to do that.");
                        }
                    } else {
                        sender.sendMessage("Starting a siege...");
                        startSiege();
                    }
                } else if (args[0].equalsIgnoreCase("siegeinfo")) {
                    sender.sendMessage(" - UVVillagers Siege Info - ");
                    if (sender instanceof Player) {
                        Player p = (Player) sender;
                        if (p.hasPermission("uvv.siegeinfo")) {
                            ArrayList<String> messages = _siegeManager.getSiegeInfo();
                            sender.sendMessage(messages.toArray(new String[messages.size()]));
                        } else {
                            sender.sendMessage("You don't have permission to do that.");
                        }
                    } else {
                        ArrayList<String> messages = _siegeManager.getSiegeInfo();
                        sender.sendMessage(messages.toArray(new String[messages.size()]));
                    }
                } else if (args[0].equalsIgnoreCase("list")) {
                    sender.sendMessage(" - UVVillagers Village List - ");
                    if (sender instanceof Player) {
                        Player p = (Player) sender;
                        if (p.hasPermission("uvv.villageinfo")) {
                            sendVillageInfo(sender, _villageManager.getAllVillages());
                        } else {
                            sender.sendMessage("You don't have permission to do that.");
                        }
                    } else {
                        sendVillageInfo(sender, _villageManager.getAllVillages());
                    }
                } else if (args[0].equalsIgnoreCase("loaded")) {
                    sender.sendMessage(" - UVVillagers Villages Loaded - ");
                    if (sender instanceof Player) {
                        Player p = (Player) sender;
                        if (p.hasPermission("uvv.villageinfo")) {
                            sendVillageInfo(sender, _villageManager.getLoadedVillages());
                        } else {
                            sender.sendMessage("You don't have permission to do that.");
                        }
                    } else {
                        sendVillageInfo(sender, _villageManager.getLoadedVillages());
                    }
                } else if (args[0].equalsIgnoreCase("nearby")) {
                    sender.sendMessage(" - UVVillagers Nearby Villages - ");
                    if (sender instanceof Player) {
                        Player p = (Player) sender;
                        if (p.hasPermission("uvv.villageinfo")) {
                            sendVillageInfo(sender, _villageManager.getVillagesNearLocation(p.getLocation(), tributeRange));
                        } else {
                            sender.sendMessage("You don't have permission to do that.");
                        }
                    } else {
                        sender.sendMessage("No location to work from...");
                    }
                } else if (args[0].equalsIgnoreCase("current")) {
                    sender.sendMessage(" - UVVillagers Current Village - ");
                    if (sender instanceof Player) {
                        Player p = (Player) sender;
                        if (p.hasPermission("uvv.villageinfo")) {
                            sendVillageInfo(sender, _villageManager.getClosestVillageToLocation(p.getLocation(), tributeRange));
                        } else {
                            sender.sendMessage("You don't have permission to do that.");
                        }
                    } else {
                        sender.sendMessage("No location to work from...");
                    }
                } else if (args[0].equalsIgnoreCase("rename")) {
                    sender.sendMessage(" - UVVillagers Rename Village - ");
                    if (sender instanceof Player) {
                        Player p = (Player) sender;
                        if (p.hasPermission("uvv.rename")) {
                            if (args.length > 1) {
                                String newName = "";
                                newName += args[1];
                                if (args.length > 2) {
                                    for (int i = 2; i < args.length; i++) {
                                        newName += " " + args[i];
                                    }
                                }
                                UVVillage village = _villageManager.getClosestVillageToLocation(p.getLocation(), tributeRange);
                                if (village != null) {
                                    if (village.getTopReputation().equalsIgnoreCase(p.getName())) {
                                        if (_villageManager.getVillageByKey(newName) == null) {
                                            if (_villageManager.renameVillage(village.getName(), newName)) {
                                                sender.sendMessage("Village renamed!");
                                            } else {
                                                sender.sendMessage("Rename failed for some reason...");
                                            }
                                            sendVillageInfo(sender, _villageManager.getVillagesNearLocation(p.getLocation(), tributeRange));
                                        } else {
                                            sender.sendMessage("There's already a village named " + newName);
                                        }
                                    } else {
                                        sender.sendMessage("You must be the most reputable player with a village to rename it! Currently that's " + village.getTopReputation());
                                    }

                                } else {
                                    sender.sendMessage("You're not near a village!");
                                }
                            } else {
                                sender.sendMessage("You must provide a name!");
                            }
                        } else {
                            sender.sendMessage("You don't have permission to do that.");
                        }
                    } else {
                        sender.sendMessage("No location to work from...");
                    }
                } else {
                    sender.sendMessage(" - UVVillagers - ");
                    sender.sendMessage("Command not found. Try one of the following:");
                    sender.sendMessage(" /uvv save");
                    sender.sendMessage(" /uvv reload");
                    sender.sendMessage(" /uvv list");
                    sender.sendMessage(" /uvv nearby");
                    sender.sendMessage(" /uvv rename New Village Name");
                }
            } else {
                // Default action
                sender.sendMessage(" - UVVillagers - ");
                sender.sendMessage("Try one of the following:");
                sender.sendMessage(" /uvv save");
                sender.sendMessage(" /uvv reload");
                sender.sendMessage(" /uvv list");
                sender.sendMessage(" /uvv nearby");
                sender.sendMessage(" /uvv rename New Village Name");
            }
            return true;
        }
        return false;
    }

    /**
     * Sends village information to the sender
     *
     * @param sender The command sender.
     * @param villages A hashmap of village objects.
     */
    private void sendVillageInfo(CommandSender sender, Map<String, UVVillage> villages) {
        for (Map.Entry<String, UVVillage> villageEntry : villages.entrySet()) {
            sendVillageInfo(sender, villageEntry.getValue());
        }
    }

    /**
     * Sends village information to the sender
     *
     * @param sender The command sender.
     * @param village A single village object.
     */
    private void sendVillageInfo(CommandSender sender, UVVillage village) {
        String rankString = "";
        if (sender instanceof Player) {
            rankString = " (" + getRank(village.getPlayerReputation(sender.getName())).getName() + ")";
        }
        sender.sendMessage(
                village.getName()
                + rankString + ": "
                + village.getDoors() + " doors, "
                + village.getPopulation() + " villagers, "
                + village.getSize() + " block size.");
    }

    /**
     * Player move listener. Fires when a player moves.
     *
     * @param event PlayerMoveEvent
     */
    @EventHandler
    private void onPlayerMoveEvent(PlayerMoveEvent event) {
        // Kick out if not on primary world
        if (event.getTo().getWorld() != getServer().getWorlds().get(0)) {
            return;
        }

        // Get closest core village
        Village coreVillage = _villageManager.getClosestCoreVillageToLocation(event.getTo(), tributeRange);

        // Get player name
        String name = event.getPlayer().getName();
        // If a village is nearby
        if (coreVillage != null) {
            Location coreVillageLocation = new Location(event.getTo().getWorld(), coreVillage.getCenter().x, coreVillage.getCenter().y, coreVillage.getCenter().z);
            // Do we have a UVVillage object for this village?
            UVVillage village = _villageManager.getClosestVillageToLocation(coreVillageLocation, coreVillage.getSize());
            if (village == null) {
                village = _villageManager.discoverVillage(coreVillageLocation, coreVillage, event.getPlayer());
                event.getPlayer().sendMessage("You discovered " + village.getName());
            }
            // Do we have a record for this player?
            if (_playerVillagesProximity.containsKey(name)) {
                // If so, check to see if we've already alerted the player that he's near this village
                String current = _playerVillagesProximity.get(name);
                if (!village.getName().equalsIgnoreCase(current)) {
                    // No? Alert him!
                    event.getPlayer().sendMessage("You're near " + village.getName() + "! Your popularity with the " + village.getPopulation() + " villagers here is " + getRank(village.getPlayerReputation(name)).getName() + ".");
                    // Set the player as near this village
                    _playerVillagesProximity.put(name, village.getName());
                }
            } else {
                // if no record, alert him!
                event.getPlayer().sendMessage("You're near " + village.getName() + "! Your popularity with the " + village.getPopulation() + " villagers here is " + getRank(village.getPlayerReputation(name)).getName() + ".");
                // Set the player as near this village
                _playerVillagesProximity.put(name, village.getName());
            }

        } else {
            // The player has left the village!
            _playerVillagesProximity.put(name, "");
        }
    }

    /**
     * CreatureSpawnEvent listener. Fires when a creature spawns.
     *
     * @param event CreatureSpawnEvent
     */
    @EventHandler
    private void onCreatureSpawnEvent(CreatureSpawnEvent event) {
            switch (event.getSpawnReason()) {
                case VILLAGE_INVASION:
                    // VILLAGE_INVASION is only triggered in a zombie siege.
                    // Send this event to the SiegeManager!
                    if (_siegeManager.usingCoreSieges()) {
                       _siegeManager.trackSpawn(event);
                    } else {
                        event.setCancelled(true);
                    }
                    break;
                default:
                    break;
            }
    }

    /**
     * EntityDeathEvent listener. Fires when an entity dies.
     *
     * @param event EntityDeathEvent
     */
    @EventHandler
    private void onEntityDeathEvent(EntityDeathEvent event) {
        _siegeManager.checkDeath(event);
        
        if (event.getEntity().getKiller() != null) {
            if (event.getEntity().getType() == EntityType.VILLAGER) {
                UVVillage village = _villageManager.getClosestVillageToLocation(event.getEntity().getLocation(), 16);
                if (village != null) {
                    Villager villager = (Villager) event.getEntity();
                    if (villager.isAdult()) {
                        village.modifyPlayerReputation(event.getEntity().getKiller().getName(), _villagerValue);
                    } else {
                        village.modifyPlayerReputation(event.getEntity().getKiller().getName(), _babyVillagerValue);
                    }
                }
            } else if (event.getEntity().getType() == EntityType.IRON_GOLEM) {
                UVVillage village = _villageManager.getClosestVillageToLocation(event.getEntity().getLocation(), 16);
                if (village != null)
                    village.modifyPlayerReputation(event.getEntity().getKiller().getName(), _ironGolemValue);
            }
        }

    }

    /**
     * UVVillageEvent listener
     *
     * @param event UVVillageEvent
     */
    @EventHandler
    private void onUVVillageEvent(UVVillageEvent event) {
        debug(event.getMessage());
        switch (event.getType()) {
            case SIEGE_BEGAN:
                getServer().broadcastMessage("A siege began at " + event.getKey());
                break;
            case SIEGE_ENDED:
                ArrayList<String> messages = event.getSiegeMessage();
                Iterator<String> messageIterator = messages.iterator();
                while (messageIterator.hasNext()) {
                    getServer().broadcastMessage(messageIterator.next());
                }
                break;
            case ABANDONED:
                getServer().broadcastMessage("The village " + event.getKey() + " is no more.");
                break;
            default:

                break;
        }
    }

    /**
     * UVTimeEvent listener
     *
     * @param event UVTimeEvent
     */
    @EventHandler
    private void onUVTimeEvent(UVTimeEvent event) {
        debug(event.getMessage());
        switch (event.getType()) {
            case DAWN:
                // Kick us out of this if we're not dealing with the main world.
                if (!event.getWorld().getName().equalsIgnoreCase(_villageManager.getWorld().getName())) {
                    return;
                }
                debug("Calculating tribute.");
                calculateTribute(event.getWorld());
                debug("Ending active sieges.");
                _siegeManager.endSiege();
                break;

            case DUSK:
                // Kick us out of this if we're not dealing with the main world.
                if (!event.getWorld().getName().equalsIgnoreCase(_villageManager.getWorld().getName())) {
                    return;
                }
                //duskHandler(event);

                //getServer().broadcastMessage("Dusk has arrived in world " + event.getWorld().getName() + "!");

                // TODO: clear pending tributes list
                // clear active siege just in case something is missing
                debug("Clearing siege data.");
                _siegeManager.clearSiege();
                break;
            case MIDNIGHT: 
                if (!event.getWorld().getName().equalsIgnoreCase(_villageManager.getWorld().getName())) {
                    return;
                }
                if(!_siegeManager.isSiegeActive() && !_siegeManager.usingCoreSieges()) {
                    debug("Trying to start a siege.");
                    if (_siegeManager.getChanceOfSiege() > getRandomNumber(0, 99)) {
                        debug("A siege is happening tonight!");
                        startSiege();
                    }
                }
                break;
            case CHECK:
                // Update villages
                _villageManager.matchVillagesToCore();
                break;
            default:
                break;
        }
    }

    /**
     * Forces a siege to start
     */
    private void startSiege() {
        Map<String, UVVillage> loadedVillages = _villageManager.getLoadedVillages();
        if (loadedVillages.size() > 0) {
            int index = getRandomNumber(0, loadedVillages.size()-1);
            UVVillage village = loadedVillages.values().toArray(new UVVillage[loadedVillages.size()])[index];

            int xOffset = getRandomNumber(village.getSize() / -2, village.getSize() / 2);
            int zOffset = getRandomNumber(village.getSize() / -2, village.getSize() / 2);
            Location location = village.getLocation();
            location.setX(location.getX() + xOffset);
            location.setZ(location.getZ() + zOffset);
            debug(String.format("Firing up a siege at %s!", location.toString()));
            _siegeManager.startSiege(location, village);
        } else {
            debug("No villages were loaded. So siege tonight!");
        }
    }
    
    /**
     * Runs tribute calculations for a world.
     *
     * @param world The world for which to calculate
     */
    private void calculateTribute(World world) {
        if (!tributeCalculating) {
            // TODO: ADD MULTIWORLD SUPPORT!
            tributeCalculating = true;

            // Make sure the villages are up to date
            _villageManager.matchVillagesToCore();

            // Get the player list
            List<Player> players = world.getPlayers();

            // Step through the players
            for (Player player : players) {
                int tributeAmount = 0, killBonus = 0;

                // Get the villages within tribute range of the player
                Map<String, UVVillage> villages = _villageManager.getVillagesNearLocation(player.getLocation(), tributeRange);

                // if a siege is active, calculate siege tribute bonuses  
                if (_siegeManager.isSiegeActive()) {
                    int kills = _siegeManager.getPlayerKills(player.getName());
                    for (int i = 0; i < kills; i++) {
                        killBonus += getRandomNumber(minPerSiegeKill, maxPerSiegeKill);
                    }
                }

                getLogger().info(player.getName() + ": ");

                for (Map.Entry<String, UVVillage> village : villages.entrySet()) {
                    int villageTributeAmount = 0, siegeBonus = 0, siegeKillTributeAmount = 0;
                    getLogger().info(" - " + village.getKey());
                    int population = village.getValue().getPopulation();

                    int numVillagerGroups = (population - (population % villagerCount)) / villagerCount;

                    debug(String.format(" - Villagers: %d (%d tribute groups)", population, numVillagerGroups));

                    // If this village was the one sieged, give the kill bonus and an extra survival "base siege" thankfulness bonus
                    if (_siegeManager.isSiegeActive() && village.getKey() == _siegeManager.getVillage().getName()) {
                        siegeBonus = numVillagerGroups * baseSiegeBonus;
                        villageTributeAmount += siegeBonus;
                        siegeKillTributeAmount = killBonus;
                        villageTributeAmount += siegeKillTributeAmount;
                    }
                    debug(String.format(" - Siege Defense Bonus: %d", siegeBonus));
                    debug(String.format(" - Siege Kills Bonus: %d", siegeKillTributeAmount));

                    // Give a random bonus per villager count
                    for (int i = 0; i < numVillagerGroups; i++) {
                        int groupTribute = getRandomNumber(minPerVillagerCount, maxPerVillagerCount);
                        debug(String.format(" - Village Group %d: %d", i, groupTribute));
                        villageTributeAmount += groupTribute;
                    }
                    debug(String.format(" - Total Before Multiplier: %d", villageTributeAmount));

                    // Apply rank multiplier
                    double multiplier = getRank(village.getValue().getPlayerReputation(player.getName())).getMultiplier();
                    debug(String.format(" - Reputation: %s", village.getValue().getPlayerReputation(player.getName())));
                    debug(String.format(" - Rank: %s", getRank(village.getValue().getPlayerReputation(player.getName())).getName()));
                    debug(String.format(" - Multiplier: %.2f", multiplier));
                    tributeAmount += (int) villageTributeAmount * multiplier;

                }
                // TO-DO: Save the tribute amount for each player/village so that receive it next time the player talks to a villager in that village.
                // This will force players to interact with every village that they are to get credit for.
                // But for now... just award the tribute directly.
                if (villages.size() > 0) {
                    if (tributeAmount > 0) {
                        ItemStack items = new ItemStack(Material.EMERALD, tributeAmount);
                        player.getInventory().addItem(items);
                        player.sendMessage("Grateful villagers gave you " + tributeAmount + " emeralds!");
                        debug(String.format("%s received %d emeralds.", player.getName(), tributeAmount));
                    } else {
                        player.sendMessage("The villagers didn't have any emeralds for you today.");
                    }
                } else {
                    player.sendMessage("You weren't near any villages large enough to pay you tribute.");
                }

            }
        }
        tributeCalculating = false;
    }

    /**
     * Utility function to get a random number
     *
     * @param minimum The minimum number to return.
     * @param maximum The maximum number to return.
     * @return A random integer between minimum and maximum
     */
    protected int getRandomNumber(int minimum, int maximum) {
        if (maximum < minimum) {
            getLogger().info("Can't generate a random number with a higher min than max.");
            return 0;
        }
        // rng.nextInt(4) returns a value 0-3, so random 1-4 = rng.nextInt(4-1+1) + 1. 
        return rng.nextInt(maximum - minimum + 1) + minimum;
    }

    /**
     * Gets the rank associated with a player's reputation points.
     *
     * @param playerReputation the player's current reputation points
     * @return the UVVillageRank object, containing name, tribute multiplier,
     * and point threshold.
     */
    protected UVVillageRank getRank(int playerReputation) {
        UVVillageRank current = null;
        for (UVVillageRank rank : _reputationRanks) {
            if (playerReputation >= rank.getThreshold()) {
                current = rank;
            }
        }
        if (current == null && _reputationRanks.size() > 0) {
            current = _reputationRanks.get(0);
        }
        if (current == null) {
            current = new UVVillageRank("unknown", Integer.MIN_VALUE, 0);
        }
        return current;
    }

    /**
     * Utility function to get the VillageManager instance.
     *
     * @return
     */
    public VillageManager getVillageManager() {
        return _villageManager;
    }

    /**
     * Checks whether any players are online within distance blocks of location
     *
     * @param location The location.
     * @param distance Maximum allowed distance of the player from the location.
     * @return True if a player is in range, false if not.
     */
    protected boolean areAnyPlayersInRange(Location location, int distance) {
        List<Player> players = location.getWorld().getPlayers();
        for (Player player : players) {
            if (location.distanceSquared(player.getLocation()) < distance * distance) {
                return true;
            }
        }
        return false;
    }
    
    protected void debug(String message) {
        if (_debug) {
            getLogger().info(message);
        }
    }
}
