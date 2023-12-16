package guitaek.bundlepots.mixin;

import com.mojang.serialization.MapCodec;
import guitaek.bundlepots.BundleInventory;
import guitaek.bundlepots.BundlePotCalculator;
import guitaek.bundlepots.BundlePots;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.DecoratedPotBlockEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.Containers;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(DecoratedPotBlock.class)
public class BundlePotBlock extends BaseEntityBlock implements SimpleWaterloggedBlock {
    private BundlePotBlock() {
        super(null);
        throw new AssertionError("this constructor shall not be called");
    }
    private void showFail(Level level, BlockPos pos, DecoratedPotBlockEntity decoratedPotBlockEntity) {
        level.playSound((Player)null, pos, SoundEvents.DECORATED_POT_INSERT_FAIL, SoundSource.BLOCKS, 1.0F, 1.0F);
        decoratedPotBlockEntity.wobble(DecoratedPotBlockEntity.WobbleStyle.NEGATIVE);
    }
    private void showSuccess(Level level, BlockPos pos, float pitch) {
        level.playSound((Player)null, pos, SoundEvents.DECORATED_POT_INSERT, SoundSource.BLOCKS, 1.0F, 0.7F + 0.5F * pitch);
        if (level instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel)level;
            serverLevel.sendParticles(ParticleTypes.DUST_PLUME, (double)pos.getX() + 0.5, (double)pos.getY() + 1.2, (double)pos.getZ() + 0.5, 7, 0.0, 0.0, 0.0, 0.0);
        }
    }
    public InteractionResult onUse(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        BlockEntity entity = level.getBlockEntity(pos);
        if (!(entity instanceof DecoratedPotBlockEntity decoratedPotBlockEntity)) {
            return InteractionResult.PASS;
        } else {
            if (level.isClientSide) {
                return InteractionResult.CONSUME;
            }
            BundleInventory bundleInventory = (BundleInventory) (Object) decoratedPotBlockEntity;
            ItemStack itemStack = player.getItemInHand(hand);
            if (!itemStack.isEmpty()) {
                decoratedPotBlockEntity.wobble(DecoratedPotBlockEntity.WobbleStyle.POSITIVE);
                player.awardStat(Stats.ITEM_USED.get(itemStack.getItem()));
                ItemStack toGive = player.isCreative() ? itemStack.copyWithCount(1) : itemStack.split(1);
                if (bundleInventory.isAddable(toGive)) {
                    bundleInventory.addItem(toGive);
                    CompoundTag nbt = new CompoundTag();
                    bundleInventory.writeToNbt(nbt);
                    float pitch = (float) BundlePotCalculator.getTotalContentSize(nbt) / 64;
                    this.showSuccess(level, pos, pitch);
                    level.updateNeighbourForOutputSignal(pos, this);
                } else {
                    if (!player.isCreative()) {
                        itemStack.grow(toGive.getCount());
                    }
                    this.showFail(level, pos, decoratedPotBlockEntity);
                }
            } else {
                this.showFail(level, pos, decoratedPotBlockEntity);
            }

            level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
            return InteractionResult.SUCCESS;
        }
    }
    @Redirect(method = "onRemove", at = @At(value="INVOKE",
            target = "Lnet/minecraft/world/Containers;dropContentsOnDestroy(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)V"))
    private void scatterOnShatter(BlockState state, BlockState newState, Level level, BlockPos pos) {
        if (state.getValue(DecoratedPotBlock.CRACKED)) {
            Containers.dropContentsOnDestroy(state, newState, level, pos);
        }
    }
    @Inject(at = @At("HEAD"), method = "getDrops", cancellable = true)
    private void onGetDrops(BlockState state, LootParams.Builder builder, CallbackInfoReturnable<List<ItemStack>> ci) {
        if (state.getBlock() == Blocks.DECORATED_POT) {
            if (!state.getValue(DecoratedPotBlock.CRACKED)) {
                List<ItemStack> customDrops = new ArrayList<>();
                BlockEntity blockEntity = builder.getParameter(LootContextParams.BLOCK_ENTITY);
                BundleInventory bundleInventory = (BundleInventory) (Object)blockEntity;
                DecoratedPotBlockEntity decoratedPotBlockEntity = (DecoratedPotBlockEntity)blockEntity;
                ItemStack itemStack = DecoratedPotBlockEntity.createDecoratedPotItem(decoratedPotBlockEntity.getDecorations());;
                CompoundTag nbt = new CompoundTag();
                bundleInventory.writeToNbt(nbt);
                BlockItem.setBlockEntityData(itemStack, BlockEntityType.DECORATED_POT, nbt);
                customDrops.add(itemStack);
                ci.setReturnValue(customDrops);
                ci.cancel();
            }
        }
    }
    @Inject(at = @At("TAIL"), method = "setPlacedBy")
    public void onPlacedTail(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack, CallbackInfo info) {
        if (!level.isClientSide) {
            level.getBlockEntity(pos, BlockEntityType.DECORATED_POT).ifPresent((blockEntity) -> {
                blockEntity.setFromItem(itemStack);
            });
        }
    }
    @Inject(at = @At("RETURN"), method = "getAnalogOutputSignal", cancellable = true)
    public void getAnalogOutputSignalReturn(BlockState state, Level level, BlockPos pos, CallbackInfoReturnable<Integer> ci) {
        int contentSize = ((BundleInventory)level.getBlockEntity(pos)).contentSize();
        // scraped from net.minecraft.screen.ScreenHandler.calculateComparatorOutput(@Nullable Inventory inventory)
        float f = (float)contentSize / (float)64;
        int result = Mth.lerpDiscrete(f, 0, 15);
        ci.setReturnValue(result);
    }
    @Shadow
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return null;
    }

    @Shadow
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return null;
    }
}
