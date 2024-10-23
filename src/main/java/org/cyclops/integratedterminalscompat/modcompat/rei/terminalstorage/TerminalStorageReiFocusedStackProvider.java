package org.cyclops.integratedterminalscompat.modcompat.rei.terminalstorage;

import dev.architectury.event.CompoundEventResult;
import me.shedaniel.math.Point;
import me.shedaniel.rei.api.client.registry.screen.FocusedStackProvider;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import org.cyclops.integratedterminals.client.gui.container.ContainerScreenTerminalStorage;
import org.cyclops.integratedterminals.core.terminalstorage.TerminalStorageTabIngredientComponentClient;

import java.util.Optional;

/**
 * @author rubensworks
 */
public class TerminalStorageReiFocusedStackProvider implements FocusedStackProvider {
    @Override
    public CompoundEventResult<EntryStack<?>> provide(Screen screen, Point point) {
        if (screen instanceof ContainerScreenTerminalStorage containerScreen) {
            return createClickableIngredient(containerScreen, point.x, point.y);
        }
        return CompoundEventResult.pass();
    }

    private <T> CompoundEventResult<EntryStack<?>> createClickableIngredient(
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
                    .map(instance -> CompoundEventResult.interruptTrue(instance instanceof ItemStack itemStack ? EntryStacks.of(itemStack) : (instance instanceof FluidStack fluidStack ? EntryStacks.of(fluidStack.getFluid(), fluidStack.getAmount()) : EntryStack.empty())))
                    .orElse(CompoundEventResult.pass());
        }
        return CompoundEventResult.pass();
    }
}
