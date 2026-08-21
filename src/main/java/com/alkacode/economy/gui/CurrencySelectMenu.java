package com.alkacode.economy.gui;

import com.alkacode.core.gui.BaseGui;
import com.alkacode.economy.CurrencyDefinition;
import com.alkacode.economy.EconomyManager;
import com.alkacode.economy.gui.config.MenuConfig;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Map;

/**
 * Selecao de moeda pra abrir o TOP 10 (/topmoedas) - so lista as definicoes de
 * moeda (sem custo de banco); a busca do TOP em si so acontece ao clicar, e roda
 * fora da main thread (ver {@link #openTop}). Mesma nota do {@link BalanceMenu}
 * sobre numero de linhas variavel continuar em Java.
 */
public final class CurrencySelectMenu extends BaseGui {

    private final JavaPlugin plugin;
    private final EconomyManager economyManager;

    public CurrencySelectMenu(JavaPlugin plugin, Player player, EconomyManager economyManager) {
        super(plugin, player, MenuConfig.getInstance().title("currency-select-menu", null),
                rows(economyManager.getCurrencies().size()), "eco_top_select");
        this.plugin = plugin;
        this.economyManager = economyManager;
    }

    private static int rows(int currencyCount) {
        return Math.max(3, Math.min(6, 2 + (int) Math.ceil(currencyCount / 7.0)));
    }

    @Override
    public void render() {
        MenuConfig menu = MenuConfig.getInstance();
        fillBorder(menu.item("currency-select-menu.border", null));

        List<CurrencyDefinition> currencies = List.copyOf(economyManager.getCurrencies());
        int innerRows = (inventory.getSize() / 9) - 2;
        for (int i = 0; i < currencies.size() && i < innerRows * 7; i++) {
            CurrencyDefinition currency = currencies.get(i);
            int row = i / 7;
            int col = i % 7;
            int slot = (1 + row) * 9 + 1 + col;
            Map<String, String> placeholders = Map.of("name", currency.name(), "symbol", currency.symbol());
            setItem(slot, createItem(currency.icon(),
                    menu.text("currency-select-menu.item-name", placeholders),
                    menu.textList("currency-select-menu.item-lore", placeholders).toArray(new String[0])),
                    event -> openTop(currency));
        }
    }

    private void openTop(CurrencyDefinition currency) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<CurrencyTopMenu.Entry> entries = economyManager.getTopBalances(currency.id(), 10).stream()
                    .map(top -> {
                        OfflinePlayer offline = Bukkit.getOfflinePlayer(top.uuid());
                        String name = offline.getName() != null ? offline.getName() : "Desconhecido";
                        return new CurrencyTopMenu.Entry(top.uuid(), name, top.balance());
                    })
                    .toList();
            Bukkit.getScheduler().runTask(plugin,
                    () -> new CurrencyTopMenu(plugin, player, economyManager, currency, entries).open());
        });
    }
}
