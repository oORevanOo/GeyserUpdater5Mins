package com.projectg.geyserupdater.bungee.util;

import com.projectg.geyserupdater.bungee.BungeeUpdater;
import com.projectg.geyserupdater.common.logger.UpdaterLogger;
import com.projectg.geyserupdater.common.util.FileUtils;
import com.projectg.geyserupdater.common.util.GeyserProperties;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class GeyserBungeeDownloader {
    private static BungeeUpdater plugin;
    private static UpdaterLogger logger;

    /**
     * Download the latest build of Geyser from Jenkins CI for the currently used branch.
     * If enabled in the config, the server will also attempt to restart.
     */
    public static void updateGeyser() {
        plugin = BungeeUpdater.getPlugin();
        logger = UpdaterLogger.getLogger();

        UpdaterLogger.getLogger().debug("Attempting to download a new build of Geyser.");

        // New task so that we don't block the main thread. All new tasks on bungeecord are async.
        plugin.getProxy().getScheduler().runAsync(plugin, () -> {
            // Download the newest geyser build
            if (downloadGeyser()) {
                String successMsg = "The latest build of Geyser has been downloaded! A restart must occur in order for changes to take effect.";
                logger.info(successMsg);
                for (ProxiedPlayer player : plugin.getProxy().getPlayers()) {
                    if (player.hasPermission("gupdater.geyserupdate")) {
                        player.sendMessage(new TextComponent(ChatColor.GREEN + successMsg));
                    }
                }
                if (plugin.getConfig().getBoolean("Auto-Restart-Server")) {
                    restartServer();
                }
            } else {
                // fail messages are already sent to the logger in downloadGeyser()
                String failMsg = "A severe error occurred when download a new build of Geyser. Please check the server console for further information!";
                for (ProxiedPlayer player : plugin.getProxy().getPlayers()) {
                    if (player.hasPermission("gupdater.geyserupdate")) {
                        player.sendMessage(new TextComponent(ChatColor.RED + failMsg));
                    }
                }
            }
        });
    }

    /**
     * Internal code for downloading the latest build of Geyser from Jenkins CI for the currently used branch.
     *
     * @return true if the download was successful, false if not.
     */
    private static boolean downloadGeyser() {
        String fileUrl;
        try {
            fileUrl = "https://ci.opencollab.dev/job/GeyserMC/job/Geyser/job/" + GeyserProperties.getGeyserGitPropertiesValue("git.branch") + "/lastSuccessfulBuild/artifact/bootstrap/bungeecord/build/libs/Geyser-BungeeCord.jar";
        } catch (IOException e) {
            logger.error("Failed to get the current Geyser branch when attempting to download a new build of Geyser!");
            e.printStackTrace();
            return false;
        }
        String outputPath = "plugins/GeyserUpdater/BuildUpdate/Geyser-BungeeCord.jar";
        try {
            FileUtils.downloadFile(fileUrl, outputPath);
        } catch (IOException e) {
            logger.error("Failed to download the newest build of Geyser");
            e.printStackTrace();
            return false;
        }

        if (!FileUtils.checkFile(outputPath, false)) {
            logger.error("Failed to find the downloaded Geyser build!");
            return false;
        } else {
            return true;
        }
    }

    /**
     * Attempt to restart the server
     */
    private static void restartServer() {
        logger.warn("Der Proxy wird in 2 Minuten neu starten! Dauer ca. 10 Sekunden!");
        for (ProxiedPlayer player : plugin.getProxy().getPlayers()) {
            player.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("Restart-Message-Players"))));
        }
        plugin.getProxy().getScheduler().schedule(plugin, () -> plugin.getProxy().stop(), 120L, TimeUnit.SECONDS);
    }
}
