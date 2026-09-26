package de.yourname.verify;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class VerifyPlugin extends JavaPlugin implements CommandExecutor {

    private VerificationManager verificationManager;
    private DiscordBotService discordBotService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        verificationManager = new VerificationManager(this);
        discordBotService = new DiscordBotService(this, verificationManager);
        discordBotService.start();

        getCommand("verify").setExecutor(this);
        getLogger().info("MinecraftDiscordVerify wurde aktiviert.");
    }

    @Override
    public void onDisable() {
        if (discordBotService != null) {
            discordBotService.shutdown();
        }
        getLogger().info("MinecraftDiscordVerify wurde deaktiviert.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + getConfig().getString("messages.command-only-player", "§cDieser Befehl kann nur von Spielern ausgeführt werden."));
            return true;
        }

        if (verificationManager.isVerified(player.getUniqueId())) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages.already-verified", "&aDu bist bereits verifiziert.")));
            return true;
        }

        String code = verificationManager.createCode(player);
        discordBotService.sendVerificationMessage(player.getName(), code);

        player.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages.verification-started", "&aEin Verifizierungscode wurde im Discord-Channel gesendet.")));
        return true;
    }

    public VerificationManager getVerificationManager() {
        return verificationManager;
    }
}
