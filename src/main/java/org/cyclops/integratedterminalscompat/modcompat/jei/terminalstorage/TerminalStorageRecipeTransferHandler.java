package org.cyclops.integratedterminalscompat.modcompat.jei.terminalstorage;

import com.google.common.collect.Lists;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.cyclops.integratedterminals.api.terminalstorage.ITerminalStorageTabCommon;
import org.cyclops.integratedterminals.core.terminalstorage.TerminalStorageTabIngredientComponentClient;
import org.cyclops.integratedterminals.core.terminalstorage.TerminalStorageTabIngredientComponentItemStackCrafting;
import org.cyclops.integratedterminals.core.terminalstorage.TerminalStorageTabIngredientComponentItemStackCraftingCommon;
import org.cyclops.integratedterminals.inventory.container.ContainerTerminalStorageBase;
import org.cyclops.integratedterminalscompat.modcompat.common.RecipeTransferHelpers;
import org.cyclops.integratedterminalscompat.modcompat.jei.JEIIntegratedTerminalsConfig;
import org.cyclops.integratedterminalscompat.modcompat.jei.RecipeInputSlotJei;
import org.cyclops.integratedterminalscompat.modcompat.jei.RecipeTransferErrorTransferResult;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

/**
 * Handles recipe clicking from JEI.
 * @author rubensworks
 */
public class TerminalStorageRecipeTransferHandler<T extends ContainerTerminalStorageBase<?>> implements IRecipeTransferHandler<T, RecipeHolder<CraftingRecipe>> {
    private final Class<T> clazz;
    private final MenuType<T> menuType;
    private int previousChangeId;

    public TerminalStorageRecipeTransferHandler(Class<T> clazz, MenuType<T> menuType) {
        this.clazz = clazz;
        this.menuType = menuType;
    }

    @Override
    public Class<T> getContainerClass() {
        return this.clazz;
    }

    @Override
    public Optional<MenuType<T>> getMenuType() {
        return Optional.of(this.menuType);
    }

    @Override
    public RecipeType<RecipeHolder<CraftingRecipe>> getRecipeType() {
        return RecipeTypes.CRAFTING;
    }

    @Nullable
    @Override
    public IRecipeTransferError transferRecipe(T container, RecipeHolder<CraftingRecipe> recipeHolder, IRecipeSlotsView recipeLayout,
                                               Player player, boolean maxTransfer, boolean doTransfer) {
        if (Objects.equals(container.getSelectedTab(), TerminalStorageTabIngredientComponentItemStackCrafting.NAME.toString())) {
            ITerminalStorageTabCommon tabCommon = container.getTabCommon(container.getSelectedTab());
            TerminalStorageTabIngredientComponentItemStackCraftingCommon tabCommonCrafting =
                    (TerminalStorageTabIngredientComponentItemStackCraftingCommon) tabCommon;

            if (!doTransfer) {
                TerminalStorageTabIngredientComponentClient tabClient = (TerminalStorageTabIngredientComponentClient)
                        container.getTabClient(container.getSelectedTab());
                return getMissingItems(container, recipeHolder.value(), recipeLayout, player, tabCommonCrafting, tabClient)
                        .orElse(null);
            } else {
                RecipeTransferHelpers.transferRecipe(
                        container,
                        getRecipeInputSlots(recipeLayout),
                        player,
                        tabCommonCrafting,
                        JEIIntegratedTerminalsConfig::getItemStackMatchCondition,
                        maxTransfer
                );
                return null;
            }
        }

        return new TransferError();
    }

    private Collection<RecipeInputSlotJei> getRecipeInputSlots(IRecipeSlotsView recipeLayout) {
        Collection<RecipeInputSlotJei> recipeInputSlots = Lists.newArrayList();
        for (IRecipeSlotView slotView : recipeLayout.getSlotViews()) {
            if (slotView.getRole() != RecipeIngredientRole.INPUT || slotView.isEmpty() || slotView.getAllIngredients().findFirst().get().getType() == VanillaTypes.ITEM_STACK) {
                recipeInputSlots.add(new RecipeInputSlotJei(slotView));
            }
        }
        return recipeInputSlots;
    }

    private Optional<IRecipeTransferError> getMissingItems(T container, CraftingRecipe recipe, IRecipeSlotsView recipeLayout, Player player, TerminalStorageTabIngredientComponentItemStackCraftingCommon tabCommonCrafting, TerminalStorageTabIngredientComponentClient<?, ?> tabClient) {
        return RecipeTransferHelpers.getMissingItems(
                recipe,
                container,
                getRecipeInputSlots(recipeLayout),
                player,
                tabCommonCrafting,
                tabClient,
                JEIIntegratedTerminalsConfig::getItemStackMatchCondition,
                () -> previousChangeId,
                id -> previousChangeId = id
        ).map(RecipeTransferErrorTransferResult::new);
    }

    public static class TransferError implements IRecipeTransferError {

        @Override
        public Type getType() {
            return Type.INTERNAL;
        }

        @Override
        public void showError(GuiGraphics guiGraphics, int mouseX, int mouseY, IRecipeSlotsView recipeSlotsView, int recipeX, int recipeY) {
            // Silently fail
        }
    }
}
