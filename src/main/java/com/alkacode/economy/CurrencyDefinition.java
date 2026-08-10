package com.alkacode.economy;

import org.bukkit.Material;

/**
 * Definicao de uma moeda carregada do config.yml (secao {@code currencies}). Ao
 * contrario do antigo enum CurrencyType, novas moedas nao exigem recompilar o
 * plugin - basta adicionar uma entrada no config e rodar {@code /alkaeconomy reload}
 * (ou usar {@code /alkaeconomy create}).
 */
public record CurrencyDefinition(
        String id,
        String name,
        String symbol,
        double defaultBalance,
        String formatPattern,
        boolean vaultEquivalent,
        Material icon
) {
}
