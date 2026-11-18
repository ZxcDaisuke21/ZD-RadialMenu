package radialmenu.zd.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class SlotEditorScreen extends Screen {
    private final Screen parent;
    private final SectorConfig.Sector[] level;
    private final int index;
    private final SectorConfig.Sector owner;

    private static final int LEFT_W = 210;
    private static final int LEFT_X = 12;

    private static final int COL_TEXT = 0xFFFFFFFF;
    private static final int COL_HINT = 0xFFA0A0A0;

    private TextFieldWidget nameField;
    private TextFieldWidget cmdField;

    private final List<ZdFlatButton> buttons = new ArrayList<>();

    public SlotEditorScreen(Screen parent, SectorConfig.Sector[] level, int index,
                            SectorConfig.Sector owner) {
        super(Text.translatable("zd.sloteditor.title"));
        this.parent = parent;
        this.level = level;
        this.index = Math.max(0, Math.min(index, level.length - 1));
        this.owner = owner;
    }

    private static String tr(String key, Object... args) {
        return Text.translatable(key, args).getString();
    }

    private SectorConfig.Sector current() {
        return level[index];
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
        SectorConfig.Sector s = current();
        String type = s.type == null ? "command" : s.type;
        int y = 40;

        nameField = new TextFieldWidget(textRenderer, LEFT_X, y, LEFT_W, 18,
                Text.literal("name"));
        nameField.setPlaceholder(Text.translatable("zd.sloteditor.name_ph"));
        nameField.setText(s.name);
        nameField.setChangedListener(t -> s.name = t);
        addDrawableChild(nameField);
        y += 22;

        if ("command".equals(type)) {
            cmdField = new TextFieldWidget(textRenderer, LEFT_X, y, LEFT_W, 18,
                    Text.literal("command"));
            cmdField.setPlaceholder(Text.translatable("zd.sloteditor.cmd_ph"));
            cmdField.setMaxLength(400);
            cmdField.setText(s.command);
            cmdField.setChangedListener(t -> s.command = t);
            addDrawableChild(cmdField);
            y += 22;
        } else if ("key".equals(type)) {
            String keyName = s.keyBinding == null || s.keyBinding.isEmpty()
                    ? tr("zd.sloteditor.no_key")
                    : Text.translatable(s.keyBinding).getString();
            zdBtn(tr("zd.sloteditor.key", keyName), LEFT_X, y, LEFT_W, 20, () ->
                    this.client.setScreen(new KeyPickerScreen(this, key -> {
                        s.keyBinding = key;
                        SectorConfig.save();
                    })));
            y += 22;
        } else {
            // подменю: наполнение — на отдельном экране
        }

        zdBtn(tr("zd.sloteditor.icons_btn", iconLabel(s)), LEFT_X, y, LEFT_W, 20, () ->
                this.client.setScreen(new IconPickerScreen(this, id -> {
                    s.icon = id;
                    SectorConfig.save();
                    rebuild();
                })));
        y += 26;

        zdBtn(tr("zd.sloteditor.type", typeLabel(s)), LEFT_X, y, LEFT_W, 20, () -> {
            s.type = nextType(s.type);
            SectorConfig.save();
            rebuild();
        });
        y += 26;

        if ("submenu".equals(type)) {
            int count = s.children == null ? 0 : s.children.length;
            zdBtn(tr("zd.sloteditor.submenu_btn", count), LEFT_X, y, LEFT_W, 20,
                    () -> RadialMenuEditorScreen.openSubmenu(s));
            y += 26;
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

    private static String iconLabel(SectorConfig.Sector s) {
        return s.icon == null || s.icon.isEmpty() ? tr("zd.iconpicker.none") : s.icon;
    }

    private static String typeKey(SectorConfig.Sector s) {
        switch (s.type == null ? "command" : s.type) {
            case "key": return "key";
            case "submenu": return "submenu";
            default: return "command";
        }
    }

    private static String typeLabel(SectorConfig.Sector s) {
        return tr("zd.type." + typeKey(s));
    }

    private static String nextType(String t) {
        switch (t == null ? "command" : t) {
            case "command": return "key";
            case "key": return "submenu";
            default: return "command";
        }
    }

    private void rebuild() {
        this.clearChildren();
        this.init();
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);

        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.translatable("zd.slot.fallback", index + 1),
                LEFT_X + LEFT_W / 2, 16, COL_TEXT);

        if (owner != null && owner.name != null && !owner.name.isEmpty()) {
            ctx.drawCenteredTextWithShadow(textRenderer,
                    Text.translatable("zd.sloteditor.owner", owner.name),
                    LEFT_X + LEFT_W / 2, 28, COL_HINT);
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
