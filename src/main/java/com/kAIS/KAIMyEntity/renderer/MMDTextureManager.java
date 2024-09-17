package com.kAIS.KAIMyEntity.renderer;

import com.kAIS.KAIMyEntity.NativeFunc;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class MMDTextureManager
{
    public static class Texture
    {
        public int tex;
        public boolean hasAlpha;
    }

    public static void Init()
    {
        nf = NativeFunc.GetInst();
        texs = new HashMap<>();
        // TODO: placeholder way to avoid excessive file read
        loadTextureAttemptTime = new HashMap<>();
    }

    public static Texture GetTexture(String filename)
    {
        Texture result = texs.get(filename);
        if (result == null)
        {
            if (!AllowTextureReload(filename)) return null;

            long nfTex = nf.LoadTexture(filename);
            if (nfTex == 0)
                return null;
            int x = nf.GetTextureX(nfTex);
            int y = nf.GetTextureY(nfTex);
            long texData = nf.GetTextureData(nfTex);
            boolean hasAlpha = nf.TextureHasAlpha(nfTex);

            int tex = GL11.glGenTextures();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, tex);
            int texSize = x * y * (hasAlpha ? 4 : 3);
            ByteBuffer texBuffer = ByteBuffer.allocateDirect(texSize);
            /*
            for (int i = 0; i < texSize; ++i)
                texBuffer.put(nf.ReadByte(texData, i));
            texBuffer.position(0);
             */
            nf.CopyDataToByteBuffer(texBuffer, texData, texSize);
            if (hasAlpha)
            {
                GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 4);
                GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, x, y, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, texBuffer);

            }
            else
            {
                GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);
                GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGB, x, y, 0, GL11.GL_RGB, GL11.GL_UNSIGNED_BYTE, texBuffer);
            }
            nf.DeleteTexture(nfTex);

            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LEVEL, 0);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);

            result = new Texture();
            result.tex = tex;
            result.hasAlpha = hasAlpha;
            texs.put(filename, result);
        }
        return result;
    }

    public static void DeleteAll()
    {
        for (Texture i : texs.values())
        {
            GL11.glDeleteTextures(i.tex);
        }
        texs = new HashMap<>();
    }

    private static boolean AllowTextureReload(String filename) {
        Long now = System.currentTimeMillis();
        if (!loadTextureAttemptTime.containsKey(filename)) {
            loadTextureAttemptTime.put(filename, now);
            return true;
        }

        Long lastAttempt = loadTextureAttemptTime.get(filename);
        if (now - lastAttempt < reloadTextureInterval) return false;

        loadTextureAttemptTime.put(filename, now);
        return true;
    }

    static NativeFunc nf;
    static Map<String, Texture> texs;
    static Map<String, Long> loadTextureAttemptTime;
    static final long reloadTextureInterval = 10000;
}
