package org.cyclops.integratedterminalscompat.modcompat.common.button;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import org.cyclops.cyclopscore.client.gui.component.button.ButtonImage;
import org.cyclops.integratedterminals.api.terminalstorage.ITerminalButtonClient;
import org.cyclops.integratedterminals.client.gui.image.Images;
import org.cyclops.integratedterminals.core.terminalstorage.TerminalStorageTabIngredientComponentClient;
import org.cyclops.integratedterminals.core.terminalstorage.TerminalStorageTabIngredientComponentCommon;

import javax.annotation.Nullable;

/**
 * @author rubensworks
 */
public class TerminalButtonItemStackCraftingGridSearchSyncClient implements ITerminalButtonClient<TerminalStorageTabIngredientComponentClient<?, ?>, TerminalStorageTabIngredientComponentCommon<?, ?>, ButtonImage> {

    private final TerminalButtonItemStackCraftingGridSearchSync button;

    public TerminalButtonItemStackCraftingGridSearchSyncClient(TerminalButtonItemStackCraftingGridSearchSync button) {
        this.button = button;
    }

    @Override
    public ButtonImage createButton(int x, int y) {
        return new ButtonImage(x, y,
                Component.translatable("gui.integratedterminalscompat.terminal_storage.craftinggrid." + this.button.mod + "sync"),
                (b) -> {},
                this.button.active ? Images.BUTTON_BACKGROUND_ACTIVE : Images.BUTTON_BACKGROUND_INACTIVE,
                this.button.image);
    }

    @Override
    public void onClick(TerminalStorageTabIngredientComponentClient<?, ?> clientTab, @Nullable TerminalStorageTabIngredientComponentCommon<?, ?> commonTab, ButtonImage guiButton, int channel, int mouseButton) {
        this.button.active = !this.button.active;

        CompoundTag data = new CompoundTag();
        data.putBoolean("active", this.button.active);
        this.button.state.setButton(clientTab.getTabSettingsName().toString(), this.button.buttonName, data);
    }

}
