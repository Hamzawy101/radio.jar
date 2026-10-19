import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Choice;
import javax.microedition.lcdui.ChoiceGroup;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Gauge;
import javax.microedition.lcdui.List;
import javax.microedition.lcdui.StringItem;
import javax.microedition.lcdui.TextBox;
import javax.microedition.lcdui.TextField;
import javax.microedition.midlet.MIDlet;

public class RadioMIDlet extends MIDlet implements CommandListener, RadioListener {
    private Display display;
    private RadioPlayer rp;
    private NowPlaying np;
    private Timer timer;
    private boolean started = false;
    private Random rnd = new Random();

    private List mainMenu, stationList, countryList, genreList, sleepList, toolsList;
    private Vector countryKeys, genreKeys;
    private Form addForm, settingsForm, aboutForm, importForm;
    private TextBox searchBox;
    private TextField fName, fUrl, fCountry, fGenre, fImport;
    private ChoiceGroup setChoice;
    private Gauge setVol;

    private Vector shown;
    private Vector ctx;
    private Displayable shownParent;
    private boolean shownTag;
    private Displayable lastBrowse;
    private Displayable sleepBack;

    long sleepAt = 0;
    private int sleepCycle = 0;
    private boolean light = false;
    private long lastFlash = 0;

    private static final int[] SLEEP_MIN = {0, 10, 15, 30, 45, 60, 90, 120};
    private static final int[] SLEEP_KEY = {0, 15, 30, 60, 90};

    private Command cmdBack = new Command("Back", Command.BACK, 1);
    private Command cmdExit = new Command("Exit", Command.EXIT, 9);
    private Command cmdNow = new Command("Now Playing", Command.SCREEN, 1);
    private Command cmdFav = new Command("Favorite on/off", Command.SCREEN, 2);
    private Command cmdHide = new Command("Hide station", Command.SCREEN, 3);
    private Command cmdDel = new Command("Delete my station", Command.SCREEN, 4);
    private Command cmdSave = new Command("Save", Command.OK, 1);
    private Command cmdSearch = new Command("Search", Command.OK, 1);
    private Command cmdImport = new Command("Import", Command.OK, 1);

    private Command npPlay = new Command("Stop / Play", Command.SCREEN, 1);
    private Command npNext = new Command("Next station", Command.SCREEN, 2);
    private Command npPrev = new Command("Previous station", Command.SCREEN, 3);
    private Command npFav = new Command("Favorite on/off", Command.SCREEN, 4);
    private Command npRandom = new Command("Random station", Command.SCREEN, 5);
    private Command npSleep = new Command("Sleep timer", Command.SCREEN, 6);
    private Command npMute = new Command("Mute on/off", Command.SCREEN, 7);
    private Command npLight = new Command("Screen light on/off", Command.SCREEN, 8);
    private Command npHide = new Command("Hide this station", Command.SCREEN, 9);
    private Command npMenu = new Command("Stations", Command.BACK, 1);
    private Command npExit = new Command("Exit", Command.EXIT, 10);

    protected void startApp() {
        if (started) return;
        started = true;
        display = Display.getDisplay(this);
        Prefs.load();
        Library.init(getClass());
        rp = new RadioPlayer(this);
        rp.volume = Prefs.getInt("vol", 70);
        rp.autoRetry = Prefs.getBool("retry", true);
        light = Prefs.getBool("light", false);
        np = new NowPlaying(this, rp);
        np.addCommand(npPlay);
        np.addCommand(npNext);
        np.addCommand(npPrev);
        np.addCommand(npFav);
        np.addCommand(npRandom);
        np.addCommand(npSleep);
        np.addCommand(npMute);
        np.addCommand(npLight);
        np.addCommand(npHide);
        np.addCommand(npMenu);
        np.addCommand(npExit);
        np.setCommandListener(this);

        mainMenu = new List("Internet Radio", List.IMPLICIT);
        for (int i = 0; i < 14; i++) mainMenu.append("-", null);
        mainMenu.addCommand(cmdExit);
        mainMenu.setCommandListener(this);
        updateMain();

        timer = new Timer();
        timer.schedule(new TimerTask() {
            public void run() { tick(); }
        }, 500, 500);

        display.setCurrent(mainMenu);
        if (Prefs.getBool("autoplay", false)) {
            Station s = Library.find(Prefs.get("last", ""));
            if (s != null) playStation(s, null);
        }
    }

    protected void pauseApp() { }

    protected void destroyApp(boolean unconditional) {
        cleanup();
    }

    private void cleanup() {
        try { if (rp != null) rp.stop(); } catch (Throwable t) { }
        try { if (timer != null) timer.cancel(); } catch (Throwable t) { }
    }

    private void quit() {
        cleanup();
        notifyDestroyed();
    }

    // ---------------------------------------------------------------- helpers

    public void radioChanged() {
        if (np != null && display != null && display.getCurrent() == np) np.repaint();
    }

    private void tick() {
        long now = System.currentTimeMillis();
        if (sleepAt > 0 && now >= sleepAt) {
            sleepAt = 0;
            rp.stop();
            if (display.getCurrent() == np) np.toast("Sleep timer: stopped");
        }
        if (light && rp.isActive() && now - lastFlash > 8000) {
            lastFlash = now;
            display.flashBackLight(10000);
        }
        if (display.getCurrent() == np) {
            np.step();
            np.repaint();
        }
    }

    private void alert(String msg, Displayable next) {
        Alert a = new Alert("Radio", msg, null, AlertType.INFO);
        a.setTimeout(2500);
        display.setCurrent(a, next);
    }

    private void updateMain() {
        mainMenu.set(0, "Now Playing", null);
        mainMenu.set(1, "Favorites (" + Library.favCount() + ")", null);
        mainMenu.set(2, "Recently Played", null);
        mainMenu.set(3, "Browse by Country", null);
        mainMenu.set(4, "Browse by Genre", null);
        mainMenu.set(5, "All Stations (" + Library.count() + ")", null);
        mainMenu.set(6, "Search", null);
        mainMenu.set(7, "Random Station", null);
        mainMenu.set(8, "Add Station", null);
        mainMenu.set(9, "Sleep Timer", null);
        mainMenu.set(10, "Station Tools", null);
        mainMenu.set(11, "Settings", null);
        mainMenu.set(12, "Help / About", null);
        mainMenu.set(13, "Exit", null);
    }

    private void showMain() {
        updateMain();
        display.setCurrent(mainMenu);
    }

    private String label(Station s) {
        String t = (Library.isFav(s) ? "* " : "") + s.name;
        if (shownTag) t = t + " (" + s.country + ")";
        return t;
    }

    private int indexOf(Vector v, Station s) {
        for (int i = 0; i < v.size(); i++) if (v.elementAt(i) == s) return i;
        return -1;
    }

    private void showStations(String title, Vector v, Displayable parent, boolean tagCountry) {
        if (v.size() == 0) {
            alert("Nothing here yet.", parent);
            return;
        }
        shown = v;
        shownParent = parent;
        shownTag = tagCountry;
        stationList = new List(title + " (" + v.size() + ")", List.IMPLICIT);
        for (int i = 0; i < v.size(); i++) stationList.append(label((Station) v.elementAt(i)), null);
        stationList.addCommand(cmdBack);
        stationList.addCommand(cmdNow);
        stationList.addCommand(cmdFav);
        stationList.addCommand(cmdHide);
        stationList.addCommand(cmdDel);
        stationList.setCommandListener(this);
        lastBrowse = stationList;
        display.setCurrent(stationList);
    }

    private void showNow() {
        display.setCurrent(np);
        np.repaint();
    }

    private void playStation(Station s, Vector context) {
        ctx = context;
        rp.play(s);
        Library.addRecent(s);
        Prefs.put("last", s.url);
        showNow();
    }

    private Vector currentCtx() {
        if (ctx == null || ctx.size() == 0) ctx = Library.visible();
        return ctx;
    }

    // ------------------------------------------------- called by NowPlaying

    void step(int dir) {
        Vector v = currentCtx();
        if (v.size() == 0) return;
        int i = rp.station == null ? -1 : indexOf(v, rp.station);
        if (i < 0) i = dir > 0 ? 0 : v.size() - 1;
        else i = (i + dir + v.size()) % v.size();
        playStation((Station) v.elementAt(i), v);
    }

    void playRandom() {
        Vector v = Library.visible();
        if (v.size() == 0) return;
        playStation((Station) v.elementAt(Math.abs(rnd.nextInt() % v.size())), v);
    }

    void togglePlay() {
        if (rp.isActive()) rp.stop();
        else if (rp.station != null) rp.play(rp.station);
        else {
            alert("Choose a station first.", mainMenu);
            return;
        }
        np.repaint();
    }

    void toggleFavNow() {
        if (rp.station == null) return;
        boolean on = Library.toggleFav(rp.station);
        np.toast(on ? "Added to favorites" : "Removed from favorites");
    }

    void toggleMute() {
        rp.setMuted(!rp.muted);
        np.toast(rp.muted ? "Muted" : "Sound on");
    }

    void toggleLight() {
        light = !light;
        Prefs.putBool("light", light);
        np.toast(light ? "Screen light: on" : "Screen light: normal");
    }

    void cycleSleep() {
        sleepCycle = (sleepCycle + 1) % SLEEP_KEY.length;
        setSleep(SLEEP_KEY[sleepCycle]);
        np.toast(SLEEP_KEY[sleepCycle] == 0 ? "Sleep timer: off" : "Sleep timer: " + SLEEP_KEY[sleepCycle] + " min");
    }

    private void setSleep(int minutes) {
        sleepAt = minutes == 0 ? 0 : System.currentTimeMillis() + minutes * 60000L;
    }

    private void hideNow() {
        Station s = rp.station;
        if (s == null) return;
        Vector v = currentCtx();
        int i = indexOf(v, s);
        Library.hide(s);
        if (i >= 0) v.removeElementAt(i);
        if (v.size() > 0) {
            playStation((Station) v.elementAt(i < 0 ? 0 : i % v.size()), v);
            np.toast("Station hidden");
        } else {
            rp.stop();
        }
    }

    // ------------------------------------------------------------- commands

    public void commandAction(Command c, Displayable d) {
        if (d == mainMenu) {
            if (c == List.SELECT_COMMAND) mainSelect(mainMenu.getSelectedIndex());
            else if (c == cmdExit) quit();
        } else if (d == np) {
            nowCmd(c);
        } else if (d == stationList) {
            stationCmd(c);
        } else if (d == countryList) {
            if (c == cmdBack) showMain();
            else if (c == List.SELECT_COMMAND) {
                int i = countryList.getSelectedIndex();
                String k = (String) countryKeys.elementAt(i);
                showStations(k, Library.by(true, k), countryList, false);
            }
        } else if (d == genreList) {
            if (c == cmdBack) showMain();
            else if (c == List.SELECT_COMMAND) {
                int i = genreList.getSelectedIndex();
                String k = (String) genreKeys.elementAt(i);
                showStations(k, Library.by(false, k), genreList, true);
            }
        } else if (d == sleepList) {
            if (c == List.SELECT_COMMAND) {
                int m = SLEEP_MIN[sleepList.getSelectedIndex()];
                setSleep(m);
                alert(m == 0 ? "Sleep timer is off." : "Radio will stop in " + m + " minutes.", sleepBack);
            } else if (c == cmdBack) display.setCurrent(sleepBack);
        } else if (d == toolsList) {
            toolsCmd(c);
        } else if (d == searchBox) {
            if (c == cmdBack) showMain();
            else if (c == cmdSearch) {
                String q = searchBox.getString().trim();
                if (q.length() == 0) return;
                Vector r = Library.search(q);
                if (r.size() == 0) alert("No stations match \"" + q + "\".", searchBox);
                else showStations("Search", r, mainMenu, true);
            }
        } else if (d == addForm) {
            addCmd(c);
        } else if (d == settingsForm) {
            if (c == cmdBack) showMain();
            else if (c == cmdSave) saveSettings();
        } else if (d == aboutForm) {
            showMain();
        } else if (d == importForm) {
            if (c == cmdBack) showTools();
            else if (c == cmdImport) doImport();
        }
    }

    private void mainSelect(int i) {
        switch (i) {
            case 0: showNow(); break;
            case 1: showStations("Favorites", Library.favorites(), mainMenu, true); break;
            case 2: showStations("Recent", Library.recent(), mainMenu, true); break;
            case 3: showCountries(); break;
            case 4: showGenres(); break;
            case 5: showStations("All", Library.visible(), mainMenu, true); break;
            case 6: showSearch(); break;
            case 7: playRandom(); break;
            case 8: showAdd(); break;
            case 9: showSleep(mainMenu); break;
            case 10: showTools(); break;
            case 11: showSettings(); break;
            case 12: showAbout(); break;
            case 13: quit(); break;
        }
    }

    private void nowCmd(Command c) {
        if (c == npMenu) {
            if (lastBrowse != null) display.setCurrent(lastBrowse);
            else showMain();
        } else if (c == npPlay) togglePlay();
        else if (c == npNext) step(1);
        else if (c == npPrev) step(-1);
        else if (c == npFav) toggleFavNow();
        else if (c == npRandom) playRandom();
        else if (c == npSleep) showSleep(np);
        else if (c == npMute) toggleMute();
        else if (c == npLight) toggleLight();
        else if (c == npHide) hideNow();
        else if (c == npExit) quit();
    }

    private void stationCmd(Command c) {
        if (c == cmdBack) {
            if (shownParent == mainMenu) showMain();
            else display.setCurrent(shownParent);
            return;
        }
        if (c == cmdNow) { showNow(); return; }
        int i = stationList.getSelectedIndex();
        if (i < 0 || i >= shown.size()) return;
        Station s = (Station) shown.elementAt(i);
        if (c == List.SELECT_COMMAND) {
            playStation(s, shown);
        } else if (c == cmdFav) {
            Library.toggleFav(s);
            stationList.set(i, label(s), null);
        } else if (c == cmdHide) {
            Library.hide(s);
            shown.removeElementAt(i);
            stationList.delete(i);
            if (shown.size() == 0) showMain();
        } else if (c == cmdDel) {
            if (!s.user) {
                alert("Only stations you added can be deleted. Use Hide instead.", stationList);
                return;
            }
            Library.deleteUser(s);
            shown.removeElementAt(i);
            stationList.delete(i);
            if (shown.size() == 0) showMain();
        }
    }

    // -------------------------------------------------------------- screens

    private void showCountries() {
        countryKeys = Library.distinct(true);
        countryList = new List("Countries", List.IMPLICIT);
        for (int i = 0; i < countryKeys.size(); i++) {
            String k = (String) countryKeys.elementAt(i);
            countryList.append(k + " (" + Library.by(true, k).size() + ")", null);
        }
        countryList.addCommand(cmdBack);
        countryList.setCommandListener(this);
        display.setCurrent(countryList);
    }

    private void showGenres() {
        genreKeys = Library.distinct(false);
        genreList = new List("Genres", List.IMPLICIT);
        for (int i = 0; i < genreKeys.size(); i++) {
            String k = (String) genreKeys.elementAt(i);
            genreList.append(k + " (" + Library.by(false, k).size() + ")", null);
        }
        genreList.addCommand(cmdBack);
        genreList.setCommandListener(this);
        display.setCurrent(genreList);
    }

    private void showSearch() {
        searchBox = new TextBox("Search name, country or genre", "", 30, TextField.ANY);
        searchBox.addCommand(cmdSearch);
        searchBox.addCommand(cmdBack);
        searchBox.setCommandListener(this);
        display.setCurrent(searchBox);
    }

    private void showSleep(Displayable back) {
        sleepBack = back;
        String[] items = {"Off", "10 minutes", "15 minutes", "30 minutes", "45 minutes",
                          "60 minutes", "90 minutes", "120 minutes"};
        sleepList = new List("Sleep Timer", List.IMPLICIT, items, null);
        sleepList.addCommand(cmdBack);
        sleepList.setCommandListener(this);
        display.setCurrent(sleepList);
    }

    private void showAdd() {
        addForm = new Form("Add Station");
        fName = new TextField("Name", "", 40, TextField.ANY);
        fUrl = new TextField("Stream URL (http://...)", "http://", 255, TextField.URL);
        fCountry = new TextField("Country", "My Stations", 30, TextField.ANY);
        fGenre = new TextField("Genre", "Eclectic", 30, TextField.ANY);
        addForm.append(fName);
        addForm.append(fUrl);
        addForm.append(fCountry);
        addForm.append(fGenre);
        addForm.addCommand(cmdSave);
        addForm.addCommand(cmdBack);
        addForm.setCommandListener(this);
        display.setCurrent(addForm);
    }

    private void addCmd(Command c) {
        if (c == cmdBack) { showMain(); return; }
        if (c != cmdSave) return;
        String n = fName.getString().trim();
        String u = fUrl.getString().trim();
        if (n.length() == 0 || u.length() < 8 || !u.startsWith("http")) {
            alert("Enter a name and a stream URL starting with http://", addForm);
            return;
        }
        if (Library.find(u) != null) {
            alert("That stream is already in the list.", addForm);
            return;
        }
        String co = fCountry.getString().trim();
        String ge = fGenre.getString().trim();
        if (co.length() == 0) co = "My Stations";
        if (ge.length() == 0) ge = "Eclectic";
        Library.addUser(new Station(co.replace('|', '/'), ge.replace('|', '/'), n.replace('|', '/'), u));
        String extra = u.startsWith("https") ? " Note: this phone may not support HTTPS streams." : "";
        updateMain();
        alert("Station added." + extra, mainMenu);
    }

    private void showTools() {
        String[] items = {
            "Import stations from URL",
            "Unhide hidden stations (" + Library.hiddenCount() + ")",
            "Clear recently played"
        };
        toolsList = new List("Station Tools", List.IMPLICIT, items, null);
        toolsList.addCommand(cmdBack);
        toolsList.setCommandListener(this);
        display.setCurrent(toolsList);
    }

    private void toolsCmd(Command c) {
        if (c == cmdBack) { showMain(); return; }
        if (c != List.SELECT_COMMAND) return;
        int i = toolsList.getSelectedIndex();
        if (i == 0) {
            importForm = new Form("Import Stations");
            fImport = new TextField("URL of a text file (http://...)", "http://", 255, TextField.URL);
            importForm.append(fImport);
            importForm.append(new StringItem(null, "One station per line: Country|Genre|Name|http://stream"));
            importForm.addCommand(cmdImport);
            importForm.addCommand(cmdBack);
            importForm.setCommandListener(this);
            display.setCurrent(importForm);
        } else if (i == 1) {
            Library.unhideAll();
            showTools();
            alert("All stations are visible again.", toolsList);
        } else if (i == 2) {
            Library.clearRecent();
            alert("Recently played list cleared.", toolsList);
        }
    }

    private void doImport() {
        final String u = fImport.getString().trim();
        if (u.length() < 8) return;
        Alert w = new Alert("Please wait", "Downloading list...", null, AlertType.INFO);
        w.setTimeout(Alert.FOREVER);
        display.setCurrent(w);
        new Thread(new Runnable() {
            public void run() {
                try {
                    String t = Net.httpGetText(u, 60000);
                    int n = Library.importText(t);
                    updateMain();
                    alert("Imported " + n + " new stations.", mainMenu);
                } catch (Throwable e) {
                    alert("Import failed: " + e.getMessage(), importForm);
                }
            }
        }).start();
    }

    private void showSettings() {
        settingsForm = new Form("Settings");
        setChoice = new ChoiceGroup("Options", Choice.MULTIPLE, new String[] {
            "Auto-play last station on start",
            "Auto-reconnect if stream drops",
            "Keep screen light on while playing"}, null);
        setChoice.setSelectedFlags(new boolean[] {
            Prefs.getBool("autoplay", false), Prefs.getBool("retry", true), light});
        setVol = new Gauge("Volume", true, 10, rp.volume / 10);
        settingsForm.append(setChoice);
        settingsForm.append(setVol);
        settingsForm.addCommand(cmdSave);
        settingsForm.addCommand(cmdBack);
        settingsForm.setCommandListener(this);
        display.setCurrent(settingsForm);
    }

    private void saveSettings() {
        boolean[] f = new boolean[3];
        setChoice.getSelectedFlags(f);
        Prefs.putBool("autoplay", f[0]);
        Prefs.putBool("retry", f[1]);
        Prefs.putBool("light", f[2]);
        rp.autoRetry = f[1];
        light = f[2];
        rp.setVolume(setVol.getValue() * 10);
        alert("Settings saved.", mainMenu);
    }

    private void showAbout() {
        aboutForm = new Form("Help / About");
        aboutForm.append(new StringItem(null,
            "Internet radio with " + Library.count() + " stations.\n\n"
            + "Now Playing keys:\n"
            + "Up/2 and Down/8: volume\n"
            + "Left/4 and Right/6: previous / next station\n"
            + "Centre/5: stop / play\n"
            + "1: favorite   3: random station\n"
            + "7: sleep timer   9: screen light\n"
            + "0: mute   * and #: volume\n\n"
            + "Options menu has everything else. "
            + "Press and hold the Menu key to leave the radio playing in the background.\n\n"
            + "Dead station? Hide it. Missing one? Add it, or edit stations.txt and rebuild.\n"
            + "HTTPS streams do not work on this phone."));
        aboutForm.addCommand(cmdBack);
        aboutForm.setCommandListener(this);
        display.setCurrent(aboutForm);
    }
}
