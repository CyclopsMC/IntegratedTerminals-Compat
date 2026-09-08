package org.cyclops.integratedterminalscompat.modcompat.common;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.cyclops.integratedterminals.api.terminalstorage.event.TerminalCraftingJobFinishedEvent;
import org.cyclops.integratedterminalscompat.GeneralConfig;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Moves crafted ingredients into the crafting grid once the crafting jobs that were started
 * from a recipe viewer have completed.
 * @author rubensworks
 */
public class CraftingGridAutoFillListener {

    public static void register() {
        NeoForge.EVENT_BUS.register(CraftingGridAutoFillListener.class);
    }

    @SubscribeEvent
    public static void onCraftingJobFinished(TerminalCraftingJobFinishedEvent event) {
        if (!GeneralConfig.craftingGridAutoFillOnCraftingJobCompletion
                || !event.isRootJob() || event.getInitiator() == null) {
            return;
        }

        ServerPlayer player = getPlayer(event.getInitiator());
        if (player == null) {
            // The initiator is not online, so there is no grid to fill
            CraftingGridAutoFill.clear(event.getInitiator());
            return;
        }

        CraftingGridAutoFill.onCraftingJobFinished(player, event.getCraftingJobId(), event.getOutputs());
    }

    @SubscribeEvent
    public static void onContainerClosed(PlayerContainerEvent.Close event) {
        CraftingGridAutoFill.clear(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        CraftingGridAutoFill.clear(event.getEntity().getUUID());
    }

    @Nullable
    protected static ServerPlayer getPlayer(UUID initiator) {
        return ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(initiator);
    }

}
