package com.alkacode.economy.gui;

import com.alkacode.core.gui.BaseGui;
import com.alkacode.economy.CurrencyDefinition;
import com.alkacode.economy.EconomyManager;
import com.alkacode.economy.gui.config.MenuConfig;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Map;

/**
 * GUI de saldo do proprio jogador (/saldo) - um item por moeda registrada em
 * {@link EconomyManager#getCurrencies()}, sem tocar o banco: o jogador que abre o
 * menu ja esta online, entao o saldo dele ja esta no cache em memoria do
 * EconomyManager desde o join (ver PlayerConnectionListener). Numero de linhas
 * varia com a quantidade de moedas cadastradas, entao a matematica de posicao
 * continua em Java (nao da pra ser uma grade ASCII fixa) - so o template do
 * rotulo/lore e a borda vem de menus.yml (chave "balance-menu"); icone/nome/
 * simbolo de cada moeda continuam vindo do proprio {@link CurrencyDefinition}.
 */
public final class BalanceMenu extends BaseGui {

    private final EconomyManager economyManager;

    public BalanceMenu(JavaPlugin plugin, Player player, EconomyManager economyManager) {
        super(plugin, player, MenuConfig.getInstance().title("balance-menu", null),
                rows(economyManager.getCurrencies().size()), "eco_saldo");
        this.economyManager = economyManager;
    }

    private static int rows(int currencyCount) {
        return Math.max(3, Math.min(6, 2 + (int) Math.ceil(currencyCount / 7.0)));
    }

    @Override
    public void render() {
        MenuConfig menu = MenuConfig.getInstance();
        fillBorder(menu.item("balance-menu.border", null));

        List<CurrencyDefinition> currencies = List.copyOf(economyManager.getCurrencies());
        int innerRows = (inventory.getSize() / 9) - 2;
        for (int i = 0; i < currencies.size() && i < innerRows * 7; i++) {
            CurrencyDefinition currency = currencies.get(i);
            double balance = economyManager.getBalance(player.getUniqueId(), currency.id());
            int row = i / 7;
            int col = i % 7;
            int slot = (1 + row) * 9 + 1 + col;
            Map<String, String> placeholders = Map.of(
                    "name", currency.name(), "symbol", currency.symbol(),
                    "balance", EconomyManager.formatValue(balance));
            setItem(slot, createItem(currency.icon(),
                    menu.text("balance-menu.item-name", placeholders),
                    menu.textList("balance-menu.item-lore", placeholders).toArray(new String[0])));
        }
    }
}
