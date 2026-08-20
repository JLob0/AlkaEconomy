package com.alkacode.economy.command;

import com.alkacode.core.api.MessageProvider;
import com.alkacode.economy.CurrencyDefinition;
import com.alkacode.economy.CurrencyRegistry;
import com.alkacode.economy.EconomyManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Comando admin do AlkaEconomy - give/take/set operam por UUID (OfflinePlayer),
 * entao funcionam mesmo com o alvo desconectado. reload/create/list gerenciam as
 * moedas dinamicas do config.yml (ver {@link CurrencyRegistry}).
 */
public final class AlkaEconomyCommand implements CommandExecutor, TabCompleter {

    private final JavaPlugin plugin;
    private final EconomyManager economyManager;
    private final CurrencyRegistry currencyRegistry;
    private final MessageProvider messages;

    public AlkaEconomyCommand(JavaPlugin plugin, EconomyManager economyManager, CurrencyRegistry currencyRegistry, MessageProvider messages) {
        this.plugin = plugin;
        this.economyManager = economyManager;
        this.currencyRegistry = currencyRegistry;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("alkaeconomy.admin")) {
            messages.sendPrefixed(sender, "<red>Voce nao tem permissao para usar este comando.");
            return true;
        }
        if (args.length == 0) {
            sendUsage(sender, label);
            return true;
        }

        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "give", "take", "set" -> handleBalanceChange(sender, label, args);
            case "reload" -> handleReload(sender);
            case "create" -> handleCreate(sender, label, args);
            case "list" -> handleList(sender);
            default -> {
                sendUsage(sender, label);
                yield true;
            }
        };
    }

    private boolean handleBalanceChange(CommandSender sender, String label, String[] args) {
        if (args.length < 4) {
            messages.sendPrefixed(sender, "<red>Uso: /" + label + " <give|take|set> <player> <currency-id> <valor>");
            return true;
        }
        String action = args[0].toLowerCase(Locale.ROOT);

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            messages.sendPrefixed(sender, "<red>Jogador '" + args[1] + "' nunca entrou no servidor.");
            return true;
        }

        String currencyId = args[2].toLowerCase(Locale.ROOT);
        if (!currencyRegistry.isValid(currencyId)) {
            messages.sendPrefixed(sender, "<red>Moeda invalida '" + args[2] + "'. Use /" + label + " list para ver as moedas registradas.");
            return true;
        }

        double amount;
        try {
            amount = EconomyManager.parseAmount(args[3])
                    .orElseThrow(() -> new NumberFormatException("invalid amount"));
        } catch (NumberFormatException e) {
            messages.sendPrefixed(sender, "<red>Valor invalido: " + args[3] + " (use ex.: 5000, 50K, 5M, 2B, 1T)");
            return true;
        }
        if (amount < 0) {
            messages.sendPrefixed(sender, "<red>O valor precisa ser positivo.");
            return true;
        }

        switch (action) {
            case "give" -> economyManager.addBalance(target.getUniqueId(), currencyId, amount);
            case "take" -> economyManager.removeBalance(target.getUniqueId(), currencyId, amount);
            case "set" -> economyManager.setBalance(target.getUniqueId(), currencyId, amount);
        }

        double newBalance = economyManager.getBalance(target.getUniqueId(), currencyId);
        messages.sendPrefixed(sender, "<green>" + actionLabel(action) + " <white>" + amount + " " + currencyId
                + " <green>para <white>" + target.getName() + "<green>. Novo saldo: <white>"
                + EconomyManager.formatValue(newBalance));
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        plugin.reloadConfig();
        currencyRegistry.reload();
        messages.sendPrefixed(sender, "<green>Config recarregado - <white>" + currencyRegistry.getAll().size()
                + " <green>moedas carregadas.");
        return true;
    }

    private boolean handleCreate(CommandSender sender, String label, String[] args) {
        if (args.length < 4) {
            messages.sendPrefixed(sender, "<red>Uso: /" + label + " create <id> <nome> <simbolo>");
            return true;
        }
        String id = args[1].toLowerCase(Locale.ROOT);
        String name = args[2];
        String symbol = args[3];

        if (!currencyRegistry.create(id, name, symbol)) {
            messages.sendPrefixed(sender, "<red>Ja existe uma moeda com o id '" + id + "'.");
            return true;
        }
        messages.sendPrefixed(sender, "<green>Moeda <white>" + id + " <green>(" + name + ", " + symbol + ") criada com sucesso.");
        return true;
    }

    private boolean handleList(CommandSender sender) {
        messages.sendPrefixed(sender, "<yellow>Moedas registradas (" + currencyRegistry.getAll().size() + "):");
        for (CurrencyDefinition currency : currencyRegistry.getAll()) {
            String vaultTag = currency.vaultEquivalent() ? " <gold>[vault]" : "";
            messages.send(sender, "<gray> - <white>" + currency.id() + " <gray>(" + currency.name()
                    + ", " + currency.symbol() + ")" + vaultTag);
        }
        return true;
    }

    private String actionLabel(String action) {
        return switch (action) {
            case "give" -> "Adicionado";
            case "take" -> "Removido";
            default -> "Definido";
        };
    }

    private void sendUsage(CommandSender sender, String label) {
        messages.sendPrefixed(sender, "<red>Uso: /" + label + " <give|take|set|reload|create|list>");
    }

    private static final List<String> SUBCOMMANDS = List.of("give", "take", "set", "reload", "create", "list");
    private static final List<String> BALANCE_ACTIONS = List.of("give", "take", "set");

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("alkaeconomy.admin")) {
            return List.of();
        }
        if (args.length == 1) {
            return filter(SUBCOMMANDS, args[0]);
        }
        String action = args[0].toLowerCase(Locale.ROOT);
        if (BALANCE_ACTIONS.contains(action)) {
            if (args.length == 2) {
                return filter(onlinePlayerNames(), args[1]);
            }
            if (args.length == 3) {
                return filter(currencyRegistry.getAll().stream().map(CurrencyDefinition::id).toList(), args[2]);
            }
        }
        return List.of();
    }

    private List<String> onlinePlayerNames() {
        return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
    }

    private List<String> filter(List<String> options, String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        return options.stream().filter(option -> option.toLowerCase(Locale.ROOT).startsWith(lower)).collect(Collectors.toList());
    }
}
