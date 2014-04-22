package net.uvnode.uvvillagers;

import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.server.v1_7_R3.Navigation;
import net.minecraft.server.v1_7_R3.Village;
import net.minecraft.server.v1_7_R3.VillageDoor;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_7_R3.entity.CraftVillager;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Villager;

/**
 * Custom village stuff
 *
 * @author James Cornwell-Shiel
 */
public final class UVVillage {

    private static UVVillagers _plugin;
    private int _numDoors;
    private Date _created;
    private Location _location;
    private Map<String, Integer> _playerReputations = new HashMap<>();
    private Map<String, Integer> _playerEmeraldTributesPending = new HashMap<>();
    private Map<String, Integer> _playerTicksHere = new HashMap<>();
    private Map<String, Integer> _playerTicksGone = new HashMap<>();
    private int _population;
    private int _size;
    private Village _villageCore;
    private String _name;
    private int _abandonStrikes;
    private CraftVillager _mayor = null;
    private ItemFrame _mayorSign;
    private Location _mayorSignLocation = null;
    private int _minX, _maxX, _minY, _maxY, _minZ, _maxZ;
    private Location _tributeChest;
    private boolean _isServerVillage;

    /**
     * Default Constructor
     *
     * @param location The location of the village
     * @param village The core village associated with this village
     * @param plugin  
     */
    public UVVillage(Location location, Village village, UVVillagers plugin) {
        _minX = Integer.MAX_VALUE;
        _maxX = Integer.MIN_VALUE;
        _minY = Integer.MAX_VALUE;
        _maxY = Integer.MIN_VALUE;
        _minZ = Integer.MAX_VALUE;
        _maxZ = Integer.MIN_VALUE;
        _name = "";
        _numDoors = 0;
        _plugin = plugin;
        _abandonStrikes = 0;
        _location = location;
        _villageCore = village;
        _isServerVillage = false;
        if (_villageCore != null) {
            updateVillageDataFromCore();
        }
    }

    /**
     * In depth constructor (for loads)
     *
     * @param location
     * @param doors
     * @param population
     * @param size
     * @param playerReputations
     * @param plugin  
     */
    public UVVillage(Location location, int doors, int population, int size, Map<String, Integer> playerReputations, boolean isServerVillage, UVVillagers plugin) {
        _minX = Integer.MAX_VALUE;
        _maxX = Integer.MIN_VALUE;
        _minY = Integer.MAX_VALUE;
        _maxY = Integer.MIN_VALUE;
        _minZ = Integer.MAX_VALUE;
        _maxZ = Integer.MIN_VALUE;
        _plugin = plugin;
        _location = location;
        _numDoors = doors;
        _population = population;
        _size = size;
        _playerReputations = playerReputations;
        _villageCore = null;
        _isServerVillage = isServerVillage;
        _name = "";
        _abandonStrikes = 0;
    }

    /**
     *
     * @param player
     * @return
     */
    public Integer collectEmeraldTribute(String player) {
        if (_playerEmeraldTributesPending.containsKey(player)) {
            Integer tribute = _playerEmeraldTributesPending.get(player);
            _playerEmeraldTributesPending.remove(player);
            return tribute;
        } else {
            return 0;
        }
    }

    /**
     *
     */
    public void clearEmeraldTributes() {
        _playerEmeraldTributesPending.clear();
    }

    /**
     *
     * @param player
     * @param amount
     */
    public void setEmeraldTribute(String player, Integer amount) {
        _playerEmeraldTributesPending.put(player, amount);
    }

    /**
     *
     * @return
     */
    public Date getCreated() {
        return _created;
    }

    /**
     *
     * @return
     */
    public String getCreatedString() {
        return DateFormat.getDateTimeInstance().format(_created);
    }

    /**
     *
     * @param created
     */
    public void setCreated(Date created) {
        _created = created;
    }

    /**
     *
     * @param created
     */
    public void setCreated(String created) {
        try {
            _created = DateFormat.getDateTimeInstance().parse(created);
        } catch (Exception e) {
            _created = new Date();
        }
    }

    /**
     *
     */
    public void setCreated() {
        _created = new Date();
    }

    /**
     * Get the number of doors in the village.
     *
     * @return number of doors
     */
    public int getDoorCount() {
        return _numDoors;
    }

    /**
     * Get the name of the village
     *
     * @return name
     */
    public String getName() {
        return _name;
    }

    /**
     * Get the location of the village
     *
     * @return location
     */
    public Location getLocation() {
        return _location;
    }

    /**
     * Get the village population
     *
     * @return population count
     */
    public int getPopulation() {
        return _population;
    }

    /**
     * Get the physical size of the village
     *
     * @return the block diameter of the village.
     */
    public int getSize() {
        return _size;
    }

    /**
     * Get the core MC village object associated with this village
     *
     * @return core village
     */
    public Village getVillageCore() {
        return _villageCore;
    }

    /**
     * Set the village name
     *
     * @param name
     */
    public void setName(String name) {
        _name = name;
        if (getMayor() != null) {
            setMayor(getMayor());
        }
    }

    /**
     * Set the number of doors
     *
     * @param doors
     */
    public void setDoors(int doors) {
        _numDoors = doors;
    }

    /**
     * Set the village location
     *
     * @param location
     */
    public void setLocation(Location location) {
        _location = location;
    }

    /**
     * Set the village population
     *
     * @param population
     */
    public void setPopulation(int population) {
        _population = population;
    }

    /**
     * Set the village size
     *
     * @param size
     */
    public void setSize(int size) {
        _size = size;
    }

    /**
     * Attach a core village
     *
     * @param villageCore core MC village
     */
    public void setVillageCore(Village villageCore) {
        _villageCore = villageCore;
    }

    /**
     * Updates the village data from the associated core village, and return a
     * value indicating what has changed.
     *
     * @return 0 if nothing changed, 1 if geometry changed, 2 if data changed, 3
     * if both changed.
     */
    public int updateVillageDataFromCore() {
        boolean geometryChanged = false, dataChanged = false;
        if (_location.getBlockX() != _villageCore.getCenter().x || _location.getBlockY() != _villageCore.getCenter().y || _location.getBlockZ() != _villageCore.getCenter().z) {
            _location.setX(_villageCore.getCenter().x);
            _location.setY(_villageCore.getCenter().y);
            _location.setZ(_villageCore.getCenter().z);
            geometryChanged = true;
        }

        if (_villageCore.getSize() != _size) {
            _size = _villageCore.getSize();
            geometryChanged = true;
        }

        List<VillageDoor> doors = _villageCore.getDoors();
        for (VillageDoor door : doors) {
            if (door.locX < _minX) {
                _minX = door.locX;
                geometryChanged = true;
            }
            if (door.locX > _maxX) {
                _maxX = door.locX;
                geometryChanged = true;
            }
            if (door.locY < _minY) {
                _minY = door.locY;
                geometryChanged = true;
            }
            if (door.locY > _maxY) {
                _maxY = door.locY;
                geometryChanged = true;
            }
            if (door.locZ < _minZ) {
                _minZ = door.locZ;
                geometryChanged = true;
            }
            if (door.locZ > _maxZ) {
                _maxZ = door.locZ;
                geometryChanged = true;
            }
        }

        if (_villageCore.getDoorCount() != _numDoors) {
            _numDoors = _villageCore.getDoorCount();
            dataChanged = true;
        }

        if (_villageCore.getPopulationCount() != _population) {
            _population = _villageCore.getPopulationCount();
            dataChanged = true;
        }

        setCorePlayerPopularities();

        return 0 + (geometryChanged ? 1 : 0) + (dataChanged ? 2 : 0);
    }

    /**
     * Increment a player's reputation with this village.
     *
     * @param name Player name
     * @param amount Increment amount
     * @return new reputation
     */
    public int modifyPlayerReputation(String name, Integer amount) {
        int currentRep = 0;
        if (_playerReputations.containsKey(name)) {
            currentRep = _playerReputations.get(name);
        }
        currentRep += amount;
        _playerReputations.put(name, currentRep);
        setCorePopularity(name, currentRep);
        return currentRep;
    }

    /**
     * Get a player's reputation with this village.
     *
     * @param name Player name
     * @return reputation
     */
    public int getPlayerReputation(String name) {
        if (_playerReputations.containsKey(name)) {
            return _playerReputations.get(name);
        } else {
            return 0;
        }
    }

    /**
     *
     * @param name
     * @return
     */
    public boolean isPlayerKnown(String name) {
        return _playerReputations.containsKey(name);
    }

    /**
     * Get all player reputations
     *
     * @return Map of player names and reputations
     */
    public Map<String, Integer> getPlayerReputations() {
        return _playerReputations;
    }

    /**
     * Get the name of the player with the top reputation
     *
     * @return player name
     */
    public String getTopReputation() {
        if (_isServerVillage) return "Server";
        String topPlayer = "Nobody";
        int topRep = Integer.MIN_VALUE;
        for (Map.Entry<String, Integer> entry : _playerReputations.entrySet()) {
            if (entry.getValue() > topRep) {
                topPlayer = entry.getKey();
                topRep = entry.getValue();
            }
        }
        return topPlayer;
    }

    /**
     * Marks the player as here in this village.
     *
     * @param playerName
     */
    public void setPlayerHere(String playerName) {
        // If not listed as here already, mark here.
        if (!_playerTicksHere.containsKey(playerName)) {
            _playerTicksHere.put(playerName, 0);
        }
        // Player is no longer gone.
        if (_playerTicksGone.containsKey(playerName)) {
            _playerTicksGone.remove(playerName);
        }
    }

    /**
     * Marks the player as not in this village.
     *
     * @param playerName
     */
    public void setPlayerGone(String playerName) {
        // If not listed as gone already, mark gone.
        if (!_playerTicksGone.containsKey(playerName)) {
            _playerTicksGone.put(playerName, 0);
        }
        // Player is no longer here.
        if (_playerTicksHere.containsKey(playerName)) {
            _playerTicksHere.remove(playerName);
        }
    }

    /**
     *
     * @param playerName
     */
    public void tickPlayerPresence(String playerName) {
        if (_playerTicksHere.containsKey(playerName)) {
            // If the player is here, increment here counter.
            _playerTicksHere.put(playerName, _playerTicksHere.get(playerName) + 1);
        } else if (_playerTicksGone.containsKey(playerName)) {
            // If the player is gone, increment gone counter.
            _playerTicksGone.put(playerName, _playerTicksGone.get(playerName) + 1);
        }
    }

    /**
     *
     * @param playerName
     * @return
     */
    public boolean isPlayerHere(String playerName) {
        return _playerTicksHere.containsKey(playerName);
    }

    /**
     *
     * @param playerName
     * @return
     */
    public int getPlayerTicksHere(String playerName) {
        if (_playerTicksHere.containsKey(playerName)) {
            return _playerTicksHere.get(playerName);
        } else {
            return 0;
        }
    }

    /**
     *
     * @param playerName
     * @return
     */
    public int getPlayerTicksGone(String playerName) {
        if (_playerTicksGone.containsKey(playerName)) {
            return _playerTicksGone.get(playerName);
        } else {
            return 0;
        }
    }

    /**
     *
     * @param playerName
     * @param value
     */
    public void setCorePopularity(String playerName, int value) {
        if (_villageCore != null) {
            _villageCore.a(playerName, _plugin.getRank(value).isHostile() ? -30 : 10);
        }
    }

    private void setCorePlayerPopularities() {
        for (Map.Entry<String, Integer> repEntry : _playerReputations.entrySet()) {
            setCorePopularity(repEntry.getKey(), repEntry.getValue());
        }
    }

    /**
     *
     * @return
     */
    public Integer addAbandonStrike() {
        _abandonStrikes++;
        return _abandonStrikes;
    }

    /**
     *
     */
    public void clearAbandonStrikes() {
        _abandonStrikes = 0;
    }

    /**
     *
     * @return
     */
    public Integer getAbandonStrikes() {
        return _abandonStrikes;
    }

    /**
     *
     * @param mayorSign
     */
    public void setMayorSign(ItemFrame mayorSign) {
        _mayorSign = mayorSign;
        _mayorSignLocation = mayorSign.getLocation();
    }

    /**
     *
     * @param mayorSignLocation
     */
    public void setMayorSign(Location mayorSignLocation) {
        _mayorSignLocation = mayorSignLocation;
        if (_mayorSignLocation.getWorld().isChunkLoaded(_mayorSignLocation.getChunk())) {
            for (Entity entity : _mayorSignLocation.getWorld().getEntitiesByClass(ItemFrame.class)) {
                if (entity.getLocation().equals(mayorSignLocation) && ((ItemFrame) entity).getItem().getType() == Material.EMERALD) {
                    _mayorSign = (ItemFrame) entity;
                }
            }
        }
    }

    /**
     *
     * @return
     */
    public ItemFrame getMayorSign() {
        if (_mayorSign == null) {
            if (_mayorSignLocation != null) {
                if (_mayorSignLocation.getWorld().isChunkLoaded(_mayorSignLocation.getChunk())) {
                    for (Entity entity : _mayorSignLocation.getWorld().getEntities()) {
                        if (entity.getType() == EntityType.ITEM_FRAME && entity.getLocation().distanceSquared(_mayorSignLocation) < 4 && ((ItemFrame) entity).getItem().getType() == Material.EMERALD) {
                            _mayorSign = (ItemFrame) entity;
                        }
                    }
                }
            }
        }
        return _mayorSign;
    }

    /**
     *
     * @return
     */
    public Location getMayorSignLocation() {
        if (_mayorSignLocation != null) {
            return _mayorSignLocation.getBlock().getLocation();
        } else {
            return null;
        }
    }

    /**
     *
     * @param mayor
     */
    public void setMayor(Villager mayor) {
        if (_mayor != null && !_mayor.isDead()) {
            _mayor.setCustomName("");
            _mayor.setCustomNameVisible(false);
        }
        _mayor = (CraftVillager) mayor;
        String name = String.format("Mayor of %s", _name);
        if (name.length() > 32) {
            name = name.substring(0, 29) + "...";
        }
        _mayor.setCustomName(name);
        _mayor.setCustomNameVisible(true);
    }

    /**
     *
     * @return
     */
    public Villager getMayor() {
        if (_mayor != null && !_mayor.isDead()) {
            return _mayor;
        } else {
            return null;
        }
    }

    /**
     *
     */
    public void moveMayor() {
        if (_mayor != null && _mayorSign != null && _mayor.getLocation().distanceSquared(_mayorSign.getLocation()) > 4) {
            Navigation nav = _mayor.getHandle().getNavigation();
            nav.a(_mayorSign.getLocation().getBlockX(), _mayorSign.getLocation().getBlockY(), _mayorSign.getLocation().getBlockZ(), 0.3f);
        }
        //_mayor.getHandle().getNavigation().a((net.minecraft.server.v1_7_R1.Entity)_mayorSign);
        if (_mayor.getLocation().distanceSquared(_mayorSign.getLocation()) > 100) {
            // Find an empty spot by the sign
            Location safe = null;
            int radius=4;
            for (int y = 0; y < radius; y++) {
                for (int x = 0; x < radius; x++) {
                    for (int z = 0; z < radius; z++) {
                        Location test = _mayorSign.getLocation().clone();
                            if (test.clone().add(x, y, z).getBlock().isEmpty() && test.clone().add(x, y+1, z).getBlock().isEmpty()) {
                            safe = test.clone().add(x, y, z);
                            break;
                        }
                        if (test.clone().add(x, y, z*-1).getBlock().isEmpty() && test.clone().add(x, y+1, z*-1).getBlock().isEmpty()) {
                            safe = test.clone().add(x, y, z*-1);
                            break;
                        }                    
                        if (test.clone().add(x*-1, y, z).getBlock().isEmpty() && test.clone().add(x*-1, y+1, z).getBlock().isEmpty()) {
                            safe = test.clone().add(x*-1, y, z);
                            break;
                        }                    
                        if (test.clone().add(x*-1, y, z*-1).getBlock().isEmpty() && test.clone().add(x*-1, y+1, z*-1).getBlock().isEmpty()) {
                            safe = test.clone().add(x*-1, y, z*-1);
                            break;
                        }                    
                        if (safe != null) {
                            break;
                        }
                    }
                }
                if (safe != null) {
                    break;
                }
            }
            // If we found a spot, teleport him.
            if (safe != null) {
                _mayor.teleport(safe);
            }
        }
    }

    /**
     *
     * @return
     */
    public int getMinX() {
        if (_minX != Integer.MAX_VALUE) {
            return _minX;
        } else {
            return getLocation().getBlockX() - (getSize() / 2);
        }
    }

    /**
     *
     * @return
     */
    public int getMaxX() {
        if (_maxX != Integer.MIN_VALUE) {
            return _maxX;
        } else {
            return getLocation().getBlockX() + (getSize() / 2);
        }
    }

    /**
     *
     * @return
     */
    public int getMinY() {
        if (_minY != Integer.MAX_VALUE) {
            return _minY;
        } else {
            return getLocation().getBlockY() - (getSize() / 2);
        }
    }

    /**
     *
     * @return
     */
    public int getMaxY() {
        if (_maxY != Integer.MIN_VALUE) {
            return _maxY;
        } else {
            return getLocation().getBlockY() + (getSize() / 2);
        }
    }

    /**
     *
     * @return
     */
    public int getMinZ() {
        if (_minZ != Integer.MAX_VALUE) {
            return _minZ;
        } else {
            return getLocation().getBlockZ() - (getSize() / 2);
        }
    }

    /**
     *
     * @return
     */
    public int getMaxZ() {
        if (_maxZ != Integer.MIN_VALUE) {
            return _maxZ;
        } else {
            return getLocation().getBlockZ() + (getSize() / 2);
        }
    }
    
    public void setBounds(int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
        _minX = minX;
        _maxX = maxX;
        _minY = minY;
        _maxY = maxY;
        _minZ = minZ;
        _maxZ = maxZ;
    }

    protected void setTributeChest(Location l) {
        if (_tributeChest != null)
            _tributeChest.getBlock().breakNaturally();
        _tributeChest = l;
    }

    public boolean hasChest() {
        try {
            if (_tributeChest.getBlock().getType().equals(Material.CHEST)) {
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }

    public Location getChest() {
        return _tributeChest;
    }
    
    protected void setServerVillage(boolean b) {
        _isServerVillage = b;
    }
    
    public boolean isServerVillage() {
        return _isServerVillage;
    }
}
