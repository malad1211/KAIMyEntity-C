package com.kAIS.KAIMyEntity.renderer;

import com.kAIS.KAIMyEntity.NativeFunc;
import com.kAIS.KAIMyEntity.config.KAIMyEntityConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.*;

public class MMDModelManager
{
    public static void Init()
    {
        models = new HashMap<>();
        modelPool = new HashMap<>();
        prevTime = System.currentTimeMillis();
    }

    public static IMMDModel LoadModel(String modelName, long layerCount)
    {
        //Model path
        File modelDir = new File(Minecraft.getMinecraft().mcDataDir, "KAIMyEntity/" + modelName);
        String modelDirStr = modelDir.getAbsolutePath();

        String modelFilenameStr;
        boolean isPMD;
        File pmxModelFilename = new File(modelDir, "model.pmx");
        if (pmxModelFilename.isFile())
        {
            modelFilenameStr = pmxModelFilename.getAbsolutePath();
            isPMD = false;
        }
        else
        {
            File pmdModelFilename = new File(modelDir, "model.pmd");
            if (pmdModelFilename.isFile())
            {
                modelFilenameStr = pmdModelFilename.getAbsolutePath();
                isPMD = true;
            }
            else
            {
                return null;
            }
        }

        return MMDModelOpenGL.Create(modelFilenameStr, modelDirStr, isPMD, layerCount);
    }

    public static MMDModelManager.Model GetPlayerModelOrInPool(EntityPlayer entity) {
        MMDModelManager.Model m = MMDModelManager.GetModelOrInPool(entity, "EntityPlayer_" + entity.getName(), true);
        if (m == null) {
            m = MMDModelManager.GetModelOrInPool(entity, "EntityPlayer", true);
        }
        return m;
    }

    public static MMDModelManager.Model GetModelOrInPool(Entity entity, String modelName, boolean isPlayer)
    {
        Model model = MMDModelManager.GetModel(entity);
        //Check if model is active.
        if (model == null)
        {
            //First check if modelPool has model.
            IMMDModel m = GetModelFromPool(modelName);
            if (m != null)
            {
                AddModel(entity, m, modelName, isPlayer);
                model = GetModel(entity);
                return model;
            }

            //Load model from file.
            m = LoadModel(modelName, isPlayer ? 3 : 1);
            if (m == null)
                return null;

            //Regist Animation user because its a new model
            MMDAnimManager.AddModel(m);

            AddModel(entity, m, modelName, isPlayer);
            model = GetModel(entity);
        }
        return model;
    }

    public static Model GetModel(Entity entity)
    {
        return models.get(entity);
    }

    public static IMMDModel GetModelFromPool(String modelName)
    {
        Stack<IMMDModel> pool = modelPool.get(modelName);
        if (pool == null)
            return null;
        if (pool.empty())
            return null;
        else
            return pool.pop();
    }

    public static void AddModel(Entity entity, IMMDModel model, String modelName, boolean isPlayer)
    {
        if (isPlayer)
        {
            NativeFunc nf = NativeFunc.GetInst();
            PlayerData pd = new PlayerData();
            pd.stateLayer0 = PlayerData.EntityStateLayer0.Idle;
            pd.stateLayer1 = PlayerData.EntityStateLayer1.Idle;
            pd.stateLayer2 = PlayerData.EntityStateLayer2.Idle;
            pd.playCustomAnim = false;
            pd.rightHandMat = nf.CreateMat();
            pd.leftHandMat = nf.CreateMat();
            pd.matBuffer = ByteBuffer.allocateDirect(64); //float * 16

            ModelWithPlayerData m = new ModelWithPlayerData();
            m.entity = entity;
            m.model = model;
            m.modelName = modelName;
            m.unuseTime = 0;
            m.playerData = pd;
            model.ResetPhysics();
            model.ChangeAnim(MMDAnimManager.GetAnimModel(model, "idle"), 0);
            models.put(entity, m);
        }
        else
        {
            ModelWithEntityState m = new ModelWithEntityState();
            m.entity = entity;
            m.model = model;
            m.modelName = modelName;
            m.unuseTime = 0;
            m.state = MMDModelManager.EntityState.Idle;
            model.ResetPhysics();
            model.ChangeAnim(MMDAnimManager.GetAnimModel(model, "idle"), 0);
            models.put(entity, m);
        }
    }

    public static void Update()
    {
        long deltaTime = System.currentTimeMillis() - prevTime;

        // we don't need this running n times per tick
        // for every entity rendered
        if (deltaTime < 200) return;

        prevTime = System.currentTimeMillis();

        List<Entity> waitForDelete = new LinkedList<>();
        for (Model i : models.values())
        {
            i.unuseTime += deltaTime;
            if (i.unuseTime > 10000)
            {
                TryModelToPool(i);
                waitForDelete.add(i.entity);
            }
        }

        for (Entity i : waitForDelete)
            models.remove(i);
    }

    public static void ReloadModel()
    {
        for (Model i : models.values())
            DeleteModel(i);
        models = new HashMap<>();
        for (Stack<IMMDModel> i : modelPool.values())
        {
            for (IMMDModel j : i)
            {
                MMDModelOpenGL.Delete((MMDModelOpenGL)j);

                //Unregist animation user
                MMDAnimManager.DeleteModel(j);
            }
        }
        modelPool = new HashMap<>();
    }

    enum EntityState { Idle, Walk, Swim, Ridden }

    public static class Model
    {
        Entity entity;
        IMMDModel model;
        String modelName;
        long unuseTime;
    }

    static class ModelWithEntityState extends Model
    {
        EntityState state;
    }

    static class ModelWithPlayerData extends Model
    {
        PlayerData playerData;
    }

    static class PlayerData
    {
        enum EntityStateLayer0 { Idle, Walk, Squat, Sneak, Sprint, Air, OnLadder, Swim, Ride, Sleep, ElytraFly, Die }
        EntityStateLayer0 stateLayer0;
        enum EntityStateLayer1 { Idle, SwingRight, SwingLeft, Item1Right, Item1Left, Item2Right, Item2Left, Item3Right, Item3Left, Item4Right, Item4Left } //Idle means no animation.
        EntityStateLayer1 stateLayer1;
        enum EntityStateLayer2 { Idle, Squat, Sneak } //Idle means no animation.
        EntityStateLayer2 stateLayer2;
        boolean playCustomAnim; //Custom animation played in layer 0.
        long rightHandMat, leftHandMat;
        ByteBuffer matBuffer;
    }

    static Map<Entity, Model> models;
    static Map<String, Stack<IMMDModel>> modelPool;
    static long prevTime;

    static void DeleteModel(Model model)
    {
        MMDModelOpenGL.Delete((MMDModelOpenGL)model.model);

        //Unregist animation user
        MMDAnimManager.DeleteModel(model.model);
    }

    static void TryModelToPool(Model model)
    {
        if (modelPool.size() > KAIMyEntityConfig.modelPoolMaxCount)
        {
            DeleteModel(model);
        }
        else
        {
            Stack<IMMDModel> pool = modelPool.get(model.modelName);
            if (pool == null)
            {
                pool = new Stack<>();
                modelPool.put(model.modelName, pool);
            }
            pool.push(model.model);
        }
    }
}
