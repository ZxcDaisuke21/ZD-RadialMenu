package radialmenu.zd.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;

public enum MenuSkin {
    CLASSIC("Классика") {
        @Override
        public int[] sectorTopLeft(int i, int count, int cx, int cy, int radius) {
            double angle = 2 * Math.PI * i / count - Math.PI / 2;
            int size = 24;
            return new int[]{
                    cx + (int) Math.round(radius * Math.cos(angle)) - size / 2,
                    cy + (int) Math.round(radius * Math.sin(angle)) - size / 2,
                    size, size};
        }

        @Override
        public void drawSector(DrawContext ctx, int i, int count, int x, int y, int w, int h, int color) {
            ctx.fill(x, y, x + w, y + h, color);
        }

        @Override
        public int pickIndex(int mx, int my, int count, int cx, int cy, int radius) {
            double dx = mx - cx;
            double dy = my - cy;
            if (dx * dx + dy * dy < 30 * 30) return -1;
            double a = normAngle(dx, dy);
            return (int) (a / (2 * Math.PI / count)) % count;
        }
    },
    HEX("Гексы") {
        @Override
        public int[] sectorTopLeft(int i, int count, int cx, int cy, int radius) {
            int r = hexRadius(count, radius);
            double step = 2.0 * r + 4;
            double centerX = cx + (i - (count - 1) / 2.0) * step;
            int h = hexHeight(r);
            return new int[]{(int) Math.round(centerX - r), cy - h / 2, 2 * r, h};
        }

        @Override
        public void drawSector(DrawContext ctx, int i, int count, int x, int y, int w, int h, int color) {
            double cx = x + w / 2.0;
            double cy = y + h / 2.0;
            double r = w / 2.0;
            drawHex(ctx, cx, cy, r + 3, 0xFF000000);
            drawHex(ctx, cx, cy, r, color);
        }

        @Override
        public int pickIndex(int mx, int my, int count, int cx, int cy, int radius) {
            return boxPick(mx, my, count, cx, cy, radius);
        }

        @Override
        public int[] infoPos(int count, int cx, int cy, int radius) {
            int r = hexRadius(count, radius);
            return new int[]{cx, cy + hexHeight(r) / 2 + 12};
        }

        @Override
        public int layoutRadius(int count, int w, int h) {
            return (w - 80) / 2;
        }
    },
    CUSTOM("Свой") {
        @Override
        public int[] sectorTopLeft(int i, int count, int cx, int cy, int radius) {
            SectorConfig.CustomCell c = cell(i, count);
            return new int[]{cx + c.dx, cy + c.dy, c.w, c.h};
        }

        @Override
        public void drawSector(DrawContext ctx, int i, int count, int x, int y, int w, int h, int color) {
            SectorConfig.CustomCell c = cell(i, count);
            boolean hover = color == HOVER_MARK;
            String shape = c.shape == null ? "rect" : c.shape;
            switch (shape) {
                case "hex" -> {
                    double ccx = x + w / 2.0;
                    double ccy = y + h / 2.0;
                    double r = Math.min(w, h) / 2.0;
                    drawHex(ctx, ccx, ccy, r + 2, 0xFF000000);
                    drawHex(ctx, ccx, ccy, r, c.color);
                    if (hover) drawHex(ctx, ccx, ccy, r, 0x40FFFFFF);
                }
                case "disc" -> {
                    double ccx = x + w / 2.0;
                    double ccy = y + h / 2.0;
                    double r = Math.min(w, h) / 2.0;
                    drawDisc(ctx, ccx, ccy, r + 1, 0x30000000);
                    drawDisc(ctx, ccx, ccy, r, c.color);
                    if (hover) drawDisc(ctx, ccx, ccy, r, 0x50FFFFFF);
                }
                case "diamond" -> {
                    double ccx = x + w / 2.0;
                    double ccy = y + h / 2.0;
                    double r = Math.min(w, h) / 2.0;
                    drawDiamond(ctx, ccx, ccy, r + 2, 0xFFFFFFFF);
                    drawDiamond(ctx, ccx, ccy, r, c.color);
                    if (hover) drawDiamond(ctx, ccx, ccy, r, 0x40FFFFFF);
                }
                case "triangle" -> {
                    double ccx = x + w / 2.0;
                    double ccy = y + h / 2.0;
                    double r = Math.min(w, h) / 2.0;
                    drawTriangle(ctx, ccx, ccy, r + 2, 0xFFFFFFFF);
                    drawTriangle(ctx, ccx, ccy, r, c.color);
                    if (hover) drawTriangle(ctx, ccx, ccy, r, 0x40FFFFFF);
                }
                default -> {
                    ctx.fill(x, y, x + w, y + h, c.color);
                    if (hover) ctx.fill(x, y, x + w, y + h, 0x40FFFFFF);
                }
            }
        }

        @Override
        public int pickIndex(int mx, int my, int count, int cx, int cy, int radius) {
            return boxPick(mx, my, count, cx, cy, radius);
        }
    },
    RING("Кольцо") {
        @Override
        public int[] sectorTopLeft(int i, int count, int cx, int cy, int radius) {
            double angle = 2 * Math.PI * i / count - Math.PI / 2;
            int size = 26;
            return new int[]{
                    cx + (int) Math.round(radius * Math.cos(angle)) - size / 2,
                    cy + (int) Math.round(radius * Math.sin(angle)) - size / 2,
                    size, size};
        }

        @Override
        public void drawBackground(DrawContext ctx, int count, int cx, int cy, int radius) {
            drawDisc(ctx, cx, cy, radius - 22, 0xE01C1C1C);
        }

        @Override
        public void drawSector(DrawContext ctx, int i, int count, int x, int y, int w, int h, int color) {
            int r = w / 2;
            drawDisc(ctx, x + r, y + h / 2, r + 1, 0xFF000000);
            drawDisc(ctx, x + r, y + h / 2, r, color);
        }

        @Override
        public int pickIndex(int mx, int my, int count, int cx, int cy, int radius) {
            double dx = mx - cx;
            double dy = my - cy;
            if (dx * dx + dy * dy < 30 * 30) return -1;
            double a = normAngle(dx, dy);
            return (int) (a / (2 * Math.PI / count)) % count;
        }

        @Override
        public int[] infoPos(int count, int cx, int cy, int radius) {
            return new int[]{cx, cy - 4};
        }

        @Override
        public int layoutRadius(int count, int w, int h) {
            return Math.max(85, count * 9);
        }
    },
    BUBBLES("Пузыри") {
        @Override
        public int[] sectorTopLeft(int i, int count, int cx, int cy, int radius) {
            double angle = 2 * Math.PI * i / count - Math.PI / 2;
            int size = 40;
            return new int[]{
                    cx + (int) Math.round(radius * Math.cos(angle)) - size / 2,
                    cy + (int) Math.round(radius * Math.sin(angle)) - size / 2,
                    size, size};
        }

        @Override
        public void drawSector(DrawContext ctx, int i, int count, int x, int y, int w, int h, int color) {
            int r = w / 2;
            int bx = x + r;
            int by = y + h / 2;
            drawDisc(ctx, bx, by, r + 1, 0x30000000);
            drawDisc(ctx, bx, by, r, PALETTE[i % PALETTE.length]);
            if (color == HOVER_MARK) {
                drawDisc(ctx, bx, by, r, 0x50FFFFFF);
            }
        }

        @Override
        public int pickIndex(int mx, int my, int count, int cx, int cy, int radius) {
            for (int i = 0; i < count; i++) {
                int[] p = sectorTopLeft(i, count, cx, cy, radius);
                int dx = mx - (p[0] + p[2] / 2);
                int dy = my - (p[1] + p[3] / 2);
                int r = p[2] / 2;
                if (dx * dx + dy * dy <= r * r) return i;
            }
            return -1;
        }

        @Override
        public int layoutRadius(int count, int w, int h) {
            return 75;
        }
    },
    ROMB("Ромбы") {
        @Override
        public int[] sectorTopLeft(int i, int count, int cx, int cy, int radius) {
            double angle = 2 * Math.PI * i / count - Math.PI / 2;
            int size = 36;
            return new int[]{
                    cx + (int) Math.round(radius * Math.cos(angle)) - size / 2,
                    cy + (int) Math.round(radius * Math.sin(angle)) - size / 2,
                    size, size};
        }

        @Override
        public void drawSector(DrawContext ctx, int i, int count, int x, int y, int w, int h, int color) {
            double ccx = x + w / 2.0;
            double ccy = y + h / 2.0;
            double r = Math.min(w, h) / 2.0;
            drawDiamond(ctx, ccx, ccy, r + 1, 0xFFFFFFFF);
            drawDiamond(ctx, ccx, ccy, r, 0xC81C1C1C);
            if (color == HOVER_MARK) drawDiamond(ctx, ccx, ccy, r, 0x50FFFFFF);
        }

        @Override
        public int pickIndex(int mx, int my, int count, int cx, int cy, int radius) {
            return boxPick(mx, my, count, cx, cy, radius);
        }
    },
    TRI("Треугольники") {
        @Override
        public int[] sectorTopLeft(int i, int count, int cx, int cy, int radius) {
            double angle = 2 * Math.PI * i / count - Math.PI / 2;
            int size = 40;
            return new int[]{
                    cx + (int) Math.round(radius * Math.cos(angle)) - size / 2,
                    cy + (int) Math.round(radius * Math.sin(angle)) - size / 2,
                    size, size};
        }

        @Override
        public void drawSector(DrawContext ctx, int i, int count, int x, int y, int w, int h, int color) {
            double ccx = x + w / 2.0;
            double ccy = y + h / 2.0;
            double r = Math.min(w, h) / 2.0;
            drawTriangle(ctx, ccx, ccy, r + 2, 0xFFFFFFFF);
            drawTriangle(ctx, ccx, ccy, r, 0xC81C1C1C);
            if (color == HOVER_MARK) drawTriangle(ctx, ccx, ccy, r, 0x50FFFFFF);
        }

        @Override
        public int pickIndex(int mx, int my, int count, int cx, int cy, int radius) {
            return boxPick(mx, my, count, cx, cy, radius);
        }
    },
    DONUT("Сегменты") {
        static final int SEG_COLOR = 0xC81C1C1C;
        static final int SEG_HOVER = 0xFF4A4A4A;

        @Override
        public int[] sectorTopLeft(int i, int count, int cx, int cy, int radius) {
            int rIn = innerRadius(radius);
            int rOut = outerRadius(count, radius);
            int rMid = midRadius(rIn, rOut);
            double rr = inSubmenu ? (rMid + rOut) / 2.0 : (rIn + rOut) / 2.0;
            double theta = (i + 0.5) * 2 * Math.PI / count - Math.PI / 2;
            int px = cx + (int) Math.round(rr * Math.cos(theta));
            int py = cy + (int) Math.round(rr * Math.sin(theta));
            return new int[]{px - 8, py - 8, 16, 16};
        }

        @Override
        public void drawAt(DrawContext ctx, int i, int count, int cx, int cy, int radius, int color) {
            int rIn = innerRadius(radius);
            int rOut = outerRadius(count, radius);
            int rMid = midRadius(rIn, rOut);
            double step = 2 * Math.PI / count;
            double gap = count > 1 ? 0.05 : 0;
            double a0 = i * step + gap / 2;
            double a1 = (i + 1) * step - gap / 2;
            int fill = color == HOVER_MARK ? SEG_HOVER : SEG_COLOR;
            if (inSubmenu) {
                drawWedge(ctx, cx, cy, rMid, rOut, a0, a1, fill);
            } else {
                drawWedge(ctx, cx, cy, rIn, rOut, a0, a1, fill);
            }
        }

        @Override
        public void drawSector(DrawContext ctx, int i, int count, int x, int y, int w, int h, int color) {
            // сегменты рисуются в drawAt — здесь пусто
        }

        @Override
        public int pickIndex(int mx, int my, int count, int cx, int cy, int radius) {
            double dx = mx - cx;
            double dy = my - cy;
            double d2 = dx * dx + dy * dy;
            int rIn = innerRadius(radius);
            int rOut = outerRadius(count, radius);
            int rMid = midRadius(rIn, rOut);
            if (inSubmenu) {
                if (d2 < (double) rMid * rMid || d2 > (double) rOut * rOut) return -1;
            } else {
                if (d2 < (double) rIn * rIn || d2 > (double) rOut * rOut) return -1;
            }
            return angleIndex(dx, dy, count);
        }

        @Override
        public boolean hasOuterRing() {
            return true;
        }

        @Override
        public int pickOuterIndex(int mx, int my, int count, int cx, int cy, int radius) {
            double dx = mx - cx;
            double dy = my - cy;
            double d2 = dx * dx + dy * dy;
            int rIn = innerRadius(radius);
            int rOut = outerRadius(count, radius);
            int rMid = midRadius(rIn, rOut);
            if (d2 < (double) rIn * rIn || d2 >= (double) rMid * rMid) return -1;
            return angleIndex(dx, dy, count);
        }

        @Override
        public void drawOuter(DrawContext ctx, int i, int count, int cx, int cy, int radius,
                              boolean hover, ItemStack icon) {
            int rIn = innerRadius(radius);
            int rOut = outerRadius(count, radius);
            int rMid = midRadius(rIn, rOut);
            double step = 2 * Math.PI / count;
            double gap = count > 1 ? 0.05 : 0;
            double a0 = i * step + gap / 2;
            double a1 = (i + 1) * step - gap / 2;
            drawWedge(ctx, cx, cy, rIn, rMid, a0, a1, PALETTE[i % PALETTE.length]);
            if (hover) {
                drawWedge(ctx, cx, cy, rIn, rMid, a0, a1, 0x40FFFFFF);
            }
            if (icon != null && !icon.isEmpty()) {
                double rr = (rIn + rMid) / 2.0;
                double theta = (i + 0.5) * 2 * Math.PI / count - Math.PI / 2;
                int px = cx + (int) Math.round(rr * Math.cos(theta));
                int py = cy + (int) Math.round(rr * Math.sin(theta));
                ctx.drawItem(icon, px - 8, py - 8);
            }
        }

        @Override
        public int[] infoPos(int count, int cx, int cy, int radius) {
            return new int[]{cx, cy - 4};
        }

        @Override
        public int labelColor() {
            return 0xFFFF5555;
        }

        @Override
        public int layoutRadius(int count, int w, int h) {
            return Math.max(70, Math.min(Math.min(w, h) / 2 - 40, 110));
        }

        private int angleIndex(double dx, double dy, int count) {
            double step = 2 * Math.PI / count;
            double gap = count > 1 ? 0.05 : 0;
            double a = normAngle(dx, dy);
            int idx = (int) Math.floor(a / step);
            if (idx < 0) idx += count;
            double local = a - idx * step;
            if (local < gap / 2 || local > step - gap / 2) return -1;
            return idx % count;
        }
    };

    public static final int HOVER_MARK = 0x80FFFFFF;

    public static final int[] PALETTE = {
            0xFF4FA3E3, 0xFF3BC46B, 0xFFFF8A2A, 0xFFE8434F,
            0xFF9B59D0, 0xFF39C2D7, 0xFFF2C12E, 0xFF5B6B8C
    };

    public static final int[] SWATCHES = {
            0x60808080, 0x40202020, 0xC81C1C1C, 0x80FFFFFF,
            0xFFFF5555, 0xFFE8434F, 0xFFFF8A2A, 0xFFF2C12E,
            0xFF3BC46B, 0xFF39C2D7, 0xFF4FA3E3, 0xFF9B59D0
    };

    // экран выставляет: true, когда открыт уровень подменю
    public static boolean inSubmenu = false;

    public final String label;

    MenuSkin(String label) {
        this.label = label;
    }

    public abstract int[] sectorTopLeft(int i, int count, int cx, int cy, int radius);

    public abstract void drawSector(DrawContext ctx, int i, int count, int x, int y, int w, int h, int color);

    public void drawAt(DrawContext ctx, int i, int count, int cx, int cy, int radius, int color) {
        int[] p = sectorTopLeft(i, count, cx, cy, radius);
        drawSector(ctx, i, count, p[0], p[1], p[2], p[3], color);
    }

    public void drawBackground(DrawContext ctx, int count, int cx, int cy, int radius) {
    }

    public abstract int pickIndex(int mx, int my, int count, int cx, int cy, int radius);

    public boolean hasOuterRing() {
        return false;
    }

    public int pickOuterIndex(int mx, int my, int count, int cx, int cy, int radius) {
        return -1;
    }

    public void drawOuter(DrawContext ctx, int i, int count, int cx, int cy, int radius,
                          boolean hover, ItemStack icon) {
    }

    public int[] infoPos(int count, int cx, int cy, int radius) {
        return new int[]{cx, cy};
    }

    public int layoutRadius(int count, int w, int h) {
        return count > 6 ? 90 : 70;
    }

    public int labelColor() {
        return 0xFFE0E0E0;
    }

    public static MenuSkin current() {
        return byId(SectorConfig.skin);
    }

    public static MenuSkin byId(String id) {
        if (id != null) {
            for (MenuSkin s : values()) {
                if (s.name().equalsIgnoreCase(id)) return s;
            }
        }
        return CLASSIC;
    }

    // нормализованный угол: 0 = вверх, растёт по часовой — единая для рисования и наведения
    static double normAngle(double dx, double dy) {
        double a = Math.atan2(dy, dx) + Math.PI / 2;
        a %= 2 * Math.PI;
        if (a < 0) a += 2 * Math.PI;
        return a;
    }

    static int boxPick(int mx, int my, int count, int cx, int cy, int radius) {
        for (int i = 0; i < count; i++) {
            int[] p = MenuSkin.current().sectorTopLeft(i, count, cx, cy, radius);
            if (mx >= p[0] && mx < p[0] + p[2] && my >= p[1] && my < p[1] + p[3]) return i;
        }
        return -1;
    }

    static SectorConfig.CustomCell cell(int i, int count) {
        SectorConfig.CustomCell[] cells = SectorConfig.customLayout;
        if (cells != null && i < cells.length && cells[i] != null) return cells[i];
        return defaultCell(i, count);
    }

    static SectorConfig.CustomCell defaultCell(int i, int count) {
        SectorConfig.CustomCell c = new SectorConfig.CustomCell();
        c.dx = (int) Math.round((i - (count - 1) / 2.0) * 70.0);
        c.dy = -40;
        c.w = 56;
        c.h = 48;
        c.color = 0x60808080;
        c.shape = "hex";
        return c;
    }

    static int hexRadius(int count, int halfWidth) {
        double r = (2.0 * halfWidth - 4.0 * (count - 1) - 16.0) / (2.0 * count);
        return Math.max(12, (int) Math.min(30, r));
    }

    static int hexHeight(int r) {
        return Math.max(16, (int) Math.round(r * 1.7320508));
    }

    static int innerRadius(int layoutRadius) {
        return Math.max(24, (int) (layoutRadius * 0.45));
    }

    static int outerRadius(int count, int layoutRadius) {
        return layoutRadius;
    }

    static int midRadius(int rIn, int rOut) {
        return (rIn + rOut) / 2;
    }

    static void drawDisc(DrawContext ctx, double cx, double cy, double r, int color) {
        if (r < 1) return;
        int yTop = (int) Math.floor(cy - r);
        int yBottom = (int) Math.ceil(cy + r);
        for (int y = yTop; y < yBottom; y++) {
            double dy = (y + 0.5) - cy;
            double w = Math.sqrt(Math.max(0, r * r - dy * dy));
            int x1 = (int) Math.round(cx - w);
            int x2 = (int) Math.round(cx + w);
            if (x2 > x1) {
                ctx.fill(x1, y, x2, y + 1, color);
            }
        }
    }

    static void drawWedge(DrawContext ctx, int cx, int cy, int rIn, int rOut, double a0, double a1, int color) {
        double span = a1 - a0;
        boolean useCross = span > 1e-6 && span < Math.PI * 0.999;
        double t0 = a0 - Math.PI / 2;
        double t1 = a1 - Math.PI / 2;
        double b0x = Math.cos(t0), b0y = Math.sin(t0);
        double b1x = Math.cos(t1), b1y = Math.sin(t1);
        for (int y = cy - rOut; y < cy + rOut; y++) {
            double dy = y + 0.5 - cy;
            if (Math.abs(dy) > rOut) continue;
            int runStart = Integer.MIN_VALUE;
            for (int x = cx - rOut; x <= cx + rOut; x++) {
                double dx = x + 0.5 - cx;
                double d2 = dx * dx + dy * dy;
                boolean in = d2 >= (double) rIn * rIn && d2 <= (double) rOut * rOut;
                if (in && useCross) {
                    in = (b0x * dy - b0y * dx) >= 0 && (b1x * dy - b1y * dx) <= 0;
                } else if (in) {
                    in = normAngle(dx, dy) >= a0 && normAngle(dx, dy) <= a1;
                }
                if (in && runStart == Integer.MIN_VALUE) {
                    runStart = x;
                } else if (!in && runStart != Integer.MIN_VALUE) {
                    ctx.fill(runStart, y, x, y + 1, color);
                    runStart = Integer.MIN_VALUE;
                }
            }
            if (runStart != Integer.MIN_VALUE) {
                ctx.fill(runStart, y, cx + rOut, y + 1, color);
            }
        }
    }

    // ромб (квадрат на 45°): ширина строки линейно убывает от середины к углам
    static void drawDiamond(DrawContext ctx, double cx, double cy, double r, int color) {
        if (r < 1) return;
        int yTop = (int) Math.floor(cy - r);
        int yBottom = (int) Math.ceil(cy + r);
        for (int y = yTop; y < yBottom; y++) {
            double dy = Math.abs(y + 0.5 - cy);
            if (dy > r) continue;
            double w = r - dy;
            int x1 = (int) Math.round(cx - w);
            int x2 = (int) Math.round(cx + w);
            if (x2 > x1) ctx.fill(x1, y, x2, y + 1, color);
        }
    }

    // треугольник остриём вверх: от вершины к основанию ширина растёт линейно
    static void drawTriangle(DrawContext ctx, double cx, double cy, double r, int color) {
        if (r < 1) return;
        int yTop = (int) Math.floor(cy - r);
        int yBottom = (int) Math.ceil(cy + r);
        for (int y = yTop; y < yBottom; y++) {
            double dy = y + 0.5 - cy;
            double w = dy + r;
            if (w < 0.5) continue;
            int x1 = (int) Math.round(cx - w);
            int x2 = (int) Math.round(cx + w);
            if (x2 > x1) ctx.fill(x1, y, x2, y + 1, color);
        }
    }

    private static void drawHex(DrawContext ctx, double cx, double cy, double r, int color) {
        if (r < 1) return;
        double halfH = r * 0.8660254;
        int yTop = (int) Math.floor(cy - halfH);
        int yBottom = (int) Math.ceil(cy + halfH);
        for (int y = yTop; y < yBottom; y++) {
            double dy = (y + 0.5) - cy;
            if (dy < -halfH) dy = -halfH;
            if (dy > halfH) dy = halfH;
            double w = r - Math.abs(dy) / 1.7320508;
            int x1 = (int) Math.round(cx - w);
            int x2 = (int) Math.round(cx + w);
            if (x2 > x1) {
                ctx.fill(x1, y, x2, y + 1, color);
            }
        }
    }
}
