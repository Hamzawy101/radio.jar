import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Vector;

public class Library {
    private static Vector all = new Vector();
    private static Vector favs = new Vector();
    private static Vector hidden = new Vector();
    private static Vector recents = new Vector();

    public static void init(Class c) {
        all.removeAllElements();
        parse(readResource(c, "/stations.txt"), '\n', false);
        parse(Prefs.get("user", ""), '\t', true);
        favs = loadList("favs");
        hidden = loadList("hidden");
        recents = loadList("recents");
    }

    private static Vector loadList(String key) {
        Vector v = new Vector();
        String s = Prefs.get(key, "");
        if (s.length() > 0) {
            String[] p = Util.split(s, '\t');
            for (int i = 0; i < p.length; i++) if (p[i].length() > 0) v.addElement(p[i]);
        }
        return v;
    }

    private static void saveList(String key, Vector v) {
        Prefs.put(key, Util.join(v, '\t'));
    }

    private static String readResource(Class c, String path) {
        InputStream is = null;
        try {
            is = c.getResourceAsStream(path);
            if (is == null) return "";
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            byte[] b = new byte[1024];
            int n;
            while ((n = is.read(b)) > 0) bo.write(b, 0, n);
            return new String(bo.toByteArray(), "UTF-8");
        } catch (Exception e) {
            return "";
        } finally {
            if (is != null) { try { is.close(); } catch (Exception e) { } }
        }
    }

    /** Lines look like: Country|Genre|Name|URL. Returns the number added. */
    private static int parse(String text, char sep, boolean user) {
        int added = 0;
        String[] ls = Util.split(text, sep);
        for (int i = 0; i < ls.length; i++) {
            String l = ls[i].trim();
            if (l.length() == 0 || l.startsWith("#")) continue;
            String[] f = Util.split(l, '|');
            if (f.length < 4) continue;
            String url = f[3].trim();
            if (!url.startsWith("http")) continue;
            if (user && find(url) != null) continue;
            Station s = new Station(f[0].trim(), f[1].trim(), f[2].trim(), url);
            s.user = user;
            all.addElement(s);
            added++;
        }
        return added;
    }

    public static Station find(String url) {
        for (int i = 0; i < all.size(); i++) {
            Station s = (Station) all.elementAt(i);
            if (s.url.equals(url)) return s;
        }
        return null;
    }

    public static boolean isHidden(Station s) { return hidden.contains(s.url); }

    public static Vector visible() {
        Vector r = new Vector();
        for (int i = 0; i < all.size(); i++) {
            Station s = (Station) all.elementAt(i);
            if (!isHidden(s)) r.addElement(s);
        }
        return r;
    }

    public static int count() { return visible().size(); }

    public static Vector distinct(boolean country) {
        Vector r = new Vector();
        Vector v = visible();
        for (int i = 0; i < v.size(); i++) {
            Station s = (Station) v.elementAt(i);
            String k = country ? s.country : s.genre;
            if (!r.contains(k)) r.addElement(k);
        }
        Util.sortStrings(r);
        return r;
    }

    public static Vector by(boolean country, String key) {
        Vector r = new Vector();
        Vector v = visible();
        for (int i = 0; i < v.size(); i++) {
            Station s = (Station) v.elementAt(i);
            if ((country ? s.country : s.genre).equals(key)) r.addElement(s);
        }
        return r;
    }

    public static Vector search(String q) {
        Vector r = new Vector();
        Vector v = visible();
        for (int i = 0; i < v.size(); i++) {
            Station s = (Station) v.elementAt(i);
            if (Util.hasIgnoreCase(s.name, q) || Util.hasIgnoreCase(s.country, q) || Util.hasIgnoreCase(s.genre, q)) {
                r.addElement(s);
            }
        }
        return r;
    }

    private static Vector fromUrls(Vector urls) {
        Vector r = new Vector();
        for (int i = 0; i < urls.size(); i++) {
            Station s = find((String) urls.elementAt(i));
            if (s != null && !isHidden(s)) r.addElement(s);
        }
        return r;
    }

    public static Vector favorites() { return fromUrls(favs); }
    public static Vector recent() { return fromUrls(recents); }
    public static int favCount() { return favorites().size(); }

    public static boolean isFav(Station s) { return favs.contains(s.url); }

    public static boolean toggleFav(Station s) {
        boolean now;
        if (favs.contains(s.url)) {
            favs.removeElement(s.url);
            now = false;
        } else {
            favs.addElement(s.url);
            now = true;
        }
        saveList("favs", favs);
        return now;
    }

    public static void addRecent(Station s) {
        recents.removeElement(s.url);
        recents.insertElementAt(s.url, 0);
        while (recents.size() > 15) recents.removeElementAt(recents.size() - 1);
        saveList("recents", recents);
    }

    public static void clearRecent() {
        recents.removeAllElements();
        saveList("recents", recents);
    }

    public static void hide(Station s) {
        if (!hidden.contains(s.url)) hidden.addElement(s.url);
        saveList("hidden", hidden);
    }

    public static int hiddenCount() { return hidden.size(); }

    public static void unhideAll() {
        hidden.removeAllElements();
        saveList("hidden", hidden);
    }

    private static void saveUser() {
        Vector lines = new Vector();
        for (int i = 0; i < all.size(); i++) {
            Station s = (Station) all.elementAt(i);
            if (s.user) lines.addElement(s.country + "|" + s.genre + "|" + s.name + "|" + s.url);
        }
        Prefs.put("user", Util.join(lines, '\t'));
    }

    public static void addUser(Station s) {
        s.user = true;
        all.addElement(s);
        saveUser();
    }

    public static void deleteUser(Station s) {
        all.removeElement(s);
        favs.removeElement(s.url);
        saveList("favs", favs);
        saveUser();
    }

    public static int importText(String text) {
        int n = parse(text, '\n', true);
        if (n > 0) saveUser();
        return n;
    }
}
