package org.etieskrill.engine.graphics.gl;

import glm.mat._4.Mat4;
import org.etieskrill.engine.graphics.assimp.Material;
import org.etieskrill.engine.graphics.assimp.Mesh;
import org.etieskrill.engine.graphics.assimp.Vertex;
import org.etieskrill.engine.graphics.gl.Texture.TextureType;
import org.etieskrill.engine.util.FloatArrayMerger;

import java.util.*;

import static org.etieskrill.engine.graphics.gl.RawModel.*;
import static org.lwjgl.opengl.GL33C.*;

public class Loader {

    public static final int GL_FLOAT_BYTE_SIZE = Float.BYTES;

    private static Loader loader;
    
    private final List<Integer> vaos = new ArrayList<>();
    private final List<Integer> vbos = new ArrayList<>();
    private final List<Integer> ebos = new ArrayList<>();
    
    private final Map<String, Texture> textures = new HashMap<>();
    
    public static Loader get() {
        if (loader == null)
            loader = new Loader();
        return loader;
    }
    
    private Loader() {}

    public RawModel loadToVAO(float[] vertices, float[] normals, float[] textures, short[] indices, int drawMode) {
        boolean hasIndexBuffer = indices != null;

        int vao = createVAO();
        storeInAttributeList(vertices, normals, textures);
        if (hasIndexBuffer) prepareIndexBuffer(indices);
        unbindVAO();

        return new RawModel(vao, hasIndexBuffer ? indices.length : vertices.length, drawMode, hasIndexBuffer);
    }
    
    public Mesh loadToVAO(Vector<Vertex> vertices, Vector<Short> indices, Material material, Mat4 transform) {
        int vao = createVAO();
    
        List<Float> _data = vertices.stream()
                .map(Vertex::toList)
                .flatMap(List::stream)
                .toList();
        float[] data = new float[_data.size()];
        for (int i = 0; i < _data.size(); i++) data[i] = _data.get(i);
        int vbo = prepareVBO(data);
        
        short[] _indices = new short[indices.size()];
        for (int i = 0; i < indices.size(); i++) _indices[i] = indices.get(i);
        int ebo = prepareIndexBuffer(_indices);
        
        unbindVAO();
        return new Mesh(material, vao, indices.size(), vbo, ebo, transform);
    }
    
    private int prepareVBO(float[] data) {
        int vbo = glGenBuffers();
        vbos.add(vbo);
    
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, data, GL_STATIC_READ);
    
        int totalStride = (Vertex.COMPONENTS) * GL_FLOAT_BYTE_SIZE;
    
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, Vertex.POSITION_COMPONENTS, GL_FLOAT, false,
                totalStride, 0);
    
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(1, Vertex.NORMAL_COMPONENTS, GL_FLOAT, true,
                totalStride, Vertex.POSITION_COMPONENTS * GL_FLOAT_BYTE_SIZE);
    
        glEnableVertexAttribArray(2);
        glVertexAttribPointer(2, Vertex.TEXTURE_COMPONENTS, GL_FLOAT, false,
                totalStride, (Vertex.POSITION_COMPONENTS + MODEL_NORMAL_COMPONENTS) * GL_FLOAT_BYTE_SIZE);
    
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        return vbo;
    }
    
    private int createVAO() {
        int vao = glGenVertexArrays();
        vaos.add(vao);

        glBindVertexArray(vao);
        
        return vao;
    }
    
    private void unbindVAO() {
        glBindVertexArray(0);
    }
    
    private int storeInAttributeList(float[] vertices, float[] normals, float[] textures) {
        int vbo = glGenBuffers();
        vbos.add(vbo);

        int vertexLength = vertices.length / MODEL_POSITION_COMPONENTS;
        if (vertexLength != normals.length / MODEL_NORMAL_COMPONENTS)
            throw new IllegalArgumentException("Number of vertex positions does not match number of vertex normals");
        else if (vertexLength != textures.length / MODEL_TEXTURE_COMPONENTS)
            throw new IllegalArgumentException("Number of vertex positions does not match number of vertex textures");

        float[] data = FloatArrayMerger.merge(vertices, normals, MODEL_POSITION_COMPONENTS, MODEL_NORMAL_COMPONENTS);
        data = FloatArrayMerger.merge(data, textures, MODEL_POSITION_COMPONENTS + MODEL_NORMAL_COMPONENTS, MODEL_TEXTURE_COMPONENTS);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, data, GL_DYNAMIC_DRAW);

        int totalStride = (MODEL_POSITION_COMPONENTS + MODEL_NORMAL_COMPONENTS + MODEL_TEXTURE_COMPONENTS) * GL_FLOAT_BYTE_SIZE;
        
        glVertexAttribPointer(0, MODEL_POSITION_COMPONENTS, GL_FLOAT, false,
                totalStride, 0);
        glEnableVertexAttribArray(0);

        glVertexAttribPointer(1, MODEL_NORMAL_COMPONENTS, GL_FLOAT, true,
                totalStride, MODEL_POSITION_COMPONENTS * GL_FLOAT_BYTE_SIZE);
        glEnableVertexAttribArray(1);

        glVertexAttribPointer(2, MODEL_TEXTURE_COMPONENTS, GL_FLOAT, false,
                totalStride, (MODEL_POSITION_COMPONENTS + MODEL_NORMAL_COMPONENTS) * GL_FLOAT_BYTE_SIZE);
        glEnableVertexAttribArray(2);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        return vbo;
    }
    
    private int prepareIndexBuffer(short[] indices) {
        int ebo = glGenBuffers();
        ebos.add(ebo);

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_DYNAMIC_DRAW);
        
        return ebo;
    }
    
    public Texture loadTexture(String file, String name) {
        return loadTexture(file, name, TextureType.UNKNOWN);
    }
    
    /**
     * Loads the texture with the specified file name relative to the textures folder, or returns the loaded texture if
     * it was already loaded, or {@code null} if an error occurred while loading or destructuring the image file.
     * @param file just the file name without the path
     * @param name internal identifier name
     * @param type the texture type
     * @return a loaded texture, or null
     */
    public Texture loadTexture(String file, String name, TextureType type) {
        if (file == null) {
            System.err.printf("[%s] Texture file name must not be null.\n", getClass().getSimpleName());
            return null;
        }
        if (name == null || name.length() == 0) {
            System.err.printf("[%s] Invalid texture name: \"%s\"\n",
                    getClass().getSimpleName(), name != null ? name : "null");
            return null;
        }
        
        if (textures.containsKey(name)) {
//            System.out.printf("[%s] %s texture for name \"%s\" was already loaded.\n",
//                    getClass().getSimpleName(), type.name(), name);
            System.out.printf("[%s] Texture \"%s\" was already loaded.\n", getClass().getSimpleName(), name);
            return textures.get(name);
        }
        
        Texture texture = Texture.ofFile(file, type);
        textures.put(name, texture);
        return texture;
    }
    
    public Texture getTexture(String name) {
        return textures.get(name);
    }
    
    public void dispose() {
        for (int vao : vaos) glDeleteBuffers(vao);
        for (int vbo : vbos) glDeleteVertexArrays(vbo);
        for (int ebo : ebos) glDeleteBuffers(ebo);
        for (Texture texture : textures.values()) texture.dispose();
    }
    
}
