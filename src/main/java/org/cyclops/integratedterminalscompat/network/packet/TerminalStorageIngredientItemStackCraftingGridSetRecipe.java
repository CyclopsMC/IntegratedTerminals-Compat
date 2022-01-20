package org.cyclops.integratedterminalscompat.network.packet;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.items.wrapper.InvWrapper;
import org.apache.commons.lang3.tuple.Pair;
import org.cyclops.commoncapabilities.api.ingredient.IngredientComponent;
import org.cyclops.commoncapabilities.api.ingredient.storage.IIngredientComponentStorage;
import org.cyclops.commoncapabilities.ingredient.storage.IngredientComponentStorageWrapperHandlerItemStack;
import org.cyclops.cyclopscore.network.CodecField;
import org.cyclops.cyclopscore.network.PacketCodec;
import org.cyclops.integratedterminals.api.terminalstorage.ITerminalStorageTabCommon;
import org.cyclops.integratedterminals.core.terminalstorage.TerminalStorageTabIngredientComponentItemStackCraftingCommon;
import org.cyclops.integratedterminals.core.terminalstorage.TerminalStorageTabIngredientComponentServer;
import org.cyclops.integratedterminals.inventory.container.ContainerTerminalStorageBase;
import org.cyclops.integratedterminals.network.packet.TerminalStorageIngredientItemStackCraftingGridClear;

import java.util.List;
import java.util.Map;

/**
 * Packet for setting the crafting grid recipe and filling it with items.
 * @author rubensworks
 *
 */
public class TerminalStorageIngredientItemStackCraftingGridSetRecipe extends PacketCodec {

    @CodecField
    private String tabId;
    @CodecField
    private int channel;
    @CodecField
    private boolean maxTransfer;
    private Map<Integer, Pair<ItemStack, Integer>> slottedIngredientsFromPlayer;
    private Map<Integer, List<Pair<ItemStack, Integer>>> slottedIngredientsFromStorage;

    public TerminalStorageIngredientItemStackCraftingGridSetRecipe() {

    }

    public TerminalStorageIngredientItemStackCraftingGridSetRecipe(String tabId, int channel, boolean maxTransfer,
                                                                   Map<Integer, Pair<ItemStack, Integer>> slottedIngredientsFromPlayer,
                                                                   Map<Integer, List<Pair<ItemStack, Integer>>> slottedIngredientsFromStorage) {
        this.tabId = tabId;
        this.channel = channel;
        this.maxTransfer = maxTransfer;
        this.slottedIngredientsFromPlayer = slottedIngredientsFromPlayer;
        this.slottedIngredientsFromStorage = slottedIngredientsFromStorage;
    }

    @Override
    public void encode(PacketBuffer output) {
        super.encode(output);

        // slottedIngredientsFromPlayer and slottedIngredientsFromStorage are encoded manually for more space efficiency
        output.writeInt(slottedIngredientsFromPlayer.size());
        for (Map.Entry<Integer, Pair<ItemStack, Integer>> entry : slottedIngredientsFromPlayer.entrySet()) {
            output.writeInt(entry.getKey());
            output.writeItem(entry.getValue().getLeft());
            output.writeInt(entry.getValue().getRight());
        }

        output.writeInt(slottedIngredientsFromStorage.size());
        for (Map.Entry<Integer, List<Pair<ItemStack, Integer>>> entry : slottedIngredientsFromStorage.entrySet()) {
            output.writeInt(entry.getKey());
            output.writeInt(entry.getValue().size());
            for (Pair<ItemStack, Integer> subEntry : entry.getValue()) {
                output.writeItem(subEntry.getLeft());
                output.writeInt(subEntry.getRight());
            }
        }
    }

    @Override
    public void decode(PacketBuffer input) {
        super.decode(input);

        // slottedIngredientsFromPlayer and slottedIngredientsFromStorage are encoded manually for more space efficiency
        int entriesSlottedIngredientsFromPlayer = input.readInt();
        this.slottedIngredientsFromPlayer = Maps.newHashMap();
        for (int i = 0; i < entriesSlottedIngredientsFromPlayer; i++) {
            this.slottedIngredientsFromPlayer.put(input.readInt(), Pair.of(input.readItem(), input.readInt()));
        }

        int entriesSlottedIngredientsFromStorage = input.readInt();
        this.slottedIngredientsFromStorage = Maps.newHashMap();
        for (int i = 0; i < entriesSlottedIngredientsFromStorage; i++) {
            int key = input.readInt();
            int entries = input.readInt();
            List<Pair<ItemStack, Integer>> alternatives = Lists.newArrayListWithExpectedSize(entries);
            for (int j = 0; j < entries; j++) {
                alternatives.add(Pair.of(input.readItem(), input.readInt()));
            }
            this.slottedIngredientsFromStorage.put(key, alternatives);
        }
    }

    @Override
    public boolean isAsync() {
        return false;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void actionClient(World world, PlayerEntity player) {

    }

    @Override
    public void actionServer(World world, ServerPlayerEntity player) {
        if(player.containerMenu instanceof ContainerTerminalStorageBase) {
            ContainerTerminalStorageBase<?> container = ((ContainerTerminalStorageBase<?>) player.containerMenu);
            ITerminalStorageTabCommon tabCommon = container.getTabCommon(tabId);
            if (tabCommon instanceof TerminalStorageTabIngredientComponentItemStackCraftingCommon) {
                TerminalStorageTabIngredientComponentServer<ItemStack, Integer> tabServerCrafting =
                        (TerminalStorageTabIngredientComponentServer<ItemStack, Integer>) container.getTabServer(tabId);
                TerminalStorageTabIngredientComponentItemStackCraftingCommon tabCommonCrafting =
                        (TerminalStorageTabIngredientComponentItemStackCraftingCommon) tabCommon;
                int slotOffset = tabCommonCrafting.getSlotCrafting().index;

                // Clear current grid into storage
                TerminalStorageIngredientItemStackCraftingGridClear.clearGrid(tabCommonCrafting, tabServerCrafting,
                        channel, true, player);

                // Try filling the grid with the given recipe

                // Fill from player inventory
                IngredientComponentStorageWrapperHandlerItemStack.ComponentStorageWrapper playerInventory =
                        new IngredientComponentStorageWrapperHandlerItemStack.ComponentStorageWrapper(IngredientComponent.ITEMSTACK, new InvWrapper(player.inventory));
                for (Map.Entry<Integer, Pair<ItemStack, Integer>> entry : this.slottedIngredientsFromPlayer.entrySet()) {
                    Integer matchCondition = entry.getValue().getRight();
                    ItemStack extracted = playerInventory.extract(entry.getValue().getLeft(), matchCondition, false);
                    Slot slot = container.getSlot(entry.getKey() + slotOffset);
                    slot.set(extracted);
                }

                // Fill from storage
                IIngredientComponentStorage<ItemStack, Integer> storage = tabServerCrafting.getIngredientNetwork()
                        .getChannel(channel);
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
                        }
                    }
                }

                // Notify the client
                // TODO?
            }
        }
    }

}