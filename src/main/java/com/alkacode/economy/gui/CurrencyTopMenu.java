package com.alkacode.economy.gui;

import com.alkacode.core.gui.BaseGui;
import com.alkacode.economy.CurrencyDefinition;
import com.alkacode.economy.EconomyManager;
import com.alkacode.economy.gui.config.MenuConfig;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * TOP 10 de uma moeda. Recebe as entradas ja resolvidas (uuid/nome/saldo) prontas -
 * {@link EconomyManager#getTopBalances} e {@link org.bukkit.OfflinePlayer#getName()}
 * sao bloqueantes, entao quem abre este menu (ver {@link CurrencySelectMenu}) busca
 * tudo numa thread assincrona antes de construir esta GUI na main thread. Layout
 * fixo (gui-layouts.yml, chave "currency-top-menu") - ver R8 no CLAUDE.md.
 */
public final class CurrencyTopMenu extends BaseGui {

    public record Entry(UUID uuid, String name, double balance) {
    }

    private final JavaPlugin plugin;
    private final EconomyManager economyManager;
    private final CurrencyDefinition currency;
    private final List<Entry> entries;
    private final MenuConfig.GuiLayout layout;

    public CurrencyTopMenu(JavaPlugin plugin, Player player, EconomyManager economyManager,
                            CurrencyDefinition currency, List<Entry> entries) {
        super(plugin, player, MenuConfig.getInstance().title("currency-top-menu", Map.of("currency", currency.name())),
                4, "eco_top_" + currency.id());
        this.plugin = plugin;
        this.economyManager = economyManager;
        this.currency = currency;
        this.entries = entries;
        this.layout = MenuConfig.getInstance().layout("currency-top-menu");
    }

    @Override
    public void render() {
        MenuConfig menu = MenuConfig.getInstance();
        fillBorder(menu.item("currency-top-menu.border", null));

        List<Integer> listSlots = layout.findSlots('0');
        if (entries.isEmpty()) {
            setItem(listSlots.get(3), menu.item("currency-top-menu.empty", null));
        } else {
            for (int i = 0; i < entries.size() && i < listSlots.size(); i++) {
                setItem(listSlots.get(i), medalItem(i + 1, entries.get(i)));
            }
        }

        setItem(layout.firstSlot('V'), menu.item("currency-top-menu.back", null),
                event -> new CurrencySelectMenu(plugin, player, economyManager).open());
    }

    private ItemStack medalItem(int position, Entry entry) {
        MenuConfig menu = MenuConfig.getInstance();
        Map<String, String> placeholders = Map.of("name", entry.name(), "position", String.valueOf(position));
        String path = switch (position) {
            case 1 -> "currency-top-menu.medal-gold";
            case 2 -> "currency-top-menu.medal-silver";
            case 3 -> "currency-top-menu.medal-bronze";
            default -> "currency-top-menu.medal-other";
        };
        String title = menu.text(path, placeholders);
        return head(entry.name(), title,
                menu.textList("currency-top-menu.item-lore", Map.of(
                        "balance", EconomyManager.formatValue(entry.balance()), "symbol", currency.symbol()))
                        .toArray(new String[0]));
    }
}
