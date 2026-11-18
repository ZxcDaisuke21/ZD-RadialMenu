package radialmenu.zd.client;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class IconPickerScreen extends Screen {

    private static final int CELL = 28;
    private static final int COL_HINT = 0xFFA0A0A0;

    private final Screen parent;
    private final Consumer<String> onPick;

    private List<Identifier> filtered = new ArrayList<>();
    private int page = 0;
    private String lastQuery = null;

    private TextFieldWidget searchField;
    private final Map<Identifier, ItemStack> stackCache = new HashMap<>();
    private final List<int[]> cellRects = new ArrayList<>(); // {x, y, w, h, индекс в filtered}

    public IconPickerScreen(Screen parent, Consumer<String> onPick) {
        super(Text.translatable("zd.iconpicker.title"));
        this.parent = parent;
        this.onPick = onPick;
    }

    private static String tr(String key, Object... args) {
        return Text.translatable(key, args).getString();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    // ---- данные ----

    private List<Identifier> query() {
        String q = searchField == null ? "" : searchField.getText().trim().toLowerCase();
        List<Identifier> out = new ArrayList<>();
        for (Identifier id : Registries.ITEM.getIds()) {
            ItemStack st = stackFor(id);
            boolean byPath = id.getPath().toLowerCase().contains(q);
            boolean byName = st != null && !st.isEmpty()
                    && st.getName().getString().toLowerCase().contains(q);
            if (q.isEmpty() || byPath || byName) out.add(id);
        }
        out.sort(Comparator.comparing(Identifier::toString));
        return out;
    }

    private void refresh() {
        String q = searchField == null ? "" : searchField.getText().trim().toLowerCase();
        if (lastQuery == null || !lastQuery.equals(q)) {
            lastQuery = q;
            filtered = query();
            page = 0;
        }
    }

    private ItemStack stackFor(Identifier id) {
        ItemStack cached = stackCache.get(id);
        if (cached != null) return cached;
        ItemStack st = ItemStack.EMPTY;
        try {
            Item item = Registries.ITEM.get(id);
            if (id.equals(Registries.ITEM.getId(item))) st = new ItemStack(item);
        } catch (Exception ignored) {
        }
        stackCache.put(id, st);
        return st;
    }

    // ---- геометрия ----

    private int gridW() {
        return Math.max(4 * CELL, Math.min(width - 60, 12 * CELL));
    }

    private int cols() {
        return gridW() / CELL;
    }

    private int rows() {
        return Math.max(1, (height - gridTopY() - 40) / CELL);
    }

    private int perPage() {
        return cols() * rows();
    }

    private int pages() {
        return Math.max(1, (filtered.size() + perPage() - 1) / perPage());
    }

    private int gridX() {
        return (width - gridW()) / 2;
    }

    private int searchY() {
        return 32;
    }

    private int navY() {
        return searchY() + 24;
    }

    private int gridTopY() {
        return navY() + 26;
    }

    private int gridH() {
        return rows() * CELL;
    }

    private void buildCells() {
        cellRects.clear();
        int gx = gridX();
        int gy = gridTopY();
        int start = page * perPage();
        for (int r = 0; r < rows(); r++) {
            for (int c = 0; c < cols(); c++) {
                int idx = start + r * cols() + c;
                if (idx >= filtered.size()) continue;
                cellRects.add(new int[]{
                        gx + c * CELL, gy + r * CELL, CELL, CELL, idx});
            }
        }
    }

    private int hoverCell(int mx, int my) {
        for (int[] r : cellRects) {
            if (mx >= r[0] && mx < r[0] + r[2] && my >= r[1] && my < r[1] + r[3]) return r[4];
        }
        return -2;
    }

    // ---- инициализация и отрисовка ----

    @Override
    protected void init() {
        if (filtered.isEmpty() && lastQuery == null) {
            refresh();
        }

        searchField = new TextFieldWidget(textRenderer, gridX(), searchY(),
                gridW() - 36, 18, Text.literal("iconsearch"));
        searchField.setPlaceholder(Text.translatable("zd.iconpicker.search"));
        searchField.setMaxLength(100);
        searchField.setText(lastQuery == null ? "" : lastQuery);
        searchField.setChangedListener(t -> lastQuery = null); // пересчитать фильтр на следующем кадре
        addDrawableChild(searchField);
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

        refresh();
        buildCells();

        // без иконки — клетка справа от поиска
        int noneX = gridX() + gridW() - 32;
        boolean noneHover = mouseX >= noneX && mouseX < noneX + 32
                && mouseY >= searchY() && mouseY < searchY() + 18;
        ctx.fill(noneX + 1, searchY() + 1, noneX + 31, searchY() + 17,
                noneHover ? 0x40FFFFFF : 0x60101418);
        ctx.fill(noneX + 10, searchY() + 8, noneX + 22, searchY() + 9, 0xFFC0C0C0);
        ctx.fill(noneX + 10, searchY() + 14, noneX + 22, searchY() + 15, 0xFFC0C0C0);
        border(ctx, noneX, searchY(), 32, 18, noneHover ? 0xFFFFFFFF : 0xFF404048);

        // навигация по страницам
        int navX = gridX();
        boolean prevHover = mouseX >= navX && mouseX < navX + 34
                && mouseY >= navY() && mouseY < navY() + 20;
        boolean nextHover = mouseX >= navX + gridW() - 34 && mouseX < navX + gridW()
                && mouseY >= navY() && mouseY < navY() + 20;
        if (page > 0 || prevHover) {
            ctx.fill(navX + 1, navY() + 1, navX + 33, navY() + 19,
                    prevHover ? 0x40FFFFFF : 0x60101418);
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("◀"),
                    navX + 17, navY() + 6, 0xFFFFFF);
        }
        if (page < pages() - 1 || nextHover) {
            ctx.fill(navX + gridW() - 33, navY() + 1, navX + gridW() - 1, navY() + 19,
                    nextHover ? 0x40FFFFFF : 0x60101418);
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("▶"),
                    navX + gridW() - 17, navY() + 6, 0xFFFFFF);
        }
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal(tr("zd.iconpicker.page", page + 1, pages())),
                gridX() + gridW() / 2, navY() + 6, 0xFFFFFF);

        // сетка
        int hover = hoverCell(mouseX, mouseY);
        for (int[] r : cellRects) {
            int idx = r[4];
            boolean isHover = idx == hover;
            ctx.fill(r[0] + 1, r[1] + 1, r[0] + r[2] - 1, r[1] + r[3] - 1,
                    isHover ? 0x40FFFFFF : 0x60101418);
            ctx.drawItem(stackFor(filtered.get(idx)), r[0] + 6, r[1] + 6);
            border(ctx, r[0], r[1], r[2], r[3], isHover ? 0xFFFFFFFF : 0xFF404048);
        }

        // подпись под курсором и пустой результат
        int capY = gridTopY() + gridH() + 4;
        if (filtered.isEmpty()) {
            ctx.drawCenteredTextWithShadow(textRenderer,
                    Text.literal(tr("zd.iconpicker.empty")),
                    width / 2, capY, COL_HINT);
        } else if (hover >= 0) {
            Identifier id = filtered.get(hover);
            ItemStack st = stackFor(id);
            String name = st.isEmpty() ? id.getPath() : st.getName().getString();
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(name),
                    width / 2, capY, 0xFFFFFF);
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(id.toString()),
                    width / 2, capY + 11, 0xFF909090);
        } else if (noneHover) {
            ctx.drawCenteredTextWithShadow(textRenderer,
                    Text.literal(tr("zd.iconpicker.none")),
                    width / 2, capY, COL_HINT);
        }
    }

    // ---- ввод ----

    private void pick(String id) {
        onPick.accept(id);
        SectorConfig.save();
        if (parent != null) this.client.setScreen(parent); else this.close();
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        int mx = (int) click.x();
        int my = (int) click.y();

        int noneX = gridX() + gridW() - 32;
        if (mx >= noneX && mx < noneX + 32 && my >= searchY() && my < searchY() + 18) {
            pick("");
            return true;
        }

        int navX = gridX();
        if (my >= navY() && my < navY() + 20) {
            if (mx >= navX && mx < navX + 34 && page > 0) {
                page--;
                rebuild();
                return true;
            }
            if (mx >= navX + gridW() - 34 && mx < navX + gridW() && page < pages() - 1) {
                page++;
                rebuild();
                return true;
            }
        }

        int idx = hoverCell(mx, my);
        if (idx >= 0) {
            pick(filtered.get(idx).toString());
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (verticalAmount != 0) {
            int np = page + (verticalAmount > 0 ? -1 : 1);
            if (np >= 0 && np < pages()) {
                page = np;
                rebuild();
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (input.key() == GLFW.GLFW_KEY_ENTER || input.key() == GLFW.GLFW_KEY_KP_ENTER) {
            String t = searchField.getText().trim();
            if (!t.isEmpty()) {
                Identifier id = Identifier.tryParse(t.toLowerCase());
                if (id != null && Registries.ITEM.containsId(id)) {
                    pick(id.toString());
                    return true;
                }
            }
        }
        return super.keyPressed(input);
    }
}
