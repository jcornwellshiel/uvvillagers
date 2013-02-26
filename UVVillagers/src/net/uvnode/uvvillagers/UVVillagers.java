/**
 * 
 */
package net.uvnode.uvvillagers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import net.minecraft.server.v1_4_R1.Village;
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
import org.bukkit.event.player.PlayerMoveEvent;
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
	UVSiege _activeSiege = null;
	
	Map<String,Integer> playerVillagesProximity = new HashMap<String, Integer>(); 
	
	// Configuration Settings
	UVTributeMode tributeMode;

	int tributeRange, 
		villagerCount, 
		minPerVillagerCount, 
		maxPerVillagerCount, 
		baseSiegeBonus, 
		minPerSiegeKill,
		maxPerSiegeKill, 
		chanceOfExtraZombies;

	
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
					if (worlds.get(i).getTime() >= 12500 && worlds.get(i).getTime() < 12520) {
						UVTimeEvent event = new UVTimeEvent(worlds.get(i), UVTimeEventType.DUSK);
						getServer().getPluginManager().callEvent(event);
					}
				}
			}
		
		}, 0, 20);
	}
	

	@Override
	public void onDisable() {
		
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
		getLogger().info("Configuration loaded.");
	}

	
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
		if(cmd.getName().equalsIgnoreCase("uvv")){
			sender.sendMessage("- UVVillagers -");
			@SuppressWarnings("unchecked")
			List<Village> villages = _worldserver.villages.getVillages();
			sender.sendMessage(villages.size() + " villages are loaded.");
			if (sender instanceof Player) {
				Location pLoc = ((Player)sender).getLocation();
				
				Village closest = _worldserver.villages.getClosestVillage(pLoc.getBlockX(), pLoc.getBlockY(), pLoc.getBlockZ(), tributeRange);
				if (closest != null) {
					sender.sendMessage("Closest village at " + closest.getCenter().x + ", " + closest.getCenter().y + ", " + closest.getCenter().z);
					sender.sendMessage(" - Population: " + closest.getPopulationCount());
					sender.sendMessage(" - Size: " + closest.getSize());
					sender.sendMessage(" - Doors: " + closest.getDoorCount());
					getLogger().info(closest.toString());
					getLogger().info(closest.hashCode() + "");
					getLogger().info("a: " + closest.a(((Player)sender).getName())); // Player popularity
					getLogger().info("d: " + closest.d(((Player)sender).getName())); // Unknown
				} else
					sender.sendMessage("No villages nearby.");
			}
			return true;
		}
		return false;
	}


	@EventHandler
	public void playerMove(PlayerMoveEvent event) {
		if (event.getTo().getWorld() != getServer().getWorlds().get(0));
		Village v = _worldserver.villages.getClosestVillage(
				event.getTo().getBlockX(),
				event.getTo().getBlockY(),
				event.getTo().getBlockZ(),
				tributeRange);
		String name = event.getPlayer().getName();
		// If a village is nearby
		if (v != null) {
			// Do we have a record for this player?
			if (playerVillagesProximity.containsKey(name)) {
				// If so, check to see if we've already alerted the player that he's near this village
				int current = playerVillagesProximity.get(name);
				if (v.hashCode() != current) {
					// No? Alert him!
					event.getPlayer().sendMessage("You're near a village! Your popularity with the " + v.getPopulationCount() + " villagers here is " + v.a(name));
					// Set the player as near this village
					playerVillagesProximity.put(name, v.hashCode());
				}
			} else {
				// if no record, alert him!
				event.getPlayer().sendMessage("You're near a village! Your popularity with the " + v.getPopulationCount() + " villagers here is " + v.a(name));				
				// Set the player as near this village
				playerVillagesProximity.put(name, v.hashCode());
			}

		} else {
			// The player has left the village!
			playerVillagesProximity.put(name, -1);
		}
	}
	
	
	@EventHandler
	public void creatureSpawn(CreatureSpawnEvent event) {
		switch(event.getSpawnReason()) {
			case VILLAGE_INVASION: 
				// VILLAGE_INVASION is only triggered in a zombie siege.
				// TO DO - ADD METHOD THAT MAKES SURE THAT ZOMBIES SPAWN AT VILLAGE HEIGHT - thebloodline has his village floating 3 blocks over land, and all the zombies spawn underneath.
				// If we haven't already registered this siege, create a new siege tracking object
				if (_activeSiege == null) {
					getServer().broadcastMessage("A zombie siege has begun!!!");
					_activeSiege = new UVSiege(
							event.getEntity().getWorld().getTime(), 
							_worldserver.villages.getClosestVillage(
									event.getEntity().getLocation().getBlockX(),
									event.getEntity().getLocation().getBlockY(),
									event.getEntity().getLocation().getBlockZ(),
									32));
				}
				
				// Add this spawn to the siege's mob list for kill tracking
				_activeSiege.addSpawn(event.getEntity());
				
				// If configured to, randomly spawn extra zombies! 
				// Eventually this will be replaced by a method that handles spawning a variety of mob types based on village size
				if (chanceOfExtraZombies > 1) { 
					Random rng = new Random();
					if (chanceOfExtraZombies > rng.nextInt(100)) {
						// Spawn a zombie and add him to the siege mob list for kill tracking.
						_activeSiege.addSpawn((LivingEntity)event.getEntity().getWorld().spawnEntity(event.getEntity().getLocation(), EntityType.ZOMBIE));
					}
				}
				break;
			default:
				break;
		}
	}
	
	
	@EventHandler
	public void entityDeath(EntityDeathEvent event) {
		// If there's an active siege being tracked, check to see if the mob killed is part of the siege 
		if (_activeSiege != null) {
			if (_activeSiege.checkEntityId(event.getEntity().getEntityId())) {
				// If so and it was killed by a player, log the kill for rewards at dawn.
				if(event.getEntity().getKiller() != null)
					_activeSiege.addPlayerKill(event.getEntity().getKiller().getName());
			}
		}
	}
	
	
	@EventHandler
	public void emeraldsAtDawn(UVTimeEvent event) {
		switch(event.getType()) {
			case DAWN:
				// TO-DO: ADD MULTIWORLD SUPPORT!
				// Kick us out of this if we're not dealing with the main world.
				if (event.getWorld().getName() != _worldserver.getWorld().getName()) { return; }
					
				//getServer().broadcastMessage("Dawn has arrived in world " + event.getWorld().getName() + "!");

				//Map<String, Integer> playerVillagerCounts = new HashMap<String, Integer>();
				List<Player> players = event.getWorld().getPlayers();
				
				@SuppressWarnings("unchecked")
				List<Village> villages = _worldserver.villages.getVillages();
				Iterator<Player> playerIterator = players.iterator();
				
				// Step through the players to calculate tribute 
				while (playerIterator.hasNext()) {
					Player p = playerIterator.next();
					Iterator<Village> villageIterator = villages.iterator();
					
					int tributeAmount = 0, killBonus = 0, numVillagesNearby = 0;
					Random rng = new Random();

					// Calculate bonus from kills
					if (_activeSiege != null) {
						int kills = _activeSiege.getPlayerKills(p.getName());
						for (int i = 0; i < kills; i++) {
							killBonus += rng.nextInt(maxPerSiegeKill + 1 - minPerSiegeKill) + minPerSiegeKill;
						}
					}

					// Step through the villages. Add their tributes if they're close enough.
					while (villageIterator.hasNext()) {
						int villageTributeAmount = 0;
						Village v = villageIterator.next();
						Location loc = new Location(event.getWorld(), v.getCenter().x, v.getCenter().y, v.getCenter().z);
						// Check village distance from player
						if (p.getLocation().distanceSquared(loc) < (tributeRange+v.getSize()) * (tributeRange+v.getSize())) {
							numVillagesNearby++;
							// Check population size
							int population = v.getPopulationCount();
							if (population > 20) {
								// Give a random bonus per 20 villagers
								for (int i = 0; i < (int)(population / villagerCount); i++) {
									villageTributeAmount += rng.nextInt(maxPerVillagerCount + 1 - minPerVillagerCount) + minPerVillagerCount + baseSiegeBonus;
								}
								// If this village was the one sieged, give the kill bonus and an extra survival thankfulness bonus
								if (_activeSiege != null && _activeSiege.getVillage().hashCode() == v.hashCode()) {
									villageTributeAmount += 1 * (int)(population / villagerCount) + killBonus;
								}
							}
						}
						// Add the tribute from this village to the total owed
						tributeAmount += villageTributeAmount;
					}
					// TO-DO: Save the tribute amount for each player/village so that receive it next time the player talks to a villager in that village.
					// This will force players to interact with every village that they are to get credit for.
					// But for now... just award the tribute directly.
					if (numVillagesNearby > 0) {
						if (tributeAmount > 0) {
							ItemStack items = new ItemStack(Material.EMERALD, tributeAmount);
							p.getInventory().addItem(items);
							p.sendMessage("Grateful villagers gave you " + tributeAmount + " emeralds!");
							getLogger().info(p.getName() + " received " + tributeAmount + " emeralds.");						
						} else
							p.sendMessage("The villagers didn't have any emeralds for you today.");
					} else
						p.sendMessage("You weren't near any villages large enough to pay you tribute.");
				}
				if (_activeSiege != null) {
					ArrayList<String> messages = _activeSiege.overviewMessage();
					Iterator<String> messageIterator = messages.iterator();
					while (messageIterator.hasNext())
						getServer().broadcastMessage(messageIterator.next());
				}
				break;
			case DUSK:
				// TO-DO: ADD MULTIWORLD SUPPORT!
				// Kick us out of this if we're not dealing with the main world.
				if (event.getWorld().getName() != _worldserver.getWorld().getName()) { return; }

				//getServer().broadcastMessage("Dusk has arrived in world " + event.getWorld().getName() + "!");

				// TO-DO: clear pending tributes list
				// clear active siege
				_activeSiege = null;
				break;
			default:
				break;
		}
	}
	
}
