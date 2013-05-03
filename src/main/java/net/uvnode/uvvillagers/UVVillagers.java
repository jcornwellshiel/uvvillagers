package net.uvnode.uvvillagers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
//import net.minecraft.server.v1_4_R1.Village;
import net.minecraft.server.v1_5_R3.Village;

import net.uvnode.uvvillagers.util.FileManager;
import org.bukkit.ChatColor;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_5_R3.entity.CraftVillager;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Villager;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.mcstats.Metrics;

/**
 * @author James Cornwell-Shiel
 *
 * Adds village tributes and additional zombie siege functionality.
 *
 */
public final class UVVillagers extends JavaPlugin implements Listener {

    private VillageManager _villageManager;
    private SiegeManager _siegeManager;
    private DynmapManager _dynmapManager;
    private FileManager baseConfiguration, villageConfiguration, siegeConfiguration, ranksConfiguration, languageConfiguration;
    private LanguageManager _languageManager;
    
    private Random rng = new Random();

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
    private ArrayList<String> tributeCalculating = new ArrayList<String>();
    private boolean _debug = false;
    
    private Integer _villagerValue;
    private Integer _babyVillagerValue;
    private Integer _ironGolemValue;
    /**
     *
     */
    protected Integer _emeraldTributeItem = -1;
    /**
     *
     */
    protected String _tributeType = "emerald",
    /**
     *
     */
    _tributeMethod = "direct";
    /**
     *
     */
    protected int _villageMinPopulation = 1;
    /**
     *
     */
    protected int _minStartingReputation;
    /**
     *
     */
    protected int _maxStartingReputation;
    /**
     *
     */
    protected int _discoverBonus;

    /**
     * Loads data and runs initialization tasks when enabling the plugin (e.g.
     * on server startup)
     */
    @Override
    public void onEnable() {
        // Initialize the village and siege manager objects
        _villageManager = new VillageManager(this);
        _siegeManager = new SiegeManager(this);

        // Register us to handle events
        getServer().getPluginManager().registerEvents(this, this);

        baseConfiguration = new FileManager(this, "config.yml");
        readBaseConfig();

        ranksConfiguration = new FileManager(this, "ranks.yml");
        readRanksConfig();
        
        villageConfiguration = new FileManager(this, "villages.yml");
        siegeConfiguration = new FileManager(this, "siege.yml");
        
        readAllVillageConfigs();//(getServer().getWorlds().get(0));
        readSiegeConfig();
        
        languageConfiguration = new FileManager(this, "language.yml");
        _languageManager = new LanguageManager(languageConfiguration.getConfigSection("strings").getValues(false));
        
        _dynmapManager = new DynmapManager(this);
        if(_dynmapManager.enable()) {
            getServer().getPluginManager().registerEvents(_dynmapManager, this);
            getLogger().info("UVVillagers Dynmap now listening for updates.");
        }
            
        
        startDayTimer();
/*
        //Start plugin metrics
        try {
            Metrics metrics = new Metrics(this);
            metrics.start();
        } catch (IOException e) {
            // Failed to submit the stats :-(
        }
*/
    }

    /**
     * Saves data when disabling the plugin (e.g. on server shutdown)
     */
    @Override
    public void onDisable() {
        _dynmapManager.disable();
        saveUpdatedVillages();
    }

    /**
     * Reads the plugin configuration.
     */
    private void readBaseConfig() {
        _minStartingReputation = baseConfiguration.getInt("minStartingReputation");
        _maxStartingReputation = baseConfiguration.getInt("maxStartingReputation");
        _discoverBonus = baseConfiguration.getInt("discoverBonus");
        _emeraldTributeItem = baseConfiguration.getInt("emeraldTributeItem");
        _tributeMethod = baseConfiguration.getString("tributeMethod");
        _tributeType = baseConfiguration.getString("tributeType");
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
            boolean isHostile = ranksConfiguration.getBoolean("ranks." + name + ".isHostile", false);
            boolean canTrade = ranksConfiguration.getBoolean("ranks." + name + ".canTrade", true);
            _reputationRanks.add(new UVVillageRank(name, threshold, multiplier, isHostile, canTrade));
        }
        Collections.sort(_reputationRanks);
        getLogger().info(String.format("%d reputation ranks loaded.", _reputationRanks.size()));
    }
    
    
    private void readAllVillageConfigs() {
        for (World world : getServer().getWorlds()) {
            if (world.getName() != null)
                readVillageConfig(world);
        }
    }
    /**
     * Reads the village configuration.
     */
    private void readVillageConfig(World world) {
        _villageManager.loadVillages(villageConfiguration.getConfigSection("villages"), world);
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
                if (args[0].equalsIgnoreCase("dump")) {
                    if (sender instanceof Player) {
                        Player p = (Player) sender;
                        if (p.hasPermission("uvv.admin")) {
                            dumpDataToSender(sender, args);
                        }
                    } else {
                        dumpDataToSender(sender, args);
                    }
                } else if (args[0].equalsIgnoreCase("reload")) {
                    if (sender instanceof Player) {
                        Player p = (Player) sender;
                        if (p.hasPermission("uvv.admin")) {
                            if (args.length > 1 && args[1].equalsIgnoreCase("villages")) {
                                sender.sendMessage("Reloading villages from disk...");
                                // Reload the config from disk.
                                villageConfiguration.loadFile();
                                // Process the new config.
                                readAllVillageConfigs();
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
                            readAllVillageConfigs();
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
                            startSiege(p.getWorld());
                        } else {
                            sender.sendMessage("You don't have permission to do that.");
                        }
                    } else {
                        sender.sendMessage("Starting a siege...");
                        startSiege();
                    }
                } else if (args[0].equalsIgnoreCase("siegeinfo")) {
                    sender.sendMessage(ChatColor.GOLD + " - UVVillagers Siege Info - ");
                    if (sender instanceof Player) {
                        Player p = (Player) sender;
                        if (p.hasPermission("uvv.siegeinfo")) {
                            ArrayList<String> messages = _siegeManager.getSiegeInfo(p.getLocation().getWorld());
                            sender.sendMessage(messages.toArray(new String[messages.size()]));
                        } else {
                            sender.sendMessage("You don't have permission to do that.");
                        }
                    } else {
                        List<World> worlds = getServer().getWorlds();
                        for (World world : worlds) {
                            ArrayList<String> messages = _siegeManager.getSiegeInfo(world);
                            sender.sendMessage(messages.toArray(new String[messages.size()]));
                        }
                    }
                } else if (args[0].equalsIgnoreCase("list")) {
                    sender.sendMessage(ChatColor.GOLD + " - UVVillagers Village List - ");
                    if (sender instanceof Player) {
                        Player p = (Player) sender;
                        if (p.hasPermission("uvv.villageinfo")) {
                            sendVillageInfo(sender, _villageManager.getAllVillages(p.getLocation().getWorld()));
                        } else {
                            sender.sendMessage("You don't have permission to do that.");
                        }
                    } else {
                        sendVillageInfo(sender, _villageManager.getAllVillages());
                    }
                } else if (args[0].equalsIgnoreCase("loaded")) {
                    sender.sendMessage(ChatColor.GOLD + " - UVVillagers Villages Loaded - ");
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
                    sender.sendMessage(ChatColor.GOLD + " - UVVillagers Nearby Villages - ");
                    if (sender instanceof Player) {
                        Player p = (Player) sender;
                        if (p.hasPermission("uvv.villageinfo")) {
                            sendVillageInfo(sender, _villageManager.getVillagesNearLocation(p.getLocation(), tributeRange));
                        } else {
                            sender.sendMessage("You don't have permission to do that.");
                        }
                    } else {
                        sender.sendMessage("Silly console, you can't do that!");
                    }
                } else if (args[0].equalsIgnoreCase("current")) {
                    sender.sendMessage(ChatColor.GOLD + " - UVVillagers Current Village - ");
                    if (sender instanceof Player) {
                        Player p = (Player) sender;
                        if (p.hasPermission("uvv.villageinfo")) {
                            sendVillageInfo(sender, _villageManager.getClosestVillageToLocation(p.getLocation(), tributeRange));
                        } else {
                            sender.sendMessage("You don't have permission to do that.");
                        }
                    } else {
                        sender.sendMessage("Silly console, you can't do that!");
                    }
                } else if (args[0].equalsIgnoreCase("rename")) {
                    sender.sendMessage(ChatColor.GOLD + " - UVVillagers Rename Village - ");
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
                                        if (_villageManager.getVillageByKey(p.getWorld(), newName) == null) {
                                            if (_villageManager.renameVillage(p.getWorld(), village.getName(), newName)) {
                                                sender.sendMessage(ChatColor.DARK_GREEN + getLanguageManager().getString("village_rename_success").replace("@village", village.getName()));
                                            } else {
                                                sender.sendMessage(ChatColor.DARK_RED + getLanguageManager().getString("village_rename_failure"));
                                            }
                                            sendVillageInfo(sender, _villageManager.getVillagesNearLocation(p.getLocation(), tributeRange));
                                        } else {
                                            sender.sendMessage(ChatColor.DARK_RED + getLanguageManager().getString("village_rename_duplicate").replace("@village", newName));
                                        }
                                    } else {
                                        sender.sendMessage(ChatColor.DARK_RED + getLanguageManager().getString("village_rename_not_top_rep").replace("@village", village.getName()).replace("@toprep", village.getTopReputation()));
                                    }

                                } else {
                                    sender.sendMessage(ChatColor.DARK_RED + getLanguageManager().getString("village_rename_no_village"));
                                }
                            } else {
                                sender.sendMessage(ChatColor.DARK_RED + getLanguageManager().getString("village_rename_no_name"));
                            }
                        } else {
                            sender.sendMessage("You don't have permission to do that.");
                        }
                    } else {
                        sender.sendMessage("Silly console, you can't do that!");
                    }
                } else {
                    sendHelp(sender);
                }
            } else {
                sendHelp(sender);
            }
            return true;
        }
        return false;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + " - UVVillagers Help - ");
        sender.sendMessage(ChatColor.GRAY + "Try one of the following:");
        sender.sendMessage(ChatColor.GRAY + " /uvv save");
        sender.sendMessage(ChatColor.GRAY + " /uvv reload");
        sender.sendMessage(ChatColor.GRAY + " /uvv list - lists all villages in this world");
        sender.sendMessage(ChatColor.GRAY + " /uvv loaded - lists all loaded villages in this world");
        sender.sendMessage(ChatColor.GRAY + " /uvv nearby - lists villages in tribute range");
        sender.sendMessage(ChatColor.GRAY + " /uvv current - displays current village info");
        sender.sendMessage(ChatColor.GRAY + " /uvv rename New Village Name - renames the village you're in");
        sender.sendMessage(ChatColor.GRAY + " /uvv siegeinfo - prints out the status of the current siege");
    }
    private void dumpDataToSender(CommandSender sender, String[] args) {
        if (args.length > 1) {
            if (args[1].equalsIgnoreCase("language")) {
                sender.sendMessage("Messages Loaded");
                for (Map.Entry<String, String> stringEntry : getLanguageManager().getAllStrings().entrySet()) {
                    sender.sendMessage(String.format(" - %s: %s", stringEntry.getKey(), stringEntry.getValue()));
                }
                
            } else if (args[1].equalsIgnoreCase("corevillages")) {
                sender.sendMessage("Core Villages Loaded");
                for (World world : getServer().getWorlds()) {
                    sender.sendMessage(" - " + world.getName());
                    for (Village coreVillage : _villageManager.getLoadedCoreVillages(world)) {
                        sender.sendMessage(String.format("   - %d %d %d: %d doors, %d villagers, %d size", coreVillage.getCenter().x, coreVillage.getCenter().y, coreVillage.getCenter().z, coreVillage.getDoorCount(), coreVillage.getPopulationCount(), coreVillage.getSize()));
                    }
                }
                
            } else if (args[1].equalsIgnoreCase("villages")) {
                sender.sendMessage("UVVillages");
                for (World world : getServer().getWorlds()) {
                    sender.sendMessage(" - " + world.getName());
                    Map<String, UVVillage> villages = _villageManager.getAllVillages(world);
                    if (villages != null && villages.size() > 0) {
                        for (Map.Entry<String, UVVillage> villageEntry : villages.entrySet()) {
                            sender.sendMessage(String.format("   - %d %d %d (X: %d-%d, Y: %d-%d, Z: %d-%d): %d doors, %d villagers, %d size", 
                                    villageEntry.getValue().getLocation().getBlockX(), 
                                    villageEntry.getValue().getLocation().getBlockY(), 
                                    villageEntry.getValue().getLocation().getBlockZ(), 
                                    villageEntry.getValue().getMinX(), 
                                    villageEntry.getValue().getMaxX(), 
                                    villageEntry.getValue().getMinY(), 
                                    villageEntry.getValue().getMaxY(), 
                                    villageEntry.getValue().getMinZ(), 
                                    villageEntry.getValue().getMaxZ(), 
                                    villageEntry.getValue().getDoorCount(),
                                    villageEntry.getValue().getPopulation(),
                                    villageEntry.getValue().getSize()));
                            Map<String, Integer> reputations = villageEntry.getValue().getPlayerReputations();
                            for (Map.Entry<String, Integer> reputationEntry : reputations.entrySet()) {
                                sender.sendMessage(String.format("     - %s: %d reputation (%s)", reputationEntry.getKey(), reputationEntry.getValue(), this.getRank(reputationEntry.getValue()).getName()));
                            }
                        }
                    }
                }
            } else if (args[1].equalsIgnoreCase("siege")) {
                sender.sendMessage("Siege Config");
/*                for (String key : _siegeManager._killValues.keySet()) {
                    String color = ((_siegeManager.getExtraMobChance(key) > 0)?ChatColor.GREEN:ChatColor.GRAY).toString();
                    sender.sendMessage(String.format("%s - %s: 1 per %d villagers has a %d percent chance to spawn. Max %d. Players get %d for a killing blow.", color, key, _siegeManager.getPopulationThreshold(key), _siegeManager.getExtraMobChance(key), _siegeManager.getMaxToSpawn(key), _siegeManager._killValues.get(key)));
                }
*/            }
        } else {
            sender.sendMessage("Please specify corevillages, villages, siege, or core.");
        }
    }
    
    /**
     * Sends village information to the sender
     *
     * @param sender The command sender.
     * @param villages A hashmap of village objects.
     */
    private void sendVillageInfo(CommandSender sender, Map<String, UVVillage> villages) {
        if (villages != null) {
            for (Map.Entry<String, UVVillage> villageEntry : villages.entrySet()) {
                sendVillageInfo(sender, villageEntry.getValue());
            }
        }
    }

    /**
     * Sends village information to the sender
     *
     * @param sender The command sender.
     * @param village A single village object.
     */
    private void sendVillageInfo(CommandSender sender, UVVillage village) {
        if (village != null) {
            String rankString = "";
            if (sender instanceof Player) {
                rankString = String.format(" (%s)", getRank(village.getPlayerReputation(sender.getName())).getName());
            }
            sender.sendMessage(ChatColor.GRAY + String.format("%s%s: %d doors, %d villagers, %d block size.", village.getName(), rankString, village.getDoorCount(), village.getPopulation(), village.getSize()));
        }
    }

    /**
     * Player move listener. Fires when a player moves.
     *
     * @param event PlayerMoveEvent
     */
    @EventHandler
    private void onPlayerMoveEvent(PlayerMoveEvent event) {
        _villageManager.updatePlayerProximity(event.getTo(), event.getPlayer(), tributeRange);
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
        if(_dynmapManager != null && _dynmapManager.isEnabled()) _dynmapManager.onUVVillageEvent(event);
        debug(event.getMessage());
        switch (event.getType()) {
            case SIEGE_BEGAN:
                getServer().broadcastMessage(ChatColor.RED + getLanguageManager().getString("siege_began").replace("@village", event.getKey()));
                break;
            case SIEGE_ENDED:
                ArrayList<String> messages = event.getSiegeMessage();
                Iterator<String> messageIterator = messages.iterator();
                while (messageIterator.hasNext()) {
                    getServer().broadcastMessage(messageIterator.next());
                }
                break;
            case ABANDONED:
                getServer().broadcastMessage(ChatColor.DARK_GRAY + getLanguageManager().getString("village_abandoned").replace("@village", event.getKey()));
                break;
            case MERGED:
                getServer().broadcastMessage(ChatColor.GRAY + getLanguageManager().getString("village_merged").replace("@village", event.getKey()).replace("@newvillage", event.getMergeMessage()));
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
                // Calculate tributes
                debug("Calculating tribute in " + event.getWorld().getName());
                calculateTribute(event.getWorld());
                // End siege tracking.
                debug("Ending active sieges in " + event.getWorld().getName());
                _siegeManager.endSiege(event.getWorld());
                break;

            case DUSK:
                // clear active siege just in case something is missing
                debug("Clearing siege data in " + event.getWorld().getName());
                _siegeManager.clearSiege(event.getWorld());
                if (_tributeMethod.equalsIgnoreCase("mayor"))
                    _villageManager.clearTributes(event.getWorld());
                break;
            case MIDNIGHT: 
                // Try to start a siege if using custom sieges
                if(!_siegeManager.isSiegeActive(event.getWorld()) && !_siegeManager.usingCoreSieges()) {
                    debug("Trying to start a siege in " + event.getWorld().getName());
                    if (_siegeManager.getChanceOfSiege() > getRandomNumber(0, 99)) {
                        debug("A siege is happening tonight in " + event.getWorld().getName());
                        startSiege(event.getWorld());
                    }
                }
                break;
            case CHECK:
                // Update villages
                _villageManager.matchVillagesToCore(event.getWorld());
                // Check for villages that need to merge
                _villageManager.checkForMerge(event.getWorld());
                // Tick village proximities 
                _villageManager.tickProximityReputations(event.getWorld());
                // Tick mayor movement 
                _villageManager.tickMayorMovement(event.getWorld());
                break;
            default:
                break;
        }
    }
    
    @EventHandler
    private void onWorldLoaded(WorldLoadEvent event) {
        readVillageConfig(event.getWorld());
    }

    @EventHandler
    private void onPlayerInteractEntityEvent(PlayerInteractEntityEvent event) {
        if (event.getRightClicked().getType() == EntityType.VILLAGER) {
            CraftVillager villager = (CraftVillager) event.getRightClicked();
            UVVillage village = _villageManager.getClosestVillageToLocation(villager.getLocation(), 8);
            if (village != null) {
                // Is it a mayor?
                if (villager.isCustomNameVisible()) {
                    String name = villager.getCustomName();
                    if (name.contains("Mayor of")) {
                        debug(event.getPlayer().getName() + " talked to " + name + " in " + village.getName());
                        if (_tributeType.equalsIgnoreCase("emerald") && _tributeMethod.equalsIgnoreCase("mayor")) {
                            giveEmeraldTribute(event.getPlayer(), village.collectEmeraldTribute(event.getPlayer().getName()));
                            event.setCancelled(true);
                        }
                    }
                } else {
                    // Is the player's reputation high enough to trade with this village?
                    UVVillageRank rank = getRank(village.getPlayerReputation(event.getPlayer().getName()));
                    if (!rank.canTrade()) {
                        event.setCancelled(true);
                        event.getPlayer().sendMessage(ChatColor.DARK_RED + getLanguageManager().getString("village_no_trade").replace("@village", village.getName()));
                    }
                }
            }
        } else if (event.getRightClicked().getType() == EntityType.ITEM_FRAME) {
            if (event.getPlayer().getItemInHand().getType() == Material.EMERALD && 
                    ((ItemFrame)event.getRightClicked()).getItem().getType() == Material.AIR) {
                
                UVVillage village = _villageManager.getClosestVillageToLocation(event.getRightClicked().getLocation(), 8);
                Villager nearestVillager = null;
                double nearestDistanceSquared = 64;
                if (village != null) {
                    
                    if (village.getMayor() != null) {
                        event.getPlayer().sendMessage(ChatColor.DARK_RED + getLanguageManager().getString("mayor_already_exists").replace("@village", village.getName()));
                        event.setCancelled(true);
                        return;
                    }
                    if (!village.getTopReputation().equalsIgnoreCase(event.getPlayer().getName())) {
                        event.getPlayer().sendMessage(ChatColor.DARK_RED + getLanguageManager().getString("mayor_not_top_rep").replace("@village", village.getName()).replace("@toprep",village.getTopReputation()));
                        event.setCancelled(true);
                        return;
                    }
                    
                    List<Entity> entitiesNearby = event.getRightClicked().getNearbyEntities(8, 8, 8);
                    for (Entity entity : entitiesNearby) {
                        if (entity.getType() == EntityType.VILLAGER) {
                            double nearestDistance = entity.getLocation().distanceSquared(event.getRightClicked().getLocation());
                            if (nearestDistance < nearestDistanceSquared)
                            nearestVillager = (Villager) entity;
                            nearestDistanceSquared = nearestDistance;
                        }
                    }
                    if (nearestVillager == null) {
                        event.getPlayer().sendMessage(ChatColor.DARK_RED + getLanguageManager().getString("mayor_no_villagers").replace("@village", village.getName()));
                    } else {
                        village.setMayorSign((ItemFrame) event.getRightClicked());
                        village.setMayor(nearestVillager);
                        debug("Mayor is " + nearestVillager.getEntityId());
                        
                        event.getPlayer().sendMessage(ChatColor.DARK_GREEN + getLanguageManager().getString("mayor_created").replace("@village", village.getName()));
                    }
                }
            }
        }
    }
    
    
    private void startSiege() {
        List<World> worlds = getServer().getWorlds();
        for (World world : worlds) {
            startSiege(world);
        }
    }
    /**
     * Forces a siege to start
     */
    private void startSiege(World world) {
        Map<String, UVVillage> loadedVillages = _villageManager.getLoadedVillages(world);
        if (loadedVillages.size() > 0) {
            int index = getRandomNumber(0, loadedVillages.size()-1);
            UVVillage village = loadedVillages.values().toArray(new UVVillage[loadedVillages.size()])[index];
            int xOffset = getRandomNumber(village.getSize() / -2, village.getSize() / 2);
            int zOffset = getRandomNumber(village.getSize() / -2, village.getSize() / 2);
            debug(String.format("Random offset X=%d Y=%d", xOffset, zOffset));
            Location location = village.getLocation().clone().add(xOffset, 0, zOffset);
            debug(String.format("Firing up a siege at %s in %s (%s)!", location.toString(), village.getName(), village.getLocation().toString()));
            _siegeManager.startSiege(location, village);
        } else {
            debug(String.format("No villages were loaded in %s. No siege tonight!",world.getName()));
        }
    }
    
    /**
     * Runs tribute calculations for a world.
     *
     * @param world The world for which to calculate
     */
    private void calculateTribute(World world) {
        
        if (!tributeCalculating.contains(world.getName())) {
            // TODO: ADD MULTIWORLD SUPPORT!
            tributeCalculating.add(world.getName());

            // Make sure the villages are up to date
            _villageManager.matchVillagesToCore(world);

            // Get the player list
            List<Player> players = world.getPlayers();

            // Step through the players
            for (Player player : players) {
                if (player.hasPermission("uvv.tribute")) {
                    int tributeAmount = 0, killBonus = 0;

                    // Get the villages within tribute range of the player
                    Map<String, UVVillage> villages = _villageManager.getVillagesNearLocation(player.getLocation(), tributeRange);

                    // if a siege is active, calculate siege tribute bonuses  
                    if (_siegeManager.isSiegeActive(world)) {
                        int kills = _siegeManager.getPlayerKills(player.getName(), world);
                        for (int i = 0; i < kills; i++) {
                            killBonus += getRandomNumber(minPerSiegeKill, maxPerSiegeKill);
                        }
                    }

                    debug(String.format("%s: ", player.getName()));

                    for (Map.Entry<String, UVVillage> village : villages.entrySet()) {
                        int villageTributeAmount = 0, siegeBonus = 0, siegeKillTributeAmount = 0;
                        debug(String.format(" - %s", village.getKey()));
                        int population = village.getValue().getPopulation();

                        int numVillagerGroups = (population - (population % villagerCount)) / villagerCount;

                        debug(String.format(" - Villagers: %d (%d tribute groups)", population, numVillagerGroups));

                        // If this village was the one sieged, give the kill bonus and an extra survival "base siege" thankfulness bonus
                        if (_siegeManager.isSiegeActive(world) && village.getKey().equalsIgnoreCase(_siegeManager.getVillage(world).getName())) {
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
                        tributeAmount += (int) (villageTributeAmount * multiplier);
                        if (_tributeMethod.equalsIgnoreCase("mayor")) {
                            village.getValue().setEmeraldTribute(player.getName(), ((int) (villageTributeAmount * multiplier)));
                            if (villageTributeAmount * multiplier > 0) {
                                if (village.getValue().getMayor() != null) {
                                    player.sendMessage(getLanguageManager().getString("tribute_mayor_ready").replace("@village", village.getValue().getName()));
                                } else {
                                    player.sendMessage(getLanguageManager().getString("tribute_no_mayor").replace("@village", village.getValue().getName()));
                                    player.sendMessage("(To create a Mayor, just place an item frame near a villager and insert an emerald!)");
                                }
                            }
                        }

                    }
                    
                    if (villages.size() > 0) {
                        if (_tributeMethod.equalsIgnoreCase("direct") && _tributeType.equalsIgnoreCase("emerald")) {
                            giveEmeraldTribute(player, (Integer) tributeAmount);
                        }
                    } else {
                        player.sendMessage(getLanguageManager().getString("tribute_too_far"));
                    }
                }
            }
        }
        tributeCalculating.remove(world.getName());
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
            current = new UVVillageRank("unknown", Integer.MIN_VALUE, 0, false, true);
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
     * Utility function to get the VillageManager instance.
     *
     * @return
     */
    public LanguageManager getLanguageManager() {
        return _languageManager;
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
    
    /**
     *
     * @param message
     */
    protected void debug(String message) {
        if (_debug) {
            getLogger().info(message);
        }
    }

    private void giveEmeraldTribute(Player player, Integer tributeAmount) {
        if (tributeAmount > 0) {
           ItemStack items;
           if (_emeraldTributeItem > 0 && Material.getMaterial(_emeraldTributeItem) != null) {
               items = new ItemStack(Material.getMaterial(_emeraldTributeItem), tributeAmount);
           } else {
               items = new ItemStack(Material.EMERALD, tributeAmount);
           }
           player.getInventory().addItem(items);

           player.sendMessage(getLanguageManager().getString("tribute_emeralds_gain").replace("@amount", tributeAmount.toString()).replace("@item", items.getType().name()));
           debug(String.format("%s received %d %s.", player.getName(), tributeAmount, items.getType().name()));
       } else {
           player.sendMessage(getLanguageManager().getString("tribute_emeralds_none"));
       }
    }

}
