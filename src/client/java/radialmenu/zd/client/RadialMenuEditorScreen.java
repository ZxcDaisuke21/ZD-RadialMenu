package radialmenu.zd.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class RadialMenuEditorScreen extends Screen {
    // известные идентификаторы привязок — те же строки, что понимает KeyBinding.byId
    private static final String[] KEY_IDS = {
            "key.forward", "key.back", "key.left", "key.right",
            "key.jump", "key.sneak", "key.sprint",
            "key.attack", "key.use", "key.pickItem",
            "key.inventory", "key.chat", "key.drop", "key.swapHands",
            "key.hotbar.1", "key.hotbar.2", "key.hotbar.3", "key.hotbar.4",
            "key.hotbar.5", "key.hotbar.6", "key.hotbar.7", "key.hotbar.8", "key.hotbar.9",
            "key.advancements", "key.playerlist", "key.socialInteractions",
            "key.saveToolbarActivator", "key.loadToolbarActivator"
    };

    // редактируемый уровень; при изменении числа слотов пересоздаётся
    private SectorConfig.Sector[] level;
    // сектор-владелец массива (для подменю); null — редактируем корень
    private final SectorConfig.Sector parentSector;
    private final RadialMenuEditorScreen parentScreen;

    // -1 — список слотов, иначе редактирование слота с этим индексом
    private int editing;

    private TextFieldWidget nameField;
    private TextFieldWidget cmdField;
    private TextFieldWidget iconField;

    // кнопки рисуем сами — без наследования от виджетов
    private final List<ZdFlatButton> buttons = new ArrayList<>();

    public RadialMenuEditorScreen() {
        this(SectorConfig.sectors, null, -1, null);
    }

    public RadialMenuEditorScreen(SectorConfig.Sector[] level, RadialMenuEditorScreen parent,
                                  int editing, SectorConfig.Sector parentSector) {
        super(Text.literal(parentSector == null
                ? "Редактор радиального меню" : "Редактор подменю"));
        this.level = level;
        this.parentScreen = parent;
        this.editing = editing;
        this.parentSector = parentSector;
    }

    public static void openAt(int slotIndex) {
        MinecraftClient.getInstance().setScreen(
                new RadialMenuEditorScreen(SectorConfig.sectors, null, slotIndex, null));
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private void zdBtn(String label, int x, int y, int w, int h, Runnable action) {
        buttons.add(new ZdFlatButton(label, x, y, w, h, action));
    }

    // меняет число слотов текущего уровня: для корня — глобально, для подменю — локально
    private void setLevelCount(int n) {
        n = Math.max(0, Math.min(10, n));
        if (parentSector == null) {
            SectorConfig.setSectorCount(n);
            level = SectorConfig.sectors;
        } else {
            SectorConfig.Sector[] arr = new SectorConfig.Sector[n];
            for (int i = 0; i < n; i++) {
                arr[i] = i < level.length ? level[i] : new SectorConfig.Sector();
            }
            parentSector.children = arr;
            level = arr;
        }
        SectorConfig.save();
        clearAndInit();
    }

    @Override
    public void init() {
        buttons.clear();
        int x = width / 2 - 155;

        if (editing >= 0 && editing < level.length) {
            initSlotEditor(x);
            return;
        }

        // ---------- список слотов ----------
        zdBtn("← Назад", x, height - 30, 100, 20, this::back);
        zdBtn("Готово", x + 210, height - 30, 100, 20, this::closeAll);

        int listTop = 56;
        for (int i = 0; i < level.length; i++) {
            int yy = listTop + i * 24;
            if (yy > height - 160) break;
            String label = level[i].name.isEmpty()
                    ? "Слот " + (i + 1) + " — пусто"
                    : "Слот " + (i + 1) + ": " + level[i].name;
            int idx = i;
            zdBtn(label, x, yy, 310, 20, () -> {
                editing = idx;
                clearAndInit();
            });
        }

        // счётчик слотов текущего уровня (корень: глобально, подменю: локально)
        int shown = parentSector == null ? SectorConfig.sectorCount : level.length;
        zdBtn("−", x, height - 58, 100, 20, () -> setLevelCount(shown - 1));
        zdBtn("Слотов: " + shown, x + 105, height - 58, 100, 20, () -> { });
        zdBtn("＋", x + 210, height - 58, 105, 20, () -> setLevelCount(shown + 1));

        if (parentSector == null) {
            zdBtn("Вид кнопок: " + MenuSkin.current().label, x, height - 110, 310, 20, () -> {
                MenuSkin[] all = MenuSkin.values();
                MenuSkin next = all[(MenuSkin.current().ordinal() + 1) % all.length];
                SectorConfig.skin = next.name();
                SectorConfig.save();
                clearAndInit();
            });

            zdBtn("Настроить вид «Свой»", x, height - 84, 310, 20, RadialLayoutEditorScreen::open);
        }
    }

    private void initSlotEditor(int x) {
        SectorConfig.Sector s = level[editing];

        zdBtn("← К списку", x, height - 30, 150, 20, () -> {
            editing = -1;
            clearAndInit();
        });
        zdBtn("Готово", x + 160, height - 30, 150, 20, this::closeAll);

        nameField = addField(x, 66, s.name, v -> s.name = v, 40);
        cmdField = addField(x, 106, s.command, v -> s.command = v, 200);
        iconField = addField(x, 146, s.icon, v -> {
            s.icon = v;
            SectorConfig.LOGGER.info("[ZDRM] иконка слота {} = '{}'", editing, v);
        }, 100);

        zdBtn("Тип: " + typeLabel(s.type), x, 186, 310, 20, () -> {
            s.type = nextType(s.type);
            if ("submenu".equals(s.type) && s.children == null) {
                s.children = new SectorConfig.Sector[0];
            }
            SectorConfig.save();
            clearAndInit();
        });

        if ("key".equals(s.type)) {
            zdBtn("‹", x, 226, 40, 20, () -> cycleKey(s, -1));
            zdBtn("Клавиша: " + keyLabel(s.keyBinding), x + 45, 226, 220, 20, () -> { });
            zdBtn("›", x + 270, 226, 40, 20, () -> cycleKey(s, 1));
        }

        if ("submenu".equals(s.type)) {
            int childCount = s.children == null ? 0 : s.children.length;
            zdBtn("Подменю: слотов " + childCount + " — редактировать", x, 226, 310, 20, () -> {
                if (s.children == null) s.children = new SectorConfig.Sector[0];
                MinecraftClient.getInstance().setScreen(
                        new RadialMenuEditorScreen(s.children, this, -1, s));
            });
        }
    }

    private void cycleKey(SectorConfig.Sector s, int dir) {
        int n = KEY_IDS.length;
        int cur = findKeyIndex(s.keyBinding);
        if (cur < 0) cur = dir > 0 ? -1 : 0;
        int next = (cur + dir + n) % n;
        s.keyBinding = KEY_IDS[next];
        SectorConfig.save();
        clearAndInit();
    }

    // ---------- утилиты ----------

    private void back() {
        if (parentScreen != null) {
            MinecraftClient.getInstance().setScreen(parentScreen);
        } else {
            close();
        }
    }

    private void closeAll() {
        SectorConfig.save();
        MinecraftClient.getInstance().setScreen(null);
    }

    private TextFieldWidget addField(int x, int y, String value,
                                     java.util.function.Consumer<String> setter, int maxLen) {
        TextFieldWidget f = new TextFieldWidget(MinecraftClient.getInstance().textRenderer,
                x, y, 310, 20, Text.literal(""));
        f.setMaxLength(maxLen);
        f.setText(value == null ? "" : value);
        f.setChangedListener(setter::accept);
        addDrawableChild(f);
        return f;
    }

    private static String typeLabel(String t) {
        return switch (t == null ? "command" : t) {
            case "key" -> "клавиша";
            case "submenu" -> "подменю";
            default -> "команда";
        };
    }

    private static String nextType(String t) {
        return switch (t == null ? "command" : t) {
            case "command" -> "key";
            case "key" -> "submenu";
            default -> "command";
        };
    }

    private static int findKeyIndex(String id) {
        if (id == null || id.isEmpty()) return -1;
        for (int i = 0; i < KEY_IDS.length; i++) {
            if (id.equals(KEY_IDS[i])) return i;
        }
        return -1;
    }

    private static String keyLabel(String id) {
        if (id == null || id.isEmpty()) return "не выбрана";
        return Text.translatable(id).getString();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        int x = width / 2 - 155;

        if (editing >= 0 && editing < level.length) {
            context.drawCenteredTextWithShadow(textRenderer,
                    Text.literal("Слот " + (editing + 1)), width / 2, 8, 0xFFFFFFFF);
            context.drawTextWithShadow(textRenderer, Text.literal("Название:"), x, 56, 0xFFA0A0A0);
            context.drawTextWithShadow(textRenderer,
                    Text.literal("key".equals(level[editing].type)
                            ? "Привязка выбирается кнопками ниже"
                            : "Команда/чат (с «/» — команда):"),
                    x, 96, 0xFFA0A0A0);
            context.drawTextWithShadow(textRenderer,
                    Text.literal("Иконка (id предмета, напр. minecraft:diamond):"),
                    x, 136, 0xFFA0A0A0);
        } else {
            context.drawCenteredTextWithShadow(textRenderer, getTitle(), width / 2, 8, 0xFFFFFFFF);

            String hint = level.length == 0
                    ? "меню пустое — нажмите ＋, чтобы добавить слоты"
                    : "клик по слоту — настроить его";
            context.drawCenteredTextWithShadow(textRenderer,
                    Text.literal(hint), width / 2, height - 134, 0xFFFFFF55);

            if (parentSector == null) {
                // превью выбранного вида — поднято выше, чтобы не задевать подсказку и кнопки
                MenuSkin sk = MenuSkin.current();
                int pcx = width / 2 - 16;
                int pcy = height - 187;
                sk.drawAt(context, 0, 3, pcx, pcy, 26, 0x60808080);
                sk.drawAt(context, 1, 3, pcx, pcy, 26, 0x80FFFFFF);
                sk.drawAt(context, 2, 3, pcx, pcy, 26, 0x60404040);
            }
        }

        for (ZdFlatButton b : buttons) {
            b.render(context, textRenderer, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        int mx = (int) click.x();
        int my = (int) click.y();

        for (ZdFlatButton b : buttons) {
            if (b.contains(mx, my)) {
                b.click();
                return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public void removed() {
        super.removed();
        SectorConfig.save();
    }

    @Override
    public void close() {
        if (parentScreen != null) {
            MinecraftClient.getInstance().setScreen(parentScreen);
        } else {
            SectorConfig.save();
            super.close();
        }
    }
}
