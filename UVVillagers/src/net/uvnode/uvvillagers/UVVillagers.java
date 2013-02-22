/**
 * 
 */
package net.uvnode.uvvillagers;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import net.minecraft.server.v1_4_R1.Village;
import net.minecraft.server.v1_4_R1.VillageSiege;
import net.minecraft.server.v1_4_R1.WorldServer;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import org.bukkit.craftbukkit.v1_4_R1.CraftWorld;
/**
 * @author James Cornwell-Shiel
 *
 * Adds village tributes and additional zombie siege functionality.
 * 
 */
public final class UVVillagers extends JavaPlugin implements Listener {
	
	WorldServer _worldserver;
	// Initialize null active siege
	UVSiege activeSiege = null;
	
	// Configuration Settings
	UVTributeMode tributeMode;

	int tributeRange, villagerCount, minPerVillagerCount, maxPerVillagerCount, baseSiegeBonus, minPerSiegeKill, maxPerSiegeKill, chanceOfExtraZombies; 

	
	@Override
	public void onEnable() {
		_worldserver = ((CraftWorld) getServer().getWorlds().get(0)).getHandle();

		getServer().getPluginManager().registerEvents(this, this);

		saveDefaultConfig();
		loadConfig();
		
		// Step through worlds every 20 ticks and throw UVTimeEvents for various times of day.
		getServer().getScheduler().runTaskTimerAsynchronously(this, new Runnable() {
			@Override
			public void run() {
				List<World> worlds = getServer().getWorlds();
				for (int i = 0; i < worlds.size(); i++) {
					if (worlds.get(i).getTime() >= 0 && worlds.get(i).getTime() < 20) {
						UVTimeEvent event = new UVTimeEvent(worlds.get(i), UVTimeEventType.DAWN);
						getServer().getPluginManager().callEvent(event);
					}
				}
			}
		
		}, 0, 20);
	}
	
	
	
	private void loadConfig() {
		tributeRange = getConfig().getInt("tributeRange", 64);
		villagerCount = getConfig().getInt("villagerCount", 20);
		minPerVillagerCount = getConfig().getInt("minPerVillagerCount", 0);
		maxPerVillagerCount = getConfig().getInt("maxPerVillagerCount", 3);
		baseSiegeBonus = getConfig().getInt("baseSiegeBonus", 1);
		minPerSiegeKill = getConfig().getInt("minPerSiegeKill", 1);
		maxPerSiegeKill = getConfig().getInt("maxPerSiegeKill", 2);
		chanceOfExtraZombies = getConfig().getInt("chanceOfExtraZombies", 20);
		if (chanceOfExtraZombies > 50) chanceOfExtraZombies = 50; // Cap this so that there are no infinite zombie spawns.
		getLogger().info("Configuration loaded.");
	}



	@Override
	public void onDisable() {
		
	}
	
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
		if(cmd.getName().equalsIgnoreCase("uvv")){
			sender.sendMessage("- UVVillagers -");
			@SuppressWarnings("unchecked")
			List<Village> villages = _worldserver.villages.getVillages();
			sender.sendMessage(villages.size() + " villages are loaded.");
			if (sender instanceof Player) {
				Location pLoc = ((Player)sender).getLocation();
				
				
				Village closest = _worldserver.villages.getClosestVillage(pLoc.getBlockX(), pLoc.getBlockY(), pLoc.getBlockZ(), 256);
				sender.sendMessage("Closest village at " + closest.getCenter().x + ", " + closest.getCenter().y + ", " + closest.getCenter().z);
				sender.sendMessage(" - Population: " + closest.getPopulationCount());
				sender.sendMessage(" - Size: " + closest.getSize());
				sender.sendMessage(" - Doors: " + closest.getDoorCount());
				getLogger().info(closest.d()+"");
				getLogger().info(closest.toString());
				getLogger().info("a: " + closest.a(((Player)sender).getName()));
				getLogger().info("d: " + closest.d(((Player)sender).getName()));
				//new VillageSiege(_worldserver);
			}
			
/*
			if (args.length > 0 && args[0] == "siege") {
				if (activeSiege == null) {
					sender.sendMessage("No sieges.");
				} else {
					activeSiege.sendOverview(sender);
				}
			} else if (args.length > 0 && args[0] == "reload") {
				sender.sendMessage("Reloading Config.");
				loadConfig();
			} else {
				sender.sendMessage("Try \"/uvv siege\" for current siege info, or \"/uvv reload\" to reload the config.");
			}*/
			return true;
		}
		return false;
	}

	/**
	 * @param event
	 * 
	 * Event handler for creatures spawning.
	 */
	@EventHandler
	public void creatureSpawn(CreatureSpawnEvent event) {
		int x, y, z;
		switch(event.getSpawnReason()) {
			case VILLAGE_INVASION:
				// Zombie siege?! Oh snap!
				x = event.getEntity().getLocation().getBlockX();
				y = event.getEntity().getLocation().getBlockY();
				z = event.getEntity().getLocation().getBlockZ();
				
				getLogger().info("CreatureSpawnEvent " + event.getEntityType().getName() + " VILLAGE_INVASION @ " + x + "," + y + "," + z + "!");
				if (activeSiege == null) {
					getServer().broadcastMessage("A zombie siege has begun!!!");
					activeSiege = new UVSiege(event.getEntity().getWorld().getTime());
				}
				activeSiege.addSpawn(event.getEntity());
				if (chanceOfExtraZombies > 1) { 
					Random rng = new Random();
					if (chanceOfExtraZombies > rng.nextInt(100)) {
						getLogger().info("An extra zombie spawns!");
						activeSiege.addSpawn((LivingEntity)event.getEntity().getWorld().spawnEntity(event.getEntity().getLocation(), EntityType.ZOMBIE));
					}
					
				}
				break;
			default:
				break;
		}
	}
	
	@EventHandler
	public void entityDeath(EntityDeathEvent event) {
		if (activeSiege != null) {
			if (activeSiege.checkEntityId(event.getEntity().getEntityId())) {
				if(event.getEntity().getKiller() != null)
					activeSiege.addPlayerKill(event.getEntity().getKiller().getName());
			}
		}
	}
	
	@EventHandler
	public void emeraldsAtDawn(UVTimeEvent event) {
		switch(event.getType()) {
			case DAWN:
				Map<String, Integer> playerVillagerCounts = new HashMap<String, Integer>();
				List<Player> players = event.getWorld().getPlayers();
				for (int i = 0; i < players.size(); i++) {
					Village closest = _worldserver.villages.getClosestVillage(
							players.get(i).getLocation().getBlockX(), 
							players.get(i).getLocation().getBlockY(), 
							players.get(i).getLocation().getBlockZ(), 
							tributeRange);
					if (closest != null) {
						playerVillagerCounts.put(players.get(i).getName(), closest.getPopulationCount());
					}
				}
				
				Iterator<String> prs = playerVillagerCounts.keySet().iterator();
				while (prs.hasNext()) {
					String pname = prs.next();
					int numVillagers = playerVillagerCounts.get(pname);
					int tributeAmount = 0;
					getLogger().info(pname + " is close to " + numVillagers + " villagers.");
					Random rng = new Random();
					if (activeSiege != null) {
						int kills = activeSiege.getPlayerKills(pname);
						for (int i = 0; i < kills; i++) {
							tributeAmount += rng.nextInt(maxPerSiegeKill + 1 - minPerSiegeKill) + minPerSiegeKill;
						}
					}
					if (numVillagers >= villagerCount) {
						for (int i = 0; i < (int)(numVillagers / villagerCount); i++) {
							if (activeSiege == null) {
								tributeAmount += rng.nextInt(maxPerVillagerCount + 1 - minPerVillagerCount) + minPerVillagerCount;
							} else {
								tributeAmount += rng.nextInt(maxPerVillagerCount + 1 - minPerVillagerCount) + minPerVillagerCount + baseSiegeBonus;
							}
						}
						if (tributeAmount > 0) {
							ItemStack items = new ItemStack(Material.EMERALD, tributeAmount);
							getServer().getPlayer(pname).getInventory().addItem(items);
							getServer().getPlayer(pname).sendMessage("Grateful villagers gave you " + tributeAmount + " emeralds!");
						}
						else
							getServer().getPlayer(pname).sendMessage("The villagers didn't have any emeralds for you today.");

						getLogger().info(pname + " received " + tributeAmount + " emeralds.");						
					}
				}
				activeSiege = null;
				break;
			default:
				break;
		}
	}
	
}
