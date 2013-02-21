/**
 * 
 */
package net.uvnode.uvvillagers;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

/**
 * @author James Cornwell-Shiel
 *
 * Adds village tributes and additional zombie siege functionality.
 * 
 */
public final class UVVillagers extends JavaPlugin implements Listener {
	
	// Initialize null active siege
	UVSiege activeSiege = null;
	
	// Configuration Settings
	UVTributeMode tributeMode;

	int tributeRange, villagerCount, minPerVillagerCount, maxPerVillagerCount, baseSiegeBonus, minPerSiegeKill, maxPerSiegeKill; 

	
	@Override
	public void onEnable() {
		getServer().getPluginManager().registerEvents(this, this);
		
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
	}



	@Override
	public void onDisable() {
		
	}
	
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
		if(cmd.getName().equalsIgnoreCase("siege")){
			if (activeSiege == null) {
				sender.sendMessage("No sieges.");
			}
			else {
				activeSiege.sendOverview(sender);
			}
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
			case VILLAGE_DEFENSE:
				// Generally speaking I think this one is always Iron Golems.
				x = event.getEntity().getLocation().getBlockX();
				y = event.getEntity().getLocation().getBlockY();
				z = event.getEntity().getLocation().getBlockZ();
				getLogger().info("CreatureSpawnEvent " + event.getEntityType().getName() + " VILLAGE_DEFENSE @ " + x + "," + y + "," + z + "!");
				break;
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
				break;
			default:
				break;
		}
	}
	
	@EventHandler
	public void entityDeath(EntityDeathEvent event) {
		if (activeSiege != null) {
			if (activeSiege.checkEntityId(event.getEntity().getEntityId())) {
				activeSiege.addPlayerKill(event.getEntity().getKiller().getName());
			}
		}
	}
	
	@EventHandler
	public void emeraldsAtDawn(UVTimeEvent event) {
		switch(event.getType()) {
			case DAWN:
				Collection<Villager> villagers = event.getWorld().getEntitiesByClass(org.bukkit.entity.Villager.class);
				List<Player> players = event.getWorld().getPlayers();
				Map<String, Integer> playerVillagerCounts = new HashMap<String, Integer>();
				Iterator<Villager> villagerIterator = villagers.iterator();
				while (villagerIterator.hasNext()) {
					Villager v = villagerIterator.next();
					for (int i = 0; i < players.size(); i++) {
						if (players.get(i).getLocation().distanceSquared(v.getLocation()) < tributeRange * tributeRange) {
							if (playerVillagerCounts.get(players.get(i).getName()) != null) {
								playerVillagerCounts.put(players.get(i).getName(), playerVillagerCounts.get(players.get(i).getName()) + 1);
							} else {
								playerVillagerCounts.put(players.get(i).getName(), 1);
							}
						}
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
							tributeAmount += rng.nextInt(maxPerSiegeKill + 1) + (maxPerSiegeKill - minPerSiegeKill);
						}
					}
					if (numVillagers >= villagerCount) {
						for (int i = 0; i < (int)(numVillagers / villagerCount); i++) {
							if (activeSiege == null) {
								tributeAmount += rng.nextInt(maxPerVillagerCount + 1) + (maxPerVillagerCount - minPerVillagerCount);
							} else {
								tributeAmount += rng.nextInt(maxPerVillagerCount + 1) + (maxPerVillagerCount - minPerVillagerCount) + baseSiegeBonus;
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
