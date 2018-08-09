package org.cyclops.integratedterminalscompat.modcompat.jei;

import mezz.jei.api.IJeiRuntime;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.JEIPlugin;
import mezz.jei.api.recipe.VanillaRecipeCategoryUid;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.cyclops.cyclopscore.client.gui.component.input.GuiTextFieldExtended;
import org.cyclops.integratedterminals.api.terminalstorage.ITerminalButton;
import org.cyclops.integratedterminals.api.terminalstorage.ITerminalStorageTabClient;
import org.cyclops.integratedterminals.api.terminalstorage.event.TerminalStorageTabClientLoadButtonsEvent;
import org.cyclops.integratedterminals.api.terminalstorage.event.TerminalStorageTabClientSearchFieldUpdateEvent;
import org.cyclops.integratedterminals.client.gui.container.GuiTerminalStorage;
import org.cyclops.integratedterminals.part.PartTypes;
import org.cyclops.integratedterminalscompat.modcompat.jei.terminalstorage.TerminalStorageAdvancedGuiHandler;
import org.cyclops.integratedterminalscompat.modcompat.jei.terminalstorage.TerminalStorageRecipeTransferHandler;
import org.cyclops.integratedterminalscompat.modcompat.jei.terminalstorage.button.TerminalButtonItemStackCraftingGridJeiSearchSync;

import javax.annotation.Nonnull;

/**
 * Helper for registering JEI manager.
 * @author rubensworks
 *
 */
@JEIPlugin
public class JEIIntegratedTerminalsConfig implements IModPlugin {

    private IJeiRuntime jeiRuntime;

    @Override
    public void register(@Nonnull IModRegistry registry) {
        if (JEIModCompat.canBeUsed) {
            // Storage terminal click handler
            registry.addRecipeClickArea(GuiTerminalStorage.class, 86, 76, 22, 15, VanillaRecipeCategoryUid.CRAFTING);
            registry.getRecipeTransferRegistry().addUniversalRecipeTransferHandler(new TerminalStorageRecipeTransferHandler(registry.getJeiHelpers().recipeTransferHandlerHelper()));
            registry.addAdvancedGuiHandlers(new TerminalStorageAdvancedGuiHandler());
            registry.addRecipeCatalyst(new ItemStack(PartTypes.TERMINAL_STORAGE.getItem()), VanillaRecipeCategoryUid.CRAFTING);

            MinecraftForge.EVENT_BUS.register(this);
        }
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        this.jeiRuntime = jeiRuntime;
    }

    @SubscribeEvent
    public void onTerminalStorageButtons(TerminalStorageTabClientLoadButtonsEvent event) {
        event.getButtons().add(new TerminalButtonItemStackCraftingGridJeiSearchSync());
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
    public void onKeyTyped(GuiScreenEvent.KeyboardInputEvent.Post event) {
        // Copy the JEI search box contents into the terminal search box.
        if (event.getGui() instanceof GuiTerminalStorage) {
            GuiTerminalStorage gui = ((GuiTerminalStorage) event.getGui());
            if (jeiRuntime.getIngredientListOverlay().hasKeyboardFocus()) {
                GuiTextFieldExtended fieldSearch = gui.getFieldSearch();
                fieldSearch.setText(jeiRuntime.getIngredientFilter().getFilterText());
                gui.getSelectedClientTab().ifPresent(tab -> {
                    if (isSearchSynced(tab)) {
                        tab.setInstanceFilter(gui.getContainer().getSelectedChannel(), fieldSearch.getText() + "");
                    }
                });
            }
        }
    }
}
