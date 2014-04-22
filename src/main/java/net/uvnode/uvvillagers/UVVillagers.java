package net.uvnode.uvvillagers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import net.minecraft.server.v1_7_R3.Village;
import net.uvnode.uvvillagers.util.FileManager;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Chest;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_7_R3.entity.CraftVillager;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Villager;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.world.WorldLoadEvent;

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
    private List<UVVillageRank> _reputationRanks = new ArrayList<>();
    private int _tributeRange,
            villagerCount,
            minPerVillagerCount,
            maxPerVillagerCount,
            baseSiegeBonus,
            minPerSiegeKill,
            maxPerSiegeKill,
            timerInterval = 200;
    private ArrayList<String> tributeCalculating = new ArrayList<>();
    private ArrayList<String> _enabledWorlds = new ArrayList<>();
    private boolean _worldFilterEnabled = false;
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
     *
     */
    protected boolean _dynmapDefaultVisible;

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
        updateBaseConfig();
        readBaseConfig();

        ranksConfiguration = new FileManager(this, "ranks.yml");
        readRanksConfig();
        
        villageConfiguration = new FileManager(this, "villages.yml");
        siegeConfiguration = new FileManager(this, "siege.yml");
        
        readAllVillageConfigs();//(getServer().getWorlds().get(0));
        readSiegeConfig();
        
        languageConfiguration = new FileManager(this, "language.yml");
        updateLanguageConfig();
        _languageManager = new LanguageManager(languageConfiguration.getConfigSection("strings").getValues(false));
        
        _dynmapManager = new DynmapManager(this);
        if(_dynmapManager.enable()) {
            getServer().getPluginManager().registerEvents(_dynmapManager, this);
            getLogger().info("UVVillagers Dynmap now listening for updates.");
        }
            
        
        startDayTimer();
    }

    /**
     * Saves data when disabling the plugin (e.g. on server shutdown)
     */
    @Override
    public void onDisable() {
        _dynmapManager.disable();
        saveUpdatedVillages();
        // Delete our objects to minimize memory leak on /reload
        _languageManager = null;
        _siegeManager = null;
        _villageManager = null;
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
        _tributeRange = baseConfiguration.getInt("tributeRange");
        villagerCount = baseConfiguration.getInt("villagerCount");
        _villageMinPopulation = baseConfiguration.getInt("villageMinPopulation");
        minPerVillagerCount = baseConfiguration.getInt("minPerVillagerCount");
        maxPerVillagerCount = baseConfiguration.getInt("maxPerVillagerCount");
        baseSiegeBonus = baseConfiguration.getInt("baseSiegeBonus");
        minPerSiegeKill = baseConfiguration.getInt("minPerSiegeKill");
        maxPerSiegeKill = baseConfiguration.getInt("maxPerSiegeKill");
        _dynmapDefaultVisible = baseConfiguration.getBoolean("dynmapDefaultVisible");
        _worldFilterEnabled = baseConfiguration.getBoolean("useWorlds");
        _enabledWorlds = baseConfiguration.getStringList("worlds");
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

                    if (isWorldEnabled(worlds.get(i).getName())) {
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

            }
        }, 0, timerInterval);
    }
    
    /**
     * Convenience overload which assumes that consoleOK should be true. 
     *
     * @param sender    The command sender
     * @param perm      The permission to check
     * @return true if the user has the permission or is a console
     */
     private boolean hasPerms(CommandSender sender, String perm) {
        return hasPerms(sender,perm,true);
     }
    /**
     * Check if player has a given permission, or if the command is from the console
     *
     * @param sender    The command sender
     * @param perm      The permission to check
     * @param consoleOK When true, non-player originating commands will have all permissions.
     * @return true if the user has the permission
     */
    private boolean hasPerms(CommandSender sender, String perm, boolean consoleOK) {
        if (sender instanceof Player) {
            Player p = (Player) sender;
            if (p.hasPermission(perm)) {
                return true;
            } else {
                sender.sendMessage("You don't have permission to do that.");
                return false;
            }
        } else {
            if (consoleOK) {
                return true;
            } else {
                sender.sendMessage("Silly console! You can't do that.");
                return false;
            }
        }
    }

    /**
     * Command listener
     *
     * @param sender The command sender.
     * @param cmd The command sent.
     * @param label The command label.
     * @param args The command arguments.
     * @return True if the command was processed.
     */
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        
        if (cmd.getName().equalsIgnoreCase("uvv")) {
            if (args.length > 0) {
                switch(args[0].toLowerCase()) {
                   case "dump":
                        if (hasPerms(sender,"uvv.admin")) {
                            dumpDataToSender(sender, args);
                        }   
                    break;
                    case "reload":
                        if (hasPerms(sender,"uvv.admin")) {
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
                    break;
                    case "debug":
                        if (!(sender instanceof Player)) {
                            _debug = !_debug;
                            sender.sendMessage("Debug is " + (_debug?"On":"Off"));
                        }
                    break;
                    case "save":
                        if (hasPerms(sender,"uvv.admin")) {
                            sender.sendMessage("Saving...");
                            saveUpdatedVillages();
                        }
                    break;
                    case "startsiege":
                        if (hasPerms(sender,"uvv.startsiege")) {
                            World world;
                            switch (args.length) {
                                case 1: // 1 arg means no details were specified. Start sieges on all worlds.
                                    sender.sendMessage("Starting a siege on all worlds...");
                                    startSiege();
                                    break;
                                case 2: // 2 args means a world was specified. Start siege in one world.
                                    world = getServer().getWorld(args[1]);
                                    if (world != null) {
                                            sender.sendMessage("Starting a siege in "+world+"...");
                                            startSiege(world);
                                    } else {
                                            sender.sendMessage(ChatColor.RED+"Unable to start a siege. '"+world+"' does not exist...");
                                    }
                                    break;
                                default: // 3 or more args means a village was specified. Concat the name and start a siege there.
                                    world = getServer().getWorld(args[1]);
                                    UVVillage village = null;
                                    
                                    if (world != null) {
                                        String villageName = "";
                                        for (int i = 2; i < args.length; i++)
                                            villageName += " " + args[i];
                                        villageName = villageName.trim();
                                        if (villageName.equalsIgnoreCase("current")) {
                                            if (sender instanceof Player) {
                                                village = _villageManager.getClosestVillageToLocation(((Player)sender).getLocation(), 0);
                                            } else {
                                                sender.sendMessage(ChatColor.RED+"You can't use the 'current' syntax as console.");
                                            }
                                        } else {
                                            village = _villageManager.getVillageByKey(world, villageName);
                                        }
                                        if (village != null) {
                                            sender.sendMessage("Starting a siege in "+village.getName()+"...");
                                            startSiege(village);
                                        } else {
                                            if (villageName == "current") {
                                                sender.sendMessage(ChatColor.RED+"You're not in a village!");
                                            } else {
                                                sender.sendMessage(ChatColor.RED+"Unable to start a siege. '"+villageName+"' does not exist in '"+world.getName()+"'...");
                                            }
                                        }
                                    } else {
                                            sender.sendMessage(ChatColor.RED+"Unable to start a siege. '"+world.getName()+"' does not exist...");
                                    }
                                    break;
                                    
                            }
                        }
                    break;
                    case "siegeinfo":
                        if (hasPerms(sender,"uvv.siegeinfo")) {
                            sender.sendMessage(ChatColor.GOLD + " - UVVillagers Siege Info - ");
                            if(args.length > 1) {
                        		World world = getServer().getWorld(args[1]);
                        		if (world != null) {
                        			ArrayList<String> messages = _siegeManager.getSiegeInfo(world);
    	                            sender.sendMessage(messages.toArray(new String[messages.size()]));
                        		} else {
                        			sender.sendMessage(ChatColor.RED+"Unable to get siege info. '"+world+"' does not exist...");
                        		}
                        	} else {
                        		List<World> worlds = getServer().getWorlds();
    	                        for (World world : worlds) {
    	                            ArrayList<String> messages = _siegeManager.getSiegeInfo(world);
    	                            sender.sendMessage(ChatColor.GREEN+"["+world.getName()+"]");
    	                            sender.sendMessage(messages.toArray(new String[messages.size()]));
    	                        }
                        	}
                            
                        }
                    break;
                    case "list":
                        if (hasPerms(sender,"uvv.villageinfo")) {
                            sender.sendMessage(ChatColor.GOLD + " - UVVillagers Village List - ");
                            sendVillageInfo(sender, _villageManager.getAllVillages());
                        }
                    break;
                    case "loaded":
                        if (hasPerms(sender,"uvv.villageinfo")) {
                            sender.sendMessage(ChatColor.GOLD + " - UVVillagers Villages Loaded - ");
                            sendVillageInfo(sender, _villageManager.getLoadedVillages());
                        }
                    break;
                    case "nearby":
                        if (hasPerms(sender,"uvv.villageinfo", false)) {
                            Player p = (Player) sender;
                            sender.sendMessage(ChatColor.GOLD + " - UVVillagers Nearby Villages - ");
                            sendVillageInfo(sender, _villageManager.getVillagesNearLocation(p.getLocation(), _tributeRange));
                        }
                    break;
                    case "current":
                        if (hasPerms(sender,"uvv.villageinfo", false)) {
                            Player p = (Player) sender;
                            sender.sendMessage(ChatColor.GOLD + " - UVVillagers Current Village - ");
                            sendVillageInfo(sender, _villageManager.getClosestVillageToLocation(p.getLocation(), _tributeRange));
                        }
                    break;
                    case "setserver":
                        if (hasPerms(sender,"uvv.admin", false)) {
                            sender.sendMessage(ChatColor.GOLD + " - UVVillagers Set Server Village - ");
                            Player p = (Player) sender;
                            UVVillage v = _villageManager.getClosestVillageToLocation(p.getLocation(), _tributeRange);
                            if (v != null) {
                                v = _villageManager.toggleServerVillage(v);
                                if (v != null) {
                                    if (v.isServerVillage()) {
                                        sender.sendMessage(_languageManager.getString("server_village").replace("@village", v.getName()).replace("@owner", "server"));
                                    } else {
                                        sender.sendMessage(_languageManager.getString("server_village").replace("@village", v.getName()).replace("@owner", "players"));
                                    }
                                } else {
                                    sender.sendMessage("An unknown error occurred while running /setserver");
                                }
                            } else {
                                sender.sendMessage("You are not in a village.");
                            }
                        }
                    break;
                    case "rename":
                        if (hasPerms(sender,"uvv.rename", false)) {
                            sender.sendMessage(ChatColor.GOLD + " - UVVillagers Rename Village - ");
                            Player p = (Player) sender;
                            if (args.length > 1) {
                                String newName = "";
                                newName += args[1];
                                if (args.length > 2) {
                                    for (int i = 2; i < args.length; i++) {
                                        newName += " " + args[i];
                                    }
                                }
                                UVVillage village = _villageManager.getClosestVillageToLocation(p.getLocation(), _tributeRange);
                                if (village != null) {
                                    if (village.getTopReputation().equalsIgnoreCase(p.getName()) || village.getTopReputation().equalsIgnoreCase("Server") && p.hasPermission("uvv.admin")) {
                                        if (_villageManager.getVillageByKey(p.getWorld(), newName) == null) {
                                            if (_villageManager.renameVillage(p.getWorld(), village.getName(), newName)) {
                                                sender.sendMessage(ChatColor.DARK_GREEN + getLanguageManager().getString("village_rename_success").replace("@village", village.getName()));
                                            } else {
                                                sender.sendMessage(ChatColor.DARK_RED + getLanguageManager().getString("village_rename_failure"));
                                            }
                                            sendVillageInfo(sender, _villageManager.getVillagesNearLocation(p.getLocation(), _tributeRange));
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
                        }
                    break;
                    default:
                        sender.sendMessage("Unrecognized Command.");
                        sendHelp(sender);
                    break;
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
        if (hasPerms(sender,"uvv.admin")) {
            sender.sendMessage(ChatColor.GRAY + " /uvv reload - reloads the configuration from disk");
            sender.sendMessage(ChatColor.GRAY + " /uvv reload villages - reloads the village list from disk (changes since last save will be lost)");
            sender.sendMessage(ChatColor.GRAY + " /uvv save - saves villages to disk");
        }
        if (hasPerms(sender,"uvv.villageinfo")) {
            sender.sendMessage(ChatColor.GRAY + " /uvv list - lists  all known villages");
            sender.sendMessage(ChatColor.GRAY + " /uvv loaded - lists villages currently loaded");
            sender.sendMessage(ChatColor.GRAY + " /uvv nearby - lists villages you're near");
            sender.sendMessage(ChatColor.GRAY + " /uvv current - lists  your current village");
        }
        if (hasPerms(sender,"uvv.rename")) {
            sender.sendMessage(ChatColor.GRAY + " /uvv rename - rename the village you're in");
        }
        if (hasPerms(sender,"uvv.siegeinfo")) {
            sender.sendMessage(ChatColor.GRAY + " /uvv siegeinfo - displays siege status data");
        }
        if (hasPerms(sender,"uvv.startsiege")) {
            sender.sendMessage(ChatColor.GRAY + " /uvv startsiege - starts a siege in a random village in each valid world. no tribute bonus granted if it's daytime");
            sender.sendMessage(ChatColor.GRAY + " /uvv startsiege <world> - starts a siege in a random village in the specified world");
            sender.sendMessage(ChatColor.GRAY + " /uvv startsiege <world> <village name> - starts a siege in a specific village");
            sender.sendMessage(ChatColor.GRAY + " /uvv startsiege <world> current - starts a siege in your current village");
        }
        if (hasPerms(sender,"uvv.admin")) {
            sender.sendMessage(ChatColor.GRAY + " /uvv setserver - toggles the current village between player-/server-owned");
        }
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
            sender.sendMessage(ChatColor.GRAY + String.format(ChatColor.GREEN+"[%s]"+ChatColor.WHITE+" %s%s: %d doors, %d villagers, %d block size.", village.getLocation().getWorld().getName(), village.getName(), rankString, village.getDoorCount(), village.getPopulation(), village.getSize()));
            if(village.getMayor() != null) {
                Location mv = village.getMayor().getLocation();
                sender.sendMessage(ChatColor.GRAY + String.format("The Mayor is currently at %d %d %d", mv.getBlockX(), mv.getBlockY(), mv.getBlockZ()));
            }
        }
    }

    /**
     * Player move listener. Fires when a player moves.
     *
     * @param event PlayerMoveEvent
     */
    /*
    @EventHandler
    private void onPlayerMoveEvent(PlayerMoveEvent event) {
        if (!isWorldEnabled(event.getTo().getWorld().getName())) return;
        _villageManager.updatePlayerProximity(event.getTo(), event.getPlayer(), _tributeRange);
    }
*/
    /**
     * CreatureSpawnEvent listener. Fires when a creature spawns.
     *
     * @param event CreatureSpawnEvent
     */
    @EventHandler
    private void onCreatureSpawnEvent(CreatureSpawnEvent event) {
        if (!isWorldEnabled(event.getLocation().getWorld().getName())) return;
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
        if (!isWorldEnabled(event.getEntity().getWorld().getName())) return;
        if (!_siegeManager.checkDeath(event)) { // Increase rep marginally for non-siege kill near village
            UVVillage village = _villageManager.getClosestVillageToLocation(event.getEntity().getLocation(), 16);
            if (village != null && event.getEntity().getKiller() != null) {
                int kv = _siegeManager.getKillValue(event.getEntity()) / 3;
                if (kv == 0) kv++;
                village.modifyPlayerReputation(event.getEntity().getKiller().getName(), kv);
            }
        }
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
        
        if (event.getEntity().getType() == EntityType.VILLAGER) {
            CraftVillager v = (CraftVillager) event.getEntity();
            String n = v.getCustomName();
            if (n != null && n.contains("Mayor of ")) {
                try {
                    getServer().broadcastMessage(String.format("The %s has died by %s!", n, event.getEntity().getLastDamageCause().getEventName()));
                    _villageManager.getVillageByKey(event.getEntity().getWorld(), n.replace("Mayor of","")).setMayor(null);
                } catch (Exception e) {
                }
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
        //if(_dynmapManager != null && _dynmapManager.isEnabled()) _dynmapManager.onUVVillageEvent(event);
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
        if (!isWorldEnabled(event.getWorld().getName())) return;
        //debug(event.getMessage());
        switch (event.getType()) {
            case DAWN:
                // Calculate tributes
                debug("Calculating tribute in " + event.getWorld().getName());
                calculateTribute(event.getWorld());
                // End siege tracking.
                debug("Ending active sieges in " + event.getWorld().getName());
                _siegeManager.endSiege(event.getWorld());
                break;
/*            case NOON:
                    if(_starvationEnabled)
                        _villageManager.tickStarvation(event.getWorld());
                break;*/
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
                
                _villageManager.tickWorld(event.getWorld(), _tributeRange);
                _villageManager.checkPlayerProximities(event.getWorld(), _tributeRange);
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
        if (!isWorldEnabled(event.getWorld().getName())) return;
        readVillageConfig(event.getWorld());
    }

    @EventHandler
    private void onPlayerInteractEntityEvent(PlayerInteractEntityEvent event) {
        if (!isWorldEnabled(event.getPlayer().getWorld().getName())) return;
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
                    if (_tributeMethod.equalsIgnoreCase("mayor")) {
                        /* //Can now just replace the mayor
                        if (village.getMayor() != null) {
                            event.getPlayer().sendMessage(ChatColor.DARK_RED + getLanguageManager().getString("mayor_already_exists").replace("@village", village.getName()));
                            event.setCancelled(true);
                            return;
                        }*/
                        if (!village.getTopReputation().equalsIgnoreCase(event.getPlayer().getName()) && !(village.getTopReputation().equalsIgnoreCase("Server") && event.getPlayer().hasPermission("uvv.admin"))) {
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
                    } else if (_tributeMethod.equalsIgnoreCase("chest")) {
                        if (!village.getTopReputation().equalsIgnoreCase(event.getPlayer().getName()) && !(village.getTopReputation().equalsIgnoreCase("Server") && event.getPlayer().hasPermission("uvv.admin"))) {
                            event.getPlayer().sendMessage(ChatColor.DARK_RED + getLanguageManager().getString("chest_not_top_rep").replace("@village", village.getName()).replace("@toprep",village.getTopReputation()));
                            event.setCancelled(true);
                            return;
                        }
                        ItemFrame i = (ItemFrame) event.getRightClicked();
                        Location l = getItemFrameAttachedLocation(i);
                        if (l.getBlock().getType().equals((Material.CHEST))) {
                            village.setTributeChest(l);
                            event.getPlayer().sendMessage(ChatColor.DARK_GREEN + getLanguageManager().getString("chest_created").replace("@village", village.getName()));
                        }
/*                        if (!village.hasChest()) {
                        } else {
                            event.getPlayer().sendMessage(ChatColor.DARK_GREEN + getLanguageManager().getString("chest_already_exists").replace("@village", village.getName()));
                            event.setCancelled(true);
                        }*/
                    }
                }
            }
        }
    }
    
    
    /**
     * Forces a siege to start
     */
    private void startSiege() {
        List<World> worlds = getServer().getWorlds();
        for (World world : worlds) {
            startSiege(world);
        }
    }
    
    /**
     * Forces a siege to start in a specific world
     */
    private void startSiege(World world) {
        if (!isWorldEnabled(world.getName())) return;
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
     * Forces a siege to start in a specific village
     */
    private void startSiege(UVVillage village) {
        World world = village.getLocation().getWorld();
        if (!isWorldEnabled(world.getName())) return;
        Map<String, UVVillage> loadedVillages = _villageManager.getLoadedVillages(world);
        if (loadedVillages.containsValue(village)) {
            int xOffset = getRandomNumber(village.getSize() / -2, village.getSize() / 2);
            int zOffset = getRandomNumber(village.getSize() / -2, village.getSize() / 2);
            debug(String.format("Random offset X=%d Y=%d", xOffset, zOffset));
            Location location = village.getLocation().clone().add(xOffset, 0, zOffset);
            debug(String.format("Firing up a siege at %s in %s (%s)!", location.toString(), village.getName(), village.getLocation().toString()));
            _siegeManager.startSiege(location, village);
        } else {
            debug(String.format("%s was not loaded. No siege tonight!", village.getName()));
        }
    }
    
    /**
     * Runs tribute calculations for a world.
     *
     * @param world The world for which to calculate
     */
    private void calculateTribute(World world) {
        
        if (!isWorldEnabled(world.getName())) return;
        
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
                    Map<String, UVVillage> villages = _villageManager.getVillagesNearLocation(player.getLocation(), _tributeRange);

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
                        
                        // Apply mayor stuff
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
                        if (_tributeMethod.equalsIgnoreCase("chest") && village.getValue().hasChest() && tributeAmount > 0) {
                            ItemStack items;
                            if (_emeraldTributeItem > 0 && Material.getMaterial(_emeraldTributeItem) != null) {
                                items = new ItemStack(Material.getMaterial(_emeraldTributeItem), tributeAmount);
                            } else {
                                items = new ItemStack(Material.EMERALD, tributeAmount);
                            }
                            ((Chest)village.getValue().getChest().getBlock().getState()).getBlockInventory().addItem(items);
                            player.sendMessage(getLanguageManager().getString("tribute_chest_ready").replace("@village", village.getValue().getName()));
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
        Player[] players = getServer().getOnlinePlayers();
        for (Player player : players) {
            if (!player.hasMetadata("NPC")) { // Citizens compatibility check
                if (location.getWorld().getName().equalsIgnoreCase(player.getLocation().getWorld().getName())) { // Add check to see if it's a real live player
                    if (location.distanceSquared(player.getLocation()) < distance * distance) {
                        return true;
                    }
                }
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

    private Location getItemFrameAttachedLocation(ItemFrame i) {
        Location l = i.getLocation();
        switch (i.getAttachedFace()) {
            case NORTH:
                l = l.subtract(0, 0, 1);
                break;
            case EAST:
                l = l.add(1, 0, 0);
                break;
            case SOUTH:
                l = l.add(0, 0, 1);
                break;
            case WEST:
                l = l.subtract(1, 0, 0);
                break;
            default:
                break;
        }
        return l;
    }

    protected boolean isWorldEnabled(String name) {
        boolean enabled;
        switch (getServer().getWorld(name).getEnvironment()) {
            case NORMAL: 
                if (_worldFilterEnabled) {
                    enabled = _enabledWorlds.contains(name);
                } else {
                    enabled = true;
                }
                break;
            default:
                enabled = false;
        }
        return enabled;
        
    }
    
    private void updateBaseConfig() {
        String configVersion = baseConfiguration.getString("configVersion");
        if (configVersion == null || "".equals(configVersion)) {
            baseConfiguration.set("configVersion", "1.3.2", false);
            baseConfiguration.set("useWorlds", false, false);
            baseConfiguration.set("worlds", "", false);
            baseConfiguration.set("dynmapDefaultVisible", false, false);             
        }
        baseConfiguration.saveFile();
    }    
    
    private void updateLanguageConfig() {
        String configVersion = languageConfiguration.getString("configVersion");
        if (configVersion == null || "".equals(configVersion)) {
            languageConfiguration.set("configVersion", "1.3.2", false);
            languageConfiguration.set("strings.chest_created", "You have created a tribute chest in @village. Tributes will be placed here daily.", false);
            languageConfiguration.set("strings.chest_already_exists", "&C@village already has a tribute chest!", false);
            languageConfiguration.set("strings.chest_create_not_top_rep", "&CYou must be the most reputable player with @village to create the Tribute Chest. Currently that is @toprep.", false);
            languageConfiguration.set("strings.tribute_chest_ready", "The villagers of @village have left a tribute for you in the tribute chest.", false);
            languageConfiguration.set("strings.server_village", "The village @village is now owned by @owner.", false);
        }
        languageConfiguration.saveFile();
    }
}
