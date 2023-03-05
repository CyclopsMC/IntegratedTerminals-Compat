package org.cyclops.integratedterminalscompat.modcompat.jei.terminalstorage;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.blaze3d.vertex.PoseStack;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraftforge.items.wrapper.InvWrapper;
import org.apache.commons.lang3.tuple.Pair;
import org.cyclops.commoncapabilities.api.ingredient.IngredientComponent;
import org.cyclops.commoncapabilities.ingredient.storage.IngredientComponentStorageWrapperHandlerItemStack;
import org.cyclops.cyclopscore.ingredient.collection.IIngredientCollectionMutable;
import org.cyclops.cyclopscore.ingredient.collection.IngredientCollectionHelpers;
import org.cyclops.cyclopscore.ingredient.collection.IngredientCollectionPrototypeMap;
import org.cyclops.integratedterminals.api.terminalstorage.ITerminalStorageTabCommon;
import org.cyclops.integratedterminals.core.terminalstorage.TerminalStorageTabIngredientComponentClient;
import org.cyclops.integratedterminals.core.terminalstorage.TerminalStorageTabIngredientComponentItemStackCrafting;
import org.cyclops.integratedterminals.core.terminalstorage.TerminalStorageTabIngredientComponentItemStackCraftingCommon;
import org.cyclops.integratedterminals.inventory.container.ContainerTerminalStorageBase;
import org.cyclops.integratedterminalscompat.IntegratedTerminalsCompat;
import org.cyclops.integratedterminalscompat.modcompat.jei.JEIIntegratedTerminalsConfig;
import org.cyclops.integratedterminalscompat.network.packet.TerminalStorageIngredientItemStackCraftingGridSetRecipe;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Handles recipe clicking from JEI.
 * @author rubensworks
 */
public class TerminalStorageRecipeTransferHandler<T extends ContainerTerminalStorageBase<?>> implements IRecipeTransferHandler<T, CraftingRecipe> {

    private final IRecipeTransferHandlerHelper recipeTransferHandlerHelper;
    private final Class<T> clazz;
    private final MenuType<T> menuType;

    private CraftingRecipe lastSimulatedRecipe;
    private long previousChangeId;
    private IRecipeTransferError lastSimulatedError;

    public TerminalStorageRecipeTransferHandler(IRecipeTransferHandlerHelper recipeTransferHandlerHelper, Class<T> clazz, MenuType<T> menuType) {
        this.recipeTransferHandlerHelper = recipeTransferHandlerHelper;
        this.clazz = clazz;
        this.menuType = menuType;
    }

    @Override
    public Class<T> getContainerClass() {
        return this.clazz;
    }

    @Override
    public Optional<MenuType<T>> getMenuType() {
        return Optional.of(this.menuType);
    }

    @Override
    public RecipeType<CraftingRecipe> getRecipeType() {
        return RecipeTypes.CRAFTING;
    }

    @Nullable
    @Override
    public IRecipeTransferError transferRecipe(T container, CraftingRecipe recipe, IRecipeSlotsView recipeLayout,
                                               Player player, boolean maxTransfer, boolean doTransfer) {
        if (Objects.equals(container.getSelectedTab(), TerminalStorageTabIngredientComponentItemStackCrafting.NAME.toString())) {
            ITerminalStorageTabCommon tabCommon = container.getTabCommon(container.getSelectedTab());
            TerminalStorageTabIngredientComponentItemStackCraftingCommon tabCommonCrafting =
                    (TerminalStorageTabIngredientComponentItemStackCraftingCommon) tabCommon;

            if (!doTransfer) {
                TerminalStorageTabIngredientComponentClient tabClient = (TerminalStorageTabIngredientComponentClient)
                        container.getTabClient(container.getSelectedTab());

                // Since this (expensive) method is invoked every tick, we use a cache.
                if (lastSimulatedRecipe == recipe && previousChangeId == tabClient.getLastChangeId()) {
                    return lastSimulatedError;
                }

                // Check in the player inventory and local client view if the required recipe ingredients are available

                // Build crafting grid index
                IIngredientCollectionMutable<ItemStack, Integer> hayStackCraftingGrid = new IngredientCollectionPrototypeMap<>(IngredientComponent.ITEMSTACK);
                for (int slot = 0; slot < tabCommonCrafting.getInventoryCrafting().getContainerSize(); slot++) {
                    hayStackCraftingGrid.add(tabCommonCrafting.getInventoryCrafting().getItem(slot));
                }

                // Build player inventory index
                IIngredientCollectionMutable<ItemStack, Integer> hayStackPlayer = new IngredientCollectionPrototypeMap<>(IngredientComponent.ITEMSTACK);
                hayStackPlayer.addAll(player.getInventory().items);

                // Build local client view of storage
                List<TerminalStorageTabIngredientComponentClient.InstanceWithMetadata<ItemStack>> unfilteredIngredients = tabClient
                        .getUnfilteredIngredientsView(container.getSelectedChannel());
                IIngredientCollectionMutable<ItemStack, Integer> hayStack = IngredientCollectionHelpers.createCollapsedCollection(IngredientComponent.ITEMSTACK);
                hayStack.addAll(unfilteredIngredients
                        .stream()
                        .filter(i -> i.getCraftingOption() == null)
                        .map(TerminalStorageTabIngredientComponentClient.InstanceWithMetadata::getInstance)
                        .collect(Collectors.toList()));

                List<IRecipeSlotView> slotsMissingItems = Lists.newArrayList();
                for (IRecipeSlotView slotView : recipeLayout.getSlotViews()) {
                    if (!slotView.isEmpty() && slotView.getRole() == RecipeIngredientRole.INPUT) {
                        ITypedIngredient<?> typedIngredient = slotView.getAllIngredients().findFirst().get();
                        if (typedIngredient.getType() == VanillaTypes.ITEM_STACK) {
                            boolean found = false;
                            for (ItemStack itemStack : ((Stream<ITypedIngredient<ItemStack>>) (Stream) slotView.getAllIngredients())
                                    .map(ITypedIngredient::getIngredient)
                                    .collect(Collectors.toSet())) {
                                int matchCondition = JEIIntegratedTerminalsConfig.getItemStackMatchCondition(itemStack);

                                // First check in the crafting grid
                                if (hayStackCraftingGrid.contains(itemStack, matchCondition)) {
                                    hayStackPlayer.remove(itemStack);
                                    found = true;
                                    break;
                                }

                                // Then check in player inventory
                                if (hayStackPlayer.contains(itemStack, matchCondition)) {
                                    hayStackPlayer.remove(itemStack);
                                    found = true;
                                    break;
                                }

                                // Then check the storage
                                if (hayStack.contains(itemStack, matchCondition)) {
                                    hayStack.remove(itemStack);
                                    found = true;
                                    break;
                                }
                            }
                            if (!found) {
                                slotsMissingItems.add(slotView);
                            }
                        }
                    }
                }

                lastSimulatedRecipe = recipe;
                previousChangeId = tabClient.getLastChangeId();
                if (!slotsMissingItems.isEmpty()) {
                    Component message = Component.translatable("jei.tooltip.error.recipe.transfer.missing");
                    return lastSimulatedError = recipeTransferHandlerHelper.createUserErrorForMissingSlots(message, slotsMissingItems);
                }

                return lastSimulatedError = null;
            } else {
                IngredientComponentStorageWrapperHandlerItemStack.ComponentStorageWrapper playerInventory =
                        new IngredientComponentStorageWrapperHandlerItemStack.ComponentStorageWrapper(IngredientComponent.ITEMSTACK, new InvWrapper(player.getInventory()));

                // Send a packet to the server if the recipe effectively needs to be applied to the grid
                Map<Integer, Pair<ItemStack, Integer>> slottedIngredientsFromPlayer = Maps.newHashMap();
                Map<Integer, List<Pair<ItemStack, Integer>>> slottedIngredientsFromStorage = Maps.newHashMap();
                int slotOffset = tabCommonCrafting.getSlotCrafting().index;
                int slotId = 0;
                for (IRecipeSlotView slotView : recipeLayout.getSlotViews()) {
                    if (!slotView.isEmpty() && slotView.getRole() == RecipeIngredientRole.INPUT) {
                        ITypedIngredient<?> typedIngredient = slotView.getAllIngredients().findFirst().get();
                        if (typedIngredient.getType() == VanillaTypes.ITEM_STACK) {
                            boolean found = false;

                            // First check if we can transfer from the player inventory
                            // No need to check the crafting grid, as the server will first clear the grid into the storage in TerminalStorageIngredientItemStackCraftingGridSetRecipe
                            Set<ItemStack> allIngredients = ((Stream<ITypedIngredient<ItemStack>>) (Stream) slotView.getAllIngredients())
                                    .map(ITypedIngredient::getIngredient)
                                    .collect(Collectors.toSet());
                            for (ItemStack itemStack : allIngredients) {
                                int matchCondition = JEIIntegratedTerminalsConfig.getItemStackMatchCondition(itemStack);

                                if (!playerInventory.extract(itemStack, matchCondition, true).isEmpty()) {
                                    found = true;

                                    // Move from player to crafting grid
                                    ItemStack extracted = playerInventory.extract(itemStack, matchCondition, false);
                                    Slot slot = container.getSlot(slotId + slotOffset);
                                    slot.set(extracted);

                                    // Do the exact same thing server-side
                                    slottedIngredientsFromPlayer.put(slotId, Pair.of(itemStack, JEIIntegratedTerminalsConfig.getItemStackMatchCondition(itemStack)));

                                    break;
                                }
                            }

                            if (!found) {
                                // Otherwise, request them from the storage
                                slottedIngredientsFromStorage.put(slotId, allIngredients
                                        .stream()
                                        .map(itemStack -> Pair.of(itemStack, JEIIntegratedTerminalsConfig.getItemStackMatchCondition(itemStack)))
                                        .collect(Collectors.toList()));
                            }
                        }
                    }
                    slotId++;
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
        public void showError(PoseStack poseStack, int mouseX, int mouseY, IRecipeSlotsView recipeSlotsView, int recipeX, int recipeY) {
            // Silently fail
        }
    }
}
