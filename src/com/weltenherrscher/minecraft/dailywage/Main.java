package com.weltenherrscher.minecraft.dailywage;

/*
*
*  Copyright (c) 2020 Weltenherrscher
*  
*  Discussion:
*  https://www.spigotmc.org/resources/daily-wage.75728/ 
*
*  Source:
*  https://github.com/Dennis1000/DailyWage 
*/

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.file.FileConfiguration;
import ch.dkrieger.coinsystem.core.CoinSystem;
import ch.dkrieger.coinsystem.core.player.CoinPlayer;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;

public class Main extends JavaPlugin {

	// Plugin consts
	public static final String PLUGIN_NAME = "DailyWage";
	public static final String PLUGIN_VERSION = "1.0.1";
	public static final String PLUGIN_AUTHOR = "Dennis Spreen aka Weltenherrscher";

	// System configuration
	private static final String CONFIG_SYSTEM = "System";
	private static final String CONFIG_SYSTEM_COINSPERDAY = CONFIG_SYSTEM + ".CoinsPerDay";
	private static final String CONFIG_SYSTEM_PASSWORD = CONFIG_SYSTEM + ".Password";
	private static final String CONFIG_SYSTEM_BACKPAYMENT = CONFIG_SYSTEM + ".BackPayment";
	private static final String CONFIG_SYSTEM_BACKPAYMENT_ENABLE = CONFIG_SYSTEM_BACKPAYMENT + ".Enable";
	private static final String CONFIG_SYSTEM_BACKPAYMENT_MAXDAYS = CONFIG_SYSTEM_BACKPAYMENT + ".MaxDays";

	// Message configuration
	private static final String MESSAGES = "Messages";
	private static final String MESSAGE_WRONGPASSWORD = MESSAGES + ".WrongPassword";
	private static final String MESSAGE_USAGEWITHPASSWORD = MESSAGES + ".UsageWithPasswordSet";
	private static final String MESSAGE_PLAYERNOTFOUND = MESSAGES + ".PlayerNotFound";
	private static final String MESSAGE_NOCOINSYSTEM = MESSAGES + ".NoCoinSystem";
	private static final String MESSAGE_ALREADYFETCHED = MESSAGES + ".AlreadyFetched";
	private static final String MESSAGE_YOURWAGE = MESSAGES + ".YourWage";
	private static final String MESSAGE_COINBALANCE = MESSAGES + ".CoinBalance";

	// Player configuration
	private static final String PLAYER_LASTPAYMENTDATE = "LastPaymentDate";

	private Integer coinsPerDay; // Coins payed out per day
	private String password; // Command password (optional)
	private Boolean backPayment; // If set then calculate pay out since last payment
	private Integer backPaymentMaxDays; // Maximum of past days
	private FileConfiguration config; // Configuration file
	private YamlConfiguration messages; // Messages file
	private File dataFolder; // Storage folder

	public void defaultConfiguration() {
		// Add system defaults
		config.options().copyDefaults(true);
		config.addDefault(CONFIG_SYSTEM_COINSPERDAY, 1);
		config.addDefault(CONFIG_SYSTEM_PASSWORD, "");
		config.addDefault(CONFIG_SYSTEM_BACKPAYMENT_ENABLE, true);
		config.addDefault(CONFIG_SYSTEM_BACKPAYMENT_MAXDAYS, 100);

		// Save the configuration
		saveConfig();

		// Add message file
		File messagesFile = new File(getDataFolder(), "messages.yml");
		messages = YamlConfiguration.loadConfiguration(messagesFile);
		messages.options().copyDefaults(true);

		// Add default messages
		messages.addDefault(MESSAGE_WRONGPASSWORD, "&cWrong password!");
		messages.addDefault(MESSAGE_USAGEWITHPASSWORD, "&cUsage: &f{command} <password> [target]");
		messages.addDefault(MESSAGE_PLAYERNOTFOUND, "&cThe player &e{player}&c was not found!");
		messages.addDefault(MESSAGE_NOCOINSYSTEM, "&cNo connection to the coin system!");
		messages.addDefault(MESSAGE_ALREADYFETCHED, "&cYou already fetched your daily wage!");
		messages.addDefault(MESSAGE_YOURWAGE, "&aYour wage is &e{wage} coins&a for {days} days");
		messages.addDefault(MESSAGE_COINBALANCE, "&fYou now have &e{coins} coins");

		try {
			messages.save(messagesFile);
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	public void loadConfiguration() {
		// Load configuration
		config = getConfig();

		// If not yet done set default configuration
		defaultConfiguration();

		// Get settings
		coinsPerDay = config.getInt(CONFIG_SYSTEM_COINSPERDAY);
		password = config.getString(CONFIG_SYSTEM_PASSWORD);
		backPayment = config.getBoolean(CONFIG_SYSTEM_BACKPAYMENT_ENABLE);
		backPaymentMaxDays = config.getInt(CONFIG_SYSTEM_BACKPAYMENT_MAXDAYS);
	}

	@Override
	public void onLoad() {
		getLogger().info(PLUGIN_NAME + " v" + PLUGIN_VERSION + " by " + PLUGIN_AUTHOR);

		// Create plugin folder if not yet done
		if (!getDataFolder().exists())
			getDataFolder().mkdirs();

		dataFolder = new File(getDataFolder(), "data");
		if (!dataFolder.exists())
			dataFolder.mkdir();
	}

	@Override
	public void onEnable() {
		getLogger().info("Load configuration");

		// Load or create default configuration
		loadConfiguration();
	}

	@Override
	public void onDisable() {
		getLogger().info("Plugin disabled.");
	}

	public long getDifferenceDays(Date d1, Date d2) {
		long diff = d2.getTime() - d1.getTime();
		return TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS);
	}

	private Player getNearestPlayer(CommandSender sender, Object ignoredPlayer) {
		Location location;

		// If command is used in a command block then the sender is BlockCommandSender
		if (sender instanceof BlockCommandSender)
			location = ((BlockCommandSender) sender).getBlock().getLocation();
		else if (sender instanceof Player)
			location = ((Player) sender).getLocation();
		else
			return null;

		// Find nearest player to location
		Player nearest = null;
		double nearestDistance = Double.MAX_VALUE;
		for (Player player : Bukkit.getOnlinePlayers()) {

			if (player != ignoredPlayer && player.getLocation().getWorld() == location.getWorld()) {
				double distance = player.getLocation().distance(location);
				if (distance < nearestDistance) {
					nearestDistance = distance;
					nearest = player;
				}
			}
		}
		return nearest;
	}

	private boolean sendMessage(Player player, String message) {

		if (player != null) {
			player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
			return true;
		} else { // if no player is found then write to console
			getLogger().info(message);
			return false;
		}
	}

	private Player getLocalPlayer(CommandSender sender) {

		Player player = null;

		// Send message to player if invoked via player
		if (sender instanceof Player)
			player = (Player) sender;
		else // else find nearest player
			player = getNearestPlayer(sender, null);

		return player;
	}

	private Player getTargetPlayer(CommandSender sender, String playerName) {

		// @p = nearest player
		if (playerName.equalsIgnoreCase("@p")) {
			return getNearestPlayer(sender, sender);
		} else {

			// Find player by name
			for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
				if (onlinePlayer.getName().equalsIgnoreCase(playerName))
					return onlinePlayer;
			}
			return null;
		}
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

		// Check for wage-command
		if (!command.getName().equalsIgnoreCase("wage") && !command.getName().equalsIgnoreCase("wage")
				&& !command.getName().equalsIgnoreCase("dailywage") && !command.getName().equalsIgnoreCase("salaire"))
			return false;

		// Find local player
		Player player = getLocalPlayer(sender);

		// Verify password if a system password is set
		if (password != null && !password.isEmpty()) {
			if (args.length >= 1) {

				if (!args[0].equalsIgnoreCase(password)) {
					sendMessage(player, messages.getString(MESSAGE_WRONGPASSWORD));
					return true;
				}

			} else {
				sendMessage(player, messages.getString(MESSAGE_USAGEWITHPASSWORD).replace("{command}", label));
				return true;
			}
		}

		// Get targeted player
		Integer posTarget = 0;
		if (!password.isEmpty())
			posTarget++;

		if (args.length >= posTarget + 1) {
			String target = args[posTarget];

			Player targetPlayer = getTargetPlayer(sender, target);
			if (targetPlayer == null) {
				sendMessage(player, messages.getString(MESSAGE_PLAYERNOTFOUND).replace("{player}", target));
				return true;
			}
			player = targetPlayer;
		}

		// Connect to the coin system and retrieve the coinPlayer be UUID
		CoinPlayer coinPlayer = CoinSystem.getInstance().getPlayerManager().getPlayer(player.getUniqueId());

		if (coinPlayer == null) {
			sendMessage(player, messages.getString(MESSAGE_NOCOINSYSTEM));
			return true;
		}

		// Set date format used for last payment
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
		Date currentTime = new Date();

		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DATE, -1);
		Date yesterday = cal.getTime();

		// Load player payment settings
		YamlConfiguration playerConfig;
		File playerFile = new File(dataFolder, player.getUniqueId() + ".yml");
		playerConfig = YamlConfiguration.loadConfiguration(playerFile);
		playerConfig.addDefault(PLAYER_LASTPAYMENTDATE, dateFormat.format(yesterday));

		// Get last payment date
		Date lastPayDay = currentTime;
		try {
			lastPayDay = dateFormat.parse(playerConfig.getString(PLAYER_LASTPAYMENTDATE));
		} catch (ParseException e) {
			e.printStackTrace();
		}

		// Calculate days in between
		Long difference = getDifferenceDays(lastPayDay, currentTime);
		if (difference < 1) {
			sendMessage(player, messages.getString(MESSAGE_ALREADYFETCHED));
		} else {

			// Ensure the maximum of back payment days
			if ((backPaymentMaxDays > 0) && (difference > backPaymentMaxDays))
				difference = Long.valueOf(backPaymentMaxDays);

			// Calculate coins - with back payment if set
			Integer coins = coinsPerDay;
			if (backPayment)
				coins = coins * Math.toIntExact(difference);

			playerConfig.set(PLAYER_LASTPAYMENTDATE, dateFormat.format(currentTime));

			// Save player data
			try {
				playerConfig.save(playerFile);

				// Add coins only if payment file was written
				coinPlayer.addCoins(coins);

				sendMessage(player,
						messages.getString(MESSAGE_YOURWAGE).replace("{wage}", coins.toString())
								.replace("{days}", difference.toString())
								.replace("{coins}", Long.toString(coinPlayer.getCoins())));

				sendMessage(player, messages.getString(MESSAGE_COINBALANCE).replace("{coins}",
						Long.toString(coinPlayer.getCoins())));

			} catch (IOException ex) {
				ex.printStackTrace();
			}

		}

		return true;
	}
}