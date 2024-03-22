package org.etieskrill.engine.graphics.model;

import org.etieskrill.engine.Disposable;
import org.etieskrill.engine.graphics.texture.AbstractTexture;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector4f;

import java.util.*;

import static java.util.Objects.requireNonNullElse;
import static org.lwjgl.assimp.Assimp.*;

//TODO separate phong/pbr via inheritance
public class Material implements Disposable {
    
    //TODO number max gl texture units check
    private final List<AbstractTexture> textures;

    private final Map<Property, Object> properties;

    public enum Property {
        NAME(AI_MATKEY_NAME),

        COLOUR_BASE(AI_MATKEY_BASE_COLOR),
        COLOUR_AMBIENT(AI_MATKEY_COLOR_AMBIENT),
        COLOUR_DIFFUSE(AI_MATKEY_COLOR_DIFFUSE),
        COLOUR_SPECULAR(AI_MATKEY_COLOR_SPECULAR),
        COLOUR_EMISSIVE(AI_MATKEY_COLOR_EMISSIVE),

        INTENSITY_EMISSIVE(AI_MATKEY_EMISSIVE_INTENSITY),

        SHININESS(AI_MATKEY_SHININESS),
        SHININESS_STRENGTH(AI_MATKEY_SHININESS_STRENGTH),
        METALLIC_FACTOR(AI_MATKEY_METALLIC_FACTOR),
        OPACITY(AI_MATKEY_OPACITY);

        private final String aiPropertyName;

        Property(String aiPropertyName) {
            this.aiPropertyName = aiPropertyName;
        }

        public String ai() {
            return aiPropertyName;
        }
    }
    
    public static final class Builder {
        private List<AbstractTexture> textures = new LinkedList<>();
        private Map<Property, Object> properties = new HashMap<>();

        public Builder addTextures(AbstractTexture... textures) {
            this.textures.addAll(List.of(textures));
            return this;
        }

        public Builder setTextures(AbstractTexture... textures) {
            this.textures.clear();
            this.textures.addAll(List.of(textures));
            return this;
        }

        public Builder setProperty(Property property, Object value) {
            this.properties.put(property, value);
            return this;
        }

        public Material build() {
            return new Material(textures, properties);
        }
    }
    
    public static Material getBlank() {
        return new Material.Builder().build();
    }

    private Material(List<AbstractTexture> textures, Map<Property, Object> properties) {
        this.textures = new ArrayList<>(textures);
        this.properties = new HashMap<>(properties);
    }
    
    public List<AbstractTexture> getTextures() {
        return textures;
    }

    public Map<Property, Object> getProperties() {
        return properties;
    }

    public Object getProperty(Property property) {
        return properties.get(property);
    }

    public void setProperty(Property property, Object value) {
        properties.put(property, value);
    }

    public Vector4f getColourProperty(Property colourProperty) {
        return (Vector4f) requireNonNullElse(properties.get(colourProperty), new Vector4f(0));
    }

    public @Nullable Number getValueProperty(Property valueProperty) {
        return (Number) properties.get(valueProperty);
    }

    private boolean wasAlreadyDisposed = false;
    
    @Override
    public void dispose() {
        if (wasAlreadyDisposed) return;
        textures.forEach(AbstractTexture::dispose);
        wasAlreadyDisposed = true;
    }
    
}
