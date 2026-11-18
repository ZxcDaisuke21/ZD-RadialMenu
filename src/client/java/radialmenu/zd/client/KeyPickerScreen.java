package radialmenu.zd.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class KeyPickerScreen extends Screen {
    private final Screen parent;
    private final Consumer<String> onPick;
    private final List<KeyBinding> keys;
    private int scroll = 0;

    private static final int ROW_H = 24;
    private static final int HEADER_H = 20;
    private static final int BTN_W = 280;

    private final List<ZdFlatButton> buttons = new ArrayList<>();
    private final List<Object[]> visible = new ArrayList<>();

    public KeyPickerScreen(Screen parent, Consumer<String> onPick) {
        super(Text.translatable("zd.keypicker.title"));
        this.parent = parent;
        this.onPick = onPick;
        this.keys = new ArrayList<>(Arrays.asList(MinecraftClient.getInstance().options.allKeys));
    }

    private static String tr(String key, Object... args) {
        return Text.translatable(key, args).getString();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private static String catId(KeyBinding kb) {
        return kb.getCategory().id().toString();
    }

    // человекочитаемое имя категории: сначала перевод игры,
    // потом наш словарь по хвосту идентификатора, потом сам хвост
    private static String catLabel(String cat) {
        if (cat == null || cat.isEmpty()) return tr("zd.cat.misc");
        String t = Text.translatable(cat).getString();
        if (!t.equals(cat)) return t;
        int dot = cat.lastIndexOf('.');
        String tail = dot >= 0 ? cat.substring(dot + 1) : cat;
        String key = "zd.cat." + tail;
        String fb = tr(key);
        return fb.equals(key) ? tail : fb;
    }

    private List<Object> buildRows() {
        List<Object> rows = new ArrayList<>();
        String lastCat = null;
        for (KeyBinding kb : keys) {
            String cat = catId(kb);
            if (!cat.equals(lastCat)) {
                rows.add(cat);
                lastCat = cat;
            }
            rows.add(kb);
        }
        return rows;
    }

    private int capacity() {
        return Math.max(1, (this.height - 70) / ROW_H);
    }

    @Override
    protected void init() {
        buttons.clear();
        visible.clear();

        List<Object> rows = buildRows();
        int maxScroll = Math.max(0, rows.size() - capacity());
        if (scroll > maxScroll) scroll = maxScroll;
        if (scroll < 0) scroll = 0;

        int x = this.width / 2 - BTN_W / 2;

        buttons.add(new ZdFlatButton("↑", x, 22, 40, 18, () -> {
            scroll--;
            rebuild();
        }));
        buttons.add(new ZdFlatButton("↓", x + BTN_W - 40, 22, 40, 18, () -> {
            scroll++;
            rebuild();
        }));

        int y = 46;
        for (int i = scroll; i < rows.size() && visible.size() < capacity(); i++) {
            Object row = rows.get(i);
            if (row instanceof String cat) {
                visible.add(new Object[]{y, Boolean.TRUE, catLabel(cat)});
                y += HEADER_H;
            } else {
                KeyBinding kb = (KeyBinding) row;
                String id = kb.getId();
                visible.add(new Object[]{y, Boolean.FALSE, keyLabel(kb)});
                buttons.add(new ZdFlatButton(keyLabel(kb), x, y, BTN_W, 20, () -> {
                    onPick.accept(id);
                    MinecraftClient.getInstance().setScreen(parent);
                }));
                y += ROW_H;
            }
        }

        buttons.add(new ZdFlatButton(tr("zd.back"), this.width / 2 - 100, this.height - 28, 200, 20,
                () -> MinecraftClient.getInstance().setScreen(parent)));
    }

    private static String keyLabel(KeyBinding kb) {
        String name = Text.translatable(kb.getId()).getString();
        String bound = kb.getBoundKeyLocalizedText().getString();
        return name + " [" + bound + "]";
    }

    private void rebuild() {
        this.clearChildren();
        this.init();
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);

        ctx.drawCenteredTextWithShadow(textRenderer, this.title, this.width / 2, 8, 0xFFFFFF);

        List<Object> rows = buildRows();
        int last = Math.min(rows.size(), scroll + capacity());
        if (!rows.isEmpty() && scroll < rows.size()) {
            ctx.drawCenteredTextWithShadow(textRenderer,
                    Text.literal(tr("zd.keypicker.range", scroll + 1, last, rows.size())),
                    this.width / 2, this.height - 44, 0xFFFFFF55);
        }

        for (Object[] row : visible) {
            int y = (Integer) row[0];
            boolean header = (Boolean) row[1];
            if (header) {
                ctx.drawCenteredTextWithShadow(textRenderer,
                        Text.literal((String) row[2]),
                        this.width / 2, y + 4, 0xFF7FBFFF);
            }
        }

        for (ZdFlatButton b : buttons) {
            b.render(ctx, textRenderer, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horiz, double vert) {
        if (vert > 0) scroll--;
        else if (vert < 0) scroll++;
        rebuild();
        return true;
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
}
