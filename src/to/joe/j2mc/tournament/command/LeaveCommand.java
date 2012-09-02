package to.joe.j2mc.tournament.command;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import to.joe.j2mc.core.J2MC_Manager;
import to.joe.j2mc.core.command.MasterCommand;
import to.joe.j2mc.tournament.J2MC_Tournament;

public class LeaveCommand extends MasterCommand {

    J2MC_Tournament plugin;

    public LeaveCommand(J2MC_Tournament tournament) {
        super(tournament);
        this.plugin = tournament;
    }

    @Override
    public void exec(CommandSender sender, String commandName, String[] args, Player player, boolean isPlayer) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players may use this command");
            return;
        }
        if (this.plugin.roundList.contains(player.getName())) {
            sender.sendMessage(ChatColor.RED + "You cannot leave the tournament when you are scheduled to fight.");
            return;
        }
        if (this.plugin.participants.contains(player.getName())) {
            this.plugin.participants.remove(player.getName());
            sender.sendMessage(ChatColor.AQUA + "You have removed yourself from the tournament");
            J2MC_Manager.getCore().getServer().broadcastMessage(ChatColor.RED + sender.getName() + ChatColor.AQUA + " has left the tournament!");
        } else {
            sender.sendMessage(ChatColor.AQUA + "You must be in the tournament to remove yourself from it");
        }
    }
}
