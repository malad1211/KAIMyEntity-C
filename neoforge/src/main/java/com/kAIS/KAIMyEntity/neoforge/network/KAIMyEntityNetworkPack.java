package com.kAIS.KAIMyEntity.neoforge.network;

import com.kAIS.KAIMyEntity.renderer.KAIMyEntityRendererPlayerHelper;
import com.kAIS.KAIMyEntity.renderer.MMDModelManager;
import com.mojang.blaze3d.systems.RenderSystem;

import io.netty.buffer.ByteBuf;

import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public record KAIMyEntityNetworkPack(int opCode, String playerUUIDString, int arg0) implements CustomPacketPayload{
    public static final Logger logger = LogManager.getLogger();
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

    public KAIMyEntityNetworkPack(int opCode, UUID uuid, int arg){
        this(opCode, uuid.toString(), arg);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void DoInClient(KAIMyEntityNetworkPack pack, IPayloadContext context) {
        Minecraft MCinstance = Minecraft.getInstance();
        UUID playerUUID = UUID.fromString(pack.playerUUIDString);
        //Ignore message when player is self.
        assert MCinstance.player != null;
        assert MCinstance.level != null;
        Player targetPlayer = MCinstance.level.getPlayerByUUID(playerUUID);
        if (playerUUID.equals(MCinstance.player.getUUID()))
            return;
        if (targetPlayer == null){
            logger.warn("received an invalid UUID.");
            return;
        }
        switch (pack.opCode) {
            case 1: {
                RenderSystem.recordRenderCall(()->{
                MMDModelManager.Model m = MMDModelManager.GetModel("EntityPlayer_" + targetPlayer.getName().getString());
                if (m != null)
                    KAIMyEntityRendererPlayerHelper.CustomAnim(targetPlayer, Integer.toString(pack.arg0));
                });
                break;
            }
            case 2: {
                RenderSystem.recordRenderCall(()->{
                MMDModelManager.Model m = MMDModelManager.GetModel("EntityPlayer_" + targetPlayer.getName().getString());
                if (m != null)
                    KAIMyEntityRendererPlayerHelper.ResetPhysics(targetPlayer);
                });
                break;
            }
        }
    }

    public static void DoInServer(KAIMyEntityNetworkPack pack, IPayloadContext context){
        PacketDistributor.sendToAllPlayers(pack);
    }
}
