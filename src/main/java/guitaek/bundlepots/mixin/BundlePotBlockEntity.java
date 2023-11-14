package guitaek.bundlepots.mixin;

import guitaek.access.IBundlePotBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.DecoratedPotBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.DecoratedPotBlockEntity;
import net.minecraft.inventory.LootableInventory;
import net.minecraft.inventory.SingleStackInventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.text.MessageFormat;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
        int bundleOcc = this.getTotalOccupancy();
        int itemOcc = getItemOccupancy(stack);
        int space = (64 - bundleOcc) / itemOcc;
        return space > 0;
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
            this.stacks = getStacksFromNbt(nbt);
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
    private static int getItemOccupancy(ItemStack stack) {
        return 64 / stack.getMaxCount();
    }
    public int getTotalOccupancy() {
        return this.stacks.stream().mapToInt((itemStack) -> {
            return getItemOccupancy(itemStack) * itemStack.getCount();
        }).sum();
    }

    @Override
    public ArrayList<ItemStack> getStacks() {
        return this.stacks;
    }

    private ArrayList<ItemStack> getStacksFromNbt(NbtCompound nbtCompound) {
        if (nbtCompound == null) {
            return new ArrayList<>();
        } else {
            NbtList nbtList = nbtCompound.getList("Items", 10);
            Stream<NbtElement> var10000 = nbtList.stream();
            return var10000.map((Function<NbtElement, NbtCompound>) element -> {
                if (element instanceof NbtCompound) {
                    return (NbtCompound) element;
                } else {
                    throw new IllegalStateException("element in nbt-list isn't a NbtCompound" + element);
                }
            }).map(ItemStack::fromNbt).collect(Collectors.toCollection(ArrayList<ItemStack>::new));
        }
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

