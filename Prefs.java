import java.util.Hashtable;
import java.util.Enumeration;
import javax.microedition.rms.RecordStore;

public class Prefs {
    private static Hashtable h = new Hashtable();

    public static void load() {
        RecordStore rs = null;
        try {
            rs = RecordStore.openRecordStore("rprefs", true);
            if (rs.getNumRecords() > 0) {
                String s = new String(rs.getRecord(1), "UTF-8");
                String[] ls = Util.split(s, '\n');
                for (int i = 0; i < ls.length; i++) {
                    int e = ls[i].indexOf('=');
                    if (e > 0) h.put(ls[i].substring(0, e), ls[i].substring(e + 1));
                }
            }
        } catch (Exception e) {
        } finally {
            close(rs);
        }
    }

    private static void close(RecordStore rs) {
        if (rs != null) {
            try { rs.closeRecordStore(); } catch (Exception e) { }
        }
    }

    private static void save() {
        RecordStore rs = null;
        try {
            StringBuffer sb = new StringBuffer();
            Enumeration k = h.keys();
            while (k.hasMoreElements()) {
                String key = (String) k.nextElement();
                sb.append(key).append('=').append((String) h.get(key)).append('\n');
            }
            byte[] b = sb.toString().getBytes("UTF-8");
            rs = RecordStore.openRecordStore("rprefs", true);
            if (rs.getNumRecords() == 0) rs.addRecord(b, 0, b.length);
            else rs.setRecord(1, b, 0, b.length);
        } catch (Exception e) {
        } finally {
            close(rs);
        }
    }

    public static String get(String k, String def) {
        String v = (String) h.get(k);
        return v == null ? def : v;
    }

    public static void put(String k, String v) {
        h.put(k, v.replace('\n', ' '));
        save();
    }

    public static int getInt(String k, int def) {
        try { return Integer.parseInt(get(k, "")); } catch (Exception e) { return def; }
    }

    public static void putInt(String k, int v) { put(k, Integer.toString(v)); }

    public static boolean getBool(String k, boolean def) {
        String v = get(k, "");
        if (v.length() == 0) return def;
        return v.equals("1");
    }

    public static void putBool(String k, boolean v) { put(k, v ? "1" : "0"); }
}
