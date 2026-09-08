package org.cyclops.integratedterminalscompat.modcompat.common;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.tuple.Pair;
import org.cyclops.commoncapabilities.api.ingredient.IIngredientMatcher;
import org.cyclops.commoncapabilities.api.ingredient.IPrototypedIngredient;
import org.cyclops.commoncapabilities.api.ingredient.IngredientComponent;
import org.cyclops.commoncapabilities.api.ingredient.storage.IIngredientComponentStorage;
import org.cyclops.integratedterminals.api.terminalstorage.ITerminalStorageTabCommon;
import org.cyclops.integratedterminals.core.terminalstorage.TerminalStorageTabIngredientComponentItemStackCraftingCommon;
import org.cyclops.integratedterminals.core.terminalstorage.TerminalStorageTabIngredientComponentServer;
import org.cyclops.integratedterminals.inventory.container.ContainerTerminalStorageBase;
import org.cyclops.integratedterminalscompat.GeneralConfig;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Server-side bookkeeping for crafting grid slots that are waiting on a crafting job.
 *
 * When auto-crafting is triggered from a recipe viewer, the grid slots whose ingredients could not be
 * taken from storage are remembered here. Once the crafting jobs that were started for them complete,
 * the crafted ingredients are moved into those slots.
 *
 * This intentionally holds no crafting mod-specific types: crafting job ids are only compared by equality
 * to ids obtained from the same crafting handler.
 *
 * A started job is not guaranteed to complete under the id it was started with, as a crafting handler
 * may split it over several crafting interfaces. Completed jobs that produce a pending ingredient are
 * therefore also acted upon, regardless of their id.
 *
 * @author rubensworks
 */
public class CraftingGridAutoFill {

    private static final Map<UUID, PendingFill> PENDING = Maps.newHashMap();

    /**
     * Remember the given pending fill for a player, replacing any previous one.
     */
    public static void register(ServerPlayer player, PendingFill pendingFill) {
        prune(player.level().getGameTime());
        PENDING.put(player.getUUID(), pendingFill);
    }

    public static void clear(UUID playerId) {
        PENDING.remove(playerId);
    }

    @Nullable
    public static PendingFill get(UUID playerId) {
        return PENDING.get(playerId);
    }

    /**
     * Drop all pending fills that have exceeded their deadline.
     */
    public static void prune(long gameTime) {
        PENDING.values().removeIf(pendingFill -> pendingFill.isExpired(gameTime));
    }

    /**
     * Handle the completion of a crafting job for the given player.
     *
     * @param player The player that requested the job.
     * @param craftingJobId The id of the completed job.
     * @param outputs The outputs that the completed job produced.
     */
    public static void onCraftingJobFinished(ServerPlayer player, Object craftingJobId,
                                             List<IPrototypedIngredient<?, ?>> outputs) {
        PendingFill pendingFill = PENDING.get(player.getUUID());
        if (pendingFill == null) {
            return;
        }
        // Evaluate both, so that a started job is always forgotten once it completes
        boolean startedJob = pendingFill.craftingJobIds.remove(craftingJobId);
        if (!startedJob && !pendingFill.producesPendingIngredient(outputs)) {
            return;
        }

        if (!tryFill(player, pendingFill) || pendingFill.isDone()) {
            PENDING.remove(player.getUUID());
        }
    }

    /**
     * Move the ingredients of all still-pending slots from storage into the crafting grid.
     *
     * @return If the pending fill is still applicable, and should be retained for later jobs.
     */
    protected static boolean tryFill(ServerPlayer player, PendingFill pendingFill) {
        if (!(player.containerMenu instanceof ContainerTerminalStorageBase<?> container)
                || player.containerMenu.containerId != pendingFill.containerId) {
            // The terminal was closed, or another gui was opened
            return false;
        }

        ITerminalStorageTabCommon tabCommon = container.getTabCommon(pendingFill.tabId);
        if (!(tabCommon instanceof TerminalStorageTabIngredientComponentItemStackCraftingCommon tabCommonCrafting)
                || !(container.getTabServer(pendingFill.tabId) instanceof TerminalStorageTabIngredientComponentServer)) {
            return false;
        }
        TerminalStorageTabIngredientComponentServer<ItemStack, Integer> tabServerCrafting =
                (TerminalStorageTabIngredientComponentServer<ItemStack, Integer>)
                        container.getTabServer(pendingFill.tabId);

        int slotOffset = tabCommonCrafting.getSlotCrafting().index + 1;
        if (!pendingFill.matchesSnapshot(tabCommonCrafting)) {
            // The player has changed the grid in the meantime, so the recipe we prepared is no longer there
            return false;
        }

        IIngredientComponentStorage<ItemStack, Integer> storage = tabServerCrafting.getIngredientNetwork()
                .getChannel(pendingFill.channel);

        pendingFill.slots.removeIf(pendingSlot -> {
            Slot slot = container.getSlot(pendingSlot.slotId + slotOffset);
            if (slot.hasItem()) {
                // Something else filled this slot already, so accept it as part of the recipe we are waiting on
                pendingFill.gridSnapshot.set(pendingSlot.slotId, slot.getItem().copy());
                return true;
            }
            for (Pair<ItemStack, Integer> alternative : pendingSlot.alternatives) {
                ItemStack extracted = storage.extract(alternative.getLeft(), alternative.getRight(), false);
                if (!extracted.isEmpty()) {
                    slot.set(extracted);
                    pendingFill.gridSnapshot.set(pendingSlot.slotId, extracted.copy());
                    return true;
                }
            }
            return false;
        });

        return true;
    }

    /**
     * The crafting grid slots of one terminal that are waiting on crafting jobs.
     */
    public static class PendingFill {

        private final int containerId;
        private final String tabId;
        private final int channel;
        private final Set<Object> craftingJobIds;
        private final List<PendingSlot> slots;
        private final NonNullList<ItemStack> gridSnapshot;
        private final long deadline;

        public PendingFill(int containerId, String tabId, int channel, Set<Object> craftingJobIds,
                           List<PendingSlot> slots, NonNullList<ItemStack> gridSnapshot, long deadline) {
            this.containerId = containerId;
            this.tabId = tabId;
            this.channel = channel;
            this.craftingJobIds = craftingJobIds;
            this.slots = slots;
            this.gridSnapshot = gridSnapshot;
            this.deadline = deadline;
        }

        public boolean isExpired(long gameTime) {
            return gameTime > this.deadline;
        }

        public boolean isDone() {
            return this.slots.isEmpty();
        }

        /**
         * @return If any of the given outputs can fill one of the pending slots.
         */
        public boolean producesPendingIngredient(List<IPrototypedIngredient<?, ?>> outputs) {
            IIngredientMatcher<ItemStack, Integer> matcher = IngredientComponent.ITEMSTACK.getMatcher();
            for (IPrototypedIngredient<?, ?> output : outputs) {
                if (output.getComponent() != IngredientComponent.ITEMSTACK) {
                    continue;
                }
                ItemStack produced = (ItemStack) output.getPrototype();
                for (PendingSlot pendingSlot : this.slots) {
                    for (Pair<ItemStack, Integer> alternative : pendingSlot.alternatives) {
                        // The produced quantity is unrelated to what the slot needs, so ignore it
                        if (matcher.matches(alternative.getLeft(), produced,
                                alternative.getRight() & matcher.getExactMatchNoQuantityCondition())) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }

        /**
         * @return If all grid slots that we are not waiting on still hold what we placed there.
         */
        public boolean matchesSnapshot(TerminalStorageTabIngredientComponentItemStackCraftingCommon tabCommon) {
            Set<Integer> pendingSlotIds = Sets.newHashSet();
            for (PendingSlot pendingSlot : this.slots) {
                pendingSlotIds.add(pendingSlot.slotId);
            }
            for (int slotId = 0; slotId < this.gridSnapshot.size(); slotId++) {
                if (!pendingSlotIds.contains(slotId)
                        && !ItemStack.matches(this.gridSnapshot.get(slotId),
                        tabCommon.getInventoryCrafting().getItem(slotId))) {
                    return false;
                }
            }
            return true;
        }

        public static NonNullList<ItemStack> snapshotGrid(
                TerminalStorageTabIngredientComponentItemStackCraftingCommon tabCommon) {
            NonNullList<ItemStack> snapshot = NonNullList
                    .withSize(tabCommon.getInventoryCrafting().getContainerSize(), ItemStack.EMPTY);
            for (int slotId = 0; slotId < snapshot.size(); slotId++) {
                snapshot.set(slotId, tabCommon.getInventoryCrafting().getItem(slotId).copy());
            }
            return snapshot;
        }

        public static long newDeadline(ServerPlayer player) {
            return player.level().getGameTime() + (long) GeneralConfig.craftingGridAutoFillTimeoutSeconds * 20;
        }
    }

    /**
     * One crafting grid slot that is waiting on a crafting job, and the ingredients that may fill it.
     */
    public static class PendingSlot {

        private final int slotId;
        private final List<Pair<ItemStack, Integer>> alternatives;

        public PendingSlot(int slotId, List<Pair<ItemStack, Integer>> alternatives) {
            this.slotId = slotId;
            this.alternatives = Lists.newArrayList(alternatives);
        }

        public int getSlotId() {
            return slotId;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof PendingSlot other
                    && other.slotId == this.slotId
                    && Objects.equals(other.alternatives, this.alternatives);
        }

        @Override
        public int hashCode() {
            return Objects.hash(slotId, alternatives);
        }
    }

}
