package org.cyclops.integratedterminalscompat.modcompat.jei.terminalstorage.button;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.cyclops.cyclopscore.client.gui.component.button.GuiButtonImage;
import org.cyclops.cyclopscore.helper.L10NHelpers;
import org.cyclops.integratedterminals.api.terminalstorage.ITerminalButton;
import org.cyclops.integratedterminals.api.terminalstorage.ITerminalStorageTabClient;
import org.cyclops.integratedterminals.client.gui.image.Images;
import org.cyclops.integratedterminals.core.terminalstorage.TerminalStorageTabIngredientComponentClient;
import org.cyclops.integratedterminals.core.terminalstorage.TerminalStorageTabIngredientComponentCommon;
import org.cyclops.integratedterminals.inventory.container.TerminalStorageState;

import javax.annotation.Nullable;
import java.util.List;

/**
 * A button for toggling JEI search box sync.
 * @author rubensworks
 */
public class TerminalButtonItemStackCraftingGridJeiSearchSync
        implements ITerminalButton<TerminalStorageTabIngredientComponentClient<?, ?>,
        TerminalStorageTabIngredientComponentCommon<?, ?>, GuiButtonImage> {

    private final TerminalStorageState state;
    private final String buttonName;

    private boolean active;

    public TerminalButtonItemStackCraftingGridJeiSearchSync(TerminalStorageState state, ITerminalStorageTabClient<?> clientTab) {
        this.state = state;
        this.buttonName = "itemstack_grid_jeisearchsync";

        if (state.hasButton(clientTab.getName().toString(), this.buttonName)) {
            NBTTagCompound data = (NBTTagCompound) state.getButton(clientTab.getName().toString(), this.buttonName);
            this.active = data.getBoolean("active");
        } else {
            this.active = false;
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public GuiButtonImage createButton(int x, int y) {
        return new GuiButtonImage(0, x, y,
                active ? Images.BUTTON_BACKGROUND_ACTIVE : Images.BUTTON_BACKGROUND_INACTIVE,
                Images.BUTTON_MIDDLE_JEI_SYNC);
    }

    @Override
    public void onClick(TerminalStorageTabIngredientComponentClient<?, ?> clientTab, @Nullable TerminalStorageTabIngredientComponentCommon<?, ?> commonTab, GuiButtonImage guiButton, int channel, int mouseButton) {
        this.active = !this.active;

        NBTTagCompound data = new NBTTagCompound();
        data.setBoolean("active", active);
        state.setButton(clientTab.getName().toString(), this.buttonName, data);
    }

    @Override
    public String getTranslationKey() {
        return "gui.integratedterminalscompat.terminal_storage.craftinggrid.jeisync";
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void getTooltip(EntityPlayer player, ITooltipFlag tooltipFlag, List<String> lines) {
        lines.add(L10NHelpers.localize("gui.integratedterminalscompat.terminal_storage.craftinggrid.jeisync.info"));
        lines.add(TextFormatting.ITALIC + L10NHelpers.localize(
                active ? "general.cyclopscore.info.enabled" : "general.cyclopscore.info.disabled"));
    }

    public boolean isActive() {
        return active;
    }
}
