package net.uvnode.uvvillagers;

import java.util.Map;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.dynmap.DynmapAPI;
import org.dynmap.markers.AreaMarker;
import org.dynmap.markers.MarkerAPI;
import org.dynmap.markers.MarkerSet;

/**
 *
 * @author James Cornwell-Shiel
 */
public class DynmapManager implements Listener {

    Plugin dynmap;
    DynmapAPI api;
    UVVillagers _plugin;
    MarkerAPI markerapi;
    MarkerSet set;
    double borderOpacity = 0.5;
    int borderWeight = 1;
    double fillOpacity = 0.25;
    int normalColor = 0x009900;
    int siegeColor = 0xFF0000;
    boolean _enabled = false;
    /**
     *
     * @param plugin
     */
    public DynmapManager(UVVillagers plugin) {
        _plugin = plugin;
    }
    
    /**
     *
     * @return
     */
    protected boolean enable() {
        try {
            PluginManager pm = _plugin.getServer().getPluginManager();
            dynmap = pm.getPlugin("dynmap");
            if (dynmap == null) {
                _plugin.getLogger().severe("Cannot find dynmap!");
                _enabled = false;
                return false;
            }

            api = (DynmapAPI) dynmap;

            if (dynmap.isEnabled()) {
                _plugin.getLogger().info("Starting up UVVillagers DynmapManager!");
                activate();
                _enabled = true;
                return true;
            } else {
                _plugin.getLogger().info("Dynmap is not enabled! UVVillagers DynmapManager is disabled.");
                _enabled = false;
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            _enabled = false;
            return false;
        }
    }

    /**
     *
     */
    protected void disable() {
        if (set != null) {
            set.deleteMarkerSet();
            set = null;
        }
    }

    /**
     * Rev 'er up! Called on startup if dynmap and uvvillagers are enabled.
     */
    private void activate() {
        try {
            // Load marker API
            markerapi = api.getMarkerAPI();
            if (markerapi == null) {
                _plugin.getLogger().severe("Cannot load marker API!");
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Get marker set
        set = markerapi.getMarkerSet("uvv.villages");

        // Create marker set if it doesn't exist, set label if it does
        if (set == null) {
            set = markerapi.createMarkerSet("uvv.villages", "Villages", null, false);
        } else {
            set.setMarkerSetLabel("Villages");
        }

        // If creating failed, drop out. 
        if (set == null) {
            _plugin.getLogger().severe("Error creating marker set.");
            return;
        }

        // Step through villages and create initial markers.
        for (Map.Entry<String, UVVillage> vdata : _plugin.getVillageManager().getAllVillages().entrySet()) {
            createMarker(vdata.getValue(), vdata.getKey());
        }

        // Start listening for events.
        _plugin.getServer().getPluginManager().registerEvents(this, _plugin);
    }

    /**
     * Creates a new dynmap AreaMarker for a village.
     *
     * @param v UVVillageData to create marker for
     * @param key key to uniquely identify this marker
     */
    private void createMarker(UVVillage v, String key) {
        /*
        // Get X range of marker
        double[] x = {
            v.getLocation().getBlockX() - (v.getSize() / 2),
            v.getLocation().getBlockX() + (v.getSize() / 2)
        };
        // Get Z range of marker
        double[] z = {
            v.getLocation().getBlockZ() - (v.getSize() / 2),
            v.getLocation().getBlockZ() + (v.getSize() / 2)
        };
        
        */
        double[] x = {v.getMinX(), v.getMaxX()};
        double[] z = {v.getMinZ(), v.getMaxZ()};
        // Create marker
        String label = makeMarkerLabel(v);
        AreaMarker marker = set.createAreaMarker(key, label, true, v.getLocation().getWorld().getName(), x, z, false);
        marker.setLabel(label, true);
        marker.setDescription(label);
        marker.setLineStyle(borderWeight, borderOpacity, normalColor);
        marker.setFillStyle(fillOpacity, normalColor);
        marker.setRangeY(v.getMinY(), v.getMaxY());
    }

    /**
     * Updates the label for an existing dynmap AreaMarker for a village.
     *
     * @param v UVVillageData to update marker for
     * @param key key to uniquely identify this marker
     */
    private void updateMarkerLabel(UVVillage v, String key) {
        // Retrieve the marker
        AreaMarker marker = set.findAreaMarker(v.getLocation().getWorld().getName() + key);
        // If the marker is found, update its label
        if (marker != null) {
            String label = makeMarkerLabel(v);
            marker.setLabel(label, true);
            marker.setDescription(label);
        }
    }

    private String makeMarkerLabel(UVVillage v) {
        return "<b>" + v.getName() + "</b>"
                + "<br>Top Player: " + v.getTopReputation()
                + "<br>Doors: " + v.getDoorCount()
                + "<br>Population: " + v.getPopulation() + " of " + ((int) (v.getDoorCount() * 0.35));
    }

    /**
     * Updates the geometry for an existing dynmap AreaMarker for a village.
     *
     * @param v UVVillageData to update marker for
     * @param key key to uniquely identify this marker
     */
    private void updateMarkerGeometry(UVVillage v, String key) {
        // Retrieve the marker
        AreaMarker marker = set.findAreaMarker(v.getLocation().getWorld().getName() + key);
        // If the marker is found, update its label
        if (marker != null) {
/*            // Get X range of marker
            double[] x = {
                v.getLocation().getBlockX() - (v.getSize() / 2),
                v.getLocation().getBlockX() + (v.getSize() / 2)
            };
            // Get Z range of marker
            double[] z = {
                v.getLocation().getBlockZ() - (v.getSize() / 2),
                v.getLocation().getBlockZ() + (v.getSize() / 2)
            };
            */
            double[] x = {v.getMinX(), v.getMaxX()};
            double[] z = {v.getMinZ(), v.getMaxZ()};
            marker.setCornerLocations(x, z);
            marker.setRangeY(v.getMinY(), v.getMaxY());
        }
    }

    /**
     * Updates the label for an existing dynmap AreaMarker for a village.
     *
     * @param v UVVillageData to update marker for
     * @param key key to uniquely identify this marker
     */
    private void updateMarkerStartSiege(UVVillage v, String key) {
        // Retrieve the marker
        AreaMarker marker = set.findAreaMarker(v.getLocation().getWorld().getName() + key);
        // If the marker is found, update its color
        if (marker != null) {
            _plugin.debug("Dynmap: Showing active siege in " + v.getLocation().getWorld().getName() + key);
            marker.setLineStyle(borderWeight, borderOpacity, siegeColor);
            marker.setFillStyle(fillOpacity, siegeColor);
        } else {
            _plugin.debug("Dynmap: marker not found with key=" + v.getLocation().getWorld().getName() + key);
        }
    }

    /**
     * Updates the label for an existing dynmap AreaMarker for a village.
     *
     * @param v UVVillageData to update marker for
     * @param key key to uniquely identify this marker
     */
    private void updateMarkerEndSiege(UVVillage v, String key) {
        // Retrieve the marker
        AreaMarker marker = set.findAreaMarker(v.getLocation().getWorld().getName() + key);
        // If the marker is found, update its label
        if (marker != null) {
            _plugin.debug("Dynmap: Removing active siege in " + v.getLocation().getWorld().getName() + key);
            marker.setLineStyle(borderWeight, borderOpacity, normalColor);
            marker.setFillStyle(fillOpacity, normalColor);
        }
    }

    /**
     * Deletes an existing dynmap AreaMarker for a village.
     *
     * @param key key to uniquely identify this marker
     */
    private void deleteMarker(String key, String world) {
        // Retrieve the marker
        AreaMarker marker = set.findAreaMarker(world + key);
        // If the marker is found, delete it
        if (marker != null) {
            marker.deleteMarker();
        }
    }

    /**
     * Listens for UVVillageEvents thrown by UVVillagers
     *
     * @param event The UVVillageEvent object.
     */
    @EventHandler
    public void onUVVillageEvent(UVVillageEvent event) {
        _plugin.debug(String.format("Dynmap: %s fired UVVillageEvent %s", event.getKey(), event.getType()));
        switch (event.getType()) {
            case DISCOVERED:
                if (event.getVillage() != null) {
                    createMarker(event.getVillage(), event.getKey());
                } else {
                    _plugin.getLogger().info("Tried to create a village marker for a null village!");
                }
                break;
            case ABANDONED:
                deleteMarker(event.getKey(), event.getWorld());
                break;
            case SIEGE_BEGAN:
                updateMarkerStartSiege(event.getVillage(), event.getKey());
                break;
            case SIEGE_ENDED:
                updateMarkerEndSiege(event.getVillage(), event.getKey());
                break;
            case VISITED:
                updateMarkerGeometry(event.getVillage(), event.getKey());
                updateMarkerLabel(event.getVillage(), event.getKey());
                break;
            case UPDATED:
                updateMarkerGeometry(event.getVillage(), event.getKey());
                updateMarkerLabel(event.getVillage(), event.getKey());
                break;
            case RENAMED:
                deleteMarker(event.getOldName(), event.getVillage().getLocation().getWorld().getName());
                createMarker(event.getVillage(), event.getKey());
                break;
            default:
            // Do nothing for unknown types.
        }
    }
    
    /**
     *
     * @return
     */
    public boolean isEnabled() {
        return _enabled;
    }
}
