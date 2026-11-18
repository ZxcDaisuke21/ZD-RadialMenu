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
    private static final int GLOW_OVERLAY = 0x66AAE2FF;

    private static final int BTN_W = 110;
    private static final int BTN_H = 20;
    private static final int BTN_MARGIN = 8;

    private final List<SectorConfig.Sector[]> levels = new ArrayList<>();
    private final List<SectorConfig.Sector> owners = new ArrayList<>();

    private int lastHover = -1;

    public RadialMenuScreen() {
        super(Text.translatable("zd.menu.title"));
        levels.add(SectorConfig.sectors);
        owners.add(null);
    }

    public static void open() {
        MinecraftClient.getInstance().setScreen(new RadialMenuScreen());
    }


    public static void openAt(int owner) {
        net.minecraft.client.MinecraftClient.getInstance()
                .setScreen(new RadialMenuEditorScreen(null, owner));
    }


    private static String tr(String key, Object... args) {
        return Text.translatable(key, args).getString();
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

    private boolean submenuSeparate() {
        return levels.size() > 1 && SectorConfig.submenuMode.equals("SEPARATE");
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        MenuSkin skin = MenuSkin.current();
        MenuSkin.inSubmenu = levels.size() > 1;
        MenuSkin.compactOuter = levels.size() > 1 && SectorConfig.submenuMode.equals("COMPACT");

        int cx = width / 2;
        int cy = height / 2;
        SectorConfig.Sector[] cur = current();
        int radius = layoutRadius();
        int selected = skin.pickIndex(mouseX, mouseY, cur.length, cx, cy, radius);

        if (selected != lastHover) {
            lastHover = selected;
            if (selected >= 0) Sounds.hover();
        }

        int parentSel = -1;
        if (skin.hasOuterRing() && levels.size() > 1 && !submenuSeparate()) {
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

            if (i == selected && SectorConfig.hoverGlow) {
                skin.drawAt(context, i, cur.length, cx, cy, radius, GLOW_OVERLAY);
            }

            SectorConfig.Sector s = cur[i];
            ItemStack icon = SectorConfig.iconStack(s);
            int midX = p[0] + p[2] / 2;
            int midY = p[1] + p[3] / 2;

            if (!icon.isEmpty()) {
                context.drawItem(icon, p[0] + (p[2] - 16) / 2, p[1] + (p[3] - 16) / 2);
            } else if (s.icon != null && !s.icon.trim().isEmpty()) {
                context.drawCenteredTextWithShadow(textRenderer, Text.literal("?"), midX, midY - 4, COL_ERROR);
            } else if (!s.name.isEmpty()) {
                context.drawCenteredTextWithShadow(textRenderer, Text.literal(s.name), midX, midY - 4, COL_TEXT);
            } else if (SectorConfig.emptyNumbers) {
                context.drawCenteredTextWithShadow(textRenderer,
                        Text.literal(String.valueOf(i + 1)), midX, midY - 4, COL_TEXT);
            }

            if (!s.name.isEmpty()) {
                context.drawCenteredTextWithShadow(textRenderer, Text.literal(s.name),
                        midX, p[1] + p[3] + 2, COL_TEXT);
            }
        }

        int[] info = skin.infoPos(cur.length, cx, cy, radius);

        if (selected >= 0) {
            SectorConfig.Sector hs = cur[selected];
            String nm = hs.name.isEmpty()
                    ? tr("zd.slot.fallback", selected + 1) : hs.name;
            context.drawCenteredTextWithShadow(textRenderer, Text.literal(nm),
                    info[0], info[1] - 10, COL_TEXT);

            String action = switch (hs.type) {
                case "key" -> tr("zd.menu.key_action",
                        Text.translatable(hs.keyBinding).getString());
                case "submenu" -> tr("zd.menu.submenu_action");
                default -> hs.command.trim();
            };
            if (!action.isEmpty()) {
                context.drawCenteredTextWithShadow(textRenderer, Text.literal(action),
                        info[0], info[1] + 4, COL_ACTION);
            }
        } else if (parentSel >= 0) {
            SectorConfig.Sector[] parent = levels.get(levels.size() - 2);
            SectorConfig.Sector ps = parent[parentSel];
            String nm = ps.name.isEmpty()
                    ? tr("zd.slot.fallback", parentSel + 1) : ps.name;
            context.drawCenteredTextWithShadow(textRenderer,
                    Text.translatable("zd.menu.parent_back", nm),
                    info[0], info[1] - 5, COL_TEXT);
        } else if (levels.size() > 1) {
            String hint = (skin != MenuSkin.CLASSIC || submenuSeparate())
                    ? tr("zd.menu.hint_outside") : tr("zd.menu.hint_center");
            context.drawCenteredTextWithShadow(textRenderer,
                    Text.literal(hint), info[0], info[1] - 5, COL_HINT);
        }

        boolean hover = mouseOnSettings(mouseX, mouseY);
        context.fill(settingsX(), settingsY(), settingsX() + BTN_W, settingsY() + BTN_H,
                hover ? COL_HOVER : COL_SECTOR);
        context.drawCenteredTextWithShadow(textRenderer, Text.translatable("zd.menu.settings"),
                settingsX() + BTN_W / 2, settingsY() + (BTN_H - 8) / 2, COL_TEXT);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        int mx = (int) click.x();
        int my = (int) click.y();

        MenuSkin skin = MenuSkin.current();
        MenuSkin.inSubmenu = levels.size() > 1;
        MenuSkin.compactOuter = levels.size() > 1 && SectorConfig.submenuMode.equals("COMPACT");

        if (mouseOnSettings(mx, my)) {
            Sounds.select();
            RadialMenuEditorScreen.openAt(-1);
            return true;
        }

        SectorConfig.Sector[] cur = current();
        int cx = width / 2;
        int cy = height / 2;
        int radius = layoutRadius();

        if (skin.hasOuterRing() && levels.size() > 1 && !submenuSeparate()) {
            SectorConfig.Sector[] parent = levels.get(levels.size() - 2);
            if (skin.pickOuterIndex(mx, my, parent.length, cx, cy, radius) >= 0) {
                Sounds.closeSubmenu();
                levels.remove(levels.size() - 1);
                owners.remove(owners.size() - 1);
                return true;
            }
        }

        int selected = skin.pickIndex(mx, my, cur.length, cx, cy, radius);

        if (selected < 0) {
            if (levels.size() > 1) {
                Sounds.closeSubmenu();
                levels.remove(levels.size() - 1);
                owners.remove(owners.size() - 1);
            } else {
                close();
            }
            return true;
        }

        SectorConfig.Sector s = cur[selected];

        if (click.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            Sounds.select();
            MinecraftClient.getInstance().setScreen(
                    new SlotEditorScreen(null, cur, selected, currentOwner()));
            return true;
        }

        if (click.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            switch (s.type) {
                case "submenu":
                    if (s.children != null && s.children.length > 0) {
                        Sounds.openSubmenu();
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
                    Sounds.select();
                    ZdRadialMenuClient.pulseKey(KeyBinding.byId(s.keyBinding));
                    close();
                    return true;

                default: // command
                    Sounds.select();
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
