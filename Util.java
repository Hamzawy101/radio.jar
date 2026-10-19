import java.util.Vector;
import javax.microedition.lcdui.Font;

public class Util {
    public static String[] split(String s, char c) {
        Vector v = new Vector();
        int st = 0;
        for (int i = 0; i <= s.length(); i++) {
            if (i == s.length() || s.charAt(i) == c) {
                v.addElement(s.substring(st, i));
                st = i + 1;
            }
        }
        String[] r = new String[v.size()];
        v.copyInto(r);
        return r;
    }

    public static String join(Vector v, char c) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < v.size(); i++) {
            if (i > 0) sb.append(c);
            sb.append((String) v.elementAt(i));
        }
        return sb.toString();
    }

    public static boolean hasIgnoreCase(String s, String q) {
        return s.toLowerCase().indexOf(q.toLowerCase()) >= 0;
    }

    public static String clip(String s, Font f, int w) {
        if (f.stringWidth(s) <= w) return s;
        int len = s.length();
        while (len > 1 && f.stringWidth(s.substring(0, len) + "..") > w) len--;
        return s.substring(0, len) + "..";
    }

    public static String fmtTime(long ms) {
        long sec = ms / 1000;
        long m = sec / 60;
        long s = sec % 60;
        long h = m / 60;
        m = m % 60;
        String r = (m < 10 ? "0" : "") + m + ":" + (s < 10 ? "0" : "") + s;
        if (h > 0) r = h + ":" + r;
        return r;
    }

    public static void sortStrings(Vector v) {
        for (int i = 1; i < v.size(); i++) {
            String x = (String) v.elementAt(i);
            int j = i - 1;
            while (j >= 0 && ((String) v.elementAt(j)).toLowerCase().compareTo(x.toLowerCase()) > 0) {
                v.setElementAt(v.elementAt(j), j + 1);
                j--;
            }
            v.setElementAt(x, j + 1);
        }
    }
}
