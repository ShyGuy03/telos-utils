package com.telosutils;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

public class TelosUtilsClient implements ClientModInitializer {
    public static final DpsConfig CONFIG = DpsConfig.load();
    public static final DpsTracker TRACKER = new DpsTracker();

    private static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath("telosutils", "main"));
    public static final KeyMapping OPEN_MENU_KEY = KeyMappingHelper.registerKeyMapping(
            new KeyMapping("key.telosutils.open_menu", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_O, CATEGORY));

    /** Commands run while the chat screen is closing, so the menu is opened on the next tick instead. */
    private static boolean openMenuNextTick;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            TRACKER.tick(client);
            while (OPEN_MENU_KEY.consumeClick()) openMenuNextTick = true;
            if (openMenuNextTick && client.screen == null) {
                openMenuNextTick = false;
                client.setScreen(new DpsConfigScreen(null));
            }
        });
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("telosutils", "hud"), DpsHud::render);

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, buildContext) -> dispatcher.register(
                literal("dps")
                        .executes(ctx -> {
                            openMenuNextTick = true;
                            return 1;
                        })
                        .then(literal("menu").executes(ctx -> {
                            openMenuNextTick = true;
                            return 1;
                        }))
                        .then(literal("reset").executes(ctx -> {
                            TRACKER.reset();
                            ctx.getSource().sendFeedback(Component.literal("§a[DPS] §fReset."));
                            return 1;
                        }))
                        .then(literal("toggle").executes(ctx -> {
                            CONFIG.hudEnabled = !CONFIG.hudEnabled;
                            CONFIG.save();
                            ctx.getSource().sendFeedback(Component.literal("§a[DPS] §fHUD " + (CONFIG.hudEnabled ? "on" : "off")));
                            return 1;
                        }))
                        .then(literal("debug").executes(ctx -> {
                            CONFIG.debug = !CONFIG.debug;
                            CONFIG.save();
                            ctx.getSource().sendFeedback(Component.literal("§a[DPS] §fDebug " + (CONFIG.debug ? "on" : "off")));
                            return 1;
                        }))
                        .then(literal("range").then(argument("blocks", IntegerArgumentType.integer(1, 64)).executes(ctx -> {
                            CONFIG.range = IntegerArgumentType.getInteger(ctx, "blocks");
                            CONFIG.save();
                            ctx.getSource().sendFeedback(Component.literal("§a[DPS] §fRange set to " + CONFIG.range + " blocks"));
                            return 1;
                        })))
        ));
    }
}
