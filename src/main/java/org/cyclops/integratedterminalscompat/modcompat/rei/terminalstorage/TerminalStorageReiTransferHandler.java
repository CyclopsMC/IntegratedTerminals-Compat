package org.cyclops.integratedterminalscompat.modcompat.rei.terminalstorage;

import com.google.common.collect.Lists;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.widgets.Slot;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.registry.transfer.TransferHandler;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.plugin.common.BuiltinPlugin;
import me.shedaniel.rei.plugin.common.displays.crafting.DefaultCraftingDisplay;
import net.minecraft.network.chat.Component;
import org.cyclops.integratedterminals.api.terminalstorage.ITerminalStorageTabCommon;
import org.cyclops.integratedterminals.core.terminalstorage.TerminalStorageTabIngredientComponentClient;
import org.cyclops.integratedterminals.core.terminalstorage.TerminalStorageTabIngredientComponentItemStackCrafting;
import org.cyclops.integratedterminals.core.terminalstorage.TerminalStorageTabIngredientComponentItemStackCraftingCommon;
import org.cyclops.integratedterminals.inventory.container.ContainerTerminalStorageBase;
import org.cyclops.integratedterminalscompat.modcompat.common.RecipeTransferHelpers;
import org.cyclops.integratedterminalscompat.modcompat.common.RecipeTransferResult;
import org.cyclops.integratedterminalscompat.modcompat.rei.RecipeInputSlotRei;
import org.cyclops.integratedterminalscompat.modcompat.rei.ReiIntegratedTerminalsConfig;

import java.util.Collection;
import java.util.Objects;

/**
 * @author rubensworks
 */
public class TerminalStorageReiTransferHandler implements TransferHandler {

    private int previousChangeId;

    @Override
    public Result handle(Context context) {
        if (context.getDisplay().getCategoryIdentifier().equals(BuiltinPlugin.CRAFTING) &&
                context.getDisplay() instanceof DefaultCraftingDisplay<?> displayCrafting &&
                context.getMenu() instanceof ContainerTerminalStorageBase<?> container &&
                Objects.equals(container.getSelectedTab(), TerminalStorageTabIngredientComponentItemStackCrafting.NAME.toString())) {
            ITerminalStorageTabCommon tabCommon = container.getTabCommon(container.getSelectedTab());
            TerminalStorageTabIngredientComponentItemStackCraftingCommon tabCommonCrafting =
                    (TerminalStorageTabIngredientComponentItemStackCraftingCommon) tabCommon;

            if (context.isActuallyCrafting()) {
                RecipeTransferHelpers.transferRecipe(
                        container,
                        getRecipeInputSlots(displayCrafting),
                        context.getMinecraft().player,
                        tabCommonCrafting,
                        ReiIntegratedTerminalsConfig::getItemStackMatchCondition,
                        context.isStackedCrafting()
                );
                return Result.createSuccessful().blocksFurtherHandling();
            } else {
                TerminalStorageTabIngredientComponentClient tabClient = (TerminalStorageTabIngredientComponentClient)
                        container.getTabClient(container.getSelectedTab());
                return RecipeTransferHelpers.getMissingItems(
                                displayCrafting.getOptionalRecipe().orElse(null),
                                container,
                                getRecipeInputSlots(displayCrafting),
                                context.getMinecraft().player,
                                tabCommonCrafting,
                                tabClient,
                                ReiIntegratedTerminalsConfig::getItemStackMatchCondition,
                                () -> previousChangeId,
                                id -> previousChangeId = id
                        ).map(transferResult -> Result.createSuccessful()
                                .color(transferResult.getButtonHighlightColor())
                                .tooltip(transferResult.getMessage().stream().reduce(Component.empty(), (c1, c2) -> c1.copy().append("\n").append(c2)))
                                .renderer((guiGraphics, mouseX, mouseY, v, widgets, rectangle, display) -> {
                                    int index = 0;
                                    for (Widget widget : widgets) {
                                        if (widget instanceof Slot widgetSlot && widgetSlot.getNoticeMark() == Slot.INPUT) {
                                            // Determine if this slot requires any highlighting
                                            int color = -1;
                                            for (RecipeInputSlotRei slot : transferResult.getSlotsMissing()) {
                                                if (slot.getIndex() == index) {
                                                    color = RecipeTransferResult.SLOT_COLOR_MISSING;
                                                    break;
                                                }
                                            }
                                            for (RecipeInputSlotRei slot : transferResult.getSlotsCraftable()) {
                                                if (slot.getIndex() == index) {
                                                    color = RecipeTransferResult.SLOT_COLOR_CRAFTABLE;
                                                    break;
                                                }
                                            }

                                            // Draw the highlight
                                            if (color != -1) {
                                                Rectangle bounds = widgetSlot.getInnerBounds();
                                                guiGraphics.pose().pushPose();
                                                guiGraphics.pose().translate(0, 0, 20);
                                                guiGraphics.fill(bounds.x, bounds.y, bounds.getMaxX(), bounds.getMaxY(), color);
                                                guiGraphics.pose().popPose();
                                            }

                                            index++;
                                        }
                                    }

                                })
                                .blocksFurtherHandling())
                        .orElse(Result.createSuccessful().blocksFurtherHandling());
            }
        }
        return Result.createNotApplicable();
    }

    private Collection<RecipeInputSlotRei> getRecipeInputSlots(DefaultCraftingDisplay<?> context) {
        Collection<RecipeInputSlotRei> recipeInputSlots = Lists.newArrayList();
        for (EntryIngredient outputEntry : context.getOutputEntries()) {
            recipeInputSlots.add(new RecipeInputSlotRei(outputEntry, false, 0));
        }
        int i = 0;
        for (EntryIngredient outputEntry : context.getOrganisedInputEntries(3, 3)) {
            recipeInputSlots.add(new RecipeInputSlotRei(outputEntry, true, i++));
        }
        return recipeInputSlots;
    }
}
