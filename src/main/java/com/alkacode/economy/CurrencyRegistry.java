package com.alkacode.economy;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Carrega as moedas da secao {@code currencies} do config.yml. A ordem de
 * declaracao no YAML e preservada (LinkedHashMap) - importa pra /alkaeconomy list
 * e pro ciclo de moedas em GUIs de outros plugins (ex: SellKeyMenu do AlkaVips).
 */
public final class CurrencyRegistry {

    private final JavaPlugin plugin;
    private final Map<String, CurrencyDefinition> currencies = new LinkedHashMap<>();

    public CurrencyRegistry(JavaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        currencies.clear();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("currencies");
        if (section == null) {
            plugin.getLogger().warning("Nenhuma secao 'currencies' encontrada no config.yml - "
                    + "nenhuma moeda sera carregada.");
            return;
        }
        for (String id : section.getKeys(false)) {
            ConfigurationSection currency = section.getConfigurationSection(id);
            if (currency == null) {
                continue;
            }
            String normalizedId = id.toLowerCase(Locale.ROOT);
            currencies.put(normalizedId, new CurrencyDefinition(
                    normalizedId,
                    currency.getString("name", id),
                    currency.getString("symbol", ""),
                    currency.getDouble("default-balance", 0.0),
                    currency.getString("format-pattern", "#,##0.00"),
                    currency.getBoolean("vault-equivalent", false),
                    resolveIcon(currency.getString("icon"), normalizedId)
            ));
        }

        long vaultEquivalents = currencies.values().stream().filter(CurrencyDefinition::vaultEquivalent).count();
        if (vaultEquivalents > 1) {
            plugin.getLogger().warning("Mais de uma moeda marcada como vault-equivalent: true - "
                    + "o Vault vai usar apenas a primeira encontrada.");
        } else if (vaultEquivalents == 0) {
            plugin.getLogger().warning("Nenhuma moeda marcada como vault-equivalent: true - "
                    + "o hook do Vault ficara indisponivel.");
        }
    }

    /** Icone (GUIs de saldo/top) e config-driven, nunca hardcoded por id de moeda - ver icon no config.yml, default GOLD_NUGGET. */
    private Material resolveIcon(String materialName, String currencyId) {
        if (materialName == null || materialName.isBlank()) {
            return Material.GOLD_NUGGET;
        }
        Material material = Material.matchMaterial(materialName);
        if (material == null) {
            plugin.getLogger().warning("Icone invalido '" + materialName + "' para a moeda '" + currencyId
                    + "' - usando GOLD_NUGGET.");
            return Material.GOLD_NUGGET;
        }
        return material;
    }

    public Optional<CurrencyDefinition> get(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(currencies.get(id.toLowerCase(Locale.ROOT)));
    }

    public boolean isValid(String id) {
        return get(id).isPresent();
    }

    public Collection<CurrencyDefinition> getAll() {
        return currencies.values();
    }

    public Optional<CurrencyDefinition> getVaultEquivalent() {
        return currencies.values().stream().filter(CurrencyDefinition::vaultEquivalent).findFirst();
    }

    /**
     * Cria uma moeda nova e persiste no config.yml em disco (via
     * {@link JavaPlugin#saveConfig()}), pra sobreviver a um restart sem precisar
     * editar o arquivo manualmente. Retorna false se o id ja existir.
     */
    public boolean create(String id, String name, String symbol) {
        String normalizedId = id.toLowerCase(Locale.ROOT);
        if (currencies.containsKey(normalizedId)) {
            return false;
        }
        String path = "currencies." + normalizedId;
        plugin.getConfig().set(path + ".name", name);
        plugin.getConfig().set(path + ".symbol", symbol);
        plugin.getConfig().set(path + ".default-balance", 0.0);
        plugin.getConfig().set(path + ".format-pattern", "#,##0.00");
        plugin.getConfig().set(path + ".vault-equivalent", false);
        plugin.getConfig().set(path + ".icon", Material.GOLD_NUGGET.name());
        plugin.saveConfig();

        currencies.put(normalizedId, new CurrencyDefinition(normalizedId, name, symbol, 0.0, "#,##0.00", false, Material.GOLD_NUGGET));
        return true;
    }
}
