package guitaek.bundlepots.mixin;

import com.mojang.serialization.MapCodec;
import guitaek.bundlepots.BundleInventory;
import guitaek.bundlepots.BundlePotCalculator;
import guitaek.bundlepots.BundlePots;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.DecoratedPotBlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
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
import java.util.logging.Logger;

@Mixin(DecoratedPotBlock.class)
public class BundlePotBlock extends BlockWithEntity implements Waterloggable {
    private BundlePotBlock() {
        super(null);
        throw new AssertionError("this constructor shall not be called");
    }
    private void showFail(World world, BlockPos pos, DecoratedPotBlockEntity decoratedPotBlockEntity) {
        world.playSound((PlayerEntity)null, pos, SoundEvents.BLOCK_DECORATED_POT_INSERT_FAIL, SoundCategory.BLOCKS, 1.0F, 1.0F);
        decoratedPotBlockEntity.wobble(DecoratedPotBlockEntity.WobbleType.NEGATIVE);
    }
    private void showSuccess(World world, BlockPos pos, float pitch) {
        world.playSound((PlayerEntity)null, pos, SoundEvents.BLOCK_DECORATED_POT_INSERT, SoundCategory.BLOCKS, 1.0F, 0.7F + 0.5F * pitch);
        if (world instanceof ServerWorld) {
            ServerWorld serverWorld = (ServerWorld)world;
            serverWorld.spawnParticles(ParticleTypes.DUST_PLUME, (double)pos.getX() + 0.5, (double)pos.getY() + 1.2, (double)pos.getZ() + 0.5, 7, 0.0, 0.0, 0.0, 0.0);
        }
    }
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        BlockEntity entity = world.getBlockEntity(pos);
        if (!(entity instanceof DecoratedPotBlockEntity decoratedPotBlockEntity)) {
            return ActionResult.PASS;
        } else {
            if (world.isClient) {
                return ActionResult.CONSUME;
            }
            BundleInventory bundleInventory = (BundleInventory) (Object) decoratedPotBlockEntity;
            ItemStack itemStack = player.getStackInHand(hand);
            if (!itemStack.isEmpty()) {
                decoratedPotBlockEntity.wobble(DecoratedPotBlockEntity.WobbleType.POSITIVE);
                player.incrementStat(Stats.USED.getOrCreateStat(itemStack.getItem()));
                ItemStack toGive = player.isCreative() ? itemStack.copyWithCount(1) : itemStack.split(1);
                if (bundleInventory.isAddable(toGive)) {
                    bundleInventory.addItem(toGive);
                    NbtCompound nbt = new NbtCompound();
                    bundleInventory.writeToNbt(nbt);
                    float pitch = (float) BundlePotCalculator.getTotalContentSize(nbt) / 64;
                    this.showSuccess(world, pos, pitch);
                    world.updateComparators(pos, this);
                } else {
                    if (!player.isCreative()) {
                        itemStack.increment(toGive.getCount());
                    }
                    this.showFail(world, pos, decoratedPotBlockEntity);
                }
            } else {
                this.showFail(world, pos, decoratedPotBlockEntity);
            }

            world.emitGameEvent(player, GameEvent.BLOCK_CHANGE, pos);
            return ActionResult.SUCCESS;
        }
    }
    @Redirect(method = "onStateReplaced", at = @At(value="INVOKE",
            target = "Lnet/minecraft/util/ItemScatterer;onStateReplaced(Lnet/minecraft/block/BlockState;Lnet/minecraft/block/BlockState;Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;)V"))
    private void scatterOnShatter(BlockState state, BlockState newState, World world, BlockPos pos) {
        if (state.get(DecoratedPotBlock.CRACKED)) {
            ItemScatterer.onStateReplaced(state, newState, world, pos);
        }
    }
    @Inject(at = @At("HEAD"), method = "getDroppedStacks", cancellable = true)
    private void onGetDrops(BlockState state, LootContextParameterSet.Builder builder, CallbackInfoReturnable<List<ItemStack>> ci) {
        if (state.getBlock() == Blocks.DECORATED_POT) {
            if (!state.get(DecoratedPotBlock.CRACKED)) {
                List<ItemStack> customDrops = new ArrayList<>();
                BlockEntity blockEntity = builder.get(LootContextParameters.BLOCK_ENTITY);
                BundleInventory bundleInventory = (BundleInventory) (Object)blockEntity;
                DecoratedPotBlockEntity decoratedPotBlockEntity = (DecoratedPotBlockEntity)blockEntity;
                ItemStack itemStack = DecoratedPotBlockEntity.getStackWith(decoratedPotBlockEntity.getSherds());;
                NbtCompound nbt = new NbtCompound();
                bundleInventory.writeToNbt(nbt);
                BlockItem.setBlockEntityNbt(itemStack, BlockEntityType.DECORATED_POT, nbt);
                customDrops.add(itemStack);
                ci.setReturnValue(customDrops);
                ci.cancel();
            }
        }
    }
    @Inject(at = @At("TAIL"), method = "onPlaced")
    public void onPlacedTail(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack, CallbackInfo info) {
        if (!world.isClient) {
            world.getBlockEntity(pos, BlockEntityType.DECORATED_POT).ifPresent((blockEntity) -> {
                blockEntity.readNbtFromStack(itemStack);
            });
        }
    }
    @Inject(at = @At("RETURN"), method = "getComparatorOutput", cancellable = true)
    public void getComparatorOutputReturn(BlockState state, World world, BlockPos pos, CallbackInfoReturnable<Integer> ci) {
        int contentSize = ((BundleInventory)world.getBlockEntity(pos)).contentSize();
        // scraped from net.minecraft.screen.ScreenHandler.calculateComparatorOutput(@Nullable Inventory inventory)
        float f = (float)contentSize / (float)64;
        int result = MathHelper.lerpPositive(f, 0, 15);
        ci.setReturnValue(result);
    }
    @Shadow
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return null;
    }

    @Shadow
    @Nullable
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return null;
    }
}
