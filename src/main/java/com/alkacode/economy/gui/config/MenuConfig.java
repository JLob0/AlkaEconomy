package com.alkacode.economy.gui.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * Centraliza titulos/nomes/lores/layout das GUIs do AlkaEconomy (menus.yml +
 * gui-layouts.yml) - mesmo mecanismo do AlkaEssentials/AlkaFish, so que combinado
 * numa unica classe (instancia unica via {@link #getInstance()}, setada no
 * onPluginEnable) porque os construtores das GUIs recebem JavaPlugin generico,
 * nao a subclasse do plugin.
 */
public final class MenuConfig {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static MenuConfig instance;

    public static MenuConfig getInstance() {
        return instance;
    }

    public static void init(JavaPlugin plugin) {
        instance = new MenuConfig(plugin);
    }

    private final JavaPlugin plugin;
    private final File menusFile;
    private YamlConfiguration menus;
    private final Map<String, GuiLayout> guiLayouts = new HashMap<>();

    private MenuConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        this.menusFile = new File(plugin.getDataFolder(), "menus.yml");
        reload();
    }

    public void reload() {
        loadMenus();
        loadGuiLayouts();
    }

    private void loadMenus() {
        if (!menusFile.exists()) {
            try {
                plugin.saveResource("menus.yml", false);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "menus.yml nao encontrado no jar - usando vazio.", e);
                menus = new YamlConfiguration();
                return;
            }
        }
        menus = YamlConfiguration.loadConfiguration(menusFile);
        mergeMissingDefaults();
    }

    /** Adiciona chaves novas do menus.yml do jar ao arquivo salvo (migracao de versao). */
    private void mergeMissingDefaults() {
        try (InputStream in = plugin.getResource("menus.yml")) {
            if (in == null) {
                return;
            }
            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(in, StandardCharsets.UTF_8));
            boolean changed = false;
            for (String key : defaults.getKeys(true)) {
                if (!menus.contains(key)) {
                    menus.set(key, defaults.get(key));
                    changed = true;
                }
            }
            if (changed) {
                menus.save(menusFile);
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Falha ao migrar menus.yml", e);
        }
    }

    private void loadGuiLayouts() {
        File file = new File(plugin.getDataFolder(), "gui-layouts.yml");
        if (!file.exists()) {
            plugin.saveResource("gui-layouts.yml", false);
        }
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        guiLayouts.clear();
        for (String key : cfg.getKeys(false)) {
            int rows = cfg.getInt(key + ".rows", 3);
            List<String> lines = cfg.getStringList(key + ".layout");
            guiLayouts.put(key, new GuiLayout(rows, lines.toArray(new String[0])));
        }
    }

    public GuiLayout layout(String key) {
        GuiLayout found = guiLayouts.get(key);
        if (found == null) {
            throw new IllegalStateException("Layout '" + key + "' nao encontrado em gui-layouts.yml");
        }
        return found;
    }

    public record GuiLayout(int rows, String[] layout) {
        public List<Integer> findSlots(char c) {
            List<Integer> slots = new ArrayList<>();
            for (int row = 0; row < layout.length; row++) {
                String line = layout[row];
                for (int col = 0; col < line.length() && col < 9; col++) {
                    if (line.charAt(col) == c) {
                        slots.add(row * 9 + col);
                    }
                }
            }
            return slots;
        }

        public int firstSlot(char c) {
            List<Integer> slots = findSlots(c);
            return slots.isEmpty() ? -1 : slots.get(0);
        }
    }

    /** Titulo (menus.yml.&lt;path&gt;.title) com placeholders. */
    public String title(String path, Map<String, String> placeholders) {
        return apply(menus.getString(path + ".title", ""), placeholders);
    }

    /** Texto avulso (menus.yml.&lt;path&gt;) com placeholders - pra rotulos/lore que o
     * Java monta dinamicamente (ex: nome de moeda + saldo, medalha do top). */
    public String text(String path, Map<String, String> placeholders) {
        return apply(menus.getString(path, ""), placeholders);
    }

    /** Lista de textos (menus.yml.&lt;path&gt;) com placeholders. */
    public List<String> textList(String path, Map<String, String> placeholders) {
        List<String> out = new ArrayList<>();
        for (String line : menus.getStringList(path)) {
            out.add(apply(line, placeholders));
        }
        return out;
    }

    /** Item (material/name de menus.yml.&lt;path&gt;) com placeholders. */
    public ItemStack item(String path, Map<String, String> placeholders) {
        ConfigurationSection section = menus.getConfigurationSection(path);
        Material material = section != null ? Material.matchMaterial(section.getString("material", "STONE")) : null;
        if (material == null) {
            material = Material.STONE;
        }
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        String name = section != null ? apply(section.getString("name", ""), placeholders) : "";
        if (!name.isEmpty()) {
            meta.displayName(MM.deserialize(name));
        }
        if (section != null && section.contains("lore")) {
            List<Component> lore = new ArrayList<>();
            for (String line : section.getStringList("lore")) {
                lore.add(MM.deserialize(apply(line, placeholders)));
            }
            meta.lore(lore);
        }
        item.setItemMeta(meta);
        return item;
    }

    private static String apply(String text, Map<String, String> placeholders) {
        if (text == null) {
            return "";
        }
        if (placeholders == null || placeholders.isEmpty()) {
            return text;
        }
        String result = text;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }
}
