package worldwarper.worldwarper;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.LinkedList;

public class CommandStart extends Command
{
	protected CommandStart() {
		super("worldwarperstart", "Start the worldwarper plugin", "/worldwarperstart", new LinkedList<>());
	}
	
	@Override
	public boolean execute(CommandSender commandSender, String s, String[] strings) {
		
		if(WorldWarper.running) {
			WorldWarper.running = false;
			Bukkit.broadcastMessage(ChatColor.RED + "World warper stopped");
		}
		else {
			WorldWarper.running = true;
			WorldWarper.lastWarp = WorldWarper.currentTick;
			Bukkit.broadcastMessage(ChatColor.GREEN + "World warper started");
		}
		
		return true;
	}
}
