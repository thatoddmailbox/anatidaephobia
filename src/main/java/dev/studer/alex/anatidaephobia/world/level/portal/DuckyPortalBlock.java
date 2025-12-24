package dev.studer.alex.anatidaephobia.world.level.portal;

import com.mojang.serialization.MapCodec;
import dev.studer.alex.anatidaephobia.AnatidaephobiaBlocks;
import dev.studer.alex.anatidaephobia.AnatidaephobiaItems;
import dev.studer.alex.anatidaephobia.network.AnatidaephobiaNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Portal;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * The Ducky Portal block - creates a portal effect similar to Nether portals
 * but made from quackmium blocks. Currently shows a chat message when entered;
 * dimension teleportation will be added later.
 */
public class DuckyPortalBlock extends Block implements Portal {
    public static final MapCodec<DuckyPortalBlock> CODEC = simpleCodec(DuckyPortalBlock::new);
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
    private static final Map<Direction.Axis, VoxelShape> SHAPES = Shapes.rotateHorizontalAxis(
            Block.column(4.0, 16.0, 0.0, 16.0)
    );

    public DuckyPortalBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(AXIS, Direction.Axis.X));
    }

    @Override
    public MapCodec<DuckyPortalBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(AXIS));
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks,
                                     BlockPos pos, Direction directionToNeighbour, BlockPos neighbourPos,
                                     BlockState neighbourState, RandomSource random) {
        Direction.Axis updateAxis = directionToNeighbour.getAxis();
        Direction.Axis axis = state.getValue(AXIS);
        boolean wrongAxis = axis != updateAxis && updateAxis.isHorizontal();

        // Check if portal structure is still valid
        if (!wrongAxis && !neighbourState.is(this) &&
                !DuckyPortalShape.findAnyShape(level, pos, axis).isComplete()) {
            return Blocks.AIR.defaultBlockState();
        }

        return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
                                InsideBlockEffectApplier effectApplier, boolean isPrecise) {
        if (entity.canUsePortal(false)) {
            // Trigger the Ducky Win Screen for players
            if (entity instanceof ServerPlayer player) {
                // Only trigger once when first entering (cooldown prevents spam)
                if (!player.isOnPortalCooldown()) {
                    // Check if the player is wearing full Quackmium armor
                    if (isWearingFullQuackmiumArmor(player)) {
                        // Send the win screen packet to the player
                        player.unRide();
                        player.level().removePlayerImmediately(player, Entity.RemovalReason.CHANGED_DIMENSION);
                        // reuse the flag used for the End. this is a good idea that we will definitely not regret later
                        player.wonGame = true;
                        AnatidaephobiaNetwork.sendDuckyWin(player);
                        player.setPortalCooldown();
                    } else {
                        // Admonish the player for not wearing full Quackmium armor
                        player.displayClientMessage(
                                Component.translatable("message.anatidaephobia.portal_no_armor")
                                        .withStyle(ChatFormatting.GOLD),
                                false
                        );
                        // Longer cooldown to prevent message spam while standing in portal
                        player.setPortalCooldown(100);
                    }
                }
            }
            // When dimension is implemented, use: entity.setAsInsidePortal(this, pos);
        }
    }

    private boolean isWearingFullQuackmiumArmor(Player player) {
        return player.getItemBySlot(EquipmentSlot.HEAD).is(AnatidaephobiaItems.QUACKMIUM_HELMET)
                && player.getItemBySlot(EquipmentSlot.CHEST).is(AnatidaephobiaItems.QUACKMIUM_CHESTPLATE)
                && player.getItemBySlot(EquipmentSlot.LEGS).is(AnatidaephobiaItems.QUACKMIUM_LEGGINGS)
                && player.getItemBySlot(EquipmentSlot.FEET).is(AnatidaephobiaItems.QUACKMIUM_BOOTS);
    }

    @Override
    public int getPortalTransitionTime(ServerLevel level, Entity entity) {
        // Players have a delay, non-players teleport instantly
        if (entity instanceof Player player) {
            return player.getAbilities().invulnerable ? 1 : 80; // 4 seconds for survival, instant for creative
        }
        return 0;
    }

    @Override
    public @Nullable TeleportTransition getPortalDestination(ServerLevel currentLevel, Entity entity, BlockPos portalEntryPos) {
        // TODO: Implement dimension teleportation
        // For now, return null (no teleportation)
        return null;
    }

    @Override
    public Transition getLocalTransition() {
        return Transition.CONFUSION; // Gives the swirly portal effect
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        // Play quacking sound occasionally
        if (random.nextInt(100) == 0) {
            level.playLocalSound(
                    pos.getX() + 0.5,
                    pos.getY() + 0.5,
                    pos.getZ() + 0.5,
                    SoundEvents.CHICKEN_AMBIENT, // TODO: Replace with custom duck sound
                    SoundSource.BLOCKS,
                    0.5F,
                    random.nextFloat() * 0.4F + 0.8F,
                    false
            );
        }

        // Spawn feather-like particles
        for (int i = 0; i < 4; ++i) {
            double x = pos.getX() + random.nextDouble();
            double y = pos.getY() + random.nextDouble();
            double z = pos.getZ() + random.nextDouble();
            double xa = (random.nextFloat() - 0.5) * 0.5;
            double ya = (random.nextFloat() - 0.5) * 0.5;
            double za = (random.nextFloat() - 0.5) * 0.5;
            int flip = random.nextInt(2) * 2 - 1;

            if (!level.getBlockState(pos.west()).is(this) && !level.getBlockState(pos.east()).is(this)) {
                x = pos.getX() + 0.5 + 0.25 * flip;
                xa = random.nextFloat() * 2.0F * flip;
            } else {
                z = pos.getZ() + 0.5 + 0.25 * flip;
                za = random.nextFloat() * 2.0F * flip;
            }

            // Use end rod particles for a feathery/magical effect
            level.addParticle(ParticleTypes.END_ROD, x, y, z, xa, ya, za);
        }
    }

    @Override
    protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        return ItemStack.EMPTY; // Can't pick up portal blocks
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        switch (rotation) {
            case COUNTERCLOCKWISE_90:
            case CLOCKWISE_90:
                return switch (state.getValue(AXIS)) {
                    case X -> state.setValue(AXIS, Direction.Axis.Z);
                    case Z -> state.setValue(AXIS, Direction.Axis.X);
                    default -> state;
                };
            default:
                return state;
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS);
    }
}
