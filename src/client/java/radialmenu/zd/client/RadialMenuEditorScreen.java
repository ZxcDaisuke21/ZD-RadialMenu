package radialmenu.zd.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class RadialMenuEditorScreen extends Screen {
    private final Screen parent;
    private SectorConfig.Sector[] level;
    private final SectorConfig.Sector owner;

    private static final int LEFT_W = 210;
    private static final int LEFT_X = 12;
    private static final int PREV_W = 280;

    private static final int COL_SECTOR = 0x60808080;
    private static final int COL_TEXT = 0xFFFFFFFF;
    private static final int COL_HINT = 0xFFA0A0A0;

    private final List<ZdFlatButton> buttons = new ArrayList<>();

    public RadialMenuEditorScreen(Screen parent) {
        this(parent, SectorConfig.sectors, null);
    }

    private RadialMenuEditorScreen(Screen parent, SectorConfig.Sector[] level, SectorConfig.Sector owner) {
        super(Text.translatable("zd.editor.title"));
        this.parent = parent;
        this.level = level;
        this.owner = owner;
    }

    public static void openAt(int index) {
        MinecraftClient.getInstance().setScreen(new RadialMenuEditorScreen(null));
    }

    public static void openSubmenu(SectorConfig.Sector owner) {
        if (owner.children == null || owner.children.length == 0) {
            owner.children = new SectorConfig.Sector[]{new SectorConfig.Sector()};
        }
        MinecraftClient.getInstance().setScreen(
                new RadialMenuEditorScreen(null, owner.children, owner));
    }

    private static String tr(String key, Object... args) {
        return Text.translatable(key, args).getString();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private void zdBtn(String label, int x, int y, int w, int h, Runnable action) {
        buttons.add(new ZdFlatButton(label, x, y, w, h, action));
    }

    @Override
    protected void init() {
        buttons.clear();
        int y = 40;

        for (int i = 0; i < level.length; i++) {
            int idx = i;
            zdBtn(slotLabel(idx), LEFT_X, y, LEFT_W, 20, () ->
                    this.client.setScreen(new SlotEditorScreen(this, level, idx, owner)));
            y += 22;
        }

        y += 8;

        if (owner == null) {
            zdBtn(tr("zd.editor.sounds", on(SectorConfig.soundsEnabled)), LEFT_X, y, LEFT_W, 20, () -> {
                SectorConfig.soundsEnabled = !SectorConfig.soundsEnabled;
                SectorConfig.save();
                Sounds.select();
                rebuild();
            });
            y += 22;
            zdBtn(tr("zd.editor.glow", on(SectorConfig.hoverGlow)), LEFT_X, y, LEFT_W, 20, () -> {
                SectorConfig.hoverGlow = !SectorConfig.hoverGlow;
                SectorConfig.save();
                rebuild();
            });
            y += 22;
            zdBtn(tr("zd.editor.numbers", on(SectorConfig.emptyNumbers)), LEFT_X, y, LEFT_W, 20, () -> {
                SectorConfig.emptyNumbers = !SectorConfig.emptyNumbers;
                SectorConfig.save();
                rebuild();
            });
            y += 24;

            zdBtn(tr("zd.editor.skin", skinLabel()), LEFT_X, y, LEFT_W, 20, () -> {
                SectorConfig.skin = nextSkin().name();
                SectorConfig.save();
                rebuild();
            });
            y += 22;

            zdBtn(tr("zd.editor.submenu_mode", modeLabel()), LEFT_X, y, LEFT_W, 20, () -> {
                SectorConfig.submenuMode = nextMode();
                SectorConfig.save();
                rebuild();
            });
            y += 24;

            zdBtn(tr("zd.editor.custom_layout"), LEFT_X, y, LEFT_W, 20,
                    () -> this.client.setScreen(new RadialLayoutEditorScreen()));
            y += 26;

            zdBtn("−", LEFT_X, y, 60, 20, () -> {
                SectorConfig.setSectorCount(SectorConfig.sectorCount - 1);
                SectorConfig.save();
                level = SectorConfig.sectors;
                rebuild();
            });
            zdBtn(tr("zd.editor.count", SectorConfig.sectorCount), LEFT_X + 65, y, LEFT_W - 130, 20, () -> { });
            zdBtn("+", LEFT_X + LEFT_W - 60, y, 60, 20, () -> {
                SectorConfig.setSectorCount(SectorConfig.sectorCount + 1);
                SectorConfig.save();
                level = SectorConfig.sectors;
                rebuild();
            });
        } else {
            zdBtn("−", LEFT_X, y, 60, 20, () -> {
                owner.children = resize(owner.children, owner.children.length - 1);
                level = owner.children;
                rebuild();
            });
            zdBtn(tr("zd.editor.count", owner.children.length), LEFT_X + 65, y, LEFT_W - 130, 20, () -> { });
            zdBtn("+", LEFT_X + LEFT_W - 60, y, 60, 20, () -> {
                owner.children = resize(owner.children, owner.children.length + 1);
                level = owner.children;
                rebuild();
            });
        }

        zdBtn(tr("zd.back"), LEFT_X, this.height - 26, 100, 20, () -> {
            SectorConfig.save();
            if (parent != null) this.client.setScreen(parent); else this.close();
        });
        zdBtn(tr("zd.done"), LEFT_X + LEFT_W - 100, this.height - 26, 100, 20, () -> {
            SectorConfig.save();
            if (parent != null) this.client.setScreen(parent); else this.close();
        });
    }

    private static SectorConfig.Sector[] resize(SectorConfig.Sector[] src, int n) {
        n = Math.max(1, Math.min(10, n));
        SectorConfig.Sector[] arr = new SectorConfig.Sector[n];
        for (int i = 0; i < n; i++) {
            arr[i] = i < src.length ? src[i] : new SectorConfig.Sector();
        }
        return arr;
    }

    private static String on(boolean v) {
        return tr(v ? "zd.on" : "zd.off");
    }

    // название вида — теперь переводимый ключ
    private static String skinLabel() {
        return Text.translatable(MenuSkin.current().label).getString();
    }

    private static MenuSkin nextSkin() {
        MenuSkin[] all = MenuSkin.values();
        MenuSkin cur = MenuSkin.current();
        for (int i = 0; i < all.length; i++) {
            if (all[i] == cur) return all[(i + 1) % all.length];
        }
        return MenuSkin.CLASSIC;
    }

    private static String modeLabel() {
        switch (SectorConfig.submenuMode) {
            case "COMPACT": return tr("zd.mode.compact");
            case "SEPARATE": return tr("zd.mode.separate");
            default: return tr("zd.mode.overlay");
        }
    }

    private static String nextMode() {
        switch (SectorConfig.submenuMode) {
            case "OVERLAY": return "COMPACT";
            case "COMPACT": return "SEPARATE";
            default: return "OVERLAY";
        }
    }

    private String slotLabel(int i) {
        SectorConfig.Sector c = level[i];
        String name = (c == null || c.name == null || c.name.isEmpty())
                ? tr("zd.editor.empty") : c.name;
        return tr("zd.editor.slot", i + 1, name);
    }

    private void rebuild() {
        this.clearChildren();
        this.init();
    }

    private static void border(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x, y, x + w, y + 1, color);
        ctx.fill(x, y + h - 1, x + w, y + h, color);
        ctx.fill(x, y, x + 1, y + h, color);
        ctx.fill(x + w - 1, y, x + w, y + h, color);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);

        ctx.drawCenteredTextWithShadow(textRenderer, this.title, this.width / 2, 14, 0xFFFFFF);

        if (owner != null && owner.name != null && !owner.name.isEmpty()) {
            ctx.drawCenteredTextWithShadow(textRenderer,
                    Text.translatable("zd.editor.submenu_of", owner.name),
                    LEFT_X + LEFT_W / 2, 26, COL_HINT);
        }

        int py = 30;
        int ph = this.height - py - 36;
        int px = this.width - PREV_W - 12;

        ctx.fill(px, py, px + PREV_W, py + ph, 0x90101418);
        border(ctx, px, py, PREV_W, ph, 0xFF40A0FF);
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.translatable("zd.editor.preview"), px + PREV_W / 2, py + 6, 0xFF7FBFFF);

        MenuSkin skin = MenuSkin.current();
        MenuSkin.inSubmenu = owner != null;
        MenuSkin.compactOuter = false;

        int count = level.length;
        int cx = px + PREV_W / 2;
        int cy = py + ph / 2;
        int avail = Math.min(PREV_W, ph) / 2 - 20;
        int radius = skin == MenuSkin.CUSTOM ? 0
                : Math.max(30, Math.min(skin.layoutRadius(count, PREV_W, ph), avail));

        skin.drawBackground(ctx, count, cx, cy, radius);

        for (int i = 0; i < count; i++) {
            skin.drawAt(ctx, i, count, cx, cy, radius, COL_SECTOR);
        }

        int hoverIdx = skin.pickIndex(mouseX, mouseY, count, cx, cy, radius);
        if (hoverIdx >= 0) {
            skin.drawAt(ctx, hoverIdx, count, cx, cy, radius, MenuSkin.HOVER_MARK);
        }

        for (int i = 0; i < count; i++) {
            int[] p = skin.sectorTopLeft(i, count, cx, cy, radius);
            SectorConfig.Sector c = level[i];
            ItemStack icon = SectorConfig.iconStack(c);
            int midX = p[0] + p[2] / 2;
            int midY = p[1] + p[3] / 2;

            if (!icon.isEmpty()) {
                ctx.drawItem(icon, p[0] + (p[2] - 16) / 2, p[1] + (p[3] - 16) / 2);
            } else {
                String label;
                if (c.name != null && !c.name.isEmpty()) {
                    label = c.name;
                } else if (SectorConfig.emptyNumbers) {
                    label = String.valueOf(i + 1);
                } else {
                    label = "";
                }
                if (!label.isEmpty()) {
                    ctx.drawCenteredTextWithShadow(textRenderer,
                            Text.literal(label), midX, midY - 4, COL_TEXT);
                }
            }
        }

        for (ZdFlatButton b : buttons) {
            b.render(ctx, textRenderer, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.gui.Click click, boolean doubled) {
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
}
