package com.mj.test;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Created by houguoli on 2017/1/15.
 */

public class GLTexView {
    private static final int TEX_COUNT = 16;
    private int mTextureIds[] = new int[TEX_COUNT];
    private int rectProgram;
    private int rectPositionParam;
    private int rectAlphaParam;
    private int rectMaskPosParam;
    private int rectModelViewProjectionParam;
    private int rectTextureParam;

    // rect
    private FloatBuffer rectVertices;
    private FloatBuffer rectTextureCoords;

    private Context mContext;

    public GLTexView(Context cxt)
    {
        mContext = cxt;
    }
    private String readRawTextFile(int resId) {
        InputStream inputStream = mContext.getResources().openRawResource(resId);
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            reader.close();
            return sb.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private int loadGLShader(int type, int resId) {
        String code = readRawTextFile(resId);
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, code);
        GLES20.glCompileShader(shader);

        // Get the compilation status.
        final int[] compileStatus = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0);

        // If the compilation failed, delete the shader.
        if (compileStatus[0] == 0) {
            Log.e("GLTexView", "Error compiling shader: " + GLES20.glGetShaderInfoLog(shader));
            GLES20.glDeleteShader(shader);
            shader = 0;
        }

        if (shader == 0) {
            throw new RuntimeException("Error creating shader.");
        }

        return shader;
    }

    public void init()
    {
        int imageVertexShader = loadGLShader(GLES20.GL_VERTEX_SHADER, R.raw.image_vertex);
        int imageFragmentShader = loadGLShader(GLES20.GL_FRAGMENT_SHADER, R.raw.image_fragment);
        rectProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(rectProgram, imageVertexShader);
        GLES20.glAttachShader(rectProgram, imageFragmentShader);
        GLES20.glLinkProgram(rectProgram);
        GLES20.glUseProgram(rectProgram);

        checkGLError("rect program");

        rectModelViewProjectionParam = GLES20.glGetUniformLocation(rectProgram, "uMVPMatrix");
        rectAlphaParam = GLES20.glGetUniformLocation(rectProgram, "uAlpha");
        rectMaskPosParam = GLES20.glGetUniformLocation(rectProgram, "uMask");

        rectPositionParam = GLES20.glGetAttribLocation(rectProgram, "vPosition");
        rectTextureParam = GLES20.glGetAttribLocation(rectProgram, "inputTextureCoordinate");

        checkGLError("rect program params");

        // make a rect
        ByteBuffer bbRectVertices = ByteBuffer.allocateDirect(WorldLayoutData.RECT_COORDS.length * 4);
        bbRectVertices.order(ByteOrder.nativeOrder());
        rectVertices = bbRectVertices.asFloatBuffer();
        rectVertices.put(WorldLayoutData.RECT_COORDS);
        rectVertices.position(0);

        ByteBuffer bbRectNormals = ByteBuffer.allocateDirect(WorldLayoutData.RECT_TEXTURECOODS.length * 4);
        bbRectNormals.order(ByteOrder.nativeOrder());
        rectTextureCoords = bbRectNormals.asFloatBuffer();
        rectTextureCoords.put(WorldLayoutData.RECT_TEXTURECOODS);
        rectTextureCoords.position(0);

    }
    int mTexIndex = 0;
    int mTexId = 0;
    boolean bOdd = true;
    public void drawRect(float []mat)
    {
        int index = mTexIndex / 10;
        if( index>= TEX_COUNT)
        {
            mTexIndex = 0;
        }
        ++mTexIndex;
        if( bOdd)
        {
            mTexId = mTextureIds[index];
            bOdd = false;
        }
        else {
            bOdd = true;
        }
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        GLES20.glUseProgram(rectProgram);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTexId);

        // Set ModelView, MVP, position, normals, and color.
        GLES20.glUniformMatrix4fv(rectModelViewProjectionParam, 1, false, mat, 0);
        GLES20.glUniform1f(rectAlphaParam, 0.5f);
        GLES20.glUniform1f(rectMaskPosParam, 0.5f);

        GLES20.glEnableVertexAttribArray(rectPositionParam);
        GLES20.glEnableVertexAttribArray(rectTextureParam);
        GLES20.glVertexAttribPointer(rectPositionParam, 3, GLES20.GL_FLOAT, false, 0, rectVertices);
        GLES20.glVertexAttribPointer(rectTextureParam, 2, GLES20.GL_FLOAT, false, 0, rectTextureCoords);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        GLES20.glDisableVertexAttribArray(rectPositionParam);
        GLES20.glDisableVertexAttribArray(rectTextureParam);

        GLES20.glDisable(GLES20.GL_BLEND);

        checkGLError("drawing rect");
    }

    private static void checkGLError(String label) {
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            Log.e("GLTexView", label + ": glError " + error);
            throw new RuntimeException(label + ": glError " + error);
        }
    }

    public void initImageTexture(Context context, int[] drawableIds) {
        //通过输入流加载图片===============begin===================

        GLES20.glGenTextures(TEX_COUNT, mTextureIds, 0);

        int count = drawableIds.length;
        for (int i = 0; i < count; ++i)
        {
            InputStream is = context.getResources().openRawResource(drawableIds[i]);
            Bitmap bitmap;
            try
            {
                bitmap = BitmapFactory.decodeStream(is);
            }
            finally
            {
                try
                {
                    is.close();
                }
                catch(IOException e)
                {
                    e.printStackTrace();
                }
            }

            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureIds[i]);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_MAG_FILTER,GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,GLES20.GL_CLAMP_TO_EDGE);
            //实际加载纹理
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);

        }

    }


    public int initImageTexture(Context context, Bitmap bm, boolean isRecycle) {
        if (bm == null) {
            return -1;
        }

        //生成纹理ID
        int[] textures = new int[1];

        GLES20.glGenTextures
                (
                        1,          //产生的纹理id的数量
                        textures,   //纹理id的数组
                        0           //偏移量
                );


        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_MAG_FILTER,GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,GLES20.GL_CLAMP_TO_EDGE);
        //实际加载纹理
        GLUtils.texImage2D
                (
                        GLES20.GL_TEXTURE_2D,   //纹理类型，在OpenGL ES中必须为GL10.GL_TEXTURE_2D
                        0, 					  //纹理的层次，0表示基本图像层，可以理解为直接贴图
                        bm, 			  //纹理图像
                        0					  //纹理边框尺寸
                );

        if (isRecycle){
            bm.recycle(); 		  //纹理加载成功后释放图片
        }
        mTextureIds[0] = textures[0];
        return textures[0];
    }

}
