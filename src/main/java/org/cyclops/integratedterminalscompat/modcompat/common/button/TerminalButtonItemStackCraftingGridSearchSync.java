package org.cyclops.integratedterminalscompat.modcompat.common.button;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.TooltipFlag;
import org.cyclops.cyclopscore.client.gui.component.button.ButtonImage;
import org.cyclops.cyclopscore.client.gui.image.Image;
import org.cyclops.integratedterminals.api.terminalstorage.ITerminalButton;
import org.cyclops.integratedterminals.api.terminalstorage.ITerminalButtonClient;
import org.cyclops.integratedterminals.api.terminalstorage.ITerminalStorageTabClient;
import org.cyclops.integratedterminals.core.terminalstorage.TerminalStorageTabIngredientComponentClient;
import org.cyclops.integratedterminals.core.terminalstorage.TerminalStorageTabIngredientComponentCommon;
import org.cyclops.integratedterminals.inventory.container.TerminalStorageState;

import java.util.List;

/**
 * A button for toggling JEI/EMI search box sync.
 * @author rubensworks
 */
public class TerminalButtonItemStackCraftingGridSearchSync
        implements ITerminalButton<TerminalStorageTabIngredientComponentClient<?, ?>,
        TerminalStorageTabIngredientComponentCommon<?, ?>, ButtonImage> {

    protected final String mod;
    protected final TerminalStorageState state;
    protected final String buttonName;
    protected final ITerminalStorageTabClient<?> clientTab;
    protected final Image image;

    protected boolean active;

    public TerminalButtonItemStackCraftingGridSearchSync(String mod, TerminalStorageState state, ITerminalStorageTabClient<?> clientTab, Image image) {
        this.mod = mod;
        this.state = state;
        this.buttonName = "itemstack_grid_" + mod + "searchsync";
        this.clientTab = clientTab;
        this.image = image;

        reloadFromState();
    }

    @Override
    public ITerminalButtonClient<TerminalStorageTabIngredientComponentClient<?, ?>, TerminalStorageTabIngredientComponentCommon<?, ?>, ButtonImage> getClient() {
        return new TerminalButtonItemStackCraftingGridSearchSyncClient(this);
    }

    @Override
    public void reloadFromState() {
        if (state.hasButton(clientTab.getTabSettingsName().toString(), this.buttonName)) {
            CompoundTag data = (CompoundTag) state.getButton(clientTab.getTabSettingsName().toString(), this.buttonName);
            this.active = data.getBooleanOr("active", false);
        } else {
            this.active = false;
        }
    }

    @Override
    public String getTranslationKey() {
        return "gui.integratedterminalscompat.terminal_storage.craftinggrid." + mod + "sync";
    }

    @Override
    public void getTooltip(Player player, TooltipFlag tooltipFlag, List<Component> lines) {
        lines.add(Component.translatable("gui.integratedterminalscompat.terminal_storage.craftinggrid." + mod + "sync.info").withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable(
                active ? "general.cyclopscore.info.enabled" : "general.cyclopscore.info.disabled")
                .withStyle(ChatFormatting.ITALIC));
    }

    public boolean isActive() {
        return active;
    }

    public static boolean isSearchSynced(ITerminalStorageTabClient<?> clientTab) {
        for (ITerminalButton<?, ?, ?> button : clientTab.getButtons()) {
            if (button instanceof TerminalButtonItemStackCraftingGridSearchSync) {
                return ((TerminalButtonItemStackCraftingGridSearchSync) button).isActive();
            }
        }
        return false;
    }
}
