package org.cyclops.integratedterminalscompat.modcompat.emi.terminalstorage;

import com.google.common.collect.Lists;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.world.item.ItemStack;
import org.cyclops.integratedterminalscompat.modcompat.common.RecipeInputSlot;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;

/**
 * @author rubensworks
 */
public class RecipeInputSlotEmiIngredient implements RecipeInputSlot {

    private final boolean input;
    private final EmiIngredient emiIngredient;

    public RecipeInputSlotEmiIngredient(EmiIngredient emiIngredient, boolean input) {
        this.emiIngredient = emiIngredient;
        this.input = input;
    }

    @NotNull
    @Override
    public Iterator<ItemStack> iterator() {
        if (!this.input) {
            return Lists.<ItemStack>newArrayList().iterator();
        }
        return emiIngredient.getEmiStacks().stream()
                .map(EmiStack::getItemStack)
                .filter(stack -> !stack.isEmpty())
                .iterator();
    }

    @Override
    public boolean isEmpty() {
        return !this.input || emiIngredient.isEmpty();
    }
}
