package com.alkacode.economy;

import com.alkacode.core.api.AlkaAPI;
import com.alkacode.core.plugin.AlkaPlugin;
import com.alkacode.economy.command.AlkaEconomyCommand;
import com.alkacode.economy.command.PlayerEconomyCommand;
import com.alkacode.economy.gui.config.MenuConfig;
import com.alkacode.economy.hook.EconomyExpansion;
import com.alkacode.economy.listener.PlayerConnectionListener;
import com.alkacode.economy.storage.EconomyRepository;
import com.alkacode.economy.vault.VaultHook;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.ServicePriority;

/**
 * Base economica da network Alka* - sobre o AlkaCore (banco/HikariCP via
 * api.getDatabase(), sem DatabaseManager/executor proprio - ver
 * {@link EconomyRepository}). Estende {@link AlkaPlugin} em vez de JavaPlugin
 * direto: isso garante que {@link AlkaAPI#get()} ja esta pronta quando
 * {@link #onPluginEnable()} roda (via `depend: [AlkaCore]` no plugin.yml) e evita
 * registrar o GuiListener de novo (o Core ja faz isso uma unica vez).
 */
public final class AlkaEconomyPlugin extends AlkaPlugin {

    private CurrencyRegistry currencyRegistry;
    private EconomyRepository economyRepository;
    private EconomyManager economyManager;

    @Override
    protected void onPluginEnable() {
        AlkaAPI api = getAlkaAPI();

        MenuConfig.init(this);
        currencyRegistry = new CurrencyRegistry(this);
        economyRepository = new EconomyRepository(api.getDatabase(), getLogger());
        economyManager = new EconomyManager(this, economyRepository, currencyRegistry);

        PlayerConnectionListener connectionListener = new PlayerConnectionListener(economyManager);
        getServer().getPluginManager().registerEvents(connectionListener, this);

        // cobre quem ja esta online num /reload, que nao dispara PlayerJoinEvent de novo
        for (Player player : Bukkit.getOnlinePlayers()) {
            connectionListener.handle(player);
        }

        AlkaEconomyCommand alkaEconomyCommand = new AlkaEconomyCommand(this, economyManager, currencyRegistry, api.getMessages());
        getCommand("alkaeconomy").setExecutor(alkaEconomyCommand);
        getCommand("alkaeconomy").setTabCompleter(alkaEconomyCommand);

        PlayerEconomyCommand playerEconomyCommand = new PlayerEconomyCommand(this, economyManager, api.getMessages());
        getCommand("saldo").setExecutor(playerEconomyCommand);
        getCommand("topmoedas").setExecutor(playerEconomyCommand);

        if (getServer().getPluginManager().getPlugin("Vault") != null) {
            getServer().getServicesManager().register(
                    Economy.class, new VaultHook(economyManager, currencyRegistry), this, ServicePriority.Highest);
            getLogger().info("Vault Hook registrado com sucesso.");
        } else {
            getLogger().warning("Vault nao encontrado - o hook de economia ficara indisponivel.");
        }

        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new EconomyExpansion(economyManager, currencyRegistry).register();
            getLogger().info("Expansao do PlaceholderAPI registrada.");
        }

        getLogger().info("AlkaEconomy habilitado (" + currencyRegistry.getAll().size() + " moedas carregadas).");
    }

    @Override
    protected void onPluginDisable() {
        if (economyManager != null) {
            economyManager.saveAll();
        }
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    public CurrencyRegistry getCurrencyRegistry() {
        return currencyRegistry;
    }
}
