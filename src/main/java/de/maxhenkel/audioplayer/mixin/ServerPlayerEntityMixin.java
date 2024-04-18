package de.maxhenkel.audioplayer.mixin;

import com.mojang.authlib.GameProfile;
import de.maxhenkel.audioplayer.CustomSound;
import de.maxhenkel.audioplayer.PlayerManager;
import de.maxhenkel.audioplayer.PlayerType;
import de.maxhenkel.audioplayer.Plugin;
import de.maxhenkel.audioplayer.util.ICanUseBoombox;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin extends PlayerEntity implements ICanUseBoombox {
    @Unique
    private UUID channelId;
    @Unique
    private ItemStack boombox;

    public ServerPlayerEntityMixin(World world, BlockPos pos, float yaw, GameProfile gameProfile) {
        super(world, pos, yaw, gameProfile);
    }

    @Inject(method = "playerTick", at = @At("TAIL"))
    private void injectTick(CallbackInfo ci) {
        PlayerManager playerManager = PlayerManager.instance();

        ItemStack itemStackInHand = this.getStackInHand(Hand.MAIN_HAND);
        ItemStack itemStackHead = this.getEquippedStack(EquipmentSlot.HEAD);

        boolean isBoomBoxNotEquipped = itemStackInHand != boombox && itemStackHead != boombox;
        boolean isPlaying = channelId != null && playerManager.isPlaying(channelId);

        if(boombox != null && isBoomBoxNotEquipped && isPlaying) this.audio_player$stop();
    }

    @Override
    public boolean audio_player$play(ItemStack itemStack, CustomSound sound) {
        PlayerManager playerManager = PlayerManager.instance();

        channelId = playerManager.playOnEntity(Plugin.voicechatServerApi, (ServerWorld) this.getWorld(), (ServerPlayerEntity) (Object) this, sound, PlayerType.TAPE);
        if(channelId == null) {
            this.sendMessage(Text.literal("Unable to play the audio from the Boombox"), true);
            return false;
        }

        boombox = itemStack;
        this.sendMessage(Text.literal("Playing audio from Boombox"), true);
        return true;
    }

    @Override
    public void audio_player$stop() {
        PlayerManager playerManager = PlayerManager.instance();

        playerManager.stop(channelId);
        channelId = null;
        boombox = null;
        this.sendMessage(Text.literal("Stopped playing audio from Boombox"), true);
    }

    @Override
    public UUID audio_player$getChannelId() {
        return channelId;
    }
}
