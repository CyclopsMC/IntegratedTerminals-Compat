package org.cyclops.integratedterminalscompat.modcompat.jei;

import com.google.common.collect.Lists;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.world.item.ItemStack;
import org.cyclops.integratedterminalscompat.modcompat.common.RecipeInputSlot;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.stream.Stream;

/**
 * @author rubensworks
 */
public class RecipeInputSlotJei implements RecipeInputSlot {

    private final IRecipeSlotView slotView;

    public RecipeInputSlotJei(IRecipeSlotView slotView) {
        this.slotView = slotView;
    }

    @Override
    public boolean isEmpty() {
        return this.slotView.getRole() != RecipeIngredientRole.INPUT || this.slotView.isEmpty();
    }

    public IRecipeSlotView getSlotView() {
        return slotView;
    }

    @NotNull
    @Override
    public Iterator<ItemStack> iterator() {
        if (this.slotView.getRole() != RecipeIngredientRole.INPUT) {
            return Lists.<ItemStack>newArrayList().iterator();
        }
        return ((Stream<ITypedIngredient<ItemStack>>) (Stream) slotView.getAllIngredients())
                .map(ITypedIngredient::getIngredient)
                .iterator();
    }
}
