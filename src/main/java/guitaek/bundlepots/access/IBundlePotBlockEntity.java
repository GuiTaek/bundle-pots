package guitaek.bundlepots.access;

import guitaek.bundlepots.BundleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

import java.util.ArrayList;
import java.util.List;

public interface IBundlePotBlockEntity extends BundleInventory {
    List<ItemStack> getStacks();
    void writeNbt(NbtCompound nbt);
}
