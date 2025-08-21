package org.cyclops.integratedterminalscompat.modcompat.rei;

import me.shedaniel.rei.api.common.entry.comparison.ItemComparatorRegistry;
import me.shedaniel.rei.api.common.plugins.REICommonPlugin;
import me.shedaniel.rei.forge.REIPluginCommon;
import net.minecraft.world.item.ItemStack;
import org.cyclops.commoncapabilities.api.capability.itemhandler.ItemMatch;

/**
 * @author rubensworks
 */
@REIPluginCommon
public class ReiIntegratedTerminalsConfigCommon implements REICommonPlugin {

    private static ItemComparatorRegistry itemComparatorRegistry;

    public static int getItemStackMatchCondition(ItemStack itemStack) {
        // By default, REI ignores NBT when matching items, unless sub type info is set.

        // Ideally, we would have to do a plain item extraction, and filter the items that match the subtype string,
        // but just using the heuristic that the existence of sub type info implies NBT matching seems to work out so far.
        // So if we would run into problems with this, this filtering is what we'd need to do.

        return !itemComparatorRegistry.containsComparator(itemStack.getItem()) ? ItemMatch.ITEM : ItemMatch.ITEM | ItemMatch.DATA;
    }

    @Override
    public void registerItemComparators(ItemComparatorRegistry registry) {
        itemComparatorRegistry = registry;
    }

}
