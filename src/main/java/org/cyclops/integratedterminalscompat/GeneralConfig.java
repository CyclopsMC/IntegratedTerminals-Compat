package org.cyclops.integratedterminalscompat;

import org.cyclops.cyclopscore.config.extendedconfig.DummyConfigCommon;

/**
 * A config with general options for this mod.
 * @author rubensworks
 *
 */
public class GeneralConfig extends DummyConfigCommon<IntegratedTerminalsCompat> {

    public GeneralConfig() {
        super(IntegratedTerminalsCompat._instance, "general");
    }

}
