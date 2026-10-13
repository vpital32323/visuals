package com.troizy.visuals;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/** Верхняя стеклянная панель: время, зелёная плашка с названием, fps и пинг. */
public final class TopBar {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final int H = 16, GAP = 5;

    private static int ping(MinecraftClient mc) {
        if (mc.getNetworkHandler() == null || mc.player == null) return 0;
        PlayerListEntry e = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
        return e == null ? 0 : e.getLatency();
    }

    /** Зелёная стеклянная плашка. Возвращает ширину. */
    public static int pill(DrawContext c, int x, int y, String text) {
        int w = Gfx.width(text) + 28;
        int acc = 0xFF000000 | Settings.accent;
        for (int i = 4; i >= 1; i--) Gfx.rrect(c, x - i, y - i, w + i * 2, H + i * 2, Settings.radius + i, Gfx.rgba(Settings.accent, 10));
        Gfx.glass(c, x, y, w, H, Settings.radius, Settings.accent, 0x78, Gfx.rgba(Settings.accent, 0xD0));
        float pulse = 0.55f + 0.45f * (float) Math.sin(System.currentTimeMillis() / 350.0);
        Gfx.circle(c, x + 9, y + H / 2, 3, Gfx.rgba(0xFFFFFF, (int) (255 * pulse)));
        Gfx.text(c, text, x + 18, y + 4, 0xFFFFFFFF);
        return w;
    }

    public static void render(DrawContext c) {
        if (!Settings.topBar) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options.hudHidden) return;
        Gfx.alpha = 1f;

        boolean menu = mc.currentScreen instanceof GlassScreen;
        String time = LocalTime.now().format(FMT);
        String pillText = menu ? "Menu opened" : Settings.NAME;
        String fps = mc.getCurrentFps() + " fps";
        int ping = ping(mc);
        String png = ping + " ms";

        int wTime = Settings.showTime ? Gfx.width(time) + 16 : 0;
        int wPill = Settings.showPill ? Gfx.width(pillText) + 28 : 0;
        int wStat = 0;
        if (Settings.showFps) wStat += Gfx.width(fps) + 8;
        if (Settings.showPing) wStat += 16 + Gfx.width(png) + 8;
        if (wStat > 0) wStat += 8;

        int count = (wTime > 0 ? 1 : 0) + (wPill > 0 ? 1 : 0) + (wStat > 0 ? 1 : 0);
        if (count == 0) return;
        int total = wTime + wPill + wStat + GAP * (count - 1);
        int x = (c.getScaledWindowWidth() - total) / 2;
        int y = 5;
        int body = Settings.bodyAlpha();

        if (wTime > 0) {
            Gfx.glass(c, x, y, wTime, H, Settings.radius, 0x0B1210, body, 0x38FFFFFF);
            Gfx.text(c, time, x + 8, y + 4, 0xFFFFFFFF);
            x += wTime + GAP;
        }
        if (wPill > 0) {
            pill(c, x, y, pillText);
            x += wPill + GAP;
        }
        if (wStat > 0) {
            Gfx.glass(c, x, y, wStat, H, Settings.radius, 0x0B1210, body, 0x38FFFFFF);
            int px = x + 8;
            if (Settings.showFps) {
                Gfx.text(c, fps, px, y + 4, 0xFFFFFFFF);
                px += Gfx.width(fps) + 8;
            }
            if (Settings.showPing) {
                int level = ping < 60 ? 4 : ping < 120 ? 3 : ping < 200 ? 2 : 1;
                int col = level >= 3 ? 0xFF3DDC84 : level == 2 ? 0xFFFFD84D : 0xFFFF4D4D;
                for (int i = 0; i < 4; i++) {
                    int bh = 3 + i * 2;
                    Gfx.rrect(c, px + i * 3, y + 12 - bh, 2, bh, 1, i < level ? col : 0x40FFFFFF);
                }
                Gfx.text(c, png, px + 16, y + 4, 0xFFFFFFFF);
            }
        }
    }
}
