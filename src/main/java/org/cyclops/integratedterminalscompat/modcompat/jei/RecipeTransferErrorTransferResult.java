package org.cyclops.integratedterminalscompat.modcompat.jei;

import com.mojang.blaze3d.vertex.PoseStack;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.cyclops.integratedterminalscompat.modcompat.common.RecipeTransferResult;

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

    @Override
    public void showError(PoseStack poseStack, int mouseX, int mouseY, IRecipeSlotsView recipeSlotsView, int recipeX, int recipeY) {
        Minecraft.getInstance().screen.renderComponentTooltip(poseStack, Stream.concat(Stream.of(Component.translatable("jei.tooltip.transfer")), this.result.getMessage().stream()).toList(), mouseX, mouseY);
        poseStack.pushPose();
        poseStack.translate(recipeX, recipeY, 0.0);
        for (RecipeInputSlotJei slot : this.result.getSlotsMissing()) {
            slot.getSlotView().drawHighlight(poseStack, RecipeTransferResult.SLOT_COLOR_MISSING);
        }
        for (RecipeInputSlotJei slot : this.result.getSlotsCraftable()) {
            slot.getSlotView().drawHighlight(poseStack, RecipeTransferResult.SLOT_COLOR_CRAFTABLE);
        }
        poseStack.popPose();
    }
}
