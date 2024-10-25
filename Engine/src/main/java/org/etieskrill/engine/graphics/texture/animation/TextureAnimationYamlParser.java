package org.etieskrill.engine.graphics.texture.animation;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.etieskrill.engine.config.ResourcePaths;
import org.joml.Vector2i;
import org.joml.Vector2ic;
import org.joml.primitives.Rectanglei;
import org.yaml.snakeyaml.Yaml;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static java.lang.Math.max;
import static org.etieskrill.engine.util.ResourceReader.getClasspathResourceAsStream;

public class TextureAnimationYamlParser {

    static TextureAnimationMetadata loadAnimationMetadata(String metaFile) {
        Yaml yaml = new Yaml();
        YamlTextureAnimationMetadata yamlMetaData = yaml.loadAs(
                getClasspathResourceAsStream(ResourcePaths.TEXTURE_PATH + metaFile),
                YamlTextureAnimationMetadata.class
        );
        if (yamlMetaData.resources == null || yamlMetaData.resources.get("texture") == null) {
            throw new RuntimeException("Animation resource cannot be null");
        }

        List<TextureAnimationFrame> frames = new ArrayList<>();
        YamlTextureAnimationFrame defaultFrame = yamlMetaData.frames.get("default");
        AtomicInteger maxX = new AtomicInteger(), maxY = new AtomicInteger();
        AtomicReference<Float> currentTimeMillis = new AtomicReference<>(0f);
        yamlMetaData.frames.forEach((frameName, frame) -> {
            if (frameName.equals("default")) return;
            var parsedFrame = parseFrame(frame, defaultFrame, currentTimeMillis);
            maxX.set(max(maxX.get(), parsedFrame.getAtlasArea().lengthX()));
            maxY.set(max(maxY.get(), parsedFrame.getAtlasArea().lengthY()));
            frames.add(parsedFrame);
        });

        return new TextureAnimationMetadata(
                yamlMetaData.resources.get("texture").file,
                new Vector2i(maxX.get(), maxY.get()),
                frames,
                currentTimeMillis.get()
        );
    }

    private static TextureAnimationFrame parseFrame(YamlTextureAnimationFrame frame, YamlTextureAnimationFrame defaultFrame, AtomicReference<Float> currentTimeMillis) {
        if (frame == null) {
            return parseDefaultFrame(defaultFrame, currentTimeMillis);
        } else {
            var newFrame = new TextureAnimationFrame(
                    new Rectanglei(getElse(frame.x, defaultFrame.x), getElse(frame.y, defaultFrame.y),
                            getElse(frame.x, defaultFrame.x) + getElse(frame.w, defaultFrame.w),
                            getElse(frame.y, defaultFrame.y) + getElse(frame.h, defaultFrame.h)),
                    currentTimeMillis.get());
            currentTimeMillis.updateAndGet(currentTime -> currentTime + getElse(frame.duration, defaultFrame.duration));
            return newFrame;
        }
    }

    private static TextureAnimationFrame parseDefaultFrame(YamlTextureAnimationFrame yamlFrame, AtomicReference<Float> currentTimeMillis) {
        var frame = new TextureAnimationFrame(
                new Rectanglei(yamlFrame.x, yamlFrame.y, yamlFrame.x + yamlFrame.w, yamlFrame.y + yamlFrame.h),
                currentTimeMillis.get());
        currentTimeMillis.updateAndGet(currentTime -> currentTime + yamlFrame.duration);
        return frame;
    }

    private static <T> T getElse(T object, T defaultObject) {
        return object == null ? defaultObject : object;
    }

}

@Data
class TextureAnimationMetadata {
    private final String textureFile;
    private final Vector2ic frameSize;
    private final List<TextureAnimationFrame> frames;
    private final float duration;
}

@Data
class TextureAnimationFrame {
    private final Rectanglei atlasArea;
    private final float time;
}

@NoArgsConstructor
class YamlTextureAnimationMetadata {
    public String $schema;
    public Map<String, YamlTextureAnimationResource> resources;
    public Map<String, YamlTextureAnimationFrame> frames;
}

@NoArgsConstructor
class YamlTextureAnimationResource {
    public String file;
}

@NoArgsConstructor
class YamlTextureAnimationFrame {
    public Integer x, y, w, h;
    public Float duration;
}
