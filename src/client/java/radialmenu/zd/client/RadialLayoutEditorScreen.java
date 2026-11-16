package radialmenu.zd.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class RadialLayoutEditorScreen extends Screen {
    private static final int PANEL_W = 210;
    private static final int PANEL_X = 12;

    private static final int COL_CANVAS = 0x90101418;
    private static final int COL_GRID = 0x28FFFFFF;
    private static final int COL_AXIS = 0x40FFFFFF;
    private static final int COL_SEL = 0xFFFFFFFF;
    private static final int COL_GRAB = 0xFF7FBFFF;
    private static final int COL_HINT = 0xFFA0A0A0;

    private static final String[] SHAPES = {"rect", "hex", "disc", "diamond", "triangle"};

    private int selected = 0;
    private boolean grabbed = false;
    private int activeSwatch = -1;

    private TextFieldWidget colorField;
    private final List<int[]> swatchRects = new ArrayList<>();
    private final List<ZdFlatButton> buttons = new ArrayList<>();

    public RadialLayoutEditorScreen() {
        super(Text.translatable("zd.layout.title"));
    }

    private static String tr(String key, Object... args) {
        return Text.translatable(key, args).getString();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private int canvasX() {
        return PANEL_X + PANEL_W + 16;
    }

    private int canvasY() {
        return 36;
    }

    private int canvasW() {
        return Math.max(60, width - canvasX() - 12);
    }

    private int canvasH() {
        return Math.max(60, height - canvasY() - 36);
    }

    private int ccx() {
        return canvasX() + canvasW() / 2;
    }

    private int ccy() {
        return canvasY() + canvasH() / 2;
    }

    private SectorConfig.CustomCell cell() {
        SectorConfig.ensureCustomCells();
        return SectorConfig.customLayout[Math.max(0, Math.min(selected, count() - 1))];
    }

    private int count() {
        return SectorConfig.customLayout.length;
    }

    private void zdBtn(String label, int x, int y, int w, int h, Runnable action) {
        buttons.add(new ZdFlatButton(label, x, y, w, h, action));
    }

    @Override
    protected void init() {
        buttons.clear();
        swatchRects.clear();
        SectorConfig.ensureCustomCells();
        if (selected >= count()) selected = count() - 1;
        if (selected < 0) selected = 0;

        int x = PANEL_X;
        int y = 34;

        zdBtn("◀", x, y, 34, 20, () -> {
            selected = (selected - 1 + count()) % count();
            rebuild();
        });
        zdBtn(tr("zd.slot.fallback", selected + 1), x + 38, y, PANEL_W - 76, 20, () -> { });
        zdBtn("▶", x + PANEL_W - 34, y, 34, 20, () -> {
            selected = (selected + 1) % count();
            rebuild();
        });
        y += 26;

        zdBtn(tr("zd.layout.shape", shapeLabel(cell().shape)), x, y, PANEL_W, 20, () -> {
            cell().shape = nextShape(cell().shape);
            SectorConfig.save();
            rebuild();
        });
        y += 26;

        zdBtn(tr("zd.layout.w_minus"), x, y, 50, 20, () -> {
            cell().w = Math.max(8, cell().w - 4);
            SectorConfig.save();
            rebuild();
        });
        zdBtn(tr("zd.layout.width", cell().w), x + 55, y, PANEL_W - 110, 20, () -> { });
        zdBtn(tr("zd.layout.w_plus"), x + PANEL_W - 50, y, 50, 20, () -> {
            cell().w = Math.min(160, cell().w + 4);
            SectorConfig.save();
            rebuild();
        });
        y += 24;
        zdBtn(tr("zd.layout.h_minus"), x, y, 50, 20, () -> {
            cell().h = Math.max(8, cell().h - 4);
            SectorConfig.save();
            rebuild();
        });
        zdBtn(tr("zd.layout.height", cell().h), x + 55, y, PANEL_W - 110, 20, () -> { });
        zdBtn(tr("zd.layout.h_plus"), x + PANEL_W - 50, y, 50, 20, () -> {
            cell().h = Math.min(120, cell().h + 4);
            SectorConfig.save();
            rebuild();
        });
        y += 28;

        zdBtn("←", x, y, 48, 18, () -> {
            cell().dx -= 2;
            SectorConfig.save();
        });
        zdBtn("↑", x + 52, y, 48, 18, () -> {
            cell().dy -= 2;
            SectorConfig.save();
        });
        zdBtn("↓", x + 104, y, 48, 18, () -> {
            cell().dy += 2;
            SectorConfig.save();
        });
        zdBtn("→", x + 156, y, 48, 18, () -> {
            cell().dx += 2;
            SectorConfig.save();
        });
        y += 26;

        int[] pal = SectorConfig.palette();
        for (int i = 0; i < pal.length; i++) {
            int col = i % 6;
            int row = i / 6;
            swatchRects.add(new int[]{x + col * 34, y + row * 26, 30, 22, i});
        }
        y += 56;

        colorField = new TextFieldWidget(textRenderer, x, y, PANEL_W, 16, Text.literal("color"));
        colorField.setPlaceholder(Text.translatable("zd.layout.color_ph"));
        colorField.setMaxLength(8);
        colorField.setText(colorHex(cell().color));
        colorField.setChangedListener(t -> {
            try {
                String v = t.trim();
                long parsed = Long.parseLong(v, 16);
                if (v.length() == 6) parsed |= 0xFF000000L;
                int argb = (int) parsed;
                cell().color = argb;
                if (activeSwatch >= 0) SectorConfig.palette()[activeSwatch] = argb;
                SectorConfig.save();
            } catch (NumberFormatException ignored) {
            }
        });
        addDrawableChild(colorField);
        y += 26;

        zdBtn(tr("zd.layout.center"), x, y, 102, 20, () -> {
            cell().dx = -cell().w / 2;
            cell().dy = -cell().h / 2;
            SectorConfig.save();
        });
        zdBtn(tr("zd.layout.reset"), x + 108, y, PANEL_W - 108, 20, () -> {
            SectorConfig.customLayout = new SectorConfig.CustomCell[0];
            SectorConfig.ensureCustomCells();
            SectorConfig.save();
            rebuild();
        });
        y += 28;

        zdBtn(tr("zd.layout.add_slot"), x, y, 102, 20, () -> {
            SectorConfig.setSectorCount(SectorConfig.sectorCount + 1);
            SectorConfig.save();
            selected = count() - 1;
            rebuild();
        });
        zdBtn(tr("zd.layout.remove_slot"), x + 108, y, PANEL_W - 108, 20, () -> {
            SectorConfig.setSectorCount(SectorConfig.sectorCount - 1);
            SectorConfig.save();
            if (selected >= count()) selected = count() - 1;
            rebuild();
        });

        zdBtn(tr("zd.back"), x, this.height - 26, 102, 20, () -> {
            SectorConfig.save();
            this.close();
        });
        zdBtn(tr("zd.done"), x + PANEL_W - 102, this.height - 26, 102, 20, () -> {
            SectorConfig.save();
            this.close();
        });
    }

    private static String shapeKey(String s) {
        for (String value : SHAPES) {
            if (value.equals(s)) return value;
        }
        return "rect";
    }

    private static String shapeLabel(String s) {
        return tr("zd.shape." + shapeKey(s));
    }

    private static String nextShape(String s) {
        for (int i = 0; i < SHAPES.length; i++) {
            if (SHAPES[i].equals(s)) return SHAPES[(i + 1) % SHAPES.length];
        }
        return SHAPES[0];
    }

    private static String colorHex(int argb) {
        return String.format("%08X", argb);
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private int pickCell(int mx, int my) {
        return ShapeHits.pick(SectorConfig.customLayout, ccx(), ccy(), mx, my);
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

        SectorConfig.ensureCustomCells();

        if (grabbed) {
            SectorConfig.CustomCell c = cell();
            c.dx = clamp(mouseX - ccx() - c.w / 2,
                    canvasX() - ccx(), canvasX() + canvasW() - c.w - ccx());
            c.dy = clamp(mouseY - ccy() - c.h / 2,
                    canvasY() - ccy(), canvasY() + canvasH() - c.h - ccy());
        }

        ctx.drawCenteredTextWithShadow(textRenderer, this.title, this.width / 2, 14, 0xFFFFFF);

        int cx0 = canvasX();
        int cy0 = canvasY();
        int cw = canvasW();
        int chh = canvasH();

        ctx.fill(cx0, cy0, cx0 + cw, cy0 + chh, COL_CANVAS);
        border(ctx, cx0, cy0, cw, chh, 0xFF40A0FF);

        for (int gx = ccx(); gx > cx0; gx -= 40) ctx.fill(gx, cy0, gx + 1, cy0 + chh, COL_GRID);
        for (int gx = ccx() + 40; gx < cx0 + cw; gx += 40) ctx.fill(gx, cy0, gx + 1, cy0 + chh, COL_GRID);
        for (int gy = ccy(); gy > cy0; gy -= 40) ctx.fill(cx0, gy, cx0 + cw, gy + 1, COL_GRID);
        for (int gy = ccy() + 40; gy < cy0 + chh; gy += 40) ctx.fill(cx0, gy, cx0 + cw, gy + 1, COL_GRID);
        ctx.fill(ccx(), cy0, ccx() + 1, cy0 + chh, COL_AXIS);
        ctx.fill(cx0, ccy(), cx0 + cw, ccy() + 1, COL_AXIS);

        MenuSkin skin = MenuSkin.CUSTOM;
        int hoverIdx = grabbed ? -1 : pickCell(mouseX, mouseY);
        for (int i = 0; i < count(); i++) {
            SectorConfig.CustomCell c = SectorConfig.customLayout[i];
            int px = ccx() + c.dx;
            int py = ccy() + c.dy;
            int mark = (i == selected || i == hoverIdx) ? MenuSkin.HOVER_MARK : 0;
            skin.drawSector(ctx, i, count(), px, py, c.w, c.h, mark);

            if (i == selected) {
                border(ctx, px, py, c.w, c.h, grabbed ? COL_GRAB : COL_SEL);
            }

            ctx.drawCenteredTextWithShadow(textRenderer,
                    Text.literal(String.valueOf(i + 1)),
                    px + c.w / 2, py + c.h / 2 - 4, 0xFFFFFFFF);
        }

        int curColor = cell().color;
        int[] pal = SectorConfig.palette();
        for (int[] s : swatchRects) {
            int color = pal[s[4]];
            ctx.fill(s[0], s[1], s[0] + s[2], s[1] + s[3], color);
            boolean isActive = s[4] == activeSwatch;
            border(ctx, s[0], s[1], s[2], s[3], isActive ? 0xFFFFFFFF : 0xFF606060);
            if (color == curColor && !isActive) {
                ctx.fill(s[0] + s[2] - 4, s[1], s[0] + s[2], s[1] + 4, 0xFFFFFFFF);
            }
        }

        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal(tr("zd.layout.hint")),
                ccx(), cy0 + chh + 4, COL_HINT);

        for (ZdFlatButton b : buttons) {
            b.render(ctx, textRenderer, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.gui.Click click, boolean doubled) {
        int mx = (int) click.x();
        int my = (int) click.y();

        if (grabbed) {
            grabbed = false;
            SectorConfig.save();
            return true;
        }

        for (int[] s : swatchRects) {
            if (mx >= s[0] && mx < s[0] + s[2] && my >= s[1] && my < s[1] + s[3]) {
                activeSwatch = s[4];
                int color = SectorConfig.palette()[s[4]];
                cell().color = color;
                if (colorField != null) colorField.setText(colorHex(color));
                SectorConfig.save();
                return true;
            }
        }

        for (ZdFlatButton b : buttons) {
            if (b.contains(mx, my)) {
                b.click();
                return true;
            }
        }

        int hit = pickCell(mx, my);
        if (hit >= 0) {
            if (hit == selected) {
                grabbed = true;
            } else {
                selected = hit;
                rebuild();
            }
            return true;
        }

        return super.mouseClicked(click, doubled);
    }

    @Override
    public void removed() {
        super.removed();
        SectorConfig.save();
    }
}
