package tgw.evolution.items;

import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.world.World;

public class ItemTemperature extends ItemEv {

    public ItemTemperature(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, World worldIn, Entity entityIn, int itemSlot, boolean isSelected) {
        CompoundNBT nbt = stack.getChildTag("Temperature");
        if (nbt == null) {
            return;
        }
    }
}
