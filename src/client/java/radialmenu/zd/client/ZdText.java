package radialmenu.zd.client;

public final class ZdText {
    private ZdText() {
    }

    // сокращение для показа в меню: до 8 символов + «…»,
    // обрезка по границе слова, чтобы не рвать «/time set» на «/time se»
    public static String shorten(String s) {
        if (s == null) return "";
        s = s.trim();
        if (s.length() <= 8) return s;
        String cut = s.substring(0, 8);
        // если следующий символ — не пробел, сдвигаемся назад до границы слова
        if (s.length() > 9 && s.charAt(8) != ' ') {
            int sp = cut.lastIndexOf(' ');
            if (sp > 0) cut = cut.substring(0, sp);
        }
        cut = cut.stripTrailing();
        return cut + "…";
    }
}
