package org.cyclops.integratedterminalscompat.modcompat.common;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Streams;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
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
import org.cyclops.integratedterminalscompat.network.packet.TerminalStorageIngredientItemStackCraftingGridSetRecipe;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Common helpers for JEI/EMI/REI
 * @author rubensworks
 */
public class RecipeTransferHelpers {

    // The amount of seconds recipeErrors will be cached for transferRecipe
    private static final long RECIPE_ERROR_CACHE_TIME = 60;
    private static final Cache<Object, Optional<RecipeTransferResult<?>>> recipeErrorCache = CacheBuilder.newBuilder()
            .expireAfterAccess(RECIPE_ERROR_CACHE_TIME, TimeUnit.SECONDS)
            .build();

    public static Optional<Pair<TerminalStorageTabIngredientComponentItemStackCraftingCommon, TerminalStorageTabIngredientComponentClient<ItemStack, Integer>>> getTabs(ContainerTerminalStorageBase<?> container) {
        if (Objects.equals(container.getSelectedTab(), TerminalStorageTabIngredientComponentItemStackCrafting.NAME.toString())) {
            ITerminalStorageTabCommon tabCommon = container.getTabCommon(container.getSelectedTab());
            TerminalStorageTabIngredientComponentItemStackCraftingCommon tabCommonCrafting =
                    (TerminalStorageTabIngredientComponentItemStackCraftingCommon) tabCommon;
            TerminalStorageTabIngredientComponentClient tabClient = (TerminalStorageTabIngredientComponentClient)
                    container.getTabClient(container.getSelectedTab());
            return Optional.of(Pair.of(tabCommonCrafting, tabClient));
        }
        return Optional.empty();
    }

    public static <T extends RecipeInputSlot> Optional<RecipeTransferResult<T>> getMissingItems(Object cacheKey, ContainerTerminalStorageBase<?> container, Iterable<T> recipeInputSlots, Player player, TerminalStorageTabIngredientComponentItemStackCraftingCommon tabCommonCrafting, TerminalStorageTabIngredientComponentClient<?, ?> tabClient, Function<ItemStack, Integer> itemStackToMatchCondition, Supplier<Integer> getId, Consumer<Integer> onChangeId) {
        Callable<Optional<RecipeTransferResult<T>>> missingItemsSupplier =
                () -> getMissingItemsUncached(container, recipeInputSlots, player, tabCommonCrafting, itemStackToMatchCondition, onChangeId);
        if (getId.get() != tabClient.getLastChangeId()) {
            // Clear cache when storage contents changed
            recipeErrorCache.invalidateAll();
        }
        try {
            return recipeErrorCache.get(cacheKey, (Callable) missingItemsSupplier);
        } catch (ExecutionException e) {
            // Throw exceptions from missingItemsSupplier
            throw new RuntimeException(e);
        }
    }

    public static <T extends RecipeInputSlot> Optional<RecipeTransferResult<T>> getMissingItemsUncached(ContainerTerminalStorageBase<?> container, Iterable<T> recipeInputSlots, Player player, TerminalStorageTabIngredientComponentItemStackCraftingCommon tabCommonCrafting, Function<ItemStack, Integer> itemStackToMatchCondition, Consumer<Integer> onChangeId) {
        TerminalStorageTabIngredientComponentClient tabClient = (TerminalStorageTabIngredientComponentClient)
                container.getTabClient(container.getSelectedTab());

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
        IIngredientCollectionMutable<ItemStack, Integer> hayStackCraftable = IngredientCollectionHelpers.createCollapsedCollection(IngredientComponent.ITEMSTACK);
        hayStack.addAll(unfilteredIngredients
                .stream()
                .filter(i -> i.getCraftingOption() == null)
                .map(TerminalStorageTabIngredientComponentClient.InstanceWithMetadata::getInstance)
                .collect(Collectors.toList()));
        hayStackCraftable.addAll(unfilteredIngredients
                .stream()
                .filter(i -> i.getCraftingOption() != null)
                .map(TerminalStorageTabIngredientComponentClient.InstanceWithMetadata::getInstance)
                .collect(Collectors.toList()));

        List<T> slotsMissingItems = Lists.newArrayList();
        List<T> slotsMissingCraftableItems = Lists.newArrayList();
        for (T slot : recipeInputSlots) {
            if (!slot.isEmpty()) {
                boolean found = false;
                boolean craftable = false;
                for (ItemStack itemStack : slot) {
                    int matchCondition = itemStackToMatchCondition.apply(itemStack);

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

                    // Also check if this item could be craftable
                    if (hayStackCraftable.contains(itemStack, matchCondition)) {
                        craftable = true;
                    }
                }

                if (!found) {
                    if (craftable) {
                        slotsMissingCraftableItems.add(slot);
                    } else {
                        slotsMissingItems.add(slot);
                    }
                }
            }
        }

        onChangeId.accept(tabClient.getLastChangeId());
        if (!slotsMissingItems.isEmpty() || !slotsMissingCraftableItems.isEmpty()) {
            return Optional.of(new RecipeTransferResult<T>(slotsMissingItems, slotsMissingCraftableItems));
        }

        return Optional.empty();
    }

    public static <T extends RecipeInputSlot> void transferRecipe(ContainerTerminalStorageBase<?> container, Iterable<T> recipeInputSlots, Player player, TerminalStorageTabIngredientComponentItemStackCraftingCommon tabCommonCrafting, Function<ItemStack, Integer> itemStackToMatchCondition, boolean maxTransfer) {
        IngredientComponentStorageWrapperHandlerItemStack.ComponentStorageWrapper playerInventory =
                new IngredientComponentStorageWrapperHandlerItemStack.ComponentStorageWrapper(IngredientComponent.ITEMSTACK, new InvWrapper(player.getInventory()));

        // Send a packet to the server if the recipe effectively needs to be applied to the grid
        Map<Integer, Pair<ItemStack, Integer>> slottedIngredientsFromPlayer = Maps.newHashMap();
        Map<Integer, List<Pair<ItemStack, Integer>>> slottedIngredientsFromStorage = Maps.newHashMap();
        int slotOffset = tabCommonCrafting.getSlotCrafting().index;
        int slotId = 0;
        for (T recipeSlot : recipeInputSlots) {
            if (!recipeSlot.isEmpty()) {
                boolean found = false;

                // First check if we can transfer from the player inventory
                // No need to check the crafting grid, as the server will first clear the grid into the storage in TerminalStorageIngredientItemStackCraftingGridSetRecipe
                for (ItemStack itemStack : recipeSlot) {
                    int matchCondition = itemStackToMatchCondition.apply(itemStack);

                    if (!playerInventory.extract(itemStack, matchCondition, true).isEmpty()) {
                        found = true;

                        // Move from player to crafting grid
                        ItemStack extracted = playerInventory.extract(itemStack, matchCondition, false);
                        Slot slot = container.getSlot(slotId + slotOffset);
                        slot.set(extracted);

                        // Do the exact same thing server-side
                        slottedIngredientsFromPlayer.put(slotId, Pair.of(itemStack, itemStackToMatchCondition.apply(itemStack)));

                        break;
                    }
                }

                if (!found) {
                    // Otherwise, request them from the storage
                    slottedIngredientsFromStorage.put(slotId, Streams.stream(recipeSlot)
                            .map(itemStack -> Pair.of(itemStack, itemStackToMatchCondition.apply(itemStack)))
                            .collect(Collectors.toList()));
                }
            }
            slotId++;
        }

        IntegratedTerminalsCompat._instance.getPacketHandler().sendToServer(
                new TerminalStorageIngredientItemStackCraftingGridSetRecipe(container.getSelectedTab(),
                        container.getSelectedChannel(), maxTransfer, slottedIngredientsFromPlayer, slottedIngredientsFromStorage, AbstractContainerScreen.hasControlDown()));
    }

}
