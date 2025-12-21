package dev.studer.alex.anatidaephobia.menu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record DuckMenuData(int entityId) {
	public static final StreamCodec<RegistryFriendlyByteBuf, DuckMenuData> STREAM_CODEC =
			StreamCodec.composite(
					ByteBufCodecs.VAR_INT, DuckMenuData::entityId,
					DuckMenuData::new
			);
}
