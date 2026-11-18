package radialmenu.zd.client;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ShapeEditorScreen extends Screen {

    private static final int PANEL_W = 210;
    private static final int PANEL_X = 12;

    // разрешения маски формы: обычное / уменьшенное / маленькое
    private static final int RES_STANDARD = 64;
    private static final int RES_HALF = 32;
    private static final int RES_SMALL = 16;

    private static final int COL_CANVAS = 0x90101418;
    private static final int COL_GRID = 0x28FFFFFF;
    private static final int COL_FILL = 0xFF7FBFFF;
    private static final int COL_HINT = 0xFFA0A0A0;

    private final Screen parent;
    private int shapeIndex = 0;
    private boolean[][] grid = new boolean[RES_STANDARD][RES_STANDARD];
    private boolean gridLoaded = false;
    private int brush = 1;

    private final List<ZdFlatButton> buttons = new ArrayList<>();

    public ShapeEditorScreen(Screen parent) {
        super(Text.translatable("zd.shapeeditor.title"));
        this.parent = parent;
    }

    private static String tr(String key, Object... args) {
        return Text.translatable(key, args).getString();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private static boolean[][] fullMask(int n) {
        boolean[][] m = new boolean[n][n];
        for (boolean[] row : m) Arrays.fill(row, true);
        return m;
    }

    private static boolean[][] copy(boolean[][] src) {
        boolean[][] out = new boolean[src.length][src.length];
        for (int y = 0; y < src.length; y++) {
            System.arraycopy(src[y], 0, out[y], 0, src.length);
        }
        return out;
    }

    private static boolean allFalse(boolean[][] m) {
        for (boolean[] row : m) {
            for (boolean v : row) {
                if (v) return false;
            }
        }
        return true;
    }

    // ближайший сосед: форма переносится на новую сетку без потери контура
    private static boolean[][] resample(boolean[][] src, int n) {
        int old = src.length;
        boolean[][] out = new boolean[n][n];
        for (int y = 0; y < n; y++) {
            int sy = Math.min(old - 1, y * old / n);
            for (int x = 0; x < n; x++) {
                int sx = Math.min(old - 1, x * old / n);
                out[y][x] = src[sy][sx];
            }
        }
        return out;
    }

    private static String resLabel(int n) {
        if (n >= RES_STANDARD) return tr("zd.res.standard");
        if (n >= RES_HALF) return tr("zd.res.half");
        return tr("zd.res.small");
    }

    private static int nextRes(int n) {
        if (n >= RES_STANDARD) return RES_HALF;
        if (n >= RES_HALF) return RES_SMALL;
        return RES_STANDARD;
    }

    private static int nextShapeId() {
        int id = 1;
        outer:
        while (true) {
            for (SectorConfig.CustomShape s : SectorConfig.customShapes) {
                if (s.id == id) { id++; continue outer; }
            }
            return id;
        }
    }

    private SectorConfig.CustomShape shape() {
        SectorConfig.ensureCustomShapes();
        if (SectorConfig.customShapes.isEmpty()) {
            SectorConfig.addCustomShape(nextShapeId(), fullMask(RES_STANDARD));
            shapeIndex = 0;
        }
        shapeIndex = Math.max(0, Math.min(shapeIndex, SectorConfig.customShapes.size() - 1));
        return SectorConfig.customShapes.get(shapeIndex);
    }

    private void loadGrid() {
        boolean[][] mask = shape().mask;
        if (mask != null && mask.length >= 4 && mask.length <= 64
                && mask[0] != null && mask.length == mask[0].length) {
            grid = copy(mask);
        } else {
            grid = fullMask(RES_STANDARD);
        }
        gridLoaded = true;
    }

    private void save() {
        shape().mask = allFalse(grid) ? null : copy(grid);
        SectorConfig.save();
    }

    private void switchShape(int dir) {
        save();
        shapeIndex = (shapeIndex + dir + SectorConfig.customShapes.size()) % SectorConfig.customShapes.size();
        loadGrid();
        rebuild();
    }

    private void newShape() {
        save();
        SectorConfig.addCustomShape(nextShapeId(), fullMask(grid.length));
        shapeIndex = SectorConfig.customShapes.size() - 1;
        loadGrid();
        rebuild();
    }

    private void deleteShape() {
        save();
        if (SectorConfig.customShapes.size() <= 1) return; // хотя бы одна должна остаться
        SectorConfig.removeCustomShape(shapeIndex);
        shapeIndex = Math.max(0, Math.min(shapeIndex, SectorConfig.customShapes.size() - 1));
        loadGrid();
        rebuild();
    }

    private void changeRes() {
        int next = nextRes(grid.length);
        grid = resample(grid, next);
        shape().mask = copy(grid);
        SectorConfig.save();
        rebuild();
    }

    private void zdBtn(String label, int x, int y, int w, int h, Runnable action) {
        buttons.add(new ZdFlatButton(label, x, y, w, h, action));
    }

    @Override
    protected void init() {
        buttons.clear();
        if (!gridLoaded) loadGrid();

        int x = PANEL_X;
        int y = 34;

        zdBtn("◀", x, y, 34, 20, () -> switchShape(-1));
        zdBtn(tr("zd.shapeeditor.count", shapeIndex + 1, SectorConfig.customShapes.size()),
                x + 38, y, PANEL_W - 76, 20, () -> { });
        zdBtn("▶", x + PANEL_W - 34, y, 34, 20, () -> switchShape(1));
        y += 26;

        zdBtn(tr("zd.shapeeditor.res", resLabel(grid.length)), x, y, PANEL_W, 20, this::changeRes);
        y += 26;

        zdBtn(tr("zd.shapeeditor.brush", brush), x, y, PANEL_W, 20, () -> {
            brush = brush % 3 + 1;
            rebuild();
        });
        y += 26;

        zdBtn(tr("zd.shapeeditor.fill"), x, y, PANEL_W, 20, () -> {
            for (boolean[] row : grid) Arrays.fill(row, true);
        });
        y += 24;
        zdBtn(tr("zd.shapeeditor.clear"), x, y, PANEL_W, 20, () -> {
            for (boolean[] row : grid) Arrays.fill(row, false);
        });
        y += 24;

        zdBtn(tr("zd.shapeeditor.new"), x, y, 102, 20, this::newShape);
        zdBtn(tr("zd.shapeeditor.delete"), x + 108, y, PANEL_W - 108, 20, this::deleteShape);
        y += 26;

        zdBtn(tr("zd.back"), x, this.height - 26, 102, 20, () -> {
            save();
            if (parent != null) this.client.setScreen(parent); else this.close();
        });
        zdBtn(tr("zd.done"), x + PANEL_W - 102, this.height - 26, 102, 20, () -> {
            save();
            if (parent != null) this.client.setScreen(parent); else this.close();
        });
    }

    private void rebuild() {
        this.clearChildren();
        this.init();
    }

    private int canvasX() {
        return PANEL_X + PANEL_W + 16;
    }

    private int canvasY() {
        return 36;
    }

    private int canvasW() {
        return Math.max(64, width - canvasX() - 12);
    }

    private int canvasH() {
        return Math.max(64, height - canvasY() - 36);
    }

    private int cellPx() {
        int s = Math.min((canvasW() - 16) / n(), (canvasH() - 16) / n());
        return Math.max(2, Math.min(30, s));
    }

    private int n() {
        return grid.length;
    }

    private int gridX() {
        return canvasX() + (canvasW() - cellPx() * n()) / 2;
    }

    private int gridY() {
        return canvasY() + (canvasH() - cellPx() * n()) / 2;
    }

    private int cellAtX(int mx) {
        return (int) Math.floor((mx - gridX()) / (double) cellPx());
    }

    private int cellAtY(int my) {
        return (int) Math.floor((my - gridY()) / (double) cellPx());
    }

    private boolean insideCanvas(int mx, int my) {
        return mx >= gridX() && mx < gridX() + cellPx() * n()
                && my >= gridY() && my < gridY() + cellPx() * n();
    }

    private void paintAt(int mx, int my, boolean erase) {
        int gx = cellAtX(mx);
        int gy = cellAtY(my);
        int r = brush - 1;
        for (int dy = -r; dy <= r; dy++) {
            for (int dx = -r; dx <= r; dx++) {
                int x = gx + dx;
                int y = gy + dy;
                if (x < 0 || y < 0 || x >= n() || y >= n()) continue;
                grid[y][x] = !erase;
            }
        }
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

        int cx0 = canvasX();
        int cy0 = canvasY();
        int cw = canvasW();
        int chh = canvasH();
        ctx.fill(cx0, cy0, cx0 + cw, cy0 + chh, COL_CANVAS);

        int gx0 = gridX();
        int gy0 = gridY();
        int px = cellPx();
        int size = px * n();
        int n = n();

        for (int i = 0; i <= n; i++) {
            ctx.fill(gx0 + i * px, gy0, gx0 + i * px + 1, gy0 + size, COL_GRID);
            ctx.fill(gx0, gy0 + i * px, gx0 + size, gy0 + i * px + 1, COL_GRID);
        }

        for (int gy = 0; gy < n; gy++) {
            for (int gx = 0; gx < n; gx++) {
                if (!grid[gy][gx]) continue;
                ctx.fill(gx0 + gx * px, gy0 + gy * px,
                        gx0 + (gx + 1) * px, gy0 + (gy + 1) * px, COL_FILL);
            }
        }

        if (insideCanvas(mouseX, mouseY)) {
            int gx = cellAtX(mouseX);
            int gy = cellAtY(mouseY);
            ctx.fill(gx0 + gx * px, gy0 + gy * px,
                    gx0 + (gx + 1) * px, gy0 + (gy + 1) * px, 0x40FFFFFF);
        }

        border(ctx, gx0, gy0, size, size, 0xFF40A0FF);

        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal(tr("zd.shapeeditor.hint")),
                cx0 + cw / 2, cy0 + chh + 4, COL_HINT);

        for (ZdFlatButton b : buttons) {
            b.render(ctx, textRenderer, mouseX, mouseY);
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

        if (insideCanvas(mx, my)) {
            paintAt(mx, my, click.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT);
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        int mx = (int) click.x();
        int my = (int) click.y();
        if (insideCanvas(mx, my)) {
            paintAt(mx, my, click.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT);
            return true;
        }
        return super.mouseDragged(click, offsetX, offsetY);
    }

    @Override
    public void removed() {
        super.removed();
        save();
    }
}
