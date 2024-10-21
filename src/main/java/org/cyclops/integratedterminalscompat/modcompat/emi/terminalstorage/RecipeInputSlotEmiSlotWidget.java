package org.cyclops.integratedterminalscompat.modcompat.emi.terminalstorage;

import com.google.common.collect.Lists;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.SlotWidget;
import net.minecraft.world.item.ItemStack;
import org.cyclops.integratedterminalscompat.modcompat.common.RecipeInputSlot;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;

/**
 * @author rubensworks
 */
public class RecipeInputSlotEmiSlotWidget implements RecipeInputSlot {

    private final SlotWidget slotWidget;

    public RecipeInputSlotEmiSlotWidget(SlotWidget slotWidget) {
        this.slotWidget = slotWidget;
    }

    public SlotWidget getSlotWidget() {
        return slotWidget;
    }

    @NotNull
    @Override
    public Iterator<ItemStack> iterator() {
        if (slotWidget.getRecipe() != null) {
            return Lists.<ItemStack>newArrayList().iterator();
        }
        return slotWidget.getStack().getEmiStacks().stream()
                .map(EmiStack::getItemStack)
                .filter(stack -> !stack.isEmpty())
                .iterator();
    }

    @Override
    public boolean isEmpty() {
        return slotWidget.getRecipe() != null || slotWidget.getStack().isEmpty();
    }
}
