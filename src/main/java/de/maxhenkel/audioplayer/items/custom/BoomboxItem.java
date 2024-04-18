package de.maxhenkel.audioplayer.items.custom;

import de.maxhenkel.audioplayer.*;
import de.maxhenkel.audioplayer.screens.boombox.BoomboxInventory;
import de.maxhenkel.audioplayer.screens.boombox.BoomboxScreenHandlerFactory;
import de.maxhenkel.audioplayer.util.ICanUseBoombox;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Equipment;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.UUID;

public class BoomboxItem extends Item implements Equipment {
    public BoomboxItem(Settings settings) {
        super(settings);
    }

    @Override
    public EquipmentSlot getSlotType() {
        return EquipmentSlot.HEAD;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack itemStack = user.getStackInHand(hand);

        if(!user.isSneaking()) {
            if(!world.isClient) {
                PlayerManager playerManager = PlayerManager.instance();

                BoomboxInventory boomboxInventory = new BoomboxInventory(itemStack);

                CustomSound customSound = CustomSound.of(boomboxInventory.getTape());

                if (customSound == null) return TypedActionResult.pass(itemStack);

                ICanUseBoombox iCanUseBoombox = (ICanUseBoombox) user;

                UUID channel = iCanUseBoombox.audio_player$getChannelId();

                if(channel == null || !playerManager.isPlaying(channel)) {
                    if(!iCanUseBoombox.audio_player$play(itemStack, customSound)) return TypedActionResult.fail(itemStack);

                    return TypedActionResult.success(itemStack);
                }

                iCanUseBoombox.audio_player$stop();
                return TypedActionResult.success(itemStack);
            }

            return TypedActionResult.pass(itemStack);
        }

        user.openHandledScreen(new BoomboxScreenHandlerFactory(itemStack));
        return TypedActionResult.success(itemStack);
    }
}
