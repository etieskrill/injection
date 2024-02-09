package org.etieskrill.engine.graphics.model;

import org.etieskrill.engine.Disposable;
import org.etieskrill.engine.entity.data.AABB;

import java.util.Collections;
import java.util.List;

import static org.lwjgl.opengl.GL33C.*;

public class Mesh implements Disposable {
    
    public static final int GL_FLOAT_BYTE_SIZE = Float.BYTES;
    
    private final Material material;
    private final List<Bone> bones;
    private final int vao, numIndices, vbo, ebo;
    private final AABB boundingBox;
    private final DrawMode drawMode;

    public enum DrawMode {
        POINTS(GL_POINTS),
        LINES(GL_LINES),
        LINE_LOOP(GL_LINE_LOOP),
        LINE_STRIP(GL_LINE_STRIP),
        TRIANGLES(GL_TRIANGLES),
        TRIANGLE_STRIP(GL_TRIANGLE_STRIP),
        TRIANGLE_FAN(GL_TRIANGLE_FAN),
        QUADS(GL_QUADS);

        private final int glDrawMode;

        DrawMode(int glDrawMode) {
            this.glDrawMode = glDrawMode;
        }

        public int gl() {
            return glDrawMode;
        }
    }
    
    public Mesh(Material material,
                List<Bone> bones,
                int vao, int numIndices, int vbo, int ebo,
                AABB boundingBox,
                DrawMode drawMode) {
        this.material = material;
        this.bones = Collections.unmodifiableList(bones);
        
        this.vao = vao;
        this.numIndices = numIndices;
        this.vbo = vbo;
        this.ebo = ebo;
        
        this.boundingBox = boundingBox;

        this.drawMode = drawMode;
    }
    
    public Material getMaterial() {
        return material;
    }

    public List<Bone> getBones() {
        return bones;
    }

    public int getNumIndices() {
        return numIndices;
    }
    
    public int getVao() {
        return vao;
    }
    
    public AABB getBoundingBox() {
        return boundingBox;
    }

    public DrawMode getDrawMode() {
        return drawMode;
    }

    @Override
    public void dispose() {
        material.dispose();
        glDeleteBuffers(vbo);
        glDeleteBuffers(ebo);
        glDeleteVertexArrays(vao);
    }
    
}
