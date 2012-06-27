package to.joe.j2mc.tournament;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;

import to.joe.j2mc.core.J2MC_Manager;
import to.joe.j2mc.tournament.command.JoinCommand;
import to.joe.j2mc.tournament.command.LeaveCommand;
import to.joe.j2mc.tournament.command.admin.DuelCommand;

public class J2MC_Tournament extends JavaPlugin implements Listener {

	public enum GameStatus {
		Fighting, //Two players are currently fighting. Listener should pay attention to the two players on the top of roundList
		Idle, //Nobody is fighting. Listener should ignore all deaths.
	}

	private Location startPositionA; //The start position of the first player
	private Location startPositionB; //The start position of the second player
	private Location respawnLoc;
	public ArrayList<Player> participants = new ArrayList<Player>(); //List of players who are still in the tournament
	public boolean registrationOpen = false; //Are new players allowed to enter the tournament?
	public List<Integer> itemList;
	public ArrayList<Player> roundList = new ArrayList<Player>(); //Array of players who will fight. Should always have an even number of players
	public GameStatus status = GameStatus.Idle;

	private boolean isPowerOfTwo(int number) {
		return (number != 0) && ((number & (number - 1)) == 0);
	}

	/*
	 * Sets up the round by filling roundList with an even number of Players who are made to fight
	 * If the number of players is a power of 2, then players fight in order
	 * If it is not a power of 2, the playerList is scrambled (because participants isn't seeded) and enough pairs are picked to bring the number down to a power of 2.
	 */
	public void setupRound() {
		if (roundList.isEmpty()) {
			if (participants.size() == 1) {
				J2MC_Manager.getCore().getServer().broadcastMessage(ChatColor.RED + participants.get(0).getName() + ChatColor.AQUA + " is the last player standing and wins this tournament!");
				participants.clear();
			} else if (isPowerOfTwo(participants.size())) {
				roundList.addAll(participants);
			} else { //Not power of 2, so must eliminate until power of 2
				int numberToEliminate = 1;
				while (!isPowerOfTwo(participants.size()-numberToEliminate))
					numberToEliminate++;
				for(int x = 0; x < numberToEliminate*2; x++) {
					//select random person
					Random playerPicker = new Random();
					while(true) {
						int playerNumber = playerPicker.nextInt(participants.size());
						if (!roundList.contains(participants.get(playerNumber))) {
							roundList.add(participants.get(playerNumber));
							break;
						}
					}
				}
			}
		}
	}

	public void load() {

		//Setup start positions
		String world = this.getConfig().getString("startLocation.world");
		startPositionA = new Location(this.getServer().getWorld(world), this.getConfig().getInt("startLocation.a.x"), this.getConfig().getInt("startLocation.a.y"), this.getConfig().getInt("startLocation.a.z"));
		startPositionB = new Location(this.getServer().getWorld(world), this.getConfig().getInt("startLocation.b.x"), this.getConfig().getInt("startLocation.b.y"), this.getConfig().getInt("startLocation.b.z"));
		respawnLoc = new Location(this.getServer().getWorld(world), this.getConfig().getInt("spawnLocation.x"), this.getConfig().getInt("spawnLocation.y"), this.getConfig().getInt("spawnLocation.z"));
		startPositionA.setYaw(this.getConfig().getInt("startLocation.a.yaw"));
		startPositionB.setYaw(this.getConfig().getInt("startLocation.b.yaw"));
		respawnLoc.setYaw(this.getConfig().getInt("spawnLocation.yaw"));

		//Setup inventory
		itemList = this.getConfig().getIntegerList("inventory");
	}

	@Override
	public void onEnable() {

		//Read configuration
		this.getConfig().options().copyDefaults(true);
		this.saveConfig();
		
		load();

		this.getServer().getPluginManager().registerEvents(this, this);

		this.getCommand("join").setExecutor(new JoinCommand(this));
		this.getCommand("leave").setExecutor(new LeaveCommand(this));
		this.getCommand("duel").setExecutor(new DuelCommand(this));

	}
	
	@EventHandler
	public void onDisconnect(PlayerQuitEvent event) {
		Logger l = J2MC_Manager.getCore().getLogger();
		if (status == GameStatus.Fighting) {
			if (event.getPlayer().equals(roundList.get(0))) {
				J2MC_Manager.getCore().getServer().broadcastMessage(ChatColor.RED + roundList.get(0).getName() + ChatColor.AQUA + " has abandoned the fight.");
				J2MC_Manager.getCore().getServer().broadcastMessage(ChatColor.RED + roundList.get(1).getName() + ChatColor.AQUA + " wins this duel!");
				l.log(Level.INFO, roundList.get(0).getName() + " left, " + roundList.get(1).getName() + " wins");
				roundList.get(0).teleport(respawnLoc);
				roundList.get(1).teleport(respawnLoc);
				participants.remove(roundList.get(0));
				status = GameStatus.Idle;
				roundList.remove(0);
				roundList.remove(0);
				return;
			} else if (event.getPlayer().equals(roundList.get(1))) {
				J2MC_Manager.getCore().getServer().broadcastMessage(ChatColor.RED + roundList.get(1).getName() + ChatColor.AQUA + " has abandoned the fight.");
				J2MC_Manager.getCore().getServer().broadcastMessage(ChatColor.RED + roundList.get(0).getName() + ChatColor.AQUA + " wins this duel!");
				l.log(Level.INFO, roundList.get(1).getName() + " left, " + roundList.get(0).getName() + " wins");
				roundList.get(0).teleport(respawnLoc);
				roundList.get(1).teleport(respawnLoc);
				participants.remove(roundList.get(1));
				status = GameStatus.Idle;
				roundList.remove(0);
				roundList.remove(0);
				return;
			}
		}
	}

	@EventHandler
	public void onDeath(PlayerDeathEvent event) {
		Logger l = J2MC_Manager.getCore().getLogger();
		if (status == GameStatus.Fighting) {
			event.getDrops().clear();
			if (event.getEntity().equals(roundList.get(0))) {
				J2MC_Manager.getCore().getServer().broadcastMessage(ChatColor.RED + roundList.get(0).getName() + ChatColor.AQUA + " is has been slain.");
				J2MC_Manager.getCore().getServer().broadcastMessage(ChatColor.RED + roundList.get(1).getName() + ChatColor.AQUA + " wins this duel!");
				l.log(Level.INFO, roundList.get(0).getName() + " killed, " + roundList.get(1).getName() + " wins");
				roundList.get(1).teleport(respawnLoc);
				participants.remove(roundList.get(0));
				status = GameStatus.Idle;
				roundList.remove(0);
				roundList.remove(0);
				return;
			} else if (event.getEntity().equals(roundList.get(1))) {
				J2MC_Manager.getCore().getServer().broadcastMessage(ChatColor.RED + roundList.get(1).getName() + ChatColor.AQUA + " is has been slain.");
				J2MC_Manager.getCore().getServer().broadcastMessage(ChatColor.RED + roundList.get(0).getName() + ChatColor.AQUA + " wins this duel!");
				l.log(Level.INFO, roundList.get(1).getName() + " killed, " + roundList.get(0).getName() + " wins");
				roundList.get(0).teleport(respawnLoc);
				participants.remove(roundList.get(1));
				status = GameStatus.Idle;
				roundList.remove(0);
				roundList.remove(0);
				return;
			}
		}
	}
	/*
	 * Takes the first two players and first checks if they are both online
	 * If both are offline, both are eliminated
	 * If one player is offline, the other automatically wins
	 */
	public void fight() {
		Logger l = J2MC_Manager.getCore().getLogger();
		l.log(Level.INFO, roundList.get(0).getName() + " and " + roundList.get(1).getName() + " fighting");
		if (roundList.size() >= 2) {
			//Check for AFK
			if (!roundList.get(0).isOnline() && !roundList.get(1).isOnline()) {
				J2MC_Manager.getCore().getServer().broadcastMessage(ChatColor.AQUA + "Both " + ChatColor.RED + roundList.get(0).getName() + ChatColor.AQUA + " and " + ChatColor.RED + roundList.get(1).getName() + ChatColor.AQUA + " are offline.");
				J2MC_Manager.getCore().getServer().broadcastMessage(ChatColor.AQUA + "Both players are eliminated from the tournament!");
				l.log(Level.INFO, "both " + roundList.get(0).getName() + " and " + roundList.get(1).getName() + " were offline, both removed");
				roundList.remove(0);
				roundList.remove(0);
				participants.remove(0);
				participants.remove(0);
				return;
			}
			if (!roundList.get(0).isOnline()) {
				J2MC_Manager.getCore().getServer().broadcastMessage(ChatColor.RED + roundList.get(0).getName() + ChatColor.AQUA + " is offline.");
				J2MC_Manager.getCore().getServer().broadcastMessage(ChatColor.RED + roundList.get(0).getName() + ChatColor.AQUA + " forfeits and " + ChatColor.RED + roundList.get(1).getName() + ChatColor.AQUA + " wins by default!");
				l.log(Level.INFO, roundList.get(0).getName() + " offline, " + roundList.get(1).getName() + " wins");
				roundList.remove(0);
				roundList.remove(0);
				participants.remove(0);
				return;
			}
			if (!roundList.get(1).isOnline()) {
				J2MC_Manager.getCore().getServer().broadcastMessage(ChatColor.RED + roundList.get(1).getName() + ChatColor.AQUA + " is offline.");
				J2MC_Manager.getCore().getServer().broadcastMessage(ChatColor.RED + roundList.get(1).getName() + ChatColor.AQUA + " forfeits and " + ChatColor.RED + roundList.get(0).getName() + ChatColor.AQUA + " wins by default!");
				l.log(Level.INFO, roundList.get(1).getName() + " offline, " + roundList.get(0).getName() + " wins");
				roundList.remove(0);
				roundList.remove(0);
				participants.remove(1);
				return;
			}
			J2MC_Manager.getCore().getServer().broadcastMessage(ChatColor.AQUA + "Now fighting: " + ChatColor.RED + roundList.get(0).getName() + ChatColor.AQUA + " and " + ChatColor.RED + roundList.get(1).getName());
			//Both players are online, set status to fighting so the event handler will pay attention
			status = GameStatus.Fighting;
			//Give each player a proper inventory, heal them, teleport them to their positions
			for (int x = 0; x < 2; x++) {
				Player p = roundList.get(x);
				PlayerInventory pInventory = p.getInventory();
				pInventory.clear(); //Working
				for (Integer i : itemList) {
					if (i.equals(262) || i.equals(341) || i.equals(332))
						pInventory.addItem(new ItemStack(i, 16));
					else if (i.equals(298) || i.equals(302) || i.equals(306) || i.equals(310) || i.equals(314) || i.equals(86))
						pInventory.setHelmet(new ItemStack(i));
					else if (i.equals(299) || i.equals(303) || i.equals(307) || i.equals(311) || i.equals(315))
						pInventory.setChestplate(new ItemStack(i));
					else if (i.equals(300) || i.equals(304) || i.equals(308) || i.equals(312) || i.equals(316))
						pInventory.setLeggings(new ItemStack(i));
					else if (i.equals(301) || i.equals(305) || i.equals(309) || i.equals(313) || i.equals(317))
						pInventory.setBoots(new ItemStack(i));
					else
						pInventory.addItem(new ItemStack(i));
				}
				p.setHealth(p.getMaxHealth()); //Working
				p.setFoodLevel(7); //Working
				if (x == 0) {
					p.teleport(startPositionA);
				} else {
					p.teleport(startPositionB);
				}
			}
		}
	}
}
