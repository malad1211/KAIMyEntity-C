package com.kAIS.KAIMyEntity.renderer;

import net.minecraft.entity.Entity;

public interface IMMDModel
{
    void Render(Entity entityIn, double x, double y, double z, float entityYaw);
    void ChangeAnim(long anim, long layer);
    void ResetPhysics();
    long GetModelLong();
    String GetModelDir();
}