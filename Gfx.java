package com.troizy.visuals;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

/** Помощники для отрисовки: скруглённые прямоугольники, стекло, жирный текст. */
public final class Gfx {
    /** Глобальная прозрачность (для анимации появления) */
    public static float alpha = 1f;
    private static final Style BOLD = Style.EMPTY.withBold(true);

    private static TextRenderer tr() { return MinecraftClient.getInstance().textRenderer; }

    public static int a(int c) {
        int al = (int) (((c >>> 24) & 255) * alpha);
        return (al << 24) | (c & 0xFFFFFF);
    }

    public static int rgba(int rgb, int al) { return (Math.max(0, Math.min(255, al)) << 24) | (rgb & 0xFFFFFF); }

    public static int mix(int c1, int c2, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int r = 0;
        for (int s = 24; s >= 0; s -= 8) {
            int x = (c1 >>> s) & 255, y = (c2 >>> s) & 255;
            r |= ((int) (x + (y - x) * t) & 255) << s;
        }
        return r;
    }

    public static float approach(float cur, float target, float speed) {
        return cur + (target - cur) * Math.min(1f, speed);
    }

    private static int inset(int r, int j) {
        if (j >= r) return 0;
        double d = r - j - 0.5;
        return r - (int) Math.round(Math.sqrt(Math.max(0, r * r - d * d)));
    }

    public static void rrect(DrawContext c, int x, int y, int w, int h, int r, int col) {
        if (w <= 0 || h <= 0) return;
        col = a(col);
        if ((col >>> 24) == 0) return;
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
        for (int i = 0; i < h; i++) {
            int in = inset(r, Math.min(i, h - 1 - i));
            c.fill(x + in, y + i, x + w - in, y + i + 1, col);
        }
    }

    public static void outline(DrawContext c, int x, int y, int w, int h, int r, int col) {
        if (w <= 0 || h <= 0) return;
        col = a(col);
        if ((col >>> 24) == 0) return;
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
        for (int i = 0; i < h; i++) {
            int j = Math.min(i, h - 1 - i);
            int in = inset(r, j);
            if (j == 0) { c.fill(x + in, y + i, x + w - in, y + i + 1, col); continue; }
            int out = Math.max(in + 1, inset(r, j - 1));
            c.fill(x + in, y + i, x + out, y + i + 1, col);
            c.fill(x + w - out, y + i, x + w - in, y + i + 1, col);
        }
    }

    public static void circle(DrawContext c, int cx, int cy, int r, int col) {
        rrect(c, cx - r, cy - r, r * 2, r * 2, r, col);
    }

    /** Стеклянная панель: тень, полупрозрачное тело, блик сверху, светлая обводка. */
    public static void glass(DrawContext c, int x, int y, int w, int h, int r, int tint, int bodyAlpha, int outlineCol) {
        for (int i = 3; i >= 1; i--) rrect(c, x - i, y - i, w + i * 2, h + i * 2, r + i, rgba(0x000000, 16 / i + 4));
        rrect(c, x, y, w, h, r, rgba(tint, bodyAlpha));
        rrect(c, x + 1, y + 1, w - 2, Math.max(2, h / 2), r, 0x16FFFFFF);
        rrect(c, x + 1, y + 1 + h / 2, w - 2, Math.max(2, h / 2 - 2), r, 0x08000000);
        outline(c, x, y, w, h, r, outlineCol);
        c.fill(x + r, y + 1, x + w - r, y + 2, a(0x40FFFFFF));
    }

    public static int width(String s) { return tr().getWidth(Text.literal(s).setStyle(BOLD)); }

    public static void text(DrawContext c, String s, int x, int y, int col) {
        col = a(col);
        if ((col >>> 24) < 4) return;
        c.drawText(tr(), Text.literal(s).setStyle(BOLD), x, y, col, true);
    }
}
