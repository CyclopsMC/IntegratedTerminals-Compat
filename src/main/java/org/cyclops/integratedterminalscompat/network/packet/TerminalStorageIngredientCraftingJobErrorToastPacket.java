package org.cyclops.integratedterminalscompat.network.packet;

import com.google.common.collect.Lists;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.cyclops.cyclopscore.network.PacketCodec;
import org.cyclops.integratedterminalscompat.Reference;

import java.util.List;

/**
 * Packet for showing a crafting job summary toast on the client.
 * @author rubensworks
 */
public class TerminalStorageIngredientCraftingJobErrorToastPacket extends PacketCodec<TerminalStorageIngredientCraftingJobErrorToastPacket> {

    public static final Type<TerminalStorageIngredientCraftingJobErrorToastPacket> ID = new Type<>(Identifier.fromNamespaceAndPath(Reference.MOD_ID, "terminal_storage_ingredient_crafting_job_error_toast"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TerminalStorageIngredientCraftingJobErrorToastPacket> CODEC = getCodec(TerminalStorageIngredientCraftingJobErrorToastPacket::new);

    private List<String> startedIngredientNames;
    private List<String> failedIngredientNames;

    public TerminalStorageIngredientCraftingJobErrorToastPacket() {
        super(ID);
    }

    public TerminalStorageIngredientCraftingJobErrorToastPacket(List<String> startedIngredientNames, List<String> failedIngredientNames) {
        super(ID);
        this.startedIngredientNames = startedIngredientNames;
        this.failedIngredientNames = failedIngredientNames;
    }

    private static void writeStringList(RegistryFriendlyByteBuf output, List<String> list) {
        output.writeInt(list.size());
        for (String s : list) {
            output.writeUtf(s);
        }
    }

    private static List<String> readStringList(RegistryFriendlyByteBuf input) {
        int count = input.readInt();
        List<String> list = Lists.newArrayListWithExpectedSize(count);
        for (int i = 0; i < count; i++) {
            list.add(input.readUtf());
        }
        return list;
    }

    @Override
    public void encode(RegistryFriendlyByteBuf output) {
        super.encode(output);
        writeStringList(output, startedIngredientNames);
        writeStringList(output, failedIngredientNames);
    }

    @Override
    public void decode(RegistryFriendlyByteBuf input) {
        super.decode(input);
        this.startedIngredientNames = readStringList(input);
        this.failedIngredientNames = readStringList(input);
    }

    @Override
    public boolean isAsync() {
        return false;
    }

    @Override
    public void actionClient(Level world, Player player) {
        TerminalStorageIngredientCraftingJobErrorToastPacketClient.showCraftingJobToast(startedIngredientNames, failedIngredientNames);
    }

    @Override
    public void actionServer(Level world, ServerPlayer player) {
        // Server-to-client only packet
    }

}
