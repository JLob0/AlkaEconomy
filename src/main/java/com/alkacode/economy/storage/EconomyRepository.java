package com.alkacode.economy.storage;

import com.alkacode.core.api.DatabaseProvider;
import com.alkacode.core.database.AbstractRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Persistencia do saldo por (jogador, moeda) via {@link DatabaseProvider} do
 * AlkaCore (HikariCP, SQLite ou MySQL conforme o config.yml do Core) - substitui
 * o antigo EconomySQLiteStore, que abria sua propria Connection JDBC e serializava
 * tudo num executor single-thread proprio.
 */
public final class EconomyRepository extends AbstractRepository {

    private final Logger logger;

    public EconomyRepository(DatabaseProvider db, Logger logger) {
        super(db);
        this.logger = logger;
        createTable();
    }

    private void createTable() {
        try (Connection conn = db.getConnection(); var stmt = conn.createStatement()) {
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS alkaeconomy_balances (
                        player_uuid VARCHAR(36) NOT NULL,
                        currency_id VARCHAR(32) NOT NULL,
                        balance DOUBLE NOT NULL DEFAULT 0,
                        PRIMARY KEY (player_uuid, currency_id)
                    )
                    """);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erro ao criar tabela alkaeconomy_balances", e);
        }
    }

    /** Traz so as moedas que o jogador ja tem linha gravada - o chamador preenche o resto com os defaults do registry. */
    public Map<String, Double> loadBalancesSync(UUID uuid) {
        Map<String, Double> balances = new HashMap<>();
        String sql = "SELECT currency_id, balance FROM alkaeconomy_balances WHERE player_uuid = ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    balances.put(rs.getString("currency_id"), rs.getDouble("balance"));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erro ao carregar saldos de " + uuid, e);
        }
        return balances;
    }

    public record TopBalanceEntry(UUID uuid, double balance) {
    }

    /** Top N jogadores por saldo de uma moeda - so considera quem ja tem linha gravada (nunca teve saldo = fora do ranking). */
    public List<TopBalanceEntry> loadTopBalancesSync(String currencyId, int limit) {
        List<TopBalanceEntry> result = new ArrayList<>();
        String sql = "SELECT player_uuid, balance FROM alkaeconomy_balances WHERE currency_id = ? ORDER BY balance DESC LIMIT ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, currencyId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new TopBalanceEntry(UUID.fromString(rs.getString("player_uuid")), rs.getDouble("balance")));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erro ao carregar top balances de " + currencyId, e);
        }
        return result;
    }

    public void saveBalanceSync(UUID uuid, String currencyId, double balance) {
        String sql = upsert("alkaeconomy_balances",
                new String[]{"player_uuid", "currency_id", "balance"},
                new String[]{"player_uuid", "currency_id"});
        try {
            execute(sql, ps -> {
                ps.setString(1, uuid.toString());
                ps.setString(2, currencyId);
                ps.setDouble(3, balance);
            });
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erro ao salvar saldo de " + uuid + " (" + currencyId + ")", e);
        }
    }
}
