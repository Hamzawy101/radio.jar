# Radio - internet radio MIDlet for Nokia S60 (E63 etc.)

165 stations worldwide, favorites, recents, country/genre browsing, search, random,
sleep timer, auto-reconnect, add/import your own stations.

## Build on GitHub (phone-friendly)
1. Put all these files in a repo (the workflow goes in `.github/workflows/build.yml`; it builds with no extra scripts).
2. Push. Open the **Actions** tab -> "Build Radio MIDlet".
3. When it finishes, grab `Radio.jar` + `Radio.jad` from the **Releases** page ("latest")
   or from the run's Artifacts.
4. Copy both to the phone (USB/Bluetooth/memory card) and open the .jar or .jad.

The same run produces a **station-report** (job summary) showing which streams answer.
Edit `stations.txt` on GitHub, commit, and it rebuilds.

## Station file format
`Country|Genre|Name|http://stream-url`  (plain http only; the E63 cannot do HTTPS)
