package org.cyclops.integratedterminalscompat.modcompat.jei;

import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.cyclops.integratedterminalscompat.modcompat.common.RecipeTransferResult;

import java.util.List;
import java.util.stream.Stream;

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
    public List<Component> getTooltip() {
        return Stream.concat(Stream.of(Component.translatable("jei.tooltip.transfer")), this.result.getMessage().stream()).toList();
    }

    @Override
    public void showError(GuiGraphics guiGraphics, int mouseX, int mouseY, IRecipeSlotsView recipeSlotsView, int recipeX, int recipeY) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(recipeX, recipeY, 0.0);
        for (RecipeInputSlotJei slot : this.result.getSlotsMissing()) {
            slot.getSlotView().drawHighlight(guiGraphics, RecipeTransferResult.SLOT_COLOR_MISSING);
        }
        for (RecipeInputSlotJei slot : this.result.getSlotsCraftable()) {
            slot.getSlotView().drawHighlight(guiGraphics, RecipeTransferResult.SLOT_COLOR_CRAFTABLE);
        }
        guiGraphics.pose().popPose();
    }
}
