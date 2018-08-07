package org.cyclops.integratedterminalscompat.modcompat.jei.terminalstorage;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import mezz.jei.api.gui.IGuiIngredient;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.recipe.VanillaRecipeCategoryUid;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.util.Translator;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import org.cyclops.commoncapabilities.api.capability.itemhandler.ItemMatch;
import org.cyclops.cyclopscore.ingredient.collection.IIngredientCollection;
import org.cyclops.integratedterminals.core.terminalstorage.TerminalStorageTabIngredientComponentClient;
import org.cyclops.integratedterminals.core.terminalstorage.TerminalStorageTabIngredientComponentItemStackCrafting;
import org.cyclops.integratedterminals.inventory.container.ContainerTerminalStorage;
import org.cyclops.integratedterminalscompat.IntegratedTerminalsCompat;
import org.cyclops.integratedterminalscompat.network.packet.TerminalStorageIngredientItemStackCraftingGridSetRecipe;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

/**
 * Handles recipe clicking from JEI.
 * @author rubensworks
 */
public class TerminalStorageRecipeTransferHandler implements IRecipeTransferHandler<ContainerTerminalStorage> {

    private final IRecipeTransferHandlerHelper recipeTransferHandlerHelper;

    public TerminalStorageRecipeTransferHandler(IRecipeTransferHandlerHelper recipeTransferHandlerHelper) {
        this.recipeTransferHandlerHelper = recipeTransferHandlerHelper;
    }

    @Override
    public Class<ContainerTerminalStorage> getContainerClass() {
        return ContainerTerminalStorage.class;
    }

    @Nullable
    @Override
    public IRecipeTransferError transferRecipe(ContainerTerminalStorage container, IRecipeLayout recipeLayout,
                                               EntityPlayer player, boolean maxTransfer, boolean doTransfer) {
        if (!recipeLayout.getRecipeCategory().getUid().equals(VanillaRecipeCategoryUid.CRAFTING)) {
            return new TransferError();
        }

        if (container.getSelectedTab().equals(TerminalStorageTabIngredientComponentItemStackCrafting.NAME.toString())) {
            if (!doTransfer) {
                // Check in the local client view if the required recipe ingredients are available
                TerminalStorageTabIngredientComponentClient tabClient = (TerminalStorageTabIngredientComponentClient)
                        container.getTabClient(container.getSelectedTab());
                IIngredientCollection<ItemStack, Integer> unfilteredIngredients = tabClient
                        .getUnfilteredIngredientsView(container.getSelectedChannel());
                List<Integer> slotsMissingItems = Lists.newArrayList();

                for (Map.Entry<Integer, ? extends IGuiIngredient<ItemStack>> entry : recipeLayout.getItemStacks().getGuiIngredients().entrySet()) {
                    IGuiIngredient<ItemStack> ingredient = entry.getValue();
                    if (ingredient != null && ingredient.isInput()) {
                        int slot = entry.getKey();
                        if (!ingredient.getAllIngredients().isEmpty() && ingredient.getAllIngredients().stream()
                                .noneMatch((i) -> unfilteredIngredients.contains(i,ItemMatch.ITEM | ItemMatch.DAMAGE | ItemMatch.NBT))) {
                            slotsMissingItems.add(slot);
                        }
                    }
                }

                if (!slotsMissingItems.isEmpty()) {
                    String message = Translator.translateToLocal("jei.tooltip.error.recipe.transfer.missing");
                    return recipeTransferHandlerHelper.createUserErrorForSlots(message, slotsMissingItems);
                }

                return null;
            } else {
                // Send a packet to the server if the recipe effectively needs to be applied to the grid
                Map<Integer, List<ItemStack>> slottedIngredients = Maps.newHashMap();
                for (Map.Entry<Integer, ? extends IGuiIngredient<ItemStack>> entry : recipeLayout.getItemStacks().getGuiIngredients().entrySet()) {
                    IGuiIngredient<ItemStack> ingredient = entry.getValue();
                    if (ingredient != null && ingredient.isInput()) {
                        int slot = entry.getKey();

                        slottedIngredients.put(slot, ingredient.getAllIngredients());
                    }
                }

                IntegratedTerminalsCompat._instance.getPacketHandler().sendToServer(
                        new TerminalStorageIngredientItemStackCraftingGridSetRecipe(container.getSelectedTab(),
                                container.getSelectedChannel(), maxTransfer, slottedIngredients));
                return null;
            }
        }

        return new TransferError();
    }

    public static class TransferError implements IRecipeTransferError {

        @Override
        public Type getType() {
            return Type.INTERNAL;
        }

        @Override
        public void showError(Minecraft minecraft, int mouseX, int mouseY, IRecipeLayout recipeLayout, int recipeX, int recipeY) {
            // Silently fail
        }
    }
}
