package com.troizy.visuals;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;

public class GlassScreen extends Screen {
    private static final String[] TABS = {"Crosshair", "HUD", "Themes", "Appearance", "Config"};
    private static final int[] SWATCH = {0xFFFFFF, 0x3DDC84, 0xFFE94D, 0xFFB020, 0xFF7A30, 0xFF4D4D, 0xFF5FD2, 0x35E0FF};
    private static final int[][] PRESETS = {{0, 0xFFFFFF}, {2, 0xFFFFFF}, {3, 0xFFFFFF}, {0, 0x35E0FF}, {1, 0x3DDC84}, {4, 0xFF4D4D}};
    private static final int W = 400, H = 260;

    private record Hit(int x, int y, int w, int h, boolean draggable, DoubleConsumer act) {
        boolean in(double mx, double my) { return mx >= x && mx < x + w && my >= y && my < y + h; }
    }

    private int tab = 0;
    private float open = 0f, tabAnim = 1f;
    private int mx, my;
    private float sp;
    private final Map<String, Float> hov = new HashMap<>();
    private final List<Hit> hits = new ArrayList<>();
    private Hit dragging;

    public GlassScreen() { super(Text.literal(Settings.NAME)); }

    @Override public boolean shouldPause() { return false; }

    @Override
    public void removed() { Settings.save(); }

    // ---------- helpers ----------
    private int acc() { return Settings.accent; }

    private boolean over(int x, int y, int w, int h) { return mx >= x && mx < x + w && my >= y && my < y + h; }

    private float anim(String id, float target, float def) {
        float v = Gfx.approach(hov.getOrDefault(id, def), target, sp * 1.5f);
        hov.put(id, v);
        return v;
    }

    private int header(DrawContext c, String t, int x, int y) {
        Gfx.circle(c, x + 2, y + 4, 2, 0xFF000000 | acc());
        Gfx.text(c, t, x + 8, y, 0xA0FFFFFF);
        return y + 13;
    }

    private int toggle(DrawContext c, String id, String label, boolean val, Consumer<Boolean> set, int x, int y, int w) {
        float h = anim(id + "h", over(x, y, w, 16) ? 1 : 0, 0);
        float k = anim(id, val ? 1 : 0, val ? 1 : 0);
        Gfx.text(c, label, x, y + 4, Gfx.mix(0xB0FFFFFF, 0xFFFFFFFF, h));
        int tx = x + w - 26;
        Gfx.rrect(c, tx, y + 2, 24, 11, 5, Gfx.mix(0x55FFFFFF, 0xFF000000 | acc(), k));
        Gfx.rrect(c, tx + 2 + (int) (13 * k), y + 4, 7, 7, 3, 0xFFFFFFFF);
        hits.add(new Hit(x, y, w, 16, false, v -> set.accept(!val)));
        return y + 18;
    }

    private int slider(DrawContext c, String label, int min, int max, int val, IntConsumer set, int x, int y, int w) {
        Gfx.text(c, label, x, y, 0xB0FFFFFF);
        String vs = "" + val;
        Gfx.text(c, vs, x + w - Gfx.width(vs), y, 0xFFFFFFFF);
        int by = y + 12;
        float f = (val - min) / (float) (max - min);
        int fw = Math.max(4, (int) (w * f));
        Gfx.rrect(c, x, by, w, 4, 2, 0x40FFFFFF);
        Gfx.rrect(c, x, by, fw, 4, 2, 0xFF000000 | acc());
        Gfx.circle(c, x + (int) (w * f), by + 2, 4, 0xFFFFFFFF);
        hits.add(new Hit(x, y + 6, w, 16, true, m -> {
            float t = (float) Math.max(0, Math.min(1, (m - x) / w));
            set.accept(min + Math.round(t * (max - min)));
        }));
        return y + 24;
    }

    private void button(DrawContext c, String id, String label, int x, int y, int w, int h, boolean sel, Runnable r) {
        float k = anim(id, over(x, y, w, h) ? 1 : 0, 0);
        Gfx.rrect(c, x, y, w, h, 5, Gfx.rgba(acc(), (int) ((sel ? 0x55 : 0x18) + 0x25 * k)));
        Gfx.outline(c, x, y, w, h, 5, sel ? (0xCC000000 | acc()) : 0x30FFFFFF);
        if (label != null) Gfx.text(c, label, x + (w - Gfx.width(label)) / 2, y + (h - 8) / 2, 0xFFFFFFFF);
        hits.add(new Hit(x, y, w, h, false, m -> r.run()));
    }

    private void swatch(DrawContext c, String id, int rgb, int x, int y, boolean sel, Runnable r) {
        float k = anim(id, over(x - 2, y - 2, 16, 16) ? 1 : 0, 0);
        if (sel || k > 0.05f) Gfx.circle(c, x + 6, y + 6, 7, sel ? 0xFFFFFFFF : Gfx.rgba(0xFFFFFF, (int) (120 * k)));
        Gfx.circle(c, x + 6, y + 6, 5, 0xFF000000 | rgb);
        hits.add(new Hit(x - 2, y - 2, 16, 16, false, m -> r.run()));
    }

    // ---------- render ----------
    @Override
    public void render(DrawContext c, int mouseX, int mouseY, float delta) {
        mx = mouseX; my = mouseY;
        hits.clear();
        sp = Settings.anims ? Math.min(1f, delta * 0.45f) : 1f;
        open = Gfx.approach(open, 1f, sp);
        tabAnim = Gfx.approach(tabAnim, 1f, sp * 1.2f);
        Gfx.alpha = open;
        try { draw(c); } finally { Gfx.alpha = 1f; }
    }

    private void draw(DrawContext c) {
        c.fill(0, 0, width, height, Gfx.a(0x66000000));
        int x0 = (width - W) / 2, y0 = (height - H) / 2;
        float s = Settings.anims ? 0.94f + 0.06f * open : 1f;

        c.getMatrices().push();
        c.getMatrices().translate(width / 2f, height / 2f, 0);
        c.getMatrices().scale(s, s, 1f);
        c.getMatrices().translate(-width / 2f, -height / 2f, 0);

        int r = Settings.radius + 4;
        Gfx.glass(c, x0, y0, W, H, r, 0x0B1210, Settings.bodyAlpha(), 0x40FFFFFF);

        // --- sidebar ---
        Gfx.circle(c, x0 + 17, y0 + 19, 6, 0xFF000000 | acc());
        Gfx.circle(c, x0 + 17, y0 + 19, 3, 0x66FFFFFF);
        Gfx.text(c, Settings.NAME, x0 + 28, y0 + 15, 0xFFFFFFFF);
        c.fill(x0 + 10, y0 + 34, x0 + 100, y0 + 35, Gfx.a(0x22FFFFFF));

        for (int i = 0; i < TABS.length; i++) {
            int ty = y0 + 42 + i * 22;
            boolean selected = i == tab;
            float hv = anim("tabh" + i, over(x0 + 8, ty, 92, 18) ? 1 : 0, 0);
            float se = anim("tabs" + i, selected ? 1 : 0, selected ? 1 : 0);
            Gfx.rrect(c, x0 + 8, ty, 92, 18, 5, Gfx.rgba(acc(), (int) (0x55 * se + 0x20 * hv)));
            if (se > 0.02f) Gfx.rrect(c, x0 + 8, ty + 4, 2, 10, 1, Gfx.rgba(acc(), (int) (255 * se)));
            Gfx.text(c, TABS[i], x0 + 18 + (int) (3 * hv), ty + 5, Gfx.mix(0xB0FFFFFF, 0xFFFFFFFF, Math.max(se, hv)));
            final int idx = i;
            hits.add(new Hit(x0 + 8, ty, 92, 18, false, m -> { if (tab != idx) { tab = idx; tabAnim = 0f; } }));
        }
        c.fill(x0 + 10, y0 + H - 32, x0 + 100, y0 + H - 31, Gfx.a(0x22FFFFFF));
        String user = MinecraftClient.getInstance().getSession().getUsername();
        Gfx.text(c, user, x0 + 12, y0 + H - 25, 0xC0FFFFFF);
        Gfx.text(c, "Right Shift", x0 + 12, y0 + H - 14, 0x70FFFFFF);

        // --- content ---
        int px = x0 + 108, py = y0 + 10, pw = W - 118, ph = H - 20;
        Gfx.rrect(c, px, py, pw, ph, r - 2, 0x1CFFFFFF);
        Gfx.outline(c, px, py, pw, ph, r - 2, 0x26FFFFFF);
        Gfx.circle(c, px + 12, py + 12, 3, 0xFF000000 | acc());
        Gfx.text(c, TABS[tab], px + 20, py + 8, 0xFFFFFFFF);
        c.fill(px + 10, py + 22, px + pw - 10, py + 23, Gfx.a(0x1EFFFFFF));

        float keep = Gfx.alpha;
        Gfx.alpha = keep * tabAnim;
        int off = (int) ((1f - tabAnim) * 10);
        int cx = px + 12 + off, cy = py + 30, cw = pw - 24;
        switch (tab) {
            case 0 -> crosshairTab(c, cx, cy, cw);
            case 1 -> hudTab(c, cx, cy, cw);
            case 2 -> themesTab(c, cx, cy, cw);
            case 3 -> appearanceTab(c, cx, cy, cw);
            default -> configTab(c, cx, cy, cw);
        }
        Gfx.alpha = keep;

        c.getMatrices().pop();
    }

    // ---------- tabs ----------
    private void crosshairTab(DrawContext c, int x, int y, int w) {
        y = toggle(c, "cOn", "Enabled", Settings.crossOn, v -> Settings.crossOn = v, x, y, w);

        y = header(c, "Presets", x, y + 2);
        int bw = (w - 5 * 4) / 6;
        for (int i = 0; i < 6; i++) {
            int bx = x + i * (bw + 4);
            final int[] p = PRESETS[i];
            button(c, "pre" + i, null, bx, y, bw, 20, Settings.style == p[0] && Settings.color == p[1],
                    () -> { Settings.style = p[0]; Settings.color = p[1]; Settings.chroma = false; });
            Crosshair.draw(c, bx + bw / 2, y + 10, p[0], p[1], 3, 2, 1);
        }
        y += 24;

        y = header(c, "Style", x, y);
        for (int i = 0; i < Crosshair.STYLES; i++) {
            int bx = x + i * (bw + 4);
            final int st = i;
            button(c, "sty" + i, null, bx, y, bw, 20, Settings.style == i, () -> Settings.style = st);
            Crosshair.draw(c, bx + bw / 2, y + 10, i, Crosshair.currentColor(), 3, 2, 1);
        }
        y += 24;

        y = header(c, "Quick colors", x, y);
        for (int i = 0; i < SWATCH.length; i++) {
            final int col = SWATCH[i];
            swatch(c, "qc" + i, col, x + i * 17, y, !Settings.chroma && Settings.color == col,
                    () -> { Settings.color = col; Settings.chroma = false; });
        }
        y += 18;
        y = toggle(c, "chroma", "Chroma (rainbow)", Settings.chroma, v -> Settings.chroma = v, x, y, w);
        y = slider(c, "Size", 1, 12, Settings.size, v -> Settings.size = v, x, y + 2, w);
        y = slider(c, "Gap", 0, 10, Settings.gap, v -> Settings.gap = v, x, y, w);
        slider(c, "Thickness", 1, 3, Settings.thick, v -> Settings.thick = v, x, y, w);
    }

    private void hudTab(DrawContext c, int x, int y, int w) {
        y = toggle(c, "tb", "Top bar", Settings.topBar, v -> Settings.topBar = v, x, y, w);
        y = toggle(c, "tt", "Show time", Settings.showTime, v -> Settings.showTime = v, x, y, w);
        y = toggle(c, "tp", "Show name pill", Settings.showPill, v -> Settings.showPill = v, x, y, w);
        y = toggle(c, "tf", "Show FPS", Settings.showFps, v -> Settings.showFps = v, x, y, w);
        toggle(c, "tg", "Show ping", Settings.showPing, v -> Settings.showPing = v, x, y, w);
    }

    private void themesTab(DrawContext c, int x, int y, int w) {
        y = header(c, "Accent color", x, y);
        for (int i = 0; i < SWATCH.length; i++) {
            final int col = i == 0 ? 0x3DDC84 : SWATCH[i];
            swatch(c, "ac" + i, col, x + i * 17, y, Settings.accent == col, () -> Settings.accent = col);
        }
        y += 24;
        y = header(c, "Preview", x, y);
        TopBar.pill(c, x + 4, y + 4, "Menu opened");
    }

    private void appearanceTab(DrawContext c, int x, int y, int w) {
        y = slider(c, "Glass opacity", 0, 100, Settings.glass, v -> Settings.glass = v, x, y, w);
        y = slider(c, "Corner radius", 2, 10, Settings.radius, v -> Settings.radius = v, x, y, w);
        toggle(c, "an", "Animations", Settings.anims, v -> Settings.anims = v, x, y, w);
    }

    private void configTab(DrawContext c, int x, int y, int w) {
        int bw = (w - 8) / 3;
        button(c, "sv", "Save", x, y, bw, 22, false, Settings::save);
        button(c, "ld", "Load", x + bw + 4, y, bw, 22, false, Settings::load);
        button(c, "rs", "Reset", x + (bw + 4) * 2, y, bw, 22, false, Settings::reset);
        Gfx.text(c, "Config: config/visuals.properties", x, y + 32, 0x80FFFFFF);
        Gfx.text(c, "Auto-saved when the menu closes", x, y + 44, 0x80FFFFFF);
    }

    // ---------- input ----------
    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            for (int i = hits.size() - 1; i >= 0; i--) {
                Hit h = hits.get(i);
                if (h.in(mx, my)) {
                    h.act().accept(mx);
                    if (h.draggable()) dragging = h;
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging != null) { dragging.act().accept(mx); return true; }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        dragging = null;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT || VisualsClient.OPEN.matchesKey(keyCode, scanCode)) {
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
