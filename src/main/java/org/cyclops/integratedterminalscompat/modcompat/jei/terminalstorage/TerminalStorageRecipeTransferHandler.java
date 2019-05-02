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
import org.cyclops.commoncapabilities.api.ingredient.IngredientComponent;
import org.cyclops.cyclopscore.ingredient.collection.IIngredientCollectionMutable;
import org.cyclops.cyclopscore.ingredient.collection.IngredientCollectionPrototypeMap;
import org.cyclops.integratedterminals.core.terminalstorage.TerminalStorageTabIngredientComponentClient;
import org.cyclops.integratedterminals.core.terminalstorage.TerminalStorageTabIngredientComponentItemStackCrafting;
import org.cyclops.integratedterminals.inventory.container.ContainerTerminalStorage;
import org.cyclops.integratedterminalscompat.IntegratedTerminalsCompat;
import org.cyclops.integratedterminalscompat.network.packet.TerminalStorageIngredientItemStackCraftingGridSetRecipe;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

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

        if (Objects.equals(container.getSelectedTab(), TerminalStorageTabIngredientComponentItemStackCrafting.NAME.toString())) {
            if (!doTransfer) {
                // Check in the local client view if the required recipe ingredients are available
                TerminalStorageTabIngredientComponentClient tabClient = (TerminalStorageTabIngredientComponentClient)
                        container.getTabClient(container.getSelectedTab());
                List<TerminalStorageTabIngredientComponentClient.InstanceWithMetadata<ItemStack>> unfilteredIngredients = tabClient
                        .getUnfilteredIngredientsView(container.getSelectedChannel());
                IIngredientCollectionMutable<ItemStack, Integer> hayStack = new IngredientCollectionPrototypeMap<>(IngredientComponent.ITEMSTACK);
                hayStack.addAll(unfilteredIngredients
                        .stream()
                        .filter(i -> i.getCraftingOption() == null)
                        .map(TerminalStorageTabIngredientComponentClient.InstanceWithMetadata::getInstance)
                        .collect(Collectors.toList()));
                List<Integer> slotsMissingItems = Lists.newArrayList();

                for (Map.Entry<Integer, ? extends IGuiIngredient<ItemStack>> entry : recipeLayout.getItemStacks().getGuiIngredients().entrySet()) {
                    IGuiIngredient<ItemStack> ingredient = entry.getValue();
                    if (ingredient != null && ingredient.isInput()) {
                        int slot = entry.getKey();
                        if (!ingredient.getAllIngredients().isEmpty()) {
                            boolean found = false;
                            for (ItemStack itemStack : ingredient.getAllIngredients()) {
                                if (hayStack.contains(itemStack, ItemMatch.ITEM | ItemMatch.DAMAGE | ItemMatch.NBT)) {
                                    hayStack.remove(itemStack);
                                    found = true;
                                    break;
                                }
                            }
                            if (!found) {
                                slotsMissingItems.add(slot);
                            }

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
