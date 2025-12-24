package dev.studer.alex.anatidaephobia.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * A simple payload to trigger the Ducky Win Screen on the client.
 * Sent from server when a player should see the win screen.
 */
public record DuckyWinPayload() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<DuckyWinPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.parse("anatidaephobia:ducky_win"));

    public static final StreamCodec<FriendlyByteBuf, DuckyWinPayload> STREAM_CODEC =
            StreamCodec.unit(new DuckyWinPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
