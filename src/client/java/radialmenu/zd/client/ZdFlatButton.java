package radialmenu.zd.client;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public class ZdFlatButton {
    public final String label;
    public final int x, y, w, h;
    private final Runnable action;

    public ZdFlatButton(String label, int x, int y, int w, int h, Runnable action) {
        this.label = label;
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.action = action;
    }

    public boolean contains(double mx, double my) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    public void click() {
        if (action != null) action.run();
    }

    public void render(DrawContext context, TextRenderer tr, int mouseX, int mouseY) {
        boolean hover = contains(mouseX, mouseY);
        int fill = hover ? 0xC0222222 : 0x98141414;   // тёмно-прозрачная подложка
        int border = hover ? 0xFFFFFFFF : 0xFFB8B8B8; // белая обводка
        context.fill(x, y, x + w, y + h, fill);
        context.fill(x, y, x + w, y + 1, border);
        context.fill(x, y + h - 1, x + w, y + h, border);
        context.fill(x, y, x + 1, y + h, border);
        context.fill(x + w - 1, y, x + w, y + h, border);
        context.drawCenteredTextWithShadow(tr, Text.literal(label), x + w / 2, y + (h - 8) / 2, 0xFFFFFFFF);
    }
}
