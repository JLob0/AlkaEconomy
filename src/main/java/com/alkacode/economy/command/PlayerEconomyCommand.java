package com.alkacode.economy.command;

import com.alkacode.core.api.MessageProvider;
import com.alkacode.economy.EconomyManager;
import com.alkacode.economy.gui.BalanceMenu;
import com.alkacode.economy.gui.CurrencySelectMenu;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Comandos de jogador (sem permissao de admin, ao contrario de /alkaeconomy):
 * /saldo abre {@link BalanceMenu} (proprio saldo em todas as moedas), /topmoedas
 * abre {@link CurrencySelectMenu} (ranking por moeda). A mesma instancia e
 * registrada para os dois comandos - o "label" recebido diferencia qual foi
 * digitado.
 */
public final class PlayerEconomyCommand implements CommandExecutor {

    private final JavaPlugin plugin;
    private final EconomyManager economyManager;
    private final MessageProvider messages;

    public PlayerEconomyCommand(JavaPlugin plugin, EconomyManager economyManager, MessageProvider messages) {
        this.plugin = plugin;
        this.economyManager = economyManager;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            messages.sendPrefixed(sender, "<red>Apenas jogadores podem usar este comando.");
            return true;
        }

        if (label.equalsIgnoreCase("topmoedas") || label.equalsIgnoreCase("ecotop") || label.equalsIgnoreCase("topeco")) {
            new CurrencySelectMenu(plugin, player, economyManager).open();
        } else {
            new BalanceMenu(plugin, player, economyManager).open();
        }
        return true;
    }
}
