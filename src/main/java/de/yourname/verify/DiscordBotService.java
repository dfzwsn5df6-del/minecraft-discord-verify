package de.yourname.verify;

import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.configuration.file.YamlConfiguration;

public class VerificationManager {

    private final VerifyPlugin plugin;
    private final Map<String, VerificationRequest> pendingCodes = new ConcurrentHashMap<>();
    private final Set<UUID> verifiedPlayers = ConcurrentHashMap.newKeySet();
    private final File verifiedFile;

    public VerificationManager(VerifyPlugin plugin) {
        this.plugin = plugin;
        this.verifiedFile = new File(plugin.getDataFolder(), "verified.yml");
        loadVerifiedPlayers();
    }

    public String createCode(Player player) {
        int codeLength = plugin.getConfig().getInt("discord.code-length", 6);
        int maxValue = (int) Math.pow(10, codeLength);
        String code = String.format("%0" + codeLength + "d", ThreadLocalRandom.current().nextInt(maxValue));
        pendingCodes.put(code, new VerificationRequest(player.getUniqueId(), player.getName()));
        return code;
    }

    public Optional<VerificationRequest> consumeCode(String code) {
        return Optional.ofNullable(pendingCodes.remove(code));
    }

    public boolean isVerified(UUID uuid) {
        return verifiedPlayers.contains(uuid);
    }

    public void markVerified(UUID uuid, String playerName, long discordUserId) {
        verifiedPlayers.add(uuid);
        saveVerifiedPlayers();
    }

    private void loadVerifiedPlayers() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        if (!verifiedFile.exists()) {
            try {
                verifiedFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("Konnte verified.yml nicht erstellen: " + e.getMessage());
            }
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(verifiedFile);
        for (String key : config.getKeys(false)) {
            verifiedPlayers.add(UUID.fromString(key));
        }
    }

    private void saveVerifiedPlayers() {
        YamlConfiguration config = new YamlConfiguration();
        for (UUID uuid : verifiedPlayers) {
            config.set(uuid.toString(), true);
        }

        try {
            config.save(verifiedFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Konnte verifizierte Spieler nicht speichern: " + e.getMessage());
        }
    }
}

record VerificationRequest(UUID uuid, String playerName) {
}
