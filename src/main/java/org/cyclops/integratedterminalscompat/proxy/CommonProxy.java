package org.cyclops.integratedterminalscompat.proxy;

import org.cyclops.cyclopscore.init.ModBase;
import org.cyclops.cyclopscore.network.PacketHandler;
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
    public ModBase getMod() {
        return IntegratedTerminalsCompat._instance;
    }

    @Override
    public void registerPacketHandlers(PacketHandler packetHandler) {
        super.registerPacketHandlers(packetHandler);

        packetHandler.register(TerminalStorageIngredientItemStackCraftingGridSetRecipe.ID, TerminalStorageIngredientItemStackCraftingGridSetRecipe.CODEC);
    }
}
