package org.cyclops.integratedterminalscompat.modcompat.jei.terminalstorage;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.blaze3d.matrix.MatrixStack;
import mezz.jei.api.constants.VanillaRecipeCategoryUid;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.gui.ingredient.IGuiIngredient;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.util.Translator;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.wrapper.InvWrapper;
import org.cyclops.commoncapabilities.api.capability.itemhandler.ItemMatch;
import org.cyclops.commoncapabilities.api.ingredient.IngredientComponent;
import org.cyclops.commoncapabilities.ingredient.storage.IngredientComponentStorageWrapperHandlerItemStack;
import org.cyclops.cyclopscore.ingredient.collection.IIngredientCollectionMutable;
import org.cyclops.cyclopscore.ingredient.collection.IngredientCollectionPrototypeMap;
import org.cyclops.integratedterminals.api.terminalstorage.ITerminalStorageTabCommon;
import org.cyclops.integratedterminals.core.terminalstorage.TerminalStorageTabIngredientComponentClient;
import org.cyclops.integratedterminals.core.terminalstorage.TerminalStorageTabIngredientComponentItemStackCrafting;
import org.cyclops.integratedterminals.core.terminalstorage.TerminalStorageTabIngredientComponentItemStackCraftingCommon;
import org.cyclops.integratedterminals.inventory.container.ContainerTerminalStorageBase;
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
public class TerminalStorageRecipeTransferHandler<T extends ContainerTerminalStorageBase<?>> implements IRecipeTransferHandler<T> {

    private final IRecipeTransferHandlerHelper recipeTransferHandlerHelper;
    private final Class<T> clazz;

    public TerminalStorageRecipeTransferHandler(IRecipeTransferHandlerHelper recipeTransferHandlerHelper, Class<T> clazz) {
        this.recipeTransferHandlerHelper = recipeTransferHandlerHelper;
        this.clazz = clazz;
    }

    @Override
    public Class<T> getContainerClass() {
        return this.clazz;
    }

    @Nullable
    @Override
    public IRecipeTransferError transferRecipe(ContainerTerminalStorageBase container, IRecipeLayout recipeLayout,
                                               PlayerEntity player, boolean maxTransfer, boolean doTransfer) {
        if (!recipeLayout.getRecipeCategory().getUid().equals(VanillaRecipeCategoryUid.CRAFTING)) {
            return new TransferError();
        }

        if (Objects.equals(container.getSelectedTab(), TerminalStorageTabIngredientComponentItemStackCrafting.NAME.toString())) {
            ITerminalStorageTabCommon tabCommon = container.getTabCommon(container.getSelectedTab());
            TerminalStorageTabIngredientComponentItemStackCraftingCommon tabCommonCrafting =
                    (TerminalStorageTabIngredientComponentItemStackCraftingCommon) tabCommon;

            if (!doTransfer) {
                // Check in the player inventory and local client view if the required recipe ingredients are available

                // Build crafting grid index
                IIngredientCollectionMutable<ItemStack, Integer> hayStackCraftingGrid = new IngredientCollectionPrototypeMap<>(IngredientComponent.ITEMSTACK);
                for (int slot = 0; slot < tabCommonCrafting.getInventoryCrafting().getSizeInventory(); slot++) {
                    hayStackCraftingGrid.add(tabCommonCrafting.getInventoryCrafting().getStackInSlot(slot));
                }

                // Build player inventory index
                IIngredientCollectionMutable<ItemStack, Integer> hayStackPlayer = new IngredientCollectionPrototypeMap<>(IngredientComponent.ITEMSTACK);
                hayStackPlayer.addAll(player.inventory.mainInventory);

                // Build local client view of storage
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
                                // First check in the crafting grid
                                if (hayStackCraftingGrid.contains(itemStack, ItemMatch.ITEM | ItemMatch.NBT)) {
                                    hayStackPlayer.remove(itemStack);
                                    found = true;
                                    break;
                                }

                                // Then check in player inventory
                                if (hayStackPlayer.contains(itemStack, ItemMatch.ITEM | ItemMatch.NBT)) {
                                    hayStackPlayer.remove(itemStack);
                                    found = true;
                                    break;
                                }

                                // Then check the storage
                                if (hayStack.contains(itemStack, ItemMatch.ITEM | ItemMatch.NBT)) {
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
                IngredientComponentStorageWrapperHandlerItemStack.ComponentStorageWrapper playerInventory =
                        new IngredientComponentStorageWrapperHandlerItemStack.ComponentStorageWrapper(IngredientComponent.ITEMSTACK, new InvWrapper(player.inventory));

                // Send a packet to the server if the recipe effectively needs to be applied to the grid
                Map<Integer, ItemStack> slottedIngredientsFromPlayer = Maps.newHashMap();
                Map<Integer, List<ItemStack>> slottedIngredientsFromStorage = Maps.newHashMap();
                int slotOffset = tabCommonCrafting.getSlotCrafting().slotNumber;
                for (Map.Entry<Integer, ? extends IGuiIngredient<ItemStack>> entry : recipeLayout.getItemStacks().getGuiIngredients().entrySet()) {
                    IGuiIngredient<ItemStack> ingredient = entry.getValue();
                    if (ingredient != null && ingredient.isInput()) {
                        int slotId = entry.getKey();
                        boolean found = false;

                        // First check if we can transfer from the player inventory
                        // No need to check the crafting grid, as the server will first clear the grid into the storage in TerminalStorageIngredientItemStackCraftingGridSetRecipe
                        for (ItemStack itemStack : ingredient.getAllIngredients()) {
                            if (!playerInventory.extract(itemStack, ItemMatch.ITEM | ItemMatch.NBT, true).isEmpty()) {
                                found = true;

                                // Move from player to crafting grid
                                ItemStack extracted = playerInventory.extract(itemStack, ItemMatch.ITEM | ItemMatch.NBT, false);
                                Slot slot = container.getSlot(slotId + slotOffset);
                                slot.putStack(extracted);

                                // Do the exact same thing server-side
                                slottedIngredientsFromPlayer.put(slotId, itemStack);

                                break;
                            }
                        }

                        if (!found) {
                            // Otherwise, request them from the storage
                            slottedIngredientsFromStorage.put(slotId, ingredient.getAllIngredients());
                        }
                    }
                }

                IntegratedTerminalsCompat._instance.getPacketHandler().sendToServer(
                        new TerminalStorageIngredientItemStackCraftingGridSetRecipe(container.getSelectedTab(),
                                container.getSelectedChannel(), maxTransfer, slottedIngredientsFromPlayer, slottedIngredientsFromStorage));
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
        public void showError(MatrixStack matrixStack, int i, int i1, IRecipeLayout iRecipeLayout, int i2, int i3) {
            // Silently fail
        }
    }
}
