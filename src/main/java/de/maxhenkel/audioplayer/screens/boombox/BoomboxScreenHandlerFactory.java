package de.maxhenkel.audioplayer.screens.boombox;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

public class BoomboxScreenHandlerFactory implements ExtendedScreenHandlerFactory {
    private final ItemStack stack;

    public BoomboxScreenHandlerFactory(ItemStack stack) {
        this.stack = stack;
    }

    @Override
    public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) {
        buf.writeItemStack(this.stack);
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable("gui.audioplayer.boombox");
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new BoomboxScreenHandler(syncId, playerInventory, this.stack);
    }
}
