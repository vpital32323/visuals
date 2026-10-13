package com.troizy.visuals;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class Settings {
    /** Название твоих визуалов — меняется здесь в одном месте */
    public static String NAME = "Troizy Visuals";

    public static boolean crossOn = true, chroma = false;
    public static boolean topBar = true, showTime = true, showFps = true, showPing = true, showPill = true;
    public static boolean anims = true;
    public static int style = 0, size = 5, gap = 3, thick = 1;
    public static int color = 0xFFFFFF;
    public static int accent = 0x3DDC84;
    public static int radius = 6;
    public static int glass = 55; // 0..100

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("visuals.properties");
    }

    public static int bodyAlpha() { return 0x30 + (int) (glass / 100f * 0x70); }

    public static void reset() {
        crossOn = true; chroma = false;
        topBar = showTime = showFps = showPing = showPill = true;
        anims = true;
        style = 0; size = 5; gap = 3; thick = 1;
        color = 0xFFFFFF; accent = 0x3DDC84; radius = 6; glass = 55;
    }

    public static void save() {
        Properties p = new Properties();
        p.setProperty("crossOn", "" + crossOn);
        p.setProperty("chroma", "" + chroma);
        p.setProperty("topBar", "" + topBar);
        p.setProperty("showTime", "" + showTime);
        p.setProperty("showFps", "" + showFps);
        p.setProperty("showPing", "" + showPing);
        p.setProperty("showPill", "" + showPill);
        p.setProperty("anims", "" + anims);
        p.setProperty("style", "" + style);
        p.setProperty("size", "" + size);
        p.setProperty("gap", "" + gap);
        p.setProperty("thick", "" + thick);
        p.setProperty("color", "" + color);
        p.setProperty("accent", "" + accent);
        p.setProperty("radius", "" + radius);
        p.setProperty("glass", "" + glass);
        try (Writer w = Files.newBufferedWriter(file())) {
            p.store(w, "Troizy Visuals");
        } catch (IOException ignored) {}
    }

    public static void load() {
        Path f = file();
        if (!Files.exists(f)) return;
        Properties p = new Properties();
        try (Reader r = Files.newBufferedReader(f)) {
            p.load(r);
        } catch (IOException e) { return; }
        try {
            crossOn = b(p, "crossOn", crossOn);
            chroma = b(p, "chroma", chroma);
            topBar = b(p, "topBar", topBar);
            showTime = b(p, "showTime", showTime);
            showFps = b(p, "showFps", showFps);
            showPing = b(p, "showPing", showPing);
            showPill = b(p, "showPill", showPill);
            anims = b(p, "anims", anims);
            style = i(p, "style", style);
            size = i(p, "size", size);
            gap = i(p, "gap", gap);
            thick = i(p, "thick", thick);
            color = i(p, "color", color);
            accent = i(p, "accent", accent);
            radius = i(p, "radius", radius);
            glass = i(p, "glass", glass);
        } catch (NumberFormatException ignored) {}
    }

    private static boolean b(Properties p, String k, boolean d) { return Boolean.parseBoolean(p.getProperty(k, "" + d)); }
    private static int i(Properties p, String k, int d) { return Integer.parseInt(p.getProperty(k, "" + d)); }
}
