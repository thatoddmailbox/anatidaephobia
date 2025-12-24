package dev.studer.alex.anatidaephobia.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

/**
 * Handles network registration for the Anatidaephobia mod.
 */
public class AnatidaephobiaNetwork {

    /**
     * Initialize network payloads (called from main mod initializer).
     * Registers the payload type on the server side.
     */
    public static void init() {
        // Register the payload type for server->client communication
        PayloadTypeRegistry.playS2C().register(DuckyWinPayload.TYPE, DuckyWinPayload.STREAM_CODEC);
    }

    /**
     * Send the Ducky Win screen trigger to a player.
     * Call this from server-side code when you want the player to see the win screen.
     *
     * @param player The player to show the win screen to
     */
    public static void sendDuckyWin(ServerPlayer player) {
        ServerPlayNetworking.send(player, new DuckyWinPayload());
    }
}
