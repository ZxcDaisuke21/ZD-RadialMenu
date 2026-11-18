package radialmenu.zd.client;

/**
 * Проверка попадания курсора в клетку вида «Свой» по её форме, а не по
 * обрамляющему прямоугольнику. Формулы повторяют рисование в MenuSkin
 * (радиус фигур = min(w, h) / 2), сверху добавляется запас в 3 пикселя,
 * чтобы попадать по кнопке было легче.
 */
public final class ShapeHits {
    // запас хитбокса в пикселях со всех сторон
    public static final int PAD = 3;

    private ShapeHits() {
    }

    // возвращает индекс клетки под курсором (cx, cy — центр всей раскладки),
    // или −1, если курсор ни в одну клетку не попал
    public static int pick(SectorConfig.CustomCell[] cells, int cx, int cy, int mx, int my) {
        for (int i = cells.length - 1; i >= 0; i--) {
            SectorConfig.CustomCell c = cells[i];
            if (c != null && hit(c, cx + c.dx, cy + c.dy, mx, my)) return i;
        }
        return -1;
    }

    // hit клетки с левым верхним углом в (px, py)
    public static boolean hit(SectorConfig.CustomCell c, int px, int py, int mx, int my) {
        if (c == null) return false;
        double cx = px + c.w / 2.0;
        double cy = py + c.h / 2.0;
        double dx = mx - cx;
        double dy = my - cy;

        String shape = c.shape == null ? "rect" : c.shape;
        if ("rect".equals(shape)) {
            // прямоугольник — по всей своей площади, с запасом
            return Math.abs(dx) <= c.w / 2.0 + PAD && Math.abs(dy) <= c.h / 2.0 + PAD;
        }

        // у остальных фигур рисунок использует радиус min(w, h) / 2
        double r = Math.min(c.w, c.h) / 2.0 + PAD;

        switch (shape) {
            case "disc":
                // круг — как drawDisc
                return dx * dx + dy * dy <= r * r;

            case "diamond":
                // ромб — как drawDiamond: |dx| + |dy| <= r
                return Math.abs(dx) + Math.abs(dy) <= r;

            case "hex": {
                // гекс с вершинами слева/справа — ровно как drawHex:
                // полуширина на высоте dy: w = r - |dy| / sqrt(3), |dy| <= r * sqrt(3)/2
                double halfH = r * 0.8660254;
                if (Math.abs(dy) > halfH) return false;
                double w = r - Math.abs(dy) / 1.7320508;
                return Math.abs(dx) <= w;
            }

            case "triangle": {
                // треугольник остриём вверх — ровно как drawTriangle:
                // полуширина на высоте dy: w = dy + r, dy от −r (вершина) до +r (основание)
                if (dy < -r || dy > r) return false;
                return Math.abs(dx) <= dy + r;
            }

            default:
                return Math.abs(dx) <= c.w / 2.0 + PAD && Math.abs(dy) <= c.h / 2.0 + PAD;
        }
    }
}
