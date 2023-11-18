package guitaek.access;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

import java.util.ArrayList;

public interface IBundlePotBlockEntity {
    boolean isAddable(ItemStack stack);
    void addItem(ItemStack item);
    ArrayList<ItemStack> getStacks();
    void writeNbt(NbtCompound nbt);
}
