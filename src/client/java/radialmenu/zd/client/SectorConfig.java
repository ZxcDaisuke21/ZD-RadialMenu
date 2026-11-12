package radialmenu.zd.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class SectorConfig {
    public static final Logger LOGGER = LoggerFactory.getLogger("zdradialmenu");

    public static class Sector {
        public String name = "";
        public String command = "";
        public String icon = "";
        public String type = "command";
        public String keyBinding = "";
        public Sector[] children = new Sector[0];
    }

    public static class CustomCell {
        public int dx = 0;
        public int dy = 0;
        public int w = 56;
        public int h = 48;
        public int color = 0x60808080;
        public String shape = "rect"; // rect | hex | disc
    }

    public static class ConfigFile {
        public String skin = "CLASSIC";
        public int sectorCount = 6;
        public Sector[] sectors = new Sector[6];
        public CustomCell[] customLayout = new CustomCell[0];
    }

    public static Sector[] sectors = new Sector[6];
    public static int sectorCount = 6;
    public static String skin = "CLASSIC";
    public static CustomCell[] customLayout = new CustomCell[0];

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String, ItemStack> ICON_CACHE = new HashMap<>();

    static {
        for (int i = 0; i < sectors.length; i++) {
            sectors[i] = new Sector();
        }
    }

    public static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("zdradialmenu.json");
    }

    public static void load() {
        sectorCount = 6;
        sectors = new Sector[6];
        for (int i = 0; i < sectors.length; i++) {
            sectors[i] = new Sector();
        }
        skin = "CLASSIC";
        customLayout = new CustomCell[0];
        ICON_CACHE.clear();
        try {
            Path p = path();
            LOGGER.info("[ZDRM] load: читаю {}", p);
            if (Files.exists(p)) {
                String text = Files.readString(p);
                boolean loaded = false;
                try {
                    ConfigFile cf = GSON.fromJson(text, ConfigFile.class);
                    if (cf != null && cf.sectors != null) {
                        skin = cf.skin == null ? "CLASSIC" : cf.skin;
                        customLayout = cf.customLayout == null ? new CustomCell[0] : cf.customLayout;
                        for (int i = 0; i < customLayout.length; i++) {
                            if (customLayout[i] == null) customLayout[i] = new CustomCell();
                        }
                        int n = cf.sectorCount;
                        if (n < 1 || n > 10) {
                            n = Math.min(10, Math.max(1, cf.sectors.length));
                        }
                        sectorCount = n;
                        sectors = new Sector[n];
                        for (int i = 0; i < n; i++) {
                            sectors[i] = i < cf.sectors.length && cf.sectors[i] != null
                                    ? cf.sectors[i] : new Sector();
                        }
                        for (int i = 0; i < sectors.length; i++) {
                            if (sectors[i].children == null) sectors[i].children = new Sector[0];
                            if (sectors[i].icon == null) sectors[i].icon = "";
                        }
                        loaded = true;
                    }
                } catch (Exception ignored) {
                }
                if (!loaded) {
                    Sector[] arr = GSON.fromJson(text, Sector[].class);
                    if (arr != null) {
                        sectorCount = Math.min(10, Math.max(1, arr.length));
                        sectors = new Sector[sectorCount];
                        for (int i = 0; i < sectorCount; i++) {
                            sectors[i] = i < arr.length && arr[i] != null ? arr[i] : new Sector();
                        }
                        applyLogging();
                    }
                } else {
                    applyLogging();
                }
            } else {
                LOGGER.info("[ZDRM] load: файла нет, стартую с пустыми слотами");
            }
        } catch (Exception e) {
            LOGGER.error("[ZDRM] load: ошибка", e);
        }
        LOGGER.info("[ZDRM] load: skin='{}', слотов={}, customCells={}",
                skin, sectorCount, customLayout.length);
    }

    private static void applyLogging() {
        for (int i = 0; i < sectors.length; i++) {
            LOGGER.info("[ZDRM] load: слот {} type='{}' icon='{}' name='{}'",
                    i, sectors[i].type, sectors[i].icon, sectors[i].name);
        }
    }

    public static void save() {
        try {
            Files.createDirectories(path().getParent());
            ConfigFile cf = new ConfigFile();
            cf.skin = skin;
            cf.sectorCount = sectorCount;
            cf.sectors = sectors;
            cf.customLayout = customLayout;
            Files.writeString(path(), GSON.toJson(cf));
            LOGGER.info("[ZDRM] save: skin='{}', слотов={}, customCells={}",
                    skin, sectorCount, customLayout.length);
            ICON_CACHE.clear();
        } catch (Exception e) {
            LOGGER.error("[ZDRM] save: ошибка", e);
        }
    }

    // меняет число слотов основного меню (1..10), сохраняя содержимое
    public static void setSectorCount(int n) {
        n = Math.max(1, Math.min(10, n));
        if (n == sectors.length) return;
        Sector[] arr = new Sector[n];
        for (int i = 0; i < n; i++) {
            arr[i] = i < sectors.length ? sectors[i] : new Sector();
        }
        sectors = arr;
        sectorCount = n;
        ensureCustomCells();
        LOGGER.info("[ZDRM] слотов стало {}", n);
    }

    public static void ensureCustomCells() {
        if (customLayout == null) customLayout = new CustomCell[0];
        if (customLayout.length != sectors.length) {
            CustomCell[] arr = new CustomCell[sectors.length];
            for (int i = 0; i < arr.length; i++) {
                arr[i] = i < customLayout.length ? customLayout[i]
                        : MenuSkin.defaultCell(i, sectors.length);
            }
            customLayout = arr;
        }
    }

    public static ItemStack iconStack(Sector s) {
        if (s == null) return ItemStack.EMPTY;
        String icon = s.icon == null ? "" : s.icon.trim();
        if (icon.isEmpty()) return ItemStack.EMPTY;

        ItemStack cached = ICON_CACHE.get(icon);
        if (cached != null) return cached;

        ItemStack result;
        try {
            Identifier id = Identifier.of(icon);
            Item item = Registries.ITEM.get(id);
            if (id.equals(Registries.ITEM.getId(item))) {
                result = new ItemStack(item);
            } else {
                LOGGER.info("[ZDRM] iconStack: '{}' в реестре НЕ найден", icon);
                result = ItemStack.EMPTY;
            }
        } catch (Exception e) {
            LOGGER.info("[ZDRM] iconStack: '{}' не парсится: {}", icon, e.toString());
            result = ItemStack.EMPTY;
        }
        ICON_CACHE.put(icon, result);
        return result;
    }
}
