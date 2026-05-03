import javax.sound.sampled.*;
import javazoom.jl.player.advanced.AdvancedPlayer;
import javazoom.jl.player.advanced.PlaybackEvent;
import javazoom.jl.player.advanced.PlaybackListener;
import java.io.*;

public class AudioPlayer {

    /** Callback interface used by MusicPlayerFrame */
    public interface PlayerCallback {
        void onFinished();
        void onPositionChanged(long microseconds, long total);
    }

    private PlayerCallback callback;

    // --- WAV/AIFF state ---
    private Clip clip;

    // --- MP3 state ---
    private AdvancedPlayer   mp3Player;
    private Thread           mp3Thread;
    private File             currentFile;
    private long             mp3TotalBytes;
    private long             mp3BytesPlayed;
    private long             mp3DurationMicros;
    private volatile boolean mp3Active = false;

    // --- Shared ---
    private volatile float volume = 0.8f;
    private boolean isMP3 = false;

    public void setListener(PlayerCallback cb) { this.callback = cb; }

    // ── Load ─────────────────────────────────────────────────
    public boolean load(File file) {
        stop();
        currentFile    = file;
        mp3BytesPlayed = 0;
        isMP3          = file.getName().toLowerCase().endsWith(".mp3");
        return isMP3 ? loadMP3(file, 0) : loadClip(file);
    }

    private boolean loadClip(File file) {
        try {
            AudioInputStream ais = AudioSystem.getAudioInputStream(file);
            AudioFormat base = ais.getFormat();
            AudioFormat decoded = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                base.getSampleRate(), 16,
                base.getChannels(), base.getChannels() * 2,
                base.getSampleRate(), false);
            AudioInputStream dec = AudioSystem.getAudioInputStream(decoded, ais);
            clip = AudioSystem.getClip();
            clip.open(dec);
            clip.addLineListener(ev -> {
                if (ev.getType() == LineEvent.Type.STOP && clip != null) {
                    if (clip.getMicrosecondPosition() >= clip.getMicrosecondLength() - 10_000) {
                        if (callback != null) callback.onFinished();
                    }
                }
            });
            startClipPositionTimer();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean loadMP3(File file, long skipBytes) {
        try {
            mp3TotalBytes     = file.length();
            mp3DurationMicros = estimateMP3Duration(file);
            mp3BytesPlayed    = skipBytes;
            FileInputStream fis = new FileInputStream(file);
            if (skipBytes > 0) fis.skip(skipBytes);
            mp3Player = new AdvancedPlayer(fis);
            mp3Player.setPlayBackListener(new JLayerCallback());
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // ── Play ─────────────────────────────────────────────────
    public void play() {
        if (isMP3) {
            if (mp3Player == null) return;
            mp3Active = true;
            startMP3PositionTimer();
            mp3Thread = new Thread(() -> {
                try { mp3Player.play(); } catch (Exception ignored) {}
            }, "mp3-player");
            mp3Thread.setDaemon(true);
            mp3Thread.start();
        } else {
            if (clip == null) return;
            clip.start();
            applyClipVolume();
        }
    }

    // ── Pause ────────────────────────────────────────────────
    public void pause() {
        if (isMP3) {
            if (mp3Player == null) return;
            // Snapshot position via timer's latest value, then stop
            mp3Active = false;
            mp3Player.stop();
            mp3Player = null;
            // Reload from paused position so next play() resumes correctly
            loadMP3(currentFile, mp3BytesPlayed);
        } else {
            if (clip == null || !clip.isRunning()) return;
            clip.stop();
        }
    }

    // ── Stop ─────────────────────────────────────────────────
    public void stop() {
        mp3Active      = false;
        mp3BytesPlayed = 0;
        if (mp3Player != null) { mp3Player.stop(); mp3Player = null; }
        if (mp3Thread  != null) { mp3Thread.interrupt(); mp3Thread = null; }
        if (clip != null)       { clip.stop(); clip.close(); clip = null; }
    }

    // ── Seek ─────────────────────────────────────────────────
    public void seek(long microseconds) {
        if (isMP3) {
            boolean wasPlaying = isPlaying();
            mp3Active = false;
            if (mp3Player != null) { mp3Player.stop(); mp3Player = null; }
            long dur = getDuration();
            long targetBytes = dur > 0 ? (long)((microseconds / (double) dur) * mp3TotalBytes) : 0;
            mp3BytesPlayed = targetBytes;
            loadMP3(currentFile, targetBytes);
            if (wasPlaying) play();
        } else {
            if (clip == null) return;
            boolean wasPlaying = clip.isRunning();
            clip.stop();
            clip.setMicrosecondPosition(microseconds);
            if (wasPlaying) { clip.start(); applyClipVolume(); }
        }
    }

    // ── Volume ───────────────────────────────────────────────
    public void setVolume(float gain) {
        this.volume = Math.max(0f, Math.min(1f, gain));
        applyClipVolume();
    }

    private void applyClipVolume() {
        if (clip == null) return;
        if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            FloatControl fc = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            float dB = (float)(Math.log10(Math.max(volume, 0.0001)) * 20.0);
            fc.setValue(Math.max(fc.getMinimum(), Math.min(fc.getMaximum(), dB)));
        }
    }

    // ── Queries ──────────────────────────────────────────────
    public boolean isPlaying() {
        if (isMP3) return mp3Active && mp3Thread != null && mp3Thread.isAlive();
        return clip != null && clip.isRunning();
    }

    public long getDuration() {
        if (isMP3) return mp3DurationMicros;
        return clip != null ? clip.getMicrosecondLength() : 0;
    }

    public long getPosition() {
        if (isMP3) {
            if (mp3TotalBytes <= 0) return 0;
            return (long)(mp3BytesPlayed / (double) mp3TotalBytes * mp3DurationMicros);
        }
        return clip != null ? clip.getMicrosecondPosition() : 0;
    }

    public File getCurrentFile() { return currentFile; }

    // ── Internal helpers ─────────────────────────────────────

    private void startClipPositionTimer() {
        Thread t = new Thread(() -> {
            while (clip != null) {
                try {
                    Thread.sleep(400);
                    if (clip != null && callback != null) {
                        callback.onPositionChanged(
                            clip.getMicrosecondPosition(), clip.getMicrosecondLength());
                    }
                } catch (InterruptedException e) { break; }
            }
        }, "clip-tracker");
        t.setDaemon(true);
        t.start();
    }

    /** Timer-based MP3 position approximation (JLayer has no position callback) */
    private void startMP3PositionTimer() {
        final long bytesAtStart = mp3BytesPlayed;
        final long timeAtStart  = System.currentTimeMillis();
        Thread t = new Thread(() -> {
            while (mp3Active) {
                try {
                    Thread.sleep(400);
                    if (!mp3Active) break;
                    long elapsed  = System.currentTimeMillis() - timeAtStart;
                    double durMs  = mp3DurationMicros / 1000.0;
                    long estBytes = durMs > 0
                        ? bytesAtStart + (long)(elapsed * (mp3TotalBytes - bytesAtStart) / durMs)
                        : bytesAtStart;
                    mp3BytesPlayed = Math.min(estBytes, mp3TotalBytes);
                    if (callback != null && mp3DurationMicros > 0) {
                        long pos = (long)(mp3BytesPlayed / (double) mp3TotalBytes * mp3DurationMicros);
                        callback.onPositionChanged(pos, mp3DurationMicros);
                    }
                } catch (InterruptedException e) { break; }
            }
        }, "mp3-tracker");
        t.setDaemon(true);
        t.start();
    }

    private long estimateMP3Duration(File f) {
        try (FileInputStream fis = new FileInputStream(f)) {
            byte[] buf = new byte[4096];
            fis.read(buf);
            for (int i = 0; i < buf.length - 4; i++) {
                if ((buf[i] & 0xFF) == 0xFF && (buf[i+1] & 0xE0) == 0xE0) {
                    int header     = ((buf[i] & 0xFF) << 24) | ((buf[i+1] & 0xFF) << 16)
                                   | ((buf[i+2] & 0xFF) << 8)  |  (buf[i+3] & 0xFF);
                    int bitrateIdx = (header >> 12) & 0xF;
                    int[] rates    = {0,32,40,48,56,64,80,96,112,128,160,192,224,256,320,0};
                    int kbps = bitrateIdx < rates.length ? rates[bitrateIdx] : 128;
                    if (kbps <= 0) kbps = 128;
                    return (f.length() * 8L * 1_000_000L) / (kbps * 1000L);
                }
            }
        } catch (Exception ignored) {}
        return (f.length() * 8L * 1_000_000L) / 128_000L;
    }

    // ── JLayer callback (only playbackStarted/playbackFinished exist) ──
    private class JLayerCallback extends PlaybackListener {
        @Override public void playbackStarted(PlaybackEvent e) {}

        @Override
        public void playbackFinished(PlaybackEvent e) {
            if (mp3Active && callback != null) {
                mp3Active      = false;
                mp3BytesPlayed = 0;
                callback.onFinished();
            }
        }
    }
}
