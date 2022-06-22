package org.cyclops.integratedterminalscompat.modcompat.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.subtypes.ISubtypeManager;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IModIngredientRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.cyclops.commoncapabilities.api.capability.itemhandler.ItemMatch;
import org.cyclops.cyclopscore.client.gui.component.input.WidgetTextFieldExtended;
import org.cyclops.integratedterminals.RegistryEntries;
import org.cyclops.integratedterminals.api.terminalstorage.ITerminalButton;
import org.cyclops.integratedterminals.api.terminalstorage.ITerminalStorageTabClient;
import org.cyclops.integratedterminals.api.terminalstorage.event.TerminalStorageTabClientLoadButtonsEvent;
import org.cyclops.integratedterminals.api.terminalstorage.event.TerminalStorageTabClientSearchFieldUpdateEvent;
import org.cyclops.integratedterminals.client.gui.container.ContainerScreenTerminalStorage;
import org.cyclops.integratedterminals.inventory.container.ContainerTerminalStorageItem;
import org.cyclops.integratedterminals.inventory.container.ContainerTerminalStoragePart;
import org.cyclops.integratedterminals.part.PartTypes;
import org.cyclops.integratedterminalscompat.Reference;
import org.cyclops.integratedterminalscompat.modcompat.jei.terminalstorage.TerminalStorageGuiHandler;
import org.cyclops.integratedterminalscompat.modcompat.jei.terminalstorage.TerminalStorageRecipeTransferHandler;
import org.cyclops.integratedterminalscompat.modcompat.jei.terminalstorage.button.TerminalButtonItemStackCraftingGridJeiSearchSync;

/**
 * Helper for registering JEI manager.
 * @author rubensworks
 *
 */
@JeiPlugin
public class JEIIntegratedTerminalsConfig implements IModPlugin {

    public static ISubtypeManager subTypeManager;

    private IJeiRuntime jeiRuntime;

    public JEIIntegratedTerminalsConfig() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    public static int getItemStackMatchCondition(ItemStack itemStack) {
        // By default, JEI ignores NBT when matching items, unless sub type info is set.

        // Ideally, we would have to do a plain item extraction, and filter the items that match the subtype string,
        // but just using the heuristic that the existence of sub type info implies NBT matching seems to work out so far.
        // So if we would run into problems with this, this filtering is what we'd need to do.

        String subTypeInfo = JEIIntegratedTerminalsConfig.subTypeManager.getSubtypeInfo(VanillaTypes.ITEM_STACK, itemStack, UidContext.Ingredient);
        return subTypeInfo == null ? ItemMatch.ITEM : ItemMatch.ITEM | ItemMatch.TAG;
    }

    @Override
    public void registerIngredients(IModIngredientRegistration registration) {
        subTypeManager = registration.getSubtypeManager();
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        registration.addUniversalRecipeTransferHandler(
                new TerminalStorageRecipeTransferHandler<>(registration.getTransferHelper(),
                        ContainerTerminalStoragePart.class, RegistryEntries.CONTAINER_PART_TERMINAL_STORAGE_PART));
        registration.addUniversalRecipeTransferHandler(
                new TerminalStorageRecipeTransferHandler<>(registration.getTransferHelper(),
                        ContainerTerminalStorageItem.class, RegistryEntries.CONTAINER_PART_TERMINAL_STORAGE_ITEM));
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGenericGuiContainerHandler(ContainerScreenTerminalStorage.class, new TerminalStorageGuiHandler());

        // Removed because otherwise non-crafting tabs will also always have the click area.
        // registration.addRecipeClickArea(ContainerScreenTerminalStorage.class, 86, 76, 22, 15, VanillaRecipeCategoryUid.CRAFTING);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(PartTypes.TERMINAL_STORAGE.getItem()), RecipeTypes.CRAFTING);
    }

    @Override
    public ResourceLocation getPluginUid() {
        return new ResourceLocation(Reference.MOD_ID, "main");
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        this.jeiRuntime = jeiRuntime;
    }

    @SubscribeEvent
    public void onTerminalStorageButtons(TerminalStorageTabClientLoadButtonsEvent event) {
        if (!event.getButtons().stream()
                .anyMatch((button) -> button instanceof TerminalButtonItemStackCraftingGridJeiSearchSync)) {
            event.getButtons().add(new TerminalButtonItemStackCraftingGridJeiSearchSync(
                    event.getContainer().getGuiState(), event.getClientTab()));
        }
    }

    protected boolean isSearchSynced(ITerminalStorageTabClient<?> clientTab) {
        for (ITerminalButton<?, ?, ?> button : clientTab.getButtons()) {
            if (button instanceof TerminalButtonItemStackCraftingGridJeiSearchSync) {
                return ((TerminalButtonItemStackCraftingGridJeiSearchSync) button).isActive();
            }
        }
        return false;
    }

    @SubscribeEvent
    public void onSearchFieldUpdated(TerminalStorageTabClientSearchFieldUpdateEvent event) {
        // Copy the terminal search box contents into the JEI search box.
        if (isSearchSynced(event.getClientTab())) {
            jeiRuntime.getIngredientFilter().setFilterText(event.getSearchString() + "");
        }
    }

    @SubscribeEvent
    public void onKeyTyped(ScreenEvent.KeyboardKeyReleasedEvent.Post event) {
        // Copy the JEI search box contents into the terminal search box.
        if (event.getScreen() instanceof ContainerScreenTerminalStorage) {
            ContainerScreenTerminalStorage<?, ?> gui = ((ContainerScreenTerminalStorage<?, ?>) event.getScreen());
            if (jeiRuntime.getIngredientListOverlay().hasKeyboardFocus()) {
                gui.getSelectedClientTab().ifPresent(tab -> {
                    if (isSearchSynced(tab)) {
                        WidgetTextFieldExtended fieldSearch = gui.getFieldSearch();
                        fieldSearch.setValue(jeiRuntime.getIngredientFilter().getFilterText());
                        tab.setInstanceFilter(gui.getMenu().getSelectedChannel(), fieldSearch.getValue() + "");
                    }
                });
            }
        }
    }
}
