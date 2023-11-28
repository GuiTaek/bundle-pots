package guitaek.bundlepots.mixin;

import guitaek.bundlepots.BundleInventory;
import guitaek.bundlepots.BundlePotCalculator;
import guitaek.bundlepots.BundlePots;
import guitaek.bundlepots.access.IBundlePotBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.DecoratedPotBlockEntity;
import net.minecraft.inventory.LootableInventory;
import net.minecraft.inventory.SingleStackInventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.Optional;
import java.util.logging.Logger;

@Mixin(DecoratedPotBlockEntity.class)
public class BundlePotBlockEntity
        extends BlockEntity
        implements LootableInventory, BundleInventory {

    private BundleInventory inventory;
    public int size() { return this.inventory.size(); }
    public boolean isEmpty() { return this.inventory.isEmpty(); }
    public ItemStack getStack(int slot) { return this.inventory.getStack(slot); }
    public ItemStack removeStack(int slot, int amount) { return this.inventory.removeStack(slot, amount); }
    public ItemStack removeStack(int slot) { return this.inventory.removeStack(slot); }
    public void setStack(int slot, ItemStack stack) { this.inventory.setStack(slot, stack); }
    public boolean canPlayerUse(PlayerEntity player) { return this.inventory.canPlayerUse(player); }
    public void clear() {this.inventory.clear();}
    @Inject(method="<init>(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)V", at=@At("TAIL"))
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
            public void writeNbt(NbtCompound nbt) { BundlePotBlockEntity.this.writeNbt(nbt);}
        };
        this.stacks = new ArrayList<>();
    }
    private BundlePotBlockEntity() {
        super(null, null, null);
        throw new AssertionError("this constructor shall never be called!");
    }
    private ArrayList<ItemStack> stacks = new ArrayList<>();
    @Shadow private DecoratedPotBlockEntity.Sherds sherds;
    @Shadow @Nullable public Identifier getLootTableId() { return null; }
    @Shadow public void setLootTableId(@Nullable Identifier lootTableId) { }
    @Shadow public long getLootTableSeed() { return 0; }
    @Shadow public void setLootTableSeed(long lootTableSeed) { }
    @Shadow public void writeNbt(NbtCompound nbt) { }
    @Inject(method = "writeNbt", at = @At("TAIL"))
    public void writeNbtTail(NbtCompound nbt, CallbackInfo info) {
        if (!this.writeLootTable(nbt) && !this.stacks.isEmpty()) {
            NbtList nbtList = new NbtList();
            this.stacks.forEach((stack) -> {
                NbtCompound nbtToAdd = new NbtCompound();
                stack.writeNbt(nbtToAdd);
                nbtList.add(0, nbtToAdd);
            });
            nbt.put("Items", nbtList);
        }

    }
    @Inject(method = "readNbt", at = @At("TAIL"))
    public void readNbtTail(NbtCompound nbt, CallbackInfo info) {
        if (!this.readLootTable(nbt)) {
            this.stacks = new ArrayList<>(BundlePotCalculator.getStacksFromNbt(nbt));
        }
    }

    @Inject(at = @At("TAIL"), method = "readNbtFromStack")
    public void readNbtFromStackTail(ItemStack stack, CallbackInfo info) {
        if(stack.getNbt() != null) {
            this.readNbt(BlockItem.getBlockEntityNbt(stack));
        }
    }

    @Inject(at = @At("RETURN"), method="asStack", cancellable = true)
    public void asStackReturn(CallbackInfoReturnable<ItemStack> info) {
        ItemStack stack = info.getReturnValue();
        NbtCompound nbt = new NbtCompound();
        this.writeNbt(nbt);
        BlockItem.setBlockEntityNbt(stack, BlockEntityType.DECORATED_POT, nbt);
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

