package org.cyclops.integratedterminalscompat.modcompat.rei;

import me.shedaniel.rei.api.client.REIRuntime;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.screen.ScreenRegistry;
import me.shedaniel.rei.api.client.registry.transfer.TransferHandlerRegistry;
import me.shedaniel.rei.api.common.entry.comparison.ItemComparatorRegistry;
import me.shedaniel.rei.api.common.util.EntryStacks;
import me.shedaniel.rei.forge.REIPluginClient;
import me.shedaniel.rei.plugin.common.BuiltinPlugin;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.cyclops.commoncapabilities.api.capability.itemhandler.ItemMatch;
import org.cyclops.cyclopscore.client.gui.component.input.WidgetTextFieldExtended;
import org.cyclops.integratedterminals.api.terminalstorage.event.TerminalStorageScreenSizeEvent;
import org.cyclops.integratedterminals.api.terminalstorage.event.TerminalStorageTabClientLoadButtonsEvent;
import org.cyclops.integratedterminals.api.terminalstorage.event.TerminalStorageTabClientSearchFieldUpdateEvent;
import org.cyclops.integratedterminals.client.gui.container.ContainerScreenTerminalStorage;
import org.cyclops.integratedterminals.client.gui.image.Images;
import org.cyclops.integratedterminals.part.PartTypes;
import org.cyclops.integratedterminalscompat.modcompat.common.button.TerminalButtonItemStackCraftingGridSearchSync;
import org.cyclops.integratedterminalscompat.modcompat.rei.terminalstorage.TerminalStorageReiFocusedStackProvider;
import org.cyclops.integratedterminalscompat.modcompat.rei.terminalstorage.TerminalStorageReiTransferHandler;

/**
 * @author rubensworks
 */
@REIPluginClient
public class ReiIntegratedTerminalsConfig implements REIClientPlugin {

    private static ItemComparatorRegistry itemComparatorRegistry;

    public static int getItemStackMatchCondition(ItemStack itemStack) {
        // By default, REI ignores NBT when matching items, unless sub type info is set.

        // Ideally, we would have to do a plain item extraction, and filter the items that match the subtype string,
        // but just using the heuristic that the existence of sub type info implies NBT matching seems to work out so far.
        // So if we would run into problems with this, this filtering is what we'd need to do.

        return !itemComparatorRegistry.containsComparator(itemStack.getItem()) ? ItemMatch.ITEM : ItemMatch.ITEM | ItemMatch.DATA;
    }

    private boolean loaded = false;
    private boolean wasReiVisible = false;

    public ReiIntegratedTerminalsConfig() {
        NeoForge.EVENT_BUS.register(this);
    }

    @Override
    public void registerItemComparators(ItemComparatorRegistry registry) {
        itemComparatorRegistry = registry;
    }

    @Override
    public void registerScreens(ScreenRegistry registry) {
        loaded = true;
        registry.registerFocusedStack(new TerminalStorageReiFocusedStackProvider());
    }

    @Override
    public void registerTransferHandlers(TransferHandlerRegistry registry) {
        registry.register(new TerminalStorageReiTransferHandler());
    }

    @Override
    public void registerCategories(CategoryRegistry registry) {
        registry.addWorkstations(BuiltinPlugin.CRAFTING, EntryStacks.of(PartTypes.TERMINAL_STORAGE.getItem()));
    }

    @SubscribeEvent
    public void onTerminalStorageButtons(TerminalStorageTabClientLoadButtonsEvent event) {
        if (this.loaded && !event.getButtons().stream()
                .anyMatch((button) -> button instanceof TerminalButtonItemStackCraftingGridSearchSync)) {
            event.getButtons().add(new TerminalButtonItemStackCraftingGridSearchSync(
                    "rei", event.getContainer().getGuiState(), event.getClientTab(), Images.BUTTON_MIDDLE_REI_SYNC));
        }
    }

    @SubscribeEvent
    public void onTerminalStorageScreenSize(TerminalStorageScreenSizeEvent event) {
        if (this.loaded) {
            try {
                boolean isOpen = REIRuntime.getInstance().getOverlay().isPresent();
                boolean wasJeiVisiblePrevious = wasReiVisible;
                if (isOpen) {
                    wasReiVisible = true;
                    event.setWidth(event.getWidth() - 170);
                } else {
                    wasReiVisible = false;
                }

                // Re-init screen if JEI was just made (in)visible
                if (wasJeiVisiblePrevious != wasReiVisible) {
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
        if (REIRuntime.getInstance().getOverlay().isPresent() && TerminalButtonItemStackCraftingGridSearchSync.isSearchSynced(event.getClientTab())) {
            REIRuntime.getInstance().getSearchTextField().setText(event.getSearchString() + "");
        }
    }

    @SubscribeEvent
    public void onKeyTyped(ScreenEvent.KeyReleased.Post event) {
        // Copy the JEI search box contents into the terminal search box.
        if (event.getScreen() instanceof ContainerScreenTerminalStorage) {
            ContainerScreenTerminalStorage<?, ?> gui = ((ContainerScreenTerminalStorage<?, ?>) event.getScreen());
            if (REIRuntime.getInstance().getOverlay().isPresent() && REIRuntime.getInstance().getSearchTextField().isFocused()) {
                gui.getSelectedClientTab().ifPresent(tab -> {
                    if (TerminalButtonItemStackCraftingGridSearchSync.isSearchSynced(tab)) {
                        WidgetTextFieldExtended fieldSearch = gui.getFieldSearch();
                        fieldSearch.setValue(REIRuntime.getInstance().getSearchTextField().getText());
                        tab.setInstanceFilter(gui.getMenu().getSelectedChannel(), fieldSearch.getValue() + "");
                    }
                });
            }
        }
    }
}
