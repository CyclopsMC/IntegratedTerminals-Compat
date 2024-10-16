package org.cyclops.integratedterminalscompat.modcompat.jei;

import com.mojang.blaze3d.vertex.PoseStack;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.cyclops.cyclopscore.helper.Helpers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A transfer error object that changes appearance based on presence of storage and craftable items.
 * @author rubensworks
 */
public class RecipeTransferErrorColored implements IRecipeTransferError {

    public static final int SLOT_COLOR_MISSING = Helpers.RGBAToInt(255, 0, 0, 100);
    public static final int SLOT_COLOR_CRAFTABLE = Helpers.RGBAToInt(0, 0, 255, 100);

    public static final int HIGHLIGHT_COLOR_FAIL = Helpers.RGBAToInt(255, 0, 0, 100);
    public static final int HIGHLIGHT_COLOR_CRAFTABLE = Helpers.RGBAToInt(0, 0, 255, 100);
    public static final int HIGHLIGHT_COLOR_CRAFTABLE_PARTIAL = Helpers.RGBAToInt(255, 125, 0, 100);

    private final List<Component> message = new ArrayList<>();
    private final Collection<IRecipeSlotView> slotsMissing;
    private final Collection<IRecipeSlotView> slotsCraftable;
    private final int color;

    public RecipeTransferErrorColored(Collection<IRecipeSlotView> slotsMissing, Collection<IRecipeSlotView> slotsCraftable) {
        this.message.add(Component.translatable("jei.tooltip.transfer"));
        this.slotsMissing = slotsMissing;
        this.slotsCraftable = slotsCraftable;
        if (slotsMissing.isEmpty()) {
            // Missing items, but they are all craftable
            this.message.add(Component.translatable("gui.integratedterminalscompat.terminal_storage.jei.transfer.craftable").withStyle(ChatFormatting.RED));
            this.message.add(Component.translatable("gui.integratedterminalscompat.terminal_storage.jei.transfer.craft.info").withStyle(ChatFormatting.ITALIC));
            this.color = HIGHLIGHT_COLOR_CRAFTABLE;
        } else if (!slotsCraftable.isEmpty()) {
            // Missing items, but only some of them are craftable
            this.message.add(Component.translatable("gui.integratedterminalscompat.terminal_storage.jei.transfer.craftable_partial").withStyle(ChatFormatting.RED));
            this.message.add(Component.translatable("gui.integratedterminalscompat.terminal_storage.jei.transfer.craft.info").withStyle(ChatFormatting.ITALIC));
            this.color = HIGHLIGHT_COLOR_CRAFTABLE_PARTIAL;
        } else {
            // Missing items, and none are craftable
            this.message.add(Component.translatable("gui.integratedterminalscompat.terminal_storage.jei.transfer.missing").withStyle(ChatFormatting.ITALIC));
            this.color = HIGHLIGHT_COLOR_FAIL;
        }
    }

    @Override
    public Type getType() {
        return this.slotsCraftable.isEmpty() ? Type.USER_FACING : Type.COSMETIC;
    }

    @Override
    public int getButtonHighlightColor() {
        return this.color;
    }

    @Override
    public void showError(PoseStack poseStack, int mouseX, int mouseY, IRecipeSlotsView recipeSlotsView, int recipeX, int recipeY) {
        Minecraft.getInstance().screen.renderComponentTooltip(poseStack, this.message, mouseX, mouseY);
        poseStack.pushPose();
        poseStack.translate(recipeX, recipeY, 0.0);
        for (IRecipeSlotView slot : this.slotsMissing) {
            slot.drawHighlight(poseStack, SLOT_COLOR_MISSING);
        }
        for (IRecipeSlotView slot : this.slotsCraftable) {
            slot.drawHighlight(poseStack, SLOT_COLOR_CRAFTABLE);
        }
        poseStack.popPose();
    }
}
