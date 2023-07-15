package org.cyclops.integratedterminalscompat.modcompat.jei.terminalstorage;

import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.runtime.IClickableIngredient;
import mezz.jei.api.runtime.IIngredientManager;
import net.minecraft.client.renderer.Rect2i;
import org.cyclops.integratedterminals.client.gui.container.ContainerScreenTerminalStorage;
import org.cyclops.integratedterminals.core.terminalstorage.TerminalStorageTabIngredientComponentClient;
import org.cyclops.integratedterminalscompat.modcompat.jei.ClickableIngredient;
import org.cyclops.integratedterminalscompat.modcompat.jei.JEIIntegratedTerminalsConfig;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * This handler allows JEI to recognise the terminal storage slot contents.
 * @author rubensworks
 * @author Snonky
 */
public class TerminalStorageGuiHandler implements IGuiContainerHandler<ContainerScreenTerminalStorage<?, ?>> {

    @Override
    public @NotNull Optional<IClickableIngredient<?>> getClickableIngredientUnderMouse(
            @NotNull ContainerScreenTerminalStorage<?, ?> containerScreen, double mouseX, double mouseY) {
        Optional<? extends IClickableIngredient<?>> clickableIngredientOptional =
                createClickableIngredient(containerScreen, mouseX,  mouseY);
        return clickableIngredientOptional.map((clickableIngredient) -> (IClickableIngredient<?>) clickableIngredient);
    }

    private <T> Optional<IClickableIngredient<T>> createClickableIngredient(
            ContainerScreenTerminalStorage<T, ?> containerScreen, double mouseX, double mouseY) {
        int slotIndex = containerScreen.getStorageSlotIndexAtPosition(mouseX, mouseY);
        @SuppressWarnings("unchecked") // Cast is safe due to filter
        Optional<TerminalStorageTabIngredientComponentClient<T, ?>> tabOptional = containerScreen.getSelectedClientTab()
                .filter(TerminalStorageTabIngredientComponentClient.class::isInstance)
                .map(TerminalStorageTabIngredientComponentClient.class::cast);
        if(slotIndex >= 0 && tabOptional.isPresent()) {
            TerminalStorageTabIngredientComponentClient<T, ?> tab = tabOptional.get();
            int channel = containerScreen.getMenu().getSelectedChannel();
            IIngredientManager ingredientManager = JEIIntegratedTerminalsConfig.jeiRuntime.getIngredientManager();
            Optional<T> instanceOptional = tab.getSlotInstance(channel, slotIndex);
            Optional<IIngredientType<T>> ingredientTypeOptional = instanceOptional
                    .flatMap(ingredientManager::getIngredientTypeChecked);
            if (instanceOptional.isPresent() && ingredientTypeOptional.isPresent()) {
                Rect2i slotRect = containerScreen.getStorageSlotRect(slotIndex);
                Optional<ITypedIngredient<T>> typedIngredientOptional = ingredientManager
                        .createTypedIngredient(ingredientTypeOptional.get(), instanceOptional.get());
                return typedIngredientOptional
                        .map((typedIngredient) -> new ClickableIngredient<>(typedIngredient, slotRect));
            }
        }
        return Optional.empty();
    }
}
