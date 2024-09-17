package com.kAIS.KAIMyEntity.renderer;

import com.kAIS.KAIMyEntity.NativeFunc;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class MMDAnimManager
{
    public static void Init()
    {
        nf = NativeFunc.GetInst();
        animStatic = new HashMap<>();
        animModel = new HashMap<>();
        // TODO: placeholder way to avoid excessive file read
        loadAnimAttemptTime = new HashMap<>();
    }

    //(Slash) For player (/Slash) Now player has multiple model. This function will be deleted.
    public static long GetAnimStatic(IMMDModel model, String animName)
    {
        String filename = GetAnimationFilename(model.GetModelDir(), animName);
        Long result = animStatic.get(filename);
        if (result == null)
        {
            if (!AllowAnimReload(filename)) return 0;

            result = nf.LoadAnimation(model.GetModelLong(), filename);
            if (result == 0) return 0;
            animStatic.put(filename, result);
        }
        return result;
    }

    public static void AddModel(IMMDModel model)
    {
        animModel.put(model, new HashMap<>());
    }

    public static void DeleteModel(IMMDModel model)
    {
        Collection<Long> arr = animModel.get(model).values();
        for (Long i : arr)
            nf.DeleteAnimation(i);
        animModel.remove(model);
    }

    //For other entity (Multiple model)
    public static long GetAnimModel(IMMDModel model, String animName)
    {
        String filename = GetAnimationFilename(model.GetModelDir(), animName);
        Map<String, Long> sub = animModel.get(model);
        Long result = sub.get(filename);
        if (result == null)
        {
            if (!AllowAnimReload(filename)) return 0;

            result = nf.LoadAnimation(model.GetModelLong(), filename);
            if (result == 0) return 0;
            sub.put(filename, result);
        }
        return result;
    }

    public static void DeleteAll()
    {
        for (Long i : animStatic.values())
            nf.DeleteAnimation(i);
    }

    private static boolean AllowAnimReload(String filename) {
        Long now = System.currentTimeMillis();
        if (!loadAnimAttemptTime.containsKey(filename)) {
            loadAnimAttemptTime.put(filename, now);
            return true;
        }

        Long lastAttempt = loadAnimAttemptTime.get(filename);
        if (now - lastAttempt < reloadAnimInterval) return false;

        loadAnimAttemptTime.put(filename, now);
        return true;
    }

    static NativeFunc nf;
    static Map<String, Long> animStatic;
    static Map<IMMDModel, Map<String, Long>> animModel;
    static Map<String, Long> loadAnimAttemptTime;
    static final long reloadAnimInterval = 10000;

    static String GetAnimationFilename(String modelDir, String animName)
    {
        File animFilename = new File(modelDir, animName + ".vmd");
        return animFilename.getAbsolutePath();
    }
}
