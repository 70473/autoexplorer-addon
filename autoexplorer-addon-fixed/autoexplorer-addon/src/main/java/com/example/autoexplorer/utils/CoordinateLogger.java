package com.example.autoexplorer.utils;

import com.example.autoexplorer.AutoExplorerAddon;
import net.fabricmc.loader.api.FabricLoader;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * CoordinateLogger — utility for writing player coordinates to disk.
 *
 * Output location: {@code <.minecraft>/autoexplorer/coords.log}
 * Output format:   {@code [2025-09-14 13:22:05]  x=128.50  y=64.00  z=-300.25  dim=minecraft:overworld}
 *
 * All methods are static — do not instantiate this class.
 */
public final class CoordinateLogger {

    private static final String LOG_DIR_NAME  = "autoexplorer";
    private static final String LOG_FILE_NAME = "coords.log";
    private static final DateTimeFormatter TIMESTAMP_FMT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private CoordinateLogger() {}

    /**
     * Appends one coordinate entry to the log file.
     *
     * @param x         Player X (from {@code player.getX()})
     * @param y         Player Y
     * @param z         Player Z
     * @param dimension Registry string for the dimension (e.g. "minecraft:overworld")
     * @return {@code true} on success, {@code false} if an {@link IOException} occurred.
     */
    public static boolean logCoordinates(double x, double y, double z, String dimension) {
        try {
            Path logFile = resolveLogFile();
            String entry = buildLogEntry(x, y, z, dimension);

            try (BufferedWriter writer = Files.newBufferedWriter(
                    logFile,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.APPEND,
                    StandardOpenOption.CREATE)) {
                writer.write(entry);
                writer.newLine();
            }

            AutoExplorerAddon.LOG.info("[CoordinateLogger] Logged: {}", entry.trim());
            return true;

        } catch (IOException e) {
            AutoExplorerAddon.LOG.error("[CoordinateLogger] Failed to write coordinates.", e);
            return false;
        }
    }

    /**
     * Writes a freeform session marker line — useful for separating sessions in the log.
     * Call from {@code AutoExplorerModule#onActivate()} if you want clear session boundaries.
     */
    public static boolean writeSessionMarker(String label) {
        try {
            Path logFile = resolveLogFile();
            String timestamp = LocalDateTime.now().format(TIMESTAMP_FMT);
            String marker = String.format("──── [%s] %s ────", timestamp, label);

            try (BufferedWriter writer = Files.newBufferedWriter(
                    logFile,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.APPEND,
                    StandardOpenOption.CREATE)) {
                writer.write(marker);
                writer.newLine();
            }
            return true;

        } catch (IOException e) {
            AutoExplorerAddon.LOG.error("[CoordinateLogger] Failed to write session marker.", e);
            return false;
        }
    }

    // ── Private helpers ────────────────────────────────────────────────────

    private static Path resolveLogFile() throws IOException {
        Path gameDir = FabricLoader.getInstance().getGameDir();
        Path logDir  = gameDir.resolve(LOG_DIR_NAME);
        Files.createDirectories(logDir);
        return logDir.resolve(LOG_FILE_NAME);
    }

    private static String buildLogEntry(double x, double y, double z, String dimension) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FMT);
        return String.format("[%s]  x=%.2f  y=%.2f  z=%.2f  dim=%s", timestamp, x, y, z, dimension);
    }
}
