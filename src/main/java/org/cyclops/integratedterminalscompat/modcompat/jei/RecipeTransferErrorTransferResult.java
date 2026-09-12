package org.cyclops.integratedterminalscompat.modcompat.jei;

import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import org.cyclops.integratedterminalscompat.modcompat.common.RecipeTransferResult;

/**
 * @author rubensworks
 */
public class RecipeTransferErrorTransferResult implements IRecipeTransferError {

    private final RecipeTransferResult<RecipeInputSlotJei> result;

    public RecipeTransferErrorTransferResult(RecipeTransferResult<RecipeInputSlotJei> result) {
        this.result = result;
    }

    @Override
    public Type getType() {
        return Type.COSMETIC;
    }

    @Override
    public int getButtonHighlightColor() {
        return this.result.getButtonHighlightColor();
    }

    // Let JEI draw the tooltip in its own tooltip pass, which happens after showError.
    // Rendering it inside showError would put the slot highlights on top of it.
    @Override
    public void getTooltip(ITooltipBuilder tooltipBuilder) {
        tooltipBuilder.add(Component.translatable("jei.tooltip.transfer"));
        tooltipBuilder.addAll(this.result.getMessage());
    }

    @Override
    public void showError(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, IRecipeSlotsView recipeSlotsView, int recipeX, int recipeY) {
        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate(recipeX, recipeY);
        for (RecipeInputSlotJei slot : this.result.getSlotsMissing()) {
            slot.getSlotView().drawHighlight(guiGraphics, RecipeTransferResult.SLOT_COLOR_MISSING);
        }
        for (RecipeInputSlotJei slot : this.result.getSlotsCraftable()) {
            slot.getSlotView().drawHighlight(guiGraphics, RecipeTransferResult.SLOT_COLOR_CRAFTABLE);
        }
        guiGraphics.pose().popMatrix();
    }
}
