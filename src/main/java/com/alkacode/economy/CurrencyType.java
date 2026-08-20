package com.alkacode.economy;

/**
 * Compatibilidade com o enum antigo: moedas agora sao dinamicas (ver
 * {@link CurrencyRegistry}, carregada do config.yml), entao isso deixou de ser um
 * enum e virou uma classe com os ids das 5 moedas legadas como constantes String -
 * plugins que faziam {@code CurrencyType.COINS}/{@code CurrencyType.DRAKONIO} etc
 * continuam compilando sem alterar a chamada, so o tipo declarado ao redor (que
 * antes era {@code CurrencyType}, agora precisa ser {@code String}) e que muda.
 * Sao apenas os ids padrao gerados no config.yml na primeira execucao - nada
 * impede o servidor de renomear/remover essas moedas ou adicionar novas.
 */
public final class CurrencyType {

    public static final String COINS = "gold";
    public static final String DRAKONIO = "alkarion";
    public static final String NACAR = "nacar";
    public static final String ESCARION = "escarion";
    public static final String SOULS = "soul";

    private CurrencyType() {
    }
}
