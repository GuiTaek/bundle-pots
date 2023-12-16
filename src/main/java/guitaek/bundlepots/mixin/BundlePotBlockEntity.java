package guitaek.bundlepots.mixin;

import guitaek.bundlepots.BundleInventory;
import guitaek.bundlepots.BundlePotCalculator;
import guitaek.bundlepots.BundlePots;
import guitaek.bundlepots.access.IBundlePotBlockEntity;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.DecoratedPotBlockEntity;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;

import org.apache.commons.compress.harmony.pack200.NewAttributeBands;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.security.auth.callback.Callback;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

@Mixin(DecoratedPotBlockEntity.class)
public class BundlePotBlockEntity
        extends BlockEntity
        implements RandomizableContainer, BundleInventory {

    private BundleInventory inventory;
    public int size() { return this.inventory.getContainerSize(); }
    public boolean isEmpty() { return this.inventory.isEmpty(); }
    public ItemStack getItem(int slot) { return this.inventory.getItem(slot); }
    public ItemStack removeItem(int slot, int amount) { return this.inventory.removeItem(slot, amount); }
    public ItemStack removeItemNoUpdate(int slot) { return this.inventory.removeItemNoUpdate(slot); }
    public void setItem(int slot, ItemStack stack) { this.inventory.setItem(slot, stack); }
    public boolean stillValid(Player player) { return this.inventory.stillValid(player); }
    public void clearContent() {this.inventory.clearContent();}
    @Inject(method="<init>(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V", at=@At("TAIL"))
    private void init_stacks(CallbackInfo info) {
        this.inventory = new BundleInventory() {
            @Override
            public List<ItemStack> getStacks() {
                return BundlePotBlockEntity.this.stacks;
            }

            @Override
            public BlockEntity asBlockEntity() {
                return BundlePotBlockEntity.this;
            }

            @Override
            public void writeToNbt(CompoundTag nbt) { BundlePotBlockEntity.this.writeToNbt(nbt); }
        };
        this.stacks = new ArrayList<>();
    }
    private BundlePotBlockEntity() {
        super(null, null, null);
        throw new AssertionError("this constructor shall never be called!");
    }
    private ArrayList<ItemStack> stacks = new ArrayList<>();
    @Shadow private DecoratedPotBlockEntity.Decorations decorations;
    @Shadow @Nullable public ResourceLocation getLootTable() { return null; }
    @Shadow public void setLootTable(@Nullable ResourceLocation lootTableId) { }
    @Shadow public long getLootTableSeed() { return 0; }
    @Shadow public void setLootTableSeed(long lootTableSeed) { }
    @Shadow protected void saveAdditional(CompoundTag nbt) { }

    // should be an invoker, unfortunately according to https://github.com/SpongePowered/Mixin/issues/399
    // it doesn't seem to be possible to have two Invokes and I guess an invoke and an inject
    // also counts. Fortunately, I'm not dependent on using invokes
    public void writeToNbt(CompoundTag nbt) {
        this.saveAdditional(nbt);
    }
    @Inject(method = "saveAdditional", at = @At("TAIL"))
    public void writeNbtTail(CompoundTag nbt, CallbackInfo info) {
        if (!this.trySaveLootTable(nbt) && !this.stacks.isEmpty()) {
            ListTag nbtTag = new ListTag();
            this.stacks.forEach((stack) -> {
                CompoundTag nbtToAdd = new CompoundTag();
                stack.save(nbtToAdd);
                nbtTag.add(0, nbtToAdd);
            });
            nbt.put("Items", nbtTag);
        }

    }
    @Inject(method = "load", at = @At("TAIL"))
    public void loadTail(CompoundTag nbt, CallbackInfo info) {
        if (!this.tryLoadLootTable(nbt)) {
            this.stacks = new ArrayList<>(BundlePotCalculator.getStacksFromNbt(nbt));
        }
    }

    @Inject(at = @At("TAIL"), method = "setFromItem")
    public void readNbtFromStackTail(ItemStack stack, CallbackInfo info) {
        if(stack.getTag() != null) {
            this.load(BlockItem.getBlockEntityData(stack));
        }
    }

    @Inject(at = @At("RETURN"), method="getPotAsItem", cancellable = true)
    public void asStackReturn(CallbackInfoReturnable<ItemStack> info) {
        ItemStack stack = info.getReturnValue();
        CompoundTag nbt = new CompoundTag();
        this.writeToNbt(nbt);
        BlockItem.setBlockEntityData(stack, BlockEntityType.DECORATED_POT, nbt);
        info.setReturnValue(stack);
    }
    // all following methods are scraped from bundle and what I don't find good is deleted
    public List<ItemStack> getStacks() {
        return this.stacks;
    }

    public BlockEntity asBlockEntity() {
        return this;
    }

}

