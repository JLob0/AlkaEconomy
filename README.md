<div align="center">

# AlkaEconomy

### O motor de economia multi-moedas da rede Alka*

Moedas dinâmicas, saldo assíncrono e ranking, tudo sobre a infraestrutura
compartilhada do **AlkaCore**.

![Java](https://img.shields.io/badge/Java-21-orange)
![Minecraft](https://img.shields.io/badge/Minecraft-1.21.8-green)
![Version](https://img.shields.io/badge/Version-1.0.7-blue)
![License](https://img.shields.io/badge/License-Proprietary-red)

</div>

---

## 📋 Sobre o Projeto

O **AlkaEconomy** é o coração financeiro da rede `Alka*`: um motor de moedas
central, com moedas **dinâmicas** configuráveis direto no `config.yml` — sem
precisar recompilar nada pra adicionar uma moeda nova. Roda sobre o banco
compartilhado do AlkaCore e se conecta ao Vault pra servir como provider de
economia pra qualquer outro plugin da rede.

## ✨ Funcionalidades Principais

- 💰 **Múltiplas moedas simultâneas** — Gold, Alkarion, Nácar, Escarion,
  Soul, Ticks e Prisma, todas configuráveis de forma independente
  (símbolo, ícone e formatação próprios).
- ⚡ **Operações assíncronas** — consultas e alterações de saldo não travam a
  thread principal do servidor.
- 🏆 **Ranking de moedas** — top 10 jogadores por moeda, direto no jogo.
- 🔌 **Ponte com Vault** — qualquer plugin que fale Vault (lojas, kits,
  rankup) funciona com o AlkaEconomy sem configuração extra.
- 🗄️ **Banco compartilhado** — persistência via AlkaCore, sem conexão de
  banco própria.

## 🎮 Comandos

| Comando | Descrição | Permissão |
| --- | --- | --- |
| `/alkaeconomy <give\|take\|set\|reload\|create\|list>` | Administração completa das moedas | `alkaeconomy.admin` |
| `/saldo` (`/saldos`, `/bal`) | Mostra seu saldo em todas as moedas | — |
| `/topmoedas` (`/ecotop`, `/topeco`) | Abre o ranking (TOP 10) de jogadores por moeda | — |

## 🔗 Integrações

Construído sobre o **AlkaCore** e serve de base para todo o restante do
ecossistema `Alka*` (AlkaShop, AlkaVips, AlkaEnderChest, AlkaFlair e outros).
Ponte opcional com **Vault**, **PlaceholderAPI** e **LuckPerms**.

## 🔧 Tecnologias Utilizadas

- **Java 21** · **Paper API 1.21.8**
- **AlkaCore** (banco de dados e infraestrutura compartilhada)
- **Vault** (ponte de economia)

## ⚙️ Instalação

1. Instale o **AlkaCore** antes (dependência obrigatória).
2. Coloque `AlkaEconomy.jar` na pasta `plugins/` do servidor.
3. Reinicie o servidor.
4. Configure suas moedas em `plugins/AlkaEconomy/config.yml`.

## 🔐 Permissões

| Permissão | Descrição | Padrão |
| --- | --- | --- |
| `alkaeconomy.admin` | Dar/tirar/definir saldo de qualquer moeda e gerenciar moedas | `op` |

## 📝 Licença

> ⚠️ **Projeto proprietário da AlkaStudio.**
>
> Código fonte destinado exclusivamente ao uso interno da rede `Alka*`.
> Reprodução, distribuição ou uso não autorizado não são permitidos.

## 🎯 Créditos

- **Desenvolvido por**: MestreDEV — AlkaStudio
- **Parte do ecossistema**: `Alka*`

---

<div align="center">

**Desenvolvido com ❤️ pela AlkaStudio**

[![AlkaStudio](https://img.shields.io/badge/AlkaStudio-JLob0-blue)](https://github.com/JLob0)

</div>
