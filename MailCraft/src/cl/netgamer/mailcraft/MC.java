package cl.netgamer.mailcraft;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;


public final class MC extends JavaPlugin
{
	// properties
	private static Logger logger;
	private static boolean usePermissions = false;
	private static String validChars = "";
	
	// ENABLE PLUGIN
	
	public void onEnable()
	{
		// utility
		logger = getLogger();
		
		// try db connection
		this.saveDefaultConfig();
		Database db = new Database(getConfig().getConfigurationSection("db"), this);
		if (!db.test())
		{
			MC.log("Database connection failed, exiting...");
			logger = null;
			return;
		}
		
		usePermissions = getConfig().getBoolean("usePermissions");
		validChars = getConfig().getString("validChars");
		
		getCommand("mailcraft").setExecutor(new Commands(db));

		// register commands
		
	}
	
	// UTILITIES
	
	protected static void log(String msg)
	{
		logger.info(msg);
	}
	
	protected static boolean can(CommandSender sender, String node)
	{
		// default allowed permissions list
		List<String> allowed = Arrays.asList(
			"mailcraft.use",
			"ds.tr",
			"dd.we"
		);
		
		// is console admin?
		if (!(sender instanceof Player)) return true;
		
		// is op?
		Player player = (Player)sender;
		if (player.isOp()) return true;
		
		// with permissions enabled: return explicit permissions
		if (usePermissions)
		return player.hasPermission(node);
		
		// with permissions disabled: return default permissions
		return allowed.contains(node);
	}
	
	protected static String cleanseBody(String[] body)
	{
		// glue fields
		String s = "";
		for (String b: body)
			s += " "+b;
		
		// cleanse fields
		return s.replaceAll("[^"+MC.validChars+"]", "_").trim();
	}
}
