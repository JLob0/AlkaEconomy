package com.alkacode.economy.hook;

import com.alkacode.economy.CurrencyRegistry;
import com.alkacode.economy.EconomyManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Placeholder dinamico %alkaeconomy_<currency-id>_<tipo>% - <currency-id> e
 * qualquer moeda registrada no config.yml (ver {@link CurrencyRegistry}, ja nao e
 * mais limitado ao enum CurrencyType) e <tipo> e "amount" (numero cru) ou
 * "formatted" (compacto com sufixo K/M/B/T via {@link EconomyManager#formatValue}).
 * So cobre jogadores online, ja que so onPlaceholderRequest(Player, ...) e
 * sobrescrito.
 */
public final class EconomyExpansion extends PlaceholderExpansion {

    private final EconomyManager economyManager;
    private final CurrencyRegistry registry;

    public EconomyExpansion(EconomyManager economyManager, CurrencyRegistry registry) {
        this.economyManager = economyManager;
        this.registry = registry;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "alkaeconomy";
    }

    @Override
    public @NotNull String getAuthor() {
        return "MestreDEV";
    }

    @Override
    public @NotNull String getVersion() {
        return "2.0.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, String params) {
        if (player == null || params == null) {
            return "";
        }

        int separatorIndex = params.lastIndexOf('_');
        if (separatorIndex <= 0 || separatorIndex == params.length() - 1) {
            return null;
        }

        String currencyId = params.substring(0, separatorIndex).toLowerCase();
        if (!registry.isValid(currencyId)) {
            return null;
        }

        // jogador sem essa moeda no banco cai no default do EconomyManager - ja
        // satisfaz o requisito de retornar o saldo inicial sem tratamento especial aqui.
        double balance = economyManager.getBalance(player.getUniqueId(), currencyId);

        return switch (params.substring(separatorIndex + 1).toLowerCase()) {
            case "amount" -> formatAmount(balance);
            case "formatted" -> EconomyManager.formatValue(balance);
            default -> null;
        };
    }

    private String formatAmount(double balance) {
        if (balance == Math.floor(balance) && !Double.isInfinite(balance)) {
            return String.valueOf((long) balance);
        }
        return String.format(java.util.Locale.US, "%.2f", balance);
    }
}
