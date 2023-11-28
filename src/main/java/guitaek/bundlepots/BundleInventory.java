package guitaek.bundlepots;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public interface BundleInventory extends Inventory {
    List<ItemStack> getStacks();

    BlockEntity asBlockEntity();

    default int contentSize() {
        NbtCompound nbt = new NbtCompound();
        this.writeNbt(nbt);
        return BundlePotCalculator.getTotalContentSize(nbt);
    }

    default int size() {
        // "size is artificial, but technically 64
        return 64;
    }

    default boolean isEmpty() {
        return this.getStacks().isEmpty();
    }

    @Override
    default boolean isValid(int slot, ItemStack stack) {
        return this.isAddable(stack);
    }
    @Override
    default ItemStack getStack(int slot) {
        if (this.getStacks().size() <= slot) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = this.getStacks().get(slot);
        if (stack == null) {
            return ItemStack.EMPTY;
        }
        return stack;
    }

    @Override
    default ItemStack removeStack(int slot, int amount) {
        if (slot >= this.getStacks().size()) {
            return null;
        }
        int count = this.getStacks().get(slot).getCount();
        if (count < amount) {
            throw new IllegalArgumentException("shall not happen, else issue a bug");
        } else {
            ItemStack stack = this.getStack(slot);
            if (count == amount) {
                this.getStacks().remove(slot);
                return stack;
            }
            return stack.split(amount);
        }
    }

    @Override
    default ItemStack removeStack(int slot) {
        if (slot >= this.getStacks().size()) {
            return null;
        }
        ItemStack result = this.getStacks().get(slot);
        this.getStacks().remove(slot);
        return result;
    }

    @Override
    default void setStack(int slot, ItemStack stack) {
        int currSize =this.getStacks().size();
        if (currSize <= slot) {
            // justification for the number of elements: when currSize == slot, we need
            // one element more
            this.getStacks().addAll(Collections.nCopies(slot - currSize + 1, null));
        }
        this.getStacks().set(slot, stack);
    }

    @Override
    default void markDirty() {
        this.asBlockEntity().markDirty();
    }

    default void clear() {
        this.getStacks().clear();
    }
    default boolean canPlayerUse(PlayerEntity player) {
        return false;
    }

    void writeNbt(NbtCompound nbt);

    default boolean isAddable(ItemStack stack) {
        NbtCompound nbt = new NbtCompound();
        this.writeNbt(nbt);
        int potSize = BundlePotCalculator.getTotalContentSize(nbt);
        int itemSize = BundlePotCalculator.getResultingItemSize(stack);
        int space = 64 - potSize - itemSize;
        return space >= 0;
    }
    default void addItem(ItemStack stack) {
        if (!this.isAddable(stack)) {
            return;
        } else {
            Optional<ItemStack> optional = this.canMergeStack(stack);
            if (optional.isPresent()) {
                ItemStack preexistingStack = optional.get();
                preexistingStack.increment(stack.getCount());
            } else {
                this.getStacks().add(stack);
            }
        }
    }


    default Optional<ItemStack> canMergeStack(ItemStack newStack) {
        return this.getStacks().stream().filter((item) ->
            ItemStack.canCombine(item, newStack)
        ).findFirst();
    }
}
