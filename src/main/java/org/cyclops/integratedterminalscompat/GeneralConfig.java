package org.cyclops.integratedterminalscompat;

import net.neoforged.fml.config.ModConfig;
import org.cyclops.cyclopscore.config.ConfigurableProperty;
import org.cyclops.cyclopscore.config.extendedconfig.DummyConfig;

/**
 * A config with general options for this mod.
 * @author rubensworks
 *
 */
public class GeneralConfig extends DummyConfig {

    @ConfigurableProperty(category = "core", comment = "If ingredients that were auto-crafted from a recipe viewer should be moved into the crafting grid once their crafting job completes.", isCommandable = true, configLocation = ModConfig.Type.SERVER)
    public static boolean craftingGridAutoFillOnCraftingJobCompletion = true;

    @ConfigurableProperty(category = "core", comment = "The number of seconds after which the crafting grid stops waiting on ingredients that are being auto-crafted from a recipe viewer.", minimalValue = 1, isCommandable = true, configLocation = ModConfig.Type.SERVER)
    public static int craftingGridAutoFillTimeoutSeconds = 600;

    public GeneralConfig() {
        super(IntegratedTerminalsCompat._instance, "general");
    }

}
