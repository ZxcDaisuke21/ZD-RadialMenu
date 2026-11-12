package radialmenu.zd.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class RadialMenuScreen extends Screen {
    private static final int COL_SECTOR = 0x60808080;
    private static final int COL_HOVER = 0x80FFFFFF;
    private static final int COL_TEXT = 0xFFFFFFFF;
    private static final int COL_HINT = 0xFFFFFF55;
    private static final int COL_ACTION = 0xFFA0A0A0;
    private static final int COL_ERROR = 0xFFFF5555;

    private static final int BTN_W = 110;
    private static final int BTN_H = 20;
    private static final int BTN_MARGIN = 8;

    private final List<SectorConfig.Sector[]> levels = new ArrayList<>();
    // владельцы уровней: owners[i] — сектор, чей children открыт на уровне i;
    // для корня — null. Нужен, чтобы правый клик открывал редактор нужного уровня
    private final List<SectorConfig.Sector> owners = new ArrayList<>();

    public RadialMenuScreen() {
        super(Text.literal("Радиальное меню"));
        levels.add(SectorConfig.sectors);
        owners.add(null);
    }

    public static void open() {
        MinecraftClient.getInstance().setScreen(new RadialMenuScreen());
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private SectorConfig.Sector[] current() {
        return levels.get(levels.size() - 1);
    }

    private SectorConfig.Sector currentOwner() {
        return owners.get(owners.size() - 1);
    }

    private int layoutRadius() {
        return MenuSkin.current().layoutRadius(current().length, width, height);
    }

    private int settingsX() {
        return width - BTN_W - BTN_MARGIN;
    }

    private int settingsY() {
        return height - BTN_H - BTN_MARGIN;
    }

    private boolean mouseOnSettings(int mx, int my) {
        return mx >= settingsX() && mx < settingsX() + BTN_W
                && my >= settingsY() && my < settingsY() + BTN_H;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        MenuSkin skin = MenuSkin.current();
        MenuSkin.inSubmenu = levels.size() > 1;

        int cx = width / 2;
        int cy = height / 2;
        SectorConfig.Sector[] cur = current();
        int radius = layoutRadius();
        int selected = skin.pickIndex(mouseX, mouseY, cur.length, cx, cy, radius);

        // кольцо родительского уровня (для «Сегментов» в подменю)
        int parentSel = -1;
        if (skin.hasOuterRing() && levels.size() > 1) {
            SectorConfig.Sector[] parent = levels.get(levels.size() - 2);
            parentSel = skin.pickOuterIndex(mouseX, mouseY, parent.length, cx, cy, radius);
            for (int i = 0; i < parent.length; i++) {
                skin.drawOuter(context, i, parent.length, cx, cy, radius,
                        i == parentSel, SectorConfig.iconStack(parent[i]));
            }
        }

        skin.drawBackground(context, cur.length, cx, cy, radius);

        for (int i = 0; i < cur.length; i++) {
            int[] p = skin.sectorTopLeft(i, cur.length, cx, cy, radius);
            int color = (i == selected) ? COL_HOVER : COL_SECTOR;
            skin.drawAt(context, i, cur.length, cx, cy, radius, color);

            SectorConfig.Sector s = cur[i];
            ItemStack icon = SectorConfig.iconStack(s);
            int midX = p[0] + p[2] / 2;
            int midY = p[1] + p[3] / 2;

            if (!icon.isEmpty()) {
                context.drawItem(icon, p[0] + (p[2] - 16) / 2, p[1] + (p[3] - 16) / 2);
            } else if (s.icon != null && !s.icon.trim().isEmpty()) {
                context.drawCenteredTextWithShadow(textRenderer, Text.literal("?"), midX, midY - 4, COL_ERROR);
            } else {
                String label = s.name.isEmpty() ? String.valueOf(i + 1) : s.name;
                context.drawCenteredTextWithShadow(textRenderer, Text.literal(label), midX, midY - 4, COL_TEXT);
            }

            if (!s.name.isEmpty()) {
                context.drawCenteredTextWithShadow(textRenderer, Text.literal(s.name),
                        midX, p[1] + p[3] + 2, skin.labelColor());
            }
        }

        int[] info = skin.infoPos(cur.length, cx, cy, radius);

        if (selected >= 0) {
            SectorConfig.Sector hs = cur[selected];
            String nm = hs.name.isEmpty() ? "Слот " + (selected + 1) : hs.name;
            context.drawCenteredTextWithShadow(textRenderer, Text.literal(nm),
                    info[0], info[1] - 10, COL_TEXT);

            String action = switch (hs.type) {
                case "key" -> "клавиша: " + Text.translatable(hs.keyBinding).getString();
                case "submenu" -> "подменю";
                default -> hs.command.trim();
            };
            if (!action.isEmpty()) {
                context.drawCenteredTextWithShadow(textRenderer, Text.literal(action),
                        info[0], info[1] + 4, COL_ACTION);
            }
        } else if (parentSel >= 0) {
            SectorConfig.Sector[] parent = levels.get(levels.size() - 2);
            SectorConfig.Sector ps = parent[parentSel];
            String nm = ps.name.isEmpty() ? "Слот " + (parentSel + 1) : ps.name;
            context.drawCenteredTextWithShadow(textRenderer,
                    Text.literal(nm + " (клик — назад)"), info[0], info[1] - 5, COL_TEXT);
        } else if (levels.size() > 1) {
            context.drawCenteredTextWithShadow(textRenderer,
                    Text.literal(skin != MenuSkin.CLASSIC ? "клик мимо кнопок — назад" : "клик в центр — назад"),
                    info[0], info[1] - 5, COL_HINT);
        }

        boolean hover = mouseOnSettings(mouseX, mouseY);
        context.fill(settingsX(), settingsY(), settingsX() + BTN_W, settingsY() + BTN_H,
                hover ? COL_HOVER : COL_SECTOR);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("Настройки"),
                settingsX() + BTN_W / 2, settingsY() + (BTN_H - 8) / 2, COL_TEXT);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        int mx = (int) click.x();
        int my = (int) click.y();

        MenuSkin skin = MenuSkin.current();
        MenuSkin.inSubmenu = levels.size() > 1;

        if (mouseOnSettings(mx, my)) {
            RadialMenuEditorScreen.openAt(-1);
            return true;
        }

        SectorConfig.Sector[] cur = current();
        int cx = width / 2;
        int cy = height / 2;
        int radius = layoutRadius();

        // клик по кольцу родителя — назад
        if (skin.hasOuterRing() && levels.size() > 1) {
            SectorConfig.Sector[] parent = levels.get(levels.size() - 2);
            if (skin.pickOuterIndex(mx, my, parent.length, cx, cy, radius) >= 0) {
                levels.remove(levels.size() - 1);
                owners.remove(owners.size() - 1);
                return true;
            }
        }

        int selected = skin.pickIndex(mx, my, cur.length, cx, cy, radius);

        if (selected < 0) {
            if (levels.size() > 1) {
                levels.remove(levels.size() - 1);
                owners.remove(owners.size() - 1);
            } else {
                close();
            }
            return true;
        }

        SectorConfig.Sector s = cur[selected];

        if (click.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            // редактируем слот текущего уровня: для подменю передаём владельца,
            // чтобы правки попадали в children нужного сектора, а не в корень
            SectorConfig.Sector owner = currentOwner();
            MinecraftClient.getInstance().setScreen(
                    new RadialMenuEditorScreen(cur, null, selected, owner));
            return true;
        }

        if (click.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            switch (s.type) {
                case "submenu":
                    if (s.children != null && s.children.length > 0) {
                        RadialMenuScreen sub = new RadialMenuScreen();
                        sub.levels.clear();
                        sub.levels.addAll(this.levels);
                        sub.levels.add(s.children);
                        sub.owners.clear();
                        sub.owners.addAll(this.owners);
                        sub.owners.add(s);
                        client.setScreen(sub);
                    }
                    return true;

                case "key":
                    ZdRadialMenuClient.pulseKey(KeyBinding.byId(s.keyBinding));
                    close();
                    return true;

                default: // command
                    String command = s.command.trim();
                    MinecraftClient mc = MinecraftClient.getInstance();
                    if (!command.isEmpty() && mc.player != null && mc.player.networkHandler != null) {
                        if (command.startsWith("/")) {
                            mc.player.networkHandler.sendChatCommand(command.substring(1));
                        } else {
                            mc.player.networkHandler.sendChatMessage(command);
                        }
                    }
                    close();
                    return true;
            }
        }
        return true;
    }

    @Override
    public boolean keyReleased(KeyInput input) {
        if (ZdRadialMenuClient.openBinding().matchesKey(input)) {
            close();
            return true;
        }
        return super.keyReleased(input);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
}
