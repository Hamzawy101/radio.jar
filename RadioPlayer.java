import javax.microedition.media.Manager;
import javax.microedition.media.Player;
import javax.microedition.media.PlayerListener;
import javax.microedition.media.control.VolumeControl;

public class RadioPlayer implements PlayerListener {
    public static final int STOPPED = 0, CONNECTING = 1, BUFFERING = 2, PLAYING = 3, ERROR = 4;

    public Station station;
    public int state = STOPPED;
    public String message = "";
    public int volume = 70;
    public boolean muted = false;
    public boolean autoRetry = true;
    public long startedAt = 0;

    private Player player;
    private int gen = 0;
    private int tries = 0;
    private RadioListener listener;

    public RadioPlayer(RadioListener l) { listener = l; }

    public boolean isActive() { return state != STOPPED && state != ERROR; }

    public long elapsed() {
        if (state == PLAYING || state == BUFFERING) return System.currentTimeMillis() - startedAt;
        return 0;
    }

    private void setState(int st, String msg) {
        state = st;
        message = msg;
        if (listener != null) listener.radioChanged();
    }

    public void play(Station s) {
        closePlayer();
        station = s;
        gen++;
        tries = 0;
        startedAt = System.currentTimeMillis();
        setState(CONNECTING, "Connecting...");
        launch(gen, 0);
    }

    public void stop() {
        gen++;
        closePlayer();
        setState(STOPPED, "Stopped");
    }

    private void launch(final int g, final int attempt) {
        new Thread(new Runnable() {
            public void run() { connect(g, attempt); }
        }).start();
    }

    private void connect(int g, int attempt) {
        Player np = null;
        try {
            String u = station.url;
            if (Net.isPlaylist(u)) {
                u = Net.resolvePlaylist(u);
            } else if (attempt == 1 || u.indexOf("livestream-redirect") >= 0) {
                String r = Net.followRedirect(u);
                if (r != null) u = r;
            }
            if (g != gen) return;
            np = Manager.createPlayer(u);
            synchronized (this) {
                if (g != gen) { np.close(); return; }
                player = np;
            }
            np.addPlayerListener(this);
            np.realize();
            applyVolume(np);
            if (g != gen) return;
            setState(BUFFERING, "Buffering...");
            np.prefetch();
            if (g != gen) return;
            np.start();
            if (g == gen && state != ERROR) setState(PLAYING, "Playing");
        } catch (Throwable t) {
            fail(g, attempt, t);
        }
    }

    private void fail(int g, int attempt, Throwable t) {
        if (g != gen) return;
        closePlayer();
        if (autoRetry && tries < 4) {
            tries++;
            setState(CONNECTING, "Retrying (" + tries + ")...");
            try { Thread.sleep(1500); } catch (Exception e) { }
            if (g != gen) return;
            connect(g, attempt == 0 ? 1 : 0);
        } else {
            String m = t.getMessage();
            if (m == null || m.length() == 0) m = "Cannot play this stream";
            if (m.length() > 40) m = m.substring(0, 40);
            setState(ERROR, m);
        }
    }

    private void closePlayer() {
        Player p;
        synchronized (this) {
            p = player;
            player = null;
        }
        if (p != null) {
            try { p.removePlayerListener(this); } catch (Exception e) { }
            try { p.stop(); } catch (Throwable e) { }
            try { p.close(); } catch (Throwable e) { }
        }
    }

    private void applyVolume(Player p) {
        try {
            VolumeControl vc = (VolumeControl) p.getControl("VolumeControl");
            if (vc != null) {
                vc.setMute(muted);
                vc.setLevel(volume);
            }
        } catch (Throwable t) { }
    }

    public void setVolume(int v) {
        if (v < 0) v = 0;
        if (v > 100) v = 100;
        volume = v;
        if (player != null) applyVolume(player);
        Prefs.putInt("vol", v);
        if (listener != null) listener.radioChanged();
    }

    public void setMuted(boolean m) {
        muted = m;
        if (player != null) applyVolume(player);
        if (listener != null) listener.radioChanged();
    }

    public void playerUpdate(Player pl, String ev, Object data) {
        if (pl != player) return;
        if (ev.equals("started")) {
            if (state != ERROR) setState(PLAYING, "Playing");
        } else if (ev.equals("bufferingStarted")) {
            setState(BUFFERING, "Buffering...");
        } else if (ev.equals("bufferingStopped")) {
            setState(PLAYING, "Playing");
        } else if (ev.equals("error") || ev.equals("endOfMedia")) {
            final int g = gen;
            new Thread(new Runnable() {
                public void run() { fail(g, 1, new Exception("Stream dropped")); }
            }).start();
        } else if (ev.equals("deviceUnavailable")) {
            setState(BUFFERING, "Paused (interrupted)");
        } else if (ev.equals("deviceAvailable")) {
            final Player cp = pl;
            new Thread(new Runnable() {
                public void run() {
                    try { cp.start(); } catch (Throwable t) { }
                }
            }).start();
        } else if (ev.equals("stopped")) {
            if (state == PLAYING) setState(BUFFERING, "Paused");
        }
    }
}
