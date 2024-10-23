package org.cyclops.integratedterminalscompat.modcompat.emi.terminalstorage;

import com.google.common.collect.Lists;
import dev.emi.emi.api.recipe.EmiCraftingRecipe;
import dev.emi.emi.api.recipe.EmiPlayerInventory;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.handler.EmiCraftContext;
import dev.emi.emi.api.recipe.handler.EmiRecipeHandler;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.Bounds;
import dev.emi.emi.api.widget.RecipeFillButtonWidget;
import dev.emi.emi.api.widget.SlotWidget;
import dev.emi.emi.api.widget.Widget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.tuple.Pair;
import org.cyclops.cyclopscore.datastructure.Wrapper;
import org.cyclops.integratedterminals.core.terminalstorage.TerminalStorageTabIngredientComponentClient;
import org.cyclops.integratedterminals.inventory.container.ContainerTerminalStorageBase;
import org.cyclops.integratedterminalscompat.modcompat.common.RecipeTransferHelpers;
import org.cyclops.integratedterminalscompat.modcompat.common.RecipeTransferResult;
import org.cyclops.integratedterminalscompat.modcompat.emi.EmiIntegratedTerminalsConfig;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author rubensworks
 */
public class TerminalStorageEmiRecipeHandler<T extends ContainerTerminalStorageBase<?>> implements EmiRecipeHandler<T> {

    private int previousChangeId;

    @Override
    public EmiPlayerInventory getInventory(AbstractContainerScreen<T> screen) {
        T container = screen.getMenu();
        return RecipeTransferHelpers.getTabs(container)
                .map(tabs -> {
                    List<TerminalStorageTabIngredientComponentClient.InstanceWithMetadata<ItemStack>> unfilteredIngredients = tabs.getValue()
                            .getUnfilteredIngredientsView(container.getSelectedChannel());
                    return new EmiPlayerInventory(unfilteredIngredients.stream()
                            .filter(ingredient -> ingredient.getCraftingOption() == null)
                            .map(TerminalStorageTabIngredientComponentClient.InstanceWithMetadata::getInstance)
                            .map(EmiStack::of)
                            .collect(Collectors.toList()));
                })
                .orElseGet(() -> new EmiPlayerInventory(Lists.newArrayList()));
    }

    @Override
    public boolean supportsRecipe(EmiRecipe emiRecipe) {
        return emiRecipe instanceof EmiCraftingRecipe;
    }

    @Override
    public boolean canCraft(EmiRecipe emiRecipe, EmiCraftContext<T> emiCraftContext) {
        return true;
    }

    @Override
    public boolean craft(EmiRecipe recipe, EmiCraftContext<T> context) {
        T container = context.getScreen().getMenu();
        RecipeTransferHelpers.getTabs(container)
                .ifPresent(tabs -> {
                    // Determine input slots
                    Collection<RecipeInputSlotEmiIngredient> recipeInputSlots = Lists.newArrayList();
                    for (EmiIngredient input : recipe.getOutputs()) {
                        recipeInputSlots.add(new RecipeInputSlotEmiIngredient(input, false));
                    }
                    for (EmiIngredient input : recipe.getInputs()) {
                        recipeInputSlots.add(new RecipeInputSlotEmiIngredient(input, true));
                    }

                    // Transfer recipe
                    RecipeTransferHelpers.transferRecipe(
                            container,
                            recipeInputSlots,
                            Minecraft.getInstance().player,
                            tabs.getLeft(),
                            EmiIntegratedTerminalsConfig::getItemStackMatchCondition,
                            false
                    );
                });
        return true;
    }

    @Override
    public List<ClientTooltipComponent> getTooltip(EmiRecipe recipe, EmiCraftContext<T> context) {
        T container = context.getScreen().getMenu();
        return RecipeTransferHelpers.getTabs(container)
                .map(tabs -> {
                    // Determine input slots
                    Collection<RecipeInputSlotEmiIngredient> recipeInputSlots = Lists.newArrayList();
                    for (EmiIngredient input : recipe.getInputs()) {
                        recipeInputSlots.add(new RecipeInputSlotEmiIngredient(input, true));
                    }

                    // Calculate missing items
                    return RecipeTransferHelpers.getMissingItems(
                                    Pair.of("tooltip", recipe),
                                    container,
                                    recipeInputSlots,
                                    Minecraft.getInstance().player,
                                    tabs.getLeft(),
                                    tabs.getRight(),
                                    EmiIntegratedTerminalsConfig::getItemStackMatchCondition,
                                    () -> previousChangeId,
                                    id -> previousChangeId = id
                            )
                            .map(result -> result.getMessage().stream().map(c -> ClientTooltipComponent.create(c.getVisualOrderText())).collect(Collectors.toList()))
                            .orElseGet(List::of);
                })
                .orElseGet(List::of);
    }

    @Override
    public void render(EmiRecipe recipe, EmiCraftContext<T> context, List<Widget> widgets, GuiGraphics guiGraphics) {
        T container = context.getScreen().getMenu();
        RecipeTransferHelpers.getTabs(container)
                .ifPresent(tabs -> {
                    // Determine input slots
                    Collection<RecipeInputSlotEmiSlotWidget> recipeInputSlots = Lists.newArrayList();
                    Wrapper<RecipeFillButtonWidget> fillButton = new Wrapper<>();
                    for (Widget widget : widgets) {
                        if (widget instanceof SlotWidget slotWidget) {
                            recipeInputSlots.add(new RecipeInputSlotEmiSlotWidget(slotWidget));
                        }
                        if (widget instanceof RecipeFillButtonWidget fillButtonWidget) {
                            fillButton.set(fillButtonWidget);
                        }
                    }

                    // Calculate missing items
                    RecipeTransferHelpers.getMissingItems(
                            Pair.of("render", recipe),
                            container,
                            recipeInputSlots,
                            Minecraft.getInstance().player,
                            tabs.getLeft(),
                            tabs.getRight(),
                            EmiIntegratedTerminalsConfig::getItemStackMatchCondition,
                            () -> previousChangeId,
                            id -> previousChangeId = id
                    ).ifPresent(transferResult -> {
                        // Render overlay on slots
                        for (RecipeInputSlotEmiSlotWidget slot : transferResult.getSlotsMissing()) {
                            Bounds bounds = slot.getSlotWidget().getBounds();
                            guiGraphics.fill(bounds.x(), bounds.y(), bounds.x() + bounds.width(), bounds.y() + bounds.height(), RecipeTransferResult.SLOT_COLOR_MISSING);
                        }
                        for (RecipeInputSlotEmiSlotWidget slot : transferResult.getSlotsCraftable()) {
                            Bounds bounds = slot.getSlotWidget().getBounds();
                            guiGraphics.fill(bounds.x(), bounds.y(), bounds.x() + bounds.width(), bounds.y() + bounds.height(), RecipeTransferResult.SLOT_COLOR_CRAFTABLE);
                        }

                        // Render overlay on button
                        Bounds bounds = fillButton.get().getBounds();
                        guiGraphics.fill(bounds.x(), bounds.y(), bounds.x() + bounds.width(), bounds.y() + bounds.height(), transferResult.getButtonHighlightColor());
                    });
                });
    }
}
