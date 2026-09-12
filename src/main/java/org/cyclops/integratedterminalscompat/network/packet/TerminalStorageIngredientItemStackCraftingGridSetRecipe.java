package org.cyclops.integratedterminalscompat.network.packet;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;
import org.apache.commons.lang3.tuple.Pair;
import org.cyclops.commoncapabilities.api.ingredient.IngredientComponent;
import org.cyclops.commoncapabilities.api.ingredient.ResourceConverterItem;
import org.cyclops.commoncapabilities.api.ingredient.storage.IIngredientComponentStorage;
import org.cyclops.commoncapabilities.ingredient.storage.IngredientComponentStorageWrapperHandlerResourceHandler;
import org.cyclops.cyclopscore.network.CodecField;
import org.cyclops.cyclopscore.network.PacketCodec;
import org.cyclops.integratedterminals.api.terminalstorage.ITerminalStorageTabCommon;
import org.cyclops.integratedterminals.api.terminalstorage.crafting.CraftingJobStartException;
import org.cyclops.integratedterminals.api.terminalstorage.crafting.ITerminalCraftingOption;
import org.cyclops.integratedterminals.api.terminalstorage.crafting.ITerminalCraftingPlan;
import org.cyclops.integratedterminals.api.terminalstorage.crafting.ITerminalStorageTabIngredientCraftingHandler;
import org.cyclops.integratedterminals.core.terminalstorage.TerminalStorageTabIngredientComponentItemStackCraftingCommon;
import org.cyclops.integratedterminals.core.terminalstorage.TerminalStorageTabIngredientComponentServer;
import org.cyclops.integratedterminals.core.terminalstorage.crafting.TerminalStorageTabIngredientCraftingHandlers;
import org.cyclops.integratedterminals.inventory.container.ContainerTerminalStorageBase;
import org.cyclops.integratedterminals.network.packet.TerminalStorageIngredientItemStackCraftingGridClear;
import org.cyclops.integratedterminalscompat.GeneralConfig;
import org.cyclops.integratedterminalscompat.IntegratedTerminalsCompat;
import org.cyclops.integratedterminalscompat.Reference;
import org.cyclops.integratedterminalscompat.modcompat.common.CraftingGridAutoFill;

import org.cyclops.cyclopscore.ingredient.collection.IIngredientMapMutable;
import org.cyclops.cyclopscore.ingredient.collection.IngredientHashMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Packet for setting the crafting grid recipe and filling it with items.
 * @author rubensworks
 *
 */
public class TerminalStorageIngredientItemStackCraftingGridSetRecipe extends PacketCodec<TerminalStorageIngredientItemStackCraftingGridSetRecipe> {

    public static final Type<TerminalStorageIngredientItemStackCraftingGridSetRecipe> ID = new Type<>(Identifier.fromNamespaceAndPath(Reference.MOD_ID, "terminal_storage_ingredient_itemstack_crafting_grid_set_recipe"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TerminalStorageIngredientItemStackCraftingGridSetRecipe> CODEC = getCodec(TerminalStorageIngredientItemStackCraftingGridSetRecipe::new);

    @CodecField
    private String tabId;
    @CodecField
    private int channel;
    @CodecField
    private boolean maxTransfer;
    private Map<Integer, Pair<ItemStack, Integer>> slottedIngredientsFromPlayer;
    private Map<Integer, List<Pair<ItemStack, Integer>>> slottedIngredientsFromStorage;
    @CodecField
    private boolean triggerCraftingJobs;

    public TerminalStorageIngredientItemStackCraftingGridSetRecipe() {
        super(ID);
    }

    public TerminalStorageIngredientItemStackCraftingGridSetRecipe(String tabId, int channel, boolean maxTransfer,
                                                                   Map<Integer, Pair<ItemStack, Integer>> slottedIngredientsFromPlayer,
                                                                   Map<Integer, List<Pair<ItemStack, Integer>>> slottedIngredientsFromStorage,
                                                                   boolean triggerCraftingJobs) {
        super(ID);
        this.tabId = tabId;
        this.channel = channel;
        this.maxTransfer = maxTransfer;
        this.slottedIngredientsFromPlayer = slottedIngredientsFromPlayer;
        this.slottedIngredientsFromStorage = slottedIngredientsFromStorage;
        this.triggerCraftingJobs = triggerCraftingJobs;
    }

    @Override
    public void encode(RegistryFriendlyByteBuf output) {
        super.encode(output);

        // slottedIngredientsFromPlayer and slottedIngredientsFromStorage are encoded manually for more space efficiency
        output.writeInt(slottedIngredientsFromPlayer.size());
        for (Map.Entry<Integer, Pair<ItemStack, Integer>> entry : slottedIngredientsFromPlayer.entrySet()) {
            output.writeInt(entry.getKey());
            ItemStack.OPTIONAL_STREAM_CODEC.encode(output, entry.getValue().getLeft());
            output.writeInt(entry.getValue().getRight());
        }

        output.writeInt(slottedIngredientsFromStorage.size());
        for (Map.Entry<Integer, List<Pair<ItemStack, Integer>>> entry : slottedIngredientsFromStorage.entrySet()) {
            output.writeInt(entry.getKey());
            output.writeInt(entry.getValue().size());
            for (Pair<ItemStack, Integer> subEntry : entry.getValue()) {
                ItemStack.OPTIONAL_STREAM_CODEC.encode(output, subEntry.getLeft());
                output.writeInt(subEntry.getRight());
            }
        }
    }

    @Override
    public void decode(RegistryFriendlyByteBuf input) {
        super.decode(input);

        // slottedIngredientsFromPlayer and slottedIngredientsFromStorage are encoded manually for more space efficiency
        int entriesSlottedIngredientsFromPlayer = input.readInt();
        this.slottedIngredientsFromPlayer = Maps.newHashMap();
        for (int i = 0; i < entriesSlottedIngredientsFromPlayer; i++) {
            this.slottedIngredientsFromPlayer.put(input.readInt(), Pair.of(ItemStack.OPTIONAL_STREAM_CODEC.decode(input), input.readInt()));
        }

        int entriesSlottedIngredientsFromStorage = input.readInt();
        this.slottedIngredientsFromStorage = Maps.newHashMap();
        for (int i = 0; i < entriesSlottedIngredientsFromStorage; i++) {
            int key = input.readInt();
            int entries = input.readInt();
            List<Pair<ItemStack, Integer>> alternatives = Lists.newArrayListWithExpectedSize(entries);
            for (int j = 0; j < entries; j++) {
                alternatives.add(Pair.of(ItemStack.OPTIONAL_STREAM_CODEC.decode(input), input.readInt()));
            }
            this.slottedIngredientsFromStorage.put(key, alternatives);
        }
    }

    @Override
    public boolean isAsync() {
        return false;
    }

    @Override
    public void actionClient(Level world, Player player) {

    }

    @Override
    public void actionServer(Level world, ServerPlayer player) {
        if(player.containerMenu instanceof ContainerTerminalStorageBase) {
            ContainerTerminalStorageBase<?> container = ((ContainerTerminalStorageBase<?>) player.containerMenu);
            ITerminalStorageTabCommon tabCommon = container.getTabCommon(tabId);
            if (tabCommon instanceof TerminalStorageTabIngredientComponentItemStackCraftingCommon) {
                TerminalStorageTabIngredientComponentServer<ItemStack, Integer> tabServerCrafting =
                        (TerminalStorageTabIngredientComponentServer<ItemStack, Integer>) container.getTabServer(tabId);
                TerminalStorageTabIngredientComponentItemStackCraftingCommon tabCommonCrafting =
                        (TerminalStorageTabIngredientComponentItemStackCraftingCommon) tabCommon;
                int slotOffset = tabCommonCrafting.getSlotCrafting().index + 1;

                // Clear current grid into storage
                TerminalStorageIngredientItemStackCraftingGridClear.clearGrid(tabCommonCrafting, tabServerCrafting,
                        channel, true, player);

                // Try filling the grid with the given recipe

                // Fill from player inventory
                IngredientComponentStorageWrapperHandlerResourceHandler.ComponentStorageWrapper<ItemResource, ItemStack, Integer> playerInventory =
                        new IngredientComponentStorageWrapperHandlerResourceHandler.ComponentStorageWrapper<>(IngredientComponent.ITEMSTACK, VanillaContainerWrapper.of(player.getInventory()), new ResourceConverterItem());
                for (Map.Entry<Integer, Pair<ItemStack, Integer>> entry : this.slottedIngredientsFromPlayer.entrySet()) {
                    Integer matchCondition = entry.getValue().getRight();
                    ItemStack extracted = playerInventory.extract(entry.getValue().getLeft(), matchCondition, false);
                    Slot slot = container.getSlot(entry.getKey() + slotOffset);
                    slot.set(extracted);
                }

                // Fill from storage
                IIngredientComponentStorage<ItemStack, Integer> storage = tabServerCrafting.getIngredientNetwork()
                        .getChannel(channel);
                List<String> startedIngredientNames = new ArrayList<>();
                List<String> failedIngredientNames = new ArrayList<>();

                // Collect slots that couldn't be filled from storage (may need crafting jobs)
                List<Map.Entry<Integer, List<Pair<ItemStack, Integer>>>> slotsNeedingCrafting = new ArrayList<>();

                for (Map.Entry<Integer, List<Pair<ItemStack, Integer>>> entry : this.slottedIngredientsFromStorage.entrySet()) {
                    int slotId = entry.getKey() + slotOffset;
                    Slot slot = container.getSlot(slotId);

                    if (!slot.hasItem()) {
                        ItemStack extracted = ItemStack.EMPTY;
                        for (Pair<ItemStack, Integer> stackEntry : entry.getValue()) {
                            int matchCondition = stackEntry.getRight();
                            extracted = storage.extract(stackEntry.getLeft(), matchCondition, false);
                            if (!extracted.isEmpty()) {
                                break;
                            }
                        }
                        if (!extracted.isEmpty()) {
                            slot.set(extracted);
                        } else if (this.triggerCraftingJobs) {
                            slotsNeedingCrafting.add(entry);
                        }
                    }
                }

                if (this.triggerCraftingJobs && !slotsNeedingCrafting.isEmpty()) {
                    // Group identical items across slots to start a single crafting job per unique item
                    // Keys are ItemStacks normalized to count=1 for identity-based lookup without repeated list scans
                    IIngredientMapMutable<ItemStack, Integer, CraftingJobGroup> craftingGroupsMap = new IngredientHashMap<>(IngredientComponent.ITEMSTACK);

                    for (Map.Entry<Integer, List<Pair<ItemStack, Integer>>> entry : slotsNeedingCrafting) {
                        boolean grouped = false;
                        outer:
                        for (ITerminalStorageTabIngredientCraftingHandler handler : TerminalStorageTabIngredientCraftingHandlers.REGISTRY.getHandlers()) {
                            for (Pair<ItemStack, Integer> stackEntry : entry.getValue()) {
                                for (ITerminalCraftingOption<ItemStack> craftingOption : (Collection<ITerminalCraftingOption<ItemStack>>) handler.getCraftingOptionsWithOutput(tabServerCrafting, channel, stackEntry.getLeft(), stackEntry.getRight())) {
                                    ITerminalCraftingPlan testPlan = handler.calculateCraftingPlan(tabServerCrafting.getNetwork(), channel, craftingOption, 1);
                                    if (testPlan.getStatus().isValid()) {
                                        // Use normalized (count=1) key for O(1) group lookup by item identity
                                        ItemStack normalizedKey = stackEntry.getLeft().copyWithCount(1);
                                        CraftingJobGroup existingGroup = craftingGroupsMap.get(normalizedKey);
                                        if (existingGroup != null) {
                                            existingGroup.totalCount += stackEntry.getLeft().getCount();
                                        } else {
                                            craftingGroupsMap.put(normalizedKey, new CraftingJobGroup(handler, craftingOption, stackEntry.getLeft().getCount(), stackEntry.getLeft()));
                                        }
                                        grouped = true;
                                        break outer;
                                    }
                                }
                            }
                        }
                        if (!grouped) {
                            ItemStack representative = entry.getValue().get(0).getLeft();
                            failedIngredientNames.add(representative.getCount() + "x " + representative.getHoverName().getString());
                        }
                    }

                    // Start one crafting job per group
                    Set<Object> startedCraftingJobIds = new HashSet<>();
                    for (CraftingJobGroup group : craftingGroupsMap.values()) {
                        // Determine how many items a single craft produces, so we only request more
                        // crafts when totalCount actually exceeds the single-craft output quantity.
                        long recipeOutputCount = 0;
                        Iterator<ItemStack> outputs = group.craftingOption.getOutputs();
                        while (outputs.hasNext()) {
                            ItemStack output = outputs.next();
                            if (ItemStack.isSameItemSameComponents(output, group.representative)) {
                                recipeOutputCount += output.getCount();
                            }
                        }
                        long requestedQuantity = (long) Math.ceil((double) group.totalCount / recipeOutputCount);
                        ITerminalCraftingPlan craftingPlan = group.handler.calculateCraftingPlan(tabServerCrafting.getNetwork(), channel, group.craftingOption, requestedQuantity);
                        if (craftingPlan.getStatus().isValid()) {
                            try {
                                group.handler.startCraftingJob(tabServerCrafting.getNetwork(), channel, craftingPlan, player, true);
                                startedCraftingJobIds.add(craftingPlan.getId());
                                startedIngredientNames.add(group.totalCount + "x " + group.representative.getHoverName().getString());
                            } catch (CraftingJobStartException e) {
                                // Ignore jobs that could not start
                                failedIngredientNames.add(group.totalCount + "x " + group.representative.getHoverName().getString());
                            }
                        } else {
                            failedIngredientNames.add(group.totalCount + "x " + group.representative.getHoverName().getString());
                        }
                    }

                    // Remember the slots we could not fill, so that they can be filled once the jobs complete
                    if (GeneralConfig.craftingGridAutoFillOnCraftingJobCompletion && !startedCraftingJobIds.isEmpty()) {
                        List<CraftingGridAutoFill.PendingSlot> pendingSlots = new ArrayList<>();
                        for (Map.Entry<Integer, List<Pair<ItemStack, Integer>>> entry : slotsNeedingCrafting) {
                            if (!container.getSlot(entry.getKey() + slotOffset).hasItem()) {
                                pendingSlots.add(new CraftingGridAutoFill.PendingSlot(entry.getKey(), entry.getValue()));
                            }
                        }
                        if (!pendingSlots.isEmpty()) {
                            CraftingGridAutoFill.register(player, new CraftingGridAutoFill.PendingFill(
                                    container.containerId, tabId, channel, startedCraftingJobIds, pendingSlots,
                                    CraftingGridAutoFill.PendingFill.snapshotGrid(tabCommonCrafting),
                                    CraftingGridAutoFill.PendingFill.newDeadline(player)));
                        }
                    }
                }

                // Notify the client with a summary of crafting jobs
                if (this.triggerCraftingJobs && (!startedIngredientNames.isEmpty() || !failedIngredientNames.isEmpty())) {
                    IntegratedTerminalsCompat._instance.getPacketHandler().sendToPlayer(
                            new TerminalStorageIngredientCraftingJobErrorToastPacket(startedIngredientNames, failedIngredientNames), player);
                }
            }
        }
    }

    private static class CraftingJobGroup {
        final ITerminalStorageTabIngredientCraftingHandler handler;
        final ITerminalCraftingOption<ItemStack> craftingOption;
        int totalCount;
        final ItemStack representative;

        CraftingJobGroup(ITerminalStorageTabIngredientCraftingHandler handler,
                         ITerminalCraftingOption<ItemStack> craftingOption,
                         int totalCount,
                         ItemStack representative) {
            this.handler = handler;
            this.craftingOption = craftingOption;
            this.totalCount = totalCount;
            this.representative = representative;
        }
    }

}
