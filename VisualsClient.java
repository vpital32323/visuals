package com.troizy.visuals;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class VisualsClient implements ClientModInitializer {
    public static KeyBinding OPEN;

    @Override
    public void onInitializeClient() {
        Settings.load();
        OPEN = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.visuals.open", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "category.visuals"));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (OPEN.wasPressed()) {
                if (mc.currentScreen == null) mc.setScreen(new GlassScreen());
            }
        });

        HudRenderCallback.EVENT.register((ctx, tickDelta) -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.options.hudHidden) return;
            Gfx.alpha = 1f;
            if (Settings.crossOn && mc.options.getPerspective().isFirstPerson()) {
                Crosshair.draw(ctx, ctx.getScaledWindowWidth() / 2, ctx.getScaledWindowHeight() / 2,
                        Settings.style, Crosshair.currentColor(), Settings.size, Settings.gap, Settings.thick);
            }
            TopBar.render(ctx);
        });
    }
}
