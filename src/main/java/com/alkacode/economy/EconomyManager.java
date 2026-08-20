package com.alkacode.economy;

import com.alkacode.economy.storage.EconomyRepository;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache local (UUID -> saldo por currencyId) espelhando {@link EconomyRepository}.
 * Toda mutacao atualiza o cache de forma sincrona e agenda a persistencia no
 * scheduler assincrono do Bukkit - sem executor proprio, ja que o
 * {@link com.alkacode.core.api.DatabaseProvider} do AlkaCore (HikariCP) ja cuida
 * de serializar o acesso ao SQLite quando necessario (pool de tamanho 1).
 */
public final class EconomyManager {

    private final Plugin plugin;
    private final EconomyRepository repository;
    private final CurrencyRegistry registry;
    private final Map<UUID, Map<String, Double>> cache = new ConcurrentHashMap<>();

    public EconomyManager(Plugin plugin, EconomyRepository repository, CurrencyRegistry registry) {
        this.plugin = plugin;
        this.repository = repository;
        this.registry = registry;
    }

    public void loadForJoin(Player player) {
        loadForJoin(player.getUniqueId());
    }

    public void loadForJoin(UUID uuid) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> cache.put(uuid, mergedWithDefaults(repository.loadBalancesSync(uuid))));
    }

    public void unload(UUID uuid) {
        cache.remove(uuid);
    }

    /** Chamado no onDisable - flush sincrono, o scheduler assincrono do Bukkit para de aceitar tarefas nesse ponto. */
    public void saveAll() {
        for (Map.Entry<UUID, Map<String, Double>> player : cache.entrySet()) {
            for (Map.Entry<String, Double> balance : player.getValue().entrySet()) {
                repository.saveBalanceSync(player.getKey(), balance.getKey(), balance.getValue());
            }
        }
    }

    private Map<String, Double> mergedWithDefaults(Map<String, Double> loaded) {
        Map<String, Double> merged = new ConcurrentHashMap<>();
        for (CurrencyDefinition currency : registry.getAll()) {
            merged.put(currency.id(), loaded.getOrDefault(currency.id(), currency.defaultBalance()));
        }
        return merged;
    }

    /**
     * Fallback sincrono para uuids fora do cache (jogador offline consultado por outro
     * plugin, comando de console, etc). Bloqueia a thread chamadora ate a query retornar -
     * mesmo comportamento do EconomyManager antigo.
     */
    private Map<String, Double> balancesOf(UUID uuid) {
        return cache.computeIfAbsent(uuid, id -> mergedWithDefaults(repository.loadBalancesSync(id)));
    }

    public double getBalance(UUID uuid, String currencyId) {
        String id = normalize(currencyId);
        Map<String, Double> balances = balancesOf(uuid);
        Double balance = balances.get(id);
        if (balance != null) {
            return balance;
        }
        return registry.get(id).map(CurrencyDefinition::defaultBalance).orElse(0.0);
    }

    /** Saldo da moeda marcada vault-equivalent: true - usada por integracoes que so conhecem um saldo "principal" (ex: Vault). */
    public double getBalance(UUID uuid) {
        return registry.getVaultEquivalent().map(currency -> getBalance(uuid, currency.id())).orElse(0.0);
    }

    public boolean has(UUID uuid, String currencyId, double amount) {
        return getBalance(uuid, currencyId) >= amount;
    }

    public void setBalance(UUID uuid, String currencyId, double amount) {
        String id = normalize(currencyId);
        if (!registry.isValid(id)) {
            plugin.getLogger().warning("Tentativa de alterar saldo de moeda desconhecida '" + id + "' - ignorado.");
            return;
        }
        double clamped = Math.max(0.0, amount);
        balancesOf(uuid).put(id, clamped);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> repository.saveBalanceSync(uuid, id, clamped));
    }

    public void addBalance(UUID uuid, String currencyId, double amount) {
        setBalance(uuid, currencyId, getBalance(uuid, currencyId) + amount);
    }

    public void removeBalance(UUID uuid, String currencyId, double amount) {
        setBalance(uuid, currencyId, getBalance(uuid, currencyId) - amount);
    }

    public boolean isValidCurrency(String currencyId) {
        return registry.isValid(currencyId);
    }

    public Collection<CurrencyDefinition> getCurrencies() {
        return registry.getAll();
    }

    public List<String> getCurrencyIds() {
        return registry.getAll().stream().map(CurrencyDefinition::id).toList();
    }

    /**
     * Top N saldos de uma moeda - le direto do banco (nao do cache em memoria, que so
     * tem jogadores online), entao reflete jogadores offline tambem. Chamada bloqueante
     * de proposito, mesmo padrao de {@link #balancesOf} para uuid fora do cache -
     * pensada pra telas de TOP/leaderboard de outros plugins, nao pra hot path.
     */
    public List<EconomyRepository.TopBalanceEntry> getTopBalances(String currencyId, int limit) {
        return repository.loadTopBalancesSync(normalize(currencyId), limit);
    }

    private static String normalize(String currencyId) {
        return currencyId == null ? null : currencyId.toLowerCase(Locale.ROOT);
    }

    private static final String[] SUFFIXES = {"", "K", "M", "B", "T"};

    /**
     * Compacta valores altos com sufixo (1500 -> "1.5K", 2500000 -> "2.5M"). Abaixo de
     * mil retorna o numero cru, sem casas decimais quando ele e inteiro. Mantido como
     * metodo estatico (assinatura antiga) porque varios plugins consumidores chamam
     * {@code EconomyManager.formatValue(amount)} diretamente.
     */
    public static String formatValue(double value) {
        double absValue = Math.abs(value);
        if (absValue < 1_000.0) {
            return trimmed(value);
        }
        int suffixIndex = Math.min((int) (Math.log10(absValue) / 3), SUFFIXES.length - 1);
        double shortValue = value / Math.pow(1_000, suffixIndex);
        return trimmed(shortValue) + SUFFIXES[suffixIndex];
    }

    private static String trimmed(double value) {
        if (value == Math.floor(value) && !Double.isInfinite(value)) {
            return String.valueOf((long) value);
        }
        return String.format(Locale.US, "%.1f", value);
    }

    // -------------------------------------------------------------------------
    // Parser de valores com sufixo (50K, 5M, 2B, 1T, Qa, Qi, Sx, Sp, Oc, No, Dc)
    // -------------------------------------------------------------------------

    /** Sufixos aceitos e seus multiplicadores, da menor para a maior. */
    private static final String[][] SUFFIX_TABLE = {
            {"K", "1000.0"},
            {"M", "1e6"},
            {"B", "1e9"},
            {"T", "1e12"},
            {"QA", "1e15"},
            {"QD", "1e15"},
            {"QI", "1e18"},
            {"QN", "1e18"},
            {"SX", "1e21"},
            {"SP", "1e24"},
            {"OC", "1e27"},
            {"NO", "1e30"},
            {"DC", "1e33"},
    };

    /**
     * Converte uma string de valor (ex: "50K", "5M", "2.5B", "5000") num double.
     * Sem sufixo é parse como número puro. Sufixos são case-insensitive.
     * Retorna Optional.empty() se o valor for inválido/negativo.
     */
    public static Optional<Double> parseAmount(String raw) {
        if (raw == null) return Optional.empty();
        String input = raw.trim().toUpperCase(Locale.ROOT);
        if (input.isEmpty()) return Optional.empty();

        // pega o maior sufixo que casa no fim (maior primeiro p/ evitar "M" de "MA")
        String numberPart = input;
        double multiplier = 1.0;
        for (String[] suffix : SUFFIX_TABLE) {
            String suf = suffix[0];
            if (input.endsWith(suf) && input.length() > suf.length()) {
                String prefix = input.substring(0, input.length() - suf.length());
                // garante que o prefixo é um número válido (evita "K5K")
                try {
                    Double.parseDouble(prefix);
                    numberPart = prefix;
                    multiplier = Double.parseDouble(suffix[1]);
                    break;
                } catch (NumberFormatException ignored) {
                    // sufixo não se aplica ao prefixo; tenta o próximo
                }
            }
        }

        try {
            double value = Double.parseDouble(numberPart) * multiplier;
            if (value < 0 || !Double.isFinite(value)) {
                return Optional.empty();
            }
            return Optional.of(value);
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
