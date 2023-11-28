package guitaek.bundlepots;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class BundlePotCalculator {
    // unfortunately, you can't overwrite Item.getMaxCount, therefore I have to be creative
    public static int getResultingItemSize(ItemStack stack) {
        int firstResult = stack.getCount() * 64 / stack.getMaxCount();
        NbtCompound stackNbt = stack.getNbt();
        if (stackNbt == null) {
            return firstResult;
        }
        NbtCompound entityNbt = stackNbt.getCompound("BlockEntityTag");
        if (entityNbt == null) {
            return firstResult;
        }
        return firstResult + BundlePotCalculator.getTotalContentSize(entityNbt);
    }
    public static int getTotalContentSize(NbtCompound nbt) {
        return BundlePotCalculator.getStacksFromNbt(nbt).stream().mapToInt((itemStack) ->
            getResultingItemSize(itemStack)
        ).sum();
    }

    public static List<ItemStack> getStacksFromNbt(NbtCompound nbtCompound) {
        if (nbtCompound == null) {
            return new ArrayList<>();
        }
        return nbtCompound.getList("Items", 10).stream()
                .map((Function<NbtElement, NbtCompound>) element -> {
            if (element instanceof NbtCompound) {
                return (NbtCompound) element;
            } else {
                throw new IllegalStateException("element in nbt-list isn't a NbtCompound" + element);
            }
        }).map(ItemStack::fromNbt).collect(Collectors.toCollection(ArrayList<ItemStack>::new));
    }
}
