package radialmenu.zd.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

public class RadialLayoutEditorScreen extends Screen {
    private int selected = 0;
    private boolean dragging = false;
    private int grabOffX;
    private int grabOffY;

    private final List<ZdFlatButton> buttons = new ArrayList<>();

    public RadialLayoutEditorScreen() {
        super(Text.literal("Вид «Свой»: раскладка кнопок"));
    }

    public static void open() {
        SectorConfig.ensureCustomCells();
        MinecraftClient.getInstance().setScreen(new RadialLayoutEditorScreen());
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private int previewCx() {
        return width / 2;
    }

    private int previewCy() {
        return height / 2 + 70;
    }

    private void zdBtn(String label, int x, int y, int w, int h, Runnable action) {
        buttons.add(new ZdFlatButton(label, x, y, w, h, action));
    }

    @Override
    public void init() {
        buttons.clear();
        if (selected >= SectorConfig.customLayout.length) {
            selected = SectorConfig.customLayout.length - 1;
        }
        if (selected < 0) selected = 0;

        SectorConfig.CustomCell c = SectorConfig.customLayout[selected];
        int x = width / 2 - 155;

        zdBtn("← Назад", x, height - 30, 100, 20, this::close);

        zdBtn("‹", x, 30, 40, 20, () -> {
            selected = (selected - 1 + SectorConfig.customLayout.length) % SectorConfig.customLayout.length;
            clearAndInit();
        });

        zdBtn("›", x + 270, 30, 40, 20, () -> {
            selected = (selected + 1) % SectorConfig.customLayout.length;
            clearAndInit();
        });

        addDrawableChild(field(x, 66, c.dx, v -> c.dx = v));
        addDrawableChild(field(x + 160, 66, c.dy, v -> c.dy = v));
        addDrawableChild(field(x, 106, c.w, v -> { if (v >= 4) c.w = v; }));
        addDrawableChild(field(x + 160, 106, c.h, v -> { if (v >= 4) c.h = v; }));
        addDrawableChild(colorField(x, 146, c.color, v -> c.color = v));

        zdBtn("Форма: " + shapeLabel(c.shape), x, 206, 310, 20, () -> {
            c.shape = nextShape(c.shape);
            SectorConfig.save();
            clearAndInit();
        });

        zdBtn("↺ Сброс раскладки", x, height - 108, 310, 20, () -> {
            SectorConfig.CustomCell[] arr = new SectorConfig.CustomCell[SectorConfig.customLayout.length];
            for (int i = 0; i < arr.length; i++) {
                arr[i] = MenuSkin.defaultCell(i, arr.length);
            }
            SectorConfig.customLayout = arr;
            selected = 0;
            SectorConfig.save();
            clearAndInit();
        });

        zdBtn("＋ Добавить ячейку", x, height - 56, 310, 20, () -> {
            if (SectorConfig.customLayout.length < 12) {
                SectorConfig.customLayout = java.util.Arrays.copyOf(
                        SectorConfig.customLayout, SectorConfig.customLayout.length + 1);
                SectorConfig.customLayout[SectorConfig.customLayout.length - 1] =
                        MenuSkin.defaultCell(SectorConfig.customLayout.length - 1, SectorConfig.customLayout.length);
                selected = SectorConfig.customLayout.length - 1;
                SectorConfig.save();
                clearAndInit();
            }
        });

        zdBtn("− Удалить ячейку", x, height - 82, 310, 20, () -> {
            if (SectorConfig.customLayout.length > 1) {
                SectorConfig.CustomCell[] arr =
                        new SectorConfig.CustomCell[SectorConfig.customLayout.length - 1];
                int k = 0;
                for (int i = 0; i < SectorConfig.customLayout.length; i++) {
                    if (i != selected) arr[k++] = SectorConfig.customLayout[i];
                }
                SectorConfig.customLayout = arr;
                if (selected >= arr.length) selected = arr.length - 1;
                SectorConfig.save();
                clearAndInit();
            }
        });
    }

    private TextFieldWidget field(int x, int y, int value, IntConsumer setter) {
        TextFieldWidget f = new TextFieldWidget(MinecraftClient.getInstance().textRenderer, x, y, 150, 20, Text.literal(""));
        f.setText(String.valueOf(value));
        f.setChangedListener(t -> {
            try {
                setter.accept(Integer.parseInt(t.trim()));
            } catch (NumberFormatException ignored) {
            }
        });
        return f;
    }

    private TextFieldWidget colorField(int x, int y, int value, IntConsumer setter) {
        TextFieldWidget f = new TextFieldWidget(MinecraftClient.getInstance().textRenderer, x, y, 150, 20, Text.literal("ARGB"));
        f.setMaxLength(10);
        f.setText(String.format("%08X", value));
        f.setChangedListener(t -> {
            try {
                String s = t.trim();
                if (s.startsWith("0x") || s.startsWith("0X")) s = s.substring(2);
                if (s.startsWith("#")) s = s.substring(1);
                setter.accept((int) (Long.parseLong(s, 16) & 0xFFFFFFFFL));
            } catch (NumberFormatException ignored) {
            }
        });
        return f;
    }

    private static String shapeLabel(String s) {
        return switch (s == null ? "rect" : s) {
            case "hex" -> "гекс";
            case "disc" -> "круг";
            case "diamond" -> "ромб";
            case "triangle" -> "треугольник";
            default -> "квадрат";
        };
    }

    private static String nextShape(String s) {
        return switch (s == null ? "rect" : s) {
            case "rect" -> "hex";
            case "hex" -> "disc";
            case "disc" -> "diamond";
            case "diamond" -> "triangle";
            default -> "rect";
        };
    }

    private int swatchX(int i) {
        return width / 2 - 155 + i * 26;
    }

    private static final int SWATCH_Y = 176;
    private static final int SWATCH_SIZE = 24;

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        int x = width / 2 - 155;
        context.drawCenteredTextWithShadow(textRenderer, getTitle(), width / 2, 8, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal("Ячейка " + (selected + 1) + " из " + SectorConfig.customLayout.length),
                width / 2, 36, 0xFFFFFF55);

        // подсказка наверху — не наезжает на кнопки
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal(dragging ? "отпустите кнопку мыши, чтобы поставить"
                        : "клик по кнопке в превью — перетащить"),
                width / 2, 46, 0xFFFFFF55);

        context.drawTextWithShadow(textRenderer, Text.literal("X (сдвиг от центра):"), x, 60, 0xFFA0A0A0);
        context.drawTextWithShadow(textRenderer, Text.literal("Y (сдвиг от центра):"), x + 160, 60, 0xFFA0A0A0);
        context.drawTextWithShadow(textRenderer, Text.literal("Ширина:"), x, 100, 0xFFA0A0A0);
        context.drawTextWithShadow(textRenderer, Text.literal("Высота:"), x + 160, 100, 0xFFA0A0A0);
        context.drawTextWithShadow(textRenderer, Text.literal("Цвет (ARGB) или палитра:"), x, 140, 0xFFA0A0A0);

        for (int i = 0; i < MenuSkin.SWATCHES.length; i++) {
            int sx = swatchX(i);
            context.fill(sx, SWATCH_Y, sx + SWATCH_SIZE, SWATCH_Y + SWATCH_SIZE, MenuSkin.SWATCHES[i]);
            int border = SectorConfig.customLayout[selected].color == MenuSkin.SWATCHES[i]
                    ? 0xFFFFFFFF : 0xFF000000;
            context.fill(sx - 1, SWATCH_Y - 1, sx + SWATCH_SIZE + 1, SWATCH_Y, border);
            context.fill(sx - 1, SWATCH_Y + SWATCH_SIZE, sx + SWATCH_SIZE + 1, SWATCH_Y + SWATCH_SIZE + 1, border);
            context.fill(sx - 1, SWATCH_Y, sx, SWATCH_Y + SWATCH_SIZE, border);
            context.fill(sx + SWATCH_SIZE, SWATCH_Y, sx + SWATCH_SIZE + 1, SWATCH_Y + SWATCH_SIZE, border);
        }

        if (dragging) {
            SectorConfig.CustomCell c = SectorConfig.customLayout[selected];
            c.dx = mouseX - grabOffX - previewCx();
            c.dy = mouseY - grabOffY - previewCy();
            long handle = MinecraftClient.getInstance().getWindow().getHandle();
            if (GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_RELEASE) {
                dragging = false;
                SectorConfig.save();
            }
        }

        MenuSkin sk = MenuSkin.CUSTOM;
        int cx = previewCx();
        int cy = previewCy();
        for (int i = 0; i < SectorConfig.customLayout.length; i++) {
            int[] p = sk.sectorTopLeft(i, SectorConfig.customLayout.length, cx, cy, 0);
            sk.drawSector(context, i, SectorConfig.customLayout.length,
                    p[0], p[1], p[2], p[3], i == selected ? MenuSkin.HOVER_MARK : 0);
            context.drawCenteredTextWithShadow(textRenderer, Text.literal(String.valueOf(i + 1)),
                    p[0] + p[2] / 2, p[1] + p[3] / 2 - 4, 0xFFFFFFFF);
        }

        for (ZdFlatButton b : buttons) {
            b.render(context, textRenderer, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        int mx = (int) click.x();
        int my = (int) click.y();

        // наши кнопки — первыми
        for (ZdFlatButton b : buttons) {
            if (b.contains(mx, my)) {
                b.click();
                return true;
            }
        }

        if (dragging) {
            dragging = false;
            SectorConfig.save();
            return true;
        }

        if (my >= SWATCH_Y - 1 && my < SWATCH_Y + SWATCH_SIZE + 1) {
            for (int i = 0; i < MenuSkin.SWATCHES.length; i++) {
                int sx = swatchX(i);
                if (mx >= sx - 1 && mx < sx + SWATCH_SIZE + 1) {
                    SectorConfig.customLayout[selected].color = MenuSkin.SWATCHES[i];
                    SectorConfig.save();
                    return true;
                }
            }
        }

        if (click.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            int cx = previewCx();
            int cy = previewCy();
            for (int i = 0; i < SectorConfig.customLayout.length; i++) {
                int[] p = MenuSkin.CUSTOM.sectorTopLeft(i, SectorConfig.customLayout.length, cx, cy, 0);
                if (mx >= p[0] && mx < p[0] + p[2] && my >= p[1] && my < p[1] + p[3]) {
                    selected = i;
                    grabOffX = mx - (p[0] + p[2] / 2);
                    grabOffY = my - (p[1] + p[3] / 2);
                    dragging = true;
                    return true;
                }
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
        SectorConfig.save();
        super.close();
    }
}
