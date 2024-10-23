package org.cyclops.integratedterminalscompat.modcompat.emi.terminalstorage;

import dev.emi.emi.api.EmiStackProvider;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.stack.EmiStackInteraction;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import org.cyclops.integratedterminals.client.gui.container.ContainerScreenTerminalStorage;
import org.cyclops.integratedterminals.core.terminalstorage.TerminalStorageTabIngredientComponentClient;

import java.util.Optional;

/**
 * @author rubensworks
 */
public class TerminalStorageEmiStackProvider implements EmiStackProvider<Screen> {
    @Override
    public EmiStackInteraction getStackAt(Screen screen, int mouseX, int mouseY) {
        if (screen instanceof ContainerScreenTerminalStorage containerScreen) {
            return createClickableIngredient(containerScreen, mouseX, mouseY);
        }
        return EmiStackInteraction.EMPTY;
    }

    private <T> EmiStackInteraction createClickableIngredient(
            ContainerScreenTerminalStorage<T, ?> containerScreen, int mouseX, int mouseY) {
        int slotIndex = containerScreen.getStorageSlotIndexAtPosition(mouseX, mouseY);
        @SuppressWarnings("unchecked") // Cast is safe due to filter
        Optional<TerminalStorageTabIngredientComponentClient<T, ?>> tabOptional = containerScreen.getSelectedClientTab()
                .filter(TerminalStorageTabIngredientComponentClient.class::isInstance)
                .map(TerminalStorageTabIngredientComponentClient.class::cast);
        if(slotIndex >= 0 && tabOptional.isPresent()) {
            TerminalStorageTabIngredientComponentClient<T, ?> tab = tabOptional.get();
            int channel = containerScreen.getMenu().getSelectedChannel();
            Optional<T> instanceOptional = tab.getSlotInstance(channel, slotIndex);
            return instanceOptional
                    .map(instance -> new EmiStackInteraction(instance instanceof ItemStack itemStack ? EmiStack.of(itemStack) : (instance instanceof FluidStack fluidStack ? EmiStack.of(fluidStack.getFluid(), fluidStack.getAmount()) : EmiStack.EMPTY), null, false))
                    .orElse(EmiStackInteraction.EMPTY);
        }
        return EmiStackInteraction.EMPTY;
    }
}
