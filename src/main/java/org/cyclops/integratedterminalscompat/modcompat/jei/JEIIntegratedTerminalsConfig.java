package org.cyclops.integratedterminalscompat.modcompat.jei;

import mezz.jei.api.*;

import javax.annotation.Nonnull;

/**
 * Helper for registering JEI manager.
 * @author rubensworks
 *
 */
@JEIPlugin
public class JEIIntegratedTerminalsConfig implements IModPlugin {

    @Override
    public void register(@Nonnull IModRegistry registry) {
        if (JEIModCompat.canBeUsed) {
            //TODO
        }
    }

}
