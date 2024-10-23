package org.cyclops.integratedterminalscompat.modcompat.rei;

import com.google.common.collect.Lists;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import net.minecraft.world.item.ItemStack;
import org.cyclops.integratedterminalscompat.modcompat.common.RecipeInputSlot;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;

/**
 * @author rubensworks
 */
public class RecipeInputSlotRei implements RecipeInputSlot {

    private final EntryIngredient ingredient;
    private final boolean input;
    private final int index;

    public RecipeInputSlotRei(EntryIngredient ingredient, boolean input, int index) {
        this.ingredient = ingredient;
        this.input = input;
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    @Override
    public boolean isEmpty() {
        return !this.input || this.ingredient.isEmpty();
    }

    @NotNull
    @Override
    public Iterator<ItemStack> iterator() {
        if (!this.input) {
            return Lists.<ItemStack>newArrayList().iterator();
        }
        return this.ingredient.stream()
                .map(i -> i.<ItemStack>castValue())
                .iterator();
    }
}
