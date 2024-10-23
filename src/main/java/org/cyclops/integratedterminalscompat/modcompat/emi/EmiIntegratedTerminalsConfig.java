package org.cyclops.integratedterminalscompat.modcompat.emi;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.stack.Comparison;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.registry.EmiComparisonDefaults;
import dev.emi.emi.screen.EmiScreenManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.cyclops.commoncapabilities.api.capability.itemhandler.ItemMatch;
import org.cyclops.cyclopscore.client.gui.component.input.WidgetTextFieldExtended;
import org.cyclops.integratedterminals.RegistryEntries;
import org.cyclops.integratedterminals.api.terminalstorage.event.TerminalStorageScreenSizeEvent;
import org.cyclops.integratedterminals.api.terminalstorage.event.TerminalStorageTabClientLoadButtonsEvent;
import org.cyclops.integratedterminals.api.terminalstorage.event.TerminalStorageTabClientSearchFieldUpdateEvent;
import org.cyclops.integratedterminals.client.gui.container.ContainerScreenTerminalStorage;
import org.cyclops.integratedterminals.client.gui.image.Images;
import org.cyclops.integratedterminals.part.PartTypes;
import org.cyclops.integratedterminalscompat.modcompat.common.button.TerminalButtonItemStackCraftingGridSearchSync;
import org.cyclops.integratedterminalscompat.modcompat.emi.terminalstorage.TerminalStorageEmiRecipeHandler;
import org.cyclops.integratedterminalscompat.modcompat.emi.terminalstorage.TerminalStorageEmiStackProvider;

/**
 * @author rubensworks
 */
@EmiEntrypoint
public class EmiIntegratedTerminalsConfig implements EmiPlugin {

    private boolean loaded = false;
    private boolean wasEmiVisible = false;

    public EmiIntegratedTerminalsConfig() {
        NeoForge.EVENT_BUS.register(this);
    }

    public static int getItemStackMatchCondition(ItemStack itemStack) {
        Comparison comparison = EmiComparisonDefaults.get(itemStack.getItem());
        return comparison == Comparison.DEFAULT_COMPARISON ? ItemMatch.ITEM : ItemMatch.ITEM | ItemMatch.DATA;
    }

    @Override
    public void register(EmiRegistry emiRegistry) {
        loaded = true;

        emiRegistry.addRecipeHandler(RegistryEntries.CONTAINER_PART_TERMINAL_STORAGE_PART.get(), new TerminalStorageEmiRecipeHandler<>());
        emiRegistry.addRecipeHandler(RegistryEntries.CONTAINER_PART_TERMINAL_STORAGE_ITEM.get(), new TerminalStorageEmiRecipeHandler<>());
        emiRegistry.addGenericStackProvider(new TerminalStorageEmiStackProvider());
        emiRegistry.addWorkstation(VanillaEmiRecipeCategories.CRAFTING, EmiStack.of(new ItemStack(PartTypes.TERMINAL_STORAGE.getItem())));
    }

    @SubscribeEvent
    public void onTerminalStorageButtons(TerminalStorageTabClientLoadButtonsEvent event) {
        if (this.loaded && !event.getButtons().stream()
                .anyMatch((button) -> button instanceof TerminalButtonItemStackCraftingGridSearchSync)) {
            event.getButtons().add(new TerminalButtonItemStackCraftingGridSearchSync(
                    "emi", event.getContainer().getGuiState(), event.getClientTab(), Images.BUTTON_MIDDLE_EMI_SYNC));
        }
    }

    @SubscribeEvent
    public void onTerminalStorageScreenSize(TerminalStorageScreenSizeEvent event) {
        if (this.loaded) {
            try {
                boolean isOpen = !EmiScreenManager.isDisabled();
                boolean wasJeiVisiblePrevious = wasEmiVisible;
                if (isOpen) {
                    wasEmiVisible = true;
                    event.setHeight(event.getHeight() - 20);
                    event.setWidth(event.getWidth() - 170);
                } else {
                    wasEmiVisible = false;
                }

                // Re-init screen if JEI was just made (in)visible
                if (wasJeiVisiblePrevious != wasEmiVisible) {
                    ((ContainerScreenTerminalStorage) Minecraft.getInstance().screen).init();
                }
            } catch (NoClassDefFoundError | ClassCastException e) {
                // Do nothing when we detect some JEI API issues
            }
        }
    }

    @SubscribeEvent
    public void onSearchFieldUpdated(TerminalStorageTabClientSearchFieldUpdateEvent event) {
        // Copy the terminal search box contents into the JEI search box.
        if (!EmiScreenManager.isDisabled() && TerminalButtonItemStackCraftingGridSearchSync.isSearchSynced(event.getClientTab())) {
            EmiScreenManager.search.setValue(event.getSearchString() + "");
        }
    }

    @SubscribeEvent
    public void onKeyTyped(ScreenEvent.KeyReleased.Post event) {
        // Copy the JEI search box contents into the terminal search box.
        if (event.getScreen() instanceof ContainerScreenTerminalStorage) {
            ContainerScreenTerminalStorage<?, ?> gui = ((ContainerScreenTerminalStorage<?, ?>) event.getScreen());
            if (!EmiScreenManager.isDisabled() && EmiScreenManager.search.isFocused()) {
                gui.getSelectedClientTab().ifPresent(tab -> {
                    if (TerminalButtonItemStackCraftingGridSearchSync.isSearchSynced(tab)) {
                        WidgetTextFieldExtended fieldSearch = gui.getFieldSearch();
                        fieldSearch.setValue(EmiScreenManager.search.getValue());
                        tab.setInstanceFilter(gui.getMenu().getSelectedChannel(), fieldSearch.getValue() + "");
                    }
                });
            }
        }
    }
}
