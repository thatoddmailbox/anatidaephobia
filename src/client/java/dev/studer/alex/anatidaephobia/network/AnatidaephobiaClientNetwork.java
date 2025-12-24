package dev.studer.alex.anatidaephobia.network;

import dev.studer.alex.anatidaephobia.screen.DuckyWinScreen;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/**
 * Handles client-side network registration for the Anatidaephobia mod.
 */
public class AnatidaephobiaClientNetwork {

    /**
     * Initialize client-side network handlers (called from client mod initializer).
     */
    public static void init() {
        // Register the handler for the Ducky Win payload
        ClientPlayNetworking.registerGlobalReceiver(DuckyWinPayload.TYPE, (payload, context) -> {
            // Show the Ducky Win Screen when we receive this packet
            context.client().execute(DuckyWinScreen::show);
        });
    }
}
