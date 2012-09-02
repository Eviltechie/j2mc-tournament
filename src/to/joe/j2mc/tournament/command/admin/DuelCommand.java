package to.joe.j2mc.tournament.command.admin;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import to.joe.j2mc.core.J2MC_Manager;
import to.joe.j2mc.core.command.MasterCommand;
import to.joe.j2mc.core.exceptions.BadPlayerMatchException;
import to.joe.j2mc.tournament.J2MC_Tournament;
import to.joe.j2mc.tournament.J2MC_Tournament.GameStatus;

public class DuelCommand extends MasterCommand {

    J2MC_Tournament plugin;

    public DuelCommand(J2MC_Tournament tournament) {
        super(tournament);
        this.plugin = tournament;
    }

    @Override
    public void exec(CommandSender sender, String commandName, String[] args, Player player, boolean isPlayer) {
        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Valid commands: kick <player>, add <player>, formup, fight, registration <open/close>, reset, r, list");
            return;
        }
        if (args[0].equalsIgnoreCase("kick")) {
            if (this.plugin.roundList.isEmpty()) {
                try {
                    Player p = J2MC_Manager.getVisibility().getPlayer(args[1], player);
                    if (this.plugin.participants.remove(p.getName()))
                        J2MC_Manager.getCore().getServer().broadcastMessage(ChatColor.RED + p.getName() + ChatColor.AQUA + " has been kicked from the tournament");
                } catch (BadPlayerMatchException e) {
                    sender.sendMessage(ChatColor.RED + e.getMessage());
                }
            } else {
                sender.sendMessage(ChatColor.RED + "You may not kick players in the middle of a round.");
            }
            return;
        }
        if (args[0].equalsIgnoreCase("add")) {
            try {
                Player p = J2MC_Manager.getVisibility().getPlayer(args[1], player);
                if (this.plugin.participants.contains(p.getName())) {
                    sender.sendMessage(ChatColor.RED + p.getName() + " is already in the tournament");
                    return;
                }
                this.plugin.participants.add(p.getName());
                J2MC_Manager.getCore().getServer().broadcastMessage(ChatColor.RED + p.getName() + ChatColor.AQUA + " has been added to the tournament");
            } catch (BadPlayerMatchException e) {
                sender.sendMessage(ChatColor.RED + e.getMessage());
            }
            return;
        }
        if (args[0].equalsIgnoreCase("formup")) { //Sets up the round
            if (this.plugin.participants.isEmpty()) {
                sender.sendMessage(ChatColor.RED + "There are no participants. Cannot formup.");
                return;
            } else if (this.plugin.roundList.isEmpty()) {
                this.plugin.setupRound();
                sender.sendMessage(ChatColor.AQUA + "Round setup sucessfully");
            } else {
                sender.sendMessage(ChatColor.RED + "A round is currently in progress");
            }
            return;
        }
        if (args[0].equalsIgnoreCase("fight")) {
            if (this.plugin.status == GameStatus.Fighting) {
                sender.sendMessage(ChatColor.RED + "A fight is already in progress");
            } else if (this.plugin.roundList.isEmpty()) {
                sender.sendMessage(ChatColor.RED + "There are no fights to be had");
            } else {
                this.plugin.fight();
            }
            return;
        }
        if (args[0].equals("r")) {
            if (this.plugin.registrationOpen)
                J2MC_Manager.getCore().getServer().broadcastMessage(ChatColor.AQUA + "Registration for the duel is now OPEN. Type " + ChatColor.RED + "/join" + ChatColor.AQUA + " to enter");
            else
                sender.sendMessage(ChatColor.RED + "Registration is closed you twat");
            return;
        }
        if (args[0].equalsIgnoreCase("registration")) {
            if (args[1].equalsIgnoreCase("open")) {
                this.plugin.registrationOpen = true;
                J2MC_Manager.getCore().getServer().broadcastMessage(ChatColor.AQUA + "Registration for the duel is now OPEN. Type " + ChatColor.RED + "/join" + ChatColor.AQUA + " to enter");
            }
            if (args[1].equalsIgnoreCase("close")) {
                this.plugin.registrationOpen = false;
                J2MC_Manager.getCore().getServer().broadcastMessage(ChatColor.AQUA + "Registration for the duel is now CLOSED.");
            }
            return;
        }
        if (args[0].equalsIgnoreCase("reset")) {
            this.plugin.registrationOpen = false;
            this.plugin.participants.clear();
            this.plugin.roundList.clear();
            this.plugin.status = GameStatus.Idle;
            sender.sendMessage(ChatColor.RED + "Registration closed and plugin reset.");
            return;
        }
        if (args[0].equalsIgnoreCase("reload")) {
            this.plugin.reloadConfig();
            this.plugin.load();
            sender.sendMessage(ChatColor.RED + "Configuration reloaded");
            return;
        }
        if (args[0].equalsIgnoreCase("list")) {
            sender.sendMessage(ChatColor.RED + this.plugin.participants.toString());
            return;
        }
        sender.sendMessage(ChatColor.RED + "Invalid command. Type /duel for options");
    }

}
