package org.cyclops.integratedterminalscompat.network.packet;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.cyclops.integratedterminalscompat.client.gui.toast.CraftingJobToast;

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
        CraftingJobToast.Type toastType;
        ItemStack icon;
        Component title;
        Component subtitle;

        if (failedIngredientNames.isEmpty()) {
            toastType = CraftingJobToast.Type.SUCCESS;
            icon = new ItemStack(Items.CRAFTING_TABLE);
            title = Component.translatable(
                            "gui.integratedterminalscompat.terminal_storage.crafting_job.summary.title.started_only")
                    .withStyle(ChatFormatting.GREEN);
            subtitle = Component.translatable(
                    "gui.integratedterminalscompat.terminal_storage.crafting_job.summary.started_only",
                    joinIngredients(startedIngredientNames));
        } else if (startedIngredientNames.isEmpty()) {
            toastType = CraftingJobToast.Type.FAILURE;
            icon = new ItemStack(Items.BARRIER);
            title = Component.translatable(
                            "gui.integratedterminalscompat.terminal_storage.crafting_job.summary.title.failed_only")
                    .withStyle(ChatFormatting.RED);
            subtitle = Component.translatable(
                    "gui.integratedterminalscompat.terminal_storage.crafting_job.summary.failed_only",
                    joinIngredients(failedIngredientNames));
        } else {
            toastType = CraftingJobToast.Type.MIXED;
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
        CraftingJobToast existing = toastManager.getToast(CraftingJobToast.class, toastType);
        if (existing != null) {
            existing.reset(title, subtitle);
        } else {
            toastManager.addToast(new CraftingJobToast(toastType, icon, title, subtitle));
        }
    }

}
