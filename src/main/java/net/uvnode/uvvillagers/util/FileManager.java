package net.uvnode.uvvillagers.util;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.bukkit.plugin.Plugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Configuration File Manager
 * 
 * Based on the FileManager utility class created by Dragonphase: https://github.com/Dragonphase
 * 
 * @author jcornwellshiel
 * @author Dragonphase (original)
 */
public class FileManager {

    public static final Logger logger = Logger.getLogger("Minecraft");
    private static Plugin plugin;
    private YamlConfiguration fileManager;
    private String fileName;

    public FileManager(Plugin instance, String filename) {
        plugin = instance;
        fileName = filename;
        saveFile();
        loadFile();
    }

    public boolean saveFile() {
        try {
            File file = new File(plugin.getDataFolder(), fileName);
            fileManager.save(file);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public void loadFile() {
        File file = new File(plugin.getDataFolder(), fileName);

        if (!file.exists()) {
            plugin.saveResource(fileName, false);
            logger.info(String.format("[%s] %s not found. Creating from defaults.", plugin.getName(), fileName));
        }

        fileManager = YamlConfiguration.loadConfiguration(file);

        InputStream defConfigStream = plugin.getResource(fileName);
        if (defConfigStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
            fileManager.setDefaults(defConfig);
        }
    }

    public Set<String> getKeys(Boolean deep) {
        try {
            return fileManager.getKeys(deep);
        } catch (Exception ex) {
            return null;
        }
    }

    public Set<String> getKeys(boolean deep, boolean load) {
        loadFile();
        return getKeys(deep);
    }

    public boolean getBoolean(String path) {
        try {
            return fileManager.getBoolean(path);
        } catch (Exception ex) {
            return false;
        }
    }

    public boolean getBoolean(String path, boolean load) {
        loadFile();
        return getBoolean(path);
    }

    public int getInt(String path) {
        try {
            return fileManager.getInt(path);
        } catch (Exception ex) {
            return 0;
        }
    }

    public int getInt(String path, boolean load) {
        loadFile();
        return getInt(path);
    }
    
    public double getDouble(String path) {
        try {
            return fileManager.getDouble(path);
        } catch (Exception ex) {
            return 0;
        }
    }

    public double getDouble(String path, boolean load) {
        loadFile();
        return getDouble(path);
    }

    public String getString(String path) {
        try {
            return fileManager.getString(path);
        } catch (Exception ex) {
            return null;
        }
    }

    public String getString(String path, boolean load) {
        loadFile();
        return getString(path);
    }

    public ArrayList<String> getStringList(String path) {
        try {
            return (ArrayList<String>) fileManager.getStringList(path);
        } catch (Exception ex) {
            return new ArrayList<String>();
        }
    }

    public List<String> getStringList(String path, boolean load) {
        loadFile();
        return getStringList(path);
    }

    public boolean createSection(String path, Map<String, Object> map) {
        try {
            fileManager.createSection(path, map);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public ConfigurationSection getConfigSection(String path) {
        try {
            return fileManager.getConfigurationSection(path);
        } catch (Exception ex) {
            return null;
        }
    }

    public ConfigurationSection getConfigSection(String path, boolean load) {
        loadFile();
        return getConfigSection(path);
    }

    public boolean isConfigurationSection(String path) {
        try {
            return fileManager.isConfigurationSection(path);
        } catch (Exception ex) {
            return false;
        }
    }

    public boolean isConfigurationSection(String path, boolean load) {
        loadFile();
        return isConfigurationSection(path);
    }

    public void set(String path, Object object, boolean load) {
        if (load) {
            loadFile();
        }
        fileManager.set(path, object);
        saveFile();
    }
}