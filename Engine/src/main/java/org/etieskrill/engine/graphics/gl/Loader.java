package org.etieskrill.engine.graphics.gl;

import glm.mat._4.Mat4;
import org.etieskrill.engine.Disposable;
import org.etieskrill.engine.graphics.assimp.Material;
import org.etieskrill.engine.graphics.assimp.Mesh;
import org.etieskrill.engine.graphics.assimp.Vertex;
import org.etieskrill.engine.graphics.gl.Texture.TextureType;

import java.util.*;

import static org.etieskrill.engine.graphics.assimp.Vertex.*;
import static org.lwjgl.opengl.GL33C.*;

public class Loader implements Disposable {

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
    
    private int createVAO() {
        int vao = glGenVertexArrays();
        vaos.add(vao);
        
        glBindVertexArray(vao);
        
        return vao;
    }
    
    private int prepareVBO(float[] data) {
        int vbo = glGenBuffers();
        vbos.add(vbo);
    
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
    
    private void setVertexAttributePointer(int index, int numComponents, boolean normalised, int offset) {
        glEnableVertexAttribArray(index);
        int totalStride = COMPONENTS * GL_FLOAT_BYTE_SIZE;
        glVertexAttribPointer(index, numComponents, GL_FLOAT, normalised, totalStride, offset);
    }
    
    private int prepareIndexBuffer(short[] indices) {
        int ebo = glGenBuffers();
        ebos.add(ebo);

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_DYNAMIC_DRAW);
        
        return ebo;
    }
    
    private void unbindVAO() {
        glBindVertexArray(0);
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
//            System.out.printf("[%s] Texture \"%s\" was already loaded.\n", getClass().getSimpleName(), name);
            return textures.get(name);
        }
        
        Texture texture = Texture.ofFile(file, type);
        textures.put(name, texture);
        return texture;
    }
    
    public Texture getTexture(String name) {
        return textures.get(name);
    }
    
    @Override
    public void dispose() {
        for (int vao : vaos) glDeleteBuffers(vao);
        for (int vbo : vbos) glDeleteVertexArrays(vbo);
        for (int ebo : ebos) glDeleteBuffers(ebo);
        for (Disposable texture : textures.values()) texture.dispose();
    }
    
}
