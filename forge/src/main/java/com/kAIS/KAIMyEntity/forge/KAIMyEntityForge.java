package com.kAIS.KAIMyEntity.forge;

import com.kAIS.KAIMyEntity.KAIMyEntity;
import com.kAIS.KAIMyEntity.forge.config.KAIMyEntityConfig;
import com.kAIS.KAIMyEntity.forge.register.KAIMyEntityRegisterCommon;

import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(KAIMyEntity.MOD_ID)
public class KAIMyEntityForge {
    //public static String[] debugStr = new String[hogehoge];

    public KAIMyEntityForge() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::preInit);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, KAIMyEntityConfig.config);
    }

    public void preInit(FMLCommonSetupEvent event) {
        KAIMyEntity.logger.info("KAIMyEntity preInit begin...");
        KAIMyEntityRegisterCommon.Register();
        KAIMyEntity.logger.info("KAIMyEntity preInit successful.");
    }
}