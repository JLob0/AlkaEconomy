package com.alkacode.economy.vault;

import com.alkacode.economy.CurrencyDefinition;
import com.alkacode.economy.CurrencyRegistry;
import com.alkacode.economy.EconomyManager;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import net.milkbowl.vault.economy.EconomyResponse.ResponseType;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Ponte Vault -> AlkaEconomy. Manipula so a moeda marcada {@code vault-equivalent: true}
 * no config.yml (resolvida via {@link CurrencyRegistry} a cada chamada, nao mais fixa
 * em CurrencyType.COINS) - as demais moedas ficam acessiveis so via EconomyManager
 * direto (API interna entre plugins AlkaCode). Nao ha suporte a bancos - nenhum
 * plugin da network usa essa feature.
 */
public final class VaultHook implements Economy {

    private final EconomyManager economyManager;
    private final CurrencyRegistry registry;

    public VaultHook(EconomyManager economyManager, CurrencyRegistry registry) {
        this.economyManager = economyManager;
        this.registry = registry;
    }

    private String currencyId() {
        return registry.getVaultEquivalent().map(CurrencyDefinition::id).orElse(null);
    }

    private CurrencyDefinition currency() {
        return registry.getVaultEquivalent().orElse(null);
    }

    @Override
    public boolean isEnabled() {
        return currencyId() != null;
    }

    @Override
    public String getName() {
        return "AlkaEconomy";
    }

    @Override
    public boolean hasBankSupport() {
        return false;
    }

    @Override
    public int fractionalDigits() {
        return 2;
    }

    @Override
    public String format(double amount) {
        CurrencyDefinition currency = currency();
        String symbol = currency == null ? "" : currency.symbol();
        return String.format("%.2f %s", amount, symbol).trim();
    }

    @Override
    public String currencyNamePlural() {
        CurrencyDefinition currency = currency();
        return currency == null ? "" : currency.name();
    }

    @Override
    public String currencyNameSingular() {
        return currencyNamePlural();
    }

    @Override
    public boolean hasAccount(String playerName) {
        return hasAccount(Bukkit.getOfflinePlayer(playerName));
    }

    @Override
    public boolean hasAccount(OfflinePlayer player) {
        return true;
    }

    @Override
    public boolean hasAccount(String playerName, String worldName) {
        return hasAccount(playerName);
    }

    @Override
    public boolean hasAccount(OfflinePlayer player, String worldName) {
        return hasAccount(player);
    }

    @Override
    public double getBalance(String playerName) {
        return getBalance(Bukkit.getOfflinePlayer(playerName));
    }

    @Override
    public double getBalance(OfflinePlayer player) {
        String id = currencyId();
        return id == null ? 0 : economyManager.getBalance(player.getUniqueId(), id);
    }

    @Override
    public double getBalance(String playerName, String world) {
        return getBalance(playerName);
    }

    @Override
    public double getBalance(OfflinePlayer player, String world) {
        return getBalance(player);
    }

    @Override
    public boolean has(String playerName, double amount) {
        return has(Bukkit.getOfflinePlayer(playerName), amount);
    }

    @Override
    public boolean has(OfflinePlayer player, double amount) {
        String id = currencyId();
        return id != null && economyManager.has(player.getUniqueId(), id, amount);
    }

    @Override
    public boolean has(String playerName, String worldName, double amount) {
        return has(playerName, amount);
    }

    @Override
    public boolean has(OfflinePlayer player, String worldName, double amount) {
        return has(player, amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(String playerName, double amount) {
        return withdrawPlayer(Bukkit.getOfflinePlayer(playerName), amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, double amount) {
        String id = currencyId();
        UUID uuid = player.getUniqueId();
        if (id == null) {
            return new EconomyResponse(0, 0, ResponseType.FAILURE, "Nenhuma moeda vault-equivalent configurada.");
        }
        if (amount < 0) {
            return new EconomyResponse(0, economyManager.getBalance(uuid, id),
                    ResponseType.FAILURE, "Nao e possivel sacar um valor negativo.");
        }
        if (!economyManager.has(uuid, id, amount)) {
            return new EconomyResponse(0, economyManager.getBalance(uuid, id),
                    ResponseType.FAILURE, "Saldo insuficiente.");
        }
        economyManager.removeBalance(uuid, id, amount);
        return new EconomyResponse(amount, economyManager.getBalance(uuid, id), ResponseType.SUCCESS, null);
    }

    @Override
    public EconomyResponse withdrawPlayer(String playerName, String worldName, double amount) {
        return withdrawPlayer(playerName, amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, String worldName, double amount) {
        return withdrawPlayer(player, amount);
    }

    @Override
    public EconomyResponse depositPlayer(String playerName, double amount) {
        return depositPlayer(Bukkit.getOfflinePlayer(playerName), amount);
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, double amount) {
        String id = currencyId();
        UUID uuid = player.getUniqueId();
        if (id == null) {
            return new EconomyResponse(0, 0, ResponseType.FAILURE, "Nenhuma moeda vault-equivalent configurada.");
        }
        if (amount < 0) {
            return new EconomyResponse(0, economyManager.getBalance(uuid, id),
                    ResponseType.FAILURE, "Nao e possivel depositar um valor negativo.");
        }
        economyManager.addBalance(uuid, id, amount);
        return new EconomyResponse(amount, economyManager.getBalance(uuid, id), ResponseType.SUCCESS, null);
    }

    @Override
    public EconomyResponse depositPlayer(String playerName, String worldName, double amount) {
        return depositPlayer(playerName, amount);
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, String worldName, double amount) {
        return depositPlayer(player, amount);
    }

    @Override
    public EconomyResponse createBank(String name, String player) {
        return unsupportedBank();
    }

    @Override
    public EconomyResponse createBank(String name, OfflinePlayer player) {
        return unsupportedBank();
    }

    @Override
    public EconomyResponse deleteBank(String name) {
        return unsupportedBank();
    }

    @Override
    public EconomyResponse bankBalance(String name) {
        return unsupportedBank();
    }

    @Override
    public EconomyResponse bankHas(String name, double amount) {
        return unsupportedBank();
    }

    @Override
    public EconomyResponse bankWithdraw(String name, double amount) {
        return unsupportedBank();
    }

    @Override
    public EconomyResponse bankDeposit(String name, double amount) {
        return unsupportedBank();
    }

    @Override
    public EconomyResponse isBankOwner(String name, String playerName) {
        return unsupportedBank();
    }

    @Override
    public EconomyResponse isBankOwner(String name, OfflinePlayer player) {
        return unsupportedBank();
    }

    @Override
    public EconomyResponse isBankMember(String name, String playerName) {
        return unsupportedBank();
    }

    @Override
    public EconomyResponse isBankMember(String name, OfflinePlayer player) {
        return unsupportedBank();
    }

    @Override
    public List<String> getBanks() {
        return Collections.emptyList();
    }

    @Override
    public boolean createPlayerAccount(String playerName) {
        return true;
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer player) {
        return true;
    }

    @Override
    public boolean createPlayerAccount(String playerName, String worldName) {
        return true;
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer player, String worldName) {
        return true;
    }

    private EconomyResponse unsupportedBank() {
        return new EconomyResponse(0, 0, ResponseType.NOT_IMPLEMENTED, "AlkaEconomy nao possui suporte a bancos.");
    }
}
