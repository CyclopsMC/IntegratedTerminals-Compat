package org.cyclops.integratedterminalscompat.modcompat.jei;

import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.runtime.IClickableIngredient;
import net.minecraft.client.renderer.Rect2i;

/**
 * Copied and modified from internal JEI.
 *
 * @author rubensworks
 */
public record ClickableIngredient<V>(ITypedIngredient<V> value, Rect2i area) implements IClickableIngredient<V> {
    @Override
    public ITypedIngredient<V> getTypedIngredient() {
        return value;
    }

    @Override
    public Rect2i getArea() {
        return area;
    }
}
