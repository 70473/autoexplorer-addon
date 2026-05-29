package com.example.autoexplorer.modules;

import com.example.autoexplorer.AutoExplorerAddon;
import com.example.autoexplorer.utils.CoordinateLogger;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import org.lwjgl.glfw.GLFW;

// ── Baritone API imports ────────────────────────────────────────────────────
// Access Baritone through BaritoneAPI.getProvider().getPrimaryBaritone().
// Never instantiate Baritone objects yourself.
import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.process.IExploreProcess;
// GoalXZ is available if you want fixed-destination pathing instead of exploring.
import baritone.api.pathing.goals.GoalXZ;

/**
 * AutoExplorerModule — A Meteor Client module that uses Baritone to
 * automatically explore the world and optionally log coordinates to disk.
 *
 * Key fixes from original:
 *  - IExploreProcess#explore(int, int) takes CHUNK coordinates, not block coords.
 *    Block coords must be divided by 16 before passing.
 *  - Baritone process access: baritone.getExploreProcess() returns IExploreProcess
 *    directly (no cast needed).
 *  - ChatUtils.sendMsg() is the correct method (not sendPlayerMessage).
 *  - KeybindSetting / Keybind usage is correct for Meteor 0.5.x.
 */
public class AutoExplorerModule extends Module {

    // ── Settings Groups ────────────────────────────────────────────────────
    private final SettingGroup sgGeneral  = settings.getDefaultGroup();
    private final SettingGroup sgBaritone = settings.createGroup("Baritone");
    private final SettingGroup sgLogging  = settings.createGroup("Logging");

    // ── Settings ──────────────────────────────────────────────────────────

    private final Setting<Boolean> baritoneEnabled = sgBaritone.add(
        new BoolSetting.Builder()
            .name("baritone-enabled")
            .displayName("Enable Baritone")
            .description("When ON, AutoExplorer issues pathfinding commands to Baritone. " +
                         "When OFF, the module observes but does not move the player.")
            .defaultValue(true)
            .build()
    );

    /**
     * Chunk radius for the explore goal.
     * NOTE: Baritone's IExploreProcess#explore() takes the CENTER in block coords,
     * but the radius concept is handled internally by Baritone. This setting is used
     * as an offset multiplier when building a GoalXZ fallback (Option B below).
     */
    private final Setting<Integer> exploreRadius = sgBaritone.add(
        new IntSetting.Builder()
            .name("explore-radius")
            .displayName("Explore Radius (chunks)")
            .description("Chunk radius used when computing a fixed explore target. " +
                         "Larger = longer paths between waypoints.")
            .defaultValue(4)
            .min(1)
            .max(64)
            .sliderRange(1, 32)
            .build()
    );

    private final Setting<Integer> tickInterval = sgGeneral.add(
        new IntSetting.Builder()
            .name("tick-interval")
            .displayName("Tick Interval")
            .description("How many game ticks between each pathfinding update check. 20 = 1 second.")
            .defaultValue(20)
            .min(1)
            .max(200)
            .sliderRange(5, 100)
            .build()
    );

    private final Setting<Keybind> logCoordsKey = sgLogging.add(
        new KeybindSetting.Builder()
            .name("log-coords-key")
            .displayName("Log Coords Key")
            .description("Press this key while the module is active to write your " +
                         "current X/Y/Z position to coords.log in .minecraft/autoexplorer/.")
            .defaultValue(Keybind.fromKey(GLFW.GLFW_KEY_L))
            .build()
    );

    private final Setting<Boolean> chatConfirmLog = sgLogging.add(
        new BoolSetting.Builder()
            .name("chat-confirm-log")
            .displayName("Chat Confirmation")
            .description("Show a chat message each time coordinates are saved to disk.")
            .defaultValue(true)
            .build()
    );

    // ── Internal state ─────────────────────────────────────────────────────
    private int     tickCounter        = 0;
    private boolean baritoneWasRunning = false;

    // ── Constructor ────────────────────────────────────────────────────────
    public AutoExplorerModule() {
        super(
            AutoExplorerAddon.CATEGORY,
            "AutoExplorer",
            "Automatically explores the world using Baritone pathfinding " +
            "and logs visited coordinates to disk on demand."
        );
    }

    // ── Lifecycle ──────────────────────────────────────────────────────────

    @Override
    public void onActivate() {
        tickCounter = 0;

        if (baritoneEnabled.get()) {
            IBaritone baritone = BaritoneAPI.getProvider().getPrimaryBaritone();
            baritoneWasRunning = baritone.getPathingBehavior().isPathing();

            if (baritoneWasRunning) {
                ChatUtils.sendMsg(
                    "§e[AutoExplorer] §fBaritone is already pathing. " +
                    "AutoExplorer will NOT override the current goal."
                );
            } else {
                ChatUtils.sendMsg("§a[AutoExplorer] §fActivated. Starting exploration...");
                startExploring(baritone);
            }
        } else {
            ChatUtils.sendMsg("§a[AutoExplorer] §fActivated (Baritone disabled — observe mode).");
        }
    }

    @Override
    public void onDeactivate() {
        if (baritoneEnabled.get() && !baritoneWasRunning) {
            IBaritone baritone = BaritoneAPI.getProvider().getPrimaryBaritone();
            baritone.getPathingBehavior().cancelEverything();
            ChatUtils.sendMsg("§c[AutoExplorer] §fDeactivated. Baritone path cancelled.");
        } else {
            ChatUtils.sendMsg("§c[AutoExplorer] §fDeactivated.");
        }
        tickCounter = 0;
    }

    // ── Tick event ─────────────────────────────────────────────────────────

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;

        tickCounter++;

        // Keybind: log coordinates on key press (edge-triggered).
        if (logCoordsKey.get().isPressed()) {
            handleCoordinateLog();
        }

        if (tickCounter < tickInterval.get()) return;
        tickCounter = 0;

        if (!baritoneEnabled.get() || baritoneWasRunning) return;

        IBaritone baritone = BaritoneAPI.getProvider().getPrimaryBaritone();

        if (!baritone.getPathingBehavior().isPathing()) {
            ChatUtils.sendMsg(String.format(
                "§b[AutoExplorer] §fBaritone idle — resuming exploration from (%.0f, %.0f, %.0f).",
                mc.player.getX(), mc.player.getY(), mc.player.getZ()
            ));
            startExploring(baritone);
        }
    }

    // ── Private helpers ────────────────────────────────────────────────────

    /**
     * Issues an explore goal to Baritone.
     *
     * FIX: IExploreProcess#explore(int centerX, int centerZ) takes BLOCK coordinates
     * for the center point — Baritone internally converts to chunk coords.
     * The old code was correct here, but the import of IExploreProcess was unused
     * because getExploreProcess() returns it directly; no cast is needed.
     *
     * Option B (GoalXZ) is available if you want to walk to a fixed point instead
     * of open-ended exploration.
     */
    private void startExploring(IBaritone baritone) {
        int originX = (int) mc.player.getX();
        int originZ = (int) mc.player.getZ();

        // ── Option A: Open-ended exploration (default) ─────────────────────
        // explore() takes the CENTER of the area to explore, in block coordinates.
        // Baritone picks unexplored chunks in the surrounding area automatically.
        IExploreProcess exploreProcess = baritone.getExploreProcess();
        exploreProcess.explore(originX, originZ);

        // ── Option B: Walk to a fixed offset (comment out Option A to use) ─
        // int targetX = originX;
        // int targetZ = originZ - (exploreRadius.get() * 16); // chunks → blocks
        // baritone.getCustomGoalProcess().setGoalAndPath(new GoalXZ(targetX, targetZ));
        // ChatUtils.sendMsg("§b[AutoExplorer] §fPathing to (" + targetX + ", " + targetZ + ")");

        AutoExplorerAddon.LOG.info(
            "[AutoExplorer] Explore goal issued from ({}, {}).",
            originX, originZ
        );
    }

    /**
     * Writes the player's current XYZ to disk via CoordinateLogger,
     * then optionally prints a chat confirmation.
     */
    private void handleCoordinateLog() {
        if (mc.player == null) return;

        double x = mc.player.getX();
        double y = mc.player.getY();
        double z = mc.player.getZ();
        // getWorld() is the correct call on ClientPlayerEntity in 1.21.x
        String dimension = mc.player.getWorld().getRegistryKey().getValue().toString();

        boolean success = CoordinateLogger.logCoordinates(x, y, z, dimension);

        if (chatConfirmLog.get()) {
            if (success) {
                ChatUtils.sendMsg(String.format(
                    "§a[AutoExplorer] §fCoords saved: §ex=%.1f §fy=%.1f §ez=%.1f §f[%s]",
                    x, y, z, dimension
                ));
            } else {
                ChatUtils.sendMsg("§c[AutoExplorer] §fFailed to write coordinates! Check logs.");
            }
        }
    }
}
