package guitaek.bundlepots;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class BundlePotCalculator {
    // unfortunately, you can't overwrite Item.getMaxCount, therefore I have to be creative
    public static int getResultingItemSize(ItemStack stack) {
        int firstResult = stack.getCount() * 64 / stack.getMaxStackSize();
        CompoundTag stackNbt = stack.getTag();
        if (stackNbt == null) {
            return firstResult;
        }
        CompoundTag entityNbt = stackNbt.getCompound("BlockEntityTag");
        if (entityNbt == null) {
            return firstResult;
        }
        return firstResult + BundlePotCalculator.getTotalContentSize(entityNbt);
    }
    public static int getTotalContentSize(CompoundTag nbt) {
        return BundlePotCalculator.getStacksFromNbt(nbt).stream().mapToInt((itemStack) ->
            getResultingItemSize(itemStack)
        ).sum();
    }

    public static List<ItemStack> getStacksFromNbt(CompoundTag nbtCompound) {
        if (nbtCompound == null) {
            return new ArrayList<>();
        }
        return nbtCompound.getList("Items", 10).stream()
                .map((Function<Tag, CompoundTag>) element -> {
            if (element instanceof CompoundTag) {
                return (CompoundTag) element;
            } else {
                throw new IllegalStateException("element in nbt-list isn't a NbtCompound" + element);
            }
        }).map(ItemStack::of).collect(Collectors.toCollection(ArrayList<ItemStack>::new));
    }
}
