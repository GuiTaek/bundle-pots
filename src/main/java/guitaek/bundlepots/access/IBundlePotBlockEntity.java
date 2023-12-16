package guitaek.bundlepots.access;

import guitaek.bundlepots.BundleInventory;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public interface IBundlePotBlockEntity extends BundleInventory {
    List<ItemStack> getStacks();
    void writeNbt(CompoundTag nbt);
}
