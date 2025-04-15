package org.etieskrill.engine.graphics.model;

import lombok.Getter;
import lombok.Setter;
import org.etieskrill.engine.Disposable;
import org.etieskrill.engine.graphics.gl.VertexArrayObject;
import org.joml.primitives.AABBf;

import java.util.Collections;
import java.util.List;

import static org.lwjgl.opengl.GL33C.*;

@Getter
public class Mesh implements Disposable {

    private final Material material;
    private final List<Bone> bones;
    private final VertexArrayObject<Vertex> vao;
    //TODO immutable
    private @Setter int numIndices;
    private final AABBf boundingBox;
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
                VertexArrayObject<Vertex> vao, int numIndices,
                AABBf boundingBox,
                DrawMode drawMode) {
        this.material = material;
        this.bones = Collections.unmodifiableList(bones);

        this.vao = vao;
        this.numIndices = numIndices;

        this.boundingBox = boundingBox;

        this.drawMode = drawMode;
    }

    public AABBf getBoundingBox() {
        return boundingBox;
    }

    @Override
    public void dispose() {
        material.dispose();
        vao.dispose();
    }

}
