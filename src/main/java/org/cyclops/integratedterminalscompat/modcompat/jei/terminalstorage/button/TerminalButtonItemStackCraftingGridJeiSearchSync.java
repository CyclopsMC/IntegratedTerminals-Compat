package org.cyclops.integratedterminalscompat.modcompat.jei.terminalstorage.button;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.cyclops.cyclopscore.client.gui.component.button.ButtonImage;
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
        TerminalStorageTabIngredientComponentCommon<?, ?>, ButtonImage> {

    private final TerminalStorageState state;
    private final String buttonName;
    private final ITerminalStorageTabClient<?> clientTab;

    private boolean active;

    public TerminalButtonItemStackCraftingGridJeiSearchSync(TerminalStorageState state, ITerminalStorageTabClient<?> clientTab) {
        this.state = state;
        this.buttonName = "itemstack_grid_jeisearchsync";
        this.clientTab = clientTab;

        reloadFromState();
    }

    @Override
    public void reloadFromState() {
        if (state.hasButton(clientTab.getTabSettingsName().toString(), this.buttonName)) {
            CompoundNBT data = (CompoundNBT) state.getButton(clientTab.getTabSettingsName().toString(), this.buttonName);
            this.active = data.getBoolean("active");
        } else {
            this.active = false;
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public ButtonImage createButton(int x, int y) {
        return new ButtonImage(x, y,
                new TranslationTextComponent("gui.integratedterminalscompat.terminal_storage.craftinggrid.jeisync"),
                (b) -> {},
                active ? Images.BUTTON_BACKGROUND_ACTIVE : Images.BUTTON_BACKGROUND_INACTIVE,
                Images.BUTTON_MIDDLE_JEI_SYNC);
    }

    @Override
    public void onClick(TerminalStorageTabIngredientComponentClient<?, ?> clientTab, @Nullable TerminalStorageTabIngredientComponentCommon<?, ?> commonTab, ButtonImage guiButton, int channel, int mouseButton) {
        this.active = !this.active;

        CompoundNBT data = new CompoundNBT();
        data.putBoolean("active", active);
        state.setButton(clientTab.getTabSettingsName().toString(), this.buttonName, data);
    }

    @Override
    public String getTranslationKey() {
        return "gui.integratedterminalscompat.terminal_storage.craftinggrid.jeisync";
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void getTooltip(PlayerEntity player, ITooltipFlag tooltipFlag, List<ITextComponent> lines) {
        lines.add(new TranslationTextComponent("gui.integratedterminalscompat.terminal_storage.craftinggrid.jeisync.info"));
        lines.add(new TranslationTextComponent(
                active ? "general.cyclopscore.info.enabled" : "general.cyclopscore.info.disabled")
                .withStyle(TextFormatting.ITALIC));
    }

    public boolean isActive() {
        return active;
    }
}
