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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
        public String shape = "rect"; // rect | hex | disc | diamond | triangle
        public boolean[][] shapeMask; // своя маска из старых конфигов; null = нет
        public int shapeRef = 0;      // 0 — стандартная фигура, >0 — id формы из библиотеки
    }

    // форма из библиотеки: рисуется один раз, назначается любым клеткам
    public static class CustomShape {
        public int id = 0;
        public boolean[][] mask;
    }

    public static class ConfigFile {
        public String skin = "CLASSIC";
        public String submenuMode = "OVERLAY";
        // звуки наведения/выбора/открытия подменю
        public boolean soundsEnabled = true;
        // пустые клетки: true — показывать номер слота, false — рисовать пустыми
        public boolean emptyNumbers = true;
        public int sectorCount = 6;
        public Sector[] sectors = new Sector[6];
        public CustomCell[] customLayout = new CustomCell[0];
        // пользовательская палитра редактора вида «Свой» (12 цветов AARRGGBB)
        public int[] customPalette;
        // библиотека кастомных форм
        public List<CustomShape> customShapes;
    }

    public static Sector[] sectors = new Sector[6];
    public static int sectorCount = 6;
    public static String skin = "CLASSIC";
    public static String submenuMode = "OVERLAY";
    public static boolean soundsEnabled = true;
    public static boolean emptyNumbers = true;
    public static CustomCell[] customLayout = new CustomCell[0];
    public static int[] customPalette = null;
    public static List<CustomShape> customShapes = new ArrayList<>();

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

    // возвращает палитру из 12 цветов, всегда валидируя её:
    // если в конфиге мусор или не хватает цветов — добираем стандартными
    public static int[] palette() {
        if (customPalette == null || customPalette.length != 12) {
            customPalette = new int[12];
        }
        for (int i = 0; i < 12; i++) {
            if (i < MenuSkin.SWATCHES.length) {
                if (customPalette[i] == 0) customPalette[i] = MenuSkin.SWATCHES[i];
            } else if (customPalette[i] == 0) {
                customPalette[i] = 0xFF808080;
            }
        }
        return customPalette;
    }

    public static void load() {
        sectorCount = 6;
        sectors = new Sector[6];
        for (int i = 0; i < sectors.length; i++) {
            sectors[i] = new Sector();
        }
        skin = "CLASSIC";
        submenuMode = "OVERLAY";
        soundsEnabled = true;
        emptyNumbers = true;
        customLayout = new CustomCell[0];
        customPalette = null;
        customShapes = new ArrayList<>();
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
                        submenuMode = cf.submenuMode == null || cf.submenuMode.isEmpty()
                                ? "OVERLAY" : cf.submenuMode;
                        // режимы подменю есть только у «Сегментов»: поверх или отдельно;
                        // на остальных видах — всегда «поверх»
                        if (!"DONUT".equals(skin)) {
                            submenuMode = "OVERLAY";
                        } else if (!"OVERLAY".equals(submenuMode) && !"SEPARATE".equals(submenuMode)) {
                            submenuMode = "OVERLAY";
                        }
                        soundsEnabled = cf.soundsEnabled;
                        emptyNumbers = cf.emptyNumbers;
                        customLayout = cf.customLayout == null ? new CustomCell[0] : cf.customLayout;
                        for (int i = 0; i < customLayout.length; i++) {
                            if (customLayout[i] == null) customLayout[i] = new CustomCell();
                            if (customLayout[i].shapeRef < 0) customLayout[i].shapeRef = 0;
                        }
                        customPalette = cf.customPalette;
                        customShapes = cf.customShapes == null ? new ArrayList<>() : cf.customShapes;
                        // валидация форм: без маски или с битой маской форма не пригодна
                        for (int i = customShapes.size() - 1; i >= 0; i--) {
                            CustomShape s = customShapes.get(i);
                            if (s == null || s.mask == null || s.mask.length < 4 || s.mask.length > 64
                                    || s.mask[0] == null || s.mask.length != s.mask[0].length) {
                                customShapes.remove(i);
                            }

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
        LOGGER.info("[ZDRM] load: skin='{}', submenuMode='{}', sounds={}, numbers={}, слотов={}, customCells={}, shapes={}",
                skin, submenuMode, soundsEnabled, emptyNumbers, sectorCount, customLayout.length, customShapes.size());
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
            cf.submenuMode = submenuMode;
            cf.soundsEnabled = soundsEnabled;
            cf.emptyNumbers = emptyNumbers;
            cf.sectorCount = sectorCount;
            cf.sectors = sectors;
            cf.customLayout = customLayout;
            cf.customPalette = customPalette;
            cf.customShapes = customShapes;
            Files.writeString(path(), GSON.toJson(cf));
            LOGGER.info("[ZDRM] save: skin='{}', submenuMode='{}', sounds={}, numbers={}, слотов={}, customCells={}, shapes={}",
                    skin, submenuMode, soundsEnabled, emptyNumbers, sectorCount, customLayout.length, customShapes.size());
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

    // добавить клетку в конец раскладки «Свой»
    public static void addCustomCell() {
        ensureCustomCells();
        CustomCell[] old = customLayout;
        CustomCell[] next = new CustomCell[old.length + 1];
        System.arraycopy(old, 0, next, 0, old.length);
        next[old.length] = MenuSkin.defaultCell(old.length, old.length + 1);
        customLayout = next;
    }

    // убрать клетку по индексу; последнюю клетку не убираем
    public static void removeCustomCell(int index) {
        ensureCustomCells();
        if (customLayout.length <= 1) return;
        int idx = Math.max(0, Math.min(index, customLayout.length - 1));
        CustomCell[] next = new CustomCell[customLayout.length - 1];
        for (int i = 0, j = 0; i < customLayout.length; i++) {
            if (i != idx) next[j++] = customLayout[i];
        }
        customLayout = next;
    }

    // сброс раскладки к дефолтным клеткам (количество сохраняется)
    public static void resetCustomLayout() {
        int n = customLayout == null ? 6 : customLayout.length;
        CustomCell[] next = new CustomCell[n];
        for (int i = 0; i < n; i++) {
            next[i] = MenuSkin.defaultCell(i, n);
        }
        customLayout = next;
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

    // ---- библиотека кастомных форм ----

    public static void ensureCustomShapes() {
        if (customShapes == null) customShapes = new ArrayList<>();
    }

    public static CustomShape addCustomShape(int id, boolean[][] mask) {
        ensureCustomShapes();
        CustomShape s = new CustomShape();
        s.id = id;
        s.mask = mask;
        customShapes.add(s);
        save();
        return s;
    }

    public static void removeCustomShape(int index) {
        ensureCustomShapes();
        if (index < 0 || index >= customShapes.size()) return;
        int removedId = customShapes.get(index).id;
        customShapes.remove(index);
        // клетки, ссылающиеся на удалённую форму, возвращаются к стандартной
        if (customLayout != null) {
            for (CustomCell c : customLayout) {
                if (c != null && c.shapeRef == removedId) c.shapeRef = 0;
            }
        }
        save();
    }

    public static CustomShape findShape(int id) {
        ensureCustomShapes();
        for (CustomShape s : customShapes) {
            if (s.id == id) return s;
        }
        return null;
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
