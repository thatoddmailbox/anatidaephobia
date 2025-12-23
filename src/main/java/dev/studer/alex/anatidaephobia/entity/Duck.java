package dev.studer.alex.anatidaephobia.entity;

import dev.studer.alex.anatidaephobia.Anatidaephobia;
import dev.studer.alex.anatidaephobia.AnatidaephobiaItems;
import dev.studer.alex.anatidaephobia.AnatidaephobiaMobEffects;
import dev.studer.alex.anatidaephobia.DuckNestManager;
import dev.studer.alex.anatidaephobia.entity.ai.goal.HungryGoal;
import dev.studer.alex.anatidaephobia.entity.ai.goal.NestGoal;
import dev.studer.alex.anatidaephobia.menu.DuckMenu;
import net.minecraft.core.particles.ParticleTypes;
import dev.studer.alex.anatidaephobia.menu.DuckMenuData;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class Duck extends PathfinderMob {
	private byte EVENT_ID_LOVE = 100;

	// Duck state enum - synced to client via DATA_DUCK_STATE
	public enum DuckState {
		DEFAULT,
		SCARED,
		NESTING,
		HUNGRY;

		public static DuckState fromOrdinal(int ordinal) {
			DuckState[] values = values();
			return (ordinal >= 0 && ordinal < values.length) ? values[ordinal] : DEFAULT;
		}
	}

	// This sucks!! I would really like to wrap everything in a nice DuckData reoord
	// Unfortunately, I can't register a custom EntityDataSerializer for my data type since Fabric doesn't seem to support it
	// (at least I think it doesn't)
	// So instead we have this sadness

	private static final EntityDataAccessor<String> DATA_DUCK_NAME = SynchedEntityData.defineId(Duck.class, EntityDataSerializers.STRING);
	private static final EntityDataAccessor<Integer> DATA_DUCK_XP = SynchedEntityData.defineId(Duck.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DATA_DUCK_HUNGER = SynchedEntityData.defineId(Duck.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DATA_DUCK_STRESS = SynchedEntityData.defineId(Duck.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DATA_DUCK_LONELINESS = SynchedEntityData.defineId(Duck.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DATA_DUCK_STATE = SynchedEntityData.defineId(Duck.class, EntityDataSerializers.INT);

	public Duck(EntityType<? extends PathfinderMob> entityType, Level level) {
		super(entityType, level);

		this.setDuckName(generateDuckName());
		this.setDuckXP(0);

		// Make the nametag always visible
		this.setCustomNameVisible(true);
	}

	private String generateDuckName() {
		// Generate a name based on UUID to keep it consistent
		String[] names = {"Quackers", "Waddles", "Donald", "Daffy", "Howard", "Mallard", "Puddles"};
		int index = Math.abs(this.getUUID().hashCode()) % names.length;
		return names[index];
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);  // Always call super first!
		output.putString("DuckName", getDuckName());
		output.putInt("DuckXP", getDuckXP());
		output.putInt("DuckHunger", getDuckHunger());
		output.putInt("DuckStress", getDuckStress());
		output.putInt("DuckLoneliness", getDuckLoneliness());
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);  // Always call super first!
		setDuckName(input.getStringOr("DuckName", "Confused Duck"));
		setDuckXP(input.getIntOr("DuckXP", 0));
		setDuckHunger(input.getIntOr("DuckHunger", 0));
		setDuckStress(input.getIntOr("DuckStress", 0));
		setDuckLoneliness(input.getIntOr("DuckLoneliness", 0));
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_DUCK_NAME, "");  // default empty string
		builder.define(DATA_DUCK_XP, 0);
		builder.define(DATA_DUCK_HUNGER, 0);
		builder.define(DATA_DUCK_STRESS, 0);
		builder.define(DATA_DUCK_LONELINESS, 0);
		builder.define(DATA_DUCK_STATE, DuckState.DEFAULT.ordinal());
	}

	@Override
	public Component getName() {
		// Generate a dynamic nametag showing duck name + state
		String state = getDuckState();
		return Component.literal(getDuckName() + " [Level " + getDuckLevel() + "] [" + state + "]");
	}

	public String getDuckName() {
		return this.entityData.get(DATA_DUCK_NAME);
	}

	public void setDuckName(String name) {
		this.entityData.set(DATA_DUCK_NAME, name);
	}

	public int getDuckXP() {
		return this.entityData.get(DATA_DUCK_XP);
	}

	public void setDuckXP(int xp) {
		this.entityData.set(DATA_DUCK_XP, xp);
	}

	public void addDuckXP(int delta) {
		setDuckXP(getDuckXP() + delta);
		// TODO: level up animation or effect?
	}

	// XP thresholds for each level (total XP required to reach that level)
	private static final int[] LEVEL_XP_THRESHOLDS = {0, 10, 30, 70, 170};

	public static int getMaxLevel() {
		return LEVEL_XP_THRESHOLDS.length;
	}

	public int getDuckLevel() {
		int xp = getDuckXP();
		for (int level = LEVEL_XP_THRESHOLDS.length; level >= 1; level--) {
			if (xp >= LEVEL_XP_THRESHOLDS[level - 1]) {
				return level;
			}
		}
		return 1;
	}

	// Returns XP progress within the current level (0 to getLevelMaxXP()-1)
	public int getLevelCurrentXP() {
		int level = getDuckLevel();
		return getDuckXP() - LEVEL_XP_THRESHOLDS[level - 1];
	}

	// Returns XP needed to advance from current level to next (0 if max level)
	public int getLevelMaxXP() {
		int level = getDuckLevel();
		if (level >= LEVEL_XP_THRESHOLDS.length) {
			return 0; // Max level
		}
		return LEVEL_XP_THRESHOLDS[level] - LEVEL_XP_THRESHOLDS[level - 1];
	}

	public int getDuckHunger() {
		return this.entityData.get(DATA_DUCK_HUNGER);
	}

	public void setDuckHunger(int hunger) {
		this.entityData.set(DATA_DUCK_HUNGER, hunger);
	}

	public void addDuckHunger(int delta) {
		int newHunger = Math.min(getDuckHunger() + delta, 10);
		setDuckHunger(newHunger);
	}

	public void reduceDuckHunger(int delta) {
		int newHunger = Math.max(getDuckHunger() - delta, 0);
		setDuckHunger(newHunger);
	}

	public int getDuckStress() {
		return this.entityData.get(DATA_DUCK_STRESS);
	}

	public void setDuckStress(int stress) {
		this.entityData.set(DATA_DUCK_STRESS, stress);
	}

	public int getDuckLoneliness() {
		return this.entityData.get(DATA_DUCK_LONELINESS);
	}

	public void setDuckLoneliness(int loneliness) {
		this.entityData.set(DATA_DUCK_LONELINESS, loneliness);
	}

	public DuckState getDuckStateEnum() {
		return DuckState.fromOrdinal(this.entityData.get(DATA_DUCK_STATE));
	}

	private String getDuckState() {
		if (this.isDeadOrDying()) {
			return "Dead";
		}

		return switch (getDuckStateEnum()) {
			case SCARED -> "Scared";
			case NESTING -> "Nesting";
			case HUNGRY -> "Hungry";
			case DEFAULT -> "Cool";
		};
	}

	// Called on server to update the synced state based on active goals
	private void updateDuckState() {
		if (this.panicGoal != null && this.panicGoal.isRunning()) {
			this.entityData.set(DATA_DUCK_STATE, DuckState.SCARED.ordinal());
		} else if (this.hungryGoal != null && this.hungryGoal.isRunning()) {
			this.entityData.set(DATA_DUCK_STATE, DuckState.HUNGRY.ordinal());
		} else if (this.nestGoal != null && this.nestGoal.isRunning()) {
			this.entityData.set(DATA_DUCK_STATE, DuckState.NESTING.ordinal());
		} else {
			this.entityData.set(DATA_DUCK_STATE, DuckState.DEFAULT.ordinal());
		}
	}

	private int getValueOfFood(ItemStack itemStack) {
		if (itemStack.is(Items.WHEAT_SEEDS) || itemStack.is(Items.MELON_SEEDS) ||
				itemStack.is(Items.PUMPKIN_SEEDS) || itemStack.is(Items.BEETROOT_SEEDS) ||
				itemStack.is(Items.TORCHFLOWER_SEEDS) || itemStack.is(Items.PITCHER_POD)) {
			return 1; // seeds
		} else if (itemStack.is(Items.SEAGRASS)) {
			return 1;
		} else if (itemStack.is(Items.KELP) || itemStack.is(Items.DRIED_KELP)) {
			return 2;
		} else if (itemStack.is(Items.BEETROOT)) {
			return 2; // TODO: but they don't like it so much
		} else if (itemStack.is(Items.SWEET_BERRIES)) {
			return 3;
		} else if (itemStack.is(Items.MELON_SLICE)) {
			return 4;
		} else if (itemStack.is(Items.GLISTERING_MELON_SLICE)) {
			return 6;
		}

		return 1; // default for any other duck food
	}

	public ItemStack getRandomNestDrop() {
		int level = getDuckLevel();
		float roll = this.random.nextFloat();

		// TODO: these are placeholders, need to make some more items
		return switch (level) {
			case 1 -> new ItemStack(AnatidaephobiaItems.DUCK_EGG);
			case 2 -> roll < 0.8f ? new ItemStack(AnatidaephobiaItems.DUCK_EGG)
					: new ItemStack(Items.FEATHER);
			case 3 -> roll < 0.6f ? new ItemStack(AnatidaephobiaItems.DUCK_EGG)
					: new ItemStack(Items.BOOK);
			case 4 -> roll < 0.6f ? new ItemStack(AnatidaephobiaItems.DUCK_EGG)
					: new ItemStack(Items.BOOK);
			case 5 -> roll < 0.6f ? new ItemStack(AnatidaephobiaItems.DUCK_EGG)
					: new ItemStack(Items.DIAMOND);
			default -> new ItemStack(AnatidaephobiaItems.DUCK_EGG);
		};
	}

	private FloatGoal floatGoal;
	private PanicGoal panicGoal;
	private TemptGoal temptGoal;
	private HungryGoal hungryGoal;
	private NestGoal nestGoal;
	private WaterAvoidingRandomStrollGoal waterAvoidingRandomStrollGoal;

	@Override
	protected void registerGoals() {
		floatGoal = new FloatGoal(this);
		this.goalSelector.addGoal(0, floatGoal);

		panicGoal = new PanicGoal(this, 1.4);
		this.goalSelector.addGoal(1, panicGoal);

		temptGoal = new TemptGoal(this, 1.0, itemStack -> itemStack.is(AnatidaephobiaItems.DUCK_FOOD), false);
		this.goalSelector.addGoal(2, temptGoal);

		hungryGoal = new HungryGoal(this);
		this.goalSelector.addGoal(2, hungryGoal);

		nestGoal = new NestGoal(this, 1.0, 16);
		this.goalSelector.addGoal(3, nestGoal);

		waterAvoidingRandomStrollGoal = new WaterAvoidingRandomStrollGoal(this, 1.0);
		this.goalSelector.addGoal(4, waterAvoidingRandomStrollGoal);
	}

	private float hungerAccum = 0;

	@Override
	public void aiStep() {
		super.aiStep();

		if (!this.level().isClientSide()) {
			// Update synced state on server side
			updateDuckState();

			if (getDeltaMovement().length() > 0.01 && getDuckStateEnum() != DuckState.HUNGRY) {
				hungerAccum += 0.02f * getDeltaMovement().length();
			}

			if (getDuckStateEnum() == DuckState.NESTING) {
				hungerAccum += 0.10f;
			}

			// Calculate stat changes
			if (tickCount % 20 == 0) {
				if (hungerAccum > 4.0f) {
					hungerAccum -= 4.0f;
					addDuckHunger(1);
				}
			}
		}
	}

	@Override
	public boolean fireImmune() {
		return true;
	}

	@Override
	public boolean canBeLeashed() {
		// TODO: it would be funny if this worked?
		return false;
	}

	@Override
	public boolean removeWhenFarAway(double distSqr) {
		// Prevent ducks from despawning like passive animals
		return false;
	}

	@Override
	public void handleEntityEvent(final byte id) {
		if (id == EVENT_ID_LOVE) {
			for (int i = 0; i < 7; ++i) {
				double xa = this.random.nextGaussian() * 0.02;
				double ya = this.random.nextGaussian() * 0.02;
				double za = this.random.nextGaussian() * 0.02;
				this.level().addParticle(ParticleTypes.HEART, this.getRandomX((double)1.0F), this.getRandomY() + (double)0.5F, this.getRandomZ((double)1.0F), xa, ya, za);
			}
		} else {
			super.handleEntityEvent(id);
		}
	}

	@Override
	public void remove(RemovalReason reason) {
		DuckNestManager.unclaimNest(this);
		super.remove(reason);
	}

	@Override
	public InteractionResult mobInteract(final Player player, final InteractionHand hand) {
		ItemStack itemStack = player.getItemInHand(hand);
		if (itemStack.is(AnatidaephobiaItems.DUCK_FOOD)) {
			boolean isBread = false;
			if (itemStack.is(Items.BREAD)) {
				// RIP duck - bread is harmful to ducks!
				isBread = true;

				// Apply bread sickness effect for 20 seconds at amplifier 0
				this.addEffect(new MobEffectInstance(
						AnatidaephobiaMobEffects.BREAD_SICKNESS,
						20 * Anatidaephobia.TICKS_PER_SECOND,
						0
				));
			}

			if (player instanceof ServerPlayer) {
				ServerPlayer serverPlayer = (ServerPlayer) player;

				if (isBread) {
					serverPlayer.sendSystemMessage(Component.translatable("message.anatidaephobia.duck_bread"));
				} else {
					serverPlayer.sendSystemMessage(Component.translatable("message.anatidaephobia.duck_hurt"));
				}

				this.usePlayerItem(player, hand, itemStack);
				this.level().broadcastEntityEvent(this, EVENT_ID_LOVE);

				if (!isBread) {
					reduceDuckHunger(getValueOfFood(itemStack));
				}

				return InteractionResult.SUCCESS_SERVER;
			}

			if (this.level().isClientSide()) {
				return InteractionResult.CONSUME;
			}
		}

		if (!this.level().isClientSide()) {
			Duck duck = this;
			player.openMenu(new ExtendedScreenHandlerFactory<DuckMenuData>() {
				@Override
				public DuckMenuData getScreenOpeningData(ServerPlayer player) {
					return new DuckMenuData(duck.getId());
				}

				@Override
				public Component getDisplayName() {
					return Component.translatable("gui.anatidaephobia.duck");
				}

				@Override
				public DuckMenu createMenu(int containerId, net.minecraft.world.entity.player.Inventory inventory, net.minecraft.world.entity.player.Player player) {
					return new DuckMenu(containerId, inventory, duck);
				}
			});
		}

		return InteractionResult.SUCCESS;
	}

	@Override
	public boolean hurtServer(ServerLevel serverLevel, DamageSource damageSource, float f) {
		boolean result = super.hurtServer(serverLevel, damageSource, f);

		// If we're dying then we already would have triggered the behavior in die() so skip this code
		if (result && !isDeadOrDying()) {
			if (damageSource.getEntity() != null) {
				if (damageSource.getEntity() instanceof LivingEntity) {
					LivingEntity livingEntity = (LivingEntity) damageSource.getEntity();

					// Strike the attacker down with lightning.
					// We make it visual-only and call thunderHit ourselves manually.
					// This is so we don't get collateral damage (such as, importantly, the duck!)
					LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(serverLevel, EntitySpawnReason.TRIGGERED);
					bolt.snapTo(livingEntity.getX(), livingEntity.getY(), livingEntity.getZ());
					bolt.setVisualOnly(true);
					serverLevel.addFreshEntity(bolt);
					livingEntity.thunderHit(serverLevel, bolt);

					if (damageSource.getEntity() instanceof ServerPlayer) {
						// If it was a player, send them a threat.
						ServerPlayer player = (ServerPlayer) damageSource.getEntity();
						player.sendSystemMessage(Component.translatable("message.anatidaephobia.duck_hurt"));
					}
				}
			}
		}

		return result;
	}

	@Override
	public void die(DamageSource damageSource) {
		if (damageSource.getEntity() instanceof ServerPlayer) {
			ServerPlayer player = (ServerPlayer) damageSource.getEntity();

			// TODO: lightning storm?
			player.level().setWeatherParameters(0, 10*Anatidaephobia.TICKS_PER_SECOND, true, true);

			player.sendSystemMessage(Component.translatable("message.anatidaephobia.duck_death"));
		} else if (damageSource.getEntity() instanceof LivingEntity) {
			LivingEntity livingEntity = (LivingEntity) damageSource.getEntity();
			// TODO: should we do something here?
		}

		super.die(damageSource);
	}

	public static AttributeSupplier.Builder createMobAttributes() {
		return Animal.createAnimalAttributes()
				.add(Attributes.MOVEMENT_SPEED, 0.25F)
				.add(Attributes.MAX_HEALTH, 10.0)
				.add(Attributes.ATTACK_DAMAGE, 2.0)
				.add(Attributes.SAFE_FALL_DISTANCE, 5.0)
				.add(Attributes.FOLLOW_RANGE, 32.0);
	}

}
