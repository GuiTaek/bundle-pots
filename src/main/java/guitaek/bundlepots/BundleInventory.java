package guitaek.bundlepots;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public interface BundleInventory extends Container {
    List<ItemStack> getStacks();

    BlockEntity asBlockEntity();

    default int contentSize() {
        CompoundTag nbt = new CompoundTag();
        this.writeToNbt(nbt);
        return BundlePotCalculator.getTotalContentSize(nbt);
    }

    @Override
    default int getContainerSize() {
        // "size is artificial, but technically 64
        return 64;
    }

    @Override
    default boolean isEmpty() {
        return this.getStacks().isEmpty();
    }

    @Override
    default boolean canPlaceItem(int slot, ItemStack stack) {
        return this.isAddable(stack);
    }
    @Override
    default ItemStack getItem(int slot) {
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
    default ItemStack removeItem(int slot, int amount) {
        if (slot >= this.getStacks().size()) {
            return null;
        }
        int count = this.getStacks().get(slot).getCount();
        if (count < amount) {
            throw new IllegalArgumentException("shall not happen, else issue a bug");
        } else {
            ItemStack stack = this.getItem(slot);
            if (count == amount) {
                this.getStacks().remove(slot);
                return stack;
            }
            return stack.split(amount);
        }
    }

    @Override
    default ItemStack removeItemNoUpdate(int slot) {
        if (slot >= this.getStacks().size()) {
            return null;
        }
        ItemStack result = this.getStacks().get(slot);
        this.getStacks().remove(slot);
        return result;
    }

    @Override
    default void setItem(int slot, ItemStack stack) {
        int currSize =this.getStacks().size();
        if (currSize <= slot) {
            // justification for the number of elements: when currSize == slot, we need
            // one element more
            this.getStacks().addAll(Collections.nCopies(slot - currSize + 1, null));
        }
        this.getStacks().set(slot, stack);
    }

    @Override
    default void setChanged() {
        this.asBlockEntity().setChanged();
    }

    @Override
    default void clearContent() {
        this.getStacks().clear();
    }

    @Override
    default boolean stillValid(Player player) {
        return false;
    }

    // was implemented in the fabric version, therefore the following comment
    // has to be named differently than in target class,
    // see https://fabricmc.net/wiki/tutorial:mixin_accessors, the comment
    // in BundlePotBlockEntity.writeToNbt and the commit message
    // that introduced this comment
    void writeToNbt(CompoundTag nbt);

    default boolean isAddable(ItemStack stack) {
        CompoundTag nbt = new CompoundTag();
        this.writeToNbt(nbt);
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
                preexistingStack.grow(stack.getCount());
            } else {
                this.getStacks().add(stack);
            }
        }
    }


    default Optional<ItemStack> canMergeStack(ItemStack newStack) {
        return this.getStacks().stream().filter((item) ->
                ItemStack.isSameItemSameTags(item, newStack)
        ).findFirst();
    }
}
