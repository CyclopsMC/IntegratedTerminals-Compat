package org.cyclops.integratedterminalscompat.proxy;

import org.cyclops.cyclopscore.init.ModBaseNeoForge;
import org.cyclops.cyclopscore.network.IPacketHandler;
import org.cyclops.cyclopscore.proxy.CommonProxyComponent;
import org.cyclops.integratedterminalscompat.IntegratedTerminalsCompat;
import org.cyclops.integratedterminalscompat.network.packet.TerminalStorageIngredientItemStackCraftingGridSetRecipe;

/**
 * Proxy for server and client side.
 * @author rubensworks
 *
 */
public class CommonProxy extends CommonProxyComponent {

    @Override
    public ModBaseNeoForge<?> getMod() {
        return IntegratedTerminalsCompat._instance;
    }

    @Override
    public void registerPackets(IPacketHandler packetHandler) {
        super.registerPackets(packetHandler);

        packetHandler.register(TerminalStorageIngredientItemStackCraftingGridSetRecipe.class, TerminalStorageIngredientItemStackCraftingGridSetRecipe.ID, TerminalStorageIngredientItemStackCraftingGridSetRecipe.CODEC);
    }
}
