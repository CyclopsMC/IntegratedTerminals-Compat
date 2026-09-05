package org.cyclops.integratedterminalscompat.network.packet;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.cyclops.commoncapabilities.api.ingredient.IngredientComponent;
import org.cyclops.integratedterminals.client.gui.toast.CraftingJobToast;

import java.util.List;

/**
 * @author rubensworks
 */
public class TerminalStorageIngredientCraftingJobErrorToastPacketClient {

    private static MutableComponent joinIngredients(List<String> names) {
        MutableComponent result = Component.empty();
        for (int i = 0; i < names.size(); i++) {
            if (i > 0) {
                result.append(Component.literal(", "));
            }
            result.append(Component.literal(names.get(i)));
        }
        return result;
    }

    public static void showCraftingJobToast(List<String> startedIngredientNames, List<String> failedIngredientNames) {
        ToastType toastType;
        ItemStack icon;
        Component title;
        Component subtitle;

        if (failedIngredientNames.isEmpty()) {
            toastType = ToastType.SUCCESS;
            icon = new ItemStack(Items.CRAFTING_TABLE);
            title = Component.translatable(
                            "gui.integratedterminalscompat.terminal_storage.crafting_job.summary.title.started_only")
                    .withStyle(ChatFormatting.GREEN);
            subtitle = Component.translatable(
                    "gui.integratedterminalscompat.terminal_storage.crafting_job.summary.started_only",
                    joinIngredients(startedIngredientNames));
        } else if (startedIngredientNames.isEmpty()) {
            toastType = ToastType.FAILURE;
            icon = new ItemStack(Items.BARRIER);
            title = Component.translatable(
                            "gui.integratedterminalscompat.terminal_storage.crafting_job.summary.title.failed_only")
                    .withStyle(ChatFormatting.RED);
            subtitle = Component.translatable(
                    "gui.integratedterminalscompat.terminal_storage.crafting_job.summary.failed_only",
                    joinIngredients(failedIngredientNames));
        } else {
            toastType = ToastType.MIXED;
            icon = new ItemStack(Items.BELL);
            title = Component.translatable(
                            "gui.integratedterminalscompat.terminal_storage.crafting_job.summary.title.mixed")
                    .withStyle(ChatFormatting.YELLOW);
            subtitle = Component.translatable(
                    "gui.integratedterminalscompat.terminal_storage.crafting_job.summary.mixed",
                    joinIngredients(startedIngredientNames),
                    joinIngredients(failedIngredientNames));
        }

        // Update an existing visible toast of the same type, or add a new one
        var toastManager = Minecraft.getInstance().getToastManager();
        CraftingJobToast<ItemStack, Integer> existing = (CraftingJobToast<ItemStack, Integer>)
                toastManager.getToast(CraftingJobToast.class, toastType);
        if (existing != null) {
            existing.reset(icon, title, subtitle);
        } else {
            toastManager.addToast(new CraftingJobToast<>(toastType, IngredientComponent.ITEMSTACK, icon,
                    title, subtitle));
        }
    }

    /**
     * The toast slot that a summary is shown in, so that summaries of the same kind replace each other.
     */
    private enum ToastType {
        SUCCESS, FAILURE, MIXED
    }

}
