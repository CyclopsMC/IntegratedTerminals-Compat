package org.cyclops.integratedterminalscompat.modcompat.common;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.cyclops.cyclopscore.helper.Helpers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author rubensworks
 */
public class RecipeTransferResult<T extends RecipeInputSlot> {

    public static final boolean HAS_CMD = System.getProperty("os.name").equals("Mac OS X");

    public static final int SLOT_COLOR_MISSING = Helpers.RGBAToInt(255, 0, 0, 100);
    public static final int SLOT_COLOR_CRAFTABLE = Helpers.RGBAToInt(0, 0, 255, 100);

    public static final int HIGHLIGHT_COLOR_FAIL = Helpers.RGBAToInt(255, 0, 0, 100);
    public static final int HIGHLIGHT_COLOR_CRAFTABLE = Helpers.RGBAToInt(0, 0, 255, 100);
    public static final int HIGHLIGHT_COLOR_CRAFTABLE_PARTIAL = Helpers.RGBAToInt(255, 125, 0, 100);

    private final List<Component> message = new ArrayList<>();
    private final Collection<T> slotsMissing;
    private final Collection<T> slotsCraftable;
    private final int color;

    public RecipeTransferResult(Collection<T> slotsMissing, Collection<T> slotsCraftable) {
        this.slotsMissing = slotsMissing;
        this.slotsCraftable = slotsCraftable;
        if (slotsCraftable.isEmpty() && slotsMissing.isEmpty()) {
            this.color = HIGHLIGHT_COLOR_CRAFTABLE;
        } else if (slotsMissing.isEmpty()) {
            // Missing items, but they are all craftable
            this.message.add(Component.translatable("gui.integratedterminalscompat.terminal_storage.jei.transfer.craftable").withStyle(ChatFormatting.RED));
            this.message.add(Component.translatable("gui.integratedterminalscompat.terminal_storage.jei.transfer.craft.info").withStyle(ChatFormatting.ITALIC));
            this.color = HIGHLIGHT_COLOR_CRAFTABLE;
        } else if (!slotsCraftable.isEmpty()) {
            // Missing items, but only some of them are craftable
            this.message.add(Component.translatable("gui.integratedterminalscompat.terminal_storage.jei.transfer.craftable_partial").withStyle(ChatFormatting.RED));
            this.message.add(Component.translatable(HAS_CMD ? "gui.integratedterminalscompat.terminal_storage.jei.transfer.craft.info_cmd" : "gui.integratedterminalscompat.terminal_storage.jei.transfer.craft.info").withStyle(ChatFormatting.ITALIC));
            this.color = HIGHLIGHT_COLOR_CRAFTABLE_PARTIAL;
        } else {
            // Missing items, and none are craftable
            this.message.add(Component.translatable("gui.integratedterminalscompat.terminal_storage.jei.transfer.missing").withStyle(ChatFormatting.ITALIC));
            this.color = HIGHLIGHT_COLOR_FAIL;
        }
    }

    public int getButtonHighlightColor() {
        return this.color;
    }

    public List<Component> getMessage() {
        return message;
    }

    public Collection<T> getSlotsCraftable() {
        return slotsCraftable;
    }

    public Collection<T> getSlotsMissing() {
        return slotsMissing;
    }

}
