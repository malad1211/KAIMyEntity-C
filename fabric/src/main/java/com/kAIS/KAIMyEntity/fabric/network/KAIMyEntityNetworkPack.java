package com.kAIS.KAIMyEntity.fabric.network;

import com.kAIS.KAIMyEntity.renderer.KAIMyEntityRendererPlayerHelper;
import com.kAIS.KAIMyEntity.renderer.MMDModelManager;

import io.netty.buffer.ByteBuf;

import java.util.UUID;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public record KAIMyEntityNetworkPack(int opCode, String playerUUIDString, int arg0) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<KAIMyEntityNetworkPack> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("kaimyentity", "networkpack"));
    public static final StreamCodec<ByteBuf, KAIMyEntityNetworkPack> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT,
        KAIMyEntityNetworkPack::opCode,
        ByteBufCodecs.STRING_UTF8,
        KAIMyEntityNetworkPack::playerUUIDString,
        ByteBufCodecs.VAR_INT,
        KAIMyEntityNetworkPack::arg0,
        KAIMyEntityNetworkPack::new
    );
    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    public static void sendToServer(int opCode, UUID playerUUID, int arg0){
        ClientPlayNetworking.send(new KAIMyEntityNetworkPack(opCode, playerUUID.toString(), arg0));
    }
    
    public static void DoInClient(FriendlyByteBuf buffer){
        DoInClient(buffer.readInt(), buffer.readUUID(), buffer.readInt());
    }

    public static void DoInClient(int opCode, UUID playerUUID, int arg0) {
        Minecraft MCinstance = Minecraft.getInstance();
        //Ignore message when player is self.
        assert MCinstance.player != null;
        if (playerUUID.equals(MCinstance.player.getUUID()))
            return;
        switch (opCode) {
            case 1: {
                MMDModelManager.Model m = MMDModelManager.GetModel("EntityPlayer_" + MCinstance.player.getName().getString());
                assert MCinstance.level != null;
                Player target = MCinstance.level.getPlayerByUUID(playerUUID);
                if (m != null && target != null)
                    KAIMyEntityRendererPlayerHelper.CustomAnim(target, Integer.toString(arg0));
                break;
            }
            case 2: {
                MMDModelManager.Model m = MMDModelManager.GetModel("EntityPlayer_" + MCinstance.player.getName().getString());
                assert MCinstance.level != null;
                Player target = MCinstance.level.getPlayerByUUID(playerUUID);
                if (m != null && target != null)
                    KAIMyEntityRendererPlayerHelper.ResetPhysics(target);
                break;
            }
        }
    }
}
