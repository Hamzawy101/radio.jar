import java.io.IOException;
import java.io.InputStream;
import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;

public class Net {
    private static final String UA = "Mozilla/5.0 (compatible; J2ME Radio)";

    public static boolean isPlaylist(String u) {
        String l = u.toLowerCase();
        int q = l.indexOf('?');
        if (q > 0) l = l.substring(0, q);
        return l.endsWith(".pls") || l.endsWith(".m3u");
    }

    private static String absolute(String base, String loc) {
        if (loc.startsWith("http://") || loc.startsWith("https://")) return loc;
        int s = base.indexOf("://");
        if (s < 0) return loc;
        int e = base.indexOf('/', s + 3);
        String root = e < 0 ? base : base.substring(0, e);
        if (loc.startsWith("/")) return root + loc;
        return root + "/" + loc;
    }

    private static boolean isRedirect(int rc) {
        return rc == 301 || rc == 302 || rc == 303 || rc == 307 || rc == 308;
    }

    /** Downloads a small text resource over HTTP, following redirects. */
    public static String httpGetText(String url, int max) throws IOException {
        String cur = url;
        for (int hop = 0; hop < 5; hop++) {
            HttpConnection c = null;
            InputStream is = null;
            try {
                c = (HttpConnection) Connector.open(cur, Connector.READ, true);
                c.setRequestMethod(HttpConnection.GET);
                c.setRequestProperty("User-Agent", UA);
                int rc = c.getResponseCode();
                if (isRedirect(rc)) {
                    String loc = c.getHeaderField("Location");
                    if (loc == null) throw new IOException("Bad redirect");
                    cur = absolute(cur, loc);
                    continue;
                }
                if (rc != 200) throw new IOException("HTTP " + rc);
                is = c.openInputStream();
                byte[] buf = new byte[max];
                int n = 0;
                while (n < max) {
                    int r = is.read(buf, n, max - n);
                    if (r <= 0) break;
                    n += r;
                }
                return new String(buf, 0, n, "UTF-8");
            } finally {
                if (is != null) { try { is.close(); } catch (Exception e) { } }
                if (c != null) { try { c.close(); } catch (Exception e) { } }
            }
        }
        throw new IOException("Too many redirects");
    }

    /** Reads the first stream address out of a .pls or .m3u playlist. */
    public static String resolvePlaylist(String url) throws IOException {
        String t = httpGetText(url, 4096);
        String[] ls = Util.split(t, '\n');
        for (int i = 0; i < ls.length; i++) {
            String l = ls[i].trim();
            if (l.length() == 0 || l.startsWith("#")) continue;
            String low = l.toLowerCase();
            if (low.startsWith("file1=")) return l.substring(6).trim();
            if (low.startsWith("http://")) return l;
        }
        throw new IOException("Empty playlist");
    }

    /** Follows redirects without reading the stream. Returns the final address, or null if unchanged/unknown. */
    public static String followRedirect(String url) {
        String cur = url;
        for (int hop = 0; hop < 5; hop++) {
            HttpConnection c = null;
            try {
                c = (HttpConnection) Connector.open(cur, Connector.READ, true);
                c.setRequestMethod(HttpConnection.GET);
                c.setRequestProperty("User-Agent", UA);
                int rc = c.getResponseCode();
                if (isRedirect(rc)) {
                    String loc = c.getHeaderField("Location");
                    if (loc == null) return null;
                    cur = absolute(cur, loc);
                    continue;
                }
                return cur.equals(url) ? null : cur;
            } catch (Throwable t) {
                return cur.equals(url) ? null : cur;
            } finally {
                if (c != null) { try { c.close(); } catch (Exception e) { } }
            }
        }
        return null;
    }
}
