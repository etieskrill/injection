module injection.engine {
    requires org.lwjgl.assimp;
    requires org.lwjgl.freetype;
    requires org.lwjgl.glfw;
    requires org.lwjgl.opengl;
    requires org.lwjgl.stb;
    requires org.lwjgl.meshoptimizer;

    requires org.joml;
    requires org.joml.primitives;

    requires org.yaml.snakeyaml;

    requires org.slf4j;

    requires static org.jetbrains.annotations;
    requires static lombok;

    exports org.etieskrill.engine.application;
    exports org.etieskrill.engine.entity;
    exports org.etieskrill.engine.entity.component;
    exports org.etieskrill.engine.entity.service;
    exports org.etieskrill.engine.entity.service.impl;
    exports org.etieskrill.engine.entity.system;
    exports org.etieskrill.engine.graphics.camera;
    exports org.etieskrill.engine.graphics.data;
    exports org.etieskrill.engine.graphics.gl;
    exports org.etieskrill.engine.graphics.gl.framebuffer;
    exports org.etieskrill.engine.graphics.gl.shader;
    exports org.etieskrill.engine.graphics.texture;
    exports org.etieskrill.engine.graphics.texture.animation;
    exports org.etieskrill.engine.input;
    exports org.etieskrill.engine.input.action;
    exports org.etieskrill.engine.input.controller;
    exports org.etieskrill.engine.time;
    exports org.etieskrill.engine.util;
    exports org.etieskrill.engine.window;

    opens org.etieskrill.injection; //FIXME gradle complains this is empty, only counts class files? find exclusion
}
