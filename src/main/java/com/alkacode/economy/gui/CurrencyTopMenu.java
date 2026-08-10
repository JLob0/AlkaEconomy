package com.alkacode.economy.gui;

import com.alkacode.core.gui.BaseGui;
import com.alkacode.economy.CurrencyDefinition;
import com.alkacode.economy.EconomyManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.UUID;

/**
 * TOP 10 de uma moeda. Recebe as entradas ja resolvidas (uuid/nome/saldo) prontas -
 * {@link EconomyManager#getTopBalances} e {@link org.bukkit.OfflinePlayer#getName()}
 * sao bloqueantes, entao quem abre este menu (ver {@link CurrencySelectMenu}) busca
 * tudo numa thread assincrona antes de construir esta GUI na main thread.
 */
public final class CurrencyTopMenu extends BaseGui {

    public record Entry(UUID uuid, String name, double balance) {
    }

    private final JavaPlugin plugin;
    private final EconomyManager economyManager;
    private final CurrencyDefinition currency;
    private final List<Entry> entries;

    public CurrencyTopMenu(JavaPlugin plugin, Player player, EconomyManager economyManager,
                            CurrencyDefinition currency, List<Entry> entries) {
        super(plugin, player, "<aqua><b>TOP " + currency.name() + "</b>", 4, "eco_top_" + currency.id());
        this.plugin = plugin;
        this.economyManager = economyManager;
        this.currency = currency;
        this.entries = entries;
    }

    @Override
    public void render() {
        fillBorder(createItem(Material.GRAY_STAINED_GLASS_PANE, " "));

        if (entries.isEmpty()) {
            setItem(13, createItem(Material.BARRIER, "<red>Nenhum dado disponivel ainda."));
        } else {
            for (int i = 0; i < entries.size() && i < 10; i++) {
                int row = i / 7;
                int col = i % 7;
                int slot = (1 + row) * 9 + 1 + col;
                setItem(slot, medalItem(i + 1, entries.get(i)));
            }
        }

        setItem(31, createItem(Material.ARROW, "<red>Voltar"),
                event -> new CurrencySelectMenu(plugin, player, economyManager).open());
    }

    private ItemStack medalItem(int position, Entry entry) {
        String title = switch (position) {
            case 1 -> "<#FFD700><bold>🥇 " + entry.name();
            case 2 -> "<#AAAAAA><bold>🥈 " + entry.name();
            case 3 -> "<#FFAA55><bold>🥉 " + entry.name();
            default -> "<#55AAFF>" + position + "º Lugar <white>- " + entry.name();
        };
        return head(entry.name(), title,
                "<gray>Saldo: <white>" + EconomyManager.formatValue(entry.balance()) + " " + currency.symbol());
    }
}
