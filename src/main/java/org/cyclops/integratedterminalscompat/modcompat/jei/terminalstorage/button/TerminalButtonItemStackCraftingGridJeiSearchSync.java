package org.cyclops.integratedterminalscompat.modcompat.jei.terminalstorage.button;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.TooltipFlag;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.cyclops.cyclopscore.client.gui.component.button.ButtonImage;
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
            CompoundTag data = (CompoundTag) state.getButton(clientTab.getTabSettingsName().toString(), this.buttonName);
            this.active = data.getBoolean("active");
        } else {
            this.active = false;
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public ButtonImage createButton(int x, int y) {
        return new ButtonImage(x, y,
                Component.translatable("gui.integratedterminalscompat.terminal_storage.craftinggrid.jeisync"),
                (b) -> {},
                active ? Images.BUTTON_BACKGROUND_ACTIVE : Images.BUTTON_BACKGROUND_INACTIVE,
                Images.BUTTON_MIDDLE_JEI_SYNC);
    }

    @Override
    public void onClick(TerminalStorageTabIngredientComponentClient<?, ?> clientTab, @Nullable TerminalStorageTabIngredientComponentCommon<?, ?> commonTab, ButtonImage guiButton, int channel, int mouseButton) {
        this.active = !this.active;

        CompoundTag data = new CompoundTag();
        data.putBoolean("active", active);
        state.setButton(clientTab.getTabSettingsName().toString(), this.buttonName, data);
    }

    @Override
    public String getTranslationKey() {
        return "gui.integratedterminalscompat.terminal_storage.craftinggrid.jeisync";
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void getTooltip(Player player, TooltipFlag tooltipFlag, List<Component> lines) {
        lines.add(Component.translatable("gui.integratedterminalscompat.terminal_storage.craftinggrid.jeisync.info").withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable(
                active ? "general.cyclopscore.info.enabled" : "general.cyclopscore.info.disabled")
                .withStyle(ChatFormatting.ITALIC));
    }

    public boolean isActive() {
        return active;
    }
}
