package org.cyclops.integratedterminalscompat.modcompat.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.JEIPlugin;
import mezz.jei.api.recipe.VanillaRecipeCategoryUid;
import net.minecraft.item.ItemStack;
import org.cyclops.integratedterminals.client.gui.container.GuiTerminalStorage;
import org.cyclops.integratedterminals.part.PartTypes;
import org.cyclops.integratedterminalscompat.modcompat.jei.terminalstorage.TerminalStorageRecipeTransferHandler;

import javax.annotation.Nonnull;

/**
 * Helper for registering JEI manager.
 * @author rubensworks
 *
 */
@JEIPlugin
public class JEIIntegratedTerminalsConfig implements IModPlugin {

    @Override
    public void register(@Nonnull IModRegistry registry) {
        if (JEIModCompat.canBeUsed) {
            // Storage terminal click handler
            registry.addRecipeClickArea(GuiTerminalStorage.class, 86, 76, 22, 15, VanillaRecipeCategoryUid.CRAFTING);
            registry.getRecipeTransferRegistry().addUniversalRecipeTransferHandler(new TerminalStorageRecipeTransferHandler(registry.getJeiHelpers().recipeTransferHandlerHelper()));
            registry.addRecipeCatalyst(new ItemStack(PartTypes.TERMINAL_STORAGE.getItem()), VanillaRecipeCategoryUid.CRAFTING);
        }
    }

}
