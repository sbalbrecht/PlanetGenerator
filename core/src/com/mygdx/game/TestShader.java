package com.mygdx.game;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.graphics.g3d.utils.RenderContext;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.GdxRuntimeException;

public class TestShader implements Shader {
    ShaderProgram shaderProgram;
    Camera camera;
    RenderContext context;
    int u_projViewTrans;
    int u_worldTrans;

    @Override
    public void init () {
        String vertexShader =
                "attribute vec3 a_position;     \n" +
                        "attribute vec3 a_normal;       \n" +
                        "attribute vec4 a_color;        \n" +
                        "uniform mat4 u_projViewTrans;      \n" +
                        "varying vec4 v_color;          \n" +
                        "void main()                    \n" +
                        "{                              \n" +
                        "   v_color = a_color;          \n" +
                        "   gl_Position = u_projViewTrans * vec4(a_position, 1.0);  \n" +
                        "}                              \n" ;
        String fragmentShader =
                "#ifdef GL_ES                   \n" +
                        "precision mediump float;       \n" +
                        "#endif                         \n" +
                        "varying vec4 v_color;          \n" +
                        "void main()                    \n" +
                        "{                              \n" +
                        "  gl_FragColor = v_color;      \n" +
                        "}";
        shaderProgram = new ShaderProgram(vertexShader, fragmentShader);
        if(!shaderProgram.isCompiled()) {
            throw new GdxRuntimeException(shaderProgram.getLog());
        }
        u_projViewTrans = shaderProgram.getUniformLocation("u_projViewTrans");
        u_worldTrans = shaderProgram.getUniformLocation("u_worldTrans");
    }
    @Override
    public void dispose () {
        shaderProgram.dispose();
    }
    @Override
    public void begin (Camera camera, RenderContext context) {
        this.camera = camera;
        this.context = context;
        shaderProgram.begin();
        shaderProgram.setUniformMatrix(u_projViewTrans, camera.combined);
        context.setDepthTest(GL20.GL_LEQUAL);
        context.setCullFace(GL20.GL_BACK);
    }
    @Override
    public void render (Renderable renderable) {
        shaderProgram.setUniformMatrix(u_worldTrans, renderable.worldTransform);
        renderable.meshPart.render(shaderProgram);
    }
    @Override
    public void end () {
        shaderProgram.end();
    }
    @Override
    public int compareTo (Shader other) {
        return 0;
    }
    @Override
    public boolean canRender (Renderable instance) {
        return true;
    }
}
