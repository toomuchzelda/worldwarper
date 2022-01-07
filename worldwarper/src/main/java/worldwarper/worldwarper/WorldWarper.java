package worldwarper.worldwarper;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandMap;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Painting;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Random;

public final class WorldWarper extends JavaPlugin
{
	public static boolean running = false;
	public static int lastWarp = 0;
	public static int currentTick = 0;
	// 5 mins
	public static final int TIME_TO_WARP = 5 * 60 * 20;
	public static final Random random = new Random();
	
	public static BukkitTask bukkitTask;
	
	@Override
	public void onEnable() {
		// Plugin startup logic
		
		running = false;
		lastWarp = 0;
		currentTick = 0;
		
		//register the command
		CommandMap map = Bukkit.getCommandMap();
		map.register("worldwarper", new CommandStart());
		
		bukkitTask = new BukkitRunnable() {
			@Override
			public void run() {
				tick();
			}
		}.runTaskTimer(this, 1, 0);
	}
	
	@Override
	public void onDisable() {
		// Plugin shutdown logic
		if(bukkitTask != null && !bukkitTask.isCancelled()) {
			bukkitTask.cancel();
		}
	}
	
	public static void tick()
	{
		currentTick++;
		if(!running)
			return;
		
		int ticksPassed = currentTick - lastWarp;
		int warpTime = lastWarp + TIME_TO_WARP;
		int minsLeft = (warpTime - currentTick) / (60 * 20);
		
		if(currentTick == warpTime) {
			
			//do teleport here
			
			//long startTime = System.currentTimeMillis();
			//Bukkit.broadcastMessage("Starting loop, time: " + startTime);
			Bukkit.broadcast(Component.text("Looking for somewhere to teleport...").color(NamedTextColor.DARK_GREEN));
			Location rand = somewhereRandom();
			//Bukkit.broadcastMessage("Ended loop, time taken: " + (System.currentTimeMillis() - startTime) + "ms");
			
			for(Player p : Bukkit.getOnlinePlayers()) {
				if(p.isInsideVehicle()) {
					Entity e = p.getVehicle();
					if(e != null) {
						e.removePassenger(p);
						e.teleport(rand);
					}
				}
				
				Vector pLoc = p.getLocation().toVector();
				Vector zero = new Vector();
				for(Entity e : p.getWorld().getEntities()) {
					if(!(e instanceof Painting) && e.getLocation().toVector().distance(pLoc) <= 5) {
						//nearby hostile mobs have 50% chance of coming along
						if(e instanceof Monster && random.nextFloat() < 0.5) {
							continue;
						}
						
						e.setVelocity(zero);
						Location eLoc = rand.clone().add(random.nextDouble(-5, 5), 0, random.nextDouble(-5, 5));
						//if they might suffocate
						if(!eLoc.getBlock().isEmpty() || !eLoc.getBlock().getRelative(0, 1, 0).isEmpty()) {
							eLoc = rand;
						}
						eLoc.setPitch(e.getLocation().getPitch());
						eLoc.setYaw(e.getLocation().getYaw());
						e.teleport(eLoc);
					}
				}
				
				p.teleport(rand);
			}
			
			rand.getWorld().playSound(rand, Sound.ITEM_CHORUS_FRUIT_TELEPORT, 99999, 1f);
			
			Bukkit.broadcast(Component.text("WOOoooOOOOooooOosh").color(TextColor.color(0, 255, 0)));
			lastWarp = currentTick;
			Bukkit.broadcast(Component.text(((TIME_TO_WARP / (60 * 20)) + 1) + "  minutes until next warp!").color(TextColor.color(255, 0 ,0)));
		}
		else if(ticksPassed % (60 * 20) == 0) { //&& minsLeft > 0) {
			Bukkit.broadcast(Component.text(minsLeft + " minutes until next warp!").color(TextColor.color(255, 0 ,0)));
		}
		
		//draw particles every tick 10 seconds before the warp happens
		if(warpTime - currentTick < 10 * 20) {
			double red = NamedTextColor.DARK_PURPLE.red();
			double green = NamedTextColor.DARK_PURPLE.green();
			double blue = NamedTextColor.DARK_PURPLE.blue();
			red /= 255;
			green /= 255;
			blue /= 255;
			if(red == 0)
				red = 0.0001;
			
			for(Player p : Bukkit.getOnlinePlayers()) {
				Location loc = p.getLocation();
				double x = loc.getX() + random.nextDouble(-5, 5);
				double y = loc.getY() + random.nextDouble(-5, 5);
				double z = loc.getZ() + random.nextDouble(-5, 5);
				
				p.spawnParticle(Particle.SPELL_MOB, x, y, z, 0, red, green, blue, 1);
			}
		}
	}
	
	public static Location somewhereRandom() {
		World[] arr = Bukkit.getWorlds().toArray(new World[0]);
		World chosenWorld = arr[Math.abs(random.nextInt()) % arr.length];
		/*for(World world : Bukkit.getWorlds()) {
			Bukkit.broadcastMessage(world.getName());
		}*/
		
		ArrayList<Location> locsList = new ArrayList<>(10);
		
		while(locsList.size() == 0) {
			loopForSpawn(chosenWorld, locsList);
		}
		
		//return locsList.get(randomMax(locsList.size() - 1));
		//Bukkit.broadcastMessage(locsList.toString());
		
		return locsList.get(randomMax(locsList.size() - 1));
	}
	
	public static void loopForSpawn(World world, ArrayList<Location> list) {
		//just randomly in 40k x 40k area, and loop to find a Y value that's safely spawnable
		int x = randomRange(-5000, 5000);
		int z = randomRange(-5000, 5000);
		int maxY;
		//don't teleport onto the nether ceiling
		if(world.getEnvironment() == World.Environment.NETHER)
			maxY = 127;
		else
			maxY = world.getMaxHeight();
		
		for(int i = world.getMinHeight(); i < maxY - 2; i++) {
			Block block = world.getBlockAt(x, i, z);
			if(block.isSolid()) {
				boolean add = false;
				if(block.getRelative(BlockFace.UP).isEmpty() && block.getRelative(BlockFace.UP, 2).isEmpty()) {
					add = true;
				}
				//30% chance spawn underwater
				else if(block.getRelative(0, 1, 0).getBlockData().getMaterial() == Material.WATER ||
						block.getRelative(0, 2, 0).getBlockData().getMaterial() == Material.WATER) {
					if(random.nextFloat() <= 0.3) {
						list.add(new Location(world, x, i + 1, z));
						add = true;
					}
				}
				
				if(add) {
					//Bukkit.broadcastMessage("Found place at: " + x + "," + (i + 1) + "," + z);
					list.add(new Location(world, x, i + 1, z));
				}
			}
		}
	}
	
	public static int randomMax(int max) {
		// + 1 to not exclude the max value itself
		return random.nextInt(max + 1);
	}
	
	public static int randomRange(int min, int max) {
		return randomMax(max) + min;
	}
}
