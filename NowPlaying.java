import java.util.Random;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;

public class NowPlaying extends Canvas {
    private RadioMIDlet app;
    private RadioPlayer rp;
    private int[] bars = new int[18];
    private Random rnd = new Random();
    private String toast = null;
    private long toastUntil = 0;

    public NowPlaying(RadioMIDlet a, RadioPlayer p) {
        app = a;
        rp = p;
    }

    public void toast(String s) {
        toast = s;
        toastUntil = System.currentTimeMillis() + 2000;
        repaint();
    }

    public void step() {
        boolean on = rp.state == RadioPlayer.PLAYING && !rp.muted;
        for (int i = 0; i < bars.length; i++) {
            int target = on ? 12 + Math.abs(rnd.nextInt() % 88) : 4;
            bars[i] += (target - bars[i]) / 2;
        }
    }

    protected void paint(Graphics g) {
        int w = getWidth();
        int h = getHeight();
        g.setColor(0x0B1220);
        g.fillRect(0, 0, w, h);

        Font fs = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_PLAIN, Font.SIZE_SMALL);
        Font fm = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_BOLD, Font.SIZE_MEDIUM);
        Font fl = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_BOLD, Font.SIZE_LARGE);
        int cx = w / 2;
        int y = 3;
        Station s = rp.station;

        g.setFont(fl);
        g.setColor(0xFFFFFF);
        g.drawString(s == null ? "No station" : Util.clip(s.name, fl, w - 8), cx, y, Graphics.TOP | Graphics.HCENTER);
        y += fl.getHeight() + 1;

        g.setFont(fs);
        g.setColor(0x8FA3BF);
        String sub = s == null ? "Pick one from the menu" : s.country + " - " + s.genre;
        g.drawString(Util.clip(sub, fs, w - 8), cx, y, Graphics.TOP | Graphics.HCENTER);
        y += fs.getHeight() + 3;

        int col = 0x7CFC9A;
        if (rp.state == RadioPlayer.ERROR) col = 0xFF6B6B;
        else if (rp.state == RadioPlayer.CONNECTING || rp.state == RadioPlayer.BUFFERING) col = 0xFFD166;
        else if (rp.state == RadioPlayer.STOPPED) col = 0xB0B8C4;
        g.setFont(fm);
        g.setColor(col);
        String msg = rp.muted && rp.state == RadioPlayer.PLAYING ? "Playing (muted)" : rp.message;
        g.drawString(Util.clip(msg, fm, w - 8), cx, y, Graphics.TOP | Graphics.HCENTER);
        y += fm.getHeight() + 4;

        int bottom = fs.getHeight() * 2 + 18;
        int eqTop = y;
        int eqBottom = h - bottom - 2;
        int eqH = eqBottom - eqTop;
        if (eqH > 10) {
            int n = bars.length;
            int bw = (w - 8) / n;
            int x0 = (w - bw * n) / 2;
            g.setColor(0x00A8E8);
            for (int i = 0; i < n; i++) {
                int bh = eqH * bars[i] / 100;
                if (bh < 2) bh = 2;
                g.fillRect(x0 + i * bw + 1, eqBottom - bh, bw - 2, bh);
            }
        }

        int by = h - bottom + 2;
        g.setFont(fs);
        g.setColor(0xFFFFFF);
        String left = Util.fmtTime(rp.elapsed());
        if (s != null && Library.isFav(s)) left = "* " + left;
        g.drawString(left, 4, by, Graphics.TOP | Graphics.LEFT);
        if (app.sleepAt > 0) {
            long rem = app.sleepAt - System.currentTimeMillis();
            if (rem < 0) rem = 0;
            g.setColor(0xFFD166);
            g.drawString("Sleep " + Util.fmtTime(rem), w - 4, by, Graphics.TOP | Graphics.RIGHT);
        }
        by += fs.getHeight() + 2;

        int bx = 4 + fs.stringWidth("Vol ") + 2;
        int bwid = w - bx - 4;
        g.setColor(0x8FA3BF);
        g.drawString("Vol", 4, by, Graphics.TOP | Graphics.LEFT);
        g.drawRect(bx, by + 2, bwid, 8);
        g.setColor(rp.muted ? 0xFF6B6B : 0x00A8E8);
        g.fillRect(bx + 1, by + 3, (bwid - 1) * rp.volume / 100, 7);
        by += 12;

        g.setColor(0x6B7A90);
        g.drawString(Util.clip("2/8 vol  4/6 prev/next  5 play  1 fav", fs, w - 6), cx, by, Graphics.TOP | Graphics.HCENTER);

        if (toast != null && System.currentTimeMillis() < toastUntil) {
            g.setFont(fm);
            int tw = fm.stringWidth(toast) + 16;
            if (tw > w - 4) tw = w - 4;
            int th = fm.getHeight() + 10;
            int tx = (w - tw) / 2;
            int ty = (h - th) / 2;
            g.setColor(0x223049);
            g.fillRect(tx, ty, tw, th);
            g.setColor(0x00A8E8);
            g.drawRect(tx, ty, tw, th);
            g.setColor(0xFFFFFF);
            g.drawString(Util.clip(toast, fm, tw - 8), cx, ty + 5, Graphics.TOP | Graphics.HCENTER);
        }
    }

    protected void keyPressed(int k) {
        handle(k);
    }

    protected void keyRepeated(int k) {
        int ga = 0;
        try { ga = getGameAction(k); } catch (Exception e) { }
        if (ga == UP || ga == DOWN || k == KEY_STAR || k == KEY_POUND) handle(k);
    }

    private void handle(int k) {
        if (k == KEY_NUM1) { app.toggleFavNow(); return; }
        if (k == KEY_NUM3) { app.playRandom(); return; }
        if (k == KEY_NUM7) { app.cycleSleep(); return; }
        if (k == KEY_NUM9) { app.toggleLight(); return; }
        if (k == KEY_NUM0) { app.toggleMute(); return; }
        if (k == KEY_STAR) { rp.setVolume(rp.volume - 10); return; }
        if (k == KEY_POUND) { rp.setVolume(rp.volume + 10); return; }
        int ga = 0;
        try { ga = getGameAction(k); } catch (Exception e) { }
        if (ga == UP) rp.setVolume(rp.volume + 10);
        else if (ga == DOWN) rp.setVolume(rp.volume - 10);
        else if (ga == LEFT) app.step(-1);
        else if (ga == RIGHT) app.step(1);
        else if (ga == FIRE) app.togglePlay();
    }
}
