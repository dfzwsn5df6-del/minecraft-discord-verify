package de.yourname.verify;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Optional;

public class DiscordBotService {

    private final VerifyPlugin plugin;
    private final VerificationManager verificationManager;
    private final String botToken;
    private final long verificationChannelId;
    private JDA jda;

    public DiscordBotService(VerifyPlugin plugin, VerificationManager verificationManager) {
        this.plugin = plugin;
        this.verificationManager = verificationManager;
        this.botToken = plugin.getConfig().getString("discord.bot-token", "");
        this.verificationChannelId = plugin.getConfig().getLong("discord.verification-channel-id", 0L);
    }

    public void start() {
        if (botToken == null || botToken.isBlank() || verificationChannelId == 0L) {
            plugin.getLogger().warning("Discord-Bot wurde deaktiviert. Bitte bot-token und verification-channel-id in der config.yml setzen.");
            return;
        }

        try {
            jda = JDABuilder.createDefault(botToken)
                    .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                    .addEventListeners(new VerificationListener())
                    .build()
                    .awaitReady();

            plugin.getLogger().info("Discord-Bot wurde gestartet. Kanal-ID: " + verificationChannelId);
        } catch (Exception e) {
            plugin.getLogger().severe("Discord-Bot konnte nicht gestartet werden: " + e.getMessage());
        }
    }

    public void shutdown() {
        if (jda != null) {
            jda.shutdown();
        }
    }

    public void sendVerificationMessage(String playerName, String code) {
        if (jda == null) {
            plugin.getLogger().warning("Discord-Bot ist nicht aktiv. Verifizierung konnte nicht gesendet werden.");
            return;
        }

        TextChannel channel = jda.getTextChannelById(verificationChannelId);
        if (channel == null) {
            plugin.getLogger().warning("Discord-Kanal wurde nicht gefunden: " + verificationChannelId);
            return;
        }

        channel.sendMessage("🔐 Minecraft-Verifikation\n" +
                "Spieler: **" + playerName + "**\n" +
                "Code: **" + code + "**\n" +
                "Schreibe im Kanal genau: `!verify " + code + "`"
        ).queue();
    }

    private class VerificationListener extends ListenerAdapter {

        @Override
        public void onMessageReceived(MessageReceivedEvent event) {
            if (event.getAuthor().isBot()) {
                return;
            }

            if (event.getChannel().getIdLong() != verificationChannelId) {
                return;
            }

            String rawMessage = event.getMessage().getContentRaw().trim();
            String code = normalizeCode(rawMessage);
            if (code == null || code.isBlank()) {
                return;
            }

            Optional<VerificationRequest> request = verificationManager.consumeCode(code);
            if (request.isEmpty()) {
                return;
            }

            VerificationRequest verificationRequest = request.get();
            verificationManager.markVerified(verificationRequest.uuid(), verificationRequest.playerName(), event.getAuthor().getIdLong());

            event.getMessage().delete().queue();
            event.getChannel().sendMessage("✅ Spieler **" + verificationRequest.playerName() + "** wurde verifiziert!").queue();

            Player onlinePlayer = plugin.getServer().getPlayer(verificationRequest.uuid());
            if (onlinePlayer != null) {
                onlinePlayer.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.verification-success", "&aDu bist jetzt verifiziert!")));
            }
        }

        private String normalizeCode(String rawMessage) {
            String trimmed = rawMessage.trim();
            if (trimmed.equalsIgnoreCase("!verify")) {
                return null;
            }

            if (trimmed.regionMatches(true, 0, "!verify", 0, 7)) {
                trimmed = trimmed.substring(7).trim();
            }

            trimmed = trimmed.replaceAll("[^A-Za-z0-9]", "");
            if (trimmed.length() != plugin.getConfig().getInt("discord.code-length", 6)) {
                return null;
            }

            return trimmed;
        }
    }
}
