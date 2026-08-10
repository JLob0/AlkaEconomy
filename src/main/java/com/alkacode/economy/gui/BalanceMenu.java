package com.alkacode.economy.gui;

import com.alkacode.core.gui.BaseGui;
import com.alkacode.economy.CurrencyDefinition;
import com.alkacode.economy.EconomyManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/**
 * GUI de saldo do proprio jogador (/saldo) - um item por moeda registrada em
 * {@link EconomyManager#getCurrencies()}, sem tocar o banco: o jogador que abre o
 * menu ja esta online, entao o saldo dele ja esta no cache em memoria do
 * EconomyManager desde o join (ver PlayerConnectionListener).
 */
public final class BalanceMenu extends BaseGui {

    private final EconomyManager economyManager;

    public BalanceMenu(JavaPlugin plugin, Player player, EconomyManager economyManager) {
        super(plugin, player, "<aqua><b>Seu Saldo</b>", rows(economyManager.getCurrencies().size()), "eco_saldo");
        this.economyManager = economyManager;
    }

    private static int rows(int currencyCount) {
        return Math.max(3, Math.min(6, 2 + (int) Math.ceil(currencyCount / 7.0)));
    }

    @Override
    public void render() {
        fillBorder(createItem(Material.GRAY_STAINED_GLASS_PANE, " "));

        List<CurrencyDefinition> currencies = List.copyOf(economyManager.getCurrencies());
        int innerRows = (inventory.getSize() / 9) - 2;
        for (int i = 0; i < currencies.size() && i < innerRows * 7; i++) {
            CurrencyDefinition currency = currencies.get(i);
            double balance = economyManager.getBalance(player.getUniqueId(), currency.id());
            int row = i / 7;
            int col = i % 7;
            int slot = (1 + row) * 9 + 1 + col;
            setItem(slot, createItem(currency.icon(),
                    "<white>" + currency.name() + " <gray>(" + currency.symbol() + ")",
                    "<gray>Saldo: <white>" + EconomyManager.formatValue(balance) + " " + currency.symbol()));
        }
    }
}
