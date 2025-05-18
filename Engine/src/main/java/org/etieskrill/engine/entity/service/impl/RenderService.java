package org.etieskrill.engine.entity.service.impl;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.etieskrill.engine.Disposable;
import org.etieskrill.engine.entity.Entity;
import org.etieskrill.engine.entity.component.*;
import org.etieskrill.engine.entity.service.Service;
import org.etieskrill.engine.graphics.camera.Camera;
import org.etieskrill.engine.graphics.data.PointLight;
import org.etieskrill.engine.graphics.gl.framebuffer.FrameBuffer;
import org.etieskrill.engine.graphics.gl.framebuffer.FrameBufferAttachment.BufferAttachmentType;
import org.etieskrill.engine.graphics.gl.framebuffer.RenderBuffer;
import org.etieskrill.engine.graphics.gl.renderer.GLParticleRenderer;
import org.etieskrill.engine.graphics.gl.renderer.GLRenderer;
import org.etieskrill.engine.graphics.gl.shader.ShaderProgram;
import org.etieskrill.engine.graphics.gl.shader.impl.*;
import org.etieskrill.engine.graphics.model.CubeMapModel;
import org.etieskrill.engine.graphics.texture.AbstractTexture;
import org.etieskrill.engine.graphics.texture.Texture2D;
import org.etieskrill.engine.graphics.texture.Textures;
import org.jetbrains.annotations.Nullable;
import org.joml.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.Math.abs;
import static java.lang.Math.max;
import static org.etieskrill.engine.graphics.gl.framebuffer.FrameBufferAttachment.BufferAttachmentType.COLOUR0;
import static org.etieskrill.engine.graphics.gl.framebuffer.FrameBufferAttachment.BufferAttachmentType.COLOUR1;
import static org.etieskrill.engine.graphics.gl.framebuffer.RenderBuffer.Type.DEPTH_STENCIL;
import static org.etieskrill.engine.graphics.texture.AbstractTexture.Format.RGBA_F16;
import static org.lwjgl.opengl.GL11C.*;
import static org.lwjgl.opengl.GL30C.GL_CULL_FACE;
import static org.lwjgl.opengl.GL30C.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL30C.GL_ONE;
import static org.lwjgl.opengl.GL30C.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL30C.GL_POINTS;
import static org.lwjgl.opengl.GL30C.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL30C.GL_ZERO;
import static org.lwjgl.opengl.GL30C.glBlendFunc;
import static org.lwjgl.opengl.GL30C.glDisable;
import static org.lwjgl.opengl.GL30C.glEnable;
import static org.lwjgl.opengl.GL30C.*;

public class RenderService implements Service, Disposable {

    protected final GLRenderer renderer;
    private final GaussBlurPostBuffers gaussBlurPostBuffers;
    protected final @Getter FrameBuffer frameBuffer;
    private final Camera camera;
    private @Getter
    @Accessors(fluent = true)
    @Setter Camera cullingCamera;
    private final Vector2ic windowSize;
    private @Accessors(fluent = true)
    @Setter
    @Nullable Vector4i customViewport;
    private final StaticShader shader;
    private final LightSourceShader lightSourceShader;

    private @Getter
    @Setter
    @Nullable CubeMapModel skybox;
    private final SkyboxShader skyboxShader;

    private @Accessors(fluent = true)
    @Setter boolean blur = true;

    private final ShaderParams shaderParams;

    //TODO remove jury-rigged service
    // - "inner services"?
    // - service groups?
    // - global render state -> context as entity?
    private final @Getter
    @Deprecated BoundingBoxRenderService boundingBoxRenderService;
    private final @Getter
    @Deprecated ParticleRenderService particleRenderService;
    private @Deprecated double lastDelta = 0;

    public RenderService(GLRenderer renderer, Camera camera, Vector2ic windowSize) {
        this.renderer = renderer;
        this.gaussBlurPostBuffers = new GaussBlurPostBuffers(windowSize);
        this.frameBuffer = gaussBlurPostBuffers.getFrameBuffer();
        this.camera = camera;
        this.cullingCamera = camera;
        this.windowSize = windowSize;
        this.shader = new StaticShader();
        this.lightSourceShader = new LightSourceShader();

        this.shaderParams = new ShaderParams(new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashSet<>());

        this.boundingBoxRenderService = new BoundingBoxRenderService(renderer, camera);
        this.particleRenderService = new ParticleRenderService(new GLParticleRenderer(), camera);

        this.skyboxShader = new SkyboxShader();
    }

    private record ShaderParams(
            Map<String, Object> uniformBindings,
            Map<String, Object[]> uniformArrayBindings,
            Map<String, AbstractTexture> textureBindings,
            Set<ShaderProgram> configuredShaders
    ) {
        void clear() {
            uniformBindings.clear();
            uniformArrayBindings.clear();
            textureBindings.clear();
            configuredShaders.clear();
        }

        void addUniform(String name, Object value) {
            uniformBindings.put(name, value);
        }

        void addUniformArray(String name, Object... values) {
            uniformArrayBindings.put(name, values);
        }

        void addTexture(String name, AbstractTexture texture) {
            textureBindings.put(name, texture);
        }

        boolean isConfigured(ShaderProgram shader) {
            return !configuredShaders.add(shader);
        }
    }

    @Override
    public boolean canProcess(Entity entity) {
        return entity.hasComponents(Transform.class, Drawable.class);
    }

    @Override
    public void preProcess(List<Entity> entities) {
        frameBuffer.clear();
        frameBuffer.bind();
        renderer.prepare();
        glViewport(0, 0, windowSize.x(), windowSize.y()); //TODO move to framebuffer

        shaderParams.clear();

        if (skybox != null) {
            renderer.render(skybox, (ShaderProgram) skyboxShader.getShader(), camera.getCombined());
        }

        for (Entity entity : entities) {
            DirectionalLightComponent directionalLightComponent = entity.getComponent(DirectionalLightComponent.class);
            if (directionalLightComponent == null) continue;
            //TODO expand to multiple directional lights
            shaderParams.addUniform("hasShadowMap", directionalLightComponent.getShadowMap() != null);
            if (directionalLightComponent.getShadowMap() != null) {
                shaderParams.addTexture("shadowMap", directionalLightComponent.getShadowMap().getTexture());
                shaderParams.addUniform("lightCombined", directionalLightComponent.getCamera().getCombined());
            }
            shaderParams.addUniformArray("globalLights", directionalLightComponent.getDirectionalLight());
            break;
        }

        PointLightComponent[] pointLightComponents = entities.stream()
                .map(entity -> entity.getComponent(PointLightComponent.class))
                .filter(Objects::nonNull)
                .toArray(PointLightComponent[]::new);

        if (pointLightComponents.length > 0) {
            shaderParams.addUniformArray("lights", (Object[]) Arrays.stream(pointLightComponents)
                    .map(PointLightComponent::getLight)
                    .toArray(PointLight[]::new));
        }

        AtomicBoolean hasPointShadowMap = new AtomicBoolean(false);
        AtomicInteger numPointShadowMaps = new AtomicInteger();
        Arrays.stream(pointLightComponents)
                .map(PointLightComponent::getShadowMap)
                .filter(Objects::nonNull)
                .distinct()
                .forEach(pointShadowMapArray -> {
                    hasPointShadowMap.set(true);
                    shaderParams.addTexture(
                            "pointShadowMaps" + numPointShadowMaps.getAndIncrement(),
                            pointShadowMapArray.getTexture());
                });
        shaderParams.addUniform("hasPointShadowMaps", hasPointShadowMap.get());

        shaderParams.addUniform("viewPosition", camera.getViewPosition());

        for (Entity entity : entities) {
            if (boundingBoxRenderService.canProcess(entity)) {
                boundingBoxRenderService.process(entity, entities, 0);
            }
        }
    }

    @Override
    public void process(Entity targetEntity, List<Entity> entities, double delta) {
        Boolean enabled = targetEntity.getComponent(Boolean.class);
        if (enabled != null && !enabled) return;

        Drawable drawable = targetEntity.getComponent(Drawable.class);
        if (!drawable.isVisible()) return;

        Transform transform = targetEntity.getComponent(Transform.class);

        WorldSpaceAABB aabb = targetEntity.getComponent(WorldSpaceAABB.class);
        if (aabb != null && !cullingCamera.frustumTestAABB(aabb)) { //TODO check if relevant service is even present
            return;
        }

        ShaderProgram shader = getConfiguredShader(targetEntity, drawable);
        if (!drawable.isDrawWireframe()) {
            renderer.render(transform, drawable.getModel(), shader, camera);
        } else {
            renderer.renderWireframe(transform, drawable.getModel(), shader, camera);
        }

        lastDelta = delta;
    }

    protected ShaderProgram getConfiguredShader(Entity entity, Drawable drawable) {
        if (drawable.getShader() != null) {
            configureShader(drawable.getShader(), shaderParams);
            return drawable.getShader();
        }

        DirectionalLightComponent directionalLightComponent = entity.getComponent(DirectionalLightComponent.class);
        PointLightComponent pointLightComponent = entity.getComponent(PointLightComponent.class);
        if (directionalLightComponent != null) {
            LightSourceShaderKt.setLight(lightSourceShader, directionalLightComponent.getDirectionalLight());
            return lightSourceShader;
        } else if (pointLightComponent != null) {
            LightSourceShaderKt.setLight(lightSourceShader, pointLightComponent.getLight());
            if (pointLightComponent.getFarPlane() != null)
                StaticShaderKt.setPointShadowFarPlane(shader, pointLightComponent.getFarPlane()); //TODO make per-light?
            return lightSourceShader;
        } else {
            configureShader(shader, shaderParams);
            StaticShaderKt.setTextureScale(shader, drawable.getTextureScale());
            return shader;
        }
    }

    private void configureShader(ShaderProgram shader, ShaderParams params) {
        if (params.isConfigured(shader)) return;

        params.uniformBindings.forEach(shader::setUniformNonStrict);
        params.uniformArrayBindings.forEach(shader::setUniformArrayNonStrict);
        params.textureBindings.forEach((name, texture) -> renderer.bindNextFreeTexture(shader, name, texture));
    }

    public HDRShader getHdrShader() {
        return gaussBlurPostBuffers.getHdrShader();
    }

    @Override
    public void postProcess(List<Entity> entities) {
        for (Entity entity : entities) { //FIXME particle rendering is suddenly really fucking slow for some reason
            if (particleRenderService.canProcess(entity)) {
                particleRenderService.process(entity, entities, lastDelta);
            }
        }

        frameBuffer.unbind();
        if (customViewport != null) {
            glViewport(customViewport.x(), customViewport.y(), customViewport.z(), customViewport.w());
        }
        gaussBlurPostBuffers.renderToScreen(blur);
    }

    @Override
    public Set<Class<? extends Service>> runAfter() {
        return Set.of(DirectionalShadowMappingService.class, PointShadowMappingService.class);
    }

    @Override
    public void dispose() {
        renderer.dispose();
        gaussBlurPostBuffers.dispose();
        shader.dispose();
        lightSourceShader.dispose();
    }

}

class GaussBlurPostBuffers implements Disposable {

    private static final int GAUSS_BLUR_ITERATIONS = 3;

    private final @Getter FrameBuffer frameBuffer;
    private final Texture2D hdrBuffer;
    private final Texture2D bloomBuffer;
    private final @Getter HDRShader hdrShader;

    private final FrameBuffer blurFrameBuffer1;
    private final Texture2D blurTextureBuffer1;
    private final FrameBuffer blurFrameBuffer2;
    private final Texture2D blurTextureBuffer2;
    private final GaussBlurShader gaussBlurShader;

    private final @Getter int dummyVAO;

    public GaussBlurPostBuffers(Vector2ic windowSize) {
        this.hdrBuffer = Textures.genBlank(windowSize, RGBA_F16);
        this.bloomBuffer = Textures.genBlank(windowSize, RGBA_F16);
        RenderBuffer depthStencilBuffer = new RenderBuffer(windowSize, DEPTH_STENCIL);
        this.frameBuffer = new FrameBuffer.Builder(windowSize)
                .attach(hdrBuffer, COLOUR0)
                .attach(bloomBuffer, COLOUR1)
                .attach(depthStencilBuffer, BufferAttachmentType.DEPTH_STENCIL)
                .build();

        this.hdrShader = new HDRShader();

        this.blurTextureBuffer1 = Textures.genBlank(windowSize, RGBA_F16);
        this.blurFrameBuffer1 = new FrameBuffer.Builder(windowSize)
                .attach(blurTextureBuffer1, COLOUR0)
                .build();
        this.blurTextureBuffer2 = Textures.genBlank(windowSize, RGBA_F16);
        this.blurFrameBuffer2 = new FrameBuffer.Builder(windowSize)
                .attach(blurTextureBuffer2, COLOUR0)
                .build();

        this.gaussBlurShader = new GaussBlurShader();

        dummyVAO = glGenVertexArrays();
    }

    public void renderToScreen(boolean blur) {
        glBindVertexArray(dummyVAO); //though effectively unused in the shader, no bound vao causes undefined behaviour while rendering. this ensures there is always one bound

        boolean blurBuffer1IsTarget = false;

        if (blur) {
            blurFrameBuffer1.bind();
            gaussBlurShader.start();
            gaussBlurShader.setSource(bloomBuffer);
            gaussBlurShader.setHorizontal(true);
            gaussBlurShader.setSampleDistance(new Vector2f(2));
            glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

            for (int i = 0; i < GAUSS_BLUR_ITERATIONS * 2 - 1; i++) {
                (blurBuffer1IsTarget ? blurFrameBuffer1 : blurFrameBuffer2).bind();
                gaussBlurShader.setSource(blurBuffer1IsTarget ? blurTextureBuffer2 : blurTextureBuffer1);
                gaussBlurShader.setHorizontal(blurBuffer1IsTarget);
                glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

                blurBuffer1IsTarget = !blurBuffer1IsTarget;
            }
        }

        FrameBuffer.bindScreenBuffer();

        hdrShader.start();
        hdrShader.setHdrBuffer(hdrBuffer);
        if (blur) hdrShader.setBloomBuffer(blurBuffer1IsTarget ? blurTextureBuffer1 : blurTextureBuffer2);
        else hdrShader.setBloomBuffer(bloomBuffer);
        glDepthMask(false);
        glDisable(GL_DEPTH_TEST);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
        glBlendFunc(GL_ONE, GL_ZERO);
        glDepthMask(true);
        glEnable(GL_DEPTH_TEST);
    }

    @Override
    public void dispose() {
        frameBuffer.dispose();
        hdrShader.dispose();
        blurFrameBuffer1.dispose();
        blurFrameBuffer2.dispose();
        gaussBlurShader.dispose();
        glDeleteVertexArrays(dummyVAO);
    }

}

//was only for debugging purposes, can maybe be reused for some visualisation
class BoundingSphereRenderer implements Disposable {

    private final ShaderProgram boundingSphereShader;
    private final int dummyVao;

    BoundingSphereRenderer(ShaderProgram boundingSphereShader) {
        this.boundingSphereShader = new ShaderProgram(List.of("BoundingSphere.glsl"), false) {
        };

        this.dummyVao = glGenVertexArrays();
    }

    private void drawBoundingSpherePerspective(Camera cullingCamera, WorldSpaceAABB aabb) {
        Vector4f position = cullingCamera.getCombined().transform(new Vector4f(aabb.center(new Vector3f()), 1));
        position.x /= position.w;
        position.y /= position.w;

        //TODO play with joml env: joml.debug=true;joml.fastmath=true;joml.sinLookup=true
        float boundRad = (aabb.getSize(new Vector3f()).length() / 2) / max(1, abs(position.z));

        Vector2ic viewport = cullingCamera.getViewportSize();
        float aspect = (float) viewport.x() / (float) viewport.y();

        boundingSphereShader.setUniform("position", position);
        boundingSphereShader.setUniform("radius", boundRad);

        boundingSphereShader.setUniform("aspect", aspect);
        boundingSphereShader.start();

        glBindVertexArray(dummyVao);

        glDisable(GL_DEPTH_TEST);
        glDisable(GL_CULL_FACE);
        glDepthMask(false);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDrawArrays(GL_POINTS, 0, 1);
        glBlendFunc(GL_ONE, GL_ZERO);
        glDepthMask(true);
        glEnable(GL_CULL_FACE);
        glEnable(GL_DEPTH_TEST);
    }

    @Override
    public void dispose() {
        boundingSphereShader.dispose();
        glDeleteVertexArrays(dummyVao);
    }

}
