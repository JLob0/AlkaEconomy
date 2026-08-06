package com.alkacode.economy.listener;

import com.alkacode.economy.EconomyManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class PlayerConnectionListener implements Listener {

    private final EconomyManager economyManager;

    public PlayerConnectionListener(EconomyManager economyManager) {
        this.economyManager = economyManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        handle(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        economyManager.unload(event.getPlayer().getUniqueId());
    }

    /**
     * Publico para tambem cobrir jogadores ja online quando o plugin e habilitado
     * (ex: apos /reload), que nao disparam PlayerJoinEvent de novo.
     */
    public void handle(Player player) {
        economyManager.loadForJoin(player);
    }
}
