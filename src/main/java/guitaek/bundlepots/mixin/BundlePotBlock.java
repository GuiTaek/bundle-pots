package guitaek.bundlepots.mixin;

import com.mojang.serialization.MapCodec;
import guitaek.access.IBundlePotBlockEntity;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.DecoratedPotBlockEntity;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameter;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Position;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
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
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        BlockEntity entity = world.getBlockEntity(pos);
        if (!(entity instanceof DecoratedPotBlockEntity decoratedPotBlockEntity)) {
            return ActionResult.PASS;
        } else {
            IBundlePotBlockEntity bundlePotBlockEntity = (IBundlePotBlockEntity) (Object) decoratedPotBlockEntity;
            ItemStack itemStack = player.getStackInHand(hand);
            Logger.getLogger("BundlePots").warning(itemStack.getItem().getTranslationKey());
            if (!itemStack.isEmpty() && bundlePotBlockEntity.isAddable(itemStack)) {
                decoratedPotBlockEntity.wobble(DecoratedPotBlockEntity.WobbleType.POSITIVE);
                player.incrementStat(Stats.USED.getOrCreateStat(itemStack.getItem()));
                ItemStack toGive = player.isCreative() ? itemStack.copyWithCount(1) : itemStack.split(1);
                bundlePotBlockEntity.addItem(toGive);
                float pitch = (float)bundlePotBlockEntity.getTotalOccupancy() / 64;

                world.playSound((PlayerEntity)null, pos, SoundEvents.BLOCK_DECORATED_POT_INSERT, SoundCategory.BLOCKS, 1.0F, 0.7F + 0.5F * pitch);
                if (world instanceof ServerWorld) {
                    ServerWorld serverWorld = (ServerWorld)world;
                    serverWorld.spawnParticles(ParticleTypes.DUST_PLUME, (double)pos.getX() + 0.5, (double)pos.getY() + 1.2, (double)pos.getZ() + 0.5, 7, 0.0, 0.0, 0.0, 0.0);
                }

                world.updateComparators(pos, this);
            } else {
                world.playSound((PlayerEntity)null, pos, SoundEvents.BLOCK_DECORATED_POT_INSERT_FAIL, SoundCategory.BLOCKS, 1.0F, 1.0F);
                decoratedPotBlockEntity.wobble(DecoratedPotBlockEntity.WobbleType.NEGATIVE);
            }

            world.emitGameEvent(player, GameEvent.BLOCK_CHANGE, pos);
            return ActionResult.SUCCESS;
        }
    }

    @Inject(at = @At("HEAD"), method = "getDroppedStacks", cancellable = true)
    private void onGetDrops(BlockState state, LootContextParameterSet.Builder builder, CallbackInfoReturnable<List<ItemStack>> ci) {
        if (state.getBlock() == Blocks.DECORATED_POT) {
            List<ItemStack> customDrops = new ArrayList<>();
            BlockEntity blockEntity = builder.get(LootContextParameters.BLOCK_ENTITY);;
            IBundlePotBlockEntity bundlePotBlockEntity = (IBundlePotBlockEntity)(Object)blockEntity;
            if (state.get(DecoratedPotBlock.CRACKED)) {
                for (ItemStack stack : bundlePotBlockEntity.getStacks()) {
                    customDrops.add(stack);
                }
                if (blockEntity instanceof DecoratedPotBlockEntity decoratedPotBlockEntity) {
                    decoratedPotBlockEntity.getSherds().stream().map(Item::getDefaultStack).forEach((stack) -> {
                        customDrops.add(stack);
                    });
                }
                ci.setReturnValue(customDrops);
                ci.cancel();
            } else {
                DecoratedPotBlockEntity decoratedPotBlockEntity = (DecoratedPotBlockEntity)blockEntity;
                ItemStack itemStack = DecoratedPotBlockEntity.getStackWith(decoratedPotBlockEntity.getSherds());;
                NbtCompound nbt = new NbtCompound();
                bundlePotBlockEntity.writeNbt(nbt);
                BlockItem.setBlockEntityNbt(itemStack, BlockEntityType.DECORATED_POT, nbt);
                customDrops.add(itemStack);
                ci.setReturnValue(customDrops);
                ci.cancel();
            }
        }
    }
    //*
    @Inject(at = @At("TAIL"), method = "onPlaced")
    public void onPlacedTail(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack, CallbackInfo info) {
        if (!world.isClient) {
            world.getBlockEntity(pos, BlockEntityType.DECORATED_POT).ifPresent((blockEntity) -> {
                blockEntity.readNbtFromStack(itemStack);
            });
        }
    }
    // */
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
