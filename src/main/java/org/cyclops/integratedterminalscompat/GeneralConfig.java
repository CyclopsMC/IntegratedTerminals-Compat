package org.cyclops.integratedterminalscompat;

import org.cyclops.cyclopscore.config.ConfigurablePropertyCommon;
import org.cyclops.cyclopscore.config.ModConfigLocation;
import org.cyclops.cyclopscore.config.extendedconfig.DummyConfigCommon;

/**
 * A config with general options for this mod.
 * @author rubensworks
 *
 */
public class GeneralConfig extends DummyConfigCommon<IntegratedTerminalsCompat> {

    @ConfigurablePropertyCommon(category = "core", comment = "If ingredients that were auto-crafted from a recipe viewer should be moved into the crafting grid once their crafting job completes.", isCommandable = true, configLocation = ModConfigLocation.SERVER)
    public static boolean craftingGridAutoFillOnCraftingJobCompletion = true;

    @ConfigurablePropertyCommon(category = "core", comment = "The number of seconds after which the crafting grid stops waiting on ingredients that are being auto-crafted from a recipe viewer.", minimalValue = 1, isCommandable = true, configLocation = ModConfigLocation.SERVER)
    public static int craftingGridAutoFillTimeoutSeconds = 600;

    public GeneralConfig() {
        super(IntegratedTerminalsCompat._instance, "general");
    }

}
