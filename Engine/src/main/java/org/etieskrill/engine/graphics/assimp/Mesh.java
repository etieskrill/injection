package org.etieskrill.engine.graphics.assimp;

import org.etieskrill.engine.Disposable;
import org.etieskrill.engine.entity.data.AABB;

import java.util.List;

import static org.etieskrill.engine.graphics.assimp.Vertex.*;
import static org.lwjgl.opengl.GL33C.*;

public class Mesh implements Disposable {
    
    public static final int GL_FLOAT_BYTE_SIZE = Float.BYTES;
    
    private final Material material;
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
    
    public static final class Loader {
        //TODO builder
        public static Mesh loadToVAO(List<Vertex> vertices, List<Integer> indices, Material material) {
            return loadToVAO(vertices, indices, material, null, null);
        }

        public static Mesh loadToVAO(List<Vertex> vertices, List<Integer> indices, Material material, AABB boundingBox) {
            return loadToVAO(vertices, indices, material, boundingBox, null);
        }

        public static Mesh loadToVAO(List<Vertex> vertices, List<Integer> indices, Material material, DrawMode drawMode) {
            return loadToVAO(vertices, indices, material, null, drawMode);
        }

        public static Mesh loadToVAO(List<Vertex> vertices, List<Integer> indices, Material material, AABB boundingBox, DrawMode drawMode) {
            int vao = createVAO();

            List<Float> _data = vertices.stream()
                    .map(Vertex::toList)
                    .flatMap(List::stream)
                    .toList();
            float[] data = new float[_data.size()];
            for (int i = 0; i < _data.size(); i++) data[i] = _data.get(i);
            int vbo = prepareVBO(data);

            int[] _indices = new int[indices.size()];
            for (int i = 0; i < indices.size(); i++) _indices[i] = indices.get(i);
            int ebo = prepareIndexBuffer(_indices);
        
            unbindVAO();
            return new Mesh(material, vao, indices.size(), vbo, ebo, boundingBox, drawMode != null ? drawMode : DrawMode.TRIANGLES);
        }
    
        private static int createVAO() {
            int vao = glGenVertexArrays();
            glBindVertexArray(vao);
            return vao;
        }
    
        private static int prepareVBO(float[] data) {
            int vbo = glGenBuffers();
            glBindBuffer(GL_ARRAY_BUFFER, vbo);
            glBufferData(GL_ARRAY_BUFFER, data, GL_STATIC_READ);
        
            setVertexAttributePointer(0, POSITION_COMPONENTS, false, 0);
            setVertexAttributePointer(1, NORMAL_COMPONENTS, true,
                    POSITION_COMPONENTS * GL_FLOAT_BYTE_SIZE);
            setVertexAttributePointer(2, TEXTURE_COMPONENTS, false,
                    (POSITION_COMPONENTS + NORMAL_COMPONENTS) * GL_FLOAT_BYTE_SIZE);
        
            glBindBuffer(GL_ARRAY_BUFFER, 0);
            return vbo;
        }
    
        private static void setVertexAttributePointer(int index, int numComponents, boolean normalised, int offset) {
            glEnableVertexAttribArray(index);
            int totalStride = COMPONENTS * GL_FLOAT_BYTE_SIZE;
            glVertexAttribPointer(index, numComponents, GL_FLOAT, normalised, totalStride, offset);
        }

        private static int prepareIndexBuffer(int[] indices) {
            int ebo = glGenBuffers();
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
            glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_DYNAMIC_DRAW);
            return ebo;
        }
    
        private static void unbindVAO() {
            glBindVertexArray(0);
        }
    }
    
    public Mesh(Material material,
                int vao, int numIndices, int vbo, int ebo,
                AABB boundingBox,
                DrawMode drawMode) {
        this.material = material;
        
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
