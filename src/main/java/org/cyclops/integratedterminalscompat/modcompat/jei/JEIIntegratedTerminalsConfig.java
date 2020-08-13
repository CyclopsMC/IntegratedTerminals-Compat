package org.cyclops.integratedterminalscompat.modcompat.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaRecipeCategoryUid;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.cyclops.cyclopscore.client.gui.component.input.WidgetTextFieldExtended;
import org.cyclops.integratedterminals.api.terminalstorage.ITerminalButton;
import org.cyclops.integratedterminals.api.terminalstorage.ITerminalStorageTabClient;
import org.cyclops.integratedterminals.api.terminalstorage.event.TerminalStorageTabClientLoadButtonsEvent;
import org.cyclops.integratedterminals.api.terminalstorage.event.TerminalStorageTabClientSearchFieldUpdateEvent;
import org.cyclops.integratedterminals.client.gui.container.ContainerScreenTerminalStorage;
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

    private IJeiRuntime jeiRuntime;

    public JEIIntegratedTerminalsConfig() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        registration.addUniversalRecipeTransferHandler(new TerminalStorageRecipeTransferHandler(registration.getTransferHelper()));
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGuiContainerHandler(ContainerScreenTerminalStorage.class, new TerminalStorageGuiHandler());

        // Removed because otherwise non-crafting tabs will also always have the click area.
        // registration.addRecipeClickArea(ContainerScreenTerminalStorage.class, 86, 76, 22, 15, VanillaRecipeCategoryUid.CRAFTING);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(PartTypes.TERMINAL_STORAGE.getItem()), VanillaRecipeCategoryUid.CRAFTING);
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
    public void onKeyTyped(GuiScreenEvent.KeyboardKeyReleasedEvent.Post event) {
        // Copy the JEI search box contents into the terminal search box.
        if (event.getGui() instanceof ContainerScreenTerminalStorage) {
            ContainerScreenTerminalStorage gui = ((ContainerScreenTerminalStorage) event.getGui());
            if (jeiRuntime.getIngredientListOverlay().hasKeyboardFocus()) {
                gui.getSelectedClientTab().ifPresent(tab -> {
                    if (isSearchSynced(tab)) {
                        WidgetTextFieldExtended fieldSearch = gui.getFieldSearch();
                        fieldSearch.setText(jeiRuntime.getIngredientFilter().getFilterText());
                        tab.setInstanceFilter(gui.getContainer().getSelectedChannel(), fieldSearch.getText() + "");
                    }
                });
            }
        }
    }
}
