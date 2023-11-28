package guitaek.bundlepots.mixin;

import guitaek.bundlepots.BundlePotCalculator;
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

@Mixin(DecoratedPotBlockEntity.class)
public class BundlePotBlockEntity
        extends BlockEntity
        implements IBundlePotBlockEntity, LootableInventory, SingleStackInventory {
    @Inject(method="<init>(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)V", at=@At("TAIL"))
    private void init_stacks(CallbackInfo info) {
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
    public void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        this.sherds.toNbt(nbt);
        NbtList nbtList = new NbtList();
        this.stacks.forEach((stack) -> {
            NbtCompound nbtToAdd = new NbtCompound();
            stack.writeNbt(nbtToAdd);
            nbtList.add(0, nbtToAdd);
        });

        if (!this.writeLootTable(nbt) && !this.stacks.isEmpty()) {
            nbt.put("Items", nbtList);
        }

    }
    public boolean isAddable(ItemStack stack) {
        NbtCompound nbt = new NbtCompound();
        this.writeNbt(nbt);
        int potSize = BundlePotCalculator.getTotalContentSize(nbt);
        int itemSize = BundlePotCalculator.getResultingItemSize(stack);
        int space = 64 - potSize - itemSize;
        return space >= 0;
    }
    public void addItem(ItemStack stack) {
        if (!this.isAddable(stack)) {
            return;
        } else {
            Optional<ItemStack> optional = this.canMergeStack(stack);
            if (optional.isPresent()) {
                ItemStack preexistingStack = optional.get();
                preexistingStack.increment(stack.getCount());
            } else {
                this.stacks.add(stack);
            }
        }
    }
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        this.sherds = DecoratedPotBlockEntity.Sherds.fromNbt(nbt);
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
    @Override
    public ArrayList<ItemStack> getStacks() {
        return this.stacks;
    }


    private Optional<ItemStack> canMergeStack(ItemStack newStack) {
        return this.stacks.stream().filter((item) -> {
            return ItemStack.canCombine(item, newStack);
        }).findFirst();
    }

    @Shadow public ItemStack getStack() {
        return null;
    }

    @Shadow public ItemStack decreaseStack(int count) {
        return null;
    }

    @Shadow public void setStack(ItemStack stack) { }
    @Shadow public BlockEntity asBlockEntity() {
        return null;
    }
}

