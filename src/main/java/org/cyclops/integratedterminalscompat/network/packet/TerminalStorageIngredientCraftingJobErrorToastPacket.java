package org.cyclops.integratedterminalscompat.network.packet;

import com.google.common.collect.Lists;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.cyclops.commoncapabilities.api.ingredient.IngredientComponent;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.cyclops.cyclopscore.network.PacketCodec;
import org.cyclops.integratedterminalscompat.Reference;
import org.cyclops.integratedterminals.client.gui.toast.CraftingJobToast;

import java.util.List;

/**
 * Packet for showing a crafting job summary toast on the client.
 * @author rubensworks
 */
public class TerminalStorageIngredientCraftingJobErrorToastPacket extends PacketCodec<TerminalStorageIngredientCraftingJobErrorToastPacket> {

    public static final Type<TerminalStorageIngredientCraftingJobErrorToastPacket> ID = new Type<>(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "terminal_storage_ingredient_crafting_job_error_toast"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TerminalStorageIngredientCraftingJobErrorToastPacket> CODEC = getCodec(TerminalStorageIngredientCraftingJobErrorToastPacket::new);

    private List<String> startedIngredientNames;
    private List<String> failedIngredientNames;

    public TerminalStorageIngredientCraftingJobErrorToastPacket() {
        super(ID);
    }

    public TerminalStorageIngredientCraftingJobErrorToastPacket(List<String> startedIngredientNames, List<String> failedIngredientNames) {
        super(ID);
        this.startedIngredientNames = startedIngredientNames;
        this.failedIngredientNames = failedIngredientNames;
    }

    private static void writeStringList(RegistryFriendlyByteBuf output, List<String> list) {
        output.writeInt(list.size());
        for (String s : list) {
            output.writeUtf(s);
        }
    }

    private static List<String> readStringList(RegistryFriendlyByteBuf input) {
        int count = input.readInt();
        List<String> list = Lists.newArrayListWithExpectedSize(count);
        for (int i = 0; i < count; i++) {
            list.add(input.readUtf());
        }
        return list;
    }

    @Override
    public void encode(RegistryFriendlyByteBuf output) {
        super.encode(output);
        writeStringList(output, startedIngredientNames);
        writeStringList(output, failedIngredientNames);
    }

    @Override
    public void decode(RegistryFriendlyByteBuf input) {
        super.decode(input);
        this.startedIngredientNames = readStringList(input);
        this.failedIngredientNames = readStringList(input);
    }

    @Override
    public boolean isAsync() {
        return false;
    }

    @OnlyIn(Dist.CLIENT)
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

    @Override
    @OnlyIn(Dist.CLIENT)
    public void actionClient(Level world, Player player) {
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
        var toastManager = Minecraft.getInstance().getToasts();
        CraftingJobToast<ItemStack, Integer> existing = (CraftingJobToast<ItemStack, Integer>)
                toastManager.getToast(CraftingJobToast.class, toastType);
        if (existing != null) {
            existing.reset(icon, title, subtitle);
        } else {
            toastManager.addToast(new CraftingJobToast<>(toastType, IngredientComponent.ITEMSTACK, icon,
                    title, subtitle));
        }
    }

    @Override
    public void actionServer(Level world, ServerPlayer player) {
        // Server-to-client only packet
    }

    /**
     * The toast slot that a summary is shown in, so that summaries of the same kind replace each other.
     */
    private enum ToastType {
        SUCCESS, FAILURE, MIXED
    }

}
