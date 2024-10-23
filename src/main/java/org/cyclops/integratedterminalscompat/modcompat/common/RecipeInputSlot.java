package org.cyclops.integratedterminalscompat.modcompat.common;

import net.minecraft.world.item.ItemStack;

/**
 * @author rubensworks
 */
public interface RecipeInputSlot extends Iterable<ItemStack> {

    public boolean isEmpty();

}
